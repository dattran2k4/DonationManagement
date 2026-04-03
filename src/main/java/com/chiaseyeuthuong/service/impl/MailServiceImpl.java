package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "MAIL-SERVICE")
public class MailServiceImpl implements MailService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int LOOKUP_CODE_EXPIRED_MINUTES = 10;
    private static final String MAIL_FROM_NAME = "CLB Chia Sẻ Yêu Thương";
    private final JavaMailSender javaMailSender;
    private final Map<String, VerificationCodeInfo> verificationCodeStore = new ConcurrentHashMap<>();

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationCodeMail(String to) {
        String code = generateVerificationCode();
        storeVerificationCode(to, code);
        String subject = "Mã xác nhận tra cứu quyên góp";
        String html = buildVerificationCodeEmail(code);
        sendVerificationHtmlMailWithInlineLogo(to, subject, html);
    }

    private String generateVerificationCode() {
        int code = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendVerificationCodeMailAsync(String to) {
        try {
            sendVerificationCodeMail(to);
        } catch (Exception ex) {
            log.error("Cannot send verification code mail async to {} caused: {}", to, ex.getMessage(), ex);
        }
    }

    @Override
    public boolean verifyLookupCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);

        VerificationCodeInfo codeInfo = verificationCodeStore.get(normalizedEmail);
        if (codeInfo == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(codeInfo.expiredAt())) {
            verificationCodeStore.remove(normalizedEmail);
            return false;
        }

        return codeInfo.code().equals(code.trim());
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendDonationThankYouMailAsync(String to, String donorName, String memoCode, String targetTitle, String amountText) {
        try {
            if (!StringUtils.hasText(to)) {
                return;
            }
            String subject = "Cảm ơn bạn đã quyên góp";
            String html = buildDonationThankYouEmail(donorName, memoCode, targetTitle, amountText);
            sendVerificationHtmlMailWithInlineLogo(to, subject, html);
        } catch (Exception ex) {
            log.error("Cannot send thank-you mail async to {} caused: {}", to, ex.getMessage(), ex);
        }
    }

    private void storeVerificationCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        if (!StringUtils.hasText(normalizedEmail)) {
            return;
        }

        verificationCodeStore.put(normalizedEmail, new VerificationCodeInfo(code, LocalDateTime.now().plusMinutes(LOOKUP_CODE_EXPIRED_MINUTES)));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String buildVerificationCodeEmail(String code) {
        return """
                <!doctype html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Mã xác nhận</title>
                </head>
                <body style="margin:0;padding:0;background:#f3f6f9;font-family:Inter,Arial,sans-serif;color:#1f2937;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f3f6f9;padding:24px 0;">
                    <tr>
                        <td align="center">
                            <table role="presentation" width="620" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border:1px solid #e5e7eb;border-radius:16px;overflow:hidden;">
                                <tr>
                                    <td align="center" style="padding:28px 24px 10px;">
                                        <img src="cid:projectLogo" alt="Logo dự án" width="64" height="64" style="display:block;width:64px;height:64px;border-radius:12px;object-fit:cover;">
                                    </td>
                                </tr>
                                <tr>
                                    <td align="center" style="padding:0 24px 8px;">
                                        <h1 style="margin:0;font-size:24px;line-height:32px;color:#111827;font-weight:800;">Xác nhận tra cứu quyên góp</h1>
                                    </td>
                                </tr>
                                <tr>
                                    <td align="center" style="padding:0 24px 18px;">
                                        <p style="margin:0;font-size:15px;line-height:24px;color:#6b7280;">
                                            Vui lòng sử dụng mã xác nhận dưới đây để tiếp tục tra cứu lịch sử quyên góp.
                                        </p>
                                    </td>
                                </tr>
                                <tr>
                                    <td align="center" style="padding:0 24px 24px;">
                                        <div style="display:inline-block;padding:14px 28px;border-radius:12px;background:#ecfdf3;border:1px dashed #34d399;">
                                            <span style="letter-spacing:8px;font-size:36px;line-height:42px;font-weight:800;color:#059669;">%s</span>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td align="center" style="padding:0 24px 12px;">
                                        <p style="margin:0;font-size:14px;line-height:22px;color:#4b5563;">
                                            Mã có hiệu lực trong <strong>10 phút</strong>. Không chia sẻ mã cho người khác.
                                        </p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding:12px 24px 24px;">
                                        <div style="height:1px;background:#e5e7eb;"></div>
                                        <p style="margin:14px 0 0;font-size:12px;line-height:20px;color:#9ca3af;text-align:center;">
                                            Email tự động từ hệ thống CLB Chia sẻ Yêu Thương.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
                </body>
                </html>
                """.formatted(code);
    }

    private String buildDonationThankYouEmail(String donorName, String memoCode, String targetTitle, String amountText) {
        String safeDonorName = StringUtils.hasText(donorName) ? donorName : "Nhà hảo tâm";
        String safeMemoCode = StringUtils.hasText(memoCode) ? memoCode : "---";
        String safeTargetTitle = StringUtils.hasText(targetTitle) ? targetTitle : "Không gắn mục tiêu";
        String safeAmountText = StringUtils.hasText(amountText) ? amountText : "---";

        return """
                <!doctype html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Thư cảm ơn</title>
                </head>
                <body style="margin:0;padding:0;background:#f3f6f9;font-family:Inter,Arial,sans-serif;color:#1f2937;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f3f6f9;padding:24px 0;">
                    <tr>
                        <td align="center">
                            <table role="presentation" width="620" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border:1px solid #e5e7eb;border-radius:16px;overflow:hidden;">
                                <tr>
                                    <td align="center" style="padding:28px 24px 10px;">
                                        <img src="cid:projectLogo" alt="Logo dự án" width="64" height="64" style="display:block;width:64px;height:64px;border-radius:12px;object-fit:cover;">
                                    </td>
                                </tr>
                                <tr>
                                    <td align="center" style="padding:0 24px 8px;">
                                        <h1 style="margin:0;font-size:24px;line-height:32px;color:#111827;font-weight:800;">Cảm ơn bạn đã đồng hành</h1>
                                    </td>
                                </tr>
                                <tr>
                                    <td align="left" style="padding:0 24px 16px;">
                                        <p style="margin:0 0 10px;font-size:15px;line-height:24px;color:#374151;">
                                            Chào <strong>%s</strong>,
                                        </p>
                                        <p style="margin:0;font-size:15px;line-height:24px;color:#374151;">
                                            Khoản quyên góp của bạn đã được xác nhận thành công. CLB Chia Sẻ Yêu Thương chân thành cảm ơn sự tin tưởng và sẻ chia của bạn.
                                        </p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding:0 24px 20px;">
                                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border:1px solid #e5e7eb;border-radius:12px;background:#f9fafb;">
                                            <tr>
                                                <td style="padding:14px 16px;font-size:14px;color:#6b7280;">Mã quyên góp</td>
                                                <td style="padding:14px 16px;font-size:14px;color:#111827;font-weight:600;text-align:right;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding:14px 16px;font-size:14px;color:#6b7280;border-top:1px solid #e5e7eb;">Mục tiêu</td>
                                                <td style="padding:14px 16px;font-size:14px;color:#111827;font-weight:600;text-align:right;border-top:1px solid #e5e7eb;">%s</td>
                                            </tr>
                                            <tr>
                                                <td style="padding:14px 16px;font-size:14px;color:#6b7280;border-top:1px solid #e5e7eb;">Số tiền</td>
                                                <td style="padding:14px 16px;font-size:14px;color:#059669;font-weight:700;text-align:right;border-top:1px solid #e5e7eb;">%s</td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding:0 24px 24px;">
                                        <div style="height:1px;background:#e5e7eb;"></div>
                                        <p style="margin:14px 0 0;font-size:12px;line-height:20px;color:#9ca3af;text-align:center;">
                                            Email tự động từ hệ thống CLB Chia Sẻ Yêu Thương.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
                </body>
                </html>
                """.formatted(safeDonorName, safeMemoCode, safeTargetTitle, safeAmountText);
    }

    private void sendVerificationHtmlMailWithInlineLogo(String to, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail, MAIL_FROM_NAME);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            ClassPathResource logoResource = new ClassPathResource("static/images/logo.jpg");
            helper.addInline("projectLogo", logoResource, "image/jpeg");

            javaMailSender.send(mimeMessage);
        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error("Cannot send verification email with inline logo to {} caused: {}", to, ex.getMessage(), ex);
            throw new RuntimeException("Không thể gửi email xác nhận", ex);
        }
    }

    private record VerificationCodeInfo(String code, LocalDateTime expiredAt) {
    }
}
