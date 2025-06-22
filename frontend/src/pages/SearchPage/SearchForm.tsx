import React, { useState, useEffect } from 'react';
import AirportSearchInput from '../../components/AirportSearchInput/AirportSearchInput';
import { FlightSearchParams } from '../../types/flightTypes';
import { format } from 'date-fns';
import styles from './SearchForm.module.css';

interface SearchFormProps {
  onSearch: (params: FlightSearchParams) => void;
}

const SearchForm: React.FC<SearchFormProps> = ({ onSearch }) => {
  const [originLocationCode, setOriginLocationCode] = useState<string>('');
  const [destinationLocationCode, setDestinationLocationCode] = useState<string>('');
  const [departureDate, setDepartureDate] = useState<string>(format(new Date(), 'yyyy-MM-dd'));
  const [returnDate, setReturnDate] = useState<string>('');
  const [adults, setAdults] = useState<number>(1);
  const [currency, setCurrencyCode] = useState<string>('USD');
  const [nonStop, setNonStop] = useState<boolean>(false);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState<boolean>(false); 

  const today = format(new Date(), 'yyyy-MM-dd');

  useEffect(() => {
    const errors: Record<string, string> = {};
    if (!originLocationCode) errors.originLocationCode = 'Required';
    if (!destinationLocationCode) errors.destinationLocationCode = 'Required';
    if (!departureDate) errors.departureDate = 'Required';
    if (adults < 1) errors.adults = 'Min. 1 adult';
    if (new Date(departureDate) < new Date(today)) errors.departureDate = 'Cannot be in the past';
    if (returnDate && new Date(returnDate) < new Date(departureDate)) errors.returnDate = 'Return after departure';

    setFormErrors(errors);
  }, [originLocationCode, destinationLocationCode, departureDate, returnDate, adults]);

  const isValid = Object.keys(formErrors).length === 0;

    const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isValid) {
      const params: FlightSearchParams = {
        originLocationCode,
        destinationLocationCode,
        departureDate,
        adults,
        currency,
        nonStop,
        ...(returnDate && { returnDate }),
      };

      setIsLoading(true); // Mostrar loading
      try {
        await onSearch(params); // Ejecutar búsqueda
      } finally {
        setIsLoading(false); // Ocultar loading después
      }
    } else {
      alert('Please correct the errors in the form.');
    }
  };

  return (
    <form onSubmit={handleSubmit} className={styles['search-form']}>
      <div className={styles['input-row']}>
        <label htmlFor="origin">Origin:</label>
        <AirportSearchInput
          id="origin"
          value={originLocationCode}
          onSelect={setOriginLocationCode}
          error={formErrors.originLocationCode}
        />
      </div>

      <div className={styles['input-row']}>
        <label htmlFor="destination">Destination:</label>
        <AirportSearchInput
          id="destination"
          value={destinationLocationCode}
          onSelect={setDestinationLocationCode}
          error={formErrors.destinationLocationCode}
        />
      </div>

      <div className={styles['input-row']}>
        <label htmlFor="departureDate">Departure:</label>
        <input
          type="date"
          id="departureDate"
          value={departureDate}
          onChange={(e) => setDepartureDate(e.target.value)}
          min={today}
          className={formErrors.departureDate ? styles['input-error'] : ''}
        />
      </div>

      <div className={styles['input-row']}>
        <label htmlFor="returnDate">Return (opt.):</label>
        <input
          type="date"
          id="returnDate"
          value={returnDate}
          onChange={(e) => setReturnDate(e.target.value)}
          min={departureDate}
          className={formErrors.returnDate ? styles['input-error'] : ''}
        />
      </div>

      <div className={styles['input-row']}>
        <label htmlFor="adults">Adults:</label>
        <input
          type="number"
          id="adults"
          value={adults}
          onChange={(e) => setAdults(parseInt(e.target.value))}
          min="1"
          className={formErrors.adults ? styles['input-error'] : ''}
        />
      </div>

      <div className={styles['input-row']}>
        <label htmlFor="currency">Currency:</label>
        <select
          id="currency"
          value={currency}
          onChange={(e) => setCurrencyCode(e.target.value)}
        >
          <option value="USD">USD</option>
          <option value="MXN">MXN</option>
          <option value="EUR">EUR</option>
        </select>
      </div>

      <div className={styles['checkbox-row']}>
        <input
          type="checkbox"
          id="nonStop"
          checked={nonStop}
          onChange={(e) => setNonStop(e.target.checked)}
        />
        <label htmlFor="nonStop">Only direct flights</label>
      </div>

      <button type="submit" disabled={!isValid || isLoading}>
        {isLoading ? 'Searching...' : 'Search Flights'}
      </button>
    </form>
  );
};

export default SearchForm;
