package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.*;
import com.gestion.qnt.model.LicenciaANAC;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.security.AuthUser;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.FoundException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ILicenciaANACBusiness;
import com.gestion.qnt.model.business.interfaces.IUsuarioBusiness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/mi-perfil")
public class MiPerfilPilotoController {

    private final IUsuarioBusiness usuarioBusiness;
    private final ILicenciaANACBusiness licenciaANACBusiness;
    private final PasswordEncoder passwordEncoder;

    public MiPerfilPilotoController(IUsuarioBusiness usuarioBusiness,
                                    ILicenciaANACBusiness licenciaANACBusiness,
                                    PasswordEncoder passwordEncoder) {
        this.usuarioBusiness = usuarioBusiness;
        this.licenciaANACBusiness = licenciaANACBusiness;
        this.passwordEncoder = passwordEncoder;
    }

    private static AuthUser authUser(Authentication auth) {
        return (AuthUser) auth.getPrincipal();
    }

    private static boolean isPilotoOrAdmin(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_PILOTO".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
    }

    // ──────────────────────────── Perfil general ────────────────────────────

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMiPerfil(Authentication authentication) {
        AuthUser auth = authUser(authentication);
        try {
            Usuario usuario = usuarioBusiness.load(auth.getId());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("usuario", usuario);
            body.put("roles", usuario.getRoles() != null ? usuario.getRoles() : List.of());
            body.put("tieneFotoPerfil", usuario.getImagenPerfil() != null && usuario.getImagenPerfil().length > 0);
            if (isPilotoOrAdmin(authentication)) {
                List<LicenciaANAC> licencias = licenciaANACBusiness.listByPiloto(auth.getId());
                body.put("licencias", licencias.stream()
                        .map(l -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("id", l.getId());
                            m.put("fechaVencimientoCma", l.getFechaVencimientoCma() != null ? l.getFechaVencimientoCma().toString() : null);
                            m.put("fechaEmision", l.getFechaEmision() != null ? l.getFechaEmision().toString() : null);
                            m.put("caducidad", l.getCaducidad() != null ? l.getCaducidad().toString() : null);
                            m.put("tieneImagenCma", l.getImagenCma() != null && l.getImagenCma().length > 0);
                            m.put("tieneImagenCertificadoIdoneidad", l.getImagenCertificadoIdoneidad() != null && l.getImagenCertificadoIdoneidad().length > 0);
                            m.put("activo", l.getActivo());
                            return m;
                        })
                        .collect(Collectors.toList()));
            } else {
                body.put("licencias", List.of());
            }
            return ResponseEntity.ok(body);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> actualizarMiPerfil(Authentication authentication,
                                                 @RequestBody ActualizarMiPerfilRequest request) {
        AuthUser auth = authUser(authentication);
        try {
            Usuario usuario = usuarioBusiness.load(auth.getId());
            if (request.nombre() != null && !request.nombre().isBlank()) usuario.setNombre(request.nombre());
            if (request.apellido() != null) usuario.setApellido(request.apellido().isBlank() ? null : request.apellido());
            if (request.dni() != null) usuario.setDni(request.dni().isBlank() ? null : request.dni());
            if (request.passwordMission() != null) {
                String pm = request.passwordMission().trim();
                if (pm.length() > 30) pm = pm.substring(0, 30);
                usuario.setPasswordMission(pm.isBlank() ? null : pm);
            }
            usuarioBusiness.update(usuario);
            return ResponseEntity.ok(usuario);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (FoundException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/cambio-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cambioPassword(Authentication authentication,
                                            @Valid @RequestBody CambioPasswordMiPerfilRequest request) {
        String email = authUser(authentication).getEmail();
        try {
            usuarioBusiness.changePassword(email, request.oldPassword(), request.newPassword(), passwordEncoder);
            return ResponseEntity.ok().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            if ("Contraseña anterior incorrecta".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // ──────────────────────────── Foto de perfil ────────────────────────────

    @PutMapping("/foto-perfil")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> subirFotoPerfil(Authentication authentication,
                                             @RequestParam("file") MultipartFile file) {
        AuthUser auth = authUser(authentication);
        try {
            Usuario usuario = usuarioBusiness.load(auth.getId());
            usuario.setImagenPerfil(file.getBytes());
            usuarioBusiness.update(usuario);
            return ResponseEntity.ok().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (FoundException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al leer el archivo");
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/foto-perfil")
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getFotoPerfil(Authentication authentication) {
        AuthUser auth = authUser(authentication);
        try {
            Usuario usuario = usuarioBusiness.load(auth.getId());
            byte[] imagen = usuario.getImagenPerfil();
            if (imagen == null || imagen.length == 0) {
                return ResponseEntity.notFound().build();
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(imagen.length);
            return ResponseEntity.ok().headers(headers).body(imagen);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ──────────────────────── Licencias ANAC (CRUD) ─────────────────────────

    @GetMapping("/licencias")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getMisLicencias(Authentication authentication) {
        AuthUser auth = authUser(authentication);
        try {
            List<LicenciaANAC> licencias = licenciaANACBusiness.listByPiloto(auth.getId());
            List<Map<String, Object>> result = licencias.stream().map(this::licenciaToMap).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/licencias")
    @Transactional
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<?> crearLicencia(Authentication authentication,
                                           @RequestBody CrearLicenciaMiPerfilRequest request) {
        AuthUser auth = authUser(authentication);
        try {
            Usuario piloto = usuarioBusiness.load(auth.getId());
            LicenciaANAC lic = new LicenciaANAC();
            lic.setPiloto(piloto);
            lic.setFechaVencimientoCma(request.fechaVencimientoCma());
            lic.setFechaEmision(request.fechaEmision());
            lic.setCaducidad(request.caducidad());
            lic.setActivo(request.activo() != null ? request.activo() : true);
            LicenciaANAC created = licenciaANACBusiness.add(lic);
            return ResponseEntity.status(HttpStatus.CREATED).body(licenciaToMap(created));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/licencias/{id}")
    @Transactional
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<?> actualizarLicencia(Authentication authentication,
                                                @PathVariable Long id,
                                                @RequestBody ActualizarLicenciaMiPerfilRequest request) {
        AuthUser auth = authUser(authentication);
        try {
            LicenciaANAC lic = licenciaANACBusiness.load(id);
            if (lic.getPiloto() == null || !lic.getPiloto().getId().equals(auth.getId())) {
                return ResponseEntity.notFound().build();
            }
            if (request.fechaVencimientoCma() != null) lic.setFechaVencimientoCma(request.fechaVencimientoCma());
            if (request.fechaEmision() != null) lic.setFechaEmision(request.fechaEmision());
            if (request.caducidad() != null) lic.setCaducidad(request.caducidad());
            if (request.activo() != null) lic.setActivo(request.activo());
            return ResponseEntity.ok(licenciaToMap(licenciaANACBusiness.update(lic)));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/licencias/{id}")
    @Transactional
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarLicencia(Authentication authentication, @PathVariable Long id) {
        AuthUser auth = authUser(authentication);
        try {
            LicenciaANAC lic = licenciaANACBusiness.load(id);
            if (lic.getPiloto() == null || !lic.getPiloto().getId().equals(auth.getId())) {
                return ResponseEntity.notFound().build();
            }
            licenciaANACBusiness.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ────────────────── Imágenes por licencia ANAC: CMA ─────────────────────

    @PutMapping("/licencias/{id}/imagen-cma")
    @Transactional
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<?> subirImagenCmaLicencia(Authentication authentication,
                                                    @PathVariable Long id,
                                                    @RequestParam("file") MultipartFile file) {
        AuthUser auth = authUser(authentication);
        try {
            LicenciaANAC lic = licenciaANACBusiness.load(id);
            if (lic.getPiloto() == null || !lic.getPiloto().getId().equals(auth.getId())) {
                return ResponseEntity.notFound().build();
            }
            lic.setImagenCma(file.getBytes());
            licenciaANACBusiness.update(lic);
            return ResponseEntity.ok().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al leer el archivo");
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/licencias/{id}/imagen-cma")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> getImagenCmaLicencia(Authentication authentication, @PathVariable Long id) {
        AuthUser auth = authUser(authentication);
        try {
            LicenciaANAC lic = licenciaANACBusiness.load(id);
            if (lic.getPiloto() == null || !lic.getPiloto().getId().equals(auth.getId())) {
                return ResponseEntity.notFound().build();
            }
            byte[] imagen = lic.getImagenCma();
            if (imagen == null || imagen.length == 0) {
                return ResponseEntity.notFound().build();
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(imagen.length);
            return ResponseEntity.ok().headers(headers).body(imagen);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ──────────── Imágenes por licencia ANAC: Certificado Idoneidad ─────────

    @PutMapping("/licencias/{id}/imagen-certificado-idoneidad")
    @Transactional
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<?> subirImagenIdoneidad(Authentication authentication,
                                                  @PathVariable Long id,
                                                  @RequestParam("file") MultipartFile file) {
        AuthUser auth = authUser(authentication);
        try {
            LicenciaANAC lic = licenciaANACBusiness.load(id);
            if (lic.getPiloto() == null || !lic.getPiloto().getId().equals(auth.getId())) {
                return ResponseEntity.notFound().build();
            }
            lic.setImagenCertificadoIdoneidad(file.getBytes());
            licenciaANACBusiness.update(lic);
            return ResponseEntity.ok().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al leer el archivo");
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/licencias/{id}/imagen-certificado-idoneidad")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> getImagenIdoneidad(Authentication authentication, @PathVariable Long id) {
        AuthUser auth = authUser(authentication);
        try {
            LicenciaANAC lic = licenciaANACBusiness.load(id);
            if (lic.getPiloto() == null || !lic.getPiloto().getId().equals(auth.getId())) {
                return ResponseEntity.notFound().build();
            }
            byte[] imagen = lic.getImagenCertificadoIdoneidad();
            if (imagen == null || imagen.length == 0) {
                return ResponseEntity.notFound().build();
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(imagen.length);
            return ResponseEntity.ok().headers(headers).body(imagen);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ──────────────────────────── Helpers ────────────────────────────────────

    private Map<String, Object> licenciaToMap(LicenciaANAC l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("fechaVencimientoCma", l.getFechaVencimientoCma() != null ? l.getFechaVencimientoCma().toString() : null);
        m.put("fechaEmision", l.getFechaEmision() != null ? l.getFechaEmision().toString() : null);
        m.put("caducidad", l.getCaducidad() != null ? l.getCaducidad().toString() : null);
        m.put("tieneImagenCma", l.getImagenCma() != null && l.getImagenCma().length > 0);
        m.put("tieneImagenCertificadoIdoneidad", l.getImagenCertificadoIdoneidad() != null && l.getImagenCertificadoIdoneidad().length > 0);
        m.put("activo", l.getActivo());
        return m;
    }
}
