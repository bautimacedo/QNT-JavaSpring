package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Bateria;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IBateriaBusiness;
import com.gestion.qnt.model.enums.Estado;
import com.gestion.qnt.repository.BateriaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class BateriaBusiness implements IBateriaBusiness {

    @Autowired
    private BateriaRepository repository;

    @Override
    public List<Bateria> list() throws BusinessException {
        try {
            return repository.findAll();
        } catch (Exception e) {
            log.error("Error al listar baterias", e);
            throw new BusinessException("Error al listar baterias", e);
        }
    }

    @Override
    public Bateria load(Long id) throws NotFoundException, BusinessException {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Bateria con id " + id));
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al cargar bateria con id {}", id, e);
            throw new BusinessException("Error al cargar bateria", e);
        }
    }

    @Override
    public Bateria add(Bateria entity) throws BusinessException {
        try {
            aplicarFechasEstado(entity, null);
            return repository.save(entity);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al agregar bateria", e);
            throw new BusinessException("Error al agregar bateria", e);
        }
    }

    @Override
    public Bateria update(Bateria entity) throws NotFoundException, BusinessException {
        try {
            Bateria anterior = load(entity.getId());
            aplicarFechasEstado(entity, anterior);
            return repository.save(entity);
        } catch (NotFoundException e) {
            throw e;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al actualizar bateria con id {}", entity.getId(), e);
            throw new BusinessException("Error al actualizar bateria", e);
        }
    }

    private void aplicarFechasEstado(Bateria entity, Bateria anterior) {
        Estado estadoAnterior = anterior != null ? anterior.getEstado() : null;
        Estado estadoNuevo = entity.getEstado();
        if (estadoNuevo == null) return;

        if (estadoNuevo == Estado.STOCK_ACTIVO && estadoAnterior != Estado.STOCK_ACTIVO) {
            entity.setFechaStockActivo(LocalDateTime.now());
        }
        if (estadoNuevo == Estado.EN_DESUSO && estadoAnterior != Estado.EN_DESUSO) {
            entity.setFechaEnDesuso(LocalDateTime.now());
            if (entity.getFechaStockActivo() != null) {
                long dias = java.time.temporal.ChronoUnit.DAYS.between(
                        entity.getFechaStockActivo().toLocalDate(),
                        LocalDateTime.now().toLocalDate());
                entity.setDiasUso((int) dias);
            }
        }
    }

    @Override
    public void delete(Long id) throws NotFoundException, BusinessException {
        try {
            load(id);
            repository.deleteById(id);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al eliminar bateria con id {}", id, e);
            throw new BusinessException("Error al eliminar bateria", e);
        }
    }
}
