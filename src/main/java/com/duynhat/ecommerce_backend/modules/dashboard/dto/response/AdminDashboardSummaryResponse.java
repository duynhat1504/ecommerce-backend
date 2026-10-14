package com.duynhat.ecommerce_backend.modules.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardSummaryResponse {

    private long totalProducts;
    private long activeProducts;
    private long lowStockProducts;
    private long totalUsers;
    private long activeUsers;
    private long totalOrders;
    private long pendingOrders;
    private long confirmedOrders;
    private long shippingOrders;
    private long completedOrders;
    private long cancelledOrders;
    private long expiredOrders;
    private long todayOrders;
    private BigDecimal todayRevenue;
    private List<DashboardRecentOrderResponse> recentOrders;
    private List<DashboardLowStockProductResponse> lowStockItems;
}
