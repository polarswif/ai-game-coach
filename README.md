# AI Game Coach

An AI coaching app for competitive games. Players upload a match scoreboard and get strengths, weaknesses, and advice for their next match.

**Status:** Early development. The backend API works end to end with fake match data and fake feedback, and computes a real statistical analysis of each match.

## Stack
- **Backend:** Java 17, Spring Boot 4.1.1, Maven
- **Tests:** JUnit 5, run on every push with GitHub Actions
- **Frontend:** React (planned)
- **AI:** Claude API (planned)

## How it works
1. **Match data** comes from a `MatchHistorySource`. Today that's a built-in match copied from a real scoreboard.
2. **`MatchAnalyzer`** computes the numbers in Java: KDA, per-minute stats, each player's share of their team's totals, and their rank on their team and in the lobby. Hero roles come from a table in `src/main/resources/heroes/`.
3. **A `CoachingService`** turns the match and that analysis into strengths, weaknesses, and advice. Today it returns canned text; next it will call Claude.

Doing the math in Java means the coaching model reads finished comparisons instead of calculating them, and the logic is covered by unit tests.

## Run it locally
1. Clone the repo and open the project folder in IntelliJ IDEA.
2. Run `GamecoachApplication`. The fake match data and fake coach are switched on in `src/main/resources/application.properties` (`gamecoach.matches.source=fake`, `gamecoach.coach.provider=fake`).
3. Try these in a browser:
   - Match list: `http://localhost:8080/api/rivals/players/anyname/matches`
   - Analysis: `http://localhost:8080/api/rivals/matches/5517881_1791071770_1288027_11001_11/analysis?username=PlayerOne`
4. Feedback is a POST request, for example in PowerShell:
   `Invoke-RestMethod -Method Post "http://localhost:8080/api/rivals/matches/5517881_1791071770_1288027_11001_11/feedback?username=PlayerOne"`

Run the tests with `./mvnw test` (or `mvnw.cmd test` on Windows).

## Roadmap
- [x] Spring Boot API with fake match data and feedback
- [x] Match analysis (team share, ranks, per-minute stats, hero roles)
- [ ] Claude-backed coaching feedback
- [ ] Scoreboard screenshot upload and parsing
- [ ] Overwatch career stats (OverFast)
- [ ] React frontend
- [ ] Saved match history (database)
- [ ] Authentication and deployment

## Data sources
Marvel Rivals match data comes from scoreboard screenshots uploaded by the user. The app doesn't access game servers or use unofficial APIs.
