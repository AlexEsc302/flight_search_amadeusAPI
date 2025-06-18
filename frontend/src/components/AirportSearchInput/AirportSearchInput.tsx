// frontend/src/components/AirportSearchInput/AirportSearchInput.tsx
import React, { useState, useEffect, useRef } from 'react';
import { searchAirports } from '../../services/api';
import { AirportSuggestion } from '../../types/airportTypes';
import styles from './AirportSearchInput.module.css'; 

interface AirportSearchInputProps {
  id: string;
  value: string;
  onSelect: (iataCode: string) => void;
  placeholder?: string;
  error?: string; 
}

const AirportSearchInput: React.FC<AirportSearchInputProps> = ({
  id,
  value,
  onSelect,
  placeholder = 'Example: LAX, Ciudad de México',
  error,
}) => {
  const [searchTerm, setSearchTerm] = useState<string>(value);
  const [suggestions, setSuggestions] = useState<AirportSuggestion[]>([]);
  const [showSuggestions, setShowSuggestions] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const inputRef = useRef<HTMLInputElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Effect to synchronize the internal value with the prop ‘value’.
  useEffect(() => {
    setSearchTerm(value);
  }, [value]);

  // Effect to search suggestions when changing the searchTerm
  useEffect(() => {
    if (searchTerm.length < 2) {
      setSuggestions([]);
      setIsLoading(false);
      setShowSuggestions(false); 
      return;
    }

    const delayDebounceFn = setTimeout(async () => {
      setIsLoading(true);
      try {
        const results = await searchAirports(searchTerm);
        setSuggestions(results);
        setShowSuggestions(results.length > 0);
      } catch (err) {
        console.error('Error fetching airport suggestions:', err);
        setSuggestions([]);
        setShowSuggestions(false); 
      } finally {
        setIsLoading(false);
      }
    }, 300); // Debounce of 300ms 

    return () => clearTimeout(delayDebounceFn); 
  }, [searchTerm]);

  // Handle clicks outside the component to hide suggestions
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      // If the click was not inside the input or the dropdown, hide the hints
      if (
        inputRef.current &&
        !inputRef.current.contains(event.target as Node) &&
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setShowSuggestions(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    setSearchTerm(newValue);
    // If input is empty, nothing has been selected yet, so onSelect(‘’)
    if (newValue === '') {
        onSelect(''); 
        setSuggestions([]); 
        setShowSuggestions(false); 
    } else {
        // If there is text, activate the visibility of the dropdown
        // The searchTerm's useEffect will already decide if there are enough characters to search for
        setShowSuggestions(true);
    }
  };

  const handleSuggestionClick = (suggestion: AirportSuggestion) => {
    // When selected, updates the input with the name (code)
    setSearchTerm(`(${suggestion.code}) ${suggestion.name} `);
    // Notifies the selected IATA code to the parent component
    onSelect(suggestion.code);
    // Hide suggestions after selection
    setSuggestions([]); 
    setShowSuggestions(false);
  };

  const handleFocus = () => {
    // When focusing, if there is already a search term (and it is long enough),
    // displays the previously loaded suggestions or triggers a new search.
    if (searchTerm.length >= 2 && suggestions.length > 0) {
        setShowSuggestions(true);
    } else if (searchTerm.length >= 2) {
        setShowSuggestions(true);
    }
  };

  return (
    <div className={styles['airport-search-input-container']}>
      <input
        type="text"
        id={id}
        value={searchTerm}
        onChange={handleInputChange}
        onFocus={handleFocus} 
        ref={inputRef}
        autoComplete="off" 
        className={error ? styles['input-error'] : ''}
      />
      {isLoading && <div className={styles['loading-spinner']}></div>}
      {error && <span className={styles['error-message']}>{error}</span>}

      {showSuggestions && (
        <div className={styles['suggestions-dropdown']} ref={dropdownRef}>
          {isLoading ? (
            <div className={styles['no-suggestions']}>Cargando...</div> 
          ) : (
            suggestions.length > 0 ? (
              suggestions.map((airport) => (
                <div
                  key={airport.code}
                  className={styles['suggestion-item']}
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => handleSuggestionClick(airport)}
                >
                  {airport.name} ({airport.code})
                </div>
              ))
            ) : (
              searchTerm.length >= 2 && <div className={styles['no-suggestions']}>No results were found.</div>
            )
          )}
        </div>
      )}
    </div>
  );
};

export default AirportSearchInput;