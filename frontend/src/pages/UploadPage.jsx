import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import Header from '../components/Header.jsx';
import GamePicker from '../components/GamePicker.jsx';
import DropZone from '../components/DropZone.jsx';
import ErrorBox from '../components/ErrorBox.jsx';
import Spinner from '../components/Spinner.jsx';
import { ArrowIcon } from '../components/Icons.jsx';
import { api } from '../api.js';
import { gameOf } from '../games.js';

/** Step 1: choose the game, add the screenshots, and send them to be read. */
export default function UploadPage({ theme }) {
  const navigate = useNavigate();

  // State: values that change while the page is open. Each setX call re-renders the page.
  // Start on the game picked last time, so "Upload again" returns to it.
  const [gameId, setGameId] = useState(() => (gameOf(readSaved(GAME_KEY)) ? readSaved(GAME_KEY) : 'overwatch'));
  const [shots, setShots] = useState({ screenshot: [], teams: [], summary: [], personal: [] });
  const [score, setScore] = useState('');
  const [hero, setHero] = useState('');
  const [heroNames, setHeroNames] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const game = gameOf(gameId);
  const isOverwatch = gameId === 'overwatch';
  const required = isOverwatch ? shots.teams : shots.screenshot;

  // Load the hero names for the "your hero" suggestions whenever the game changes.
  // `ignore` stops a slow answer for the previous game from overwriting the new one.
  useEffect(() => {
    let ignore = false;
    api.heroes(gameId)
      .then((list) => { if (!ignore) setHeroNames(list.map((h) => h.name)); })
      .catch(() => { if (!ignore) setHeroNames([]); });
    return () => { ignore = true; };
  }, [gameId]);

  function setShot(field) {
    return (files) => setShots((current) => ({ ...current, [field]: files }));
  }

  async function handleSubmit(event) {
    event.preventDefault(); // stop the browser's own form submit (a page reload)
    setBusy(true);
    setError(null);
    const form = new FormData();
    if (isOverwatch) {
      form.append('teams', shots.teams[0]);
      if (shots.summary[0]) form.append('summary', shots.summary[0]);
      shots.personal.forEach((file) => form.append('personal', file));
    } else {
      form.append('screenshot', shots.screenshot[0]);
      if (score.trim()) form.append('score', score.trim());
    }
    if (hero.trim()) form.append('hero', hero.trim());
    try {
      const parsed = await api.upload(gameId, form);
      // Go to the review page. `state` carries the warnings and hero doubts along (they aren't stored on the server).
      navigate(`/${gameId}/matches/${parsed.match.matchId}/review`,
        { state: { warnings: parsed.warnings, heroDoubts: parsed.heroDoubts } });
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  }

  return (
    <div className="page page--upload">
      <Header theme={theme} />
      <main className="container stack-lg">
        <section className="hero-text">
          <h1>Upload your match.<br />Get coached.</h1>
          <p>Drop in your end-of-match screenshots. We read every stat, you check the read, and the coach tells you
            what went well, what to fix and what to try next game.</p>
        </section>

        <section aria-labelledby="game-label" className="stack-sm">
          <h2 id="game-label" className="section-label">1 · Choose your game</h2>
          <GamePicker value={gameId} onChange={(id) => { setGameId(id); save(GAME_KEY, id); setHero(''); setError(null); }} />
        </section>

        <form className="panel stack-md" onSubmit={handleSubmit} aria-labelledby="shots-label">
          <h2 id="shots-label" className="section-label">2 · Add your screenshots</h2>

          {isOverwatch ? (
            <div className="zone-grid">
              <DropZone title="Teams" badge="Required" badgeKind="solid" files={shots.teams} onFiles={setShot('teams')}
                description="The scoreboard with all 10 players. This is the match itself." />
              <DropZone title="Summary" badge="Recommended" badgeKind="accent" files={shots.summary} onFiles={setShot('summary')}
                description="Result, final score, map, mode, game length and your heroes." />
              <DropZone title="Personal" badge="Optional" badgeKind="outline" soft multiple files={shots.personal}
                onFiles={setShot('personal')} description="One per hero you played. Adds hero stats like accuracy and ability kills." />
            </div>
          ) : (
            <div className="zone-row">
              <DropZone title="Scoreboard" badge="Required" badgeKind="solid" files={shots.screenshot} onFiles={setShot('screenshot')}
                description="The end-of-match scoreboard with all 12 players, taken before you leave the results screen." />
              <label className="field-card">
                <span className="drop-zone__title">Round score</span>
                <span className="drop-zone__description">From match history, your rounds first. Optional.</span>
                <input type="text" className="input" placeholder="3-2" value={score} onChange={(e) => setScore(e.target.value)} />
              </label>
            </div>
          )}

          <div className="submit-row">
            <label className="field">
              <span className="field__label">Your hero <span className="muted">(optional, only if we read it wrong)</span></span>
              <input type="text" className="input" list="hero-names" placeholder={`e.g. ${game.heroExample}`}
                value={hero} onChange={(e) => setHero(e.target.value)} />
              <datalist id="hero-names">
                {heroNames.map((name) => <option key={name} value={name} />)}
              </datalist>
            </label>
            <button type="submit" className="button button--primary button--large" disabled={busy || required.length === 0}>
              Read my match <ArrowIcon />
            </button>
          </div>
          {busy && <Spinner label="Reading your screenshots. This takes about 30 seconds." />}
          {error && <ErrorBox message={error} />}
          <p className="small muted">Your screenshots are only used to read this match.</p>
        </form>
      </main>
    </div>
  );
}

const GAME_KEY = 'gamecoach.game';

// Browser storage can be missing or blocked (private windows, strict settings): fall back quietly.
function readSaved(key) {
  try { return window.localStorage.getItem(key); } catch { return null; }
}

function save(key, value) {
  try { window.localStorage.setItem(key, value); } catch { /* not remembered this time; nothing breaks */ }
}
