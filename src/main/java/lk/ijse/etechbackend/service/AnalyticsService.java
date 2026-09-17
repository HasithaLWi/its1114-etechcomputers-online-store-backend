package lk.ijse.etechbackend.service;

import lk.ijse.etechbackend.dto.analytics.*;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsService {
    AnalyticsOverviewDTO getOverview();
    List<BranchRevenueDTO> getBranchRevenue();
    List<TopProductDTO> getTopProducts(int limit);

    AnalyticsSummaryDTO getSummary(LocalDateTime from, LocalDateTime to, String branchId);
    List<SalesTrendDTO> getSalesTrends(LocalDateTime from, LocalDateTime to, String branchId);
    List<BranchPerformanceDTO> getBranchPerformance(LocalDateTime from, LocalDateTime to);
    List<CategoryPerformanceDTO> getCategoryPerformance(LocalDateTime from, LocalDateTime to, String branchId);
    List<TopProductDTO> getTopProductsFiltered(LocalDateTime from, LocalDateTime to, String branchId, int limit);
    InventoryHealthDTO getInventoryHealth(String branchId);
}
