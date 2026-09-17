package lk.ijse.etechbackend.service;

import lk.ijse.etechbackend.dto.PageResponseDTO;
import lk.ijse.etechbackend.dto.order.OrderCreateRequestDTO;
import lk.ijse.etechbackend.dto.order.OrderResponseDTO;
import lk.ijse.etechbackend.dto.order.OrderStatusUpdateDTO;
import lk.ijse.etechbackend.enumiration.OrderStatus;

import java.util.List;

public interface OrderService {
    List<OrderResponseDTO> getAllOrders(OrderStatus status, String branchId);
    PageResponseDTO<OrderResponseDTO> getFilteredOrders(OrderStatus status, String branchId, String search, int page, int size, String sortBy, String sortDir);
    List<OrderResponseDTO> getMyOrders(String username);
    OrderResponseDTO getOrderByCode(String orderCode);
    OrderResponseDTO createOrder(String currentUsernameOrNull, OrderCreateRequestDTO request);
    OrderResponseDTO updateOrderStatus(Long orderId, OrderStatusUpdateDTO request);
    OrderResponseDTO updateOrderStatus(String idOrCode, OrderStatusUpdateDTO request);
}
