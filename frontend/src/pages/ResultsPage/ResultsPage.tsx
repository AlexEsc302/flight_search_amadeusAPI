// frontend/src/pages/ResultsPage/ResultsPage.tsx
import React, { useEffect, useState, useMemo, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { FlightSearchResult, FlightSearchParams, GroupedFlightOffer, PriceDTO } from '../../types/flightTypes';
import GroupedFlightCard from '../../components/GroupedFlightCard/GroupedFlightCard'; 
import styles from './ResultsPage.module.css';
import { searchFlights } from '../../services/api';
import { format, parseISO } from 'date-fns';
import { es } from 'date-fns/locale'; 

const ResultsPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [groupedFlights, setGroupedFlights] = useState<GroupedFlightOffer[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<'price' | 'duration'>('price'); 
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc'); 

  const searchParams: FlightSearchParams | undefined = location.state?.searchParams;
  const initialResults: FlightSearchResult[] | undefined = location.state?.results;

  // Flight grouping function 
  const groupAndProcessFlights = useCallback((flights: FlightSearchResult[], isRoundTripSearch: boolean): GroupedFlightOffer[] => {
    const grouped: { [key: string]: { outbound?: FlightSearchResult; inbound?: FlightSearchResult } } = {};
    const processedOffers: GroupedFlightOffer[] = [];

    flights.forEach(flight => {
        const parentId = flight.parentOfferId || flight.id; 

        if (!grouped[parentId]) {
            grouped[parentId] = {};
        }

        const flightDepartureDate = format(parseISO(flight.departureDateTime), 'yyyy-MM-dd');

        if (flightDepartureDate === searchParams?.departureDate) {
            grouped[parentId].outbound = flight;
        } else if (isRoundTripSearch && searchParams?.returnDate && flightDepartureDate === searchParams.returnDate) {
            grouped[parentId].inbound = flight;
        } else if (!isRoundTripSearch) {
             grouped[parentId].outbound = flight; 
        }
    });

    for (const parentId in grouped) {
        const group = grouped[parentId];
        if (group.outbound) {
            const totalPrice: PriceDTO = group.outbound.price; 

            processedOffers.push({
                offerId: parentId,
                outboundFlight: group.outbound,
                inboundFlight: group.inbound,
                totalPrice: totalPrice,
                numberOfAdults: group.outbound.numberOfAdults, 
                isRoundTrip: !!group.inbound, 
            });
        }
    }
    return processedOffers;
  }, [searchParams]); 


  useEffect(() => {
    if (initialResults && initialResults.length > 0) {
      const isRoundTripSearch = !!searchParams?.returnDate;
      const processed = groupAndProcessFlights(initialResults, isRoundTripSearch);
      setGroupedFlights(processed);
      setIsLoading(false);
      return;
    }

    // If there are no initial results but there are search parameters,
    // we try to search again (for example, if the user reloads the page).
    if (searchParams) {
      const fetchFlights = async () => {
        try {
          setIsLoading(true);
          setError(null);
          const fetchedFlights = await searchFlights(searchParams);
          const isRoundTripSearch = !!searchParams.returnDate; 
          const processed = groupAndProcessFlights(fetchedFlights, isRoundTripSearch);
          setGroupedFlights(processed);
        } catch (err) {
          console.error('Error fetching flights on results page:', err);
          setError('The flights could not be loaded. Please try again.');
        } finally {
          setIsLoading(false);
        }
      };
      fetchFlights();
    } else {
      // If there are neither results nor parameters, redirect to the search page.
      navigate('/');
    }
  }, [searchParams, initialResults, navigate, groupAndProcessFlights]); 

  // Grouped flights ordering logic
  const sortedFlights = useMemo(() => {
    // Create a copy so as not to mutate the original status
    const sortableFlights = [...groupedFlights];

    if (sortBy === 'price') {
      sortableFlights.sort((a, b) => {
        const priceA = parseFloat(a.totalPrice.total);
        const priceB = parseFloat(b.totalPrice.total);
        return sortOrder === 'asc' ? priceA - priceB : priceB - priceA;
      });
    } else if (sortBy === 'duration') {
      const parseDurationToMinutes = (isoDuration: string): number => {
        const matches = isoDuration.match(/PT(?:(\d+)H)?(?:(\d+)M)?/);
        if (!matches) return 0;
        const hours = matches[1] ? parseInt(matches[1], 10) : 0;
        const minutes = matches[2] ? parseInt(matches[2], 10) : 0;
        return hours * 60 + minutes;
      };

      sortableFlights.sort((a, b) => {
        const durationA_outbound = parseDurationToMinutes(a.outboundFlight.duration);
        const durationB_outbound = parseDurationToMinutes(b.outboundFlight.duration);

        let totalDurationA = durationA_outbound;
        if (a.inboundFlight) {
            totalDurationA += parseDurationToMinutes(a.inboundFlight.duration);
            a.outboundFlight.stops.forEach(stop => totalDurationA += parseDurationToMinutes(stop.layoverDuration));
            a.inboundFlight.stops.forEach(stop => totalDurationA += parseDurationToMinutes(stop.layoverDuration));
        } else {
            a.outboundFlight.stops.forEach(stop => totalDurationA += parseDurationToMinutes(stop.layoverDuration));
        }

        let totalDurationB = durationB_outbound;
        if (b.inboundFlight) {
            totalDurationB += parseDurationToMinutes(b.inboundFlight.duration);
            b.outboundFlight.stops.forEach(stop => totalDurationB += parseDurationToMinutes(stop.layoverDuration));
            b.inboundFlight.stops.forEach(stop => totalDurationB += parseDurationToMinutes(stop.layoverDuration));
        } else {
            b.outboundFlight.stops.forEach(stop => totalDurationB += parseDurationToMinutes(stop.layoverDuration));
        }

        return sortOrder === 'asc' ? totalDurationA - totalDurationB : totalDurationB - totalDurationA;
      });
    }

    return sortableFlights;
  }, [groupedFlights, sortBy, sortOrder]);


  const handleSortChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
    setSortBy(event.target.value as 'price' | 'duration');
  };

  const handleSortOrderChange = () => {
    setSortOrder(prevOrder => (prevOrder === 'asc' ? 'desc' : 'asc'));
  };

  const handleReturnToSearch = () => {
    navigate('/');
  };

  const handleFlightCardClick = (offerId: string) => {
    console.log("Clicked on flight offer ID:", offerId);
    navigate(`/details/${offerId}`); 
  };

  return (
    <div className={styles['results-page']}>
      <div className={styles['header-section']}>
        <button onClick={handleReturnToSearch} className={styles['return-button']}>
          &lt; Return to Search Page
        </button>
        <h1>Flight Results</h1>
        {searchParams && (
            <p className={styles['search-summary']}>
                {searchParams.originLocationCode} &rarr; {searchParams.destinationLocationCode} | {format(parseISO(searchParams.departureDate), 'd MMM yyyy', { locale: es })}
                {searchParams.returnDate && ` - ${format(parseISO(searchParams.returnDate), 'd MMM yyyy', { locale: es })}`} | {searchParams.adults} adult(s) | {searchParams.currency}
            </p>
        )}
      </div>

      <div className={styles['sort-options']}>
        <label htmlFor="sort-by">Order by:</label>
        <select id="sort-by" value={sortBy} onChange={handleSortChange}>
          <option value="price">Price</option>
          <option value="duration">Duration</option>
        </select>
        <button onClick={handleSortOrderChange} className={styles['sort-order-button']}>
          {sortOrder === 'asc' ? 'ASC' : 'DESC'} {sortOrder === 'asc' ? '↑' : '↓'}
        </button>
      </div>

      {isLoading && <p className={styles['loading-message']}>Loading flights...</p>}
      {error && <p className={styles['error-message']}>{error}</p>}
      {!isLoading && !error && groupedFlights.length === 0 && (
        <p className={styles['no-results-message']}>No flights found for your search.</p>
      )}

      <div className={styles['flight-list']}>
        {!isLoading && !error && sortedFlights.map((groupedFlight) => (
          <GroupedFlightCard
            key={groupedFlight.offerId}
            groupedFlight={groupedFlight}
            onClick={handleFlightCardClick}
          />
        ))}
      </div>
    </div>
  );
};

export default ResultsPage;