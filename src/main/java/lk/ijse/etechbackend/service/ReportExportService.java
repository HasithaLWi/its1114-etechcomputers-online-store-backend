package lk.ijse.etechbackend.service;

import lk.ijse.etechbackend.dto.analytics.*;

import java.util.List;

public interface ReportExportService {

    byte[] generatePdfReport(
            AnalyticsSummaryDTO summary,
            List<BranchPerformanceDTO> branches,
            List<TopProductDTO> topProducts,
            List<CategoryPerformanceDTO> categories,
            InventoryHealthDTO inventory,
            String dateRangeLabel,
            String branchScopeLabel,
            String generatedBy
    );

    byte[] generateExcelReport(
            AnalyticsSummaryDTO summary,
            List<BranchPerformanceDTO> branches,
            List<TopProductDTO> topProducts,
            List<CategoryPerformanceDTO> categories,
            InventoryHealthDTO inventory,
            String dateRangeLabel,
            String branchScopeLabel,
            String generatedBy
    );

    byte[] generateCsvReport(
            AnalyticsSummaryDTO summary,
            List<BranchPerformanceDTO> branches,
            List<TopProductDTO> topProducts,
            List<CategoryPerformanceDTO> categories,
            InventoryHealthDTO inventory,
            String dateRangeLabel,
            String branchScopeLabel,
            String generatedBy
    );
}
