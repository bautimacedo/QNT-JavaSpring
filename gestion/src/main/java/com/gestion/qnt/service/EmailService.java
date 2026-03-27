package com.gestion.qnt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("QNT Drones – Recuperar contraseña");
        message.setText(
            "Hola,\n\n" +
            "Recibimos una solicitud para restablecer la contraseña de tu cuenta en QNT Drones.\n\n" +
            "Hacé clic en el siguiente enlace para crear una nueva contraseña:\n\n" +
            resetLink + "\n\n" +
            "Este enlace expira en 1 hora.\n\n" +
            "Si no solicitaste esto, podés ignorar este correo.\n\n" +
            "— Equipo QNT Drones"
        );

        try {
            mailSender.send(message);
            log.info("Email de recuperación enviado a {}", toEmail);
        } catch (Exception e) {
            log.error("Error enviando email de recuperación a {}", toEmail, e);
            throw e;
        }
    }
}
