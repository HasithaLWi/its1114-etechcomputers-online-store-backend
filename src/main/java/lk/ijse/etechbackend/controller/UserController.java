package lk.ijse.etechbackend.controller;

import jakarta.validation.Valid;
import lk.ijse.etechbackend.dto.*;
import lk.ijse.etechbackend.enumiration.Status;
import lk.ijse.etechbackend.enumiration.UserRole;
import lk.ijse.etechbackend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> getAllUsers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String search) {
        log.info("REST: Fetching user directory by {} (role={}, branch={}, search={})",
                userDetails.getUsername(), role, branch, search);
        List<UserDTO> users = userService.getAllUsers(userDetails.getUsername(), role, branch, search);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Users retrieved successfully")
                .body(users)
                .build());
    }

    @GetMapping(value = "/filter", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> getFilteredUsers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("REST: Querying users paged by {} - role: {}, status: {}, branch: {}, userType: {}, search: {}, page: {}, size: {}, sortBy: {}, sortDir: {}",
                userDetails.getUsername(), role, status, branch, userType, search, page, size, sortBy, sortDir);
        PageResponseDTO<UserDTO> response = userService.getFilteredUsers(
                userDetails.getUsername(), role, status, branch, userType, search, page, size, sortBy, sortDir);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Users retrieved successfully")
                .body(response)
                .build());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/all-employees")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> getAllEmployees(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String search) {
        log.info("REST: Fetching employee directory by {} (role={}, branch={}, search={})",
                userDetails.getUsername(), role, branch, search);
        List<UserDTO> employees = userService.getAllEmployees(userDetails.getUsername(), role, branch, search);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Employees retrieved successfully")
                .body(employees)
                .build());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/all-customers")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> getAllCustomers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String search) {
        log.info("REST: Fetching customer directory by {} (search={})",
                userDetails.getUsername(), search);
        List<UserDTO> customers = userService.getAllCustomers(userDetails.getUsername(), search);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Customers retrieved successfully")
                .body(customers)
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> getUserById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        log.info("REST: Fetching user ID {} by {}", id, userDetails.getUsername());
        UserDTO user = userService.getUserById(userDetails.getUsername(), id);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("User retrieved successfully")
                .body(user)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> createUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDTO request) {
        log.info("REST: Creating user {} with role {} by {}",
                request.getUsername(), request.getRole(), userDetails.getUsername());
        UserDTO createdUser = userService.createUser(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("User created successfully")
                .body(createdUser)
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> updateUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UserDTO request) {
        log.info("REST: Updating user ID {} by {}", id, userDetails.getUsername());
        UserDTO updatedUser = userService.updateUser(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("User updated successfully")
                .body(updatedUser)
                .build());
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> changeUserRole(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UserDTO request) {
        log.info("REST: Changing role of user ID {} to {} by {}", id, request.getRole(), userDetails.getUsername());
        UserDTO updatedUser = userService.changeUserRole(userDetails.getUsername(), id, request);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("User role updated successfully")
                .body(updatedUser)
                .build());
    }

    @GetMapping("/roles")
    public ResponseEntity<CommonResponse> getRoles() {
        log.info("REST: Fetching all system user roles");
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("User roles retrieved successfully")
                .body(userService.getRoles())
                .build());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> changeUserStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> statusMap) {
        String status = statusMap.get("status");
        log.info("REST: Changing status of user ID {} to {} by {}", id, status, userDetails.getUsername());
        UserDTO statusReq = UserDTO.builder()
                .status(status != null ? lk.ijse.etechbackend.enumiration.Status.valueOf(status.toUpperCase()) : null)
                .build();
        UserDTO updatedUser = userService.updateUserStatus(userDetails.getUsername(), id, statusReq);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("User status updated successfully")
                .body(updatedUser)
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        log.info("REST: Deleting user ID {} by {}", id, userDetails.getUsername());
        userService.deleteUser(userDetails.getUsername(), id);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("User account removed")
                .build());
    }

    @PutMapping("/me/profile")
    public ResponseEntity<CommonResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDTO request) {
        log.info("REST: Self-profile update requested by {}", userDetails.getUsername());
        UserDTO updatedProfile = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Profile updated successfully")
                .body(updatedProfile)
                .build());
    }

    @PutMapping("/me/password")
    public ResponseEntity<CommonResponse> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDTO request) {
        log.info("REST: Self-password change requested by {}", userDetails.getUsername());
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Password changed successfully")
                .build());
    }
}
