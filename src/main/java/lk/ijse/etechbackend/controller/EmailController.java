package lk.ijse.etechbackend.controller;

import jakarta.validation.Valid;
import lk.ijse.etechbackend.dto.ApiResponse;
import lk.ijse.etechbackend.dto.SupportInquiryDTO;
import lk.ijse.etechbackend.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/support")
    public ResponseEntity<ApiResponse> sendSupportInquiry(@Valid @RequestBody SupportInquiryDTO inquiry) {
        log.info("REST: Support inquiry received from [{}] with subject [{}]", inquiry.getEmail(), inquiry.getSubject());
        emailService.sendSupportInquiry(inquiry);
        return ResponseEntity.ok(ApiResponse.success("Your message has been delivered to ETech Support. An acknowledgment has been sent to your email."));
    }
}
