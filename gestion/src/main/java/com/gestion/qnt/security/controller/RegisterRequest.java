package com.gestion.qnt.security.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body JSON para POST /auth/register.
 */
public class RegisterRequest {

    @NotBlank(message = "nombre es obligatorio")
    @Size(max = 255)
    private String nombre;

    @Size(max = 255)
    private String apellido;

    @NotBlank(message = "email es obligatorio")
    @Email(message = "email debe ser válido")
    private String email;

    @NotBlank(message = "password es obligatoria")
    @Size(min = 12, message = "La contraseña debe tener al menos 12 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "La contraseña debe contener al menos una mayúscula, una minúscula y un número"
    )
    private String password;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
