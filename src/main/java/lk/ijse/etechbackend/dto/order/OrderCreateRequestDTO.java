package lk.ijse.etechbackend.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDTO {
    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer email is required")
    private String customerEmail;

    @NotBlank(message = "Customer phone is required")
    private String customerPhone;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Fulfillment branch ID is required")
    private String fulfillmentBranchId;

    private BigDecimal distanceKm;
    private BigDecimal deliveryLatitude;
    private BigDecimal deliveryLongitude;
    private String paymentMethod;
    private String paymentReference;
    private String paymentStatus;

    @NotEmpty(message = "Order items cannot be empty")
    private List<OrderItemRequestDTO> items;
}
