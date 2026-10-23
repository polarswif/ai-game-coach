/**
 * Every call to the Spring Boot API in one place. Pages call these functions instead of using
 * fetch() directly, so URLs and error handling live here.
 */

/** Sends a request and returns the parsed JSON, or throws an Error with the server's explanation. */
async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`/api${path}`, options);
  } catch {
    throw new Error('Can’t reach the Game Coach server. Is the Spring Boot app running?');
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

export const api = {
  heroes: (game) => request(`/${game}/heroes`),

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
