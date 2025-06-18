// This file defines the types used for airport suggestions in the application.
export interface AirportSuggestion {
  code: string; // IATA, example "LAX"
  name: string; // Name, example "Los Angeles Intl." o "Mexico City"
}