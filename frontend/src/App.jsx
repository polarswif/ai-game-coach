import { Routes, Route, Navigate } from 'react-router';
import UploadPage from './pages/UploadPage.jsx';
import ReviewPage from './pages/ReviewPage.jsx';
import ResultsPage from './pages/ResultsPage.jsx';
import MatchesPage from './pages/MatchesPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import RequireAuth from './components/RequireAuth.jsx';
import { SAMPLE } from './sample.js';
import { useTheme } from './theme.js';

/**
 * The app's screens and their URLs. ":game" and ":matchId" are URL parameters: a page reads them
 * with useParams(), e.g. /overwatch/matches/ow-1a2b3c4d/review.
 */
export default function App() {
  const theme = useTheme();
  return (
    <Routes>
      {/* Open to everyone */}
      <Route path="/login" element={<LoginPage theme={theme} />} />
      <Route path="/sample" element={<ResultsPage theme={theme} sample={SAMPLE} />} />
      {/* Need an account: RequireAuth sends visitors to /login first */}
      <Route path="/" element={<RequireAuth><UploadPage theme={theme} /></RequireAuth>} />
      <Route path="/matches" element={<RequireAuth><MatchesPage theme={theme} /></RequireAuth>} />
      <Route path="/:game/matches/:matchId/review" element={<RequireAuth><ReviewPage theme={theme} /></RequireAuth>} />
      <Route path="/:game/matches/:matchId/results" element={<RequireAuth><ResultsPage theme={theme} /></RequireAuth>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
