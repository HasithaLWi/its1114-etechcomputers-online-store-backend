package lk.ijse.etechbackend.service.impl;

import lk.ijse.etechbackend.dto.PageResponseDTO;
import lk.ijse.etechbackend.dto.order.*;
import lk.ijse.etechbackend.entity.*;
import lk.ijse.etechbackend.enumiration.OrderStatus;
import lk.ijse.etechbackend.exception.BadRequestException;
import lk.ijse.etechbackend.exception.ResourceNotFoundException;
import lk.ijse.etechbackend.repository.*;
import lk.ijse.etechbackend.service.EmailService;
import lk.ijse.etechbackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final UserRepository userRepository;
    private final DealBundleRepository dealBundleRepository;
    private final HotDealRepository hotDealRepository;
    private final EmailService emailService;

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders(OrderStatus status, String branchId) {
        log.info("Fetching orders (status={}, branchId={})", status, branchId);
        List<Order> orders;

        if (status != null && branchId != null && !branchId.isBlank()) {
            orders = orderRepository.findByStatus(status).stream()
                    .filter(o -> o.getFulfillmentBranch() != null && branchId.equals(o.getFulfillmentBranch().getId()))
                    .collect(Collectors.toList());
        } else if (status != null) {
            orders = orderRepository.findByStatus(status);
        } else if (branchId != null && !branchId.isBlank()) {
            orders = orderRepository.findByFulfillmentBranchId(branchId);
        } else {
            orders = orderRepository.findAll();
        }

        return orders.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<OrderResponseDTO> getFilteredOrders(OrderStatus status, String branchId, String search, int page, int size, String sortBy, String sortDir) {
        log.info("Fetching paged orders (status={}, branchId={}, search={}, page={}, size={}, sortBy={}, sortDir={})",
                status, branchId, search, page, size, sortBy, sortDir);

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = (sortBy != null && !sortBy.isBlank()) ? sortBy : "orderDate";
        if ("totalAmount".equalsIgnoreCase(sortProperty) || "total".equalsIgnoreCase(sortProperty)) {
            sortProperty = "totalAmount";
        } else if ("date".equalsIgnoreCase(sortProperty)) {
            sortProperty = "orderDate";
        } else if ("id".equalsIgnoreCase(sortProperty)) {
            sortProperty = "id";
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        String cleanSearch = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        String cleanBranch = (branchId != null && !branchId.isBlank() && !"ALL".equalsIgnoreCase(branchId.trim())) ? branchId.trim() : null;

        Page<Order> orderPage = orderRepository.filterOrdersPaged(status, cleanBranch, cleanSearch, pageable);
        List<OrderResponseDTO> dtos = orderPage.getContent().stream().map(this::toDTO).collect(Collectors.toList());

        return PageResponseDTO.of(orderPage, dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getMyOrders(String username) {
        log.info("Fetching orders for customer: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(user.getId());
        return orders.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderByCode(String orderCode) {
        log.info("Fetching order by code: {}", orderCode);
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with code: " + orderCode));
        return toDTO(order);
    }

    @Override
    public OrderResponseDTO createOrder(String currentUsernameOrNull, OrderCreateRequestDTO request) {
        log.info("Placing new order for customer: {}", request.getCustomerName());

        Branch branch = branchRepository.findById(request.getFulfillmentBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Fulfillment branch not found: " + request.getFulfillmentBranchId()));

        User user = null;
        if (currentUsernameOrNull != null && !currentUsernameOrNull.isBlank()) {
            user = userRepository.findByUsername(currentUsernameOrNull).orElse(null);
        }

        String orderCode = "ORD-2026-" + (int)(Math.random() * 9000 + 1000);

        BigDecimal distanceKm = request.getDistanceKm() != null ? request.getDistanceKm() : BigDecimal.valueOf(5.0);
        BigDecimal baseRate = branch.getBaseShippingRate() != null ? branch.getBaseShippingRate() : new BigDecimal("350.00");
        BigDecimal distanceCharge = distanceKm.multiply(BigDecimal.valueOf(15)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal calculatedShippingFee = baseRate.add(distanceCharge).setScale(2, RoundingMode.HALF_UP);

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        boolean isFreeShippingPromo = false;
        java.util.Set<Long> processedBundleIds = new java.util.HashSet<>();

        String paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod().trim() : "Credit / Debit Card";
        String paymentReference = request.getPaymentReference();
        if (paymentReference == null || paymentReference.isBlank()) {
            if (paymentMethod.toLowerCase().contains("cash") || paymentMethod.toLowerCase().contains("delivery")) {
                paymentReference = "COD-REF-" + (int)(Math.random() * 900000 + 100000);
            } else {
                paymentReference = "PAY-LKR-" + (int)(Math.random() * 900000 + 100000);
            }
        } else {
            paymentReference = paymentReference.trim();
        }

        String paymentStatus = request.getPaymentStatus();
        if (paymentStatus == null || paymentStatus.isBlank()) {
            if (paymentMethod.toLowerCase().contains("cash") || paymentMethod.toLowerCase().contains("delivery")) {
                paymentStatus = "PENDING_ON_DELIVERY";
            } else {
                paymentStatus = "PAID";
            }
        } else {
            paymentStatus = paymentStatus.trim().toUpperCase();
        }

        Order order = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .customerName(request.getCustomerName().trim())
                .customerEmail(request.getCustomerEmail().trim())
                .customerPhone(request.getCustomerPhone().trim())
                .shippingAddress(request.getShippingAddress().trim())
                .city(request.getCity().trim())
                .fulfillmentBranch(branch)
                .distanceKm(distanceKm)
                .deliveryLatitude(request.getDeliveryLatitude())
                .deliveryLongitude(request.getDeliveryLongitude())
                .subtotal(BigDecimal.ZERO)
                .shippingFee(calculatedShippingFee)
                .tax(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .status(OrderStatus.Pending)
                .paymentMethod(paymentMethod)
                .paymentReference(paymentReference)
                .paymentStatus(paymentStatus)
                .build();

        for (OrderItemRequestDTO itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + itemReq.getProductId()));

            int reqQty = itemReq.getQuantity();

            // Check stock in fulfillment branch
            BranchInventory inventory = branchInventoryRepository.findByProductIdAndBranchId(product.getId(), branch.getId())
                    .orElseThrow(() -> new BadRequestException("Product " + product.getName() + " is not available at branch " + branch.getName()));

            if (inventory.getQuantity() < reqQty) {
                throw new BadRequestException("Insufficient stock for " + product.getName() + " at " + branch.getName() +
                        ". Available: " + inventory.getQuantity() + ", Requested: " + reqQty);
            }

            // Deduct stock atomically
            inventory.setQuantity(inventory.getQuantity() - reqQty);
            branchInventoryRepository.save(inventory);

            // Determine effective unit price:
            BigDecimal effectiveUnitPrice = product.getPrice();
            if (itemReq.getUnitPrice() != null && itemReq.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                // If client passed a bundle-discounted unit price, honor it
                effectiveUnitPrice = itemReq.getUnitPrice();
            } else {
                // Or check if product has an active hot deal
                Optional<HotDeal> hotDealOpt = hotDealRepository.findByProductId(product.getId());
                if (hotDealOpt.isPresent() && Boolean.TRUE.equals(hotDealOpt.get().getIsActive())) {
                    effectiveUnitPrice = hotDealOpt.get().getPromoPrice();
                }
            }

            // Check promotional Free Shipping eligibility:
            if (itemReq.getBundleId() != null) {
                Optional<DealBundle> bundleOpt = dealBundleRepository.findById(itemReq.getBundleId());
                if (bundleOpt.isPresent()) {
                    DealBundle bundle = bundleOpt.get();
                    if (Boolean.TRUE.equals(bundle.getIsFreeShipping())) {
                        isFreeShippingPromo = true;
                    }
                    // Increment bundle soldCount
                    if (!processedBundleIds.contains(bundle.getId())) {
                        processedBundleIds.add(bundle.getId());
                        bundle.setSoldCount((bundle.getSoldCount() != null ? bundle.getSoldCount() : 0) + 1);
                        dealBundleRepository.save(bundle);
                    }
                }
            }

            Optional<HotDeal> hotDealOpt = hotDealRepository.findByProductId(product.getId());
            if (hotDealOpt.isPresent() && Boolean.TRUE.equals(hotDealOpt.get().getIsActive()) && Boolean.TRUE.equals(hotDealOpt.get().getIsFreeShipping())) {
                isFreeShippingPromo = true;
            }

            BigDecimal itemTotal = effectiveUnitPrice.multiply(BigDecimal.valueOf(reqQty));
            subtotal = subtotal.add(itemTotal);

            String itemWarranty = product.getWarranty() != null && !product.getWarranty().isBlank()
                    ? product.getWarranty().trim()
                    : "Official Hardware Warranty";

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .unitPrice(effectiveUnitPrice)
                    .quantity(reqQty)
                    .totalPrice(itemTotal)
                    .bundleId(itemReq.getBundleId())
                    .warranty(itemWarranty)
                    .build();

            orderItems.add(orderItem);
        }

        BigDecimal finalShippingFee = isFreeShippingPromo ? BigDecimal.ZERO : calculatedShippingFee;
        BigDecimal totalAmount = subtotal.add(finalShippingFee);

        order.setShippingFee(finalShippingFee);
        order.setSubtotal(subtotal);
        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);
        log.info("Successfully created order code: {} (total: {}, freeShipping: {}, paymentStatus: {})",
                savedOrder.getOrderCode(), savedOrder.getTotalAmount(), isFreeShippingPromo, savedOrder.getPaymentStatus());

        // Dispatch official Tax Invoice & Order Confirmation email asynchronously
        try {
            emailService.sendOrderConfirmationInvoice(savedOrder);
        } catch (Exception e) {
            log.error("Failed to trigger order confirmation invoice email for order #{}: {}", savedOrder.getOrderCode(), e.getMessage());
        }

        return toDTO(savedOrder);
    }

    @Override
    public OrderResponseDTO updateOrderStatus(Long orderId, OrderStatusUpdateDTO request) {
        log.info("Updating order ID {} status to: {}", orderId, request.getStatus());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        if (oldStatus == newStatus) {
            return toDTO(order);
        }

        // If transitioning to Cancelled and wasn't previously Cancelled, restore stock
        if (newStatus == OrderStatus.Cancelled && oldStatus != OrderStatus.Cancelled) {
            Branch branch = order.getFulfillmentBranch();
            if (branch != null && order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    if (item.getProduct() != null) {
                        BranchInventory inventory = branchInventoryRepository.findByProductIdAndBranchId(item.getProduct().getId(), branch.getId())
                                .orElseGet(() -> BranchInventory.builder()
                                        .product(item.getProduct())
                                        .branch(branch)
                                        .quantity(0)
                                        .build());
                        inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
                        branchInventoryRepository.save(inventory);
                    }
                }
            }
        }

        // When order is Delivered, mark payment status as PAID (e.g. settling Cash on Delivery)
        if (newStatus == OrderStatus.Delivered) {
            order.setPaymentStatus("PAID");
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        // Dispatch complete Tax Invoice with PAID status on delivery
        if (newStatus == OrderStatus.Delivered) {
            try {
                emailService.sendOrderDeliveredInvoice(saved);
            } catch (Exception e) {
                log.error("Failed to trigger order delivered invoice email for order #{}: {}", saved.getOrderCode(), e.getMessage());
            }
        }

        return toDTO(saved);
    }

    @Override
    public OrderResponseDTO updateOrderStatus(String idOrCode, OrderStatusUpdateDTO request) {
        if (idOrCode == null || idOrCode.isBlank()) {
            throw new BadRequestException("Order ID or code is required");
        }
        String clean = idOrCode.trim().replace("#", "");
        Order order = null;
        try {
            Long id = Long.parseLong(clean);
            order = orderRepository.findById(id).orElse(null);
        } catch (NumberFormatException ignored) {}

        if (order == null) {
            order = orderRepository.findByOrderCode(clean)
                    .orElseGet(() -> orderRepository.findByOrderCode(idOrCode.trim())
                            .orElseThrow(() -> new ResourceNotFoundException("Order not found with code or ID: " + idOrCode)));
        }

        return updateOrderStatus(order.getId(), request);
    }

    private OrderResponseDTO toDTO(Order o) {
        List<OrderItemResponseDTO> itemDTOs = new ArrayList<>();
        if (o.getItems() != null) {
            for (OrderItem item : o.getItems()) {
                String image = null;
                if (item.getProduct() != null && item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                    image = item.getProduct().getImages().get(0).getImageUrl();
                }
                String warranty = item.getWarranty();
                if ((warranty == null || warranty.isBlank()) && item.getProduct() != null) {
                    warranty = item.getProduct().getWarranty();
                }
                if (warranty == null || warranty.isBlank()) {
                    warranty = "Official Hardware Warranty";
                }

                itemDTOs.add(OrderItemResponseDTO.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .productName(item.getProductName())
                        .productSku(item.getProductSku())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .totalPrice(item.getTotalPrice())
                        .image(image)
                        .bundleId(item.getBundleId())
                        .warranty(warranty)
                        .build());
            }
        }

        return OrderResponseDTO.builder()
                .id(o.getId())
                .orderCode(o.getOrderCode())
                .userId(o.getUser() != null ? o.getUser().getId() : null)
                .customerName(o.getCustomerName())
                .customerEmail(o.getCustomerEmail())
                .customerPhone(o.getCustomerPhone())
                .shippingAddress(o.getShippingAddress())
                .city(o.getCity())
                .fulfillmentBranchId(o.getFulfillmentBranch() != null ? o.getFulfillmentBranch().getId() : null)
                .fulfillmentBranchName(o.getFulfillmentBranch() != null ? o.getFulfillmentBranch().getName() : null)
                .distanceKm(o.getDistanceKm())
                .deliveryLatitude(o.getDeliveryLatitude())
                .deliveryLongitude(o.getDeliveryLongitude())
                .subtotal(o.getSubtotal())
                .shippingFee(o.getShippingFee())
                .tax(o.getTax())
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus())
                .paymentMethod(o.getPaymentMethod())
                .paymentReference(o.getPaymentReference())
                .paymentStatus(o.getPaymentStatus())
                .items(itemDTOs)
                .orderDate(o.getOrderDate())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
