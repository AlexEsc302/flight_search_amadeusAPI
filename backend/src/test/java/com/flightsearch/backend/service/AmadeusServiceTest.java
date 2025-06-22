package com.flightsearch.backend.service;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.DisplayName;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightsearch.backend.config.AmadeusConfig;
import com.flightsearch.backend.dto.FlightDetailsResponseDTO;
import com.flightsearch.backend.dto.FlightSearchResultDTO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.MockResponse;

@DisplayName("AmadeusServiceTest")
public class AmadeusServiceTest {

    private MockWebServer mockWebServer;
    private AmadeusService amadeusService;
    private WebTestClient webTestClient;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Mock
    private AmadeusConfig amadeusConfig;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        mockWebServer = new MockWebServer();
        mockWebServer.start();
        // Mock the AmadeusConfig to return the mock server URL
        when(amadeusConfig.getBaseUrl()).thenReturn(mockWebServer.url("/").toString());
        when(amadeusConfig.getKey()).thenReturn("testApiKey");
        when(amadeusConfig.getSecret()).thenReturn("testApiSecret");

        amadeusService = new AmadeusService(amadeusConfig);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Should successfully get an access token")
    void getAccessToken_success() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"access_token\":\"testAccessToken\",\"token_type\":\"Bearer\",\"expires_in\":3600}"));

        Mono<String> accessTokenMono = amadeusService.getAccessToken();

        StepVerifier.create(accessTokenMono)
                .expectNext("testAccessToken")
                .verifyComplete();
    }

    @Test 
    @DisplayName("Should handle error when getting access token")
    void getAccessToken_error() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"error\":\"invalid_client\",\"error_description\":\"Invalid client credentials\"}"));

        Mono<String> accessTokenMono = amadeusService.getAccessToken();

        StepVerifier.create(accessTokenMono)
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("Should successfully search airports")
    void searchAirportsSimple_success() throws IOException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"access_token\":\"testAccessToken\",\"token_type\":\"Bearer\",\"expires_in\":3600}"));

        String airportSearchResponseJson = "{" +
            "\"data\": [" +
            "    {\"iataCode\": \"MEX\", \"name\": \"Mexico City International Airport\", \"address\": {\"cityName\": \"Mexico City\"}}," +
            "    {\"iataCode\": \"CUN\", \"name\": \"Cancun International Airport\", \"address\": {\"cityName\": \"Cancun\"}}" +
            "]," +
            "\"meta\": {\"count\": 2}" +
            "}";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(airportSearchResponseJson));

        Mono<JsonNode> airportsMono = amadeusService.searchAirportsSimple("MEX");
        
        StepVerifier.create(airportsMono)
            .assertNext(jsonNode -> {
                assertNotNull(jsonNode, "The JsonNode response should not be null");
                assertTrue(jsonNode.has("data"), "Response should have a 'data' field");
                assertTrue(jsonNode.get("data").isArray(), "'data' field should be an array");
                assertEquals(2, jsonNode.get("data").size(), "Expected 2 airport results");
                assertEquals("MEX", jsonNode.get("data").get(0).get("iataCode").asText(), "First airport should be MEX");
                assertEquals("Cancun International Airport", jsonNode.get("data").get(1).get("name").asText(), "Second airport name should match");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle error when searching airports")
    void searchAirportsSimple_error() throws IOException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"access_token\":\"testAccessToken\",\"token_type\":\"Bearer\",\"expires_in\":3600}"));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"errors\": [{\"status\": 500, \"code\": 1000, \"title\": \"Internal server error on Amadeus side\"}]}")
        );
        
        
        Mono<JsonNode> airportsMono = amadeusService.searchAirportsSimple("MEX");

        StepVerifier.create(airportsMono)
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("Should successfully search for flights and map results")
    void searchFlights_success() throws IOException {

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"access_token\":\"testAccessToken\",\"token_type\":\"Bearer\",\"expires_in\":3600}"));

        String flightOffersResponse = """
            {
                "dictionaries": {
                    "carriers": {
                        "AA": "American Airlines"
                    }
                },
                "data": [
                    {
                        "id": "1",
                        "price": {
                            "currency": "USD",
                            "grandTotal": "500.00",
                            "base": "450.00"
                        },
                        "travelerPricings": [
                            {
                                "price": {
                                    "total": "500.00"
                                }
                            }
                        ],
                        "itineraries": [
                            {
                                "duration": "PT2H",
                                "segments": [
                                    {
                                        "departure": { "iataCode": "MEX", "at": "2025-07-01T08:00:00" },
                                        "arrival": { "iataCode": "LAX", "at": "2025-07-01T10:00:00" },
                                        "carrierCode": "AA",
                                        "number": "100",
                                        "duration": "PT2H",
                                        "operating": {
                                            "carrierCode": "AA"
                                        }
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(flightOffersResponse));

        String airportSearchMEX = """
            {
                "data": [
                    {
                        "iataCode": "MEX",
                        "name": "Mexico City Intl Airport",
                        "address": {
                            "cityName": "Mexico City"
                        }
                    }
                ]
            }
            """;

        String airportSearchLAX = """
            {
                "data": [
                    {
                        "iataCode": "LAX",
                        "name": "Los Angeles Intl Airport",
                        "address": {
                            "cityName": "Los Angeles"
                        }
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(airportSearchMEX));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(airportSearchLAX));

        Mono<List<FlightSearchResultDTO>> resultMono = amadeusService.searchFlights(
            "MEX", "LAX", "2025-07-01", 1, "USD", true, null
        );

        StepVerifier.create(resultMono)
            .assertNext(result -> {
                assertNotNull(result);
                assertEquals(1, result.size());

                FlightSearchResultDTO flight = result.get(0);
                assertEquals("1-0", flight.getId());
                assertEquals("MEX", flight.getDepartureAirport().getCode());
                assertEquals("LAX", flight.getArrivalAirport().getCode());
                assertEquals("AA", flight.getAirline().getCode());
                assertEquals("American Airlines", flight.getAirline().getName());
                assertEquals("500.00", flight.getPrice().getTotal());
                assertEquals("USD", flight.getPrice().getCurrency());
                assertEquals("PT2H", flight.getDuration());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle error when flight search API fails")
    void searchFlights_errorFromAmadeus() throws IOException {
        
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"access_token\":\"testAccessToken\",\"token_type\":\"Bearer\",\"expires_in\":3600}"));

            
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("""
                {
                    "errors": [
                        {
                            "status": 500,
                            "code": 1234,
                            "title": "Internal Server Error",
                            "detail": "Something went wrong on the server"
                        }
                    ]
                }
            """));

        Mono<List<FlightSearchResultDTO>> resultMono = amadeusService.searchFlights(
            "MEX", "LAX", "2025-07-01", 1, "USD", true, null
        );

        StepVerifier.create(resultMono)
            .expectErrorMatches(error -> error instanceof RuntimeException &&
                error.getMessage().contains("500") 
            )
            .verify();
        }

    @Test
    void testGetFlightOfferDetails_validId_shouldReturnMonoWithJsonNode() {
        // Arrange
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode offerNode = objectMapper.createObjectNode();

        // Price node
        ObjectNode priceNode = objectMapper.createObjectNode();
        priceNode.put("base", "200.00");
        priceNode.put("grandTotal", "250.00");
        priceNode.put("currency", "USD");
        offerNode.set("price", priceNode);

        // Traveler pricings
        ArrayNode travelerPricings = objectMapper.createArrayNode();
        ObjectNode traveler = objectMapper.createObjectNode();
        traveler.put("travelerId", "1");

        ObjectNode travelerPriceNode = objectMapper.createObjectNode();
        travelerPriceNode.put("total", "250.00");
        traveler.set("price", travelerPriceNode);

        traveler.set("fareDetailsBySegment", objectMapper.createArrayNode()); // Optional
        travelerPricings.add(traveler);
        offerNode.set("travelerPricings", travelerPricings);

        // Itineraries with one segment
        ArrayNode itineraries = objectMapper.createArrayNode();
        ObjectNode itinerary = objectMapper.createObjectNode();
        itinerary.put("duration", "PT2H");

        ArrayNode segments = objectMapper.createArrayNode();
        ObjectNode segment = objectMapper.createObjectNode();
        segment.put("id", "1");
        segment.put("carrierCode", "AM");
        segment.put("number", "198");
        segment.put("duration", "PT2H");

        ObjectNode departure = objectMapper.createObjectNode();
        departure.put("iataCode", "MEX");
        departure.put("at", "2025-06-20T08:00");
        segment.set("departure", departure);

        ObjectNode arrival = objectMapper.createObjectNode();
        arrival.put("iataCode", "CUN");
        arrival.put("at", "2025-06-20T10:30");
        segment.set("arrival", arrival);

        ObjectNode aircraft = objectMapper.createObjectNode();
        aircraft.put("code", "737");
        segment.set("aircraft", aircraft);

        ObjectNode operating = objectMapper.createObjectNode();
        operating.put("carrierCode", "AM");
        segment.set("operating", operating);

        segments.add(segment);
        itinerary.set("segments", segments);
        itineraries.add(itinerary);
        offerNode.set("itineraries", itineraries);

        // Put in cache
        amadeusService.getFlightOffersCache().put("TEST123", offerNode);

        Map<String, String> airportNamesMap = new HashMap<>();
        airportNamesMap.put("MEX", "Mexico City");
        airportNamesMap.put("CUN", "Cancun");

        // Act
        Mono<JsonNode> resultMono = amadeusService.getFlightOfferDetails("TEST123");

        StepVerifier.create(resultMono)
            .assertNext(result -> {
                assertNotNull(result);
                // Validate mapping
                FlightDetailsResponseDTO dto = amadeusService.mapToFlightDetailsResponseDTO("TEST123", result, airportNamesMap);
                assertEquals("TEST123", dto.getAmadeusOfferId());
                assertNotNull(dto.getTotalPrice());
                assertEquals("250.00", dto.getTotalPrice().getTotal());
                assertEquals("50.00", dto.getTotalPrice().getFees()); // 250 - 200
                assertEquals(1, dto.getNumberOfAdults());
                assertEquals("250.00", dto.getTotalPrice().getPricePerAdult());

                assertFalse(dto.getItineraries().isEmpty());
                assertEquals("OUTBOUND", dto.getItineraries().get(0).getDirection());
            })
            .verifyComplete();
    }

    
}
