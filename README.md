# ✈️ Flight Search App (Amadeus API)

This is a reactive Java Spring Boot application that allows users to search for flights using the [Amadeus for Developers API](https://developers.amadeus.com/).

## 🚀 Features

- 🔎 Search flights by origin, destination, date, number of adults, and optional return date
- 🧾 Map and display flight segments with relevant info
- 💰 Retrieve total prices and currency
- 🌐 Reactive WebClient with `Mono` for non-blocking performance
- 🔐 OAuth2 token management with Amadeus API

## 🛠 Tech Stack

- Java 21
- Spring Boot 3
- Spring WebFlux
- Jackson for JSON parsing
- Amadeus API (Test environment)

## 📦 Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/AlexEsc302/flight_search_amadeusAPI.git
   cd flight_search_amadeusAPI
2. Add your Amadeus credentials in application.properties:

  amadeus.client.id=#YourClient
  amadeus.client.secret=#YourSecret

3. Run the application:
  ./gradlew bootRun

4. Call the endpoint:
  GET /api/flights/search?origin=GDL&destination=MEX&departureDate=2025-07-01&adults=1


## 🧪 Sample Response

{
        "id": "1-0",
        "parentOfferId": null,
        "departureDateTime": "2025-06-15T21:20:00",
        "arrivalDateTime": "2025-06-16T06:00:00",
        "departureAirport": {
            "code": "LAX",
            "name": "LOS ANGELES INTL"
        },
        "arrivalAirport": {
            "code": "JFK",
            "name": "JOHN F KENNEDY INTL"
        },
        "airline": {
            "code": "F9",
            "name": "FRONTIER AIRLINES"
        },
        "operatingAirline": null,
        "duration": "PT5H40M",
        "segments": [
            {
                "departureIataCode": "LAX",
                "arrivalIataCode": "JFK",
                "departureDateTime": "2025-06-15T21:20:00",
                "arrivalDateTime": "2025-06-16T06:00:00",
                "carrierCode": "F9",
                "number": "2504",
                "duration": "PT5H40M",
                "operatingCarrierCode": "F9"
            }
        ],
        "stops": [],
        "price": {
            "base": "157.19",
            "total": "184.29",
            "fees": "27.10",
            "pricePerAdult": "184.29",
            "currency": "USD"
        },
        "numberOfAdults": 1
    }


