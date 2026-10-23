import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router';
import Header from '../components/Header.jsx';
import ErrorBox from '../components/ErrorBox.jsx';
import Spinner from '../components/Spinner.jsx';
import { api } from '../api.js';
import { gameOf } from '../games.js';
import { formatNumber, titleCase } from '../format.js';

const CLOSENESS = { CLOSE: 'Close', CLEAR_EDGE: 'Clear edge', ONE_SIDED: 'One-sided' };

/** Step 3: the coaching, next to the numbers it's based on. */
export default function ResultsPage({ theme }) {
  const { game: gameId, matchId } = useParams();
  const version = useLocation().state?.version ?? '';
  const game = gameOf(gameId);

  const [match, setMatch] = useState(null);
  const [analysis, setAnalysis] = useState(null);
  const [feedback, setFeedback] = useState(null);
  const [error, setError] = useState(null);
  const [attempt, setAttempt] = useState(0); // bumping this re-runs the effect: "try again"

  useEffect(() => {
    if (!game) return undefined;
    let ignore = false;
    setError(null);
    // The match and analysis are free and quick; the coaching is a paid Claude call that takes a while.
    Promise.all([api.getMatch(gameId, matchId), api.analysis(gameId, matchId)])
      .then(([m, a]) => { if (!ignore) { setMatch(m); setAnalysis(a); } })
      .catch((err) => { if (!ignore) setError(err.message); });
    api.feedback(gameId, matchId, version)
      .then((f) => { if (!ignore) setFeedback(f); })
      .catch((err) => { if (!ignore) setError(err.message); });
    return () => { ignore = true; };
  }, [game, gameId, matchId, version, attempt]);

  // useCallback keeps the same function between renders, handy when it's passed to a child.
  const retry = useCallback(() => {
    api.forgetFeedback(gameId, matchId, version);
    setAttempt((n) => n + 1);
  }, [gameId, matchId, version]);

  if (!game) return null;
  const you = match?.players.find((p) => p.uploader);
  const hero = you?.[game.heroField];

  return (
    <div className="page page--inner">
      <div className="stripe" />
      <Header theme={theme} step={3} />
      <main className="container stack-md">
        <section className="result-banner">
          <div className="stack-xs result-banner__text">
            <span className="eyebrow eyebrow--dark">{[hero, match?.mode, match?.map, resultText(match)].filter(Boolean).join(' · ')}</span>
            <h1 className="page-title">Your coaching</h1>
            <p>{feedback?.matchContext ?? 'Your coach is looking at the match…'}</p>
          </div>
          {analysis?.closeness && (
            <div className="verdict">
              <span className="verdict__label">How close was it?</span>
              <span className="verdict__value">{CLOSENESS[analysis.closeness.level] ?? titleCase(analysis.closeness.level)}</span>
            </div>
          )}
        </section>

        {error && <ErrorBox message={error} onRetry={retry} />}

        <div className="results-layout">
          <div className="results-main stack-md">
            {!feedback && !error && <Spinner label="Writing your coaching. This takes 20 to 40 seconds." />}
            {feedback && (
              <>
                <FeedbackList title="Strengths" tone="good" items={feedback.strengths}
                  empty="Nothing stood out as a strength this match, and that's fine. Look at the advice below." />
                <FeedbackList title="Weaknesses" tone="bad" items={feedback.weaknesses} empty="No clear weaknesses this match." />
                <FeedbackList title="Try next match" tone="next" items={feedback.advice} />
              </>
            )}
          </div>
          {analysis && <StatsPanel game={game} analysis={analysis} />}
        </div>
      </main>
    </div>
  );
}

/** One list of coaching points. Renders the `empty` text when the list has nothing in it. */
function FeedbackList({ title, tone, items, empty }) {
  return (
    <section className="card stack-sm">
      <h2 className="card__title"><span className={`dot dot--${tone}`} aria-hidden="true" />{title}</h2>
      {items && items.length > 0 ? (
        <ol className="points">
          {items.map((item) => <li key={item}>{item}</li>)}
        </ol>
      ) : (
        <p className="muted">{empty}</p>
      )}
    </section>
  );
}

/** Your numbers from the analysis: value, rank, share of your team, and for Overwatch the same-role enemy. */
function StatsPanel({ game, analysis }) {
  const entries = Object.entries(analysis.stats ?? {});
  const role = game.roles[analysis.role]?.toLowerCase();
  return (
    <aside className="card stats-panel stack-sm" aria-labelledby="stats-title">
      <h2 id="stats-title" className="card__title">Your numbers</h2>
      <p className="small muted">Bars show your share of your team's total. Rank 1 is best (for deaths, the fewest).</p>
      {entries.map(([key, stat]) => (
        <div key={key} className="stat">
          <div className="stat__top">
            <span className="stat__label">{game.stats[key] ?? titleCase(key)}</span>
            <span className="muted small">Rank {stat.lobbyRank} of {analysis.lobbySize}</span>
          </div>
          <div className="stat__values">
            <span className="stat__value">{formatNumber(stat.value)}</span>
            {stat.sameRoleEnemyBest != null && role && (
              <span className="muted small">Best enemy {role}: {formatNumber(stat.sameRoleEnemyBest)}</span>
            )}
          </div>
          <div className="bar"><div className="bar__fill" style={{ width: `${Math.min(100, stat.teamSharePercent ?? 0)}%` }} /></div>
        </div>
      ))}
      <Link to="/" className="button button--primary">Coach another match</Link>
    </aside>
  );
}

function resultText(match) {
  if (!match?.result) return null;
  return `${titleCase(match.result)}${match.score ? ` ${match.score.teamA}–${match.score.teamB}` : ''}`;
}
