/** Small helpers for showing values the way people read them. */

/** "PT5M30S" (how Java sends a Duration) → "5:30". */
export function formatDuration(iso) {
  if (!iso) return null;
  const match = /PT(?:(\d+)H)?(?:(\d+)M)?(?:([\d.]+)S)?/.exec(iso);
  if (!match) return iso;
  const hours = Number(match[1] ?? 0);
  const minutes = Number(match[2] ?? 0) + hours * 60;
  const seconds = Math.round(Number(match[3] ?? 0));
  return `${minutes}:${String(seconds).padStart(2, '0')}`;
}

/** 7013 → "7,013". */
export function formatNumber(value) {
  return typeof value === 'number' ? value.toLocaleString('en-US') : value ?? '–';
}

/** "VICTORY" → "Victory". */
export function titleCase(text) {
  return text ? text.charAt(0) + text.slice(1).toLowerCase().replaceAll('_', ' ') : '';
}
