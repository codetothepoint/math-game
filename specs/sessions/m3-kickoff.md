# Session plan: M3 kickoff — the first vertical thread

For a Claude Code session in Android Studio on a machine with the Android SDK. Goal: the first playable `build` level on a phone, end to end, with grey-box art. Everything it needs to decide correctness already lives in `core` (M2); this session draws, listens, and wires.

Read first: `CLAUDE.md`, `specs/build-plan.md` (M3), `specs/mechanics/engine-build.md` §2, §3 (`tiles`), §5, §6, §10 (`w04.nbt.multiply-multidigit`), `specs/mechanics/tier-adventure.md` §3.3–§3.5, `specs/platform/android/README.md`.

## Step 0 — Prove the Android build
- `./gradlew :core:test` green locally (first run of the real Gradle path; `tools/core-check.sh` was the sandbox substitute).
- `./gradlew :app:assembleDebug`. The Android module build files were written without an SDK present; fix whatever AGP complains about, keep the module boundaries in ADR 0005.
- Run the dev launcher on a phone or emulator in landscape; it should print "Curriculum loaded: 344 skills, 28 in world 4".

## Step 1 — Tier tokens (grey-box subset)
In `ui`: a `Tokens` object from `tier-adventure.md` §3.1–§3.2 (both themes), type scale, spacing rhythm, motion durations. Skins stay grey: pieces are flat rectangles in `ink` at 20% on `surface`. Fonts bundled (Rubik, Atkinson Hyperlegible).

## Step 2 — Level frame
In `ui`: a `LevelFrame` composable per `tier-adventure.md` §3.3: target strip (text, read-aloud button, hint button), workspace slot, kit tray (bottom on phones), readout dock, **Lock it in** in the right thumb zone, left-handed mirror as a parameter. Landscape-locked activity for levels. No logic.

## Step 3 — The `tiles` kit for `product`
In `engines`:
- `TilesWorkspace`: a square grid; drag from the tray places a tile; a corner handle stretches it to a `TileRect`; drag across a group draws a cut that snaps to grid lines; long-press duplicates; drag-off removes. Emits a `TilesConstruction` (core type) on every change.
- Readout: for a `Product` target, `w × h` while whole; `(20 + 3) × (10 + 4)` with the four region labels once cut (the labels come from `core` place-value cuts: `placeValueCuts(n)`).
- Lock it in: call `BuildEngine.satisfied(target, construction)`. Not satisfied → the target outline overlays the workspace and the differing region pulses (`attention`), the item stays open, `firstTry = false`. Satisfied → settle animation, next item.
- Hints: `HintLadder(recipe.hints keys in order)`; rung 1 draws the place-value lines, rung 2 fills the tens-by-tens region, rung 3 shows the full model faintly.
- Strategy: at lock-in, `BuildEngine.detectStrategy(target, construction, recipe.strategies.values)` then map through `recipe.strategyForRule` into `ItemResult.strategy`.

## Step 4 — Wire the loop
In `app`:
- Build a profile with `MasteryModel.newProfile(4)` (in memory for now; persistence is M6).
- `LevelBuilder.build(profile, "w04.nbt.multiply-multidigit", Engine.BUILD, now, rng)` → `LevelPlan`.
- Play the plan's items through the `tiles` workspace; collect `ItemResult`s (`responseMs` from target shown to satisfying lock-in; `kind` from the plan).
- On the last item: `LevelResult` with `observesStrategies` from the plan and `secretGoalsHit = SecretGoals.achieved(recipe.secretGoals.map { it.id }, records)`; `model.finishLevel(profile, result)`.
- Dev launcher shows the resulting `NodeState` (score, stars, repItems, strategies) after the level. That printout is the M3 first-thread gate.

## Step 5 — Tests for this thread
- Screenshot test: `TilesWorkspace` with a 23 × 14 rectangle cut at 20 | 3 and 10 | 4, phone landscape, both themes.
- Gesture test: a cut gesture at x = 19.6 snaps to 20; at 20.4 snaps to 20; a stretch to (23, 14) yields `TileRect(23, 14)`.
- End-to-end: eight items answered through the UI produce eight `ItemResult`s and one `LevelResult` whose numbers match what `MasteryModel` computes in a pure test with the same inputs.

## Then
The other six kits (`blocks`, `parts`, `strokes`, `marks`, `gears`, `units`), each driven by its recipes and `Solutions.forBuild` for the third hint rung. Run `/review` before the M3 PR.

## Don't
- Don't compute correctness, strategy, or score in Compose. If a composable needs to know whether something is right, it calls `core`.
- Don't add identity art yet (M5). Don't show scores, timers, or percentages anywhere a player sees.
