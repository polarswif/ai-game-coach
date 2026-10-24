import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import Header from '../components/Header.jsx';
import ErrorBox from '../components/ErrorBox.jsx';
import Spinner from '../components/Spinner.jsx';
import { BurstIcon, CrosshairIcon } from '../components/Icons.jsx';
import { api } from '../api.js';
import { GAMES } from '../games.js';
import { formatDate, formatSeconds, titleCase } from '../format.js';

const FILTERS = [
  { id: '', label: 'All games' },
  { id: 'rivals', label: 'Marvel Rivals' },
  { id: 'overwatch', label: 'Overwatch' },
];

/** Every saved match, newest first, with links back to its review and coaching. */
export default function MatchesPage({ theme }) {
  const [filter, setFilter] = useState('');
  const [matches, setMatches] = useState(null); // null = still loading
  const [error, setError] = useState(null);

  useEffect(() => {
    let ignore = false;
    setMatches(null);
    setError(null);
    api.matches(filter)
      .then((list) => { if (!ignore) setMatches(list); })
      .catch((err) => { if (!ignore) setError(err.message); });
    return () => { ignore = true; };
  }, [filter]);

  async function remove(match) {
    // window.confirm shows the browser's own yes/no box; deleting can't be undone.
    if (!window.confirm(`Delete this ${GAMES[match.game]?.name ?? ''} match on ${match.map ?? 'an unknown map'} and its coaching?`)) {
      return;
    }
    try {
      await api.deleteMatch(match.matchId);
      // Take it out of the list we already have instead of loading the whole list again.
      setMatches((current) => current.filter((m) => m.matchId !== match.matchId));
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="page page--inner">
      <div className="stripe" />
      <Header theme={theme} />
      <main className="container stack-md">
        <section className="title-row">
          <div className="stack-xs">
            <span className="eyebrow">Saved in your database</span>
            <h1 className="page-title">My matches</h1>
          </div>
          <div className="filter-tabs" role="group" aria-label="Show matches from">
            {FILTERS.map((f) => (
              <button key={f.id || 'all'} type="button" aria-pressed={filter === f.id}
                className={filter === f.id ? 'filter-tab filter-tab--on' : 'filter-tab'} onClick={() => setFilter(f.id)}>
                {f.label}
              </button>
            ))}
          </div>
        </section>

        {error && <ErrorBox message={error} />}
        {!matches && !error && <Spinner label="Loading your matches" />}

        {matches && matches.length === 0 && (
          <section className="card stack-sm empty">
            <h2 className="card__title">No matches yet</h2>
            <p className="muted">Upload a match and it will show up here, with its coaching saved alongside it.</p>
            <Link to="/" className="button button--primary">Upload a match</Link>
          </section>
        )}

        {matches && matches.length > 0 && (
          <ul className="match-list">
            {matches.map((m) => (
              <li key={m.matchId} className="match-row">
                <span className="match-row__icon">{m.game === 'rivals' ? <BurstIcon /> : <CrosshairIcon />}</span>
                <span className="match-row__main">
                  <span className="match-row__title">
                    {[m.yourHero, m.map].filter(Boolean).join(' · ') || 'Match'}
                  </span>
                  <span className="match-row__meta">
                    {[GAMES[m.game]?.name, m.mode, formatSeconds(m.durationSeconds), formatDate(m.uploadedAt)].filter(Boolean).join(' · ')}
                  </span>
                </span>
                <span className={`result result--${(m.result ?? 'unknown').toLowerCase()}`}>
                  {m.result ? `${titleCase(m.result)}${m.score ? ` ${m.score.replace('-', '–')}` : ''}` : 'Result unknown'}
                </span>
                <span className="match-row__actions">
                  <Link to={`/${m.game}/matches/${m.matchId}/review`} className="button button--outline button--small">Review</Link>
                  <Link to={`/${m.game}/matches/${m.matchId}/results`} className="button button--primary button--small">Coaching</Link>
                  <button type="button" className="icon-button icon-button--small" onClick={() => remove(m)}
                    aria-label={`Delete match on ${m.map ?? 'unknown map'}`}>
                    <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
                      <path d="M3 5 H15 M7 5 V3 H11 V5 M5 5 L6 15 H12 L13 5" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </button>
                </span>
              </li>
            ))}
          </ul>
        )}
      </main>
    </div>
  );
}
