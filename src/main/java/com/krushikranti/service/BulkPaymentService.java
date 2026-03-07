package com.krushikranti.service;

import com.krushikranti.dto.response.BulkOrderResponse;
import com.krushikranti.dto.response.PaymentOrderResponse;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.*;
import com.krushikranti.repository.BulkOrderRepository;
import com.krushikranti.repository.DealOfferRepository;
import com.krushikranti.repository.UserRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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

    /**
     * Create a BulkOrder and Razorpay payment order when a deal is accepted
     */
    @Transactional
    public PaymentOrderResponse initiatePayment(String wholesalerEmail, Long dealOfferId) {
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

        // Check if order already exists
        BulkOrder existingOrder = bulkOrderRepository.findByDealOffer(deal).orElse(null);
        if (existingOrder != null && existingOrder.getPaymentStatus() == BulkOrder.PaymentStatus.PAID) {
            throw new RuntimeException("Payment already completed for this deal");
        }

        BigDecimal totalAmount = deal.getTotalPrice();
        BigDecimal platformFee = totalAmount.multiply(PLATFORM_FEE_PERCENT).setScale(2, RoundingMode.HALF_UP);
        BigDecimal farmerPayout = totalAmount.multiply(FARMER_PAYOUT_PERCENT).setScale(2, RoundingMode.HALF_UP);

        BulkOrder bulkOrder = existingOrder != null ? existingOrder : BulkOrder.builder()
                .dealOffer(deal)
                .farmer(conversation.getFarmer())
                .wholesaler(wholesaler)
                .bulkProduct(conversation.getBulkProduct())
                .totalAmount(totalAmount)
                .platformFee(platformFee)
                .farmerPayout(farmerPayout)
                .build();

        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // Razorpay takes amount in paise
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
            bulkOrder.setRazorpayOrderId(razorpayOrderId);
            bulkOrderRepository.save(bulkOrder);

            log.info("Created Razorpay order {} for bulk deal {}", razorpayOrderId, dealOfferId);

            return PaymentOrderResponse.builder()
                    .id(razorpayOrderId)
                    .currency(razorpayOrder.get("currency"))
                    .amount(razorpayOrder.get("amount"))
                    .status(razorpayOrder.get("status"))
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for bulk deal", e);
            throw new RuntimeException("Error communicating with payment gateway", e);
        }
    }

    /**
     * Verify payment and process fulfillment (split payments + shipping)
     */
    @Transactional
    public BulkOrderResponse verifyAndProcessPayment(String razorpayOrderId, String razorpayPaymentId,
            String razorpaySignature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);

            boolean isSignatureValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);

            if (!isSignatureValid) {
                throw new RuntimeException("Invalid payment signature");
            }

            BulkOrder bulkOrder = bulkOrderRepository.findByRazorpayOrderId(razorpayOrderId)
                    .orElseThrow(() -> new RuntimeException("Bulk order not found for razorpay order: " + razorpayOrderId));

            bulkOrder.setRazorpayPaymentId(razorpayPaymentId);
            bulkOrder.setPaymentStatus(BulkOrder.PaymentStatus.PAID);
            bulkOrder.setOrderStatus(BulkOrder.OrderStatus.CONFIRMED);

            // Log payment split for admin visibility
            log.info("Bulk payment processed - Total: ₹{}, Platform Fee (5%): ₹{}, Farmer Payout (95%): ₹{}",
                    bulkOrder.getTotalAmount(), bulkOrder.getPlatformFee(), bulkOrder.getFarmerPayout());

            // TODO: Implement actual fund transfer to farmer's account via Razorpay Route/Transfer API
            // For now, just log and track. In production, use Razorpay Linked Accounts or manual payout.

            bulkOrderRepository.save(bulkOrder);

            // Trigger Shiprocket shipment creation
            try {
                createBulkShipment(bulkOrder);
            } catch (Exception e) {
                log.error("Failed to create shipment for bulk order: {}", bulkOrder.getId(), e);
                // Don't fail payment verification
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
        // For bulk B2B orders, we use Shiprocket's B2B shipping
        // This is a simplified version. In production, you'd need proper address details.
        
        String token = shiprocketService.authenticate();
        
        // Store tracking info
        bulkOrder.setDeliveryStatus(BulkOrder.DeliveryStatus.PICKUP_SCHEDULED);
        bulkOrder.setOrderStatus(BulkOrder.OrderStatus.PROCESSING);
        
        // Generate a tracking reference (in production, this comes from Shiprocket API response)
        bulkOrder.setTrackingUrl("https://shiprocket.co/tracking/" + bulkOrder.getId());
        
        bulkOrderRepository.save(bulkOrder);
        log.info("Bulk shipment scheduled for order: {}", bulkOrder.getId());
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
     * Get a specific bulk order
     */
    public BulkOrderResponse getOrder(String userEmail, Long orderId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        BulkOrder order = bulkOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("BulkOrder", "id", orderId));

        // Check access
        boolean hasAccess = user.getRole() == User.Role.ROLE_ADMIN
                || order.getFarmer().getId().equals(user.getId())
                || order.getWholesaler().getId().equals(user.getId());

        if (!hasAccess) {
            throw new RuntimeException("You do not have access to this order");
        }

        return BulkOrderResponse.fromEntity(order);
    }
}
