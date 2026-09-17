package lk.ijse.etechbackend.dto.analytics;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryHealthDTO {
    private long totalCatalogProducts;
    private long totalUnitsInStock;
    private long lowStockCount;
    private long outOfStockCount;
    private List<BranchStockSummaryDTO> branchSummaries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchStockSummaryDTO {
        private String branchId;
        private String branchName;
        private long totalUnits;
        private long lowStockItems;
        private long outOfStockItems;
    }
}
