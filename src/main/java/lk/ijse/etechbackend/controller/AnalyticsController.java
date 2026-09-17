package lk.ijse.etechbackend.controller;

import lk.ijse.etechbackend.dto.CommonResponse;
import lk.ijse.etechbackend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> getSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "ALL") String branchId) {
        log.info("REST: Fetching analytics summary from={}, to={}, branchId={}", from, to, branchId);
        LocalDateTime start = parseStartDate(from);
        LocalDateTime end = parseEndDate(to);

        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Analytics summary retrieved successfully")
                .body(analyticsService.getSummary(start, end, branchId))
                .build());
    }

    @GetMapping(value = "/sales-trends", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> getSalesTrends(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "ALL") String branchId) {
        log.info("REST: Fetching sales trend from={}, to={}, branchId={}", from, to, branchId);
        LocalDateTime start = parseStartDate(from);
        LocalDateTime end = parseEndDate(to);

        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Sales trends retrieved successfully")
                .body(analyticsService.getSalesTrends(start, end, branchId))
                .build());
    }

    @GetMapping(value = "/branch-performance", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> getBranchPerformance(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        log.info("REST: Fetching branch performance matrix from={}, to={}", from, to);
        LocalDateTime start = parseStartDate(from);
        LocalDateTime end = parseEndDate(to);

        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Branch performance retrieved successfully")
                .body(analyticsService.getBranchPerformance(start, end))
                .build());
    }

    @GetMapping(value = "/category-performance", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> getCategoryPerformance(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "ALL") String branchId) {
        log.info("REST: Fetching category performance from={}, to={}, branchId={}", from, to, branchId);
        LocalDateTime start = parseStartDate(from);
        LocalDateTime end = parseEndDate(to);

        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Category performance retrieved successfully")
                .body(analyticsService.getCategoryPerformance(start, end, branchId))
                .build());
    }

    @GetMapping(value = "/top-products", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> getTopProducts(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false, defaultValue = "ALL") String branchId,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("REST: Fetching top products limit={}, from={}, to={}, branchId={}", limit, from, to, branchId);
        LocalDateTime start = parseStartDate(from);
        LocalDateTime end = parseEndDate(to);

        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Top products retrieved successfully")
                .body(analyticsService.getTopProductsFiltered(start, end, branchId, limit))
                .build());
    }

    @GetMapping(value = "/inventory-health", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> getInventoryHealth(
            @RequestParam(required = false, defaultValue = "ALL") String branchId) {
        log.info("REST: Fetching inventory health branchId={}", branchId);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Inventory health retrieved successfully")
                .body(analyticsService.getInventoryHealth(branchId))
                .build());
    }

    // Legacy backward compatibility
    @GetMapping(value = "/overview", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> getAnalyticsOverview() {
        log.info("REST: Fetching legacy analytics overview");
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Analytics overview retrieved successfully")
                .body(analyticsService.getOverview())
                .build());
    }

    @GetMapping(value = "/branch-revenue", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> getBranchRevenue() {
        log.info("REST: Fetching legacy branch revenue");
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Branch revenue breakdown retrieved successfully")
                .body(analyticsService.getBranchRevenue())
                .build());
    }

    // Helper Date Parsers
    private LocalDateTime parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || "null".equalsIgnoreCase(dateStr)) return null;
        try {
            LocalDate d = LocalDate.parse(dateStr.trim());
            return d.atStartOfDay();
        } catch (Exception e) {
            log.warn("Invalid start date format: {}", dateStr);
            return null;
        }
    }

    private LocalDateTime parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || "null".equalsIgnoreCase(dateStr)) return null;
        try {
            LocalDate d = LocalDate.parse(dateStr.trim());
            return d.atTime(LocalTime.MAX);
        } catch (Exception e) {
            log.warn("Invalid end date format: {}", dateStr);
            return null;
        }
    }
}
