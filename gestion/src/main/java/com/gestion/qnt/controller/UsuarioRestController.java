package com.gestion.qnt.controller;

import com.gestion.qnt.config.ApiConstants;
import com.gestion.qnt.controller.dto.AssignRoleRequest;
import com.gestion.qnt.controller.dto.AprobarUsuarioRequest;
import com.gestion.qnt.controller.dto.ChangePasswordRequest;
import com.gestion.qnt.controller.dto.CreateUsuarioRequest;
import com.gestion.qnt.model.Role;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.FoundException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IRoleBusiness;
import com.gestion.qnt.model.business.interfaces.IUsuarioBusiness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(ApiConstants.URL_BASE + "/usuarios")
public class UsuarioRestController {

    private final IUsuarioBusiness usuarioBusiness;
    private final IRoleBusiness roleBusiness;
    private final PasswordEncoder passwordEncoder;

    public UsuarioRestController(IUsuarioBusiness usuarioBusiness,
                                 IRoleBusiness roleBusiness,
                                 PasswordEncoder passwordEncoder) {
        this.usuarioBusiness = usuarioBusiness;
        this.roleBusiness = roleBusiness;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> list() {
        try {
            return ResponseEntity.ok(usuarioBusiness.list());
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lista usuarios con estado PENDIENTE_APROBACION (solo ADMIN).
     */
    @GetMapping("/pendientes")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> listPendientes() {
        try {
            return ResponseEntity.ok(usuarioBusiness.listPendientes());
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lista usuarios que tienen el rol ROLE_PILOTO (solo ADMIN).
     */
    @GetMapping("/pilotos")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> listPilotos() {
        try {
            return ResponseEntity.ok(usuarioBusiness.listPilotos());
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Aprueba un usuario pendiente: asigna rol y pone estado ACTIVO (solo ADMIN).
     */
    @PutMapping("/{id}/aprobar")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> aprobar(@PathVariable Long id, @Valid @RequestBody AprobarUsuarioRequest request) {
        try {
            Usuario updated = usuarioBusiness.aprobar(id, request.roleCodigo());
            return ResponseEntity.ok(updated);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            if ("El usuario no está pendiente de aprobación".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Usuario> searchByEmail(@RequestParam String email) {
        try {
            return ResponseEntity.ok(usuarioBusiness.load(email));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody CreateUsuarioRequest request) {
        try {
            Usuario usuario = new Usuario();
            usuario.setNombre(request.nombre());
            usuario.setApellido(request.apellido());
            usuario.setEmail(request.email());
            usuario.setPassword(passwordEncoder.encode(request.password()));
            usuario.setActivo(true);

            if (request.roleCodigos() != null && !request.roleCodigos().isEmpty()) {
                List<Role> roles = new ArrayList<>();
                for (String codigo : request.roleCodigos()) {
                    roles.add(roleBusiness.load(codigo));
                }
                usuario.setRoles(roles);
            } else if (request.roleIds() != null && !request.roleIds().isEmpty()) {
                List<Role> roles = new ArrayList<>();
                for (Long id : request.roleIds()) {
                    roles.add(roleBusiness.load(id));
                }
                usuario.setRoles(roles);
            }

            Usuario created = usuarioBusiness.add(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (FoundException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Rol no encontrado");
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Usuario body) {
        try {
            if (!id.equals(body.getId())) {
                body.setId(id);
            }
            Usuario updated = usuarioBusiness.update(body);
            return ResponseEntity.ok(updated);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (FoundException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            usuarioBusiness.changePassword(request.email(), request.oldPassword(), request.newPassword(), passwordEncoder);
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

    @PutMapping("/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> disable(@RequestParam String email) {
        try {
            usuarioBusiness.disable(email);
            return ResponseEntity.ok().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> enable(@RequestParam String email) {
        try {
            usuarioBusiness.enable(email);
            return ResponseEntity.ok().build();
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/assign-role")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignRole(@RequestBody AssignRoleRequest request) {
        try {
            Usuario user = usuarioBusiness.load(request.email());
            Role role = roleBusiness.load(request.roleCodigo());
            Usuario updated = usuarioBusiness.addRole(role, user);
            return ResponseEntity.ok(updated);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/remove-role")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeRole(@RequestBody AssignRoleRequest request) {
        try {
            Usuario user = usuarioBusiness.load(request.email());
            Role role = roleBusiness.load(request.roleCodigo());
            Usuario updated = usuarioBusiness.removeRole(role, user);
            return ResponseEntity.ok(updated);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
