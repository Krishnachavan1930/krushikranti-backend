package com.krushikranti.service;

import com.krushikranti.dto.response.PaymentOrderResponse;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.Order;
import com.krushikranti.repository.OrderRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Transactional
    public PaymentOrderResponse createPaymentOrder(Long internalOrderId) {
        Order dbOrder = orderRepository.findById(internalOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", internalOrderId));

        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // Razorpay takes amount in paise
            int amountInPaise = dbOrder.getTotalPrice().multiply(new BigDecimal("100")).intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + internalOrderId);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            String razorpayOrderId = razorpayOrder.get("id");
            dbOrder.setRazorpayOrderId(razorpayOrderId);
            orderRepository.save(dbOrder);

            return PaymentOrderResponse.builder()
                    .id(razorpayOrderId)
                    .currency(razorpayOrder.get("currency"))
                    .amount(razorpayOrder.get("amount"))
                    .status(razorpayOrder.get("status"))
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order", e);
            throw new RuntimeException("Error communicating with payment gateway", e);
        }
    }

    @Transactional
    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);

            boolean isSignatureValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);

            if (isSignatureValid) {
                // Find order linked to this razorpay order
                // Note: We'd normally use a custom jpa query here, but let's assume we can
                // fetch it like this for brevity
                Order dbOrder = orderRepository.findAll().stream()
                        .filter(order -> razorpayOrderId.equals(order.getRazorpayOrderId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException(
                                "associated order not found for razorpay order: " + razorpayOrderId));

                dbOrder.setStatus(Order.OrderStatus.PAID);
                dbOrder.setRazorpayPaymentId(razorpayPaymentId);
                orderRepository.save(dbOrder);
                return true;
            } else {
                return false;
            }
        } catch (RazorpayException e) {
            log.error("Exception checking signature", e);
            return false;
        }
    }
}
