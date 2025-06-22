import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'; // Import 'act'
import '@testing-library/jest-dom';
import SearchForm from '../SearchPage/SearchForm'; // Ensure the path is correct
import { format } from 'date-fns';

// Mock the AirportSearchInput component
// This is crucial to isolate the SearchForm test from AirportSearchInput's internal logic.
jest.mock('../../components/AirportSearchInput/AirportSearchInput', () => {
    // eslint-disable-next-line react/display-name
    return ({ id, value, onSelect, error }: any) => (
        <div>
        <input
            id={id}
            data-testid={`${id}-airport-input`} // Add data-testid for easy selection
            value={value}
            onChange={(e) => onSelect(e.target.value)} // Simulate onSelect being called with input value
        />
        {/* It's CRUCIAL that your real AirportSearchInput component renders a SPAN with the error if passed. */}
        {/* The mock here simulates it with a data-testid so the test can find it. */}
        {error && <span data-testid={`${id}-error`}>{error}</span>}
        </div>
    );
    });

    // Mock the date-fns module to control the current date in tests
    jest.useFakeTimers();

    describe('SearchForm', () => {
    const mockOnSearch = jest.fn();
    const today = format(new Date(), 'yyyy-MM-dd'); // Current date at the time of running tests

    let alertMock: jest.SpyInstance; // Declare alertMock here so it's available in beforeEach/afterEach

    beforeEach(() => {
        mockOnSearch.mockClear();
        // Set a consistent "today" date for date-related tests
        jest.setSystemTime(new Date('2025-06-22T10:00:00Z')); // Use Z for UTC to avoid timezone issues
        // Mock window.alert to prevent it from appearing in the test console
        alertMock = jest.spyOn(window, 'alert').mockImplementation(() => {});
    });

    afterEach(() => { // Use afterEach to clean up mocks after each test
        alertMock.mockRestore(); // Restore the original alert function
    });

    afterAll(() => {
        jest.useRealTimers(); // Restore real timers after all tests are done
    });

    test('renders all form fields correctly', () => {
        render(<SearchForm onSearch={mockOnSearch} />);

        // Verify all form inputs and elements are present
        expect(screen.getByLabelText(/Origin:/i)).toBeInTheDocument();
        expect(screen.getByTestId('origin-airport-input')).toBeInTheDocument();
        expect(screen.getByLabelText(/Destination:/i)).toBeInTheDocument();
        expect(screen.getByTestId('destination-airport-input')).toBeInTheDocument();
        expect(screen.getByLabelText(/Departure:/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Return \(opt\.\):/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Adults:/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Currency:/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/Only direct flights/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeInTheDocument();

        // Verify the initial value of the departure date (should be today)
        expect(screen.getByLabelText(/Departure:/i)).toHaveValue(today);
    });

    test('displays validation errors for required fields on initial render (if empty strings are passed)', async () => {
        render(<SearchForm onSearch={mockOnSearch} />);

        // Wait for the useEffect to run and update the form errors state.
        // Initially, only origin and destination are empty strings.
        await waitFor(() => {
        expect(screen.getByTestId('origin-error')).toHaveTextContent('Required');
        expect(screen.getByTestId('destination-error')).toHaveTextContent('Required');
        });

        // Verify the submit button is disabled due to validation errors
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeDisabled();
    });

    test('removes validation errors when required fields are filled', async () => {
        render(<SearchForm onSearch={mockOnSearch} />);

        // Wait for initial errors to appear (due to empty origin/destination)
        await waitFor(() => {
        expect(screen.getByTestId('origin-error')).toBeInTheDocument();
        });

        // Fill in the required fields
        fireEvent.change(screen.getByTestId('origin-airport-input'), { target: { value: 'MEX' } });
        fireEvent.change(screen.getByTestId('destination-airport-input'), { target: { value: 'CUN' } });

        // Wait for the errors to disappear (useEffect reacts to state changes)
        await waitFor(() => {
        expect(screen.queryByTestId('origin-error')).not.toBeInTheDocument();
        expect(screen.queryByTestId('destination-error')).not.toBeInTheDocument();
        });

        // The button should now be enabled as other initial fields (adults=1, departureDate=today) are valid.
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeEnabled();
    });

    test('validates departure date cannot be in the past', async () => {
        render(<SearchForm onSearch={mockOnSearch} />);

        // Fill in required fields to isolate the date validation error
        fireEvent.change(screen.getByTestId('origin-airport-input'), { target: { value: 'MEX' } });
        fireEvent.change(screen.getByTestId('destination-airport-input'), { target: { value: 'CUN' } });

        // Set a past date for departure
        fireEvent.change(screen.getByLabelText(/Departure:/i), { target: { value: '2025-06-21' } }); // Today is 2025-06-22

        // Since your SearchForm.tsx does not render a <span> with the error message for departureDate,
        // we cannot use `screen.getByText(/Cannot be in the past/i)`.
        // Instead, we verify that the submit button is disabled as a result of the validation.
        await waitFor(() => {
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeDisabled();
        });

        // Optional: If CSS modules styles have an impact, you could check for classes:
        // const departureInput = screen.getByLabelText(/Departure:/i);
        // expect(departureInput).toHaveClass('input-error'); // This assumes 'input-error' is applied
    });

    test('validates return date cannot be before departure date', async () => {
        render(<SearchForm onSearch={mockOnSearch} />);

        // Fill in required fields to isolate the date validation error
        fireEvent.change(screen.getByTestId('origin-airport-input'), { target: { value: 'MEX' } });
        fireEvent.change(screen.getByTestId('destination-airport-input'), { target: { value: 'CUN' } });

        // Set a valid future departure date
        fireEvent.change(screen.getByLabelText(/Departure:/i), { target: { value: '2025-07-10' } });
        // Set a return date earlier than the departure date
        fireEvent.change(screen.getByLabelText(/Return \(opt\.\):/i), { target: { value: '2025-07-09' } });

        // Similar to the previous case, if there's no error <span>, we verify the button's state.
        await waitFor(() => {
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeDisabled();
        });

        // Optional: check CSS class
        // const returnInput = screen.getByLabelText(/Return \(opt\.\):/i);
        // expect(returnInput).toHaveClass('input-error');
    });

    test('allows users to select currency and non-stop option', () => {
        render(<SearchForm onSearch={mockOnSearch} />);

        // Select MXN currency
        fireEvent.change(screen.getByLabelText(/Currency:/i), { target: { value: 'MXN' } });
        expect(screen.getByLabelText(/Currency:/i)).toHaveValue('MXN');

        // Check "Only direct flights" checkbox
        fireEvent.click(screen.getByLabelText(/Only direct flights/i));
        expect(screen.getByLabelText(/Only direct flights/i)).toBeChecked();
    });

    test('calls onSearch with correct parameters and handles loading state on valid submit', async () => {
        render(<SearchForm onSearch={mockOnSearch} />);

        // Fill all fields to make the form valid
        fireEvent.change(screen.getByTestId('origin-airport-input'), { target: { value: 'MEX' } });
        fireEvent.change(screen.getByTestId('destination-airport-input'), { target: { value: 'CUN' } });
        fireEvent.change(screen.getByLabelText(/Departure:/i), { target: { value: '2025-07-01' } });
        fireEvent.change(screen.getByLabelText(/Return \(opt\.\):/i), { target: { value: '2025-07-10' } });
        fireEvent.change(screen.getByLabelText(/Adults:/i), { target: { value: '2' } });
        fireEvent.change(screen.getByLabelText(/Currency:/i), { target: { value: 'EUR' } });
        fireEvent.click(screen.getByLabelText(/Only direct flights/i));

        // Ensure the form is valid and button is enabled before submitting
        await waitFor(() => {
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeEnabled();
        });

        // Mock onSearch to return a resolved promise, simulating a successful async call.
        // Using mockImplementation gives us access to the promise via mock.results.
        mockOnSearch.mockImplementation(() => Promise.resolve());

        // *** CRUCIAL CHANGE: Use act() to wrap the submit action and await its effects ***
        // This ensures that all state updates triggered by the submission (isLoading: true,
        // onSearch completion, isLoading: false in finally block) are processed within an act context.
        await act(async () => {
        fireEvent.click(screen.getByRole('button', { name: /Search Flights/i }));
        // Wait for the promise returned by mockOnSearch to resolve.
        // This is vital for the `finally` block with `setIsLoading(false)` to run within this act.
        await mockOnSearch.mock.results[0].value;
        });

        // After the `act` block, all state updates should be flushed.
        // Verify that mockOnSearch was called with the correct parameters.
        expect(mockOnSearch).toHaveBeenCalledTimes(1);
        expect(mockOnSearch).toHaveBeenCalledWith({
        originLocationCode: 'MEX',
        destinationLocationCode: 'CUN',
        departureDate: '2025-07-01',
        adults: 2,
        currency: 'EUR',
        nonStop: true,
        returnDate: '2025-07-10',
        });

        // Verify that the loading state has returned to normal (button re-enabled and text changed back)
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeEnabled();
        expect(screen.getByRole('button', { name: /Search Flights/i })).toHaveTextContent('Search Flights');
    });
});