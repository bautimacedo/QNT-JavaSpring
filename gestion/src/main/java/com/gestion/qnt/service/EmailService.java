package com.gestion.qnt.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontend-url:https://qntdrones.com}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("QNT Drones – Recuperar contraseña");
            helper.setText(buildHtml(resetLink), true);
            mailSender.send(mimeMessage);
            log.info("Email de recuperación enviado a {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error enviando email de recuperación a {}", toEmail, e);
            throw new RuntimeException("Error al enviar el email de recuperación", e);
        }
    }

    private String buildHtml(String resetLink) {
        return "<!DOCTYPE html>" +
        "<html lang=\"es\">" +
        "<head>" +
        "<meta charset=\"UTF-8\">" +
        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
        "<title>Recuperar contraseña – QNT Drones</title>" +
        "</head>" +
        "<body style=\"margin:0;padding:0;background:#f0f4f4;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;\">" +

        // Top accent bar
        "<div style=\"height:4px;background:linear-gradient(90deg,#113e4c 0%,#2b555b 50%,#658582 100%);\"></div>" +

        // Wrapper
        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f0f4f4;padding:40px 16px;\">" +
        "<tr><td align=\"center\">" +
        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;\">" +

        // Logo header
        "<tr><td style=\"padding-bottom:28px;text-align:center;\">" +
        "<img src=\"https://qntdrones.com/Qnt_Logo.png\" alt=\"QNT Drones\" width=\"72\" height=\"72\"" +
             " style=\"display:block;margin:0 auto 10px;border-radius:50%;\">" +
        "<div style=\"font-size:17px;font-weight:700;color:#113e4c;letter-spacing:.06em;\">QNT DRONES</div>" +
        "<div style=\"font-size:11px;color:#658582;letter-spacing:.03em;\">Sistema de Gestión de Flota</div>" +
        "</td></tr>" +

        // Main card
        "<tr><td style=\"background:#fff;border-radius:16px;box-shadow:0 4px 32px rgba(17,62,76,.10);border:1px solid #dce8e8;overflow:hidden;\">" +

        // Card top gradient
        "<div style=\"height:6px;background:linear-gradient(90deg,#113e4c 0%,#2b555b 50%,#658582 100%);\"></div>" +

        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">" +
        "<tr><td style=\"padding:40px 40px 32px;\">" +

        // Title
        "<h1 style=\"margin:0 0 8px;font-size:26px;font-weight:700;color:#113e4c;line-height:1.2;\">Recuperar contraseña</h1>" +
        "<p style=\"margin:0 0 28px;font-size:15px;color:#536c6b;line-height:1.6;\">" +
        "Recibimos una solicitud para restablecer la contraseña de tu cuenta.<br>" +
        "Hacé clic en el botón de abajo para crear una nueva contraseña." +
        "</p>" +

        // CTA Button
        "<table cellpadding=\"0\" cellspacing=\"0\" style=\"margin-bottom:28px;\">" +
        "<tr><td style=\"border-radius:12px;background:linear-gradient(135deg,#113e4c 0%,#2b555b 100%);" +
             "box-shadow:0 6px 20px rgba(17,62,76,.32);\">" +
        "<a href=\"" + resetLink + "\" " +
           "style=\"display:inline-block;padding:15px 36px;font-size:15px;font-weight:700;color:#fff;" +
                  "text-decoration:none;letter-spacing:.03em;\">Restablecer contraseña</a>" +
        "</td></tr></table>" +

        // Expiry notice
        "<table cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;margin-bottom:28px;\">" +
        "<tr>" +
        "<td style=\"width:4px;background:linear-gradient(180deg,#113e4c,#658582);border-radius:4px;\"></td>" +
        "<td style=\"padding:12px 16px;background:#f0f6f6;border-radius:0 8px 8px 0;\">" +
        "<span style=\"font-size:13px;color:#2b555b;font-weight:600;\">&#9200; Este enlace expira en 1 hora.</span><br>" +
        "<span style=\"font-size:12px;color:#658582;\">Por seguridad, el link solo puede usarse una vez.</span>" +
        "</td>" +
        "</tr></table>" +

        // URL fallback
        "<p style=\"margin:0 0 6px;font-size:12px;color:#8aabaa;\">Si el botón no funciona, copiá este enlace en tu navegador:</p>" +
        "<p style=\"margin:0;font-size:11px;word-break:break-all;\">" +
        "<a href=\"" + resetLink + "\" style=\"color:#2b555b;text-decoration:underline;\">" + resetLink + "</a>" +
        "</p>" +

        "</td></tr>" +

        // Card footer divider
        "<tr><td style=\"padding:0 40px;\">" +
        "<hr style=\"border:none;border-top:1px solid #e8f0f0;margin:0;\">" +
        "</td></tr>" +

        // Ignore notice
        "<tr><td style=\"padding:20px 40px 32px;\">" +
        "<p style=\"margin:0;font-size:13px;color:#8aabaa;line-height:1.6;\">" +
        "<strong style=\"color:#b0c8c8;\">&#128274; No solicitaste esto?</strong><br>" +
        "Podés ignorar este correo de forma segura. Tu contraseña no será modificada." +
        "</p>" +
        "</td></tr>" +

        "</table>" +
        "</td></tr>" +

        // Footer
        "<tr><td style=\"padding:24px 0 8px;text-align:center;\">" +
        "<p style=\"margin:0 0 6px;font-size:12px;color:#a0b8b8;\">&#128640; <strong>QNT Drones</strong> · Parte de Quintana Energy</p>" +
        "<p style=\"margin:0;font-size:11px;color:#b8cccc;\">Este es un correo automático, por favor no respondas a este mensaje.</p>" +
        "</td></tr>" +

        "</table>" +
        "</td></tr></table>" +

        "</body></html>";
    }
}
