import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router';
import App from './App.jsx';
import './index.css';

// The entry point: React takes over the <div id="root"> in index.html and renders <App /> into it.
// BrowserRouter keeps the page in sync with the address bar, so each screen has its own URL.
// StrictMode runs extra checks while developing (it runs effects twice to catch bugs).
createRoot(document.getElementById('root')).render(
  <StrictMode>
    {/* basename: on GitHub Pages the site lives under /ai-game-coach/, set by Vite's base option. */}
    <BrowserRouter basename={import.meta.env.BASE_URL.replace(/\/$/, '')}>
      <App />
    </BrowserRouter>
  </StrictMode>,
);
