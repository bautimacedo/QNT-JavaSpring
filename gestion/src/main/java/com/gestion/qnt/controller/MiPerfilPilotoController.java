package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.*;
import com.gestion.qnt.model.Licencia;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.security.AuthUser;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.FoundException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.ILicenciaBusiness;
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
    private final ILicenciaBusiness licenciaBusiness;
    private final PasswordEncoder passwordEncoder;

    public MiPerfilPilotoController(IUsuarioBusiness usuarioBusiness,
                                    ILicenciaBusiness licenciaBusiness,
                                    PasswordEncoder passwordEncoder) {
        this.usuarioBusiness = usuarioBusiness;
        this.licenciaBusiness = licenciaBusiness;
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

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMiPerfil(Authentication authentication) {
        AuthUser auth = authUser(authentication);
        try {
            Usuario usuario = usuarioBusiness.load(auth.getId());
            Map<String, Object> body = new HashMap<>();
            body.put("usuario", usuario);
            body.put("tieneImagenCma", usuario.getImagenCma() != null && usuario.getImagenCma().length > 0);
            if (isPilotoOrAdmin(authentication)) {
                List<Licencia> licencias = licenciaBusiness.listByPiloto(auth.getId());
                body.put("licencias", licencias.stream()
                        .map(l -> Map.<String, Object>of(
                                "id", l.getId(),
                                "nombre", l.getNombre() != null ? l.getNombre() : "",
                                "numLicencia", l.getNumLicencia() != null ? l.getNumLicencia() : "",
                                "caducidad", l.getCaducidad() != null ? l.getCaducidad().toString() : null
                        ))
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

    @GetMapping("/cma")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCma(Authentication authentication) {
        AuthUser auth = authUser(authentication);
        try {
            Usuario usuario = usuarioBusiness.load(auth.getId());
            Map<String, Object> body = new HashMap<>();
            body.put("vencimiento", usuario.getCmaVencimiento());
            body.put("tieneImagen", usuario.getImagenCma() != null && usuario.getImagenCma().length > 0);
            return ResponseEntity.ok(body);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/cma")
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<?> actualizarCmaVencimiento(Authentication authentication,
                                                     @RequestBody CmaVencimientoRequest request) {
        AuthUser auth = authUser(authentication);
        try {
            Usuario usuario = usuarioBusiness.load(auth.getId());
            usuario.setCmaVencimiento(request.vencimiento());
            usuarioBusiness.update(usuario);
            return ResponseEntity.ok().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (FoundException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/cma/imagen")
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<?> subirImagenCma(Authentication authentication,
                                             @RequestParam("file") MultipartFile file) {
        AuthUser auth = authUser(authentication);
        try {
            Usuario usuario = usuarioBusiness.load(auth.getId());
            usuario.setImagenCma(file.getBytes());
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

    @GetMapping("/cma/imagen")
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> getImagenCma(Authentication authentication) {
        AuthUser auth = authUser(authentication);
        try {
            Usuario usuario = usuarioBusiness.load(auth.getId());
            byte[] imagen = usuario.getImagenCma();
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

    @GetMapping("/licencias")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<List<Licencia>> getMisLicencias(Authentication authentication) {
        AuthUser auth = authUser(authentication);
        try {
            return ResponseEntity.ok(licenciaBusiness.listByPiloto(auth.getId()));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/licencias")
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<?> crearLicencia(Authentication authentication,
                                           @RequestBody CrearLicenciaMiPerfilRequest request) {
        AuthUser auth = authUser(authentication);
        try {
            Usuario piloto = usuarioBusiness.load(auth.getId());
            Licencia licencia = new Licencia();
            licencia.setNombre(request.nombre() != null ? request.nombre() : "");
            licencia.setNumLicencia(request.numLicencia());
            licencia.setPiloto(piloto);
            licencia.setFechaCompra(request.fechaCompra());
            licencia.setCaducidad(request.caducidad());
            licencia.setVersion(request.version());
            licencia.setActivo(request.activo() != null ? request.activo() : true);
            Licencia created = licenciaBusiness.add(licencia);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/licencias/{id}")
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<?> actualizarLicencia(Authentication authentication,
                                                @PathVariable Long id,
                                                @RequestBody ActualizarLicenciaMiPerfilRequest request) {
        AuthUser auth = authUser(authentication);
        try {
            Licencia licencia = licenciaBusiness.load(id);
            if (licencia.getPiloto() == null || !licencia.getPiloto().getId().equals(auth.getId())) {
                return ResponseEntity.notFound().build();
            }
            if (request.nombre() != null) licencia.setNombre(request.nombre());
            licencia.setNumLicencia(request.numLicencia());
            licencia.setFechaCompra(request.fechaCompra());
            licencia.setCaducidad(request.caducidad());
            licencia.setVersion(request.version());
            if (request.activo() != null) licencia.setActivo(request.activo());
            return ResponseEntity.ok(licenciaBusiness.update(licencia));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/licencias/{id}")
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarLicencia(Authentication authentication, @PathVariable Long id) {
        AuthUser auth = authUser(authentication);
        try {
            Licencia licencia = licenciaBusiness.load(id);
            if (licencia.getPiloto() == null || !licencia.getPiloto().getId().equals(auth.getId())) {
                return ResponseEntity.notFound().build();
            }
            licenciaBusiness.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/licencias/{id}/imagen")
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<?> subirImagenLicencia(Authentication authentication,
                                                 @PathVariable Long id,
                                                 @RequestParam("file") MultipartFile file) {
        AuthUser auth = authUser(authentication);
        try {
            Licencia licencia = licenciaBusiness.load(id);
            if (licencia.getPiloto() == null || !licencia.getPiloto().getId().equals(auth.getId())) {
                return ResponseEntity.notFound().build();
            }
            licencia.setImagen(file.getBytes());
            licenciaBusiness.update(licencia);
            return ResponseEntity.ok().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al leer el archivo");
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/licencias/{id}/imagen")
    @PreAuthorize("hasRole('PILOTO') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> getImagenLicencia(Authentication authentication, @PathVariable Long id) {
        AuthUser auth = authUser(authentication);
        try {
            Licencia licencia = licenciaBusiness.load(id);
            if (licencia.getPiloto() == null || !licencia.getPiloto().getId().equals(auth.getId())) {
                return ResponseEntity.notFound().build();
            }
            byte[] imagen = licencia.getImagen();
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
}
