import { Link, NavLink } from 'react-router';
import { LogoMark, MoonIcon, SunIcon } from './Icons.jsx';

const STEPS = ['Upload', 'Review', 'Coaching'];

/**
 * The bar at the top of every page. Props are a component's inputs: `theme` comes from App,
 * `step` (1 to 3) says which step to highlight, and is left out on the upload page.
 */
export default function Header({ theme, step }) {
  const dark = theme.theme === 'dark';
  return (
    <header className="header">
      <Link to="/" className="brand">
        <LogoMark />
        <span>Game Coach</span>
      </Link>
      <div className="header__right">
        <NavLink to="/matches" className="pill">My matches</NavLink>
        {step ? (
          <ol className="steps">
            {STEPS.map((label, index) => (
              <li key={label} className={index + 1 === step ? 'steps__item steps__item--current' : 'steps__item'}
                aria-current={index + 1 === step ? 'step' : undefined}>
                {index + 1} {label}
              </li>
            ))}
          </ol>
        ) : (
          <nav className="nav">
            <NavLink to="/" end className="pill">Upload</NavLink>
          </nav>
        )}
        <button type="button" className="icon-button" onClick={theme.toggle}
          aria-label={dark ? 'Switch to light mode' : 'Switch to dark mode'}>
          {dark ? <SunIcon /> : <MoonIcon />}
        </button>
      </div>
    </header>
  );
}
