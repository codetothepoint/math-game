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

## Delegation and models
Design reasoning is expensive and rare; implementation against a settled spec is cheap and frequent. Match the model to the kind of thinking the task needs, not to its size.

| Kind of work | Who does it | Model |
|--------------|-------------|-------|
| Game design, new specs, resolving a spec ambiguity, ADR decisions, curriculum authoring, anything where the answer is not yet in `specs/` | The main session, in conversation with the user | Fable (the session's own model) |
| Adversarial review of a branch before merge | `adversarial-reviewer` subagent | Opus |
| Implementing a settled spec section into code with tests; porting a reference implementation; writing generators or engine logic from a spec; fixing a reviewed finding that has a spec citation | `coder` subagent | Sonnet |
| Renames, generated-file sync, mirroring a change across two files, formatting, running validators and summarizing output, triaging a log or a stack trace, bulk mechanical edits | `chore` subagent | Haiku |
| Finding where something lives in the codebase; reading across many files to answer one question | `Explore` subagent | Haiku |

Rules:
- Before delegating implementation, the spec section must exist and be settled. A `coder` that hits an ambiguity stops and sends the question back; it does not decide. If you find yourself writing design into a coder's brief, the spec is not done: finish the spec first, in the main session.
- Brief a subagent with the spec sections and files to read, the gate to hit, and the commands to run. Not with your own conclusions about the code.
- The main session reviews what comes back (tests green, gate met, questions answered) and keeps the conversation with the user. It does not re-do the subagent's work on Fable.
- A reviewer is never the model that wrote the code in the same session, and is never briefed with the author's reasoning.
- When several coder tasks are independent (two engines, two kits), run them in parallel on separate branches or worktrees.

## Working agreements
- One milestone per branch; the milestone's gate is the merge criterion.
- **Every PR gets an adversarial review before merge.** Run `/review` (it spawns the `adversarial-reviewer` subagent in `.claude/agents/`). Every finding is resolved in the PR's `## Review` section: fixed in a named commit with a test, or declined with a spec citation. A PR with an unresolved finding does not merge. The reviewer reads the code cold; never brief it with your own conclusions.
- Commit messages name the spec sections implemented (e.g. `core: mastery §5 stars, §7 revisits`).
- Keep `specs/build-plan.md` checkboxes current in the same commit as the work.
- Do not add time estimates, streaks, daily goals, or notifications to the player; the constitution and `specs/world-map.md` §8 forbid them.

## Environment notes
- `core` builds and tests with a JDK and Gradle alone. The Android modules need the Android SDK (Android Studio on a workstation).
- Python 3 with `pyyaml` and `jsonschema` runs the validators.
