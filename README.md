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
| `specs/mastery.md` | Mastery model: evidence, stars, availability, revisits |
| `specs/mechanics/engine-*.md` | Engine specs (`build`, `manipulate` so far) |
| `specs/mechanics/recipes/` | Recipes binding nodes to engines, per world |
| `specs/decisions/` | Architecture decision records |
| `tools/curriculum.py` | Validates the skill graph and builds `specs/curriculum/generated/` |
| `tools/recipes.py` | Validates recipes against the curriculum and engine specs; prints coverage |
| `tools/mastery_ref.py`, `tools/test_mastery.py` | Reference implementation of the mastery model and its scenarios |

Run everything: `python3 tools/curriculum.py && python3 tools/recipes.py && python3 tools/test_mastery.py`

## Status
Specs in progress toward a world-4 vertical slice on the `build` and `manipulate` engines. Curriculum graph v1 (344 skills), mastery model v1, two engine specs, and 26 world-4 recipes are complete and validated. No game code yet.

Platform: native Android, with specs written so an iOS equivalent could be generated from them (ADR 0001).
