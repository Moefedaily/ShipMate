package com.shipmate.unit.controller.Geocode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.shipmate.controller.Geocode.GeocodingController;

class GeocodingControllerTest {

    private GeocodingController controller;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        controller = new GeocodingController();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(controller, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void search_shouldProxyEncodedQueryWithExpectedHeaders() {
        server.expect(requestTo("https://nominatim.openstreetmap.org/search?q=Paris+France&format=json&addressdetails=1&limit=5"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("User-Agent", "ShipMate/1.0 (contact@shipmate.app)"))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("[{\"display_name\":\"Paris, France\"}]", MediaType.APPLICATION_JSON));

        var response = controller.search("Paris France");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Paris, France");
        server.verify();
    }
}
