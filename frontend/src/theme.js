import { useEffect, useState } from 'react';

const KEY = 'game-coach-theme';

/** The saved choice, or the system setting the first time. localStorage can throw (private mode). */
function initialTheme() {
  try {
    const saved = localStorage.getItem(KEY);
    if (saved === 'light' || saved === 'dark') return saved;
  } catch {
    // no saved choice available
  }
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

/**
 * A custom hook: a function starting with "use" that bundles state and effects so any component
 * can share the logic. Returns the current theme and a toggle.
 */
export function useTheme() {
  const [theme, setTheme] = useState(initialTheme);

  // After each render where the theme changed: put it on <html data-theme="..."> (the CSS
  // variables in index.css switch on it) and remember it for next time.
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    try {
      localStorage.setItem(KEY, theme);
    } catch {
      // not saved; the toggle still works for this visit
    }
  }, [theme]);

  return { theme, toggle: () => setTheme((t) => (t === 'dark' ? 'light' : 'dark')) };
}
