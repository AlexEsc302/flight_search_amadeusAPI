package com.flightsearch.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import com.flightsearch.backend.config.AmadeusConfig;
import com.flightsearch.backend.dto.AirlineDTO;
import com.flightsearch.backend.dto.AirportDTO;
import com.flightsearch.backend.dto.FlightSearchResultDTO;
import com.flightsearch.backend.dto.FlightSegmentDTO;
import com.flightsearch.backend.dto.ItineraryDTO;
import com.flightsearch.backend.dto.PriceDTO;
import com.flightsearch.backend.dto.DetailedSegmentDTO;
import com.flightsearch.backend.dto.FlightDetailsResponseDTO;
import com.flightsearch.backend.dto.StopDTO; 
import com.flightsearch.backend.dto.FareDetailDTO;
import com.flightsearch.backend.dto.AmenityDTO; 


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
import java.util.Objects; 

@Service
public class AmadeusService {

    private static final Logger logger = LoggerFactory.getLogger(AmadeusService.class);

    private final WebClient webClient;
    private final AmadeusConfig amadeusConfig;
    private String accessToken;

    // --- CACHE FOR FLIGHT OFFERS ---
    // Stores the raw JsonNode of the flight offer by its Amadeus 'id'
    private final Map<String, JsonNode> flightOffersCache = new ConcurrentHashMap<>();
    // --- END CACHE ---

    // Static maps for airline and aircraft names
    private static final Map<String, String> airlineNamesMap = new HashMap<>();
    private static final Map<String, String> aircraftTypeNamesMap = new HashMap<>();

    // Initialize the maps
    static {
        // Airline names (IATA code -> Full Name)
        airlineNamesMap.put("AA", "AMERICAN AIRLINES");
        airlineNamesMap.put("AC", "AIR CANADA");
        airlineNamesMap.put("DL", "DELTA AIR LINES");
        airlineNamesMap.put("UA", "UNITED AIRLINES");
        airlineNamesMap.put("SW", "SOUTHWEST AIRLINES");
        airlineNamesMap.put("F9", "FRONTIER AIRLINES");
        airlineNamesMap.put("NK", "SPIRIT AIRLINES");
        airlineNamesMap.put("KE", "KOREAN AIR");
        airlineNamesMap.put("AF", "AIR FRANCE");
        airlineNamesMap.put("LH", "LUFTHANSA");
        airlineNamesMap.put("BA", "BRITISH AIRWAYS");
    }

    static {
        // Aircraft type names (Amadeus code -> Full Name)
        aircraftTypeNamesMap.put("74H", "BOEING 747-8");
        aircraftTypeNamesMap.put("7M8", "BOEING 737 MAX 8");
        aircraftTypeNamesMap.put("32A", "AIRBUS A320"); 
        aircraftTypeNamesMap.put("320", "AIRBUS A320");
        aircraftTypeNamesMap.put("321", "AIRBUS A321");
        aircraftTypeNamesMap.put("319", "AIRBUS A319");
        aircraftTypeNamesMap.put("223", "AIRBUS A220-300"); 
        aircraftTypeNamesMap.put("32Q", "AIRBUS A320neo");
        aircraftTypeNamesMap.put("738", "BOEING 737-800");
        aircraftTypeNamesMap.put("77L", "BOEING 777-200LR");
        aircraftTypeNamesMap.put("789", "BOEING 787-9 Dreamliner");
    }


    public AmadeusService(AmadeusConfig amadeusConfig) {
        this.amadeusConfig = amadeusConfig;
        this.webClient = WebClient.builder()
            .baseUrl(amadeusConfig.getBaseUrl())
            .build();
    }

