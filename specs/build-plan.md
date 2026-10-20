# Build Plan: World 4 Vertical Slice

Status: **v1** — sequences the slice into milestones with gates. Written for Claude Code sessions that start from `CLAUDE.md` and the specs. No time estimates; dependencies and gates only.

## Definition of done
A player profile started in world 4 can: open Canyon Camp → see 28 sites in five regions with correct states → pick a site and a job → play a `build` level and a `manipulate` level generated from the recipes → have signals scored by the mastery model → earn flags, reveal a secret goal, unlock a parent-set chest → return to a map that changed. A guardian can set a PIN, create the profile, set the starting world, create one reward, and read the stats page. Everything offline, nothing faked.

Spec sources, in the order a session should read them: `constitution.md` → `game-design.md` → `mastery.md` → `mechanics/README.md` → `mechanics/engine-build.md` → `mechanics/engine-manipulate.md` → `mechanics/recipes/world-04.yaml` → `mechanics/tier-adventure.md` → `world-map.md` → `parent-portal.md` → `platform/android/README.md` → `decisions/`.

## Rules for every milestone
- Specs are the source of truth. A discrepancy found while building is fixed in the spec and the code in the same change, and `tools/*.py` must still pass.
- `core` stays pure Kotlin (ADR 0005). Anything that imports an Android class does not go in `core`.
- Tests mirror the specs: `core` tests reproduce `tools/test_mastery.py` scenario by scenario with the same numbers, and each engine spec's engine-logic cases by number.
- Placeholder art until M5. Mechanics before skins; the engine fun check is run on grey boxes first.
- One milestone per branch; the gate is what merges it.

## M0 — Scaffold
**Scope:** Gradle multi-module Android project per `platform/android/README.md`; the validators wired into the build.
- [ ] Modules `app`, `core` (Kotlin JVM, no Android), `engines`, `identity`, `ui`; version catalog; Compose BOM; minSdk 26
- [ ] `core` depends on nothing Android; a test proves it (`core` compiles with the Kotlin JVM plugin only)
- [ ] Bundle `specs/curriculum/generated/curriculum.json` and `specs/mechanics/recipes/*.yaml` as `core` test resources and `app` assets; a Gradle task copies them from `specs/` so there is one source
- [ ] CI: `python3 tools/curriculum.py --check`, `python3 tools/recipes.py`, `python3 tools/test_mastery.py`, then `./gradlew :core:test`
- [ ] A dev launcher activity in `app` that lists screens for M3/M4 (no map yet)

**Gate:** `./gradlew :core:test` runs (zero tests) and CI passes.
**Note:** `core` builds and tests without an Android SDK; the rest needs Android Studio on a workstation.

## M1 — Core: curriculum and mastery
**Scope:** `mastery.md` in Kotlin, byte-for-byte on the numbers.
- [ ] Curriculum model and loader from `curriculum.json`; validation (ids, prerequisites, acyclic) as a unit test over the bundled file
- [ ] `NodeState`, `Profile`, `ItemResult`, `LevelResult`, `RevisitState` (`mastery.md` §2–3)
- [ ] Evidence and score update (§4); stars (§5); availability, reach credit, starting world and assumed nodes (§6); revisits, transfer representation, sets (§7); representation rotation helper (§8); milestones (§10); map state (§11)
- [ ] Session log (`parent-portal.md` §9) and the stats computations (`parent-portal.md` §6) as pure functions
- [ ] Tests: the 12 scenarios from `mastery.md` §12 with identical expected values; a property test that availability is monotonic; a test that the world-4 frontier on day one matches scenario 6

**Gate:** all tests green; numbers equal to `tools/test_mastery.py` to three decimals.

## M2 — Core: levels and engine logic
**Scope:** everything an engine needs that does not touch a screen.
- [ ] Recipe model and loader for `world-04.yaml`; validation mirroring `tools/recipes.py`
- [ ] Difficulty bands (`mechanics/README.md`); level assembly: core items + revisit slots + one assumed-node item (`mastery.md` §7.2, §7.5)
- [ ] Generators for all 26 world-4 recipes: each band's parameters produce solvable items within range; property tests per recipe (ranges, denominators, constraint satisfiability, prime counts)
- [ ] `build` satisfaction for every target type in `engine-build.md` §4 and strategy detection for every rule in §7; the six engine-logic cases in §12
- [ ] `manipulate` satisfaction and tolerance for `engine-manipulate.md` §4 and §6, strategy detection for §7; the six cases in §12
- [ ] Secret-goal evaluation for the world-4 recipes' conditions, as pure predicates over an item or level record
- [ ] Hint ladder state machine (three rungs, counts requests)

