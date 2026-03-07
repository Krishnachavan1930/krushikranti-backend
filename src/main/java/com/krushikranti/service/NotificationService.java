package com.krushikranti.service;

import com.krushikranti.dto.response.NotificationResponse;
import com.krushikranti.dto.request.OrderNotificationEvent;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.Notification;
import com.krushikranti.model.Order;
import com.krushikranti.model.User;
import com.krushikranti.repository.NotificationRepository;
import com.krushikranti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create a notification and optionally send via WebSocket
     */
    @Transactional
    public Notification createNotification(User user, String title, String message, 
                                           Notification.NotificationType type, 
                                           Long referenceId, String referenceType) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        
        // Send via WebSocket to user-specific topic
        sendWebSocketNotification(user.getId(), NotificationResponse.fromEntity(saved));
        
        return saved;
    }

    /**
     * Send order notification to farmer and admin
     */
    @Transactional
    public void notifyNewOrder(Order order) {
        // Notify farmer
        User farmer = order.getProduct().getFarmer();
        String farmerMessage = String.format(
            "New Order Received\nProduct: %s\nCustomer: %s\nQuantity: %d\nAmount: ₹%.2f",
            order.getProduct().getName(),
            order.getUser().getFirstName() + " " + order.getUser().getLastName(),
            order.getQuantity(),
            order.getTotalPrice()
        );
        
        createNotification(farmer, "New Order Received", farmerMessage, 
                Notification.NotificationType.ORDER_PLACED, order.getId(), "ORDER");

        // Send broadcast to farmer dashboard topic
        OrderNotificationEvent event = OrderNotificationEvent.builder()
                .orderId(order.getId())
                .productName(order.getProduct().getName())
                .customerName(order.getUser().getFirstName() + " " + order.getUser().getLastName())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalPrice())
                .timestamp(LocalDateTime.now())
                .build();
        
        messagingTemplate.convertAndSend("/topic/orders/farmer/" + farmer.getId(), event);
        messagingTemplate.convertAndSend("/topic/orders", event);
        
        log.info("Sent order notification for order {} to farmer {}", order.getId(), farmer.getId());
    }

    /**
     * Notify about order status change
     */
    @Transactional
    public void notifyOrderStatusChange(Order order, String oldStatus, String newStatus) {
        // Notify customer
        User customer = order.getUser();
        String statusMessage = String.format(
            "Your order #%d has been updated\nStatus: %s → %s\nProduct: %s",
            order.getId(),
            oldStatus,
            newStatus,
            order.getProduct().getName()
        );
        
        Notification.NotificationType type = mapStatusToNotificationType(newStatus);
        createNotification(customer, "Order Status Updated", statusMessage, type, order.getId(), "ORDER");
    }

    /**
     * Notify about shipment update
     */
    @Transactional
    public void notifyShipmentUpdate(Order order, String trackingStatus) {
        User customer = order.getUser();
        String message = String.format(
            "Shipment Update for Order #%d\nStatus: %s\nTracking Number: %s\nCourier: %s",
            order.getId(),
            trackingStatus,
            order.getAwbCode() != null ? order.getAwbCode() : "N/A",
            order.getCourierName() != null ? order.getCourierName() : "N/A"
        );
        
        createNotification(customer, "Shipment Update", message, 
                Notification.NotificationType.ORDER_SHIPPED, order.getId(), "ORDER");
    }

    /**
     * Get notifications for a user
     */
    public List<NotificationResponse> getUserNotifications(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(NotificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get paginated notifications
     */
    public Page<NotificationResponse> getUserNotificationsPaginated(String userEmail, int page, int size) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(NotificationResponse::fromEntity);
    }

    /**
     * Get unread notifications count
     */
    public long getUnreadCount(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(String userEmail, Long notificationId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        notificationRepository.markAsRead(notificationId, user);
    }

    /**
     * Mark all notifications as read
     */
    @Transactional
    public void markAllAsRead(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        notificationRepository.markAllAsReadForUser(user);
    }

    /**
     * Send notification via WebSocket
     */
    private void sendWebSocketNotification(Long userId, NotificationResponse notification) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + userId, notification);
            log.debug("Sent WebSocket notification to user {}", userId);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user {}", userId, e);
        }
    }

    private Notification.NotificationType mapStatusToNotificationType(String status) {
        return switch (status.toUpperCase()) {
            case "CONFIRMED" -> Notification.NotificationType.ORDER_CONFIRMED;
            case "SHIPPED" -> Notification.NotificationType.ORDER_SHIPPED;
            case "DELIVERED" -> Notification.NotificationType.ORDER_DELIVERED;
            case "CANCELLED" -> Notification.NotificationType.ORDER_CANCELLED;
            default -> Notification.NotificationType.SYSTEM;
        };
    }
}
