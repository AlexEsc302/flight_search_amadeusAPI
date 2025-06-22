// frontend/src/components/AirportSearchInput/__tests__/AirportSearchInput.test.tsx

import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import AirportSearchInput from '../AirportSearchInput';
import * as api from '../../../services/api'; // Import the api module to mock it

// Mock the API service to control search results
jest.mock('../../../services/api', () => ({
  searchAirports: jest.fn(),
}));

// Mock the CSS module to prevent issues with class names
jest.mock('../AirportSearchInput.module.css', () => ({
  'airport-search-input-container': 'airport-search-input-container',
  'input-error': 'input-error',
  'loading-spinner': 'loading-spinner',
  'error-message': 'error-message',
  'suggestions-dropdown': 'suggestions-dropdown',
  'no-suggestions': 'no-suggestions',
  'suggestion-item': 'suggestion-item',
}));

// Use Jest's fake timers to control setTimeout (for debouncing)
jest.useFakeTimers();

describe('AirportSearchInput', () => {
  const mockOnSelect = jest.fn();
  const mockSearchAirports = api.searchAirports as jest.MockedFunction<typeof api.searchAirports>;

  beforeEach(() => {
    // Clear all mocks before each test to ensure isolation
    mockOnSelect.mockClear();
    mockSearchAirports.mockClear();
    jest.clearAllTimers(); // Clear any pending fake timers
    // Ensure fake timers are used for *every* test in this describe block
    jest.useFakeTimers(); 
  });

  afterEach(() => {
    // Run all pending timers to ensure all microtasks are flushed
    // This is important to avoid act warnings from unhandled promises/timers
    act(() => {
      jest.runOnlyPendingTimers();
    });
    jest.useRealTimers(); // Restore real timers after each test
  });

  test('renders with initial value and placeholder', () => {
    render(
      <AirportSearchInput
        id="origin-input"
        value="MEX"
        onSelect={mockOnSelect}
        placeholder="Enter origin airport"
      />
    );
    const input = screen.getByDisplayValue('MEX'); 
    expect(input).toHaveValue('MEX');
    expect(input).toHaveAttribute('id', 'origin-input'); // Verify the ID is correctly applied
  });

  test('updates value when prop changes', () => {
    const { rerender } = render(
      <AirportSearchInput id="origin-input" value="LAX" onSelect={mockOnSelect} />
    );

    const input = screen.getByDisplayValue('LAX');
    expect(input).toHaveValue('LAX');

    rerender(<AirportSearchInput id="origin-input" value="JFK" onSelect={mockOnSelect} />);
    expect(screen.getByDisplayValue('JFK')).toBeInTheDocument(); // Input value should change
  });

  test('does not show suggestions initially or for short search terms', async () => {
    render(<AirportSearchInput id="origin-input" value="" onSelect={mockOnSelect} />);

    const input = screen.getByDisplayValue(''); // Input is initially empty
    fireEvent.change(input, { target: { value: 'M' } }); // Single character

    act(() => {
      jest.advanceTimersByTime(100);
    });

    expect(screen.queryByRole('listbox')).not.toBeInTheDocument(); // Suggestions dropdown (if it were listbox role)
    expect(screen.queryByText(/Cargando/i)).not.toBeInTheDocument();
    expect(mockSearchAirports).not.toHaveBeenCalled();

    // Type two characters, but don't advance time enough
    fireEvent.change(input, { target: { value: 'ME' } });
    act(() => {
      jest.advanceTimersByTime(200); // 200ms total, still less than 300ms debounce
    });
    expect(mockSearchAirports).not.toHaveBeenCalled();
  });

  test('calls onSelect and hides suggestions when a suggestion is clicked', async () => {
    mockSearchAirports.mockResolvedValueOnce([
      { code: 'MEX', name: 'Mexico City International Airport' },
    ]);

    render(<AirportSearchInput id="origin-input" value="" onSelect={mockOnSelect} />);
    const input = screen.getByDisplayValue('');

    fireEvent.change(input, { target: { value: 'mex' } });

    // Advance timers for debounce and API call, awaiting the promise resolution
    await act(async () => {
      jest.advanceTimersByTime(300);
      await mockSearchAirports.mock.results[0].value;
    });

    // Ensure suggestions are visible
    expect(screen.getByText('Mexico City International Airport (MEX)')).toBeInTheDocument();

    const suggestionItem = screen.getByText('Mexico City International Airport (MEX)');
    
    // Clicking a suggestion triggers state updates (setSearchTerm, onSelect, setSuggestions, setShowSuggestions)
    // These must be wrapped in act(). `fireEvent.click` is usually wrapped in act, but if it triggers
    // subsequent async effects (like a change that triggers another debounce), you might need explicit act.
    // In this case, `setSearchTerm` and `onSelect` are synchronous.
    // The `waitFor` should cover subsequent re-renders after the click.
    await act(async () => { // Wrapping fireEvent.click in act is good practice for components with complex state changes
      fireEvent.click(suggestionItem);
    });

    expect(input).toHaveValue('(MEX) Mexico City International Airport '); // Input value updated
    expect(mockOnSelect).toHaveBeenCalledTimes(1);
    expect(mockOnSelect).toHaveBeenCalledWith('MEX');
    expect(screen.queryByText('Mexico City International Airport (MEX)')).not.toBeInTheDocument(); // Suggestions should be hidden
    expect(screen.queryByText('No results were found.')).not.toBeInTheDocument();
  });

  test('hides suggestions when clicking outside the component', async () => {
    mockSearchAirports.mockResolvedValueOnce([
      { code: 'MEX', name: 'Mexico City International Airport' },
    ]);

    render(<AirportSearchInput id="origin-input" value="" onSelect={mockOnSelect} />);
    const input = screen.getByDisplayValue('');

    fireEvent.change(input, { target: { value: 'mex' } });
    await act(async () => {
      jest.advanceTimersByTime(300); // Advance timers for debounce and API call
      await mockSearchAirports.mock.results[0].value;
    });

    // Ensure suggestions are visible
    expect(screen.getByText('Mexico City International Airport (MEX)')).toBeInTheDocument();

    // Simulate a click outside the component (e.g., on the body)
    // `fireEvent.mouseDown` on `document.body` should trigger the `handleClickOutside` useEffect.
    // This state update (setShowSuggestions(false)) must be wrapped in act().
    await act(async () => {
      fireEvent.mouseDown(document.body);
    });
    
    expect(screen.queryByText('Mexico City International Airport (MEX)')).not.toBeInTheDocument();
    expect(screen.queryByText('No results were found.')).not.toBeInTheDocument();
  });

  test('displays error message when `error` prop is provided', () => {
    render(
      <AirportSearchInput id="origin-input" value="" onSelect={mockOnSelect} error="Airport code is invalid" />
    );

    expect(screen.getByText('Airport code is invalid')).toBeInTheDocument();
    const input = screen.getByDisplayValue('');
    expect(input).toHaveClass('input-error'); // Assumes styles are correctly mocked or applied
  });

  test('clears suggestions and calls onSelect("") when input is cleared', async () => {
    mockSearchAirports.mockResolvedValueOnce([
      { code: 'MEX', name: 'Mexico City International Airport' },
    ]);

    render(<AirportSearchInput id="origin-input" value="MEX" onSelect={mockOnSelect} />);
    const input = screen.getByDisplayValue('MEX'); // Initially has value 'MEX'

    // Type something to trigger suggestions
    fireEvent.change(input, { target: { value: 'me' } });
    await act(async () => {
      jest.advanceTimersByTime(300);
      await mockSearchAirports.mock.results[0].value;
    });
    
    expect(screen.getByText('Mexico City International Airport (MEX)')).toBeInTheDocument();

    // Clear the input
    // This change triggers onSelect('') and also setSuggestions([]) and setShowSuggestions(false)
    await act(async () => { // Wrap the event that causes state updates
      fireEvent.change(input, { target: { value: '' } });
    });

    expect(input).toHaveValue('');
    expect(mockOnSelect).toHaveBeenCalledWith(''); // Should call onSelect with empty string
    expect(screen.queryByText('Mexico City International Airport (MEX)')).not.toBeInTheDocument(); // Suggestions should be hidden
    expect(screen.queryByText('No results were found.')).not.toBeInTheDocument();
  });

  test('re-shows suggestions on focus if search term is long enough and suggestions were previously loaded', async () => {
    // 1. Initial render and search to populate suggestions
    mockSearchAirports.mockResolvedValueOnce([
      { code: 'MEX', name: 'Mexico City International Airport' },
    ]);

    render(<AirportSearchInput id="origin-input" value="" onSelect={mockOnSelect} />);
    const input = screen.getByDisplayValue('');

    fireEvent.change(input, { target: { value: 'mex' } });
    await act(async () => {
      jest.advanceTimersByTime(300);
      await mockSearchAirports.mock.results[0].value;
    });

    expect(screen.getByText('Mexico City International Airport (MEX)')).toBeInTheDocument();

    // 2. Simulate clicking outside to hide suggestions
    await act(async () => {
      fireEvent.mouseDown(document.body);
    });
    expect(screen.queryByText('Mexico City International Airport (MEX)')).not.toBeInTheDocument();

    // 3. Focus the input again
    // `fireEvent.focus` also needs to be in act because `handleFocus` calls `setShowSuggestions`.
    await act(async () => {
      fireEvent.focus(input);
    });

    // Wait for suggestions to reappear (they should instantly, as they are already in state)
    // No need for waitFor if the change is synchronous within the act.
    expect(screen.getByText('Mexico City International Airport (MEX)')).toBeInTheDocument();
  });

  
});