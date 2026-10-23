import { Routes, Route, Navigate } from 'react-router';
import UploadPage from './pages/UploadPage.jsx';
import ReviewPage from './pages/ReviewPage.jsx';
import ResultsPage from './pages/ResultsPage.jsx';
import { useTheme } from './theme.js';

/**
 * The app's screens and their URLs. ":game" and ":matchId" are URL parameters: a page reads them
 * with useParams(), e.g. /overwatch/matches/ow-1a2b3c4d/review.
 */
export default function App() {
  const theme = useTheme();
  return (
    <Routes>
      <Route path="/" element={<UploadPage theme={theme} />} />
      <Route path="/:game/matches/:matchId/review" element={<ReviewPage theme={theme} />} />
      <Route path="/:game/matches/:matchId/results" element={<ResultsPage theme={theme} />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
