package lk.ijse.etechbackend.repository;

import lk.ijse.etechbackend.entity.Order;
import lk.ijse.etechbackend.enumiration.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByFulfillmentBranchId(String branchId);

    Page<Order> findAllByOrderByOrderDateDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status != 'Cancelled'")
    BigDecimal calculateGrossRevenue();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status != 'Cancelled'")
    Long countValidOrders();

    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:branchId IS NULL OR o.fulfillmentBranch.id = :branchId) AND " +
           "(:search IS NULL OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(o.customerEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(o.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> filterOrdersPaged(
            @Param("status") OrderStatus status,
            @Param("branchId") String branchId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE " +
           "(:from IS NULL OR o.orderDate >= :from) AND " +
           "(:to IS NULL OR o.orderDate <= :to) AND " +
           "(:branchId IS NULL OR :branchId = 'ALL' OR o.fulfillmentBranch.id = :branchId)")
    BigDecimal calculateFilteredGrossRevenue(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("branchId") String branchId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE " +
           "o.status != lk.ijse.etechbackend.enumiration.OrderStatus.Cancelled AND " +
           "(:from IS NULL OR o.orderDate >= :from) AND " +
           "(:to IS NULL OR o.orderDate <= :to) AND " +
           "(:branchId IS NULL OR :branchId = 'ALL' OR o.fulfillmentBranch.id = :branchId)")
    BigDecimal calculateFilteredNetRevenue(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("branchId") String branchId);

    @Query("SELECT COUNT(o) FROM Order o WHERE " +
           "(:from IS NULL OR o.orderDate >= :from) AND " +
           "(:to IS NULL OR o.orderDate <= :to) AND " +
           "(:branchId IS NULL OR :branchId = 'ALL' OR o.fulfillmentBranch.id = :branchId)")
    Long countFilteredOrders(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("branchId") String branchId);

    @Query("SELECT COUNT(o) FROM Order o WHERE " +
           "(:from IS NULL OR o.orderDate >= :from) AND " +
           "(:to IS NULL OR o.orderDate <= :to) AND " +
           "(:branchId IS NULL OR :branchId = 'ALL' OR o.fulfillmentBranch.id = :branchId) AND " +
           "o.status = :status")
    Long countFilteredOrdersByStatus(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("branchId") String branchId,
            @Param("status") OrderStatus status);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE " +
           "oi.order.status != lk.ijse.etechbackend.enumiration.OrderStatus.Cancelled AND " +
           "(:from IS NULL OR oi.order.orderDate >= :from) AND " +
           "(:to IS NULL OR oi.order.orderDate <= :to) AND " +
           "(:branchId IS NULL OR :branchId = 'ALL' OR oi.order.fulfillmentBranch.id = :branchId)")
    Long calculateTotalUnitsSold(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("branchId") String branchId);

    @Query(value = "SELECT DATE_FORMAT(o.order_date, '%Y-%m-%d') as orderDate, " +
                   "COALESCE(SUM(CASE WHEN o.status != 'Cancelled' THEN o.total_amount ELSE 0 END), 0) as dailyRevenue, " +
                   "COUNT(o.id) as dailyOrders " +
                   "FROM orders o " +
                   "WHERE (:from IS NULL OR o.order_date >= :from) " +
                   "AND (:to IS NULL OR o.order_date <= :to) " +
                   "AND (:branchId IS NULL OR :branchId = 'ALL' OR o.fulfillment_branch_id = :branchId) " +
                   "GROUP BY DATE_FORMAT(o.order_date, '%Y-%m-%d') " +
                   "ORDER BY orderDate ASC", nativeQuery = true)
    List<Object[]> findDailySalesTrend(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("branchId") String branchId);

    @Query("SELECT b.id, b.name, b.city, " +
           "COUNT(o), " +
           "SUM(CASE WHEN o.status = lk.ijse.etechbackend.enumiration.OrderStatus.Delivered THEN 1 ELSE 0 END), " +
           "COALESCE(SUM(CASE WHEN o.status != lk.ijse.etechbackend.enumiration.OrderStatus.Cancelled THEN o.totalAmount ELSE 0 END), 0) " +
           "FROM Branch b " +
           "LEFT JOIN Order o ON o.fulfillmentBranch.id = b.id AND " +
           "(:from IS NULL OR o.orderDate >= :from) AND " +
           "(:to IS NULL OR o.orderDate <= :to) " +
           "GROUP BY b.id, b.name, b.city")
    List<Object[]> findBranchPerformance(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
