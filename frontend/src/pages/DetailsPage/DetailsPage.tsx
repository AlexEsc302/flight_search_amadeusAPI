// frontend/src/pages/DetailsPage/DetailsPage.tsx
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchFlightDetails } from '../../services/api';
import { FlightDetailsResponseDTO,  ItineraryDTO,  StopDTO } from '../../types/flightTypes';
import { format, parseISO, isSameDay} from 'date-fns'; 
import styles from './DetailsPage.module.css';

const formatDuration = (isoDuration: string | undefined): string => {
  if (!isoDuration) return '';
  const matches = isoDuration.match(/PT(?:(\d+)H)?(?:(\d+)M)?/);
  if (!matches) return isoDuration;

  const hours = matches[1] ? parseInt(matches[1], 10) : 0;
  const minutes = matches[2] ? parseInt(matches[2], 10) : 0;

  let formatted = '';
  if (hours > 0) formatted += `${hours}h`;
  if (minutes > 0) formatted += ` ${minutes}m`;
  return formatted.trim();
};

const formatCurrency = (amount: string | number, currency: string): string => {
  if (typeof amount === 'string') {
    amount = parseFloat(amount);
  }
  return amount.toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }) + ` ${currency}`;
};

const DetailsPage: React.FC = () => {
  const { offerId } = useParams<{ offerId: string }>();
  const navigate = useNavigate();
  const [flightDetails, setFlightDetails] = useState<FlightDetailsResponseDTO | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!offerId) {
      setError('Flight offer ID not provided.');
      setIsLoading(false);
      return;
    }

    const getFlightDetails = async () => {
      try {
        setIsLoading(true);
        setError(null);
        const data = await fetchFlightDetails(offerId);
        setFlightDetails(data);
      } catch (err) {
        console.error('Error fetching flight details:', err);
        setError('Flight details could not be loaded. Please try again.');
      } finally {
        setIsLoading(false);
      }
    };

    getFlightDetails();
  }, [offerId]);

  const handleReturnToResults = () => {
    navigate(-1);
  };

  if (isLoading) {
    return (
      <div className={styles['details-page']}>
        <p className={styles['loading-message']}>Loading flight details...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className={styles['details-page']}>
        <p className={styles['error-message']}>{error}</p>
        <button onClick={handleReturnToResults} className={styles['return-button']}>
          &lt; Back to Results
        </button>
      </div>
    );
  }

  if (!flightDetails) {
    return (
      <div className={styles['details-page']}>
        <p className={styles['no-details-message']}>No details found for this flight offer.</p>
        <button onClick={handleReturnToResults} className={styles['return-button']}>
          &lt; Back to Results
        </button>
      </div>
    );
  }

  // Componente auxiliar para renderizar un itinerario (ida o vuelta)
  const renderItinerary = (itinerary: ItineraryDTO, stops: StopDTO[] = []) => {
    // Ordenar segmentos por fecha/hora de salida
    const sortedSegments = [...itinerary.segments].sort((a, b) =>
      parseISO(a.departureDateTime).getTime() - parseISO(b.departureDateTime).getTime()
    );

    return (
      <div className={styles['itinerary-section']}>
        <h2>
          {itinerary.direction === 'OUTBOUND' ? 'Outbound' : 'Return'}
          <span className={styles['total-itinerary-duration']}>
            ({formatDuration(itinerary.duration)})
          </span>
        </h2>
        <p className={styles['itinerary-route-summary']}>
          {itinerary.departureAirport.name} ({itinerary.departureAirport.code}) &rarr;{' '}
          {itinerary.arrivalAirport.name} ({itinerary.arrivalAirport.code})
          <br/>
          Date: {format(parseISO(itinerary.departureDateTime), 'EEEE d MMMM')}
        </p>

        {sortedSegments.map((segment, index) => {
          const departureDate = parseISO(segment.departureDateTime);
          const arrivalDate = parseISO(segment.arrivalDateTime);
          const isNextDayArrival = !isSameDay(departureDate, arrivalDate);
          return (
            <React.Fragment key={segment.id}>
              <div className={styles['segment-card']}>
                <div className={styles['segment-header']}>
                  <div className={styles['segment-time']}>
                    <span className={styles['time']}>{format(departureDate, 'HH:mm')}</span>
                    <span className={styles['airport-code']}>{segment.departureAirportName} ({segment.departureIataCode})</span>
                  </div>
                  <div className={styles['segment-arrow-info']}>
                    <span className={styles['arrow']}>&rarr;</span>
                    <span className={styles['duration']}>{formatDuration(segment.duration)}</span>
                  </div>
                  <div className={styles['segment-time']}>
                    <span className={styles['time']}>{format(arrivalDate, 'HH:mm')}</span>
                    <span className={styles['airport-code']}>{segment.arrivalAirportName} ({segment.arrivalIataCode})</span>
                    {isNextDayArrival && (
                      <span className={styles['next-day-indicator']}>+1 day</span>
                    )}
                  </div>
                </div>
                <div className={styles['segment-details']}>
                  <p>
                    Airline: {segment.airlineName} ({segment.carrierCode}) - Flight: {segment.number}
                    {segment.operatingCarrierCode && segment.operatingCarrierCode !== segment.carrierCode && (
                      <span className={styles['operating-carrier']}>
                        {' '} (Operated by: {segment.operatingCarrierName || segment.operatingCarrierCode})
                      </span>
                    )}
                  </p>
                  {segment.aircraftTypeName && (
                    <p>Aircraft: {segment.aircraftTypeName} ({segment.aircraftCode})</p>
                  )}
                </div>

                {segment.travelerFareDetails && segment.travelerFareDetails.length > 0 && (
                  <div className={styles['fare-details']}>
                    <h3>Passenger Fare Details:</h3>
                    {segment.travelerFareDetails.map((fareDetail, fareIndex) => (
                      <div key={fareIndex} className={styles['fare-detail-item']}>
                        <p>Cabin: {fareDetail.cabin}</p>
                        <p>Class Code: {fareDetail.classCode}</p>
                        {fareDetail.amenities && fareDetail.amenities.length > 0 && (
                          <div className={styles['amenities-list']}>
                            <h4>Amenities:</h4>
                            <ul>
                              {fareDetail.amenities.map((amenity, amenityIndex) => (
                                <li key={amenityIndex}>
                                  {amenity.description} ({amenity.chargeable ? 'Chargeable' : 'Free'})
                                </li>
                              ))}
                            </ul>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {index < sortedSegments.length - 1 && (() => {
                const currentSegmentArrivalIata = segment.arrivalIataCode;
                const nextSegmentDepartureIata = sortedSegments[index + 1].departureIataCode;

                const matchingStop = stops.find(stop => stop.airportCode === currentSegmentArrivalIata && stop.airportCode === nextSegmentDepartureIata);

                if (matchingStop && matchingStop.layoverDuration) {
                  return (
                    <div className={styles['layover-info']}>
                      <div className={styles['layover-line']}></div>
                      <p className={styles['layover-text']}>
                        Layover of {formatDuration(matchingStop.layoverDuration)} in {matchingStop.airportName || matchingStop.airportCode}
                      </p>
                      <div className={styles['layover-line']}></div>
                    </div>
                  );
                }
                return null;
              })()}
            </React.Fragment>
          );
        })}
      </div>
    );
  };

  // Extract round-trip itineraries
  const outboundItinerary = flightDetails.itineraries.find(it => it.direction === 'OUTBOUND');
  const inboundItinerary = flightDetails.itineraries.find(it => it.direction === 'INBOUND');

  return (
    <div className={styles['details-page']}>
      <div className={styles['header-section']}>
        <button onClick={handleReturnToResults} className={styles['return-button']}>
          &lt; Back to Results
        </button>
        <h1>Flight Details</h1>
      </div>

      <div className={styles['details-content']}>
        <div className={styles['flight-info-column']}>
          {outboundItinerary && renderItinerary(outboundItinerary, outboundItinerary.stops || [])}
          {inboundItinerary && renderItinerary(inboundItinerary, inboundItinerary.stops || [])}
        </div>

        <div className={styles['price-breakdown-column']}>
          <h2>Price breakdown</h2>
          <div className={styles['price-item']}>
            <span>Base Price Per Adult:</span>
            <span>{formatCurrency(parseFloat(flightDetails.totalPrice.base) / flightDetails.numberOfAdults, flightDetails.totalPrice.currency)}</span>
          </div>
          {flightDetails.totalPrice.fees && (
            <div className={styles['price-item']}>
              <span>Fees Per Adult:</span>
              <span>{formatCurrency(parseFloat(flightDetails.totalPrice.fees) / flightDetails.numberOfAdults, flightDetails.totalPrice.currency)}</span>
            </div>
          )}
          {flightDetails.totalPrice.pricePerAdult && (
            <div className={styles['price-item']}>
              <span>Price Per Traveler:</span>
              <span>{formatCurrency(flightDetails.totalPrice.pricePerAdult, flightDetails.totalPrice.currency)}</span>
            </div>
          )}
          <div className={`${styles['price-item']} ${styles['total-price']}`}>
            <span>Total Price ({flightDetails.numberOfAdults} Pax):</span>
            <span>{formatCurrency(flightDetails.totalPrice.total, flightDetails.totalPrice.currency)}</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DetailsPage;