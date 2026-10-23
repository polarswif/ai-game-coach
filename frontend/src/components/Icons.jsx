/** Small inline SVG icons. aria-hidden: they decorate text that already says what they mean. */

export function LogoMark({ size = 34 }) {
  return (
    <svg className="logo-mark" width={size} height={size} viewBox="0 0 34 34" aria-hidden="true">
      <rect x="1" y="1" width="32" height="32" rx="9" className="logo-mark__bg" />
      <path d="M24.7 10.6 A10 10 0 1 0 26.8 19.1 H22" className="logo-mark__line" strokeLinejoin="round" />
      <path d="M20.5 13.5 A5 5 0 1 0 20.5 20.5" className="logo-mark__line" />
    </svg>
  );
}

export function BurstIcon() {
  return (
    <svg width="48" height="48" viewBox="0 0 48 48" aria-hidden="true">
      <path d="M24 3 L28 15 L40 8 L34 20 L46 24 L34 28 L40 40 L28 33 L24 45 L20 33 L8 40 L14 28 L2 24 L14 20 L8 8 L20 15 Z"
        fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinejoin="round" />
      <path d="M26 15 L19 26 H24 L22 34 L29 22 H24 Z" fill="currentColor" />
    </svg>
  );
}

export function CrosshairIcon() {
  return (
    <svg width="48" height="48" viewBox="0 0 48 48" aria-hidden="true">
      <circle cx="24" cy="24" r="15" fill="none" stroke="currentColor" strokeWidth="2.4" />
      <circle cx="24" cy="24" r="3" fill="currentColor" />
      <path d="M24 2 V14 M24 34 V46 M2 24 H14 M34 24 H46" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" />
    </svg>
  );
}

export function SunIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 20 20" aria-hidden="true">
      <circle cx="10" cy="10" r="4" fill="none" stroke="currentColor" strokeWidth="2" />
      <path d="M10 1.5V4 M10 16V18.5 M1.5 10H4 M16 10H18.5 M4 4L5.7 5.7 M14.3 14.3L16 16 M4 16L5.7 14.3 M14.3 5.7L16 4"
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

export function MoonIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 20 20" aria-hidden="true">
      <path d="M16.5 12.5 A7 7 0 1 1 7.5 3.5 A5.5 5.5 0 0 0 16.5 12.5 Z" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
    </svg>
  );
}

export function ArrowIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 20 20" aria-hidden="true">
      <path d="M4 10 H16 M11 5 L16 10 L11 15" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function WarningIcon() {
  return (
    <svg width="26" height="26" viewBox="0 0 26 26" aria-hidden="true" className="warning-icon">
      <path d="M13 3 L24 22 H2 Z" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinejoin="round" />
      <path d="M13 10 V15 M13 18.5 V19" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
    </svg>
  );
}
