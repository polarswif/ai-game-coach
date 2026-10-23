/**
 * What differs between the two games: names, roles, scoreboard columns, and the field that holds a
 * row's hero. The pages read this instead of checking "if rivals ... else ..." everywhere.
 */
export const GAMES = {
  rivals: {
    id: 'rivals',
    name: 'Marvel Rivals',
    blurb: 'One scoreboard screenshot · 6v6',
    heroField: 'lastHero',
    heroExample: 'Daredevil',
    roles: { VANGUARD: 'Vanguard', DUELIST: 'Duelist', STRATEGIST: 'Strategist', UNKNOWN: 'Unknown' },
    roleShort: { VANGUARD: 'VAN', DUELIST: 'DUE', STRATEGIST: 'STR', UNKNOWN: '?' },
    columns: [
      { key: 'kills', label: 'K' },
      { key: 'deaths', label: 'D' },
      { key: 'assists', label: 'A' },
      { key: 'finalHits', label: 'FH' },
      { key: 'damage', label: 'Damage' },
      { key: 'blocked', label: 'Blocked' },
      { key: 'healing', label: 'Healing' },
      { key: 'accuracyPercent', label: 'Acc', percent: true },
    ],
    stats: { KILLS: 'KOs', DEATHS: 'Deaths', ASSISTS: 'Assists', FINAL_HITS: 'Final hits', DAMAGE: 'Damage', BLOCKED: 'Damage blocked', HEALING: 'Healing' },
  },
  overwatch: {
    id: 'overwatch',
    name: 'Overwatch',
    blurb: 'Match report screens · 5v5',
    heroField: 'hero',
    heroExample: 'Ramattra',
    roles: { TANK: 'Tank', DAMAGE: 'Damage', SUPPORT: 'Support', UNKNOWN: 'Unknown' },
    roleShort: { TANK: 'TANK', DAMAGE: 'DMG', SUPPORT: 'SUP', UNKNOWN: '?' },
    columns: [
      { key: 'eliminations', label: 'E' },
      { key: 'assists', label: 'A' },
      { key: 'deaths', label: 'D' },
      { key: 'damage', label: 'Damage' },
      { key: 'healing', label: 'Healing' },
      { key: 'mitigation', label: 'Mitigation' },
    ],
    stats: { ELIMINATIONS: 'Eliminations', ASSISTS: 'Assists', DEATHS: 'Deaths', DAMAGE: 'Hero damage', HEALING: 'Healing', MITIGATION: 'Mitigation' },
  },
};

export function gameOf(id) {
  return GAMES[id] ?? null;
}
