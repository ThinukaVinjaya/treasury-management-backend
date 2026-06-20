package com.batch.treasury_management.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Send beautiful HTML email
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("cst19treasurysystem@gmail.com");
            helper.setText(htmlContent, true);   // true = HTML

            mailSender.send(message);
            System.out.println("✅ HTML Email sent to: " + to);
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send HTML email to " + to + " | " + e.getMessage());
        }
    }

    /**
     * Send HTML broadcast to multiple users
     */
    public void sendHtmlBroadcast(List<String> toList, String subject, String message) {
        if (toList == null || toList.isEmpty()) return;

        String htmlContent = buildBeautifulEmailTemplate(message);

        for (String email : toList) {
            if (email != null && !email.trim().isEmpty()) {
                sendHtmlEmail(email.trim(), subject, htmlContent);
            }
        }
    }

    /**
     * Beautiful Professional Email Template
     */
    private String buildBeautifulEmailTemplate(String message) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background: #f4f7fa; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #1e3a8a, #3b82f6); color: white; padding: 30px 20px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; }
                    .content { padding: 40px 30px; line-height: 1.7; color: #333; font-size: 16px; }
                    .footer { background: #f8fafc; padding: 25px; text-align: center; color: #64748b; font-size: 14px; border-top: 1px solid #e2e8f0; }
                    .button { display: inline-block; background: #3b82f6; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🏛️ University Batch Treasury</h1>
                    </div>
                    <div class="content">
                        %s
                    </div>
                    <div class="footer">
                        <p>University Batch Treasury Management System</p>
                        <p>This is an official communication. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(message.replace("\n", "<br>"));
    }

    // Keep old methods for backward compatibility
    public void sendSimpleEmail(String to, String subject, String text) {
        sendHtmlEmail(to, subject, "<p>" + text.replace("\n", "<br>") + "</p>");
    }

    public void sendEmailToMultiple(List<String> toList, String subject, String text) {
        sendHtmlBroadcast(toList, subject, text);
    }

    public void sendContributionReminder(String to, String fullName, String month) {
        String message = "Dear " + fullName + ",<br><br>" +
                "This is a friendly reminder that your monthly contribution for <strong>" + month +
                "</strong> is still pending.<br><br>" +
                "Please make the payment at the earliest possible.<br><br>" +
                "Thank you for your continued support!<br><br>" +
                "Best Regards,<br>University Batch Treasury Team";

        sendHtmlEmail(to, "📢 Contribution Reminder - " + month, message);
    }

    public void sendForgotPasswordOtp(String to, String fullName, String otp) {
        String subject = "🔑 Forgot Password Verification Code";

        String htmlContent = """
        <h2>Forgot Password Request</h2>
        <p>Dear <b>%s</b>,</p>
        <p>Your password reset verification code is:</p>
        <h1 style="color:#006400; font-size:32px; letter-spacing:4px;">%s</h1>
        <p>This code is valid for <b>10 minutes</b>.</p>
        <p>If you did not request this, please ignore this email.</p>
        <br>
        <p>Best Regards,<br>University Batch Treasury Team</p>
        """.formatted(fullName, otp);

        sendHtmlEmail(to, subject, htmlContent);
    }
}