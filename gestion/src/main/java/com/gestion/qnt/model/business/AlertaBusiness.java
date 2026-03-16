package com.gestion.qnt.model.business;

import com.gestion.qnt.model.Alerta;
import com.gestion.qnt.model.Bateria;
import com.gestion.qnt.model.LicenciaANAC;
import com.gestion.qnt.model.Tarea;
import com.gestion.qnt.model.Usuario;
import com.gestion.qnt.model.business.exceptions.BusinessException;
import com.gestion.qnt.model.business.exceptions.NotFoundException;
import com.gestion.qnt.model.business.interfaces.IAlertaBusiness;
import com.gestion.qnt.model.enums.Estado;
import com.gestion.qnt.model.enums.EstadoTarea;
import com.gestion.qnt.model.enums.NivelAlerta;
import com.gestion.qnt.model.enums.PrioridadTarea;
import com.gestion.qnt.model.enums.TipoAlerta;
import com.gestion.qnt.repository.AlertaRepository;
import com.gestion.qnt.repository.BateriaRepository;
import com.gestion.qnt.repository.DronRepository;
import com.gestion.qnt.repository.LicenciaANACRepository;
import com.gestion.qnt.repository.TareaRepository;
import com.gestion.qnt.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class AlertaBusiness implements IAlertaBusiness {

    @Autowired
    private AlertaRepository alertaRepository;

    @Autowired
    private LicenciaANACRepository licenciaANACRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BateriaRepository bateriaRepository;

    @Autowired
    private DronRepository dronRepository;

    @Autowired
    private TareaRepository tareaRepository;

    private static final BigDecimal TEMP_BATERIA_MAX = new BigDecimal("50");
    private static final int CICLOS_CARGA_MAX = 250;

    @Override
    public List<Alerta> listActivas() throws BusinessException {
        try {
            return alertaRepository.findByResueltaFalseOrderByNivelAscFechaCreacionDesc();
        } catch (Exception e) {
            log.error("Error al listar alertas activas", e);
            throw new BusinessException("Error al listar alertas", e);
        }
    }

    @Override
    @Transactional
    public Alerta resolver(Long id) throws NotFoundException, BusinessException {
        try {
            Alerta alerta = alertaRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("No existe Alerta con id " + id));
            alerta.setResuelta(true);
            alerta.setFechaResolucion(LocalDateTime.now());
            return alertaRepository.save(alerta);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al resolver alerta {}", id, e);
            throw new BusinessException("Error al resolver alerta", e);
        }
    }

    @Override
    @Transactional
    @org.springframework.scheduling.annotation.Scheduled(fixedRateString = "${alertas.check-interval-ms:300000}")
    public void generarAlertas() throws BusinessException {
        try {
            log.info("Iniciando generación de alertas...");

            Set<String> clavesGeneradas = new HashSet<>();

            generarAlertasCma(clavesGeneradas);
            generarAlertasBaterias(clavesGeneradas);
            generarAlertasTelemetria(clavesGeneradas);
            resolverAlertasObsoletas(clavesGeneradas);

            log.info("Generación de alertas completada. {} alertas activas.", clavesGeneradas.size());
        } catch (Exception e) {
            log.error("Error al generar alertas", e);
            throw new BusinessException("Error al generar alertas", e);
        }
    }

    // ─── CMA ──────────────────────────────────────────────────────────────────

    private void generarAlertasCma(Set<String> clavesGeneradas) {
        LocalDate hoy = LocalDate.now();

        // 1. Pilotos con CMA vencida (licencia activa con fecha pasada)
        List<LicenciaANAC> vencidas = licenciaANACRepository.findActivasConCmaVencida(hoy);
        for (LicenciaANAC lic : vencidas) {
            Usuario piloto = lic.getPiloto();
            String nombre = piloto.getNombre() + (piloto.getApellido() != null ? " " + piloto.getApellido() : "");
            String clave = TipoAlerta.CMA.name() + "_PILOTO_" + piloto.getId();
            clavesGeneradas.add(clave);
            crearSiNoExiste(
                    TipoAlerta.CMA, NivelAlerta.CRITICA,
                    nombre + ": CMA vencida",
                    "Requiere renovación inmediata. Vencida el " + lic.getFechaVencimientoCma(),
                    "PILOTO", piloto.getId(), clave
            );
        }

        // 2. Pilotos sin ninguna licencia ANAC activa
        List<Long> conLicencia = licenciaANACRepository.findPilotoIdsConLicenciaActiva();
        List<Usuario> pilotos = usuarioRepository.findByRoleCodigoWithRoles("PILOTO");
        for (Usuario piloto : pilotos) {
            if (!conLicencia.contains(piloto.getId())) {
                String nombre = piloto.getNombre() + (piloto.getApellido() != null ? " " + piloto.getApellido() : "");
                String clave = TipoAlerta.CMA.name() + "_PILOTO_SIN_LIC_" + piloto.getId();
                clavesGeneradas.add(clave);
                crearSiNoExiste(
                        TipoAlerta.CMA, NivelAlerta.CRITICA,
                        nombre + ": Sin licencia ANAC activa",
                        "No habilitado para operar drones",
                        "PILOTO", piloto.getId(), clave
                );
            }
        }
    }

    // ─── Baterías ─────────────────────────────────────────────────────────────

    private void generarAlertasBaterias(Set<String> clavesGeneradas) {
        // Baterías en mantenimiento
        List<Bateria> enMantenimiento = bateriaRepository.findByEstado(Estado.EN_MANTENIMIENTO);
        for (Bateria b : enMantenimiento) {
            String clave = TipoAlerta.STOCK.name() + "_BATERIA_MANT_" + b.getId();
            clavesGeneradas.add(clave);
            String nombre = formatNombreBateria(b);
            crearSiNoExiste(
                    TipoAlerta.STOCK, NivelAlerta.ADVERTENCIA,
                    nombre + ": en mantenimiento",
                    "Batería fuera de servicio temporalmente",
                    "BATERIA", b.getId(), clave
            );
        }

        // Baterías en desuso
        List<Bateria> enDesuso = bateriaRepository.findByEstado(Estado.EN_DESUSO);
        for (Bateria b : enDesuso) {
            String clave = TipoAlerta.STOCK.name() + "_BATERIA_DESUSO_" + b.getId();
            clavesGeneradas.add(clave);
            String nombre = formatNombreBateria(b);
            crearSiNoExiste(
                    TipoAlerta.STOCK, NivelAlerta.INFO,
                    nombre + ": en desuso",
                    "Batería retirada de operación",
                    "BATERIA", b.getId(), clave
            );
        }

        // Baterías con ciclos de carga excedidos (> 250)
        List<Bateria> ciclasExcedidas = bateriaRepository.findByCiclosCargaGreaterThan(CICLOS_CARGA_MAX);
        for (Bateria b : ciclasExcedidas) {
            String clave = TipoAlerta.MANTENIMIENTO.name() + "_BATERIA_CICLOS_" + b.getId();
            clavesGeneradas.add(clave);
            String nombre = formatNombreBateria(b);
            crearSiNoExiste(
                    TipoAlerta.MANTENIMIENTO, NivelAlerta.CRITICA,
                    nombre + ": ciclos de carga excedidos",
                    b.getCiclosCarga() + " ciclos — límite " + CICLOS_CARGA_MAX + ". Reemplazar batería",
                    "BATERIA", b.getId(), clave
            );
            // Crear tarea de cambio de batería si no existe una pendiente
            crearTareaCambioBateriaSiNoExiste(b, nombre);
        }
    }

    private void crearTareaCambioBateriaSiNoExiste(Bateria b, String nombreBateria) {
        String tituloTarea = "Cambio de batería: " + nombreBateria;
        boolean yaExiste = tareaRepository.existsByTituloAndEstadoNot(tituloTarea, EstadoTarea.COMPLETADA);
        if (!yaExiste) {
            Tarea tarea = new Tarea();
            tarea.setTitulo(tituloTarea);
            tarea.setDescripcion("Batería con " + b.getCiclosCarga() + " ciclos de carga (límite: " + CICLOS_CARGA_MAX + "). "
                    + "Reemplazar la batería " + nombreBateria + " para mantener la seguridad operativa.");
            tarea.setEstado(EstadoTarea.PENDIENTE);
            tarea.setPrioridad(PrioridadTarea.ALTA);
            tareaRepository.save(tarea);
            log.info("Tarea de cambio de batería creada automáticamente para: {}", nombreBateria);
        }
    }

    // ─── Telemetría MQTT ───────────────────────────────────────────────────────

    private void generarAlertasTelemetria(Set<String> clavesGeneradas) {
        // Drones con temperatura de batería > 50°C (dato de última telemetría MQTT)
        List<com.gestion.qnt.model.Dron> dronesCalientes = dronRepository.findByBateriaTempCGreaterThan(TEMP_BATERIA_MAX);
        for (com.gestion.qnt.model.Dron d : dronesCalientes) {
            String nombre = (d.getNombre() != null && !d.getNombre().isBlank())
                    ? d.getNombre()
                    : (d.getMarca() != null ? d.getMarca() + " " : "") + (d.getModelo() != null ? d.getModelo() : "Dron #" + d.getId());
            String clave = TipoAlerta.TELEMETRIA.name() + "_DRON_TEMP_BAT_" + d.getId();
            clavesGeneradas.add(clave);
            crearSiNoExiste(
                    TipoAlerta.TELEMETRIA, NivelAlerta.CRITICA,
                    nombre + ": temperatura de batería alta",
                    "Temperatura actual: " + d.getBateriaTempC() + "°C — límite " + TEMP_BATERIA_MAX + "°C",
                    "DRON", d.getId(), clave
            );
        }
    }

    // ─── Resolver obsoletas ───────────────────────────────────────────────────

    /** Resuelve alertas cuya condición ya no aplica (la clave no está en el set generado). */
    private void resolverAlertasObsoletas(Set<String> clavesGeneradas) {
        List<Alerta> activas = alertaRepository.findByResueltaFalseOrderByNivelAscFechaCreacionDesc();
        for (Alerta a : activas) {
            if (a.getClaveDedup() != null && !clavesGeneradas.contains(a.getClaveDedup())) {
                a.setResuelta(true);
                a.setFechaResolucion(LocalDateTime.now());
                alertaRepository.save(a);
                log.info("Alerta resuelta automáticamente: {}", a.getClaveDedup());
            }
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private void crearSiNoExiste(TipoAlerta tipo, NivelAlerta nivel, String mensaje,
                                  String subtitulo, String entidadTipo, Long entidadId, String clave) {
        if (!alertaRepository.existsByClaveDedup(clave)) {
            Alerta a = new Alerta(tipo, nivel, mensaje, subtitulo, entidadTipo, entidadId);
            a.setClaveDedup(clave);
            alertaRepository.save(a);
            log.info("Nueva alerta creada: {}", clave);
        }
    }

    private String formatNombreBateria(Bateria b) {
        if (b.getNombre() != null && !b.getNombre().isBlank()) return b.getNombre();
        String s = "";
        if (b.getMarca() != null) s += b.getMarca() + " ";
        if (b.getModelo() != null) s += b.getModelo();
        return s.isBlank() ? "Batería #" + b.getId() : s.trim();
    }
}
