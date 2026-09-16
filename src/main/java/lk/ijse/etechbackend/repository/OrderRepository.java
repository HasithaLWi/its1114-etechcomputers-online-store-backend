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
}
