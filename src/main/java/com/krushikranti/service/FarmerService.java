package com.krushikranti.service;

import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.BulkOrder;
import com.krushikranti.model.User;
import com.krushikranti.repository.BulkOrderRepository;
import com.krushikranti.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FarmerService {

    private final BulkOrderRepository bulkOrderRepository;
    private final UserRepository userRepository;

    /**
     * Returns dashboard analytics stats for the authenticated farmer.
     * All figures are derived from bulk_orders associated with the farmer.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats(String farmerEmail) {
        User farmer = userRepository.findByEmail(farmerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", farmerEmail));

        // Total revenue = SUM(farmer_payout) for PAID orders
        BigDecimal totalRevenue = bulkOrderRepository.sumFarmerPayoutByStatus(
                farmer, BulkOrder.PaymentStatus.PAID);

        // Total orders regardless of status
        Long totalOrders = bulkOrderRepository.countTotalByFarmer(farmer);

        // Completed orders = orderStatus = DELIVERED
        Long completedOrders = bulkOrderRepository.countByFarmerAndOrderStatus(
                farmer, BulkOrder.OrderStatus.DELIVERED);

        // Active orders = not DELIVERED and not CANCELLED
        List<BulkOrder.OrderStatus> excludedStatuses = Arrays.asList(
                BulkOrder.OrderStatus.DELIVERED, BulkOrder.OrderStatus.CANCELLED);
        Long activeOrders = bulkOrderRepository.countByFarmerExcludingStatuses(farmer, excludedStatuses);

        // Top selling product by number of PAID orders
        List<String> topProductNames = bulkOrderRepository.findTopProductNamesByFarmer(
                farmer, BulkOrder.PaymentStatus.PAID, PageRequest.of(0, 1));
        String topSellingProduct = topProductNames.isEmpty() ? "-" : topProductNames.get(0);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        stats.put("totalOrders", totalOrders != null ? totalOrders : 0L);
        stats.put("completedOrders", completedOrders != null ? completedOrders : 0L);
        stats.put("activeOrders", activeOrders != null ? activeOrders : 0L);
        stats.put("topSellingProduct", topSellingProduct);

        log.debug("Farmer {} dashboard stats: revenue={}, orders={}, completed={}, active={}, topProduct={}",
                farmerEmail, stats.get("totalRevenue"), totalOrders, completedOrders, activeOrders, topSellingProduct);

        return stats;
    }
}
