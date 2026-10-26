/**
 * Every call to the Spring Boot API in one place. Pages call these functions instead of using
 * fetch() directly, so URLs and error handling live here.
 */

import { auth } from './auth.js';

/**
 * Where the API lives. Empty while developing (Vite passes /api on to Spring Boot); on the hosted
 * site it's the server's address, set at build time with VITE_API_BASE.
 */
const API_BASE = (import.meta.env.VITE_API_BASE ?? '').replace(/\/$/, '');

/** Sends a request and returns the parsed JSON, or throws an Error with the server's explanation. */
async function request(path, options = {}) {
  const token = auth.token();
  const headers = { ...(options.headers ?? {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) };
  let response;
  try {
    response = await fetch(`${API_BASE}/api${path}`, { ...options, headers });
  } catch {
    throw new Error('Can’t reach the Game Coach server. It may be starting up: try again in 30 seconds.');
  }
  if (response.status === 401 && token && !path.startsWith('/auth/')) {
    auth.signOut(); // the login expired: the pages send you to the login screen
    throw new Error('Your login has expired. Please log in again.');
  }
  const text = await response.text();
  let body = null;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = null; // not JSON (for example an HTML error page)
  }
  if (!response.ok) {
    // The API answers errors as ProblemDetail JSON: {"status": 404, "detail": "No uploaded match..."}.
    throw new Error(body?.detail ?? `The server answered ${response.status}.`);
  }
  return body;
}

/**
 * Coaching costs money per call, so the same match is never asked for twice at once (React's
 * StrictMode runs effects twice while developing). The promise is kept and shared instead.
 */
const pending = new Map();
function once(key, makeRequest) {
  if (!pending.has(key)) {
    pending.set(key, makeRequest().catch((error) => {
      pending.delete(key); // a failed request can be retried
      throw error;
    }));
  }
  return pending.get(key);
}

const json = (body) => ({ method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });

export const api = {
  login: (username, password) => request('/auth/login', json({ username, password })),

  signup: (username, password, inviteCode) => request('/auth/signup', json({ username, password, inviteCode })),

  heroes: (game) => request(`/${game}/heroes`),

  /** Saved matches, newest first. game is 'rivals', 'overwatch', or left out for both. */
  matches: (game) => request(game ? `/matches?game=${game}` : '/matches'),

  deleteMatch: (matchId) => request(`/matches/${matchId}`, { method: 'DELETE' }),

  /** formData holds the screenshots and optional fields, named the way the controller expects. */
  upload: (game, formData) => request(`/${game}/matches`, { method: 'POST', body: formData }),

  getMatch: (game, matchId) => request(`/${game}/matches/${matchId}`),

  /** The review step: sends the corrected match back and gets fresh warnings. */
  saveMatch: (game, matchId, match) =>
    request(`/${game}/matches/${matchId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(match),
    }),

  analysis: (game, matchId) => request(`/${game}/matches/${matchId}/analysis`),

  /** version changes when the match is corrected, so a fixed match gets fresh coaching. */
  feedback: (game, matchId, version = '') =>
    once(`${game}/${matchId}/${version}`, () => request(`/${game}/matches/${matchId}/feedback`, { method: 'POST' })),

  forgetFeedback: (game, matchId, version = '') => pending.delete(`${game}/${matchId}/${version}`),
};
