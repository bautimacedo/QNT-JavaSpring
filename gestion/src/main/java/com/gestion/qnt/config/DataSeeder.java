package com.gestion.qnt.config;

import com.gestion.qnt.model.Site;
import com.gestion.qnt.repository.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SiteRepository siteRepository;

    public DataSeeder(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedSite("CL", "Cañadón León");
    }

    private void seedSite(String codigo, String nombre) {
        if (siteRepository.findByCodigo(codigo).isEmpty()) {
            Site site = new Site();
            site.setCodigo(codigo);
            site.setNombre(nombre);
            siteRepository.save(site);
            log.info("DataSeeder: site '{}' ({}) creado", codigo, nombre);
        }
    }
}
