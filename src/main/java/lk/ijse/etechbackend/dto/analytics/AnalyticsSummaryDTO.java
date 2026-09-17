package lk.ijse.etechbackend.dto.analytics;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryDTO {
    private BigDecimal grossRevenue;
    private BigDecimal netRevenue;
    private long totalOrders;
    private long completedOrders;
    private long pendingOrders;
    private long cancelledOrders;
    private BigDecimal avgOrderValue;
    private long totalUnitsSold;
    private double fulfillmentRate;
    private long activeUsers;
}
