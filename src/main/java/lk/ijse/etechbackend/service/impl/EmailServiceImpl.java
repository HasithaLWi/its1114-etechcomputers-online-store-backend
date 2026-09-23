package lk.ijse.etechbackend.service.impl;

import jakarta.mail.internet.MimeMessage;
import lk.ijse.etechbackend.dto.SupportInquiryDTO;
import lk.ijse.etechbackend.dto.profile.BusinessProfileDTO;
import lk.ijse.etechbackend.service.BusinessProfileService;
import lk.ijse.etechbackend.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lk.ijse.etechbackend.entity.Branch;
import lk.ijse.etechbackend.entity.Order;
import lk.ijse.etechbackend.entity.OrderItem;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private BusinessProfileService businessProfileService;

    @Value("${app.mail.from:eteccomputers38@gmail.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:ETech Computers Support}")
    private String fromName;

    /**
     * Dynamically loads store sender email from the business_profile database table.
     * Falls back to application.properties if database is initializing or unavailable.
     */
    public String getEffectiveStoreEmail() {
        if (businessProfileService != null) {
            try {
                BusinessProfileDTO profile = businessProfileService.getProfile();
                if (profile != null && profile.getSupportEmail() != null && !profile.getSupportEmail().isBlank()) {
                    return profile.getSupportEmail().trim();
                }
            } catch (Exception e) {
                log.debug("Could not read supportEmail from BusinessProfile: {}", e.getMessage());
            }
        }
        return fromEmail;
    }

    /**
     * Dynamically loads store name from the business_profile database table.
     */
    public String getEffectiveStoreName() {
        if (businessProfileService != null) {
            try {
                BusinessProfileDTO profile = businessProfileService.getProfile();
                if (profile != null && profile.getStoreName() != null && !profile.getStoreName().isBlank()) {
                    return profile.getStoreName().trim();
                }
            } catch (Exception e) {
                log.debug("Could not read storeName from BusinessProfile: {}", e.getMessage());
            }
        }
        return fromName;
    }

    @Override
    public void sendPasswordResetOtp(String toEmail, String recipientName, String otp) {
        String effectiveEmail = getEffectiveStoreEmail();
        String effectiveName = getEffectiveStoreName();
        String subject = effectiveName + " — Password Reset Verification Code: " + otp;
        String name = (recipientName != null && !recipientName.isBlank()) ? recipientName : "Valued Customer";

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Reset Password OTP</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f1f5f9; color: #0f172a;">
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color: #f1f5f9; padding: 30px 15px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%%" style="max-width: 580px; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06); border: 1px solid #e2e8f0;">
                      <!-- Header -->
                      <tr>
                        <td style="background: linear-gradient(135deg, #0f172a 0%%, #1e293b 100%%); padding: 28px 32px; text-align: center; border-bottom: 3px solid #2563eb;">
                          <h1 style="margin: 0; font-size: 24px; font-weight: 800; color: #ffffff; letter-spacing: -0.5px;">
                            %s
                          </h1>
                          <p style="margin: 4px 0 0 0; font-size: 11px; color: #94a3b8; letter-spacing: 1px; text-transform: uppercase;">
                            Next-Gen Tech Store
                          </p>
                        </td>
                      </tr>
                      <!-- Content Body -->
                      <tr>
                        <td style="padding: 32px 32px 24px 32px;">
                          <h2 style="margin: 0 0 12px 0; font-size: 18px; font-weight: 700; color: #0f172a;">Password Reset Verification</h2>
                          <p style="margin: 0 0 16px 0; font-size: 14px; line-height: 1.6; color: #475569;">
                            Hello <strong>%s</strong>,
                          </p>
                          <p style="margin: 0 0 24px 0; font-size: 14px; line-height: 1.6; color: #475569;">
                            We received a request to reset the password associated with your account. Use the 6-digit verification code below to complete the reset:
                          </p>
                          
                          <!-- OTP Code Box -->
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin-bottom: 24px;">
                            <tr>
                              <td align="center" style="background: #f8fafc; border: 2px dashed #cbd5e1; border-radius: 10px; padding: 20px;">
                                <span style="font-size: 11px; text-transform: uppercase; letter-spacing: 1.5px; font-weight: 700; color: #64748b; display: block; margin-bottom: 6px;">Your 6-Digit OTP</span>
                                <span style="font-family: 'Courier New', monospace; font-size: 36px; font-weight: 800; letter-spacing: 10px; color: #2563eb; display: block;">%s</span>
                                <span style="font-size: 12px; color: #ef4444; font-weight: 600; display: block; margin-top: 8px;">⏱ Expires in 10 minutes</span>
                              </td>
                            </tr>
                          </table>

                          <!-- Security Notice -->
                          <div style="background-color: #eff6ff; border-left: 4px solid #3b82f6; padding: 12px 16px; border-radius: 0 6px 6px 0; margin-bottom: 24px;">
                            <p style="margin: 0; font-size: 12px; line-height: 1.5; color: #1e40af;">
                              <strong>Security Tip:</strong> Never share this code with anyone. Customer support representatives will never ask for your verification code or account password.
                            </p>
                          </div>

                          <p style="margin: 0; font-size: 13px; line-height: 1.5; color: #64748b;">
                            If you did not request this password reset, please disregard this email or contact our support desk immediately at <a href="mailto:%s" style="color: #2563eb; text-decoration: none;">%s</a>.
                          </p>
                        </td>
                      </tr>
                      <!-- Footer -->
                      <tr>
                        <td style="background-color: #f8fafc; padding: 20px 32px; border-top: 1px solid #e2e8f0; text-align: center;">
                          <p style="margin: 0 0 6px 0; font-size: 11px; color: #94a3b8;">
                            &copy; %d %s Inc. All rights reserved.
                          </p>
                          <p style="margin: 0; font-size: 11px; color: #94a3b8;">
                            Sent from %s &bull; Safe 256-Bit SSL Encrypted System
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(effectiveName, name, otp, effectiveEmail, effectiveEmail, Year.now().getValue(), effectiveName, effectiveEmail);

        sendGenericHtmlEmail(toEmail, subject, html);
    }

    @Override
    @Async
    public void sendPasswordChangedAlert(String toEmail, String recipientName) {
        String effectiveEmail = getEffectiveStoreEmail();
        String effectiveName = getEffectiveStoreName();
        String subject = "Security Alert: Your " + effectiveName + " Password Was Changed";
        String name = (recipientName != null && !recipientName.isBlank()) ? recipientName : "Customer";

        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family: sans-serif; background-color: #f8fafc; padding: 20px; color: #0f172a;">
              <div style="max-width: 560px; margin: 0 auto; background: #ffffff; border-radius: 10px; border: 1px solid #e2e8f0; padding: 28px;">
                <h2 style="color: #10b981; margin-top: 0;">✔ Password Changed Successfully</h2>
                <p>Hello <strong>%s</strong>,</p>
                <p>The password for your account associated with <strong>%s</strong> was successfully updated.</p>
                <div style="background-color: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; padding: 14px; margin: 20px 0;">
                  <strong style="color: #b91c1c;">Did not make this change?</strong>
                  <p style="margin: 6px 0 0 0; font-size: 12px; color: #7f1d1d;">
                    If you did not perform this password change, someone may have compromised your account. Please reply immediately to <a href="mailto:%s">%s</a> to lock your session.
                  </p>
                </div>
                <p style="font-size: 12px; color: #64748b;">Warm regards,<br>%s Security Team</p>
              </div>
            </body>
            </html>
            """.formatted(name, toEmail, effectiveEmail, effectiveEmail, effectiveName);

        try {
            sendGenericHtmlEmail(toEmail, subject, html);
        } catch (Exception e) {
            log.warn("Could not dispatch password changed alert email: {}", e.getMessage());
        }
    }

    @Override
    @Async
    public void sendWelcomeEmail(String toEmail, String recipientName, String username) {
        String effectiveEmail = getEffectiveStoreEmail();
        String effectiveName = getEffectiveStoreName();
        String subject = "Welcome to " + effectiveName + ", " + recipientName + "!";
        String html = """
            <!DOCTYPE html>
            <html>
            <body style="font-family: sans-serif; background-color: #f8fafc; padding: 20px; color: #0f172a;">
              <div style="max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden;">
                <div style="background: #0f172a; padding: 24px; text-align: center;">
                  <h1 style="color: #ffffff; margin: 0; font-size: 22px;">Welcome to <span style="color: #3b82f6;">%s</span></h1>
                </div>
                <div style="padding: 28px;">
                  <h2>Hello %s!</h2>
                  <p>Thank you for joining Sri Lanka's premier destination for high-performance laptops, custom PC components, and genuine peripherals.</p>
                  <p><strong>Your Account Details:</strong></p>
                  <ul>
                    <li><strong>Username:</strong> %s</li>
                    <li><strong>Email:</strong> %s</li>
                  </ul>
                  <p>Enjoy our 2-Year Official Warranty, 100%% Genuine Products, and 24/7 Expert Tech Support on every purchase.</p>
                  <p>Happy Computing,<br>The %s Team</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(effectiveName, recipientName, username, toEmail, effectiveName);

        try {
            sendGenericHtmlEmail(toEmail, subject, html);
        } catch (Exception e) {
            log.warn("Could not dispatch welcome email: {}", e.getMessage());
        }
    }

    @Override
    @Async
    public void sendSupportInquiry(SupportInquiryDTO inquiry) {
        String effectiveEmail = getEffectiveStoreEmail();
        String effectiveName = getEffectiveStoreName();
        String internalSubject = "[Support Ticket] " + inquiry.getCategory() + " - " + inquiry.getSubject();
        String internalHtml = """
            <div style="font-family: sans-serif; padding: 20px;">
              <h2>New Customer Support Inquiry</h2>
              <p><strong>Customer:</strong> %s (%s)</p>
              <p><strong>Category:</strong> %s</p>
              <p><strong>Order Code:</strong> %s</p>
              <p><strong>Subject:</strong> %s</p>
              <hr/>
              <p><strong>Message:</strong></p>
              <blockquote style="background: #f1f5f9; padding: 15px; border-left: 4px solid #2563eb;">%s</blockquote>
            </div>
            """.formatted(
                inquiry.getName(), inquiry.getEmail(),
                inquiry.getCategory() != null ? inquiry.getCategory() : "General",
                inquiry.getOrderCode() != null ? inquiry.getOrderCode() : "N/A",
                inquiry.getSubject(), inquiry.getMessage()
        );

        try {
            // Send alert to store business inbox
            sendGenericHtmlEmail(effectiveEmail, internalSubject, internalHtml);
        } catch (Exception e) {
            log.error("Could not send internal support alert: {}", e.getMessage());
        }

        // Send auto-acknowledgment to customer
        String customerSubject = "We received your request: " + inquiry.getSubject();
        String customerHtml = """
            <div style="font-family: sans-serif; padding: 20px; max-width: 560px; margin: 0 auto; background: #fff; border: 1px solid #e2e8f0; border-radius: 8px;">
              <h2 style="color: #2563eb;">Thank you for reaching out to %s Support</h2>
              <p>Hello %s,</p>
              <p>We have received your support inquiry regarding <strong>"%s"</strong>. Our technical support team has been assigned your ticket and will review it within 24 business hours.</p>
              <p>If you have additional details, feel free to reply directly to this email.</p>
              <hr/>
              <p style="font-size: 11px; color: #64748b;">%s Customer Care &bull; %s</p>
            </div>
            """.formatted(effectiveName, inquiry.getName(), inquiry.getSubject(), effectiveName, effectiveEmail);

        try {
            sendGenericHtmlEmail(inquiry.getEmail(), customerSubject, customerHtml);
        } catch (Exception e) {
            log.error("Could not send customer acknowledgment: {}", e.getMessage());
        }
    }

    @Override
    public void sendGenericHtmlEmail(String toEmail, String subject, String htmlContent) {
        String effectiveEmail = getEffectiveStoreEmail();
        String effectiveName = getEffectiveStoreName();
        log.info("[EmailService] Dispatching real email over internet from [{}] <{}> to: [{}] | Subject: [{}]", effectiveName, effectiveEmail, toEmail, subject);

        if (mailSender == null) {
            log.error("[EmailService] JavaMailSender bean is not configured in Spring context.");
            throw new IllegalStateException("Email delivery service is unavailable. JavaMailSender is not initialized.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            helper.setFrom(effectiveEmail, effectiveName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("[EmailService] Successfully delivered real email to: [{}]", toEmail);
        } catch (Exception e) {
            log.error("[EmailService] SMTP delivery failed to [{}]: {}", toEmail, e.getMessage());
            throw new RuntimeException("SMTP email dispatch failed: " + e.getMessage(), e);
        }
    }
    @Override
    @Async
    public void sendNewsletterWelcomeEmail(String toEmail, String recipientName) {
        String effectiveEmail = getEffectiveStoreEmail();
        String effectiveName = getEffectiveStoreName();
        String subject = "Subscription Confirmed: Welcome to " + effectiveName + " Insider!";
        String name = (recipientName != null && !recipientName.isBlank()) ? recipientName.trim() : "Tech Enthusiast";
        String unsubscribeUrl = "https://etechcomputers.vercel.app/#unsubscribe?email=" + toEmail;

        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Welcome to ETech Insider</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #0f172a;">
              <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="table-layout: fixed;">
                <tr>
                  <td align="center" style="padding: 32px 16px;">
                    <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 600px; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05); border: 1px solid #e2e8f0;">
                      <!-- Header -->
                      <tr>
                        <td style="background-color: #0f172a; padding: 28px 32px; text-align: center;">
                          <h1 style="margin: 0; font-size: 24px; font-weight: 800; color: #ffffff; letter-spacing: -0.5px;">
                            %s <span style="color: #3b82f6;">Insider</span>
                          </h1>
                          <p style="margin: 6px 0 0 0; font-size: 13px; color: #94a3b8; font-weight: 500;">
                            Official Tech Updates &bull; Hardware Releases &bull; PC Guides
                          </p>
                        </td>
                      </tr>
                      <!-- Hero Accent -->
                      <tr>
                        <td style="height: 4px; background: linear-gradient(90deg, #2563eb 0%%, #38bdf8 100%%);"></td>
                      </tr>
                      <!-- Main Body -->
                      <tr>
                        <td style="padding: 36px 32px;">
                          <h2 style="margin: 0 0 16px 0; font-size: 20px; font-weight: 700; color: #0f172a;">
                            Hello %s,
                          </h2>
                          <p style="margin: 0 0 16px 0; font-size: 15px; line-height: 1.6; color: #334155;">
                            Thank you for subscribing to the <strong>%s</strong> newsletter. Your subscription is confirmed!
                          </p>
                          <p style="margin: 0 0 24px 0; font-size: 15px; line-height: 1.6; color: #334155;">
                            You will now receive direct notifications regarding:
                          </p>
                          <!-- Feature List -->
                          <div style="background-color: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; padding: 20px; margin-bottom: 24px;">
                            <table border="0" cellpadding="0" cellspacing="0" width="100%%">
                              <tr>
                                <td style="padding: 6px 0; font-size: 14px; color: #1e293b;">
                                  <strong style="color: #2563eb;">&bull; New Hardware Arrivals:</strong> Next-gen GPUs, processors, motherboards, and PC cases.
                                </td>
                              </tr>
                              <tr>
                                <td style="padding: 6px 0; font-size: 14px; color: #1e293b;">
                                  <strong style="color: #2563eb;">&bull; Custom Build Spotlights:</strong> Professional workstation designs and performance benchmarks.
                                </td>
                              </tr>
                              <tr>
                                <td style="padding: 6px 0; font-size: 14px; color: #1e293b;">
                                  <strong style="color: #2563eb;">&bull; Tech Insights:</strong> PC building guides, thermal optimization, and official store announcements.
                                </td>
                              </tr>
                            </table>
                          </div>
                          <p style="margin: 0 0 16px 0; font-size: 14px; line-height: 1.6; color: #475569;">
                            We respect your privacy and will never share your information or clutter your inbox with spam.
                          </p>
                          <p style="margin: 24px 0 0 0; font-size: 14px; color: #64748b;">
                            Warm regards,<br>
                            <strong style="color: #0f172a;">The %s Team</strong>
                          </p>
                        </td>
                      </tr>
                      <!-- Footer with Unsubscribe -->
                      <tr>
                        <td style="background-color: #f8fafc; padding: 24px 32px; border-top: 1px solid #e2e8f0; text-align: center;">
                          <p style="margin: 0 0 8px 0; font-size: 12px; color: #64748b;">
                            You received this email because you subscribed to updates on %s.
                          </p>
                          <p style="margin: 0; font-size: 12px; color: #94a3b8;">
                            Need assistance? Contact our support team at <a href="mailto:%s" style="color: #2563eb; text-decoration: none;">%s</a>
                          </p>
                          <p style="margin: 12px 0 0 0; font-size: 12px;">
                            <a href="%s" style="color: #ef4444; text-decoration: underline; font-weight: 500;">
                              Unsubscribe from ETech Newsletters
                            </a>
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(effectiveName, name, effectiveName, effectiveName, effectiveName, effectiveEmail, effectiveEmail, unsubscribeUrl);

        try {
            sendGenericHtmlEmail(toEmail, subject, html);
        } catch (Exception e) {
            log.error("[EmailService] Failed to send newsletter welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendCampaignEmail(String toEmail, String recipientName, String subject, String preheader, String contentHtml) {
        String effectiveEmail = getEffectiveStoreEmail();
        String effectiveName = getEffectiveStoreName();
        String name = (recipientName != null && !recipientName.isBlank()) ? recipientName.trim() : "Valued Customer";
        String unsubscribeUrl = "https://etechcomputers.vercel.app/#unsubscribe?email=" + toEmail;
        String storeUrl = "https://etechcomputers.vercel.app/#deals";
        String safePreheader = (preheader != null && !preheader.isBlank()) ? preheader.trim() : "Special member pricing valid across all Sri Lanka branches until Sunday.";
        String safeSubject = (subject != null && !subject.isBlank()) ? subject.trim() : "ETech Computers Marketing Broadcast";

        // 1. Personalize placeholders
        String processedContent = contentHtml != null ? contentHtml : "";
        processedContent = processedContent.replace("{{subscriber_name}}", name);
        processedContent = processedContent.replace("{{store_url}}", storeUrl);

        // 2. Format paragraphs into clean HTML if plain text provided
        String bodyHtml;
        if (processedContent.contains("<p>") || processedContent.contains("<div>") || processedContent.contains("<table")) {
            bodyHtml = processedContent;
        } else {
            String[] paragraphs = processedContent.split("\\r?\\n\\r?\\n");
            StringBuilder sb = new StringBuilder();
            for (String p : paragraphs) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    sb.append("<p style=\"margin: 0 0 16px 0; font-size: 14px; line-height: 1.65; color: #334155;\">")
                      .append(trimmed.replace("\n", "<br/>"))
                      .append("</p>");
                }
            }
            bodyHtml = sb.toString();
        }

        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>%s</title>
              <!-- Hidden preheader text -->
              <div style="display: none; max-height: 0px; overflow: hidden; font-size: 1px; line-height: 1px; color: #fff; opacity: 0;">
                %s &zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;
              </div>
            </head>
            <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased; color: #0f172a;">
              <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="table-layout: fixed; background-color: #f1f5f9;">
                <tr>
                  <td align="center" style="padding: 32px 16px;">
                    <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 600px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.08); border: 1px solid #e2e8f0;">
                      
                      <!-- Top Header Bar with Logo & Branding -->
                      <tr>
                        <td style="background: linear-gradient(135deg, #0f172a 0%%, #1e293b 100%%); padding: 24px 32px; border-bottom: 3px solid #2563eb;">
                          <table border="0" cellpadding="0" cellspacing="0" width="100%%">
                            <tr>
                              <td style="vertical-align: middle;">
                                <div style="display: inline-block; width: 34px; height: 34px; border-radius: 8px; background-color: #2563eb; color: #ffffff; font-weight: 900; font-size: 14px; line-height: 34px; text-align: center; vertical-align: middle; margin-right: 10px;">ET</div>
                                <span style="font-size: 18px; font-weight: 800; color: #ffffff; vertical-align: middle; letter-spacing: -0.5px;">%s</span>
                              </td>
                              <td align="right" style="vertical-align: middle;">
                                <span style="display: inline-block; padding: 4px 10px; background-color: rgba(59, 130, 246, 0.2); border: 1px solid rgba(59, 130, 246, 0.4); border-radius: 12px; font-size: 10px; font-weight: 800; color: #93c5fd; text-transform: uppercase; letter-spacing: 0.5px;">Official Broadcast</span>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>

                      <!-- Subject Line Header Banner -->
                      <tr>
                        <td style="background-color: #f8fafc; padding: 20px 32px; border-bottom: 1px solid #e2e8f0;">
                          <h2 style="margin: 0; font-size: 18px; font-weight: 800; color: #0f172a; line-height: 1.4;">
                            %s
                          </h2>
                          <p style="margin: 6px 0 0 0; font-size: 12px; color: #64748b;">
                            %s
                          </p>
                        </td>
                      </tr>

                      <!-- Main Campaign Content -->
                      <tr>
                        <td style="padding: 28px 32px 20px 32px;">
                          %s

                          <!-- Call To Action Button -->
                          <div style="margin: 28px 0 8px 0; text-align: left;">
                            <a href="%s" style="display: inline-block; background-color: #2563eb; color: #ffffff; font-size: 13px; font-weight: 700; text-decoration: none; padding: 12px 24px; border-radius: 8px; box-shadow: 0 4px 10px rgba(37, 99, 235, 0.25);">
                              Shop Deals & Announcements &rarr;
                            </a>
                          </div>
                        </td>
                      </tr>

                      <!-- Footer with Unsubscribe & Support -->
                      <tr>
                        <td style="background-color: #f8fafc; padding: 24px 32px; border-top: 1px solid #e2e8f0; text-align: center;">
                          <p style="margin: 0 0 6px 0; font-size: 11px; color: #64748b; font-weight: 600;">
                            %s &bull; Next-Gen Tech Store Official Sri Lanka
                          </p>
                          <p style="margin: 0 0 10px 0; font-size: 11px; color: #94a3b8;">
                            You received this broadcast as an active subscriber &bull; Support: <a href="mailto:%s" style="color: #2563eb; text-decoration: none;">%s</a>
                          </p>
                          <p style="margin: 0; font-size: 11px;">
                            <a href="%s" style="color: #ef4444; text-decoration: underline;">
                              Unsubscribe from these marketing broadcasts
                            </a>
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
                safeSubject,
                safePreheader,
                effectiveName,
                safeSubject,
                safePreheader,
                bodyHtml,
                storeUrl,
                effectiveName,
                effectiveEmail,
                effectiveEmail,
                unsubscribeUrl
            );

        try {
            sendGenericHtmlEmail(toEmail, safeSubject, html);
        } catch (Exception e) {
            log.error("[EmailService] Failed to send campaign email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendOrderConfirmationInvoice(Order order) {
        if (order == null || order.getCustomerEmail() == null || order.getCustomerEmail().isBlank()) {
            log.warn("[EmailService] Cannot send order invoice: Order or customer email is null");
            return;
        }

        try {
            String toEmail = order.getCustomerEmail().trim();
            String effectiveEmail = getEffectiveStoreEmail();
            String effectiveName = getEffectiveStoreName();
            String customerName = (order.getCustomerName() != null && !order.getCustomerName().isBlank())
                    ? order.getCustomerName().trim() : "Valued Customer";
            String orderCode = order.getOrderCode() != null ? order.getOrderCode().trim() : "N/A";
            String subject = "Official Tax Invoice & Order Confirmation: #" + orderCode + " | " + effectiveName;

            // Formatted Date
            String orderDateStr;
            if (order.getOrderDate() != null) {
                orderDateStr = order.getOrderDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));
            } else {
                orderDateStr = "Recent Order";
            }

            // Payment Details
            String paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod().trim() : "Credit / Debit Card";
            String paymentRef = order.getPaymentReference() != null ? order.getPaymentReference().trim() : "PAY-REF-" + orderCode;
            String paymentStatus = order.getPaymentStatus() != null ? order.getPaymentStatus().trim().toUpperCase() : "PAID";

            String paymentBadgeHtml;
            if ("PAID".equals(paymentStatus)) {
                paymentBadgeHtml = "<span style=\"display: inline-block; padding: 4px 10px; background-color: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 9999px; color: #065f46; font-size: 11px; font-weight: 800; text-transform: uppercase; font-family: monospace;\">PAID</span>";
            } else {
                paymentBadgeHtml = "<span style=\"display: inline-block; padding: 4px 10px; background-color: #fffbeb; border: 1px solid #fde68a; border-radius: 9999px; color: #92400e; font-size: 11px; font-weight: 800; text-transform: uppercase; font-family: monospace;\">PENDING ON DELIVERY</span>";
            }

            // Branch Details
            Branch branch = order.getFulfillmentBranch();
            String branchName = branch != null ? branch.getName() : "Colombo Central SuperStore";
            String branchAddress = branch != null ? (branch.getAddress() + ", " + branch.getCity()) : "450 Galle Road, Colombo 03";
            String branchPhone = branch != null ? branch.getPhone() : "+94 11 234 5678";

            // Delivery Details
            String shippingAddress = (order.getShippingAddress() != null ? order.getShippingAddress().trim() : "") +
                    (order.getCity() != null ? ", " + order.getCity().trim() : "");
            String customerPhone = order.getCustomerPhone() != null ? order.getCustomerPhone().trim() : "N/A";

            // Items Table HTML Generation
            StringBuilder itemsHtml = new StringBuilder();
            Set<String> distinctWarranties = new LinkedHashSet<>();
            List<OrderItem> items = order.getItems();
            if (items != null && !items.isEmpty()) {
                int index = 1;
                for (OrderItem item : items) {
                    String pName = item.getProductName() != null ? item.getProductName() : "Hardware Component";
                    String sku = item.getProductSku() != null ? item.getProductSku() : "SKU-GEN";
                    int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                    BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                    BigDecimal totalPrice = item.getTotalPrice() != null ? item.getTotalPrice() : unitPrice.multiply(BigDecimal.valueOf(qty));

                    String itemWarranty = item.getWarranty();
                    if ((itemWarranty == null || itemWarranty.isBlank()) && item.getProduct() != null) {
                        itemWarranty = item.getProduct().getWarranty();
                    }
                    if (itemWarranty == null || itemWarranty.isBlank()) {
                        itemWarranty = "Official Hardware Warranty";
                    } else {
                        itemWarranty = itemWarranty.trim();
                    }
                    distinctWarranties.add(itemWarranty);

                    itemsHtml.append("""
                        <tr style="border-bottom: 1px solid #f1f5f9;">
                          <td style="padding: 12px 14px; vertical-align: middle;">
                            <strong style="color: #0f172a; font-size: 13px; display: block; margin-bottom: 3px;">%s</strong>
                            <div style="font-size: 11px; color: #64748b;">
                              <span style="font-family: monospace;">SKU: %s</span>
                              <span style="margin: 0 6px; color: #cbd5e1;">&bull;</span>
                              <span style="color: #0369a1; background-color: #f0f9ff; border: 1px solid #bae6fd; padding: 1px 7px; border-radius: 4px; font-weight: 600; font-size: 10px;">%s</span>
                            </div>
                          </td>
                          <td style="padding: 12px 14px; text-align: center; color: #334155; font-size: 13px; font-weight: 600; vertical-align: middle;">
                            %d
                          </td>
                          <td style="padding: 12px 14px; text-align: right; color: #334155; font-size: 13px; font-family: monospace; vertical-align: middle;">
                            Rs. %,.2f
                          </td>
                          <td style="padding: 12px 14px; text-align: right; color: #0f172a; font-size: 13px; font-weight: 700; font-family: monospace; vertical-align: middle;">
                            Rs. %,.2f
                          </td>
                        </tr>
                        """.formatted(pName, sku, itemWarranty, qty, unitPrice, totalPrice));
                    index++;
                }
            } else {
                itemsHtml.append("""
                    <tr>
                      <td colspan="4" style="padding: 16px; text-align: center; color: #64748b; font-size: 13px;">
                        Hardware components ordered
                      </td>
                    </tr>
                    """);
            }

            // Dynamic Warranty Card Box Generation
            String warrantyTitle;
            String warrantyDesc;
            if (distinctWarranties.isEmpty()) {
                warrantyTitle = "✔ Official Hardware Warranty Active";
                warrantyDesc = "All hardware serial numbers are registered into our nationwide service network. Keep this invoice email or order code for official claims.";
            } else if (distinctWarranties.size() == 1) {
                String singleWarranty = distinctWarranties.iterator().next();
                warrantyTitle = "✔ " + singleWarranty + " Active";
                warrantyDesc = "All hardware serial numbers are registered under " + singleWarranty + " in our nationwide service network. Keep this invoice email or order code for official claims.";
            } else {
                warrantyTitle = "✔ Official Hardware Warranty Coverage Active";
                StringBuilder sbDesc = new StringBuilder("Coverage registered per item: ");
                int wCount = 0;
                for (String w : distinctWarranties) {
                    if (wCount > 0) sbDesc.append(", ");
                    sbDesc.append("<strong>").append(w).append("</strong>");
                    wCount++;
                }
                sbDesc.append(". Keep this invoice email or order code for official claims.");
                warrantyDesc = sbDesc.toString();
            }

            String warrantyBoxHtml = """
                <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px; font-size: 11px; color: #475569; line-height: 1.5;">
                  <strong style="color: #0f172a; display: block; margin-bottom: 4px;">%s</strong>
                  %s
                </div>
                """.formatted(warrantyTitle, warrantyDesc);

            // Financial Summary
            BigDecimal subtotal = order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO;
            BigDecimal tax = order.getTax() != null ? order.getTax() : BigDecimal.ZERO;
            BigDecimal shipping = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
            BigDecimal grandTotal = order.getTotalAmount() != null ? order.getTotalAmount() : subtotal.add(tax).add(shipping);

            String shippingHtml;
            if (shipping.compareTo(BigDecimal.ZERO) == 0) {
                shippingHtml = "<span style=\"color: #059669; font-weight: 800; font-family: monospace;\">FREE</span> " +
                        "<span style=\"display: inline-block; padding: 2px 6px; background-color: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 4px; font-size: 9px; font-weight: 800; color: #065f46;\">PROMO APPLIED</span>";
            } else {
                shippingHtml = String.format("Rs. %,.2f", shipping);
            }

            String trackingUrl = "https://etechcomputers.vercel.app/#orders";

            // Full Tax Invoice HTML Document
            String invoiceHtml = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased; color: #0f172a;">
                  <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="table-layout: fixed; background-color: #f1f5f9;">
                    <tr>
                      <td align="center" style="padding: 32px 16px;">
                        <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 640px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.08); border: 1px solid #e2e8f0;">
                          
                          <!-- Top Header: Legal Entity & Tax Header -->
                          <tr>
                            <td style="background: linear-gradient(135deg, #0f172a 0%%, #1e293b 100%%); padding: 24px 32px; border-bottom: 3px solid #2563eb;">
                              <table border="0" cellpadding="0" cellspacing="0" width="100%%">
                                <tr>
                                  <td style="vertical-align: middle;">
                                    <div style="display: inline-block; width: 34px; height: 34px; border-radius: 8px; background-color: #2563eb; color: #ffffff; font-weight: 900; font-size: 14px; line-height: 34px; text-align: center; vertical-align: middle; margin-right: 10px;">ET</div>
                                    <span style="font-size: 18px; font-weight: 800; color: #ffffff; vertical-align: middle; letter-spacing: -0.5px;">%s</span>
                                    <div style="font-size: 10px; color: #94a3b8; margin-top: 4px; font-family: monospace;">
                                      Reg No: PV 00234512 &bull; VAT: 114589201-7000
                                    </div>
                                  </td>
                                  <td align="right" style="vertical-align: middle;">
                                    <span style="display: inline-block; padding: 6px 12px; background-color: rgba(37, 99, 235, 0.2); border: 1px solid rgba(59, 130, 246, 0.4); border-radius: 8px; font-size: 11px; font-weight: 800; color: #93c5fd; text-transform: uppercase; letter-spacing: 0.5px;">
                                      OFFICIAL TAX INVOICE
                                    </span>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Order & Payment Status Subheader -->
                          <tr>
                            <td style="background-color: #f8fafc; padding: 18px 32px; border-bottom: 1px solid #e2e8f0;">
                              <table border="0" cellpadding="0" cellspacing="0" width="100%%">
                                <tr>
                                  <td>
                                    <span style="font-size: 11px; color: #64748b; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;">Invoice / Order Code</span>
                                    <div style="font-size: 18px; font-weight: 900; color: #0f172a; font-family: monospace; margin-top: 2px;">
                                      #%s
                                    </div>
                                    <div style="font-size: 11px; color: #64748b; margin-top: 2px;">
                                      Placed on: %s
                                    </div>
                                  </td>
                                  <td align="right" style="vertical-align: middle;">
                                    <div>%s</div>
                                    <div style="font-size: 10px; color: #64748b; font-family: monospace; margin-top: 4px;">
                                      Ref: %s
                                    </div>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Customer Billing / Shipping & Branch Dispatch Grid -->
                          <tr>
                            <td style="padding: 24px 32px 16px 32px; border-bottom: 1px solid #f1f5f9;">
                              <table border="0" cellpadding="0" cellspacing="0" width="100%%">
                                <tr>
                                  <td width="50%%" style="vertical-align: top; padding-right: 16px;">
                                    <h4 style="margin: 0 0 8px 0; font-size: 11px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px;">
                                      Customer & Delivery Address
                                    </h4>
                                    <p style="margin: 0; font-size: 13px; font-weight: 700; color: #0f172a;">%s</p>
                                    <p style="margin: 4px 0 0 0; font-size: 12px; color: #475569; line-height: 1.4;">%s</p>
                                    <p style="margin: 4px 0 0 0; font-size: 12px; color: #64748b;">Phone: <strong style="color: #334155;">%s</strong></p>
                                    <p style="margin: 2px 0 0 0; font-size: 12px; color: #64748b;">Email: <strong style="color: #334155;">%s</strong></p>
                                  </td>
                                  <td width="50%%" style="vertical-align: top; padding-left: 16px; border-left: 1px solid #f1f5f9;">
                                    <h4 style="margin: 0 0 8px 0; font-size: 11px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px;">
                                      Fulfillment Hub & Payment
                                    </h4>
                                    <p style="margin: 0; font-size: 13px; font-weight: 700; color: #0f172a;">%s</p>
                                    <p style="margin: 4px 0 0 0; font-size: 12px; color: #475569; line-height: 1.4;">%s</p>
                                    <p style="margin: 4px 0 0 0; font-size: 12px; color: #64748b;">Branch Hotline: <strong style="color: #334155;">%s</strong></p>
                                    <p style="margin: 4px 0 0 0; font-size: 12px; color: #2563eb; font-weight: 600;">Payment: %s</p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Itemized Hardware Invoice Table -->
                          <tr>
                            <td style="padding: 20px 32px;">
                              <h4 style="margin: 0 0 12px 0; font-size: 11px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px;">
                                Itemized Hardware Breakdown
                              </h4>
                              <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="border-collapse: collapse; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden;">
                                <thead>
                                  <tr style="background-color: #0f172a; color: #ffffff;">
                                    <th align="left" style="padding: 10px 14px; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.5px;">Product / Item</th>
                                    <th align="center" style="padding: 10px 14px; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.5px; width: 60px;">Qty</th>
                                    <th align="right" style="padding: 10px 14px; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.5px; width: 110px;">Unit Price</th>
                                    <th align="right" style="padding: 10px 14px; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.5px; width: 110px;">Line Total</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  %s
                                </tbody>
                              </table>
                            </td>
                          </tr>

                          <!-- Financial Totals Table -->
                          <tr>
                            <td style="padding: 0 32px 24px 32px;">
                              <table border="0" cellpadding="0" cellspacing="0" width="100%%">
                                <tr>
                                  <td width="55%%" style="vertical-align: top; padding-right: 20px;">
                                    %s
                                  </td>
                                  <td width="45%%" style="vertical-align: top;">
                                    <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="font-size: 13px;">
                                      <tr>
                                        <td style="padding: 4px 0; color: #64748b;">Subtotal</td>
                                        <td align="right" style="padding: 4px 0; color: #0f172a; font-family: monospace; font-weight: 600;">Rs. %,.2f</td>
                                      </tr>
                                      <tr>
                                        <td style="padding: 4px 0; color: #64748b;">Insured Express Delivery</td>
                                        <td align="right" style="padding: 4px 0;">%s</td>
                                      </tr>
                                      <tr style="border-top: 2px solid #e2e8f0;">
                                        <td style="padding: 10px 0 0 0; font-weight: 800; color: #0f172a; font-size: 14px;">Total Amount</td>
                                        <td align="right" style="padding: 10px 0 0 0; font-weight: 900; color: #2563eb; font-size: 18px; font-family: monospace;">Rs. %,.2f</td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- CTA Button: Track Order Online -->
                          <tr>
                            <td align="center" style="padding: 10px 32px 28px 32px; border-bottom: 1px solid #f1f5f9;">
                              <a href="%s" style="display: inline-block; background-color: #2563eb; color: #ffffff; font-size: 13px; font-weight: 700; text-decoration: none; padding: 13px 28px; border-radius: 10px; box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);">
                                View & Track Your Order Online &rarr;
                              </a>
                              <p style="margin: 8px 0 0 0; font-size: 11px; color: #94a3b8;">
                                Log in anytime to track live delivery dispatch or view past order receipts.
                              </p>
                            </td>
                          </tr>

                          <!-- Footer & Company Details -->
                          <tr>
                            <td style="background-color: #f8fafc; padding: 24px 32px; text-align: center; font-size: 11px; color: #64748b;">
                              <p style="margin: 0 0 6px 0; font-weight: 700; color: #334155;">
                                %s (Pvt) Ltd &bull; Sri Lanka's Premier Computing Destination
                              </p>
                              <p style="margin: 0 0 6px 0;">
                                Head Office: 450 Galle Road, Colombo 03 &bull; Support: <a href="mailto:%s" style="color: #2563eb; text-decoration: none;">%s</a>
                              </p>
                              <p style="margin: 0; color: #94a3b8; font-size: 10px;">
                                This document serves as a valid computer-generated Tax Invoice and Proof of Purchase under the Inland Revenue Act of Sri Lanka.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                    subject,
                    effectiveName,
                    orderCode,
                    orderDateStr,
                    paymentBadgeHtml,
                    paymentRef,
                    customerName,
                    shippingAddress,
                    customerPhone,
                    toEmail,
                    branchName,
                    branchAddress,
                    branchPhone,
                    paymentMethod,
                    itemsHtml.toString(),
                    warrantyBoxHtml,
                    subtotal,
                    shippingHtml,
                    grandTotal,
                    trackingUrl,
                    effectiveName,
                    effectiveEmail,
                    effectiveEmail
                );

            sendGenericHtmlEmail(toEmail, subject, invoiceHtml);
            log.info("[EmailService] Order confirmation tax invoice successfully dispatched for order #{}", orderCode);
        } catch (Exception e) {
            log.error("[EmailService] Failed to dispatch order confirmation tax invoice: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendOrderDeliveredInvoice(Order order) {
        if (order == null || order.getCustomerEmail() == null || order.getCustomerEmail().isBlank()) {
            log.warn("[EmailService] Cannot send delivery invoice: Order or customer email is null");
            return;
        }

        try {
            String toEmail = order.getCustomerEmail().trim();
            String effectiveEmail = getEffectiveStoreEmail();
            String effectiveName = getEffectiveStoreName();
            String customerName = (order.getCustomerName() != null && !order.getCustomerName().isBlank())
                    ? order.getCustomerName().trim() : "Valued Customer";
            String orderCode = order.getOrderCode() != null ? order.getOrderCode().trim() : "N/A";
            String subject = "Order Delivered & Final Paid Tax Invoice: #" + orderCode + " | " + effectiveName;

            // Formatted Date
            String orderDateStr;
            if (order.getOrderDate() != null) {
                orderDateStr = order.getOrderDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));
            } else {
                orderDateStr = "Recent Order";
            }

            // Payment Details (Verified as PAID on delivery)
            String paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod().trim() : "Cash on Delivery";
            String paymentRef = order.getPaymentReference() != null ? order.getPaymentReference().trim() : "COD-REF-" + orderCode;
            String paymentBadgeHtml = "<span style=\"display: inline-block; padding: 4px 10px; background-color: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 9999px; color: #065f46; font-size: 11px; font-weight: 800; text-transform: uppercase; font-family: monospace;\">PAID &bull; VERIFIED</span>";

            // Branch Details
            Branch branch = order.getFulfillmentBranch();
            String branchName = branch != null ? branch.getName() : "Colombo Central SuperStore";
            String branchAddress = branch != null ? (branch.getAddress() + ", " + branch.getCity()) : "450 Galle Road, Colombo 03";
            String branchPhone = branch != null ? branch.getPhone() : "+94 11 234 5678";

            // Delivery Details
            String shippingAddress = (order.getShippingAddress() != null ? order.getShippingAddress().trim() : "") +
                    (order.getCity() != null ? ", " + order.getCity().trim() : "");
            String customerPhone = order.getCustomerPhone() != null ? order.getCustomerPhone().trim() : "N/A";

            // Items Table HTML Generation
            StringBuilder itemsHtml = new StringBuilder();
            Set<String> distinctWarranties = new LinkedHashSet<>();
            List<OrderItem> items = order.getItems();
            if (items != null && !items.isEmpty()) {
                for (OrderItem item : items) {
                    String pName = item.getProductName() != null ? item.getProductName() : "Hardware Component";
                    String sku = item.getProductSku() != null ? item.getProductSku() : "SKU-GEN";
                    int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                    BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                    BigDecimal totalPrice = item.getTotalPrice() != null ? item.getTotalPrice() : unitPrice.multiply(BigDecimal.valueOf(qty));

                    String itemWarranty = item.getWarranty();
                    if ((itemWarranty == null || itemWarranty.isBlank()) && item.getProduct() != null) {
                        itemWarranty = item.getProduct().getWarranty();
                    }
                    if (itemWarranty == null || itemWarranty.isBlank()) {
                        itemWarranty = "Official Hardware Warranty";
                    } else {
                        itemWarranty = itemWarranty.trim();
                    }
                    distinctWarranties.add(itemWarranty);

                    itemsHtml.append("""
                        <tr style="border-bottom: 1px solid #f1f5f9;">
                          <td style="padding: 12px 14px; vertical-align: middle;">
                            <strong style="color: #0f172a; font-size: 13px; display: block; margin-bottom: 3px;">%s</strong>
                            <div style="font-size: 11px; color: #64748b;">
                              <span style="font-family: monospace;">SKU: %s</span>
                              <span style="margin: 0 6px; color: #cbd5e1;">&bull;</span>
                              <span style="color: #0369a1; background-color: #f0f9ff; border: 1px solid #bae6fd; padding: 1px 7px; border-radius: 4px; font-weight: 600; font-size: 10px;">%s</span>
                            </div>
                          </td>
                          <td style="padding: 12px 14px; text-align: center; color: #334155; font-size: 13px; font-weight: 600; vertical-align: middle;">
                            %d
                          </td>
                          <td style="padding: 12px 14px; text-align: right; color: #334155; font-size: 13px; font-family: monospace; vertical-align: middle;">
                            Rs. %,.2f
                          </td>
                          <td style="padding: 12px 14px; text-align: right; color: #0f172a; font-size: 13px; font-weight: 700; font-family: monospace; vertical-align: middle;">
                            Rs. %,.2f
                          </td>
                        </tr>
                        """.formatted(pName, sku, itemWarranty, qty, unitPrice, totalPrice));
                }
            } else {
                itemsHtml.append("""
                    <tr>
                      <td colspan="4" style="padding: 16px; text-align: center; color: #64748b; font-size: 13px;">
                        Hardware components ordered
                      </td>
                    </tr>
                    """);
            }

            // Dynamic Warranty Card Box Generation
            String warrantyTitle;
            String warrantyDesc;
            if (distinctWarranties.isEmpty()) {
                warrantyTitle = "✔ Official Hardware Warranty Registered & Active";
                warrantyDesc = "Your hardware warranty is officially in effect as of today's delivery. All serial numbers are registered into our nationwide service network. Keep this receipt email for warranty claims or branch services.";
            } else if (distinctWarranties.size() == 1) {
                String singleWarranty = distinctWarranties.iterator().next();
                warrantyTitle = "✔ " + singleWarranty + " Registered & Active";
                warrantyDesc = "Your " + singleWarranty + " is officially in effect as of today's delivery. All hardware serial numbers are registered in our nationwide service network. Keep this receipt email for warranty claims or branch services.";
            } else {
                warrantyTitle = "✔ Official Hardware Warranty Active as of Delivery";
                StringBuilder sbDesc = new StringBuilder("All items are active under their respective warranties: ");
                int wCount = 0;
                for (String w : distinctWarranties) {
                    if (wCount > 0) sbDesc.append(", ");
                    sbDesc.append("<strong>").append(w).append("</strong>");
                    wCount++;
                }
                sbDesc.append(". Keep this receipt email for warranty claims or branch services.");
                warrantyDesc = sbDesc.toString();
            }

            String warrantyBoxHtml = """
                <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px; font-size: 11px; color: #475569; line-height: 1.5;">
                  <strong style="color: #0f172a; display: block; margin-bottom: 4px;">%s</strong>
                  %s
                </div>
                """.formatted(warrantyTitle, warrantyDesc);

            // Financial Summary
            BigDecimal subtotal = order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO;
            BigDecimal tax = order.getTax() != null ? order.getTax() : BigDecimal.ZERO;
            BigDecimal shipping = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
            BigDecimal grandTotal = order.getTotalAmount() != null ? order.getTotalAmount() : subtotal.add(tax).add(shipping);

            String shippingHtml;
            if (shipping.compareTo(BigDecimal.ZERO) == 0) {
                shippingHtml = "<span style=\"color: #059669; font-weight: 800; font-family: monospace;\">FREE</span> " +
                        "<span style=\"display: inline-block; padding: 2px 6px; background-color: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 4px; font-size: 9px; font-weight: 800; color: #065f46;\">PROMO APPLIED</span>";
            } else {
                shippingHtml = String.format("Rs. %,.2f", shipping);
            }

            String trackingUrl = "https://etechcomputers.vercel.app/#orders";

            // Full Tax Invoice HTML Document for Delivered & Paid Order
            String invoiceHtml = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased; color: #0f172a;">
                  <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="table-layout: fixed; background-color: #f1f5f9;">
                    <tr>
                      <td align="center" style="padding: 32px 16px;">
                        <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 640px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.08); border: 1px solid #e2e8f0;">
                          
                          <!-- Top Header: Legal Entity & Tax Header -->
                          <tr>
                            <td style="background: linear-gradient(135deg, #0f172a 0%%, #1e293b 100%%); padding: 24px 32px; border-bottom: 3px solid #10b981;">
                              <table border="0" cellpadding="0" cellspacing="0" width="100%%">
                                <tr>
                                  <td style="vertical-align: middle;">
                                    <div style="display: inline-block; width: 34px; height: 34px; border-radius: 8px; background-color: #10b981; color: #ffffff; font-weight: 900; font-size: 14px; line-height: 34px; text-align: center; vertical-align: middle; margin-right: 10px;">ET</div>
                                    <span style="font-size: 18px; font-weight: 800; color: #ffffff; vertical-align: middle; letter-spacing: -0.5px;">%s</span>
                                    <div style="font-size: 10px; color: #94a3b8; margin-top: 4px; font-family: monospace;">
                                      Reg No: PV 00234512 &bull; VAT: 114589201-7000
                                    </div>
                                  </td>
                                  <td align="right" style="vertical-align: middle;">
                                    <span style="display: inline-block; padding: 6px 12px; background-color: rgba(16, 185, 129, 0.2); border: 1px solid rgba(16, 185, 129, 0.4); border-radius: 8px; font-size: 11px; font-weight: 800; color: #6ee7b7; text-transform: uppercase; letter-spacing: 0.5px;">
                                      DELIVERED & PAID INVOICE
                                    </span>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Delivery Confirmation & Settlement Banner -->
                          <tr>
                            <td style="padding: 20px 32px 0 32px;">
                              <div style="background-color: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 12px; padding: 16px;">
                                <table border="0" cellpadding="0" cellspacing="0" width="100%%">
                                  <tr>
                                    <td width="30" style="vertical-align: top; font-size: 20px; line-height: 1; color: #059669;">✔</td>
                                    <td style="vertical-align: top; padding-left: 8px;">
                                      <strong style="color: #065f46; font-size: 13px; display: block;">Hardware Handover Confirmed & All Payments Settled</strong>
                                      <p style="margin: 4px 0 0 0; font-size: 12px; color: #047857; line-height: 1.45;">
                                        Your order <strong>#%s</strong> has been successfully delivered. Any Cash on Delivery dues have been collected and verified as <strong>PAID in full</strong>. Your official ETech Hardware Warranty coverage is now active.
                                      </p>
                                    </td>
                                  </tr>
                                </table>
                              </div>
                            </td>
                          </tr>

                          <!-- Order & Payment Status Subheader -->
                          <tr>
                            <td style="padding: 18px 32px; border-bottom: 1px solid #e2e8f0;">
                              <table border="0" cellpadding="0" cellspacing="0" width="100%%">
                                <tr>
                                  <td>
                                    <span style="font-size: 11px; color: #64748b; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px;">Order Code</span>
                                    <div style="font-size: 18px; font-weight: 900; color: #0f172a; font-family: monospace; margin-top: 2px;">
                                      #%s
                                    </div>
                                    <div style="font-size: 11px; color: #64748b; margin-top: 2px;">
                                      Placed on: %s
                                    </div>
                                  </td>
                                  <td align="right" style="vertical-align: middle;">
                                    <div>%s</div>
                                    <div style="font-size: 10px; color: #64748b; font-family: monospace; margin-top: 4px;">
                                      Ref: %s
                                    </div>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Customer Billing / Shipping & Branch Dispatch Grid -->
                          <tr>
                            <td style="padding: 24px 32px 16px 32px; border-bottom: 1px solid #f1f5f9;">
                              <table border="0" cellpadding="0" cellspacing="0" width="100%%">
                                <tr>
                                  <td width="50%%" style="vertical-align: top; padding-right: 16px;">
                                    <h4 style="margin: 0 0 8px 0; font-size: 11px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px;">
                                      Customer & Delivery Address
                                    </h4>
                                    <p style="margin: 0; font-size: 13px; font-weight: 700; color: #0f172a;">%s</p>
                                    <p style="margin: 4px 0 0 0; font-size: 12px; color: #475569; line-height: 1.4;">%s</p>
                                    <p style="margin: 4px 0 0 0; font-size: 12px; color: #64748b;">Phone: <strong style="color: #334155;">%s</strong></p>
                                    <p style="margin: 2px 0 0 0; font-size: 12px; color: #64748b;">Email: <strong style="color: #334155;">%s</strong></p>
                                  </td>
                                  <td width="50%%" style="vertical-align: top; padding-left: 16px; border-left: 1px solid #f1f5f9;">
                                    <h4 style="margin: 0 0 8px 0; font-size: 11px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px;">
                                      Fulfillment Hub & Payment
                                    </h4>
                                    <p style="margin: 0; font-size: 13px; font-weight: 700; color: #0f172a;">%s</p>
                                    <p style="margin: 4px 0 0 0; font-size: 12px; color: #475569; line-height: 1.4;">%s</p>
                                    <p style="margin: 4px 0 0 0; font-size: 12px; color: #64748b;">Branch Hotline: <strong style="color: #334155;">%s</strong></p>
                                    <p style="margin: 4px 0 0 0; font-size: 12px; color: #059669; font-weight: 700;">Payment: %s (PAID)</p>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Itemized Hardware Invoice Table -->
                          <tr>
                            <td style="padding: 20px 32px;">
                              <h4 style="margin: 0 0 12px 0; font-size: 11px; font-weight: 800; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px;">
                                Itemized Hardware Breakdown
                              </h4>
                              <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="border-collapse: collapse; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden;">
                                <thead>
                                  <tr style="background-color: #0f172a; color: #ffffff;">
                                    <th align="left" style="padding: 10px 14px; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.5px;">Product / Item</th>
                                    <th align="center" style="padding: 10px 14px; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.5px; width: 60px;">Qty</th>
                                    <th align="right" style="padding: 10px 14px; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.5px; width: 110px;">Unit Price</th>
                                    <th align="right" style="padding: 10px 14px; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.5px; width: 110px;">Line Total</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  %s
                                </tbody>
                              </table>
                            </td>
                          </tr>

                          <!-- Financial Totals Table -->
                          <tr>
                            <td style="padding: 0 32px 24px 32px;">
                              <table border="0" cellpadding="0" cellspacing="0" width="100%%">
                                <tr>
                                  <td width="55%%" style="vertical-align: top; padding-right: 20px;">
                                    %s
                                  </td>
                                  <td width="45%%" style="vertical-align: top;">
                                    <table border="0" cellpadding="0" cellspacing="0" width="100%%" style="font-size: 13px;">
                                      <tr>
                                        <td style="padding: 4px 0; color: #64748b;">Subtotal</td>
                                        <td align="right" style="padding: 4px 0; color: #0f172a; font-family: monospace; font-weight: 600;">Rs. %,.2f</td>
                                      </tr>
                                      <tr>
                                        <td style="padding: 4px 0; color: #64748b;">Insured Express Delivery</td>
                                        <td align="right" style="padding: 4px 0;">%s</td>
                                      </tr>
                                      <tr style="border-top: 2px solid #e2e8f0;">
                                        <td style="padding: 10px 0 0 0; font-weight: 800; color: #0f172a; font-size: 14px;">Total Settled</td>
                                        <td align="right" style="padding: 10px 0 0 0; font-weight: 900; color: #059669; font-size: 18px; font-family: monospace;">Rs. %,.2f</td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- CTA Button: Track Order Online -->
                          <tr>
                            <td align="center" style="padding: 10px 32px 28px 32px; border-bottom: 1px solid #f1f5f9;">
                              <a href="%s" style="display: inline-block; background-color: #059669; color: #ffffff; font-size: 13px; font-weight: 700; text-decoration: none; padding: 13px 28px; border-radius: 10px; box-shadow: 0 4px 12px rgba(5, 150, 105, 0.25);">
                                View Order & Download Receipt &rarr;
                              </a>
                              <p style="margin: 8px 0 0 0; font-size: 11px; color: #94a3b8;">
                                Log in anytime to review past purchases or download invoices.
                              </p>
                            </td>
                          </tr>

                          <!-- Footer & Company Details -->
                          <tr>
                            <td style="background-color: #f8fafc; padding: 24px 32px; text-align: center; font-size: 11px; color: #64748b;">
                              <p style="margin: 0 0 6px 0; font-weight: 700; color: #334155;">
                                %s (Pvt) Ltd &bull; Sri Lanka's Premier Computing Destination
                              </p>
                              <p style="margin: 0 0 6px 0;">
                                Head Office: 450 Galle Road, Colombo 03 &bull; Support: <a href="mailto:%s" style="color: #2563eb; text-decoration: none;">%s</a>
                              </p>
                              <p style="margin: 0; color: #94a3b8; font-size: 10px;">
                                This document serves as a valid computer-generated Tax Invoice and Final Proof of Payment under the Inland Revenue Act of Sri Lanka.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                    subject,
                    effectiveName,
                    orderCode,
                    orderCode,
                    orderDateStr,
                    paymentBadgeHtml,
                    paymentRef,
                    customerName,
                    shippingAddress,
                    customerPhone,
                    toEmail,
                    branchName,
                    branchAddress,
                    branchPhone,
                    paymentMethod,
                    itemsHtml.toString(),
                    warrantyBoxHtml,
                    subtotal,
                    shippingHtml,
                    grandTotal,
                    trackingUrl,
                    effectiveName,
                    effectiveEmail,
                    effectiveEmail
                );

            sendGenericHtmlEmail(toEmail, subject, invoiceHtml);
            log.info("[EmailService] Order delivered invoice successfully dispatched for order #{}", orderCode);
        } catch (Exception e) {
            log.error("[EmailService] Failed to dispatch order delivered invoice: {}", e.getMessage(), e);
        }
    }

}


