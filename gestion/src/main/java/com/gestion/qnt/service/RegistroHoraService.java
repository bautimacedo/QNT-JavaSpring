package com.gestion.qnt.service;

import com.gestion.qnt.controller.dto.CreateRegistroHoraRequest;
import com.gestion.qnt.controller.dto.ResumenHorasResponse;
import com.gestion.qnt.controller.dto.UpdateRegistroHoraRequest;
import com.gestion.qnt.model.RegistroHora;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.repository.RegistroHoraRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class RegistroHoraService {

    private static final ZoneId ARG = ZoneId.of("America/Argentina/Buenos_Aires");

    private final RegistroHoraRepository repo;

    public RegistroHoraService(RegistroHoraRepository repo) {
        this.repo = repo;
    }

    public RegistroHora crear(CreateRegistroHoraRequest req, Usuario autor) {
        validar(req.fecha());
        RegistroHora r = new RegistroHora();
        r.setAutor(autor);
        r.setFecha(req.fecha());
        r.setDescripcion(req.descripcion());
        return repo.save(r);
    }

    public List<RegistroHora> listar(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null) {
            return repo.findByFechaBetweenOrderByFechaDescCreatedAtDesc(desde, hasta);
        }
        return repo.findAllByOrderByFechaDescCreatedAtDesc();
    }

    /** Resumen de horas por autor (filtro opcional por rango), agregado en la BD. */
    public List<ResumenHorasResponse> resumen(LocalDate desde, LocalDate hasta) {
        List<Object[]> filas = (desde != null && hasta != null)
                ? repo.resumenPorAutorEntre(desde, hasta)
                : repo.resumenPorAutor();
        return filas.stream()
                .map(row -> new ResumenHorasResponse(
                        (Long) row[0],
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue()))
                .toList();
    }

    /** Solo el autor del registro puede editarlo. */
    public RegistroHora actualizar(Long id, UpdateRegistroHoraRequest req, Usuario solicitante) {
        RegistroHora r = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado: " + id));
        if (!r.getAutor().getId().equals(solicitante.getId()))
            throw new SecurityException("Solo el autor puede editar este registro");

        LocalDate fecha = req.fecha() != null ? req.fecha() : r.getFecha();
        validar(fecha);

        r.setFecha(fecha);
        if (req.descripcion() != null) r.setDescripcion(req.descripcion());
        r.setUpdatedAt(Instant.now());
        return repo.save(r);
    }

    /** Solo el autor del registro puede eliminarlo. */
    public void eliminar(Long id, Usuario solicitante) {
        RegistroHora r = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado: " + id));
        if (!r.getAutor().getId().equals(solicitante.getId()))
            throw new SecurityException("Solo el autor puede eliminar este registro");
        repo.delete(r);
    }

    private void validar(LocalDate fecha) {
        if (fecha == null) throw new IllegalArgumentException("La fecha es obligatoria");
        if (fecha.isAfter(LocalDate.now(ARG)))
            throw new IllegalArgumentException("La fecha no puede ser futura");
    }
}
