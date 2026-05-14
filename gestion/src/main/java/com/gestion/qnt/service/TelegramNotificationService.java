package com.gestion.qnt.service;

import com.gestion.qnt.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final String botToken;
    private final Long groupChatId;
    private final RestClient restClient;
    private final UsuarioRepository usuarioRepository;

    public TelegramNotificationService(
            @Value("${telegram.bot-token:}") String botToken,
            @Value("${telegram.group-chat-id:0}") Long groupChatId,
            UsuarioRepository usuarioRepository) {
        this.botToken     = botToken;
        this.groupChatId  = groupChatId;
        this.usuarioRepository = usuarioRepository;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.telegram.org")
                .build();
    }

    /** Envía al grupo operativo y a todos los usuarios con telegramUserId registrado. */
    public void notifyAll(String text) {
        if (botToken.isBlank()) {
            log.debug("Telegram deshabilitado: TELEGRAM_BOT_TOKEN no configurado");
            return;
        }
        // Grupo principal
        if (groupChatId != 0) send(groupChatId, text);
        // Usuarios individuales
        usuarioRepository.findAll().stream()
                .filter(u -> u.getTelegramUserId() != null)
                .forEach(u -> send(u.getTelegramUserId(), text));
    }

    public void send(Long chatId, String text) {
        if (botToken.isBlank() || chatId == null) return;
        try {
            restClient.post()
                    .uri("/bot{token}/sendMessage", botToken)
                    .body(Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Error enviando mensaje Telegram a {}: {}", chatId, e.getMessage());
        }
    }
}
