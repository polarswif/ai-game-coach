# AI Game Coach

An AI coaching app for competitive games. Players upload a match scoreboard and get strengths, weaknesses, and advice for their next match.

**Status:** Early development. Backend API with fake data and fake feedback is working.

## Stack
- **Backend:** Java 17, Spring Boot 4.1.1, Maven
- **Frontend:** React (planned)
- **AI:** Claude API (planned)

## Run it locally
1. Clone the repo and open the `gamecoach` folder in IntelliJ IDEA.
2. Make sure `src/main/resources/application.properties` contains `spring.profiles.active=dev`.
3. Run `GamecoachApplication`.
4. Try: `http://localhost:8080/api/rivals/players/anyname/matches`

## Roadmap
- [x] Spring Boot API with fake match data and feedback
- [ ] Claude-backed coaching feedback
- [ ] Scoreboard screenshot upload and parsing
- [ ] Overwatch career stats (OverFast)
- [ ] React frontend
- [ ] Saved match history (database)
- [ ] Authentication and deployment

## Data sources
Marvel Rivals match data comes from scoreboard screenshots uploaded by the user. The app doesn't access game servers or use unofficial APIs.