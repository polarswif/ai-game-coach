# AI Game Coach

An AI coaching app for competitive games. Players upload a screenshot of the end-of-match scoreboard and get strengths, weaknesses, and advice for their next match.

**Status:** Early development. The backend API works end to end with built-in match data, computes a real statistical analysis of each match, and can coach with Claude (or with free canned feedback).

## Stack
- **Backend:** Java 17, Spring Boot 4.1.1, Maven
- **Tests:** JUnit 5, run on every push with GitHub Actions
- **Frontend:** React (planned)
- **AI:** Claude API (Messages API over Spring's `RestClient`)

## How it works
1. **Match data** comes from a `MatchHistorySource`. Today that's three built-in matches typed in from real Marvel Rivals in-game scoreboards: role icon, last hero played, equipped team-up, Proficiency Badge, medals, K/D/A, final hits, damage, blocked, healing, accuracy, and MVP/SVP for every row.
2. **`MatchAnalyzer`** computes the numbers in Java: KDA, per-minute stats, each player's share of their team's totals, and their rank on their team and in the lobby. Each hero's role comes from a table in `src/main/resources/heroes/`, with the row's role icon filling the gaps (such as Deadpool, who can play any role). It also reports both teams' compositions, and works out each equipped team-up from a table of all 108 team-ups: whether it was enhanced, and whether the hero's other team-up would have been enhanced instead. It carries each player's medals (most KOs, damage, blocked, healing or assists in the lobby) and best multi-kill. When a row shows a Lord or animated Lord portrait or a Proficiency Badge (the emblem on the portrait's corner), it records the minimum proficiency level that proves on the hero (players can leave these unequipped, so a plain row proves nothing).
3. **A `CoachingService`** turns the match and that analysis into strengths, weaknesses, and advice. `ClaudeCoachingService` sends the analysis and scoreboard to Claude as labeled text and makes Claude answer through a `submit_coaching` tool whose input matches the `Feedback` record, so the reply always has the same shape. Results are cached per match and player. `FakeCoachingService` returns canned text for free.

Doing the math in Java means the coaching model reads finished comparisons instead of calculating them, and the logic is covered by unit tests.

## Run it locally
1. Clone the repo and open the project folder in IntelliJ IDEA.
2. Run `GamecoachApplication`. The fake match data and fake coach are switched on in `src/main/resources/application.properties` (`gamecoach.matches.source=fake`, `gamecoach.coach.provider=fake`).
3. Try these in a browser:
   - Match list: `http://localhost:8080/api/rivals/players/anyname/matches`
   - Analysis of the uploader (the highlighted row): `http://localhost:8080/api/rivals/matches/10378920354/analysis`
   - Analysis of another player: `http://localhost:8080/api/rivals/matches/10378920354/analysis?username=player44`
4. Feedback is a POST request, for example in PowerShell:
   `Invoke-RestMethod -Method Post "http://localhost:8080/api/rivals/matches/10378920354/feedback"`

Run the tests with `./mvnw test` (or `mvnw.cmd test` on Windows). The tests never call the real API, so they need no key.

## Use the Claude coach
1. Create an API key in the [Claude Console](https://platform.claude.com/) and set a monthly spend limit there.
2. Put the key in the `ANTHROPIC_API_KEY` environment variable. Never paste it into `application.properties` or any file in the repo.
   - IntelliJ: Run > Edit Configurations > `GamecoachApplication` > Environment variables: `ANTHROPIC_API_KEY=your-key`
   - PowerShell (current window only): `$env:ANTHROPIC_API_KEY="your-key"`
3. Change `gamecoach.coach.provider=fake` to `gamecoach.coach.provider=claude` in `application.properties`.
4. Run the app and send the feedback request from step 4 above.

The model (`gamecoach.claude.model`, default `claude-sonnet-5-5`) and the reply length cap (`gamecoach.claude.max-tokens`) are in the same file. The app logs the tokens each coaching request used. If the provider is `claude` and the key is missing, the app stops at startup with a message saying so.

## Roadmap
- [x] Spring Boot API with fake match data and feedback
- [x] Match analysis (team share, ranks, per-minute stats, hero roles)
- [x] Model matches the in-game scoreboard (role icons, final hits, MVP/SVP, hidden names)
- [x] Team compositions, Season 9 team-ups, medals and proficiency in the analysis
- [x] Claude-backed coaching feedback
- [ ] Scoreboard screenshot upload and parsing (in-game first, tracker.gg as an optional extra)
- [ ] Overwatch career stats (OverFast)
- [ ] React frontend
- [ ] Saved match history (database)
- [ ] Authentication and deployment

## Data sources
Marvel Rivals match data comes from end-of-match scoreboard screenshots uploaded by the user. The app doesn't access game servers or use unofficial APIs.
