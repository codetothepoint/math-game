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
| `specs/world-map.md` | Player flow, map, site states, level end, chests |
| `specs/parent-portal.md` | PIN, setup basics, rewards, stats |
| `specs/mechanics/engine-*.md` | Engine specs (`build`, `manipulate` so far) |
| `specs/mechanics/tier-*.md` | Tier style guides (`adventure` so far, with world 4's identity) |
| `specs/mechanics/recipes/` | Recipes binding nodes to engines, per world |
| `specs/platform/android/` | Everything Android-specific |
| `specs/decisions/` | Architecture decision records |
| `tools/curriculum.py` | Validates the skill graph and builds `specs/curriculum/generated/` |
| `tools/recipes.py` | Validates recipes against the curriculum and engine specs; prints coverage |
| `tools/mastery_ref.py`, `tools/test_mastery.py` | Reference implementation of the mastery model and its scenarios |

Run everything: `python3 tools/curriculum.py && python3 tools/recipes.py && python3 tools/test_mastery.py`

## Status
Specs for the world-4 vertical slice are complete: curriculum graph v1 (344 skills), mastery model v1 with a reference implementation, `build` and `manipulate` engine specs, 26 world-4 recipes, the Adventure tier guide with world 4's identity (Canyon Camp), the world map and player flow, the Parent Portal, and the platform decisions. No game code yet; next is the build plan.

Platform: native Android, with specs written so an iOS equivalent could be generated from them (ADR 0001).
