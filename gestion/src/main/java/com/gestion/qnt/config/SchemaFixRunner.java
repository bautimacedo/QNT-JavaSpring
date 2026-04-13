package com.gestion.qnt.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Aplica correcciones de schema que Hibernate no puede manejar solo
 * (e.g. eliminar NOT NULL en columnas que el ORM insiste en re-agregar).
 * Corre una sola vez después de que Hibernate termina su ddl-auto=update.
 */
@Component
public class SchemaFixRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void fixSchema() {
        // piloto_id es nullable: el piloto se asigna al lanzar la misión, no al crearla
        jdbcTemplate.execute(
            "ALTER TABLE misiones ALTER COLUMN piloto_id DROP NOT NULL"
        );
    }
}
