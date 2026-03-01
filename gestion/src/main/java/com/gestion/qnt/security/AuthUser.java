package com.gestion.qnt.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Principal de seguridad: DTO con id, email y autoridades.
 * No expone la entidad JPA ni fuerza lazy loading.
 */
public class AuthUser implements UserDetails {

    private final Long id;
    private final String email;
    private final List<GrantedAuthority> authorities;

    public AuthUser(Long id, String email, Collection<String> roleCodigos) {
        this.id = id;
        this.email = email;
        this.authorities = roleCodigos == null
                ? List.of()
                : roleCodigos.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
