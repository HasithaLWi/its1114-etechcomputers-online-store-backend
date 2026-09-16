package lk.ijse.etechbackend.service;

import lk.ijse.etechbackend.dto.SupportInquiryDTO;

public interface EmailService {

    void sendPasswordResetOtp(String toEmail, String recipientName, String otp);

    void sendPasswordChangedAlert(String toEmail, String recipientName);

    void sendWelcomeEmail(String toEmail, String recipientName, String username);

    void sendSupportInquiry(SupportInquiryDTO inquiry);

    void sendNewsletterWelcomeEmail(String toEmail, String recipientName);

    void sendCampaignEmail(String toEmail, String recipientName, String subject, String preheader, String contentHtml);

    void sendGenericHtmlEmail(String toEmail, String subject, String htmlContent);
}
