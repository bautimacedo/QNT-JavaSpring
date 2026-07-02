package com.gestion.qnt.service;

import com.gestion.qnt.model.Ticket;
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

    @Value("${app.tickets.admin-email:bautimrf@gmail.com}")
    private String ticketsAdminEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${app.reportes.actividades-emails:}")
    private String reportesActividadesEmails;

    /** Envía un reporte de actividades (PDF adjunto) a la lista configurada de destinatarios. */
    public void sendReporteActividades(String asunto, String titulo, String periodo,
                                       byte[] pdf, String nombrePdf) {
        String[] destinatarios = java.util.Arrays.stream(reportesActividadesEmails.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        if (destinatarios.length == 0) {
            log.warn("No hay destinatarios configurados (app.reportes.actividades-emails); no se envía el reporte '{}'", titulo);
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromAddress);
            h.setTo(destinatarios);
            h.setSubject(asunto);
            h.setText(baseHtmlWrapper(titulo, periodo,
                    "<p style=\"font-size:14px;color:#333;\">Adjuntamos el reporte de actividades en PDF.</p>"), true);
            h.addAttachment(nombrePdf, new org.springframework.core.io.ByteArrayResource(pdf));
            mailSender.send(msg);
            log.info("Reporte de actividades '{}' enviado a {} destinatario(s)", titulo, destinatarios.length);
        } catch (MessagingException e) {
            log.error("Error enviando el reporte de actividades '{}'", titulo, e);
        }
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

    public void sendTicketCreatedAdmin(Ticket ticket) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromAddress);
            h.setTo(ticketsAdminEmail);
            h.setSubject("[QNT] Nuevo ticket #" + ticket.getId() + " — " + ticket.getTitulo());
            h.setText(buildTicketAdminHtml(ticket), true);
            mailSender.send(msg);
            log.info("Mail de nuevo ticket #{} enviado a admin", ticket.getId());
        } catch (MessagingException e) {
            log.error("Error enviando mail de ticket #{} a admin", ticket.getId(), e);
        }
    }

    public void sendTicketResolvedToAutor(Ticket ticket) {
        String toEmail = ticket.getAutor().getEmail();
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(fromAddress);
            h.setTo(toEmail);
            h.setSubject("[QNT] Tu ticket #" + ticket.getId() + " fue actualizado");
            h.setText(buildTicketResolvedHtml(ticket), true);
            mailSender.send(msg);
            log.info("Mail de resolución de ticket #{} enviado a {}", ticket.getId(), toEmail);
        } catch (MessagingException e) {
            log.error("Error enviando mail de resolución de ticket #{} a {}", ticket.getId(), toEmail, e);
        }
    }

    private String buildTicketAdminHtml(Ticket ticket) {
        String autor = ticket.getAutor().getNombre() + " " +
                (ticket.getAutor().getApellido() != null ? ticket.getAutor().getApellido() : "");
        return baseHtmlWrapper(
            "Nuevo ticket reportado",
            "Se ha registrado un nuevo ticket en el sistema.",
            "<table style=\"width:100%;border-collapse:collapse;margin-bottom:24px;\">" +
            row("ID", "#" + ticket.getId()) +
            row("Título", ticket.getTitulo()) +
            row("Descripción", ticket.getDescripcion() != null ? ticket.getDescripcion() : "—") +
            row("Autor", autor + " &lt;" + ticket.getAutor().getEmail() + "&gt;") +
            row("Estado", "ABIERTO") +
            row("Fecha", ticket.getCreatedAt().toString()) +
            "</table>" +
            "<a href=\"" + frontendUrl + "/home/tickets\" " +
            "style=\"display:inline-block;padding:13px 32px;background:linear-gradient(135deg,#113e4c,#2b555b);" +
            "color:#fff;text-decoration:none;border-radius:10px;font-weight:700;font-size:14px;\">Ver tickets</a>"
        );
    }

    private String buildTicketResolvedHtml(Ticket ticket) {
        String resolver = ticket.getResolvedBy() != null
            ? ticket.getResolvedBy().getNombre() + " " +
              (ticket.getResolvedBy().getApellido() != null ? ticket.getResolvedBy().getApellido() : "")
            : "—";
        return baseHtmlWrapper(
            "Tu ticket fue actualizado",
            "El estado de tu ticket ha sido modificado.",
            "<table style=\"width:100%;border-collapse:collapse;margin-bottom:24px;\">" +
            row("ID", "#" + ticket.getId()) +
            row("Título", ticket.getTitulo()) +
            row("Estado", ticket.getEstado().name()) +
            row("Nota de resolución", ticket.getNotaResolucion() != null ? ticket.getNotaResolucion() : "—") +
            row("Resuelto por", resolver) +
            row("Fecha", ticket.getUpdatedAt().toString()) +
            "</table>"
        );
    }

    private static String row(String label, String value) {
        return "<tr>" +
               "<td style=\"padding:8px 12px;background:#f0f6f6;font-size:13px;font-weight:600;color:#2b555b;" +
               "border-radius:4px;white-space:nowrap;\">" + label + "</td>" +
               "<td style=\"padding:8px 12px;font-size:13px;color:#333;\">" + value + "</td>" +
               "</tr>";
    }

    private String baseHtmlWrapper(String title, String subtitle, String content) {
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"></head>" +
        "<body style=\"margin:0;padding:0;background:#f0f4f4;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;\">" +
        "<div style=\"height:4px;background:linear-gradient(90deg,#113e4c,#2b555b,#658582);\"></div>" +
        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f0f4f4;padding:40px 16px;\">" +
        "<tr><td align=\"center\"><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;\">" +
        "<tr><td style=\"padding-bottom:24px;text-align:center;\">" +
        "<img src=\"https://qntdrones.com/Qnt_Logo.png\" alt=\"QNT Drones\" width=\"60\" height=\"60\" style=\"display:block;margin:0 auto 8px;border-radius:50%;\">" +
        "<div style=\"font-size:16px;font-weight:700;color:#113e4c;\">QNT DRONES</div>" +
        "</td></tr>" +
        "<tr><td style=\"background:#fff;border-radius:16px;box-shadow:0 4px 32px rgba(17,62,76,.10);border:1px solid #dce8e8;overflow:hidden;\">" +
        "<div style=\"height:6px;background:linear-gradient(90deg,#113e4c,#2b555b,#658582);\"></div>" +
        "<div style=\"padding:36px 40px;\">" +
        "<h1 style=\"margin:0 0 8px;font-size:22px;font-weight:700;color:#113e4c;\">" + title + "</h1>" +
        "<p style=\"margin:0 0 24px;font-size:14px;color:#536c6b;\">" + subtitle + "</p>" +
        content +
        "</div>" +
        "<div style=\"padding:16px 40px 24px;border-top:1px solid #e8f0f0;\">" +
        "<p style=\"margin:0;font-size:12px;color:#8aabaa;\">Este es un correo automático, por favor no respondas a este mensaje.</p>" +
        "</div>" +
        "</td></tr>" +
        "</table></td></tr></table></body></html>";
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
