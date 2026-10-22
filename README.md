# AI Game Coach

An AI coaching app for competitive games. Players upload a screenshot of the end-of-match scoreboard and get strengths, weaknesses, and advice for their next match.

**Status:** Early development. The backend API works end to end with built-in match data, computes a real statistical analysis of each match, and can coach with Claude (or with free canned feedback).

## Stack
- **Backend:** Java 17, Spring Boot 4.1.1, Maven
- **Tests:** JUnit 5, run on every push with GitHub Actions
- **Frontend:** React (planned)
- **AI:** Claude API (Messages API over Spring's `RestClient`)

## How it works
1. **Match data** comes from an uploaded scoreboard screenshot, read by Claude's vision (`ClaudeScoreboardParser`) and checked by `ScoreboardValidator`, or from a `MatchHistorySource`. The built-in source has three matches typed in from real Marvel Rivals in-game scoreboards: role icon, last hero played, equipped team-up, Proficiency Badge, medals, K/D/A, final hits, damage, blocked, healing, accuracy, and MVP/SVP for every row.
2. **`MatchAnalyzer`** computes the numbers in Java: KDA, per-minute stats, each player's share of their team's totals, and their rank on their team and in the lobby. Each hero's role comes from a table in `src/main/resources/heroes/`, with the row's role icon filling the gaps (such as Deadpool, who can play any role). It also reports both teams' compositions, and works out each equipped team-up from a table of all 108 team-ups: whether it was enhanced, and whether the hero's other team-up would have been enhanced instead. It carries each player's medals (most KOs, damage, blocked, healing or assists in the lobby) and best multi-kill. When a row shows a Lord or animated Lord portrait or a Proficiency Badge (the emblem on the portrait's corner), it records the minimum proficiency level that proves on the hero (players can leave these unequipped, so a plain row proves nothing).
   It also rates how close the match was (from the round score when known, otherwise from each team's deaths), because a stomp says less about a player than a hard-fought match.
3. **A `CoachingService`** turns the match and that analysis into strengths, weaknesses, advice, and a note on how much weight the coaching deserves. `ClaudeCoachingService` sends the analysis, the scoreboard, and player-checked hero notes (`src/main/resources/heroes/marvel-rivals-hero-notes.txt`) to Claude as labeled text, and Claude answers through a `submit_coaching` tool whose input matches the `Feedback` record, so the reply always has the same shape. Results are cached per match and player. `FakeCoachingService` returns canned text for free.

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

### Read a screenshot
With the Claude provider on, upload an end-of-match scoreboard (PNG, JPEG, WebP or GIF, up to 5 MB). In PowerShell:

```powershell
curl.exe -s -F "screenshot=@C:\path\to\scoreboard.png" -F "score=3-2" -F "hero=Daredevil" http://localhost:8080/api/rivals/matches
```

`score` and `hero` are optional. `score` is the round score from the match history screen, your team's rounds first. `hero` is the hero you played: the reader reads stats very reliably but can mistake one small hero portrait for another, and your own hero is what coaching depends on most. One screenshot costs about 6 cents with Sonnet 5.5 (roughly 7,700 input and 4,200 output tokens). The reply holds the match as read (with a `matchId` like `upload-1a2b3c4d`) and a list of `warnings`, things to check such as a medal that doesn't match the lobby leader or a team-up that doesn't exist. Fix mistakes by sending the corrected match back with `PUT /api/rivals/matches/{matchId}`, then ask for feedback with that id as usual. Uploaded matches live in memory until the app restarts.

The model (`gamecoach.claude.model`, default `claude-sonnet-5-5`) and the reply length cap (`gamecoach.claude.max-tokens`) are in the same file. The app logs the tokens each coaching request used. If the provider is `claude` and the key is missing, the app stops at startup with a message saying so.

## Overwatch match coaching
The same flow as Rivals, from the end-of-match report (the screen after a match, or the match in your history). Take up to three kinds of screenshot:
- **Teams** (required): the scoreboard with all 10 players, E / A / D / damage / healing / mitigation.
- **Summary** (recommended): result, final score, map, mode, game length, and your heroes by name. Without it there's no result or per-10 stats.
- **Personal** (optional, one per hero tab): hero-specific stats such as weapon accuracy or ability kills.

```
curl -F "teams=@teams.png" -F "summary=@summary.png" -F "personal=@personal.png" http://localhost:8080/api/overwatch/matches
```

All screens go to Claude in one request, each labeled, so they can be cross-checked: the Summary's totals pick out your row and name your hero in text, so `hero` (the hero you ended on) is only needed if the reader still gets it wrong. Review the warnings, fix mistakes with `PUT /api/overwatch/matches/{matchId}`, then:
- `GET /api/overwatch/matches/{matchId}/analysis`: per-10 rates, team share, ranks, and the best enemy in your role (tank against tank).
- `POST /api/overwatch/matches/{matchId}/feedback`: coaching, in the same strengths / weaknesses / advice shape as Rivals.

Coaching includes hero notes for the heroes you played (`overwatch-hero-notes.txt`): researched descriptions of how each hero plays now and how to read their stats, from the Overwatch Wiki and community guides, in our own words. Add `?username=NAME` to either to analyze someone else in the match. Close-match judgment uses the final score, except in Push, where the score only says who pushed further, so deaths are used instead. Matches live in memory until the app restarts.

## Overwatch career stats
Overwatch doesn't publish individual matches through any API, so besides screenshots the app can read a player's career: their public Blizzard profile, read through [OverFast API](https://github.com/TeKrop/overfast-api) (free, no key). `OverFastCareerSource` fetches ranks and stats and maps them into `CareerProfile`; `CareerAnalyzer` then compares each hero with the same player's other heroes in that role (per 10 minutes, and win rate), since without matches the player's own other heroes are the fairest benchmark. Heroes under an hour or 5 games are flagged as small samples.

Search first. Blizzard no longer shows the number after the `#` in search, so several players can share a name; each result carries a `playerId`, plus the title, avatar and last-update time that tell them apart. Use the `playerId` (or a BattleTag with a dash instead of `#`; names are case-sensitive) in the other URLs:
- Search: `http://localhost:8080/api/overwatch/players?name=Swift`
- Career as reported: `http://localhost:8080/api/overwatch/players/{playerId}/profile`
- Analysis: `http://localhost:8080/api/overwatch/players/{playerId}/analysis?mode=competitive&platform=pc`

In the analysis, a percent difference is `null` when it can't be compared fairly: the other heroes' rate is too small to divide by, or the hero has no assists at all (Blizzard only counts assists for some heroes).

`mode` is `quickplay` (default) or `competitive`; `platform` is `pc` or `console`, or left out to combine both. The profile must be public (Overwatch: Social options, Career Profile Visibility). The public OverFast instance limits requests per second and caches each player for an hour; for heavy use, self-host it and set `gamecoach.overfast.base-url`.

## Roadmap
- [x] Spring Boot API with fake match data and feedback
- [x] Match analysis (team share, ranks, per-minute stats, hero roles)
- [x] Model matches the in-game scoreboard (role icons, final hits, MVP/SVP, hidden names)
- [x] Team compositions, Season 9 team-ups, medals and proficiency in the analysis
- [x] Claude-backed coaching feedback
- [x] Scoreboard screenshot upload and parsing, with consistency checks and a review step
- [ ] Match history screenshot (round score) and tracker.gg as optional extras
- [x] Overwatch career stats and analysis (OverFast)
- [x] Overwatch match coaching from match report screenshots
- [x] Overwatch hero notes (researched; Sombra, Roadhog and Doctrine still to write)
- [ ] React frontend
- [ ] Saved match history (database)
- [ ] Authentication and deployment

## Data sources
Marvel Rivals and Overwatch match data come from end-of-match screenshots uploaded by the user. The app doesn't access game servers. Overwatch career stats come from public Blizzard career profiles via OverFast API; the Overwatch hero table (`overwatch-heroes.csv`) is taken from OverFast's data (MIT license).
