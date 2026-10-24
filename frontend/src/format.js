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

/** "3:05 PM, Oct 23" from an ISO date string (or seconds since 1970, which some JSON settings send). */
export function formatDate(value) {
  if (value == null) return '';
  const date = typeof value === 'number' ? new Date(value * 1000) : new Date(value);
  return Number.isNaN(date.getTime()) ? String(value)
    : date.toLocaleString('en-US', { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' });
}

/** 640 → "10:40". */
export function formatSeconds(seconds) {
  if (seconds == null) return null;
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
}
