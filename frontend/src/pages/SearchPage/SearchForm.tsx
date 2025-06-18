// frontend/src/pages/SearchPage/SearchForm.tsx
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

  const today = format(new Date(), 'yyyy-MM-dd');

  useEffect(() => {
    const errors: Record<string, string> = {};
    if (!originLocationCode) errors.originLocationCode = 'Field required';
    if (!destinationLocationCode) errors.destinationLocationCode = 'Field required';
    if (!departureDate) errors.departureDate = 'Field required';
    if (adults < 1) errors.adults = 'There must be at least one adult';

    if (departureDate && new Date(departureDate) < new Date(today)) {
      errors.departureDate = 'Exit date cannot be in the past.';
    }

    if (returnDate && returnDate !== '' && new Date(returnDate) < new Date(departureDate || today)) {
        errors.returnDate = 'The date of return cannot be earlier than the date of departure.';
    }


    setFormErrors(errors);
  }, [originLocationCode, destinationLocationCode, departureDate, returnDate, adults, today]);

  const isValid = Object.keys(formErrors).length === 0 && originLocationCode !== '' && destinationLocationCode !== '';

  const handleSubmit = (e: React.FormEvent) => {
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
      onSearch(params);
    } else {
      alert('Please correct the errors in the form.');
    }
  };

  return (
    <form onSubmit={handleSubmit} className="search-form">
      <div className={`${styles['form-group']} ${styles['inline-fields']}`}>
        <label htmlFor="origin">Origin:</label>
        <AirportSearchInput
          id="origin"
          value={originLocationCode}
          onSelect={(code) => setOriginLocationCode(code)}
          placeholder="IATA Code or Airport Name"
          error={formErrors.originLocationCode}
        />
      </div>

      <div className={`${styles['form-group']} ${styles['inline-fields']}`}>
        <label htmlFor="destination">Destination:</label>
        <AirportSearchInput
          id="destination"
          value={destinationLocationCode}
          onSelect={(code) => setDestinationLocationCode(code)}
          placeholder="IATA Code or Airport Name"
          error={formErrors.destinationLocationCode}
        />
      </div>

      <div className="form-group">
        <label htmlFor="departureDate">Departure date:</label>
        <input
          type="date"
          id="departureDate"
          value={departureDate}
          onChange={(e) => setDepartureDate(e.target.value)}
          min={today}
          className={formErrors.departureDate ? 'input-error' : ''}
        />
        {formErrors.departureDate && <span className="error-message">{formErrors.departureDate}</span>}
      </div>

      <div className="form-group">
        <label htmlFor="returnDate">Return date (optional):</label>
        <input
          type="date"
          id="returnDate"
          value={returnDate}
          onChange={(e) => setReturnDate(e.target.value)}
          min={departureDate || today}
          className={formErrors.returnDate ? 'input-error' : ''}
        />
        {formErrors.returnDate && <span className="error-message">{formErrors.returnDate}</span>}
      </div>

      <div className="form-group">
        <label htmlFor="adults">Adults:</label>
        <input
          type="number"
          id="adults"
          value={adults}
          onChange={(e) => setAdults(parseInt(e.target.value))}
          min="1"
          className={formErrors.adults ? 'input-error' : ''}
        />
        {formErrors.adults && <span className="error-message">{formErrors.adults}</span>}
      </div>

      <div className="form-group">
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

      <div className="form-group checkbox-group">
        <input
          type="checkbox"
          id="nonStop"
          checked={nonStop}
          onChange={(e) => setNonStop(e.target.checked)}
        />
        <label htmlFor="nonStop">Direct flights only</label>
      </div>

      <button type="submit" disabled={!isValid}>Search Flights</button>
    </form>
  );
};

export default SearchForm;