    /**
     * Get Access Token
     * Amadeus API requires OAuth2 authentication
     */
    public Mono<String> getAccessToken() {
        logger.info("Getting access token from Amadeus...");

        String credentials = Base64.getEncoder()
            .encodeToString((amadeusConfig.getKey() + ":" + amadeusConfig.getSecret()).getBytes());

        return webClient.post()
            .uri("/v1/security/oauth2/token")
            .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .bodyValue("grant_type=client_credentials")
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> {
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

        return getAccessToken()
            .flatMap(token -> {
                logger.info("Using token to search airports...");

                return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/v1/reference-data/locations")
                        .queryParam("subType", "AIRPORT")
                        .queryParam("keyword", keyword)
                        .queryParam("page[limit]", 5)
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
                    .flatMap(rawFlightResponse -> {

                        JsonNode dataNodeForCaching = rawFlightResponse.get("data");
                        if (dataNodeForCaching != null && dataNodeForCaching.isArray()) {
                            for (JsonNode offerToCache : dataNodeForCaching) {
                                if (offerToCache.has("id")) {
                                    String amadeusOfferId = offerToCache.get("id").asText();
                                    flightOffersCache.put(amadeusOfferId, offerToCache);
                                    logger.debug("Cached flight offer with ID: {}", amadeusOfferId);
                                }
                            }
                        } else {
                             logger.warn("No 'data' found in raw flight response for caching purposes.");
                        }

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

                        Map<String, String> fullAirportNamesMap = new ConcurrentHashMap<>();

                        if (uniqueAirportCodes.isEmpty()) {
                            logger.warn("No airport codes found in flight offers response. Skipping airport name lookup.");
                            return Mono.just(mapToFlightSearchResultInternal(rawFlightResponse, fullAirportNamesMap));
                        }

                        return Flux.fromIterable(uniqueAirportCodes)
                            .flatMap(iataCode -> {
                                return searchAirportsSimple(iataCode)
                                    .map(airportDetailsNode -> {
                                        if (airportDetailsNode != null && airportDetailsNode.has("data") && airportDetailsNode.get("data").isArray()) {
                                            JsonNode firstAirport = airportDetailsNode.get("data").get(0);
                                            if (firstAirport != null) {
                                                String airportName = safeGetText(firstAirport, "name");
                                                if (airportName == null) {
                                                    airportName = safeGetText(firstAirport, "address", "cityName");
                                                }
                                                if (airportName == null) {
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
                                                    fullAirportNamesMap.put(iataCode, iataCode);
                                                }
                                            }
                                        }
                                        return iataCode;
                                    })
                                    .onErrorResume(e -> {
                                        logger.error("Error fetching airport details for {}: {}", iataCode, e.getMessage());
                                        fullAirportNamesMap.put(iataCode, iataCode);
                                        return Mono.just(iataCode);
                                    });
                            })
                            .then(Mono.defer(() -> {
                                logger.info("Finished fetching all airport names. Proceeding to map flight offers.");
                                return Mono.just(mapToFlightSearchResultInternal(rawFlightResponse, fullAirportNamesMap));
                            }));
                    });
            })
            .doOnSuccess(response -> logger.info("Successfully mapped flight search response with airport names"))
            .doOnError(error -> logger.error("Flight search failed: {}", error.getMessage()));
    }

    public Map<String, JsonNode> getFlightOffersCache() {
        return flightOffersCache;
    }

    // --- Internal mapping method ---
    private List<FlightSearchResultDTO> mapToFlightSearchResultInternal(JsonNode jsonNode, Map<String, String> fullAirportNamesMap) {
        List<FlightSearchResultDTO> flightOffers = new ArrayList<>();

        Map<String, String> airlineNames = new HashMap<>(); // This is local to search mapping for now

        JsonNode dictionaries = jsonNode.get("dictionaries");
        if (dictionaries != null) {
            JsonNode carriers = dictionaries.get("carriers");
            if (carriers != null) {
                carriers.properties().forEach(entry -> {
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
            String offerId = offer.get("id") != null ? offer.get("id").asText() : null;
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

                if (offerPrice.getBase() != null && offerPrice.getTotal() != null) {
                    try {
                        double base = Double.parseDouble(offerPrice.getBase());
                        double total = Double.parseDouble(offerPrice.getTotal());
                        double fees = total - base;
                        offerPrice.setFees(String.format("%.2f", (fees)));
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

            // --- Mapping method Itineraries ---
            JsonNode itineraries = offer.get("itineraries");
            if (itineraries != null && itineraries.isArray() && itineraries.size() > 0) {
                boolean isRoundTrip = itineraries.size() > 1;

                for (int itineraryIndex = 0; itineraryIndex < itineraries.size(); itineraryIndex++) {
                    JsonNode itinerary = itineraries.get(itineraryIndex);
                    FlightSearchResultDTO result = new FlightSearchResultDTO();

                    result.setId(offerId + "-" + itineraryIndex);

                    if (isRoundTrip) {
                        result.setParentOfferId(offerId);
                    } else {
                        result.setParentOfferId(null);
                    }

                    result.setNumberOfAdults(numberOfAdults);
                    result.setPrice(offerPrice);

                    result.setDuration(itinerary.get("duration") != null ? itinerary.get("duration").asText() : null);

                    List<FlightSegmentDTO> segments = new ArrayList<>();
                    List<StopDTO> stops = new ArrayList<>(); 
                    LocalDateTime previousSegmentArrival = null;

                    JsonNode segmentsArray = itinerary.get("segments");
                    if (segmentsArray != null && segmentsArray.isArray() && segmentsArray.size() > 0) {
                        for (int i = 0; i < segmentsArray.size(); i++) {
                            JsonNode segment = segmentsArray.get(i);
                            FlightSegmentDTO flightSegment = new FlightSegmentDTO();

                            String departureIataCode = safeGetText(segment.get("departure"), "iataCode"); 
                            String departureDateTime = safeGetText(segment.get("departure"), "at");     
                            String arrivalIataCode = safeGetText(segment.get("arrival"), "iataCode");     
                            String arrivalDateTime = safeGetText(segment.get("arrival"), "at");         
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

                            if (i == 0) {
                                result.setDepartureDateTime(departureDateTime);
                                result.setDepartureAirport(new AirportDTO(departureIataCode, fullAirportNamesMap.getOrDefault(departureIataCode, departureIataCode)));
                                result.setAirline(new AirlineDTO(carrierCode, airlineNames.getOrDefault(carrierCode, carrierCode)));
                                if (operatingCarrierCode != null && !Objects.equals(operatingCarrierCode, carrierCode)) { // Use Objects.equals for string comparison
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
                                        String stopAirportCode = safeGetText(segmentsArray.get(i-1).get("arrival"), "iataCode"); // Access nested arrival node
                                        String stopAirportName = fullAirportNamesMap.getOrDefault(stopAirportCode, stopAirportCode); // Get name from map

                                        StopDTO stopDto = new StopDTO(); 
                                        stopDto.setAirportCode(stopAirportCode);
                                        stopDto.setAirportName(stopAirportName);
                                        stopDto.setLayoverDuration(formatDuration(layoverDuration)); // Use formatDuration helper

                                        stops.add(stopDto);
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
                    flightOffers.add(result);
                }
            } else {
                logger.warn("No 'itineraries' found or not array/empty for offer ID: {}", offerId);
            }
        }
        return flightOffers;
    }

    // FETCH FLIGHT OFFER DETAILS
    public Mono<JsonNode> getFlightOfferDetails(String amadeusOfferId) {
        logger.info("Attempting to retrieve flight offer details for Amadeus ID: {} from cache.", amadeusOfferId);

        JsonNode cachedOffer = flightOffersCache.get(amadeusOfferId);

        if (cachedOffer == null) {
            logger.error("Flight offer with ID {} not found in cache. This offer cannot be detailed.", amadeusOfferId);
            // It's critical that the offer is in the cache from the initial search.
            // If not, there's no way to get its details without re-running a search.
            return Mono.error(new IllegalArgumentException("Flight offer details for ID " + amadeusOfferId + " not found or expired in cache. Please perform a new search."));
        } else {
            logger.info("Successfully retrieved flight offer ID {} from cache for detailed mapping.", amadeusOfferId);
            // This is the full original flight offer from the search.
            return Mono.just(cachedOffer);
        }
    }

    //--- mapToFlightDetailsResponseDTO ---
    public FlightDetailsResponseDTO mapToFlightDetailsResponseDTO(String amadeusOfferId, JsonNode flightOffer, Map<String, String> airportNamesMap) {
        FlightDetailsResponseDTO dto = new FlightDetailsResponseDTO();
        dto.setAmadeusOfferId(amadeusOfferId);

        JsonNode travelerPricingsNode = flightOffer.get("travelerPricings");

        // --- TotalPrice ---
        JsonNode priceNode = flightOffer.get("price");
        if (priceNode != null) {
            PriceDTO priceDTO = new PriceDTO();
            priceDTO.setBase(safeGetText(priceNode, "base"));
            priceDTO.setTotal(safeGetText(priceNode, "grandTotal")); 
            priceDTO.setCurrency(safeGetText(priceNode, "currency"));

            // Calculate fees as total - base
            if (priceDTO.getBase() != null && priceDTO.getTotal() != null) {
                try {
                    double base = Double.parseDouble(priceDTO.getBase());
                    double total = Double.parseDouble(priceDTO.getTotal());
                    double Fees = total - base;
                    priceDTO.setFees(String.format("%.2f", Fees));
                } catch (NumberFormatException e) {
                    logger.warn("Could not parse price numbers for fees calculation for offer ID {}: {}. Setting fees to null.", amadeusOfferId, e.getMessage());
                    priceDTO.setFees(null); // Set to null on error
                }
            } else {
                logger.warn("Base or GrandTotal price is null for offer ID {}. Cannot calculate fees. Setting fees to null.", amadeusOfferId);
                priceDTO.setFees(null);
            }

            int numberOfAdults = 0;
            if (travelerPricingsNode != null && travelerPricingsNode.isArray()) {
                numberOfAdults = ((ArrayNode) travelerPricingsNode).size();
                dto.setNumberOfAdults(numberOfAdults);

                if (travelerPricingsNode.size() > 0) {
                    JsonNode firstTravelerPricing = travelerPricingsNode.get(0);
                    if (firstTravelerPricing.has("price") && firstTravelerPricing.get("price").has("total")) {
                        priceDTO.setPricePerAdult(firstTravelerPricing.get("price").get("total").asText());
                    } else {
                        logger.warn("pricePerAdult not found in travelerPricings for offer ID: {}. Falling back to total price.", amadeusOfferId);
                        priceDTO.setPricePerAdult(priceDTO.getTotal()); // Fallback
                    }
                } else {
                    logger.warn("travelerPricings array empty for offer ID: {}. Falling back to total price.", amadeusOfferId);
                    priceDTO.setPricePerAdult(priceDTO.getTotal()); // Fallback
                }
            } else {
                logger.warn("travelerPricings array not found for offer ID: {}. Setting numberOfAdults to 0. Falling back to total price.", amadeusOfferId);
                dto.setNumberOfAdults(0);
                priceDTO.setPricePerAdult(priceDTO.getTotal()); // Fallback
            }

            dto.setTotalPrice(priceDTO);
        } else {
            logger.warn("No 'price' node found in FlightOffer response for offer ID: {}", amadeusOfferId);
        }

        // --- Itineraries ---
        List<ItineraryDTO> itineraries = new ArrayList<>();
        JsonNode itinerariesNode = flightOffer.get("itineraries");
        if (itinerariesNode != null && itinerariesNode.isArray()) {
            int itineraryIndex = 0;
            for (JsonNode itineraryNode : itinerariesNode) {
                ItineraryDTO itineraryDTO = new ItineraryDTO();
                itineraryDTO.setId(amadeusOfferId + "-" + itineraryIndex++);
                itineraryDTO.setDuration(safeGetText(itineraryNode, "duration"));

                itineraryDTO.setDirection(itineraryIndex == 1 ? "OUTBOUND" : "INBOUND");


                JsonNode segmentsNode = itineraryNode.get("segments");
                if (segmentsNode != null && segmentsNode.isArray() && segmentsNode.size() > 0) {
                    JsonNode firstSegment = segmentsNode.get(0);
                    JsonNode lastSegment = segmentsNode.get(segmentsNode.size() - 1);

                    // This is where is set the departure and arrival times for the ITINERARY
                    // They are taken from the first and last segments of that itinerary.
                    itineraryDTO.setDepartureDateTime(safeGetText(firstSegment.get("departure"), "at"));
                    itineraryDTO.setArrivalDateTime(safeGetText(lastSegment.get("arrival"), "at"));

                    // Departure Airport for Itinerary (from the first segment's departure)
                    AirportDTO departureAirport = new AirportDTO();
                    departureAirport.setCode(safeGetText(firstSegment.get("departure"), "iataCode"));
                    departureAirport.setName(airportNamesMap.getOrDefault(departureAirport.getCode(), departureAirport.getCode())); 
                    itineraryDTO.setDepartureAirport(departureAirport);

                    // Arrival Airport for Itinerary (from the last segment's arrival)
                    AirportDTO arrivalAirport = new AirportDTO();
                    arrivalAirport.setCode(safeGetText(lastSegment.get("arrival"), "iataCode"));
                    arrivalAirport.setName(airportNamesMap.getOrDefault(arrivalAirport.getCode(), arrivalAirport.getCode())); 
                    itineraryDTO.setArrivalAirport(arrivalAirport);
                }


                // Stops (layovers)
                List<StopDTO> stops = new ArrayList<>();
                if (segmentsNode != null && segmentsNode.isArray()) {
                    // Loop through segments to find layovers. A layover exists between N and N+1 segments.
                    for (int i = 0; i < segmentsNode.size() - 1; i++) {
                        JsonNode currentSegment = segmentsNode.get(i);
                        JsonNode nextSegment = segmentsNode.get(i + 1);

                        String stopAirportCode = safeGetText(currentSegment.get("arrival"), "iataCode");
                        String nextDepartureTimeStr = safeGetText(nextSegment.get("departure"), "at");
                        String currentArrivalTimeStr = safeGetText(currentSegment.get("arrival"), "at");

                        if (stopAirportCode != null && nextDepartureTimeStr != null && currentArrivalTimeStr != null) {
                            try {
                                // Parse dates and times to calculate layover duration
                                LocalDateTime nextDepartureTime = LocalDateTime.parse(nextDepartureTimeStr);
                                LocalDateTime currentArrivalTime = LocalDateTime.parse(currentArrivalTimeStr);
                                Duration layoverDuration = Duration.between(currentArrivalTime, nextDepartureTime);

                                StopDTO stopDTO = new StopDTO();
                                stopDTO.setAirportCode(stopAirportCode);
                                // Lookup airport name from a dictionary if available
                                stopDTO.setAirportName(stopAirportCode); // Placeholder
                                stopDTO.setLayoverDuration("PT" + layoverDuration.toHours() + "H" + layoverDuration.toMinutesPart() + "M");
                                stops.add(stopDTO);
                            } catch (DateTimeParseException e) {
                                logger.warn("Error parsing date/time for layover duration for offer ID {}. Current Arrival: '{}', Next Departure: '{}'. Details: {}", amadeusOfferId, currentArrivalTimeStr, nextDepartureTimeStr, e.getMessage());
                            }
                        }
                    }
                }
                itineraryDTO.setStops(stops);


                // Mapping of Segments
                List<DetailedSegmentDTO> detailedSegments = new ArrayList<>();
                if (segmentsNode != null && segmentsNode.isArray()) {
                    for (JsonNode segmentNode : segmentsNode) {
                        DetailedSegmentDTO detailedSegment = new DetailedSegmentDTO();

                        String currentSegmentId = safeGetText(segmentNode, "id"); 
                        logger.debug("Processing segment with ID: {}", currentSegmentId);

                        detailedSegment.setDepartureIataCode(safeGetText(segmentNode.get("departure"), "iataCode"));
                        detailedSegment.setArrivalIataCode(safeGetText(segmentNode.get("arrival"), "iataCode"));
                        detailedSegment.setDepartureDateTime(safeGetText(segmentNode.get("departure"), "at"));
                        detailedSegment.setArrivalDateTime(safeGetText(segmentNode.get("arrival"), "at"));
                        detailedSegment.setCarrierCode(safeGetText(segmentNode, "carrierCode"));
                        detailedSegment.setNumber(safeGetText(segmentNode, "number"));
                        detailedSegment.setDuration(safeGetText(segmentNode, "duration"));
                        detailedSegment.setOperatingCarrierCode(safeGetText(segmentNode.get("operating"), "carrierCode"));
                        detailedSegment.setAircraftCode(safeGetText(segmentNode.get("aircraft"), "code"));

                        // Lookup names using predefined maps or a more comprehensive dictionary
                        detailedSegment.setDepartureAirportName(detailedSegment.getDepartureIataCode()); 
                        detailedSegment.setArrivalAirportName(detailedSegment.getArrivalIataCode());  

                        detailedSegment.setAirlineName(getAirlineName(detailedSegment.getCarrierCode()));
                        detailedSegment.setOperatingAirlineName(getAirlineName(detailedSegment.getOperatingCarrierCode()));
                        detailedSegment.setAircraftTypeName(getAircraftTypeName(detailedSegment.getAircraftCode()));

                        // --- Mapping of FareDetails and AMENITIES ---
                        // For each segment, find the corresponding fare details for each traveler
                        List<FareDetailDTO> travelerFareDetailsForSegment = new ArrayList<>();
                        if (travelerPricingsNode != null && travelerPricingsNode.isArray()) {
                            for (JsonNode travelerPricingNode : travelerPricingsNode) {
                                JsonNode fareDetailsBySegmentArrayNode = travelerPricingNode.get("fareDetailsBySegment");
                                if (fareDetailsBySegmentArrayNode != null && fareDetailsBySegmentArrayNode.isArray()) {
                                    for (JsonNode fareDetailNode : fareDetailsBySegmentArrayNode) {
                                        String fareDetailSegmentId = safeGetText(fareDetailNode, "segmentId");
                                        logger.debug("  Comparing fareDetailNode segmentId '{}' with currentSegmentId '{}' for traveler ID '{}'",
                                            fareDetailSegmentId, currentSegmentId, safeGetText(travelerPricingNode, "travelerId"));

                                        // Match current segment's ID with the segmentId in fareDetailsBySegment
                                        if (fareDetailSegmentId != null && fareDetailSegmentId.equals(currentSegmentId)) {
                                            logger.debug("  MATCH FOUND! Processing fareDetailNode for segmentId: {}", currentSegmentId);

                                            FareDetailDTO fareDetail = new FareDetailDTO();
                                            fareDetail.setCabin(safeGetText(fareDetailNode, "cabin"));
                                            fareDetail.setFareBasis(safeGetText(fareDetailNode, "fareBasis"));
                                            fareDetail.setBrandedFare(safeGetText(fareDetailNode, "brandedFare"));
                                            fareDetail.setClassCode(safeGetText(fareDetailNode, "class"));

                                            // Initialize amenities list for this TravelerFareDetail
                                            fareDetail.setAmenities(new ArrayList<>());

                                            JsonNode amenitiesArrayNode = fareDetailNode.get("amenities");
                                            if (amenitiesArrayNode == null) {
                                                logger.debug("    Amenities node is NULL for segmentId {} / fareBasis {}", fareDetailSegmentId, fareDetail.getFareBasis());
                                            } else if (!amenitiesArrayNode.isArray()) {
                                                logger.debug("    Amenities node is NOT an array for segmentId {} / fareBasis {}. Type: {}", fareDetailSegmentId, fareDetail.getFareBasis(), amenitiesArrayNode.getNodeType());
                                            } else if (amenitiesArrayNode.size() == 0) {
                                                logger.debug("    Amenities array is EMPTY for segmentId {} / fareBasis {}.", fareDetailSegmentId, fareDetail.getFareBasis());
                                            } else {
                                                logger.debug("    Amenities array found with size {} for segmentId {} / fareBasis {}.", amenitiesArrayNode.size(), fareDetailSegmentId, fareDetail.getFareBasis());
                                                // Only iterate if the array has elements
                                                for (JsonNode amenityNode : amenitiesArrayNode) {
                                                    AmenityDTO amenityDTO = new AmenityDTO();
                                                    amenityDTO.setDescription(safeGetText(amenityNode, "description"));
                                                    amenityDTO.setChargeable(safeGetBoolean(amenityNode, "isChargeable", false));
                                                    amenityDTO.setAmenityType(safeGetText(amenityNode, "amenityType"));

                                                    fareDetail.getAmenities().add(amenityDTO);
                                                    logger.debug("      Added Amenity: Description='{}'", amenityDTO.getDescription());
                                                }
                                            }
                                            travelerFareDetailsForSegment.add(fareDetail);
                                        } else {
                                            logger.debug("  NO MATCH: fareDetailNode segmentId '{}' != currentSegmentId '{}'", fareDetailSegmentId, currentSegmentId);
                                        }
                                    }
                                }
                            }
                        }
                        detailedSegment.setTravelerFareDetails(travelerFareDetailsForSegment);
                        detailedSegments.add(detailedSegment);
                    }
                }
                itineraryDTO.setSegments(detailedSegments);
                itineraries.add(itineraryDTO);
            }
        }
        dto.setItineraries(itineraries);

        return dto;
    }

    // --- HELPER METHODS ---

    /**
     * Retrieves the full name of an airline given its IATA code.
     * Uses a static map for demonstration purposes.
     * @param carrierCode The IATA airline code (e.g., "F9", "NK").
     * @return The full airline name, or the code if not found in the map.
     */
    private String getAirlineName(String carrierCode) {
        if (carrierCode == null) return null; 
        return airlineNamesMap.getOrDefault(carrierCode, carrierCode);
    }

    /**
     * Retrieves the full type name of an aircraft given its IATA code.
     * Uses a static map for demonstration purposes.
     * @param aircraftCode The IATA aircraft type code (e.g., "320", "74H").
     * @return The full aircraft type name, or the code if not found in the map.
     */
    private String getAircraftTypeName(String aircraftCode) {
        if (aircraftCode == null) return null; 
        return aircraftTypeNamesMap.getOrDefault(aircraftCode, aircraftCode);
    }

    /**
     * Formats a java.time.Duration object into an ISO 8601 duration string (e.g., "PT1H30M").
     * @param duration The Duration object to format.
     * @return The ISO 8601 string representation, or null if input is null.
     */
    private String formatDuration(Duration duration) {
        if (duration == null) {
            return null;
        }
        return duration.toString();
    }

    /**
     * Safely gets text from a JsonNode, returning null if the node or value is missing.
     * Handles nested fields by passing an array of field names.
     * @param node The parent JsonNode.
     * @param fieldNames The names of the fields to retrieve, in hierarchical order.
     * @return The text value of the field, or null if not found.
     */
    public String safeGetText(JsonNode node, String... fieldNames) {
        if (node == null || fieldNames == null || fieldNames.length == 0) {
            return null;
        }
        JsonNode currentNode = node;
        for (String fieldName : fieldNames) {
            if (currentNode != null && currentNode.has(fieldName) && !currentNode.get(fieldName).isNull()) {
                currentNode = currentNode.get(fieldName);
            } else {
                return null;
            }
        }
        return currentNode != null ? currentNode.asText() : null;
    }

   
    /**
     * Helper method to safely get a boolean from a JsonNode.
     *
     * @param node The JsonNode to extract from.
     * @param fieldName The name of the field.
     * @param defaultValue The default value if not found or not boolean.
     * @return The boolean value, or defaultValue.
     */
    private boolean safeGetBoolean(JsonNode node, String fieldName, boolean defaultValue) {
        if (node != null && node.has(fieldName) && node.get(fieldName).isBoolean()) {
            return node.get(fieldName).asBoolean();
        }
        return defaultValue;
    }

}
