package com.krushikranti.service;

import com.krushikranti.dto.response.ShipmentResponse;
import com.krushikranti.dto.response.TrackingResponse;
import com.krushikranti.model.Order;
import com.krushikranti.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiprocketService {

    @Value("${shiprocket.email}")
    private String shiprocketEmail;

    @Value("${shiprocket.password}")
    private String shiprocketPassword;

    @Value("${shiprocket.api.base-url}")
    private String shiprocketBaseUrl;

    private final WebClient.Builder webClientBuilder;

    // Cache the token with expiry
    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> tokenExpiry = new AtomicReference<>();

    /**
     * Authenticate with Shiprocket API and get JWT token
     */
    public String authenticate() {
        // Check if we have a valid cached token
        if (cachedToken.get() != null && tokenExpiry.get() != null 
                && LocalDateTime.now().isBefore(tokenExpiry.get())) {
            return cachedToken.get();
        }

        try {
            WebClient webClient = webClientBuilder.baseUrl(shiprocketBaseUrl).build();

            Map<String, String> authRequest = new HashMap<>();
            authRequest.put("email", shiprocketEmail);
            authRequest.put("password", shiprocketPassword);

            Map<String, Object> response = webClient.post()
                    .uri("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(authRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("token")) {
                String token = (String) response.get("token");
                cachedToken.set(token);
                // Token is valid for 24 hours, refresh after 23 hours
                tokenExpiry.set(LocalDateTime.now().plusHours(23));
                log.info("Successfully authenticated with Shiprocket API");
                return token;
            }

            throw new RuntimeException("Failed to get token from Shiprocket");
        } catch (Exception e) {
            log.error("Shiprocket authentication failed", e);
            throw new RuntimeException("Shiprocket authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Create shipment after order is placed and payment is completed
     */
    public ShipmentResponse createShipment(Order order) {
        String token = authenticate();
        User customer = order.getUser();

        try {
            WebClient webClient = webClientBuilder.baseUrl(shiprocketBaseUrl).build();

            // Build the shipment request
            Map<String, Object> shipmentRequest = new HashMap<>();
            shipmentRequest.put("order_id", "KK-" + order.getId());
            shipmentRequest.put("order_date", order.getCreatedAt().toString());
            shipmentRequest.put("pickup_location", "Primary");
            shipmentRequest.put("channel_id", "");
            shipmentRequest.put("comment", "KrushiKranti Order");
            
            // Billing details
            shipmentRequest.put("billing_customer_name", customer.getFirstName());
            shipmentRequest.put("billing_last_name", customer.getLastName() != null ? customer.getLastName() : "");
            shipmentRequest.put("billing_address", order.getShippingAddress() != null ? order.getShippingAddress() : "");
            shipmentRequest.put("billing_address_2", "");
            shipmentRequest.put("billing_city", order.getShippingCity() != null ? order.getShippingCity() : "");
            shipmentRequest.put("billing_pincode", order.getShippingPincode() != null ? order.getShippingPincode() : "");
            shipmentRequest.put("billing_state", order.getShippingState() != null ? order.getShippingState() : "");
            shipmentRequest.put("billing_country", "India");
            shipmentRequest.put("billing_email", customer.getEmail());
            shipmentRequest.put("billing_phone", order.getCustomerPhone() != null ? order.getCustomerPhone() : customer.getPhone());

            // Shipping details (same as billing for now)
            shipmentRequest.put("shipping_is_billing", true);

            // Order items
            Map<String, Object> orderItem = new HashMap<>();
            orderItem.put("name", order.getProduct().getName());
            orderItem.put("sku", "PROD-" + order.getProduct().getId());
            orderItem.put("units", order.getQuantity());
            orderItem.put("selling_price", order.getProduct().getRetailPrice().doubleValue());
            shipmentRequest.put("order_items", new Map[]{orderItem});

            // Payment details
            shipmentRequest.put("payment_method", "Prepaid");
            shipmentRequest.put("sub_total", order.getTotalPrice().doubleValue());
            shipmentRequest.put("length", 10); // in cm
            shipmentRequest.put("breadth", 10);
            shipmentRequest.put("height", 10);
            shipmentRequest.put("weight", 0.5); // in kg

            Map<String, Object> response = webClient.post()
                    .uri("/orders/create/adhoc")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(shipmentRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                ShipmentResponse shipmentResponse = new ShipmentResponse();
                
                if (response.containsKey("shipment_id")) {
                    shipmentResponse.setShipmentId(String.valueOf(response.get("shipment_id")));
                }
                if (response.containsKey("order_id")) {
                    shipmentResponse.setShiprocketOrderId(String.valueOf(response.get("order_id")));
                }
                if (response.containsKey("awb_code")) {
                    shipmentResponse.setAwbCode(String.valueOf(response.get("awb_code")));
                }
                if (response.containsKey("courier_name")) {
                    shipmentResponse.setCourierName(String.valueOf(response.get("courier_name")));
                }
                shipmentResponse.setSuccess(true);
                shipmentResponse.setMessage("Shipment created successfully");
                
                log.info("Successfully created shipment for order: {}", order.getId());
                return shipmentResponse;
            }

            throw new RuntimeException("Empty response from Shiprocket");
        } catch (Exception e) {
            log.error("Failed to create shipment for order: {}", order.getId(), e);
            ShipmentResponse errorResponse = new ShipmentResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Failed to create shipment: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Get tracking information using AWB code
     */
    public TrackingResponse getTrackingInfo(String awbCode) {
        String token = authenticate();

        try {
            WebClient webClient = webClientBuilder.baseUrl(shiprocketBaseUrl).build();

            Map<String, Object> response = webClient.get()
                    .uri("/courier/track/awb/{awbCode}", awbCode)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("tracking_data")) {
                Map<String, Object> trackingData = (Map<String, Object>) response.get("tracking_data");
                
                TrackingResponse trackingResponse = new TrackingResponse();
                trackingResponse.setAwbCode(awbCode);
                trackingResponse.setSuccess(true);
                
                if (trackingData.containsKey("shipment_track")) {
                    Object shipmentTrack = trackingData.get("shipment_track");
                    if (shipmentTrack instanceof java.util.List && !((java.util.List<?>) shipmentTrack).isEmpty()) {
                        Map<String, Object> latestTrack = (Map<String, Object>) ((java.util.List<?>) shipmentTrack).get(0);
                        trackingResponse.setCurrentStatus(String.valueOf(latestTrack.get("current_status")));
                        trackingResponse.setCurrentLocation(String.valueOf(latestTrack.get("current_status_body")));
                        if (latestTrack.containsKey("edd")) {
                            trackingResponse.setEstimatedDelivery(String.valueOf(latestTrack.get("edd")));
                        }
                    }
                }
                
                if (trackingData.containsKey("shipment_track_activities")) {
                    trackingResponse.setTrackingActivities((java.util.List<Map<String, Object>>) trackingData.get("shipment_track_activities"));
                }
                
                if (trackingData.containsKey("track_url")) {
                    trackingResponse.setTrackingUrl(String.valueOf(trackingData.get("track_url")));
                }

                return trackingResponse;
            }

            TrackingResponse notFoundResponse = new TrackingResponse();
            notFoundResponse.setAwbCode(awbCode);
            notFoundResponse.setSuccess(false);
            notFoundResponse.setMessage("Tracking information not found");
            return notFoundResponse;

        } catch (Exception e) {
            log.error("Failed to get tracking info for AWB: {}", awbCode, e);
            TrackingResponse errorResponse = new TrackingResponse();
            errorResponse.setAwbCode(awbCode);
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Failed to fetch tracking info: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Request AWB assignment for a shipment
     */
    public ShipmentResponse requestAwb(String shiprocketShipmentId) {
        String token = authenticate();

        try {
            WebClient webClient = webClientBuilder.baseUrl(shiprocketBaseUrl).build();

            Map<String, Object> awbRequest = new HashMap<>();
            awbRequest.put("shipment_id", shiprocketShipmentId);

            Map<String, Object> response = webClient.post()
                    .uri("/courier/assign/awb")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(awbRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            ShipmentResponse shipmentResponse = new ShipmentResponse();
            if (response != null && response.containsKey("response")) {
                Map<String, Object> respData = (Map<String, Object>) response.get("response");
                Map<String, Object> awbData = (Map<String, Object>) respData.get("data");
                
                if (awbData != null) {
                    shipmentResponse.setAwbCode(String.valueOf(awbData.get("awb_code")));
                    shipmentResponse.setCourierName(String.valueOf(awbData.get("courier_name")));
                    shipmentResponse.setSuccess(true);
                    shipmentResponse.setMessage("AWB assigned successfully");
                }
            }
            return shipmentResponse;

        } catch (Exception e) {
            log.error("Failed to request AWB for shipment: {}", shiprocketShipmentId, e);
            ShipmentResponse errorResponse = new ShipmentResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Failed to assign AWB: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Cancel a shipment
     */
    public boolean cancelShipment(String shiprocketOrderId) {
        String token = authenticate();

        try {
            WebClient webClient = webClientBuilder.baseUrl(shiprocketBaseUrl).build();

            Map<String, Object> cancelRequest = new HashMap<>();
            cancelRequest.put("ids", new String[]{shiprocketOrderId});

            Map<String, Object> response = webClient.post()
                    .uri("/orders/cancel")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(cancelRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return response != null && !response.containsKey("error");

        } catch (Exception e) {
            log.error("Failed to cancel shipment: {}", shiprocketOrderId, e);
            return false;
        }
    }

    /**
     * Create Shiprocket shipment for bulk B2B orders
     */
    public ShipmentResponse createBulkShipment(com.krushikranti.model.BulkOrder bulkOrder, String token) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(shiprocketBaseUrl).build();

            // Build the shipment request for bulk order
            Map<String, Object> shipmentRequest = new HashMap<>();
            shipmentRequest.put("order_id", "BULK-" + bulkOrder.getId());
            shipmentRequest.put("order_date", bulkOrder.getCreatedAt().toString());
            shipmentRequest.put("pickup_location", "Primary");
            shipmentRequest.put("channel_id", "");
            shipmentRequest.put("comment", "KrushiKranti Bulk Order - B2B");
            
            // Billing and shipping details from saved address
            shipmentRequest.put("billing_customer_name", bulkOrder.getShippingName());
            shipmentRequest.put("billing_last_name", "");
            shipmentRequest.put("billing_address", bulkOrder.getShippingAddress());
            shipmentRequest.put("billing_address_2", "");
            shipmentRequest.put("billing_city", bulkOrder.getShippingCity());
            shipmentRequest.put("billing_pincode", bulkOrder.getShippingPincode());
            shipmentRequest.put("billing_state", bulkOrder.getShippingState());
            shipmentRequest.put("billing_country", "India");
            shipmentRequest.put("billing_email", bulkOrder.getWholesaler().getEmail());
            shipmentRequest.put("billing_phone", bulkOrder.getShippingPhone());

            // Same as billing
            shipmentRequest.put("shipping_is_billing", true);

            // Order items
            Map<String, Object> orderItem = new HashMap<>();
            orderItem.put("name", bulkOrder.getBulkProduct().getName());
            orderItem.put("sku", "BULK-" + bulkOrder.getBulkProduct().getId());
            orderItem.put("units", bulkOrder.getDealOffer().getQuantity());
            orderItem.put("selling_price", bulkOrder.getDealOffer().getPricePerUnit().doubleValue());
            shipmentRequest.put("order_items", new Map[]{orderItem});

            // Payment details
            shipmentRequest.put("payment_method", "Prepaid");
            shipmentRequest.put("sub_total", bulkOrder.getTotalAmount().doubleValue());
            
            // Dimensions (for bulk orders, typically larger)
            shipmentRequest.put("length", 50);
            shipmentRequest.put("breadth", 50);
            shipmentRequest.put("height", 50);
            shipmentRequest.put("weight", 10); // in kg

            Map<String, Object> response = webClient.post()
                    .uri("/orders/create/adhoc")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(shipmentRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                ShipmentResponse shipmentResponse = new ShipmentResponse();
                
                if (response.containsKey("shipment_id")) {
                    shipmentResponse.setShipmentId(String.valueOf(response.get("shipment_id")));
                }
                if (response.containsKey("order_id")) {
                    shipmentResponse.setShiprocketOrderId(String.valueOf(response.get("order_id")));
                }
                if (response.containsKey("awb_code")) {
                    shipmentResponse.setAwbCode(String.valueOf(response.get("awb_code")));
                }
                if (response.containsKey("courier_name")) {
                    shipmentResponse.setCourierName(String.valueOf(response.get("courier_name")));
                }
                
                // Set tracking URL
                shipmentResponse.setTrackingUrl("https://shiprocket.co/tracking/" + shipmentResponse.getAwbCode());
                shipmentResponse.setSuccess(true);
                shipmentResponse.setMessage("Bulk shipment created successfully");
                
                log.info("Successfully created bulk shipment for order: {} with shipment_id: {}", 
                        bulkOrder.getId(), shipmentResponse.getShipmentId());
                return shipmentResponse;
            }

            return null;
        } catch (Exception e) {
            log.error("Failed to create bulk shipment for order: {}", bulkOrder.getId(), e);
            return null;
        }
    }
}
