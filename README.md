# Math Game

A math game that carries a player from 1st grade through 12th grade at their own pace. Every step is a game worth playing; progression reflects real understanding. No tests, no quizzes, no worksheets with a skin.

Working title. Branding comes later.

## Specs
This project is spec-driven. `specs/` is the source of truth.

| Path | What |
|------|------|
| `specs/constitution.md` | Non-negotiable principles |
| `specs/game-design.md` | Game design document |
| `specs/curriculum/` | Skill graph, as data, worlds 1–12 |
| `specs/mechanics/` | Mini-game engines and tier style guides |
| `specs/decisions/` | Architecture decision records |
| `tools/curriculum.py` | Validates the skill graph and builds `specs/curriculum/generated/` |

## Status
Specs in progress. Curriculum graph v1 is complete (344 skills, 12 worlds) and validated. No game code yet.

Platform: native Android, with specs written so an iOS equivalent could be generated from them (ADR 0001).
