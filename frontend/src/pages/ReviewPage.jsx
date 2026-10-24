import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router';
import Header from '../components/Header.jsx';
import TeamTable from '../components/TeamTable.jsx';
import ErrorBox from '../components/ErrorBox.jsx';
import Spinner from '../components/Spinner.jsx';
import { ArrowIcon, WarningIcon } from '../components/Icons.jsx';
import { api } from '../api.js';
import { gameOf } from '../games.js';
import { formatDuration, titleCase } from '../format.js';

/** Step 2: show the match as read, let the user fix heroes, then go to coaching. */
export default function ReviewPage({ theme }) {
  const { game: gameId, matchId } = useParams(); // from the URL, e.g. /overwatch/matches/ow-1a2b/review
  const location = useLocation();
  const navigate = useNavigate();
  const game = gameOf(gameId);

  const [match, setMatch] = useState(null);
  const [heroes, setHeroes] = useState([]);
  const [warnings, setWarnings] = useState(location.state?.warnings ?? null);
  const [changed, setChanged] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // Load the match and the hero list when the page opens (or the URL changes).
  useEffect(() => {
    if (!game) return undefined;
    let ignore = false;
    Promise.all([api.getMatch(gameId, matchId), api.heroes(gameId)])
      .then(([loadedMatch, heroList]) => {
        if (ignore) return;
        setMatch(loadedMatch);
        setHeroes(heroList);
      })
      .catch((err) => { if (!ignore) setError(err.message); });
    return () => { ignore = true; };
  }, [game, gameId, matchId]);

  // useMemo keeps a computed value until its inputs change, instead of recomputing every render.
  const heroesByRole = useMemo(() => {
    const byRole = {};
    heroes.forEach((h) => { (byRole[h.role] ??= []).push(h.name); });
    return { byRole, all: heroes.map((h) => h.name), roleOf: Object.fromEntries(heroes.map((h) => [h.name, h.role])) };
  }, [heroes]);

  if (!game) return <Navigate404 />;

  // Rows the reader flagged: a warning that names the player and mentions their portrait or hero.
  const isFlagged = (player) => {
    if (!warnings || !player.name) return false;
    const name = player.name.toLowerCase();
    return warnings.some((w) => {
      const text = w.toLowerCase();
      return text.includes(name) && (text.includes('portrait') || text.includes('hero'));
    });
  };

  // Changing a hero keeps the row's role as the reader saw it: role icons are read very reliably,
  // so a hero from another role is more often a slip than a real change. The row then shows a note
  // with a button to switch the role on purpose (see roleMismatch below).
  function changeHero(index, heroName) {
    setMatch((current) => ({
      ...current,
      players: current.players.map((p, i) => (i === index ? { ...p, [game.heroField]: heroName } : p)),
    }));
    setChanged(true);
  }

  function changeRole(index, role) {
    setMatch((current) => ({
      ...current,
      players: current.players.map((p, i) => (i === index ? { ...p, role } : p)),
    }));
    setChanged(true);
  }

  /** The hero's real role when it differs from the row's role icon; null when they agree. */
  const roleMismatch = (player) => {
    const heroRole = heroesByRole.roleOf[player[game.heroField]];
    return heroRole && player.role && player.role !== 'UNKNOWN' && heroRole !== player.role ? heroRole : null;
  };

  function changeYou(index) {
    setMatch((current) => ({ ...current, players: current.players.map((p, i) => ({ ...p, uploader: i === index })) }));
    setChanged(true);
  }

  async function continueToCoaching() {
    setSaving(true);
    setError(null);
    try {
      let version = '';
      if (changed) {
        const saved = await api.saveMatch(gameId, matchId, match);
        setWarnings(saved.warnings);
        version = String(Date.now()); // a corrected match gets fresh coaching
      }
      navigate(`/${gameId}/matches/${matchId}/results`, { state: { version } });
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  }

  const rowsFor = (team) => (match?.players ?? [])
    .map((player, index) => ({ player, index, flagged: isFlagged(player), heroRole: roleMismatch(player) }))
    .filter(({ player }) => player.team === team);
  const flaggedCount = (match?.players ?? []).filter(isFlagged).length;
  const mismatchCount = (match?.players ?? []).filter((p) => roleMismatch(p)).length;
  const you = match?.players.find((p) => p.uploader);

  return (
    <div className="page page--inner">
      <div className="stripe" />
      <Header theme={theme} step={2} />
      <main className="container stack-md">
        <section className="title-row">
          <div className="stack-xs">
            <span className="eyebrow">{[game.name, match?.mode, match?.map].filter(Boolean).join(' · ')}</span>
            <h1 className="page-title">Check the read</h1>
            <p className="lead">Every number was read from your screenshots. Portraits are harder, so fix any hero we
              guessed wrong before coaching.</p>
          </div>
          {match && (
            <div className="facts">
              <Fact label="Result" value={resultText(match)} tone={match.result === 'VICTORY' ? 'win' : undefined} />
              <Fact label="Length" value={formatDuration(match.duration) ?? 'Unknown'} />
              <Fact label="Your hero" value={you?.[game.heroField] ?? 'Not marked'} />
            </div>
          )}
        </section>

        {warnings && warnings.length > 0 && (
          <section className="warning-box" aria-labelledby="warn-title">
            <WarningIcon />
            <div className="stack-xs">
              <h2 id="warn-title">{flaggedCount > 0 ? `${flaggedCount} ${flaggedCount === 1 ? 'hero needs' : 'heroes need'} a quick check` : 'A few things to check'}</h2>
              <details>
                <summary>Everything the reader noted ({warnings.length})</summary>
                <ul>{warnings.map((w) => <li key={w}>{w}</li>)}</ul>
              </details>
            </div>
          </section>
        )}

        {mismatchCount > 0 && (
          <section className="warning-box" aria-live="polite">
            <WarningIcon />
            <p><strong>{mismatchCount === 1 ? '1 row has' : `${mismatchCount} rows have`} a hero that doesn't match the role icon.</strong>{' '}
              That's usually a slip in the dropdown. Check the highlighted {mismatchCount === 1 ? 'row' : 'rows'}.</p>
          </section>
        )}

        {error && <ErrorBox message={error} />}
        {!match && !error && <Spinner label="Loading the match" />}

        {match && (
          <>
            <TeamTable game={game} title="Your team" headKind="own" rows={rowsFor('A')} heroesByRole={heroesByRole}
              onHeroChange={changeHero} onRoleChange={changeRole} onYouChange={changeYou} />
            <TeamTable game={game} title="Enemy team" headKind="enemy" rows={rowsFor('B')} heroesByRole={heroesByRole}
              onHeroChange={changeHero} onRoleChange={changeRole} />
            <div className="actions">
              <Link to="/" className="button button--outline">Upload again</Link>
              <button type="button" className="button button--primary button--large" onClick={continueToCoaching}
                disabled={saving || !you}>
                {changed ? 'Save fixes and coach me' : 'Looks right, coach me'} <ArrowIcon />
              </button>
            </div>
            {!you && <p className="small muted right">Mark your row in the "You" column first.</p>}
          </>
        )}
      </main>
    </div>
  );
}

function Fact({ label, value, tone }) {
  return (
    <div className="fact">
      <span className="fact__label">{label}</span>
      <span className={tone ? `fact__value fact__value--${tone}` : 'fact__value'}>{value}</span>
    </div>
  );
}

function resultText(match) {
  if (!match.result) return 'Unknown';
  const score = match.score ? ` ${match.score.teamA}–${match.score.teamB}` : '';
  return `${titleCase(match.result)}${score}`;
}

function Navigate404() {
  return (
    <main className="container"><ErrorBox message="Unknown game in this link." /><Link to="/">Back to upload</Link></main>
  );
}
