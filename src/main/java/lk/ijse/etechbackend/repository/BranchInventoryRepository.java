package lk.ijse.etechbackend.repository;

import lk.ijse.etechbackend.entity.BranchInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchInventoryRepository extends JpaRepository<BranchInventory, Long> {

    List<BranchInventory> findByProductId(Long productId);

    List<BranchInventory> findByBranchId(String branchId);

    Optional<BranchInventory> findByProductIdAndBranchId(Long productId, String branchId);

    @Query("SELECT SUM(bi.quantity) FROM BranchInventory bi WHERE bi.product.id = :productId")
    Integer calculateTotalStockForProduct(@Param("productId") Long productId);

    void deleteByProductId(Long productId);

    @Query("SELECT COALESCE(SUM(bi.quantity), 0) FROM BranchInventory bi WHERE (:branchId IS NULL OR :branchId = 'ALL' OR bi.branch.id = :branchId)")
    Long calculateTotalUnitsInStock(@Param("branchId") String branchId);

    @Query("SELECT COUNT(DISTINCT bi.product.id) FROM BranchInventory bi WHERE bi.quantity <= bi.product.lowStockMargin AND bi.quantity > 0 AND (:branchId IS NULL OR :branchId = 'ALL' OR bi.branch.id = :branchId)")
    Long countLowStockItems(@Param("branchId") String branchId);

    @Query("SELECT COUNT(DISTINCT bi.product.id) FROM BranchInventory bi WHERE bi.quantity = 0 AND (:branchId IS NULL OR :branchId = 'ALL' OR bi.branch.id = :branchId)")
    Long countOutOfStockItems(@Param("branchId") String branchId);

    @Query("SELECT b.id, b.name, " +
           "COALESCE(SUM(bi.quantity), 0), " +
           "SUM(CASE WHEN bi.quantity <= p.lowStockMargin AND bi.quantity > 0 THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN bi.quantity = 0 THEN 1 ELSE 0 END) " +
           "FROM Branch b " +
           "LEFT JOIN BranchInventory bi ON bi.branch.id = b.id " +
           "LEFT JOIN bi.product p " +
           "GROUP BY b.id, b.name")
    List<Object[]> findBranchStockSummaries();
}
