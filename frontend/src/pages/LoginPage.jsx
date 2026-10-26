import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';
import Header from '../components/Header.jsx';
import ErrorBox from '../components/ErrorBox.jsx';
import { api } from '../api.js';
import { auth, useAuth } from '../auth.js';

/** Log in, or sign up with an invite code. Uploading is invite-only because each read costs money. */
export default function LoginPage({ theme }) {
  const session = useAuth();
  const navigate = useNavigate();
  const from = useLocation().state?.from ?? '/';
  const [mode, setMode] = useState('login'); // 'login' or 'signup'
  const [form, setForm] = useState({ username: '', password: '', inviteCode: '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  if (session) return <Navigate to={from} replace />;

  // One handler for every field: the input's name says which part of the form to change.
  const change = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  async function submit(event) {
    event.preventDefault(); // stop the browser's own form submit (a full page reload)
    setBusy(true);
    setError(null);
    try {
      const response = mode === 'login'
        ? await api.login(form.username, form.password)
        : await api.signup(form.username, form.password, form.inviteCode);
      auth.signIn(response);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  }

  const signup = mode === 'signup';
  return (
    <div className="page page--upload">
      <div className="stripe" />
      <Header theme={theme} />
      <main className="container stack-lg">
        <section className="panel auth-panel stack-md" aria-labelledby="auth-title">
          <div className="stack-xs">
            <span className="eyebrow">Invite only</span>
            <h1 id="auth-title" className="page-title">{signup ? 'Create your account' : 'Log in'}</h1>
            <p className="lead">Every screenshot read is a paid AI call, so uploading is limited to people with an
              invite. You can still see <Link to="/sample">a sample coaching</Link> from a real match.</p>
          </div>
          <div className="filter-tabs" role="tablist" aria-label="Log in or sign up">
            <button type="button" role="tab" aria-selected={!signup} className={!signup ? 'filter-tab filter-tab--on' : 'filter-tab'}
              onClick={() => { setMode('login'); setError(null); }}>Log in</button>
            <button type="button" role="tab" aria-selected={signup} className={signup ? 'filter-tab filter-tab--on' : 'filter-tab'}
              onClick={() => { setMode('signup'); setError(null); }}>I have an invite</button>
          </div>
          <form className="stack-sm" onSubmit={submit}>
            <label className="field">
              <span className="field__label">Username</span>
              <input className="input" name="username" value={form.username} onChange={change}
                autoComplete="username" required minLength={3} maxLength={20} />
            </label>
            <label className="field">
              <span className="field__label">Password {signup && <span className="muted">(8 or more characters)</span>}</span>
              <input className="input" type="password" name="password" value={form.password} onChange={change}
                autoComplete={signup ? 'new-password' : 'current-password'} required minLength={signup ? 8 : 1} />
            </label>
            {signup && (
              <label className="field">
                <span className="field__label">Invite code</span>
                <input className="input" name="inviteCode" value={form.inviteCode} onChange={change}
                  autoComplete="off" required />
              </label>
            )}
            {error && <ErrorBox message={error} />}
            <button type="submit" className="button button--primary button--large" disabled={busy}>
              {busy ? 'One moment…' : signup ? 'Create account' : 'Log in'}
            </button>
          </form>
        </section>
      </main>
    </div>
  );
}
