/**
 * A real Overwatch match (names hidden) with the analysis and coaching the app produced, shown at
 * /sample so visitors without an invite can see what the coach does. No API calls, so it's free.
 */
export const SAMPLE = {
  "gameId": "overwatch",
  "match": {
    "matchId": "sample",
    "mode": "Push",
    "map": "Colosseo",
    "duration": "PT5M30S",
    "result": "VICTORY",
    "score": {
      "teamA": 1,
      "teamB": 0
    },
    "players": [
      {
        "name": "You",
        "title": null,
        "team": "A",
        "role": "TANK",
        "hero": "Ramattra",
        "eliminations": 21,
        "assists": 0,
        "deaths": 1,
        "damage": 7013,
        "healing": 105,
        "mitigation": 4405,
        "uploader": true
      },
      {
        "name": "Teammate 1",
        "title": null,
        "team": "A",
        "role": "DAMAGE",
        "hero": "Symmetra",
        "eliminations": 10,
        "assists": 2,
        "deaths": 2,
        "damage": 2491,
        "healing": 50,
        "mitigation": 506,
        "uploader": false
      },
      {
        "name": "Teammate 2",
        "title": null,
        "team": "A",
        "role": "DAMAGE",
        "hero": "Sombra",
        "eliminations": 13,
        "assists": 3,
        "deaths": 2,
        "damage": 5104,
        "healing": 0,
        "mitigation": 0,
        "uploader": false
      },
      {
        "name": "Teammate 3",
        "title": null,
        "team": "A",
        "role": "SUPPORT",
        "hero": "Zenyatta",
        "eliminations": 15,
        "assists": 9,
        "deaths": 1,
        "damage": 5109,
        "healing": 2805,
        "mitigation": 0,
        "uploader": false
      },
      {
        "name": "Teammate 4",
        "title": null,
        "team": "A",
        "role": "SUPPORT",
        "hero": "Baptiste",
        "eliminations": 5,
        "assists": 15,
        "deaths": 1,
        "damage": 2105,
        "healing": 5723,
        "mitigation": 0,
        "uploader": false
      },
      {
        "name": "Enemy 1",
        "title": null,
        "team": "B",
        "role": "TANK",
        "hero": "Roadhog",
        "eliminations": 3,
        "assists": 0,
        "deaths": 6,
        "damage": 4337,
        "healing": 1123,
        "mitigation": 3829,
        "uploader": false
      },
      {
        "name": "Enemy 2",
        "title": null,
        "team": "B",
        "role": "DAMAGE",
        "hero": "Ashe",
        "eliminations": 5,
        "assists": 2,
        "deaths": 5,
        "damage": 3518,
        "healing": 0,
        "mitigation": 0,
        "uploader": false
      },
      {
        "name": "Enemy 3",
        "title": null,
        "team": "B",
        "role": "DAMAGE",
        "hero": "Mei",
        "eliminations": 3,
        "assists": 1,
        "deaths": 6,
        "damage": 3045,
        "healing": 347,
        "mitigation": 2322,
        "uploader": false
      },
      {
        "name": "Enemy 4",
        "title": null,
        "team": "B",
        "role": "SUPPORT",
        "hero": "Juno",
        "eliminations": 4,
        "assists": 3,
        "deaths": 4,
        "damage": 1570,
        "healing": 5399,
        "mitigation": 344,
        "uploader": false
      },
      {
        "name": "Enemy 5",
        "title": null,
        "team": "B",
        "role": "SUPPORT",
        "hero": "Ana",
        "eliminations": 4,
        "assists": 4,
        "deaths": 4,
        "damage": 1985,
        "healing": 4365,
        "mitigation": 909,
        "uploader": false
      }
    ],
    "yourHeroes": [
      {
        "hero": "Ramattra",
        "percentPlayed": 100.0,
        "playTime": "PT5M29S"
      }
    ],
    "heroStats": []
  },
  "analysis": {
    "player": "You",
    "team": "A",
    "role": "TANK",
    "hero": "Ramattra",
    "outcome": "WIN",
    "teamSize": 5,
    "lobbySize": 10,
    "teamComposition": "1-2-2",
    "enemyComposition": "1-2-2",
    "heroesPlayed": [],
    "eliminationsPerDeath": 21.0,
    "stats": {
      "ELIMINATIONS": {
        "value": 21,
        "per10": 38.2,
        "teamSharePercent": 32.8,
        "teamRank": 1,
        "lobbyRank": 1,
        "sameRoleEnemyBest": 3
      },
      "ASSISTS": {
        "value": 0,
        "per10": 0.0,
        "teamSharePercent": 0.0,
        "teamRank": 5,
        "lobbyRank": 9,
        "sameRoleEnemyBest": 0
      },
      "DEATHS": {
        "value": 1,
        "per10": 1.8,
        "teamSharePercent": 14.3,
        "teamRank": 1,
        "lobbyRank": 1,
        "sameRoleEnemyBest": 6
      },
      "DAMAGE": {
        "value": 7013,
        "per10": 12750.9,
        "teamSharePercent": 32.1,
        "teamRank": 1,
        "lobbyRank": 1,
        "sameRoleEnemyBest": 4337
      },
      "HEALING": {
        "value": 105,
        "per10": 190.9,
        "teamSharePercent": 1.2,
        "teamRank": 3,
        "lobbyRank": 7,
        "sameRoleEnemyBest": 1123
      },
      "MITIGATION": {
        "value": 4405,
        "per10": 8009.1,
        "teamSharePercent": 89.7,
        "teamRank": 1,
        "lobbyRank": 1,
        "sameRoleEnemyBest": 3829
      }
    },
    "heroStats": [],
    "closeness": {
      "level": "ONE_SIDED",
      "roundsWon": null,
      "roundsLost": null,
      "teamAheadInFights": true,
      "teamDeaths": 7,
      "enemyDeaths": 25,
      "deathRatio": 3.57,
      "damageSharePercent": 60.2,
      "healingSharePercent": 43.6
    }
  },
  "feedback": {
    "matchContext": "A one-sided win: the enemy died 25 times to your team's 7. Your numbers are strong, but a weak opponent inflates them, so give this coaching moderate weight.",
    "strengths": [
      "You stayed alive: 1 death, the fewest in the lobby, against 6 for the enemy Roadhog. As the only tank, that kept your front line up all match.",
      "You were the top pressure source: 21 eliminations and 7,013 hero damage, both rank 1 of 10. The Roadhog managed 3 and 4,337.",
      "You did nearly all your team's blocking: 4,405 mitigated, 89.7% of the team total and ahead of the Roadhog's 3,829."
    ],
    "weaknesses": [
      "0 assists, the fewest on your team. Ramattra's Ravenous Vortex and Pummel set up kills for others; check whether you used them only to finish targets yourself."
    ],
    "advice": [
      "Before pushing forward, check you still have an exit. Against a stronger team, one death in Nemesis Form costs much more.",
      "Use Ravenous Vortex to pull fliers down and slow targets for your damage players, then see if assists go above 0.",
      "When a teammate gets focused, drop Void Barrier between them and the damage."
    ]
  }
};
