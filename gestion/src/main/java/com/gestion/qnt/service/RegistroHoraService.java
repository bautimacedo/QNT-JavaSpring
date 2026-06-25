package com.gestion.qnt.service;

import com.gestion.qnt.controller.dto.CreateRegistroHoraRequest;
import com.gestion.qnt.controller.dto.ResumenHorasResponse;
import com.gestion.qnt.controller.dto.UpdateRegistroHoraRequest;
import com.gestion.qnt.model.RegistroHora;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.repository.RegistroHoraRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RegistroHoraService {

    private final RegistroHoraRepository repo;

    public RegistroHoraService(RegistroHoraRepository repo) {
        this.repo = repo;
    }

    public RegistroHora crear(CreateRegistroHoraRequest req, Usuario autor) {
        if (req.fecha() == null) throw new IllegalArgumentException("La fecha es obligatoria");
        if (req.horas() == null || req.horas().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Las horas deben ser mayores a 0");

        RegistroHora r = new RegistroHora();
        r.setAutor(autor);
        r.setFecha(req.fecha());
        r.setHoras(req.horas());
        r.setDescripcion(req.descripcion());
        return repo.save(r);
    }

    public List<RegistroHora> listarTodos() {
        return repo.findAllByOrderByFechaDescCreatedAtDesc();
    }

    /** Resumen de horas totales por autor, sobre todos los registros. */
    public List<ResumenHorasResponse> resumen() {
        Map<Long, ResumenAcum> porAutor = new LinkedHashMap<>();
        for (RegistroHora r : repo.findAllByOrderByFechaDescCreatedAtDesc()) {
            Usuario a = r.getAutor();
            if (a == null) continue;
            ResumenAcum acum = porAutor.computeIfAbsent(a.getId(),
                    k -> new ResumenAcum(a.getNombre(), a.getApellido()));
            acum.total = acum.total.add(r.getHoras() != null ? r.getHoras() : BigDecimal.ZERO);
            acum.count++;
        }
        List<ResumenHorasResponse> out = new ArrayList<>();
        porAutor.forEach((id, acum) ->
                out.add(new ResumenHorasResponse(id, acum.nombre, acum.apellido, acum.total, acum.count)));
        return out;
    }

    /** Solo el autor del registro puede editarlo. */
    public RegistroHora actualizar(Long id, UpdateRegistroHoraRequest req, Usuario solicitante) {
        RegistroHora r = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado: " + id));
        if (!r.getAutor().getId().equals(solicitante.getId()))
            throw new SecurityException("Solo el autor puede editar este registro");

        if (req.fecha() != null) r.setFecha(req.fecha());
        if (req.horas() != null) {
            if (req.horas().compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Las horas deben ser mayores a 0");
            r.setHoras(req.horas());
        }
        r.setDescripcion(req.descripcion());
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

    private static class ResumenAcum {
        final String nombre;
        final String apellido;
        BigDecimal total = BigDecimal.ZERO;
        long count = 0;
        ResumenAcum(String nombre, String apellido) { this.nombre = nombre; this.apellido = apellido; }
    }
}
