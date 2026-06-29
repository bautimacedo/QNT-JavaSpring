package com.gestion.qnt.clima;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Config de estaciones Tempest (una por site/yacimiento). Reemplaza los @Value sueltos
 * para soportar múltiples estaciones. Análogo a {@link ClimaProperties}.
 */
@Component
@ConfigurationProperties(prefix = "tempest")
public class TempestProperties {

    private String token;
    private String baseUrl = "https://swd.weatherflow.com/swd/rest";
    private List<SiteConfig> sites = new ArrayList<>();

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public List<SiteConfig> getSites() { return sites; }
    public void setSites(List<SiteConfig> sites) { this.sites = sites; }

    public static class SiteConfig {
        private String code;            // CL, EFO, CAM, ...
        private String name;
        private String stationId;       // ID de estación WeatherFlow
        private String deviceId = "";   // device ID (fallback), opcional
        private double windOffset = 180; // corrección de veleta, por estación
        private double lat;
        private double lon;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getStationId() { return stationId; }
        public void setStationId(String stationId) { this.stationId = stationId; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public double getWindOffset() { return windOffset; }
        public void setWindOffset(double windOffset) { this.windOffset = windOffset; }
        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }
    }
}
