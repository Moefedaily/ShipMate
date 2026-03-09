package com.shipmate.controller.Geocode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/geocoding")
public class GeocodingController {

    private final RestTemplate restTemplate;

    public GeocodingController() {
        this.restTemplate = new RestTemplate();
    }

    @GetMapping("/search")
    public ResponseEntity<String> search(@RequestParam String q) {
        String url = "https://nominatim.openstreetmap.org/search"
                + "?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                + "&format=json&addressdetails=1&limit=5";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "ShipMate/1.0 (contact@shipmate.app)");
        headers.set("Accept", "application/json");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }
}