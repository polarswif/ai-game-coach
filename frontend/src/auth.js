import { useSyncExternalStore } from 'react';

/**
 * Who is signed in. The server hands back a token at login; it's kept in localStorage so a refresh
 * doesn't sign you out, and sent with every API call (see api.js). Components read it with useAuth(),
 * which re-renders them whenever someone signs in or out.
 */
const KEY = 'gamecoach.session';
const listeners = new Set();
let session = load();

function load() {
  try {
    const saved = JSON.parse(window.localStorage.getItem(KEY) ?? 'null');
    // A token past its expiry would just be refused by the server, so drop it now.
    return saved && new Date(saved.expiresAt) > new Date() ? saved : null;
  } catch {
    return null; // storage blocked or unreadable: start signed out
  }
}

function update(next) {
  session = next;
  try {
    if (next) window.localStorage.setItem(KEY, JSON.stringify(next));
    else window.localStorage.removeItem(KEY);
  } catch {
    // Not saved: you stay signed in until the page closes, which is fine.
  }
  listeners.forEach((listener) => listener());
}

export const auth = {
  /** { token, username, expiresAt } from the server, or null. */
  get: () => session,
  token: () => session?.token ?? null,
  signIn: (response) => update(response),
  signOut: () => update(null),
  subscribe: (listener) => {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};

/**
 * useSyncExternalStore is React's way to read a value that lives outside React (here, the session):
 * it calls subscribe once and re-renders the component when the value changes.
 */
export function useAuth() {
  return useSyncExternalStore(auth.subscribe, auth.get);
}
