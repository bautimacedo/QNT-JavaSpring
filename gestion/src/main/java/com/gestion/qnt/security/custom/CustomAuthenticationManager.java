package com.gestion.qnt.security.custom;

import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.enums.EstadoUsuario;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IUsuarioBusiness;
import com.gestion.qnt.debug.DebugLog;
import com.gestion.qnt.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
@Primary
public class CustomAuthenticationManager implements AuthenticationManager {

    private final PasswordEncoder passwordEncoder;
    private final IUsuarioBusiness usuarioBusiness;

    @Override
    @Transactional(readOnly = true)
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String rawPassword = authentication.getCredentials() == null ? null : authentication.getCredentials().toString();

        // #region agent log
        DebugLog.log("CustomAuthenticationManager.java:authenticate", "authenticate entry",
                Map.of("email", email != null ? email : "null", "passwordPresent", rawPassword != null && !rawPassword.isEmpty()), "A");
        // #endregion

        if (email == null || rawPassword == null) {
            throw new BadCredentialsException("Credenciales incompletas");
        }

        Usuario usuario;
        try {
            usuario = usuarioBusiness.load(email);
        } catch (NotFoundException e) {
            // #region agent log
            DebugLog.log("CustomAuthenticationManager.java:authenticate", "Usuario not found by email", Map.of("email", email), "A");
            // #endregion
            throw new BadCredentialsException("Credenciales incorrectas");
        } catch (BusinessException e) {
            log.error("Error de negocio al cargar usuario por email: {}", email, e);
            throw new AuthenticationServiceException("Error al autenticar", e);
        }

        // #region agent log
        DebugLog.log("CustomAuthenticationManager.java:authenticate", "usuario loaded",
                Map.of("usuarioId", usuario.getId(), "activo", usuario.getActivo() != null ? usuario.getActivo() : "null"), "B");
        // #endregion

        if (!passwordEncoder.matches(rawPassword, usuario.getPassword())) {
            // #region agent log
            DebugLog.log("CustomAuthenticationManager.java:authenticate", "password mismatch", Map.of("email", email), "B");
            // #endregion
            throw new BadCredentialsException("Contraseña incorrecta");
        }

        EstadoUsuario estado = usuario.getEstado();
        if (estado != null && estado != EstadoUsuario.ACTIVO) {
            if (estado == EstadoUsuario.PENDIENTE_APROBACION) {
                throw new DisabledException("Tu cuenta está pendiente de aprobación por un administrador");
            }
            if (estado == EstadoUsuario.DESACTIVADO) {
                throw new DisabledException("Tu cuenta está desactivada");
            }
            throw new DisabledException("Tu cuenta está desactivada");
        }

        // Compatibilidad: si estado es ACTIVO, asegurar activo == true
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new DisabledException("Tu cuenta está desactivada");
        }

        var roleCodigos = usuario.getRoles() == null
                ? Collections.<String>emptyList()
                : usuario.getRoles().stream()
                        .map(r -> r.getCodigo())
                        .collect(Collectors.toList());

        AuthUser principal = new AuthUser(usuario.getId(), usuario.getEmail(), roleCodigos);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    /**
     * Envuelve nombre (email) y contraseña en un Authentication no autenticado
     * para pasarlo a {@link #authenticate(Authentication)}.
     */
    public Authentication authWrap(String name, String pass) {
        return new UsernamePasswordAuthenticationToken(name, pass, Collections.emptyList());
    }
}