**Gate:** every recipe generates 1,000 items per band without an invalid one; all engine-logic cases pass.

## M3 — `build` engine on screen
**Scope:** the first vertical thread, then the rest of the kits. Grey-box art.
- [ ] Level frame (`tier-adventure.md` §3.3): target strip, workspace, kit tray, readout, Lock it in; landscape on phone
- [ ] **First thread:** `tiles` kit for `w04.nbt.multiply-multidigit`: stretch, cut, duplicate, remove; live readout in symbolic and visual; lock-in satisfied / not-yet overlays (§6); hints; emits `ItemResult`s and a `LevelResult` into core; a dev-launcher entry plays a full level and prints the resulting `NodeState`
- [ ] Remaining kits used in world 4: `blocks`, `parts`, `strokes`, `marks`, `gears`, `units`, each driven by its recipes; `physical`, `visual`, `symbolic`, `verbal` representations per `engine-build.md` §8
- [ ] Read-aloud on target tap (platform TTS)
- [ ] Screenshot tests per kit at phone and tablet sizes, both themes; gesture tests for snap and cut

**Gate:** all 15 world-4 `build` recipes playable from the dev launcher; the fun check reviewed on grey boxes (a reviewer names the math from the workspace with the target hidden).

## M4 — `manipulate` engine on screen
**Scope:** the five world-4 instruments.
- [ ] `number-line` with pinch zoom and buttons; `fraction-bar` with slicer and shade edge; `grid` with drag-shade and mirror line; `angle` with arms and protractor ring; `place-chart` with digits and hoist lever
- [ ] Detents with haptic tick; tolerance per instrument; not-yet overlays (`engine-manipulate.md` §6); hints
- [ ] All 11 world-4 `manipulate` recipes playable; `match` target with two instruments

**Gate:** all 11 recipes playable; engine-logic cases hold against the rendered instruments (the UI reports the same satisfaction as core).

## M5 — Map, flow, and Canyon Camp
**Scope:** `world-map.md` and the world-4 identity.
- [ ] Profile picker, map, site states as shapes, lanterns, site sheet with jobs, level end sequence, chests, world overview, first-arrival moment
- [ ] Canyon Camp identity: map painting, five regions and site icons, kit and instrument skins replacing grey boxes, ambient and music cues, crew lines, rewards on the map
- [ ] Tier tokens as the design system (`tier-adventure.md` §3) in both themes; motion and sound tables implemented
- [ ] Accessibility: content descriptions, reduced motion, left-handed mirror

**Gate:** the definition-of-done flow runs end to end with placeholder portal data; `world-map.md` §9 acceptance criteria pass on a phone.

## M6 — Parent Portal and persistence
**Scope:** `parent-portal.md`, ADR 0004.
- [ ] Local store with versioned schema and migrations; profiles, node state, session log, rewards, settings, PIN and recovery hashes
- [ ] First launch flow; PIN pad with backoff; recovery phrase
- [ ] Players, starting world (re-runs assumed rule), sound settings
- [ ] Rewards: create on each milestone type, lifecycle, the single notification
- [ ] Stats pages (§6) from core's pure functions
- [ ] Export to JSON

**Gate:** `parent-portal.md` §9 acceptance criteria; a cold start with an exported file restored matches the original state.

## M7 — Polish and playtest
- [ ] Sound and haptic pass against the tier tables; TTS lines read well
- [ ] Performance on a mid-range phone: a 99 × 29 tile rectangle at 60 fps
- [ ] Playtest checklist from the constitution: no tests, no scores shown, fun with stars off, sophistication fits world 4
- [ ] Open questions in each spec resolved or deferred explicitly

**Gate:** two play sessions by the target age without the builder present, with notes filed as spec changes.

## After the slice
Specs for `story`, `fluency`, `strategy`, `sort`, `sim` on the same template; the remaining seven world-4 nodes; worlds 3 and 5 identities; then the recipe pass for worlds 1–3 and 5–8.
