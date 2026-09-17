package lk.ijse.etechbackend.controller;

import lk.ijse.etechbackend.dto.analytics.*;
import lk.ijse.etechbackend.service.AnalyticsService;
import lk.ijse.etechbackend.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AnalyticsService analyticsService;
    private final ReportExportService reportExportService;

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam(defaultValue = "PDF") String format,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "ALL") String branchId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("REST: Exporting report format={}, from={}, to={}, branchId={}", format, from, to, branchId);

        LocalDateTime start = parseStartDate(from);
        LocalDateTime end = parseEndDate(to);

        String dateRangeLabel = (from != null && !from.isBlank()) || (to != null && !to.isBlank())
                ? (from != null ? from : "Beginning") + " to " + (to != null ? to : "Present")
                : "All Time Cumulative";

        String branchScopeLabel = "ALL".equalsIgnoreCase(branchId) || branchId == null || branchId.isBlank()
                ? "All Branches (Corporate Consolidated)"
                : "Branch: " + branchId;

        String auditor = userDetails != null ? userDetails.getUsername() : "Administrator";

        AnalyticsSummaryDTO summary = analyticsService.getSummary(start, end, branchId);
        List<BranchPerformanceDTO> branches = analyticsService.getBranchPerformance(start, end);
        List<TopProductDTO> topProducts = analyticsService.getTopProductsFiltered(start, end, branchId, 25);
        List<CategoryPerformanceDTO> categories = analyticsService.getCategoryPerformance(start, end, branchId);
        InventoryHealthDTO inventory = analyticsService.getInventoryHealth(branchId);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String normalizedFormat = format != null ? format.trim().toUpperCase() : "PDF";

        byte[] fileBytes;
        String filename;
        MediaType mediaType;

        if ("XLSX".equals(normalizedFormat) || "EXCEL".equals(normalizedFormat)) {
            fileBytes = reportExportService.generateExcelReport(
                    summary, branches, topProducts, categories, inventory, dateRangeLabel, branchScopeLabel, auditor);
            filename = "ETec_Report_" + timestamp + ".xlsx";
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if ("CSV".equals(normalizedFormat)) {
            fileBytes = reportExportService.generateCsvReport(
                    summary, branches, topProducts, categories, inventory, dateRangeLabel, branchScopeLabel, auditor);
            filename = "ETec_Report_" + timestamp + ".csv";
            mediaType = MediaType.parseMediaType("text/csv; charset=UTF-8");
        } else {
            fileBytes = reportExportService.generatePdfReport(
                    summary, branches, topProducts, categories, inventory, dateRangeLabel, branchScopeLabel, auditor);
            filename = "ETec_Report_" + timestamp + ".pdf";
            mediaType = MediaType.APPLICATION_PDF;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(fileBytes.length);
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileBytes);
    }

    private LocalDateTime parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || "null".equalsIgnoreCase(dateStr)) return null;
        try {
            LocalDate d = LocalDate.parse(dateStr.trim());
            return d.atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || "null".equalsIgnoreCase(dateStr)) return null;
        try {
            LocalDate d = LocalDate.parse(dateStr.trim());
            return d.atTime(LocalTime.MAX);
        } catch (Exception e) {
            return null;
        }
    }
}
