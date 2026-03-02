package com.gestion.qnt.security.filters;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.security.AuthConstants;
import com.gestion.qnt.security.AuthUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filtro que lee el JWT del header Authorization (Bearer) o del query param authtoken,
 * verifica firma y expiración, y setea el principal en el SecurityContext.
 * Para la ruta de login no hace nada y limpia el contexto, para evitar 403 por auth previa.
 */
public class JWTAuthorizationFilter extends OncePerRequestFilter {

    private static final String PATH_LOGIN = ApiConstants.URL_BASE + "/auth/login";

    private final AuthConstants authConstants;

    public JWTAuthorizationFilter(AuthConstants authConstants) {
        this.authConstants = authConstants;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path != null && path.equals(PATH_LOGIN)) {
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
            return;
        }

        String token = getTokenFromRequest(request);

        if (token != null && !token.isBlank()) {
            try {
                UsernamePasswordAuthenticationToken authentication = getAuthentication(token);
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JWTVerificationException e) {
                // Token inválido o expirado: no setear autenticación; las rutas protegidas devolverán 401
            }
        }

        chain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader(AuthConstants.AUTH_HEADER_NAME);
        if (header != null && header.startsWith(AuthConstants.TOKEN_PREFIX)) {
            return header.substring(AuthConstants.TOKEN_PREFIX.length()).trim();
        }
        String param = request.getParameter(AuthConstants.AUTH_PARAM_NAME);
        return param != null ? param.trim() : null;
    }

    private UsernamePasswordAuthenticationToken getAuthentication(String token) {
        DecodedJWT decoded = JWT.require(Algorithm.HMAC512(authConstants.getSecret()))
                .build()
                .verify(token);

        String subject = decoded.getSubject();
        Long internalId = decoded.getClaim("internalId").isNull() ? null : decoded.getClaim("internalId").asLong();
        String email = decoded.getClaim("email").isNull() ? subject : decoded.getClaim("email").asString();
        List<String> roles = decoded.getClaim("roles").isNull()
                ? Collections.emptyList()
                : decoded.getClaim("roles").asList(String.class);

        // Normalizar: Spring Security hasRole('X') espera authority "ROLE_X"
        List<String> normalizedRoles = roles.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .collect(Collectors.toList());

        AuthUser principal = new AuthUser(internalId != null ? internalId : 0L, email, normalizedRoles);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
