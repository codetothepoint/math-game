# Math Game — instructions for Claude Code

This repository is spec-driven. `specs/` is the source of truth; code implements it and never contradicts it silently.

## Start here, every session
1. Read `specs/constitution.md`. Every change is checked against its eleven principles.
2. Read `specs/build-plan.md` and find the current milestone. Read the specs it lists, in order.
3. Run the validators before and after any change to `specs/`:
   ```
   python3 tools/curriculum.py && python3 tools/recipes.py && python3 tools/test_mastery.py
   ```

## How the specs fit
- `specs/curriculum/` — the skill graph as data (344 nodes, worlds 1–12). Edit the YAML, never `generated/`.
- `specs/mastery.md` — the rules for evidence, stars, availability, revisits. `tools/mastery_ref.py` executes them; Kotlin `core` must agree with it number for number.
- `specs/mechanics/` — engines (`engine-*.md`), tiers (`tier-*.md`), and recipes (`recipes/world-NN.yaml`) that bind a node to an engine.
- `specs/world-map.md`, `specs/parent-portal.md` — the screens outside a level.
- `specs/platform/android/` — the only place Android specifics are allowed.
- `specs/decisions/` — ADRs. Read 0001 (platform-neutral specs), 0003 (identities), 0005 (pure core) before touching module boundaries.

## Rules
- When code and spec disagree, fix the spec and the code in the same change, say which one was wrong, and keep `tools/*.py` passing.
- `core/` is pure Kotlin. No Android imports. If you need one, you are in the wrong module.
- Engines never score; they emit `ItemResult` and `LevelResult` (`specs/mastery.md` §3). The mastery model in `core` scores.
- Engines and recipes never reference an identity's assets; they reference slots (`specs/mechanics/tier-adventure.md` §2).
- Nothing the player sees shows a score, percentage, timer, or the word "test". The Parent Portal is the only place with numbers, and it shows the ones `specs/parent-portal.md` §6 allows.
- Copy follows `specs/mechanics/tier-adventure.md` §3.6: jobs, not questions; "not yet", never "wrong".
- Placeholder art until milestone M5. Mechanics first.
- Tests mirror specs: scenario numbers from `specs/mastery.md` §12 and engine-logic cases from each engine spec §12 are reproduced exactly.

## Working agreements
- One milestone per branch; the milestone's gate is the merge criterion.
- Commit messages name the spec sections implemented (e.g. `core: mastery §5 stars, §7 revisits`).
- Keep `specs/build-plan.md` checkboxes current in the same commit as the work.
- Do not add time estimates, streaks, daily goals, or notifications to the player; the constitution and `specs/world-map.md` §8 forbid them.

## Environment notes
- `core` builds and tests with a JDK and Gradle alone. The Android modules need the Android SDK (Android Studio on a workstation).
- Python 3 with `pyyaml` and `jsonschema` runs the validators.
