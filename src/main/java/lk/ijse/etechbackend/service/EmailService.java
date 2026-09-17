package lk.ijse.etechbackend.service;

import lk.ijse.etechbackend.dto.SupportInquiryDTO;
import lk.ijse.etechbackend.entity.Order;

public interface EmailService {

    void sendOrderConfirmationInvoice(Order order);

    void sendOrderDeliveredInvoice(Order order);

    void sendPasswordResetOtp(String toEmail, String recipientName, String otp);

    void sendPasswordChangedAlert(String toEmail, String recipientName);

    void sendWelcomeEmail(String toEmail, String recipientName, String username);

    void sendSupportInquiry(SupportInquiryDTO inquiry);

    void sendNewsletterWelcomeEmail(String toEmail, String recipientName);

    void sendCampaignEmail(String toEmail, String recipientName, String subject, String preheader, String contentHtml);

    void sendGenericHtmlEmail(String toEmail, String subject, String htmlContent);
}
