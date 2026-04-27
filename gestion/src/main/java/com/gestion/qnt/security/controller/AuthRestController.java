package com.gestion.qnt.security.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.AuthMeResponse;
import com.gestion.qnt.debug.DebugLog;
import com.gestion.qnt.model.PasswordResetToken;
import com.gestion.qnt.repository.PasswordResetTokenRepository;
import com.gestion.qnt.security.AuthConstants;
import com.gestion.qnt.security.AuthUser;
import com.gestion.qnt.security.custom.CustomAuthenticationManager;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.enums.EstadoUsuario;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IUsuarioBusiness;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.FoundException;
import com.gestion.qnt.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiConstants.URL_BASE)
@lombok.extern.slf4j.Slf4j
public class AuthRestController {

    private final CustomAuthenticationManager customAuthenticationManager;
    private final AuthConstants authConstants;
    private final PasswordEncoder passwordEncoder;
    private final IUsuarioBusiness usuarioBusiness;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${app.password-reset.expiration-minutes:60}")
    private long expirationMinutes;

    public AuthRestController(CustomAuthenticationManager customAuthenticationManager,
                              AuthConstants authConstants,
                              PasswordEncoder passwordEncoder,
                              IUsuarioBusiness usuarioBusiness,
                              PasswordResetTokenRepository tokenRepository,
                              EmailService emailService) {
        this.customAuthenticationManager = customAuthenticationManager;
        this.authConstants = authConstants;
        this.passwordEncoder = passwordEncoder;
        this.usuarioBusiness = usuarioBusiness;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    @PostMapping(value = "/auth/login", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> login(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestBody(required = false) LoginRequest body) {
        String user = username;
        String pass = password;
        if ((user == null || user.isEmpty() || pass == null || pass.isEmpty()) && body != null) {
            user = body.getUsername();
            pass = body.getPassword();
        }
        if (user == null || user.isEmpty() || pass == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Faltan username y password");
        }
        // #region agent log
        DebugLog.log("AuthRestController.java:login", "login request received",
                Map.of("usernameLength", user.length(), "passwordPresent", !pass.isEmpty()),
                "D");
        // #endregion
        try {
            Authentication auth = customAuthenticationManager.authenticate(
                    customAuthenticationManager.authWrap(user, pass));
            String token = buildToken(auth);
            return ResponseEntity.ok(token);
        } catch (BadCredentialsException e) {
            // #region agent log
            DebugLog.log("AuthRestController.java:login", "BadCredentialsException",
                    Map.of("message", e.getMessage()), "A");
            // #endregion
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (AuthenticationServiceException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al autenticar");
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * Registro público. Crea un usuario con estado PENDIENTE_APROBACION (sin roles).
     * Debe ser aprobado por un ADMIN para poder hacer login.
     */
    @PostMapping(value = "/auth/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            Usuario usuario = new Usuario();
            usuario.setNombre(request.getNombre());
            usuario.setApellido(request.getApellido());
            usuario.setEmail(request.getEmail());
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
            usuario.setEstado(EstadoUsuario.PENDIENTE_APROBACION);
            usuario.setActivo(false);
            usuario.setRoles(new java.util.ArrayList<>());

            Usuario created = usuarioBusiness.add(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (FoundException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping(value = "/auth/refresh", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> refresh(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido o sesión expirada");
        }
        return ResponseEntity.ok(buildToken(authentication));
    }

    @GetMapping("/auth/me")
    public ResponseEntity<Object> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AuthUser user = (AuthUser) authentication.getPrincipal();
        List<String> authorities = user.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());
        AuthMeResponse body = new AuthMeResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                authorities
        );
        return ResponseEntity.ok(body);
    }

    /**
     * Solo desarrollo: codifica una contraseña con BCrypt.
     * Ruta permitAll en SecurityConfiguration solo para /api/qnt/v1/demo/**
     */
    @GetMapping(value = "/demo/encodepass", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> encodePass(@RequestParam String password) {
        return ResponseEntity.ok(passwordEncoder.encode(password));
    }

    /**
     * Solo desarrollo: comprueba si la contraseña coincide con la guardada para ese email.
     * Útil para diagnosticar "Credenciales incorrectas" (hash en BD, longitud, etc.).
     */
    @GetMapping(value = "/demo/checkpass", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> checkPass(@RequestParam String email, @RequestParam String password) {
        try {
            var usuario = usuarioBusiness.load(email);
            String stored = usuario.getPassword();
            boolean matches = passwordEncoder.matches(password, stored);
            String json = String.format(
                    "{\"email\":\"%s\",\"usuarioEncontrado\":true,\"passwordCoincide\":%s,\"longitudHashEnBD\":%d,\"hashEmpiezaCon\":\"%s\"}",
                    email.replace("\"", "\\\""),
                    matches,
                    stored != null ? stored.length() : 0,
                    stored != null && stored.length() >= 4 ? stored.substring(0, 4) : "n/a"
            );
            return ResponseEntity.ok(json);
        } catch (NotFoundException e) {
            return ResponseEntity.ok("{\"email\":\"" + email.replace("\"", "\\\"") + "\",\"usuarioEncontrado\":false,\"mensaje\":\"No existe usuario con ese email\"}");
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }

    /**
     * Solicita recuperación de contraseña. Siempre responde 200 aunque el email no exista
     * (evita enumerar usuarios). Envía un email con link de un solo uso válido por 1 hora.
     */
    @PostMapping(value = "/auth/forgot-password", produces = MediaType.TEXT_PLAIN_VALUE)
    @Transactional
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        try {
            Usuario user = usuarioBusiness.load(req.email());
            // Eliminar tokens anteriores del mismo usuario
            tokenRepository.deleteByUsuarioId(user.getId());

            PasswordResetToken prt = new PasswordResetToken();
            prt.setToken(UUID.randomUUID().toString());
            prt.setUsuario(user);
            prt.setExpiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES));
            tokenRepository.save(prt);

            emailService.sendPasswordResetEmail(user.getEmail(), prt.getToken());
        } catch (NotFoundException e) {
            // No revelar si el email existe o no
            log.info("forgot-password: email no encontrado, respuesta genérica");
        } catch (Exception e) {
            log.error("forgot-password: error inesperado", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar la solicitud. Intentá de nuevo.");
        }
        return ResponseEntity.ok("Si el email está registrado, recibirás un enlace en tu bandeja de entrada.");
    }

    /**
     * Restablece la contraseña usando el token recibido por email.
     */
    @PostMapping(value = "/auth/reset-password", produces = MediaType.TEXT_PLAIN_VALUE)
    @Transactional
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest req) {
        if (req.newPassword() == null || req.newPassword().length() < 6) {
            return ResponseEntity.badRequest().body("La contraseña debe tener al menos 6 caracteres.");
        }
        try {
            PasswordResetToken prt = tokenRepository.findByToken(req.token())
                    .orElseThrow(() -> new IllegalArgumentException("Token inválido o inexistente."));
            if (prt.isExpired()) {
                tokenRepository.delete(prt);
                return ResponseEntity.badRequest().body("El enlace expiró. Solicitá uno nuevo.");
            }
            Usuario user = prt.getUsuario();
            user.setPassword(passwordEncoder.encode(req.newPassword()));
            tokenRepository.delete(prt);
            usuarioBusiness.update(user);
            return ResponseEntity.ok("Contraseña actualizada correctamente.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("reset-password: error inesperado", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al restablecer la contraseña. Intentá de nuevo.");
        }
    }

    record ForgotPasswordRequest(String email) {}
    record ResetPasswordRequest(String token, String newPassword) {}

    private String buildToken(Authentication auth) {
        AuthUser user = (AuthUser) auth.getPrincipal();
        List<String> roles = user.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());
        Date expiresAt = new Date(System.currentTimeMillis() + authConstants.getExpirationMs());
        return JWT.create()
                .withSubject(user.getUsername())
                .withClaim("internalId", user.getId())
                .withClaim("email", user.getEmail())
                .withClaim("roles", roles)
                .withExpiresAt(expiresAt)
                .sign(Algorithm.HMAC512(authConstants.getSecret()));
    }
}
