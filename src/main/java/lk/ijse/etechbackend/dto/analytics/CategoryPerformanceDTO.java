package lk.ijse.etechbackend.dto.analytics;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPerformanceDTO {
    private String categoryId;
    private String categoryName;
    private long unitsSold;
    private BigDecimal revenue;
    private BigDecimal percentage;
}
