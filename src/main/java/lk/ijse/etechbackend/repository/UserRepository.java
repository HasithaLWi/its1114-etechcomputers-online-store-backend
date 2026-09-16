package lk.ijse.etechbackend.repository;

import lk.ijse.etechbackend.dto.UserDTO;
import lk.ijse.etechbackend.entity.User;
import lk.ijse.etechbackend.enumiration.Status;
import lk.ijse.etechbackend.enumiration.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

        Optional<User> findByUsername(String username);

        Optional<User> findByEmail(String email);

        boolean existsByUsername(String username);

        boolean existsByEmail(String email);

        List<User> findByRole(UserRole role);

        List<User> findByRoleNot(UserRole role);

        @Query("SELECT u FROM User u WHERE " +
                        "(:excludeSuperAdmin = false OR u.role <> lk.ijse.etechbackend.enumiration.UserRole.SUPERADMIN) AND "
                        +
                        "(:role IS NULL OR u.role = :role) AND " +
                        "(:branchId IS NULL OR u.assignedBranch.id = :branchId) AND " +
                        "(u.role != 'CUSTOMER') AND " +
                        "(u.status != 'DELETED') AND " +
                        "(:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        " LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        " LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
        List<User> filterEmployeeUsers(
                        @Param("excludeSuperAdmin") boolean excludeSuperAdmin,
                        @Param("role") UserRole role,
                        @Param("branchId") String branchId,
                        @Param("search") String search);

        @Query("SELECT u FROM User u WHERE " +
                        "(:excludeSuperAdmin = false OR u.role <> lk.ijse.etechbackend.enumiration.UserRole.SUPERADMIN) AND "
                        +
                        "(:role IS NULL OR u.role = :role) AND " +
                        "(u.status != 'DELETED') AND " +
                        "(:branchId IS NULL OR u.assignedBranch.id = :branchId) AND " +
                        "(:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        " LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        " LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
        List<User> filterUsers(
                        @Param("excludeSuperAdmin") boolean excludeSuperAdmin,
                        @Param("role") UserRole role,
                        @Param("branchId") String branchId,
                        @Param("search") String search);

        @Query("SELECT u FROM User u WHERE u.role = 'CUSTOMER' AND (u.status != 'DELETED') AND " +
                        "(:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        " LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        " LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
        List<User> filterCustomerUsers(@Param("search") String search);

        @Query("SELECT u FROM User u WHERE " +
                        "(:excludeSuperAdmin = false OR u.role <> lk.ijse.etechbackend.enumiration.UserRole.SUPERADMIN) AND " +
                        "(:role IS NULL OR u.role = :role) AND " +
                        "(u.status != 'DELETED') AND " +
                        "(:status IS NULL OR u.status = :status) AND " +
                        "(:branchId IS NULL OR u.assignedBranch.id = :branchId) AND " +
                        "(:userType IS NULL OR (:userType = 'employees' AND u.role != lk.ijse.etechbackend.enumiration.UserRole.CUSTOMER) OR (:userType = 'customers' AND u.role = lk.ijse.etechbackend.enumiration.UserRole.CUSTOMER)) AND " +
                        "(:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        " LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        " LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<User> filterUsersPaged(
                        @Param("excludeSuperAdmin") boolean excludeSuperAdmin,
                        @Param("role") UserRole role,
                        @Param("status") Status status,
                        @Param("branchId") String branchId,
                        @Param("userType") String userType,
                        @Param("search") String search,
                        Pageable pageable);
}
