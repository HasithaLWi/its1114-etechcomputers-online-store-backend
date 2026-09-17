package lk.ijse.etechbackend.service.impl;

import lk.ijse.etechbackend.dto.analytics.*;
import lk.ijse.etechbackend.enumiration.OrderStatus;
import lk.ijse.etechbackend.repository.*;
import lk.ijse.etechbackend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BranchInventoryRepository branchInventoryRepository;

    @Override
    public AnalyticsOverviewDTO getOverview() {
        log.info("Calculating executive financial overview");
        BigDecimal grossRevenue = orderRepository.calculateGrossRevenue();
        if (grossRevenue == null) grossRevenue = BigDecimal.ZERO;

        Long totalOrdersCount = orderRepository.countValidOrders();
        long totalOrders = totalOrdersCount != null ? totalOrdersCount : 0L;

        BigDecimal avgOrderValue = totalOrders > 0
                ? grossRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long activeUsers = userRepository.count();

        return AnalyticsOverviewDTO.builder()
                .grossRevenue(grossRevenue)
                .totalOrders(totalOrders)
                .avgOrderValue(avgOrderValue)
                .activeUsers(activeUsers)
                .build();
    }

    @Override
    public List<BranchRevenueDTO> getBranchRevenue() {
        log.info("Calculating branch revenue summary");
        List<BranchPerformanceDTO> performances = getBranchPerformance(null, null);
        List<BranchRevenueDTO> results = new ArrayList<>();
        for (BranchPerformanceDTO bp : performances) {
            results.add(BranchRevenueDTO.builder()
                    .branchId(bp.getBranchId())
                    .branchName(bp.getBranchName())
                    .orderCount(bp.getOrderCount())
                    .revenue(bp.getRevenue())
                    .percentage(bp.getPercentage())
                    .build());
        }
        return results;
    }

    @Override
    public List<TopProductDTO> getTopProducts(int limit) {
        return getTopProductsFiltered(null, null, null, limit);
    }

    @Override
    public AnalyticsSummaryDTO getSummary(LocalDateTime from, LocalDateTime to, String branchId) {
        log.info("SQL Aggregating analytics summary: from={}, to={}, branchId={}", from, to, branchId);

        BigDecimal grossRevenue = orderRepository.calculateFilteredGrossRevenue(from, to, branchId);
        if (grossRevenue == null) grossRevenue = BigDecimal.ZERO;

        BigDecimal netRevenue = orderRepository.calculateFilteredNetRevenue(from, to, branchId);
        if (netRevenue == null) netRevenue = BigDecimal.ZERO;

        Long totalOrdersVal = orderRepository.countFilteredOrders(from, to, branchId);
        long totalOrders = totalOrdersVal != null ? totalOrdersVal : 0L;

        Long completedVal = orderRepository.countFilteredOrdersByStatus(from, to, branchId, lk.ijse.etechbackend.enumiration.OrderStatus.Delivered);
        long completedOrders = completedVal != null ? completedVal : 0L;

        Long pendingVal = orderRepository.countFilteredOrdersByStatus(from, to, branchId, lk.ijse.etechbackend.enumiration.OrderStatus.Pending);
        long pendingOrders = pendingVal != null ? pendingVal : 0L;

        Long cancelledVal = orderRepository.countFilteredOrdersByStatus(from, to, branchId, lk.ijse.etechbackend.enumiration.OrderStatus.Cancelled);
        long cancelledOrders = cancelledVal != null ? cancelledVal : 0L;

        Long unitsSoldVal = orderRepository.calculateTotalUnitsSold(from, to, branchId);
        long totalUnitsSold = unitsSoldVal != null ? unitsSoldVal : 0L;

        long nonCancelled = totalOrders - cancelledOrders;
        BigDecimal avgOrderValue = nonCancelled > 0
                ? netRevenue.divide(BigDecimal.valueOf(nonCancelled), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double fulfillmentRate = nonCancelled > 0
                ? (double) Math.round(((double) completedOrders / nonCancelled) * 1000.0) / 10.0
                : 0.0;

        long activeUsers = userRepository.count();

        return AnalyticsSummaryDTO.builder()
                .grossRevenue(grossRevenue)
                .netRevenue(netRevenue)
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .pendingOrders(pendingOrders)
                .cancelledOrders(cancelledOrders)
                .avgOrderValue(avgOrderValue)
                .totalUnitsSold(totalUnitsSold)
                .fulfillmentRate(fulfillmentRate)
                .activeUsers(activeUsers)
                .build();
    }

    @Override
    public List<SalesTrendDTO> getSalesTrends(LocalDateTime from, LocalDateTime to, String branchId) {
        log.info("Fetching daily sales trend: from={}, to={}, branchId={}", from, to, branchId);
        List<Object[]> rows = orderRepository.findDailySalesTrend(from, to, branchId);
        List<SalesTrendDTO> trends = new ArrayList<>();

        for (Object[] row : rows) {
            String dateStr = String.valueOf(row[0]);
            BigDecimal rev = row[1] instanceof BigDecimal
                    ? (BigDecimal) row[1]
                    : new BigDecimal(String.valueOf(row[1]));
            long ordersCount = ((Number) row[2]).longValue();

            trends.add(SalesTrendDTO.builder()
                    .date(dateStr)
                    .revenue(rev)
                    .orderCount(ordersCount)
                    .build());
        }
        return trends;
    }

    @Override
    public List<BranchPerformanceDTO> getBranchPerformance(LocalDateTime from, LocalDateTime to) {
        log.info("Fetching multi-branch performance matrix: from={}, to={}", from, to);
        List<Object[]> rows = orderRepository.findBranchPerformance(from, to);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        List<BranchPerformanceDTO> list = new ArrayList<>();

        for (Object[] row : rows) {
            String bid = (String) row[0];
            String bname = (String) row[1];
            String city = (String) row[2];
            long count = ((Number) row[3]).longValue();
            long completed = ((Number) row[4]).longValue();
            BigDecimal rev = row[5] instanceof BigDecimal
                    ? (BigDecimal) row[5]
                    : new BigDecimal(String.valueOf(row[5]));

            totalRevenue = totalRevenue.add(rev);

            double rate = count > 0 ? (double) Math.round(((double) completed / count) * 1000.0) / 10.0 : 0.0;

            list.add(BranchPerformanceDTO.builder()
                    .branchId(bid)
                    .branchName(bname)
                    .city(city != null ? city : "Main Hub")
                    .orderCount(count)
                    .completedOrders(completed)
                    .revenue(rev)
                    .percentage(BigDecimal.ZERO)
                    .fulfillmentRate(rate)
                    .build());
        }

        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            for (BranchPerformanceDTO bp : list) {
                BigDecimal pct = bp.getRevenue().multiply(BigDecimal.valueOf(100))
                        .divide(totalRevenue, 1, RoundingMode.HALF_UP);
                bp.setPercentage(pct);
            }
        }

        return list;
    }

    @Override
    public List<CategoryPerformanceDTO> getCategoryPerformance(LocalDateTime from, LocalDateTime to, String branchId) {
        log.info("Fetching category performance breakdown: from={}, to={}, branchId={}", from, to, branchId);
        List<Object[]> rows = orderItemRepository.findCategoryPerformance(from, to, branchId);

        BigDecimal totalCategoryRev = BigDecimal.ZERO;
        List<CategoryPerformanceDTO> list = new ArrayList<>();

        for (Object[] row : rows) {
            String catId = String.valueOf(row[0]);
            String catName = String.valueOf(row[1]);
            long units = ((Number) row[2]).longValue();
            BigDecimal rev = row[3] instanceof BigDecimal
                    ? (BigDecimal) row[3]
                    : new BigDecimal(String.valueOf(row[3]));

            totalCategoryRev = totalCategoryRev.add(rev);

            list.add(CategoryPerformanceDTO.builder()
                    .categoryId(catId)
                    .categoryName(catName)
                    .unitsSold(units)
                    .revenue(rev)
                    .percentage(BigDecimal.ZERO)
                    .build());
        }

        if (totalCategoryRev.compareTo(BigDecimal.ZERO) > 0) {
            for (CategoryPerformanceDTO cp : list) {
                BigDecimal pct = cp.getRevenue().multiply(BigDecimal.valueOf(100))
                        .divide(totalCategoryRev, 1, RoundingMode.HALF_UP);
                cp.setPercentage(pct);
            }
        }

        return list;
    }

    @Override
    public List<TopProductDTO> getTopProductsFiltered(LocalDateTime from, LocalDateTime to, String branchId, int limit) {
        int take = limit > 0 ? limit : 10;
        log.info("Fetching top selling hardware products (limit={})", take);
        List<Object[]> rows = orderItemRepository.findTopSellingProducts(from, to, branchId, PageRequest.of(0, take));

        List<TopProductDTO> list = new ArrayList<>();
        for (Object[] row : rows) {
            Long pid = ((Number) row[0]).longValue();
            String name = (String) row[1];
            String sku = (String) row[2];
            String catName = (String) row[3];
            long units = ((Number) row[4]).longValue();
            BigDecimal rev = row[5] instanceof BigDecimal
                    ? (BigDecimal) row[5]
                    : new BigDecimal(String.valueOf(row[5]));

            list.add(TopProductDTO.builder()
                    .productId(pid)
                    .name(name)
                    .sku(sku)
                    .categoryName(catName)
                    .unitsSold(units)
                    .revenue(rev)
                    .build());
        }
        return list;
    }

    @Override
    public InventoryHealthDTO getInventoryHealth(String branchId) {
        log.info("Fetching inventory health metrics: branchId={}", branchId);
        long totalProducts = productRepository.count();
        Long totalUnitsVal = branchInventoryRepository.calculateTotalUnitsInStock(branchId);
        long totalUnits = totalUnitsVal != null ? totalUnitsVal : 0L;

        Long lowStockVal = branchInventoryRepository.countLowStockItems(branchId);
        long lowStock = lowStockVal != null ? lowStockVal : 0L;

        Long outOfStockVal = branchInventoryRepository.countOutOfStockItems(branchId);
        long outOfStock = outOfStockVal != null ? outOfStockVal : 0L;

        List<Object[]> branchRows = branchInventoryRepository.findBranchStockSummaries();
        List<InventoryHealthDTO.BranchStockSummaryDTO> branchSummaries = new ArrayList<>();

        for (Object[] row : branchRows) {
            String bid = (String) row[0];
            String bname = (String) row[1];
            long units = ((Number) row[2]).longValue();
            long lowCount = ((Number) row[3]).longValue();
            long outCount = ((Number) row[4]).longValue();

            branchSummaries.add(InventoryHealthDTO.BranchStockSummaryDTO.builder()
                    .branchId(bid)
                    .branchName(bname)
                    .totalUnits(units)
                    .lowStockItems(lowCount)
                    .outOfStockItems(outCount)
                    .build());
        }

        return InventoryHealthDTO.builder()
                .totalCatalogProducts(totalProducts)
                .totalUnitsInStock(totalUnits)
                .lowStockCount(lowStock)
                .outOfStockCount(outOfStock)
                .branchSummaries(branchSummaries)
                .build();
    }
}
