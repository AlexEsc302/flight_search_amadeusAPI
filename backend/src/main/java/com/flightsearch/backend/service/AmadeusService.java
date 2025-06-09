package com.flightsearch.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.flightsearch.backend.config.AmadeusConfig;
import com.flightsearch.backend.dto.AirlineDTO;
import com.flightsearch.backend.dto.AirportDTO;
import com.flightsearch.backend.dto.FlightSearchResultDTO;
import com.flightsearch.backend.dto.FlightSegmentDTO;
import com.flightsearch.backend.dto.PriceDTO;
import com.flightsearch.backend.dto.StopDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AmadeusService {
    
    private static final Logger logger = LoggerFactory.getLogger(AmadeusService.class);
    
    private final WebClient webClient;
    private final AmadeusConfig amadeusConfig;
    private String accessToken;
    
    public AmadeusService(AmadeusConfig amadeusConfig) {
        this.amadeusConfig = amadeusConfig;
        // Create a WebClient that will make HTTP requests to Amadeus API
        this.webClient = WebClient.builder()
            .baseUrl(amadeusConfig.getBaseUrl()) // https://test.api.amadeus.com
            .build();
    }
    
    /**
     * Get Access Token
     * Amadeus API requires OAuth2 authentication
     */
    public Mono<String> getAccessToken() {
        logger.info("Getting access token from Amadeus...");
        
        // Encode our API key and secret in Base64 (required by OAuth2)
        String credentials = Base64.getEncoder()
            .encodeToString((amadeusConfig.getKey() + ":" + amadeusConfig.getSecret()).getBytes());
        
        // Make POST request to get token
        return webClient.post()
            .uri("/v1/security/oauth2/token")
            .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .bodyValue("grant_type=client_credentials")
            .retrieve()
            .bodyToMono(JsonNode.class) // Convert response to JSON
            .map(response -> {
                // Extract the access token from the response
                accessToken = response.get("access_token").asText();
                logger.info("Successfully got access token: {}", accessToken.substring(0, 10) + "...");
                return accessToken;
            })
            .doOnError(error -> {
                logger.error("Failed to get access token: {}", error.getMessage());
            });
    }
    
    /**
     * Simple Airport Search
     * Search for airports using a keyword
     */
    public Mono<JsonNode> searchAirportsSimple(String keyword) {
        logger.info("Searching for airports with keyword: {}", keyword);
        
        // First get the access token, then use it to search airports
        return getAccessToken()
            .flatMap(token -> {
                logger.info("Using token to search airports...");
                
                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/v1/reference-data/locations")
                        .queryParam("subType", "AIRPORT")
                        .queryParam("keyword", keyword)
                        .queryParam("page[limit]", 5) // Limit to 5 results
                        .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class);
            })
            .doOnSuccess(response -> {
                logger.info("Successfully got airport search response");
            })
            .doOnError(error -> {
                logger.error("Airport search failed: {}", error.getMessage());
            });
    }

    /**
     * Flight Search
     * Search for flights based on criteria
     */

    public Mono<List<FlightSearchResultDTO>> searchFlights(String origin, String destination, String departureDate,
                                                     Integer adults, String currency, Boolean nonStop, String returnDate) {
        logger.info("Searching flights from {} to {} on {}, {} adults, currency: {}, nonStop: {}, returnDate: {}", origin, destination, departureDate, adults, currency, nonStop, returnDate);

        return getAccessToken()
            .flatMap(token -> {
                logger.info("Using token to search flights...");

                // Make the initial flight offers API call
                return webClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                            .path("/v2/shopping/flight-offers")
                            .queryParam("originLocationCode", origin)
                            .queryParam("destinationLocationCode", destination)
                            .queryParam("departureDate", departureDate)
                            .queryParam("adults", adults)
                            .queryParam("currencyCode", currency)
                            .queryParam("nonStop", nonStop)
                            .queryParam("max", 5);
                        if (returnDate != null && !returnDate.isEmpty()) {
                            builder.queryParam("returnDate", returnDate);
                        }
                        return builder.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .doOnSuccess(response -> logger.info("Successfully received raw flight search response"))
                    .doOnError(error -> logger.error("Raw flight search failed: {}", error.getMessage()))
                    .flatMap(rawFlightResponse -> { // Now, after getting the raw flight response
                        // Extract all unique airport IATA codes from the raw response
                        Set<String> uniqueAirportCodes = new HashSet<>();
                        JsonNode dataNode = rawFlightResponse.get("data");
                        if (dataNode != null && dataNode.isArray()) {
                            for (JsonNode offer : dataNode) {
                                JsonNode itineraries = offer.get("itineraries");
                                if (itineraries != null && itineraries.isArray()) {
                                    for (JsonNode itinerary : itineraries) {
                                        JsonNode segmentsArray = itinerary.get("segments");
                                        if (segmentsArray != null && segmentsArray.isArray()) {
                                            for (JsonNode segment : segmentsArray) {
                                                if (segment.has("departure") && segment.get("departure").has("iataCode")) {
                                                    uniqueAirportCodes.add(segment.get("departure").get("iataCode").asText());
                                                }
                                                if (segment.has("arrival") && segment.get("arrival").has("iataCode")) {
                                                    uniqueAirportCodes.add(segment.get("arrival").get("iataCode").asText());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // For each unique airport code, fetch its full details using searchAirportsSimple
                        // We'll create a Map<String, String> of IATA_CODE -> FULL_AIRPORT_NAME
                        Map<String, String> fullAirportNamesMap = new ConcurrentHashMap<>(); // Use ConcurrentHashMap for Flux.flatMap operations
                        
                        if (uniqueAirportCodes.isEmpty()) {
                            // If no airport codes found, proceed directly with mapping (names will be IATA codes)
                            logger.warn("No airport codes found in flight offers response. Skipping airport name lookup.");
                            return Mono.just(mapToFlightSearchResultInternal(rawFlightResponse, fullAirportNamesMap));
                        }

                        // Use Flux.flatMap for parallel/sequential calls to searchAirportsSimple
                        // and then collect them into a map.
                        return Flux.fromIterable(uniqueAirportCodes)
                            .flatMap(iataCode -> {
                                // Call searchAirportsSimple for each unique IATA code
                                return searchAirportsSimple(iataCode) // This method already returns Mono<JsonNode>
                                    .map(airportDetailsNode -> {
                                        if (airportDetailsNode != null && airportDetailsNode.has("data") && airportDetailsNode.get("data").isArray()) {
                                            JsonNode firstAirport = airportDetailsNode.get("data").get(0); // Take the first result
                                            if (firstAirport != null) {
                                                String airportName = safeGetText(firstAirport, "name"); // Prefer 'name' for airport
                                                if (airportName == null) { // Fallback to cityName if 'name' is empty/null (e.g., if it's a city entry)
                                                    airportName = safeGetText(firstAirport, "address", "cityName");
                                                }
                                                if (airportName == null) { // Fallback to detailedName, and clean it up
                                                    String detailedName = safeGetText(firstAirport, "detailedName");
                                                    if (detailedName != null && detailedName.contains(":")) {
                                                        airportName = detailedName.substring(detailedName.indexOf(":") + 1).trim();
                                                    } else {
                                                        airportName = detailedName;
                                                    }
                                                }
                                                if (airportName != null) {
                                                    fullAirportNamesMap.put(iataCode, airportName);
                                                    logger.debug("Fetched full airport name for {}: {}", iataCode, airportName);
                                                } else {
                                                    logger.warn("Could not find a suitable name for airport IATA: {}", iataCode);
                                                    fullAirportNamesMap.put(iataCode, iataCode); // Fallback to IATA code if no name found
                                                }
                                            }
                                        }
                                        return iataCode; // Return something to continue the Flux stream
                                    })
                                    .onErrorResume(e -> {
                                        logger.error("Error fetching airport details for {}: {}", iataCode, e.getMessage());
                                        fullAirportNamesMap.put(iataCode, iataCode); // Put IATA code as fallback
                                        return Mono.empty(); // Continue with other airport lookups
                                    });
                            })
                            .then(Mono.defer(() -> { // After all airport lookups are done, map the flights
                                logger.info("Finished fetching all airport names. Proceeding to map flight offers.");
                                return Mono.just(mapToFlightSearchResultInternal(rawFlightResponse, fullAirportNamesMap));
                            }));
                    });
            })
            .doOnSuccess(response -> logger.info("Successfully mapped flight search response with airport names"))
            .doOnError(error -> logger.error("Flight search failed: {}", error.getMessage()));
    }


    // --- Internal mapping method ---
    private List<FlightSearchResultDTO> mapToFlightSearchResultInternal(JsonNode jsonNode, Map<String, String> fullAirportNamesMap) {
        List<FlightSearchResultDTO> flightOffers = new ArrayList<>();

        // Helper maps to get names from Amadeus dictionaries (for airlines mainly now)
        // We will still populate airlineNames from dictionaries as they are usually sufficient
        Map<String, String> airlineNames = new HashMap<>();

        JsonNode dictionaries = jsonNode.get("dictionaries");
        if (dictionaries != null) {
            // We no longer rely on dictionaries.locations for full names, but keep it for completeness if needed
            // JsonNode locations = dictionaries.get("locations");
            // if (locations != null) { ... } // This part can be removed or simplified

            JsonNode carriers = dictionaries.get("carriers");
            if (carriers != null) {
                carriers.fields().forEachRemaining(entry -> {
                    String iataCode = entry.getKey();
                    String name = entry.getValue().asText();
                    airlineNames.put(iataCode, name);
                });
                logger.debug("Mapped Airline Names (from flight offers dictionaries): {}", airlineNames);
            }
        } else {
            logger.warn("No 'dictionaries' found in Amadeus API response. Airline names might be missing.");
        }

        JsonNode dataNode = jsonNode.get("data");
        if (dataNode == null || !dataNode.isArray() || dataNode.isEmpty()) {
            logger.warn("No 'data' (flight offers) found in Amadeus API response or it's not an array/empty.");
            return flightOffers;
        }

        for (JsonNode offer : dataNode) {
            String offerId = offer.get("id") != null ? offer.get("id").asText() : null; // This is the Amadeus Offer ID
            logger.debug("Processing flight offer ID: {}", offerId);

            int numberOfAdults = 1;
            JsonNode travelerPricingsNode = offer.get("travelerPricings");
            if (travelerPricingsNode != null && travelerPricingsNode.isArray()) {
                numberOfAdults = travelerPricingsNode.size();
            } else {
                logger.warn("travelerPricings not found or not array for offer ID: {}. Defaulting adults to 1.", offerId);
            }

            JsonNode priceNode = offer.get("price");
            PriceDTO offerPrice = null;
            if (priceNode != null) {
                offerPrice = new PriceDTO();
                offerPrice.setCurrency(safeGetText(priceNode, "currency"));
                offerPrice.setTotal(safeGetText(priceNode, "grandTotal"));
                offerPrice.setBase(safeGetText(priceNode, "base"));

                // Fees calculation (as before)
                if (offerPrice.getBase() != null && offerPrice.getTotal() != null) {
                    try {
                        double base = Double.parseDouble(offerPrice.getBase());
                        double total = Double.parseDouble(offerPrice.getTotal());
                        offerPrice.setFees(String.format("%.2f", (total - base)));
                    } catch (NumberFormatException e) {
                        logger.warn("Could not parse price numbers for fees calculation for offer ID {}: {}", offerId, e.getMessage());
                        offerPrice.setFees(null);
                    }
                } else {
                    JsonNode feesArray = priceNode.get("fees");
                    if (feesArray != null && feesArray.isArray()) {
                        double totalFees = 0.0;
                        for (JsonNode fee : feesArray) {
                            if (fee.has("amount")) {
                                try {
                                    totalFees += fee.get("amount").asDouble();
                                } catch (Exception e) {
                                    logger.warn("Error parsing individual fee amount for offer ID {}: {}", offerId, e.getMessage());
                                }
                            }
                        }
                        offerPrice.setFees(String.format("%.2f", totalFees));
                    } else {
                        offerPrice.setFees(null);
                    }
                }

                // Price per traveler (as before)
                if (travelerPricingsNode != null && travelerPricingsNode.isArray() && travelerPricingsNode.size() > 0) {
                    JsonNode firstTravelerPricing = travelerPricingsNode.get(0);
                    if (firstTravelerPricing.has("price") && firstTravelerPricing.get("price").has("total")) {
                        offerPrice.setPricePerAdult(firstTravelerPricing.get("price").get("total").asText());
                    } else {
                        logger.warn("pricePerAdult not found in travelerPricings for offer ID: {}", offerId);
                        offerPrice.setPricePerAdult(offerPrice.getTotal());
                    }
                } else {
                    logger.warn("travelerPricings array not found or empty for offer ID: {}", offerId);
                    offerPrice.setPricePerAdult(offerPrice.getTotal());
                }
            } else {
                logger.warn("No 'price' node found for offer ID: {}", offerId);
            }

            JsonNode itineraries = offer.get("itineraries");
            if (itineraries != null && itineraries.isArray() && itineraries.size() > 0) {
                // If there's only one itinerary, it's a one-way flight.
                // If there are two, it's a roundtrip (typically).
                boolean isRoundTrip = itineraries.size() > 1;

                for (int itineraryIndex = 0; itineraryIndex < itineraries.size(); itineraryIndex++) {
                    JsonNode itinerary = itineraries.get(itineraryIndex);
                    FlightSearchResultDTO result = new FlightSearchResultDTO();
                    
                    // Unique ID for each itinerary (e.g., "1-0", "1-1")
                    result.setId(offerId + "-" + itineraryIndex);
                    
                    // Set the parentOfferId for grouping
                    // Only set if it's part of a roundtrip, otherwise it's null (or same as its own ID)
                    if (isRoundTrip) {
                        result.setParentOfferId(offerId); // Set original Amadeus offer ID as parent
                    } else {
                        result.setParentOfferId(null); // Or set to its own ID if you prefer to indicate no parent
                    }

                    result.setNumberOfAdults(numberOfAdults);
                    result.setPrice(offerPrice); // Associate the offer's total price with each itinerary

                    result.setDuration(itinerary.get("duration") != null ? itinerary.get("duration").asText() : null);

                    List<FlightSegmentDTO> segments = new ArrayList<>();
                    List<StopDTO> stops = new ArrayList<>();
                    LocalDateTime previousSegmentArrival = null;

                    JsonNode segmentsArray = itinerary.get("segments");
                    if (segmentsArray != null && segmentsArray.isArray() && segmentsArray.size() > 0) {
                        for (int i = 0; i < segmentsArray.size(); i++) {
                            JsonNode segment = segmentsArray.get(i);
                            FlightSegmentDTO flightSegment = new FlightSegmentDTO();

                            String departureIataCode = safeGetText(segment, "departure", "iataCode");
                            String departureDateTime = safeGetText(segment, "departure", "at");
                            String arrivalIataCode = safeGetText(segment, "arrival", "iataCode");
                            String arrivalDateTime = safeGetText(segment, "arrival", "at");
                            String carrierCode = safeGetText(segment, "carrierCode");
                            String number = safeGetText(segment, "number");
                            String segmentDuration = safeGetText(segment, "duration");
                            String operatingCarrierCode = null;
                            if (segment.has("operating") && segment.get("operating").has("carrierCode")) {
                                operatingCarrierCode = segment.get("operating").get("carrierCode").asText();
                            }

                            flightSegment.setDepartureIataCode(departureIataCode);
                            flightSegment.setDepartureDateTime(departureDateTime);
                            flightSegment.setArrivalIataCode(arrivalIataCode);
                            flightSegment.setArrivalDateTime(arrivalDateTime);
                            flightSegment.setCarrierCode(carrierCode);
                            flightSegment.setNumber(number);
                            flightSegment.setDuration(segmentDuration);
                            flightSegment.setOperatingCarrierCode(operatingCarrierCode);

                            segments.add(flightSegment);

                            // Set overall departure and arrival times from the first/last segment of THIS itinerary
                            if (i == 0) {
                                result.setDepartureDateTime(departureDateTime);
                                result.setDepartureAirport(new AirportDTO(departureIataCode, fullAirportNamesMap.getOrDefault(departureIataCode, departureIataCode)));
                                result.setAirline(new AirlineDTO(carrierCode, airlineNames.getOrDefault(carrierCode, carrierCode)));
                                if (operatingCarrierCode != null && !operatingCarrierCode.equals(carrierCode)) {
                                    result.setOperatingAirline(new AirlineDTO(operatingCarrierCode, airlineNames.getOrDefault(operatingCarrierCode, operatingCarrierCode)));
                                } else {
                                    result.setOperatingAirline(null);
                                }
                            }
                            if (i == segmentsArray.size() - 1) {
                                result.setArrivalDateTime(arrivalDateTime);
                                result.setArrivalAirport(new AirportDTO(arrivalIataCode, fullAirportNamesMap.getOrDefault(arrivalIataCode, arrivalIataCode)));
                            }

                            // Calculate layover time and add stops (as before)
                            if (previousSegmentArrival != null && departureDateTime != null) {
                                try {
                                    LocalDateTime currentSegmentDeparture = LocalDateTime.parse(departureDateTime);
                                    Duration layoverDuration = Duration.between(previousSegmentArrival, currentSegmentDeparture);

                                    if (!layoverDuration.isNegative() && !layoverDuration.isZero()) {
                                        String stopAirportCode = safeGetText(segmentsArray.get(i-1), "arrival", "iataCode");
                                        stops.add(new StopDTO(new AirportDTO(stopAirportCode, fullAirportNamesMap.getOrDefault(stopAirportCode, stopAirportCode)), layoverDuration.toString()));
                                    }
                                } catch (DateTimeParseException e) {
                                    logger.warn("Could not parse date/time for layover calculation for offer ID {}: {}", offerId, e.getMessage());
                                } catch (IllegalArgumentException e) {
                                    logger.warn("IllegalArgumentException during layover calculation for offer ID {}: {}", offerId, e.getMessage());
                                }
                            }
                            if (arrivalDateTime != null) {
                                try {
                                    previousSegmentArrival = LocalDateTime.parse(arrivalDateTime);
                                } catch (DateTimeParseException e) {
                                    logger.warn("Could not parse arrivalDateTime for previousSegmentArrival tracking for offer ID {}: {}", offerId, e.getMessage());
                                    previousSegmentArrival = null;
                                }
                            } else {
                                previousSegmentArrival = null;
                            }
                        } // End of segments loop
                    } else {
                        logger.warn("No 'segments' found or not array/empty for itinerary {} of offer ID: {}", itineraryIndex, offerId);
                    }
                    result.setSegments(segments);
                    result.setStops(stops);
                    flightOffers.add(result); // Add THIS itinerary's result to the list
                } // End of itineraries loop
            } else {
                logger.warn("No 'itineraries' found or not array/empty for offer ID: {}", offerId);
            }
        } // End of offers loop
        return flightOffers;
    }
    private String safeGetText(JsonNode parent, String fieldName) {
        if (parent != null && parent.has(fieldName) && parent.get(fieldName) != null) {
            return parent.get(fieldName).asText();
        }
        return null;
    }

    // Helper method for safe JsonNode text extraction (nested fields, varargs)
    private String safeGetText(JsonNode parent, String... path) {
        JsonNode currentNode = parent;
        for (String field : path) {
            if (currentNode == null || !currentNode.has(field) || currentNode.get(field) == null) {
                return null; // Return null if any part of the path is missing or null
            }
            currentNode = currentNode.get(field);
        }
        return currentNode != null ? currentNode.asText() : null;
    }
}