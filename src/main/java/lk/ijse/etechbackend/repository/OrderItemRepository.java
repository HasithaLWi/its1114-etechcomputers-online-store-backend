package lk.ijse.etechbackend.repository;

import lk.ijse.etechbackend.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByProductId(Long productId);

    @Query("SELECT oi.product.id, oi.productName, oi.productSku, " +
           "COALESCE(c.name, 'General Hardware'), " +
           "SUM(oi.quantity), " +
           "SUM(oi.totalPrice) " +
           "FROM OrderItem oi " +
           "JOIN oi.order o " +
           "LEFT JOIN oi.product p " +
           "LEFT JOIN p.category c " +
           "WHERE o.status != lk.ijse.etechbackend.enumiration.OrderStatus.Cancelled " +
           "AND (:from IS NULL OR o.orderDate >= :from) " +
           "AND (:to IS NULL OR o.orderDate <= :to) " +
           "AND (:branchId IS NULL OR :branchId = 'ALL' OR o.fulfillmentBranch.id = :branchId) " +
           "GROUP BY oi.product.id, oi.productName, oi.productSku, c.name " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingProducts(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("branchId") String branchId,
            Pageable pageable);

    @Query("SELECT COALESCE(c.id, 'uncat'), " +
           "COALESCE(c.name, 'Uncategorized'), " +
           "SUM(oi.quantity), " +
           "SUM(oi.totalPrice) " +
           "FROM OrderItem oi " +
           "JOIN oi.order o " +
           "LEFT JOIN oi.product p " +
           "LEFT JOIN p.category c " +
           "WHERE o.status != lk.ijse.etechbackend.enumiration.OrderStatus.Cancelled " +
           "AND (:from IS NULL OR o.orderDate >= :from) " +
           "AND (:to IS NULL OR o.orderDate <= :to) " +
           "AND (:branchId IS NULL OR :branchId = 'ALL' OR o.fulfillmentBranch.id = :branchId) " +
           "GROUP BY c.id, c.name " +
           "ORDER BY SUM(oi.totalPrice) DESC")
    List<Object[]> findCategoryPerformance(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("branchId") String branchId);
}
