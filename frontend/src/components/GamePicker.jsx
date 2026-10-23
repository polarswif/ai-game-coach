import { GAMES } from '../games.js';
import { BurstIcon, CrosshairIcon } from './Icons.jsx';

const ICONS = { rivals: <BurstIcon />, overwatch: <CrosshairIcon /> };

/**
 * Two big buttons, one per game. A "controlled" component: it doesn't keep its own state. The
 * parent passes the current `value` and an `onChange` function, and this just reports clicks.
 */
export default function GamePicker({ value, onChange }) {
  return (
    <div className="game-picker">
      {Object.values(GAMES).map((game) => {
        const selected = game.id === value;
        return (
          <button key={game.id} type="button" aria-pressed={selected}
            className={selected ? 'game-card game-card--selected' : 'game-card'}
            onClick={() => onChange(game.id)}>
            <span className="game-card__main">
              {ICONS[game.id]}
              <span className="game-card__text">
                <span className="game-card__name">{game.name}</span>
                <span className="game-card__blurb">{game.blurb}</span>
              </span>
            </span>
            {selected && <span className="badge badge--accent">Selected</span>}
          </button>
        );
      })}
    </div>
  );
}
