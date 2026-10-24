import { formatNumber } from '../format.js';

/**
 * One team's rows. Each hero cell is a dropdown so a wrong portrait read can be fixed, and the
 * "You" column marks whose row gets coached. Rows the reader was unsure about are highlighted.
 */
export default function TeamTable({ game, title, rows, heroesByRole, onHeroChange, onRoleChange, onYouChange, headKind }) {
  return (
    <section className={`team-table team-table--${headKind}`}>
      <div className="team-table__head">
        <h2>{title}</h2>
        <span>{composition(game, rows)}</span>
      </div>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th scope="col" className="col-player">Player</th>
              <th scope="col" className="col-role">Role</th>
              <th scope="col" className="col-hero">Hero</th>
              {game.columns.map((column) => <th key={column.key} scope="col" className="num">{column.label}</th>)}
              {/* The enemy table gets the same column, empty, so both tables line up. */}
              <th scope="col" className="col-you center">{onYouChange ? 'You' : ''}</th>
            </tr>
          </thead>
          <tbody>
            {rows.map(({ player, index, flagged, heroRole }) => (
              <tr key={index} className={heroRole ? 'row--mismatch' : player.uploader ? 'row--you' : flagged ? 'row--flagged' : undefined}>
                <td>
                  <span className="player">
                    <span className="player__name">
                      {player.name ?? '(hidden name)'}
                      {player.uploader && <span className="badge badge--solid badge--small">You</span>}
                    </span>
                    {player.title && <span className="player__title">{player.title}</span>}
                  </span>
                </td>
                <td><span className="role-chip">{game.roleShort[player.role] ?? '?'}</span></td>
                <td>
                  <label className="hero-pick">
                    {flagged && <span className="hero-pick__flag">Not sure: check this hero</span>}
                    <select className={flagged ? 'select select--flagged' : 'select'} value={player[game.heroField] ?? ''}
                      aria-label={`Hero for ${player.name ?? 'hidden player'}`}
                      onChange={(event) => onHeroChange(index, event.target.value)}>
                      {/* Keep the value as read even if it isn't in the list, so nothing changes silently. */}
                      {!heroesByRole.all.includes(player[game.heroField]) && (
                        <option value={player[game.heroField] ?? ''}>{player[game.heroField] ?? 'Unknown'}</option>
                      )}
                      {Object.entries(heroesByRole.byRole).map(([role, names]) => (
                        <optgroup key={role} label={game.roles[role] ?? role}>
                          {names.map((name) => <option key={name} value={name}>{name}</option>)}
                        </optgroup>
                      ))}
                    </select>
                  </label>
                  {heroRole && (
                    <span className="role-note">
                      The icon showed {game.roles[player.role] ?? player.role}, but {player[game.heroField]} is
                      a {game.roles[heroRole] ?? heroRole}.{' '}
                      <button type="button" className="link-button" onClick={() => onRoleChange(index, heroRole)}>
                        Use {game.roles[heroRole] ?? heroRole}
                      </button>
                    </span>
                  )}
                </td>
                {game.columns.map((column) => (
                  <td key={column.key} className="num">
                    {column.percent ? (player[column.key] == null ? '–' : `${Math.round(player[column.key])}%`) : formatNumber(player[column.key])}
                  </td>
                ))}
                <td className="center">
                  {onYouChange && (
                    <input type="radio" name="you" checked={player.uploader}
                      aria-label={`${player.name ?? 'This row'} is me`} onChange={() => onYouChange(index)} />
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

/** "1 tank · 2 damage · 2 support" from the role icons. */
function composition(game, rows) {
  const counts = {};
  rows.forEach(({ player }) => { counts[player.role] = (counts[player.role] ?? 0) + 1; });
  return Object.entries(counts)
    .map(([role, count]) => `${count} ${(game.roles[role] ?? role).toLowerCase()}`)
    .join(' · ');
}
