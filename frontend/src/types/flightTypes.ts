// frontend/src/types/flightTypes.ts

// Types for flight search parameters to send to the backend
export interface FlightSearchParams {
  originLocationCode: string;
  destinationLocationCode: string;
  departureDate: string; // Format YYYY-MM-DD
  adults: number;
  currency: string; // Example: "USD", "MXN", "EUR"
  nonStop: boolean;
  returnDate?: string; // Optional, format YYYY-MM-DD
}

// Tipos para los resultados de la búsqueda de vuelos (FlightSearchResultDTO del backend)
export interface FlightSearchResult {
  id: string; // Backend ID
  parentOfferId?: string | null; // Amadeus ID
  numberOfAdults: number;
  price: PriceDTO;
  duration: string; // Example: "PT10H30M" (ISO 8601 duration)
  departureDateTime: string; // Date and hour, ej. "2025-10-26T14:30:00"
  departureAirport: AirportDTO; 
  arrivalDateTime: string; 
  arrivalAirport: AirportDTO;
  airline: AirlineDTO;
  operatingAirline?: AirlineDTO | null; 
  segments: FlightSegmentDTO[]; // Segments of the flight or scales
  stops: StopDTO[]; // Scales, if any
  isRoundTrip?: boolean; 
}

export interface GroupedFlightOffer {
    offerId: string; 
    outboundFlight: FlightSearchResult; 
    inboundFlight?: FlightSearchResult; 
    totalPrice: PriceDTO; 
    numberOfAdults: number; 
    isRoundTrip: boolean;
    totalDuration?: string; 
}

// Price information DTO
export interface PriceDTO {
  currency: string;
  total: string; // Example. "123.45"
  base: string;
  fees?: string | null; // Total - base
  pricePerAdult?: string | null;
}

// Airtport information DTO
export interface AirportDTO {
  code: string; // Código IATA
  name: string; // Nombre completo (ej. "Los Angeles (LAX)")
}

// Airline Information DTO
export interface AirlineDTO {
  code: string; // IATA code of the airline (ej. "AA" for American Airlines)
  name: string; // Airline name (ej. "American Airlines")
}

// Flight segment information DTO
export interface FlightSegmentDTO {
  departureIataCode: string;
  departureDateTime: string;
  arrivalIataCode: string;
  arrivalDateTime: string;
  carrierCode: string; 
  number: string; 
  duration: string; // Duration, Example: "PT2H0M"
  operatingCarrierCode?: string | null; 
}

// Stop information DTO
export interface StopDTO {
  airportCode: string;
  airportName: string;
  layoverDuration: string; 
}

export interface FlightDetailsResponseDTO {
  id: string;
  numberOfAdults: number;
  totalPrice: PriceDTO;
  itineraries: ItineraryDTO[]; 
}

export interface ItineraryDTO {
  id: string;
  duration: string; // Total itinerary duration (Example: "PT12H45M")
  direction: "OUTBOUND" | "INBOUND"; // Dirección del vuelo
  departureDateTime: string;
  departureAirport: AirportDTO;
  arrivalDateTime: string;
  arrivalAirport: AirportDTO;
  segments: DetailedSegmentDTO[]; 
  stops: StopDTO[]; 
}

export interface DetailedSegmentDTO {
  id?: string; 
  departureIataCode: string;
  departureDateTime: string;
  arrivalIataCode: string;
  arrivalDateTime: string;
  carrierCode: string; 
  number: string;
  duration: string;
  operatingCarrierCode?: string | null; 
  operatingCarrierName?: string | null; 
  aircraftCode?: string | null; 
  aircraftName?: string | null; 
  aircraftTypeName?: string | null; 
  departureAirportName?: string; 
  arrivalAirportName?: string; 
  airlineName?: string; 
  travelerFareDetails: FareDetailDTO[]; 
}

export interface FareDetailDTO {
  travelerType: string; 
  fareBasis: string; 
  class: string; 
  cabin: string; 
  amenities: AmenityDTO[];
  brandedFare?: string; 
  classCode?: string; 
}

export interface AmenityDTO {
  description: string; 
  isChargeable: boolean; 
  amenityType?: string; 
  chargeable: boolean;
}