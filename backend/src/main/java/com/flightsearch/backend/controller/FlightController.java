package com.flightsearch.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightsearch.backend.service.AmadeusService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
            .map(result -> ResponseEntity.ok().<Object>body(result)) // <--- Crucial change here
            .onErrorResume(error -> {
                logger.error("Error during flight search: {}", error.getMessage(), error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorJson("An unexpected error occurred during flight search. Please try again later.")));
            });
    }

    private JsonNode createErrorJson(String message) {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("error", message);
        return error;
    }
}