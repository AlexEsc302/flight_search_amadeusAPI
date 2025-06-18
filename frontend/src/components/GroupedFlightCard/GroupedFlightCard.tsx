// frontend/src/components/GroupedFlightCard/GroupedFlightCard.tsx
import React from 'react';
import { GroupedFlightOffer, FlightSearchResult } from '../../types/flightTypes';
import { format, parseISO } from 'date-fns';
import { es } from 'date-fns/locale';
import styles from './GroupedFlightCard.module.css';

interface GroupedFlightCardProps {
  groupedFlight: GroupedFlightOffer;
  onClick?: (flightId: string) => void;
}

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

const FlightSegmentDisplay: React.FC<{ flight: FlightSearchResult }> = ({ flight }) => {
  const departureTime = format(parseISO(flight.departureDateTime), 'HH:mm');
  const arrivalTime = format(parseISO(flight.arrivalDateTime), 'HH:mm');
  const departureDate = format(parseISO(flight.departureDateTime), 'EEE d MMM', { locale: es });
  const arrivalDate = format(parseISO(flight.arrivalDateTime), 'EEE d MMM', { locale: es });

  const numStops = flight.stops.length;
  const isNonStop = numStops === 0;

  return (
    <div className={styles['flight-segment-display']}>
      <div className={styles['flight-times-route']}>
        <div className={styles['time-info']}>
          <span className={styles['time']}>{departureTime}</span>
          <span className={styles['airport-name-code']}>{flight.departureAirport.name} ({flight.departureAirport.code})</span>
        </div>
        <div className={styles['arrow-duration']}>
          <span className={styles['arrow']}>&rarr;</span>
          <span className={styles['duration']}>{formatDuration(flight.duration)}</span>
        </div>
        <div className={styles['time-info']}>
          <span className={styles['time']}>{arrivalTime}</span>
          <span className={styles['airport-name-code']}>{flight.arrivalAirport.name} ({flight.arrivalAirport.code})</span>
        </div>
      </div>
      <div className={styles['flight-date']}>
        {departureDate} {departureDate !== arrivalDate && `(llega ${arrivalDate})`}
      </div>
      <div className={styles['flight-airline-stops']}>
        <span className={styles['airline-name']}>{flight.airline.name} ({flight.airline.code})</span>
        {!isNonStop && (
          <span className={styles['stops-info']}>
            &bull; {numStops} Stop{numStops > 1 ? 's' : ''}
          </span>
        )}
      </div>
      {flight.operatingAirline && flight.operatingAirline.code !== flight.airline.code && (
        <div className={styles['operating-airline']}>
          Operated by: {flight.operatingAirline.name} ({flight.operatingAirline.code})
        </div>
      )}
      {!isNonStop && (
        <div className={styles['layover-details']}>
          Layovers:
          {flight.stops.map((stop, index) => (
            <span key={index} className={styles['layover-item']}>
              {stop.airportName} ({stop.airportCode}) {formatDuration(stop.layoverDuration)}
              {index < numStops - 1 && ', '}
            </span>
          ))}
        </div>
      )}
    </div>
  );
};


const GroupedFlightCard: React.FC<GroupedFlightCardProps> = ({ groupedFlight, onClick }) => {
  const { outboundFlight, inboundFlight, totalPrice, isRoundTrip, offerId } = groupedFlight;

  const handleCardClick = () => {
    if (onClick) {
      onClick(offerId);
    }
  };

  return (
    <div className={styles['grouped-flight-card']} onClick={handleCardClick}>
      <div className={styles['flight-info-section']}>
        <h3 className={styles['flight-direction-title']}>Outbound</h3>
        <FlightSegmentDisplay flight={outboundFlight} />

        {isRoundTrip && inboundFlight && (
          <>
            <h3 className={styles['flight-direction-title']}>Return</h3>
            <FlightSegmentDisplay flight={inboundFlight} />
          </>
        )}
      </div>

      <div className={styles['flight-price-section']}>
        <div className={styles['total-price']}>
          <span className={styles['price-value']}>
            {parseFloat(totalPrice.total).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            {totalPrice.currency}
          </span>
          <span className={styles['price-label']}>Total</span>
        </div>
        <div className={styles['per-traveler-price']}>
          <span className={styles['price-value']}>
            {parseFloat(totalPrice.pricePerAdult || '0').toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            {totalPrice.currency}
          </span>
          <span className={styles['price-label']}>Per adult</span>
        </div>
      </div>
    </div>
  );
};

export default GroupedFlightCard;