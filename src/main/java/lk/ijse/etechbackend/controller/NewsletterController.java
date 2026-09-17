package lk.ijse.etechbackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lk.ijse.etechbackend.dto.CommonResponse;
import lk.ijse.etechbackend.dto.newsletter.*;
import lk.ijse.etechbackend.enumiration.SubscriberSource;
import lk.ijse.etechbackend.enumiration.SubscriberStatus;
import lk.ijse.etechbackend.service.NewsletterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;

    @GetMapping(value = "/subscribers", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'STAFF')")
    public ResponseEntity<CommonResponse> getAllSubscribers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SubscriberStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("REST: Fetching subscribers with status filter: {}", status);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Subscribers retrieved successfully")
                .body(newsletterService.getSubscribers(search, status, page, size))
                .build());
    }

    @GetMapping(value = "/subscribers/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'STAFF')")
    public ResponseEntity<CommonResponse> getSubscriberById(@PathVariable Long id) {
        log.info("REST: Fetching subscriber by ID: {}", id);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Subscriber retrieved successfully")
                .body(newsletterService.getSubscriberById(id))
                .build());
    }

    @PostMapping(value = "/subscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse> subscribe(
            @Valid @RequestBody SubscriberRequestDTO request,
            HttpServletRequest httpRequest) {
        String ip = httpRequest != null ? httpRequest.getRemoteAddr() : "127.0.0.1";
        log.info("REST: New subscriber registration - {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("Subscribed to newsletter successfully")
                .body(newsletterService.subscribe(request, ip))
                .build());
    }

    
    @GetMapping(value = "/unsubscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse> unsubscribeGet(@RequestParam String email) {
        log.info("REST: Unsubscribing via GET link - {}", email);
        newsletterService.unsubscribe(email);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Unsubscribed from newsletter successfully")
                .build());
    }
    @PostMapping(value = "/unsubscribe", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommonResponse> unsubscribe(@RequestBody SubscriberDTO request) {
        log.info("REST: Unsubscribing - {}", request.getEmail());
        newsletterService.unsubscribe(request.getEmail());
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Unsubscribed from newsletter successfully")
                .build());
    }

    @PatchMapping(value = "/subscribers/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'STAFF')")
    public ResponseEntity<CommonResponse> updateSubscriberStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        log.info("REST: Updating subscriber ID {} status to {}", id, status);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Subscriber status updated successfully")
                .body(newsletterService.updateStatus(id, status))
                .build());
    }

    @PutMapping(value = "/subscribers/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'STAFF')")
    public ResponseEntity<CommonResponse> updateSubscriber(@PathVariable Long id,@RequestBody SubscriberRequestDTO request) {
        log.info("REST: Updating subscriber ID {}", id);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Subscriber updated successfully")
                .body(newsletterService.updateSubscriber(id, request))
                .build());
    }

    @DeleteMapping(value = "/subscribers/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> deleteSubscriber(@PathVariable Long id) {
        log.info("REST: Deleting subscriber ID {}", id);
        newsletterService.deleteSubscriber(id);
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Subscriber deleted successfully")
                .build());
    }

    @PatchMapping(value = "/subscribers/bulk-status", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> bulkUpdateStatus(@RequestBody List<Long> request, @RequestParam String status) {
        log.info("REST: Bulk updating status for {} subscribers", request.size());
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Bulk status updated successfully")
                .body(newsletterService.bulkUpdateStatus(request, status))
                .build());
    }

    @DeleteMapping(value = "/subscribers/bulk-delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> bulkDelete(@RequestBody List<Long> request) {
        log.info("REST: Bulk deleting {} subscribers", request.size());
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Bulk subscribers deleted successfully")
                .body(newsletterService.bulkDelete(request))
                .build());
    }

    @PostMapping(value = "/campaigns/send", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN')")
    public ResponseEntity<CommonResponse> sendCampaign(@Valid @RequestBody CampaignSendRequestDTO request) {
        log.info("REST: Sending newsletter campaign - {}", request.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.builder()
                .status(HttpStatus.CREATED.value())
                .message("Campaign broadcast sent successfully")
                .body(newsletterService.sendCampaign(request))
                .build());
    }

    @GetMapping(value = "/campaigns", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'STAFF')")
    public ResponseEntity<CommonResponse> getAllCampaigns() {
        log.info("REST: Fetching campaign history");
        return ResponseEntity.ok(CommonResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Campaign history retrieved successfully")
                .body(newsletterService.getAllCampaigns())
                .build());
    }
}
