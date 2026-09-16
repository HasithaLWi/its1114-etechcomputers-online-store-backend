package lk.ijse.etechbackend.controller;

import jakarta.validation.Valid;
import lk.ijse.etechbackend.dto.CommonResponse;
import lk.ijse.etechbackend.dto.PageResponseDTO;
import lk.ijse.etechbackend.dto.order.OrderCreateRequestDTO;
import lk.ijse.etechbackend.dto.order.OrderResponseDTO;
import lk.ijse.etechbackend.dto.order.OrderStatusUpdateDTO;
import lk.ijse.etechbackend.enumiration.OrderStatus;
import lk.ijse.etechbackend.exception.BadRequestException;
import lk.ijse.etechbackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'STAFF')")
    public ResponseEntity<CommonResponse> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String branchId) {
        log.info("REST: Fetching all orders, status: {}, branch: {}", status, branchId);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Orders retrieved successfully")
                .body(orderService.getAllOrders(status, branchId))
                .build());
    }

    @GetMapping(value = "/filter", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'STAFF')")
    public ResponseEntity<CommonResponse> getFilteredOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String branchId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("REST: Querying orders paged - status: {}, branchId: {}, search: {}, page: {}, size: {}, sortBy: {}, sortDir: {}",
                status, branchId, search, page, size, sortBy, sortDir);
        PageResponseDTO<OrderResponseDTO> response = orderService.getFilteredOrders(
                status, branchId, search, page, size, sortBy, sortDir);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Orders retrieved successfully")
                .body(response)
                .build());
    }

    @GetMapping(value = "/my-orders", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonResponse> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        log.info("REST: Fetching order history for user: {}", username);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("My orders retrieved successfully")
                .body(orderService.getMyOrders(username))
                .build());
    }

    @GetMapping(value = "/{orderCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse> getOrderByCode(@PathVariable String orderCode) {
        log.info("REST: Fetching order by code: {}", orderCode);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Order retrieved successfully")
                .body(orderService.getOrderByCode(orderCode))
                .build());
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse> createOrder(@Valid @RequestBody OrderCreateRequestDTO request,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        log.info("REST: Creating order by user: {}, customer: {}", username, request.getCustomerEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("Order placed successfully")
                .body(orderService.createOrder(username, request))
                .build());
    }

    @PatchMapping(value = "/{idOrCode}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'STAFF', 'CUSTOMER')")
    public ResponseEntity<CommonResponse> updateOrderStatus(
            @PathVariable String idOrCode,
            @RequestBody(required = false) OrderStatusUpdateDTO request,
            @RequestParam(required = false) OrderStatus status) {
        OrderStatus newStatus = (request != null && request.getStatus() != null) ? request.getStatus() : status;
        if (newStatus == null) {
            throw new BadRequestException("Order status is required");
        }
        OrderStatusUpdateDTO updateDTO = new OrderStatusUpdateDTO(newStatus);
        log.info("REST: Updating status for order {} to {}", idOrCode, newStatus);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Order status updated successfully")
                .body(orderService.updateOrderStatus(idOrCode, updateDTO))
                .build());
    }
}
