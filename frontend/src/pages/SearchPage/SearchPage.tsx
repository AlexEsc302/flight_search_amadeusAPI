// frontend/src/pages/SearchPage/SearchPage.tsx
import React from 'react';
import SearchForm from './SearchForm';
import { useNavigate } from 'react-router-dom';
import { searchFlights } from '../../services/api';
import { FlightSearchParams, FlightSearchResult } from '../../types/flightTypes';
import styles from './SearchPage.module.css';


const SearchPage: React.FC = () => {
  const navigate = useNavigate();

  const handleSearch = async (params: FlightSearchParams) => {
    try {
      const results: FlightSearchResult[] = await searchFlights(params);
      navigate('/results', { state: { searchParams: params, results: results } });
    } catch (error) {
      console.error('Error while searching for flights:', error);
      alert('There was an error when searching for flights.'); 
    }
  };

  return (
    <div className={styles['search-page']}>

      <h1>Flight Booking</h1>
      <SearchForm onSearch={handleSearch} />
    </div>
  );
};

export default SearchPage;