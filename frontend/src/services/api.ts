// frontend/src/services/api.ts
import { AirportSuggestion } from '../types/airportTypes'; 
import { FlightSearchResult, FlightSearchParams, FlightDetailsResponseDTO } from '../types/flightTypes';

const API_BASE_URL = 'http://localhost:8080/api'; 

export const searchAirports = async (keyword: string): Promise<AirportSuggestion[]> => {
  try {
    const response = await fetch(`${API_BASE_URL}/airports?keyword=${encodeURIComponent(keyword)}`);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const data = await response.json();
    return data.data.map((airport: any) => ({ 
      code: airport.iataCode, 
      name: airport.name || airport.cityName || airport.iataCode, 
    }));
  } catch (error) {
    console.error('Error searching airports:', error);
    // Return an empty array so the UI doesn't break
    return [];
  }
};

export const searchFlights = async (params: FlightSearchParams): Promise<FlightSearchResult[]> => {
  try {
    // Constructs query parameters
    const queryParams = new URLSearchParams({
      origin: params.originLocationCode,
      destination: params.destinationLocationCode,
      departureDate: params.departureDate,
      adults: params.adults.toString(), 
      currency: params.currency || 'USD', 
      nonStop: params.nonStop?.toString() || 'false', 
    });

    // Add returnDate if it exists
    if (params.returnDate) {
      queryParams.append('returnDate', params.returnDate);
    }

    // Complete URL with query parameters
    const url = `${API_BASE_URL}/flights?${queryParams.toString()}`; 

    const response = await fetch(url, {
      method: 'GET'
    });

    if (!response.ok) {
      const errorText = await response.text();
      let errorMessage = `HTTP error! status: ${response.status}`;
      try {
        const errorData = JSON.parse(errorText);
        errorMessage = errorData.message || errorMessage;
      } catch (parseError) {
        errorMessage = errorText || errorMessage;
      }
      throw new Error(errorMessage);
    }
    return response.json();
  } catch (error) {
    console.error('Error searching flights:', error);
    throw error;
  }
};


export const fetchFlightDetails = async (offerId: string): Promise<FlightDetailsResponseDTO> => {
  try {
    const response = await fetch(`${API_BASE_URL}/flights/${offerId}/details`);
    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Failed to fetch flight details');
    }
    return response.json();
  } catch (error) {
    console.error(`Error in fetchFlightDetails for offerId ${offerId}:`, error);
    throw error;
  }
};