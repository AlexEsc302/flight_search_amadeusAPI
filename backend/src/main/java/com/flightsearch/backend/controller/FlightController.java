package com.flightsearch.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightsearch.backend.dto.FlightDetailsResponseDTO;
import com.flightsearch.backend.service.AmadeusService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000") // Allow React frontend
public class FlightController {

    private static final Logger logger = LoggerFactory.getLogger(FlightController.class);
    private final AmadeusService amadeusService;
    private final ObjectMapper objectMapper;

    public FlightController(AmadeusService amadeusService) {
        this.amadeusService = amadeusService;
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        logger.info("Health check called");
        return ResponseEntity.ok("Backend is running!");
    }

    @GetMapping("/test-token")
    public Mono<ResponseEntity<String>> testToken() {
        logger.info("Testing Amadeus API token...");

        return amadeusService.getAccessToken()
            .map(token -> {
                String tokenPreview = token.substring(0, Math.min(10, token.length())) + "...";
                return ResponseEntity.ok("Token obtained successfully: " + tokenPreview);
            })
            .onErrorReturn(ResponseEntity.internalServerError()
                .body("Failed to get token. Check your API credentials."));
    }

    @GetMapping("/airports")
    public Mono<ResponseEntity<JsonNode>> searchAirports(@RequestParam String keyword) {
        logger.info("Airport search called with keyword: {}", keyword);

        if (keyword == null || keyword.trim().length() < 2) {
            return Mono.just(ResponseEntity.badRequest().body(createErrorJson("Keyword must be at least 2 characters long.")));
        }

        return amadeusService.searchAirportsSimple(keyword.trim())
            .map(response -> {
                logger.info("Airport search successful");
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                logger.error("Airport search failed: {}", error.getMessage(), error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorJson("An unexpected error occurred during airport search.")));
            });
    }

    /**
     * Flight search
     * GET /api/flights?origin=LAX&destination=JFK&departureDate=2025-07-15&adults=1&currency=USD&nonStop=false&returnDate=2025-07-20
     */
    @GetMapping("/flights")
    public Mono<ResponseEntity<Object>> searchFlights( 
        @RequestParam String origin,
        @RequestParam String destination,
        @RequestParam String departureDate,
        @RequestParam(defaultValue = "1") Integer adults,
        @RequestParam(defaultValue = "USD") String currency,
        @RequestParam(defaultValue = "false") Boolean nonStop,
        @RequestParam(required = false) String returnDate
    ) {
        logger.info("Flight search request received: origin={}, destination={}, departureDate={}, adults={}, currency={}, nonStop={}, returnDate={}",
            origin, destination, departureDate, adults, currency, nonStop, returnDate);

        // --- VALIDACIONES DE FECHA ---
        LocalDate parsedDepartureDate;
        try {
            parsedDepartureDate = LocalDate.parse(departureDate);
        } catch (DateTimeParseException e) {
            logger.warn("Invalid departureDate format: {}", departureDate);
            return Mono.just(ResponseEntity.badRequest().body(createErrorJson("Invalid departure date format. Please use YYYY-MM-DD.")));
        }

        LocalDate today = LocalDate.now(); // Current date 
        if (parsedDepartureDate.isBefore(today)) {
            logger.warn("Departure date {} is in the past (today is {}).", departureDate, today);
            return Mono.just(ResponseEntity.badRequest().body(createErrorJson("Departure date cannot be in the past.")));
        }

        if (returnDate != null && !returnDate.isEmpty()) {
            LocalDate parsedReturnDate;
            try {
                parsedReturnDate = LocalDate.parse(returnDate);
            } catch (DateTimeParseException e) {
                logger.warn("Invalid returnDate format: {}", returnDate);
                return Mono.just(ResponseEntity.badRequest().body(createErrorJson("Invalid return date format. Please use YYYY-MM-DD.")));
            }

            if (parsedReturnDate.isBefore(parsedDepartureDate)) {
                logger.warn("Return date {} is before departure date {}.", returnDate, departureDate);
                return Mono.just(ResponseEntity.badRequest().body(createErrorJson("Return date cannot be before departure date.")));
            }
            if (parsedReturnDate.isEqual(parsedDepartureDate)) {
                logger.warn("Return date {} is same as departure date {}. For roundtrip flights, return date cannot be the same as departure date.", returnDate, departureDate);
                return Mono.just(ResponseEntity.badRequest().body(createErrorJson("For roundtrip flights, return date cannot be the same as departure date.")));
            }
        }

        // Llamar al servicio
        return amadeusService.searchFlights(origin, destination, departureDate, adults, currency, nonStop, returnDate)
            .map(result -> ResponseEntity.ok().<Object>body(result)) 
            .onErrorResume(error -> {
                logger.error("Error during flight search: {}", error.getMessage(), error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorJson("An unexpected error occurred during flight search. Please try again later.")));
            });
    }

    /**
     * Flight details endpoint
     * GET /api/flights/{amadeusOfferId}/details
     * amadeusOfferId is the original Amadeus ID, NOT the one suffixed with -0 or -1
     */
    @GetMapping("/flights/{amadeusOfferId}/details")
    public Mono<ResponseEntity<Object>> getFlightDetails(@PathVariable String amadeusOfferId) {
        logger.info("Received request for flight details for Amadeus Offer ID: {}", amadeusOfferId);

        return amadeusService.getFlightOfferDetails(amadeusOfferId) // Mono<JsonNode> (this is now the *individual flight offer* JsonNode)
            .flatMap(individualFlightOfferNode -> { // Renamed for clarity: it's the cached flight offer
                final JsonNode mainFlightOfferNode = individualFlightOfferNode; // No need to extract further, this IS the offer

                Set<String> uniqueAirportCodes = new HashSet<>();
                // Populate uniqueAirportCodes using mainFlightOfferNode (the correct node)
                JsonNode itineraries = mainFlightOfferNode.get("itineraries");
                if (itineraries != null && itineraries.isArray()) {
                    for (JsonNode itinerary : itineraries) {
                        JsonNode segmentsArray = itinerary.get("segments");
                        if (segmentsArray != null && segmentsArray.isArray()) {
                            for (JsonNode segment : segmentsArray) {
                                if (segment.has("departure") && segment.get("departure").has("iataCode")) {
                                    uniqueAirportCodes.add(amadeusService.safeGetText(segment.get("departure"), "iataCode"));
                                }
                                if (segment.has("arrival") && segment.get("arrival").has("iataCode")) {
                                    uniqueAirportCodes.add(amadeusService.safeGetText(segment.get("arrival"), "iataCode"));
                                }
                            }
                        }
                    }
                }

                Map<String, String> fullAirportNamesMap = new ConcurrentHashMap<>();
                Mono<FlightDetailsResponseDTO> resultMono; 

                if (uniqueAirportCodes.isEmpty()) {
                    logger.warn("No airport codes found in flight offer details response for offer ID: {}. Skipping airport name lookup.", amadeusOfferId);
                    resultMono = Mono.just(amadeusService.mapToFlightDetailsResponseDTO(amadeusOfferId, mainFlightOfferNode, fullAirportNamesMap));
                } else {
                    resultMono = Flux.fromIterable(uniqueAirportCodes)
                        .flatMap(iataCode -> 
                            amadeusService.searchAirportsSimple(iataCode)
                                .map(airportDetailsNode -> {
                                    String airportName = null;
                                    if (airportDetailsNode != null && airportDetailsNode.has("data") && airportDetailsNode.get("data").isArray() && airportDetailsNode.get("data").size() > 0) {
                                        JsonNode airportData = airportDetailsNode.get("data").get(0);
                                        airportName = amadeusService.safeGetText(airportData, "name");
                                        if (airportName == null) {
                                            JsonNode addressNode = airportData.get("address");
                                            if (addressNode != null) {
                                                airportName = amadeusService.safeGetText(addressNode, "cityName");
                                            }
                                        }
                                        if (airportName == null) {
                                            String detailedName = amadeusService.safeGetText(airportData, "detailedName");
                                            if (detailedName != null && detailedName.contains(":")) {
                                                airportName = detailedName.substring(detailedName.indexOf(":") + 1).trim();
                                            } else {
                                                airportName = detailedName;
                                            }
                                        }
                                    }
                                    fullAirportNamesMap.put(iataCode, airportName != null ? airportName : iataCode);
                                    return (Void) null;
                                })
                                .onErrorResume(e -> {
                                    logger.error("Error fetching airport details for {}: {}. Falling back to IATA code.", iataCode, e.getMessage());
                                    fullAirportNamesMap.put(iataCode, iataCode);
                                    return Mono.empty();
                                })
                        )
                        .then(Mono.defer(() -> {
                            logger.info("Finished fetching all airport names for flight details for offer ID: {}. Mapping details response.", amadeusOfferId);
                            return Mono.just(amadeusService.mapToFlightDetailsResponseDTO(amadeusOfferId, mainFlightOfferNode, fullAirportNamesMap));
                        }));
                }
                
                return resultMono.map(detailsDTO -> ResponseEntity.ok().<Object>body(detailsDTO)); // Moved .map here
            })
            .onErrorResume(error -> {
                logger.error("Error during flight details fetch for offer ID {}: {}", amadeusOfferId, error.getMessage(), error);
                if (error instanceof IllegalArgumentException) {
                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorJson(error.getMessage())));
                }
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorJson("An unexpected error occurred while fetching flight details. Please try again later.")));
            });
    }

    private JsonNode createErrorJson(String message) {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", message);
        return error;
    }
}