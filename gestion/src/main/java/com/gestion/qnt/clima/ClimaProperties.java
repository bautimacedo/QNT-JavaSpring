package com.gestion.qnt.clima;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "owm")
public class ClimaProperties {

    private String apiKey;
    private String url;
    private List<SiteConfig> sites = new ArrayList<>();

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public List<SiteConfig> getSites() { return sites; }
    public void setSites(List<SiteConfig> sites) { this.sites = sites; }

    public static class SiteConfig {
        private String code;
        private String name;
        private double lat;
        private double lon;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }

        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }
    }
}
