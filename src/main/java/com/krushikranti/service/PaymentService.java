package com.krushikranti.service;

import com.krushikranti.dto.request.CheckoutPaymentInitiateRequest;
import com.krushikranti.dto.request.CheckoutPaymentItemRequest;
import com.krushikranti.dto.response.PaymentOrderResponse;
import com.krushikranti.dto.response.PaymentVerificationResponse;
import com.krushikranti.dto.response.ShipmentResponse;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.Order;
import com.krushikranti.model.Product;
import com.krushikranti.model.User;
import com.krushikranti.repository.OrderRepository;
import com.krushikranti.repository.ProductRepository;
import com.krushikranti.repository.UserRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.krushikranti.util.RazorpayUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ShiprocketService shiprocketService;
    private final NotificationService notificationService;
    private final InvoiceService invoiceService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${app.commission.admin-percentage:0.10}")
    private BigDecimal adminCommissionPercentage;

    @Value("${app.commission.farmer-percentage:0.90}")
    private BigDecimal farmerAmountPercentage;

    private final ConcurrentHashMap<String, PendingCheckoutData> pendingPayments = new ConcurrentHashMap<>();

    private record PendingCheckoutData(User buyer, CheckoutPaymentInitiateRequest request) {
    }

    @Transactional
    public PaymentOrderResponse createPaymentOrder(String userEmail, CheckoutPaymentInitiateRequest request) {
        User buyer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one checkout item is required");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CheckoutPaymentItemRequest item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", item.getProductId()));

            if (product.getQuantity() < item.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }

            BigDecimal itemTotal = product.getRetailPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            int amountInPaise = totalAmount.multiply(new BigDecimal("100")).intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + buyer.getId() + "_" + System.currentTimeMillis());

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            String razorpayOrderId = razorpayOrder.get("id");
            pendingPayments.put(razorpayOrderId, new PendingCheckoutData(buyer, request));

            return new PaymentOrderResponse(
                    razorpayOrderId,
                    razorpayOrder.get("currency"),
                    razorpayOrder.get("amount"),
                    razorpayOrder.get("status")
            );

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order", e);
            throw new RuntimeException("Error communicating with payment gateway", e);
        }
    }

    @Transactional
    public PaymentVerificationResponse verifyPaymentSignature(String userEmail, String razorpayOrderId,
            String razorpayPaymentId, String razorpaySignature) {
        try {
            boolean isSignatureValid = RazorpayUtils.verifySignature(
                    razorpayOrderId,
                    razorpayPaymentId,
                    razorpaySignature,
                    razorpayKeySecret);

            if (!isSignatureValid) {
                throw new IllegalArgumentException("Payment verification failed");
            }

            List<Order> existingOrders = orderRepository.findAllByRazorpayOrderIdOrderByIdAsc(razorpayOrderId);
            if (!existingOrders.isEmpty()) {
                return PaymentVerificationResponse.builder()
                        .verified(true)
                        .primaryOrderId(existingOrders.get(0).getId())
                        .orderIds(existingOrders.stream().map(Order::getId).toList())
                        .build();
            }

            PendingCheckoutData pendingData = pendingPayments.get(razorpayOrderId);
            if (pendingData == null) {
                throw new IllegalArgumentException("No pending checkout found for the provided Razorpay order");
            }

            if (!pendingData.buyer().getEmail().equals(userEmail)) {
                throw new IllegalArgumentException("You do not have access to verify this payment");
            }

            List<Order> createdOrders = new ArrayList<>();
            for (CheckoutPaymentItemRequest item : pendingData.request().getItems()) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product", "id", item.getProductId()));

                if (product.getQuantity() < item.getQuantity()) {
                    throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
                }

                BigDecimal totalPrice = product.getRetailPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                BigDecimal adminCommission = totalPrice.multiply(adminCommissionPercentage)
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal farmerAmount = totalPrice.multiply(farmerAmountPercentage)
                        .setScale(2, RoundingMode.HALF_UP);

                Order order = Order.builder()
                        .orderNumber("KK-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .user(pendingData.buyer())
                        .buyer(pendingData.buyer())
                        .product(product)
                        .quantity(item.getQuantity())
                        .totalAmount(totalPrice)
                        .totalPrice(totalPrice)
                        .status(Order.OrderStatus.CONFIRMED)
                        .adminCommission(adminCommission)
                        .farmerAmount(farmerAmount)
                        .deliveryStatus(Order.DeliveryStatus.PENDING)
                        .shippingAddress(pendingData.request().getShippingAddress())
                        .shippingCity(pendingData.request().getShippingCity())
                        .shippingState(pendingData.request().getShippingState())
                        .shippingPincode(pendingData.request().getShippingPincode())
                        .customerPhone(pendingData.request().getCustomerPhone())
                        .razorpayOrderId(razorpayOrderId)
                        .razorpayPaymentId(razorpayPaymentId)
                        .build();

                product.setQuantity(product.getQuantity() - item.getQuantity());
                productRepository.save(product);

                Order savedOrder = orderRepository.save(order);
                createdOrders.add(savedOrder);

                try {
                    notificationService.notifyNewOrder(savedOrder);
                    notificationService.notifyOrderStatusChange(savedOrder, "PENDING", "CONFIRMED");
                } catch (Exception e) {
                    log.error("Failed to send payment notification for order: {}", savedOrder.getId(), e);
                }

                try {
                    ShipmentResponse shipmentResponse = shiprocketService.createShipment(savedOrder);
                    if (shipmentResponse.isSuccess()) {
                        savedOrder.setShipmentId(shipmentResponse.getShipmentId());
                        savedOrder.setAwbCode(shipmentResponse.getAwbCode());
                        savedOrder.setCourierName(shipmentResponse.getCourierName());
                        savedOrder.setDeliveryStatus(Order.DeliveryStatus.PICKUP_SCHEDULED);
                        savedOrder.setTrackingStatus("Shipment Created");
                        orderRepository.save(savedOrder);
                    }
                } catch (Exception e) {
                    log.error("Failed to create shipment for order: {}", savedOrder.getId(), e);
                }

                try {
                    invoiceService.generateInvoiceForOrder(savedOrder.getId());
                } catch (Exception e) {
                    log.error("Failed to generate invoice for order: {}", savedOrder.getId(), e);
                }
            }

            pendingPayments.remove(razorpayOrderId);

            return PaymentVerificationResponse.builder()
                    .verified(true)
                    .primaryOrderId(createdOrders.get(0).getId())
                    .orderIds(createdOrders.stream().map(Order::getId).toList())
                    .build();
        } catch (RazorpayException e) {
            log.error("Exception checking signature", e);
            throw new RuntimeException("Payment verification failed", e);
        }
    }
}
