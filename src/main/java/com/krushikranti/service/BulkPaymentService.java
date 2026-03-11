package com.krushikranti.service;

import com.krushikranti.dto.request.BulkPaymentInitiateRequest;
import com.krushikranti.dto.response.BulkOrderResponse;
import com.krushikranti.dto.response.PaymentOrderResponse;
import com.krushikranti.dto.response.TrackingResponse;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.util.RazorpayUtils;
import com.krushikranti.model.*;
import com.krushikranti.repository.BulkOrderRepository;
import com.krushikranti.repository.DealOfferRepository;
import com.krushikranti.repository.UserRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkPaymentService {

    private final BulkOrderRepository bulkOrderRepository;
    private final DealOfferRepository dealOfferRepository;
    private final UserRepository userRepository;
    private final ShiprocketService shiprocketService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    private static final BigDecimal PLATFORM_FEE_PERCENT = new BigDecimal("0.05"); // 5%
    private static final BigDecimal FARMER_PAYOUT_PERCENT = new BigDecimal("0.95"); // 95%

    // Temporary in-memory store: razorpayOrderId → pending payment data.
    // BulkOrder is only persisted after successful payment verification.
    private final ConcurrentHashMap<String, PendingPaymentData> pendingPayments = new ConcurrentHashMap<>();

    private record PendingPaymentData(Long dealOfferId, User wholesaler, BulkPaymentInitiateRequest shippingRequest) {
    }

    /**
     * Create a Razorpay payment order for an accepted deal.
     * The BulkOrder entity is NOT persisted here — only after payment verification
     * succeeds.
     */
    @Transactional
    public PaymentOrderResponse initiatePayment(String wholesalerEmail, Long dealOfferId,
            BulkPaymentInitiateRequest shippingRequest) {
        User wholesaler = userRepository.findByEmail(wholesalerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", wholesalerEmail));

        DealOffer deal = dealOfferRepository.findById(dealOfferId)
                .orElseThrow(() -> new ResourceNotFoundException("DealOffer", "id", dealOfferId));

        if (deal.getStatus() != DealOffer.DealStatus.ACCEPTED) {
            throw new RuntimeException("Cannot initiate payment for a non-accepted deal");
        }

        Conversation conversation = deal.getConversation();
        if (!conversation.getWholesaler().getId().equals(wholesaler.getId())) {
            throw new RuntimeException("Only the wholesaler in this deal can initiate payment");
        }

        // Reject if payment was already completed for this deal
        bulkOrderRepository.findByDealOffer(deal).ifPresent(existingOrder -> {
            if (existingOrder.getPaymentStatus() == BulkOrder.PaymentStatus.PAID) {
                throw new RuntimeException("Payment already completed for this deal");
            }
        });

        BigDecimal totalAmount = deal.getTotalPrice();

        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            int amountInPaise = totalAmount.multiply(new BigDecimal("100")).intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "bulk_" + dealOfferId);
            orderRequest.put("notes", new JSONObject()
                    .put("deal_offer_id", dealOfferId)
                    .put("product_name", conversation.getBulkProduct().getName())
                    .put("farmer_id", conversation.getFarmer().getId())
                    .put("wholesaler_id", wholesaler.getId()));

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // Cache shipping + deal data until payment is verified
            pendingPayments.put(razorpayOrderId, new PendingPaymentData(dealOfferId, wholesaler, shippingRequest));

            log.info("Created Razorpay order {} for bulk deal {} — BulkOrder will be persisted after verification",
                    razorpayOrderId, dealOfferId);

            return new PaymentOrderResponse(
                    razorpayOrderId,
                    razorpayOrder.get("currency"),
                    razorpayOrder.get("amount"),
                    razorpayOrder.get("status"));

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for bulk deal", e);
            throw new RuntimeException("Error communicating with payment gateway", e);
        }
    }

    /**
     * Verify Razorpay signature, then (and only then) create and persist the
     * BulkOrder,
     * and trigger Shiprocket shipment.
     */
    @Transactional
    public BulkOrderResponse verifyAndProcessPayment(String razorpayOrderId, String razorpayPaymentId,
            String razorpaySignature) {
        try {
            boolean isSignatureValid = RazorpayUtils.verifySignature(
                    razorpayOrderId,
                    razorpayPaymentId,
                    razorpaySignature,
                    razorpayKeySecret);
            if (!isSignatureValid) {
                throw new RuntimeException("Invalid payment signature");
            }

            // Retrieve cached data from initiation
            PendingPaymentData pendingData = pendingPayments.get(razorpayOrderId);
            if (pendingData == null) {
                // Duplicate verify call — BulkOrder may already be created
                return bulkOrderRepository.findByRazorpayOrderId(razorpayOrderId)
                        .map(BulkOrderResponse::fromEntity)
                        .orElseThrow(() -> new RuntimeException(
                                "No pending payment data found for order: " + razorpayOrderId));
            }

            DealOffer deal = dealOfferRepository.findById(pendingData.dealOfferId())
                    .orElseThrow(() -> new ResourceNotFoundException("DealOffer", "id", pendingData.dealOfferId()));

            Conversation conversation = deal.getConversation();
            BigDecimal totalAmount = deal.getTotalPrice();
            BigDecimal platformFee = totalAmount.multiply(PLATFORM_FEE_PERCENT).setScale(2, RoundingMode.HALF_UP);
            BigDecimal farmerPayout = totalAmount.multiply(FARMER_PAYOUT_PERCENT).setScale(2, RoundingMode.HALF_UP);

            BulkPaymentInitiateRequest shipping = pendingData.shippingRequest();

            // Create BulkOrder only now — after successful payment
            BulkOrder bulkOrder = BulkOrder.builder()
                    .dealOffer(deal)
                    .farmer(conversation.getFarmer())
                    .wholesaler(pendingData.wholesaler())
                    .bulkProduct(conversation.getBulkProduct())
                    .totalAmount(totalAmount)
                    .platformFee(platformFee)
                    .farmerPayout(farmerPayout)
                    .razorpayOrderId(razorpayOrderId)
                    .razorpayPaymentId(razorpayPaymentId)
                    .paymentStatus(BulkOrder.PaymentStatus.PAID)
                    .orderStatus(BulkOrder.OrderStatus.CONFIRMED)
                    .shippingName(shipping.getFullName())
                    .shippingPhone(shipping.getPhone())
                    .shippingAddress(shipping.getAddress())
                    .shippingCity(shipping.getCity())
                    .shippingState(shipping.getState())
                    .shippingPincode(shipping.getPincode())
                    .build();

            bulkOrderRepository.save(bulkOrder);
            pendingPayments.remove(razorpayOrderId); // Clean up cache

            log.info("BulkOrder {} created post-payment — Total: ₹{}, Platform Fee: ₹{}, Farmer Payout: ₹{}",
                    bulkOrder.getId(), bulkOrder.getTotalAmount(), bulkOrder.getPlatformFee(),
                    bulkOrder.getFarmerPayout());

            // Trigger Shiprocket shipment
            try {
                createBulkShipment(bulkOrder);
            } catch (Exception e) {
                log.error("Failed to create shipment for bulk order: {}", bulkOrder.getId(), e);
                // Do not fail the payment response
            }

            return BulkOrderResponse.fromEntity(bulkOrder);

        } catch (RazorpayException e) {
            log.error("Payment signature verification failed", e);
            throw new RuntimeException("Payment verification failed", e);
        }
    }

    /**
     * Create Shiprocket shipment for bulk order
     */
    private void createBulkShipment(BulkOrder bulkOrder) {
        try {
            String token = shiprocketService.authenticate();

            // Create shipment via Shiprocket
            var shipmentResponse = shiprocketService.createBulkShipment(bulkOrder, token);

            if (shipmentResponse != null) {
                bulkOrder.setShipmentId(shipmentResponse.getShipmentId());
                bulkOrder.setAwbCode(shipmentResponse.getAwbCode());
                bulkOrder.setCourierName(shipmentResponse.getCourierName());
                bulkOrder.setTrackingUrl(shipmentResponse.getTrackingUrl());
                bulkOrder.setDeliveryStatus(BulkOrder.DeliveryStatus.PICKUP_SCHEDULED);
                bulkOrder.setOrderStatus(BulkOrder.OrderStatus.PROCESSING);

                // Set estimated delivery (typically 5-7 days for B2B)
                bulkOrder.setEstimatedDelivery(LocalDateTime.now().plusDays(5));
            } else {
                // Fallback if Shiprocket fails
                bulkOrder.setDeliveryStatus(BulkOrder.DeliveryStatus.PICKUP_SCHEDULED);
                bulkOrder.setOrderStatus(BulkOrder.OrderStatus.PROCESSING);
                bulkOrder.setTrackingUrl("https://shiprocket.co/tracking/" + bulkOrder.getId());
            }

            bulkOrderRepository.save(bulkOrder);
            log.info("Bulk shipment scheduled for order: {} with AWB: {}",
                    bulkOrder.getId(), bulkOrder.getAwbCode());
        } catch (Exception e) {
            log.error("Shiprocket shipment creation failed, setting default status", e);
            bulkOrder.setDeliveryStatus(BulkOrder.DeliveryStatus.PICKUP_SCHEDULED);
            bulkOrder.setOrderStatus(BulkOrder.OrderStatus.PROCESSING);
            bulkOrderRepository.save(bulkOrder);
        }
    }

    /**
     * Get bulk orders for a user
     */
    public List<BulkOrderResponse> getOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        List<BulkOrder> orders;
        if (user.getRole() == User.Role.ROLE_ADMIN) {
            orders = bulkOrderRepository.findAllByOrderByCreatedAtDesc();
        } else if (user.getRole() == User.Role.ROLE_FARMER) {
            orders = bulkOrderRepository.findByFarmerOrderByCreatedAtDesc(user);
        } else if (user.getRole() == User.Role.ROLE_WHOLESALER) {
            orders = bulkOrderRepository.findByWholesalerOrderByCreatedAtDesc(user);
        } else {
            throw new RuntimeException("You do not have access to bulk orders");
        }

        return orders.stream()
                .map(BulkOrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get a single bulk order by ID (access-controlled)
     */
    public BulkOrderResponse getOrder(String userEmail, Long orderId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        BulkOrder order = bulkOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("BulkOrder", "id", orderId));
        boolean hasAccess = user.getRole() == User.Role.ROLE_ADMIN
                || order.getFarmer().getId().equals(user.getId())
                || order.getWholesaler().getId().equals(user.getId());
        if (!hasAccess) {
            throw new RuntimeException("You do not have access to this order");
        }
        return BulkOrderResponse.fromEntity(order);
    }

    /**
     * Get tracking info for a bulk order
     */
    public TrackingResponse getOrderTracking(String userEmail, Long orderId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        BulkOrder order = bulkOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("BulkOrder", "id", orderId));

        boolean hasAccess = user.getRole() == User.Role.ROLE_ADMIN
                || order.getFarmer().getId().equals(user.getId())
                || order.getWholesaler().getId().equals(user.getId());

        if (!hasAccess) {
            throw new RuntimeException("You do not have access to this order");
        }

        if (order.getAwbCode() == null || order.getAwbCode().isBlank()) {
            TrackingResponse noTrack = new TrackingResponse();
            noTrack.setSuccess(false);
            noTrack.setMessage("Shipment not yet dispatched — no AWB code available.");
            noTrack.setDeliveryStatus(order.getDeliveryStatus().name());
            noTrack.setOrderStatus(order.getOrderStatus().name());
            return noTrack;
        }

        TrackingResponse tracking = shiprocketService.getTrackingInfo(order.getAwbCode());
        tracking.setOrderId(orderId);
        tracking.setOrderStatus(order.getOrderStatus().name());
        tracking.setDeliveryStatus(order.getDeliveryStatus().name());
        tracking.setCourierName(order.getCourierName());
        tracking.setDeliveryPartnerName(order.getCourierName());
        if (tracking.getTrackingUrl() == null) {
            tracking.setTrackingUrl(order.getTrackingUrl());
        }
        return tracking;
    }
}
