import React from 'react';
import './App.css';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import SearchPage from './pages/SearchPage/SearchPage';
import ResultsPage from './pages/ResultsPage/ResultsPage';
import DetailsPage from './pages/DetailsPage/DetailsPage';

function App() {
  return (
    <Router>
      <div className="App">
        <Routes>
          {/* Ruta principal para la página de búsqueda */}
          <Route path="/" element={<SearchPage />} />
          <Route path="/results" element={<ResultsPage />} />
          <Route path="/details/:offerId" element={<DetailsPage />} /> 
        </Routes>
      </div>
    </Router>
  );
}

export default App;
