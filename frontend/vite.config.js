import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The React dev server runs on http://localhost:5173. Every request to /api is passed on to the
// Spring Boot app on port 8080, so the browser sees one site and no CORS setup is needed.
export default defineConfig({
  plugins: [react()],
  // The path the site is served from: "/" locally, "/ai-game-coach/" on GitHub Pages (set by the deploy workflow).
  base: process.env.VITE_BASE ?? '/',
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
