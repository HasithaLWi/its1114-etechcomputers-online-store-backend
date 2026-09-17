package lk.ijse.etechbackend.dto.analytics;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchPerformanceDTO {
    private String branchId;
    private String branchName;
    private String city;
    private long orderCount;
    private long completedOrders;
    private BigDecimal revenue;
    private BigDecimal percentage;
    private double fulfillmentRate;
}
