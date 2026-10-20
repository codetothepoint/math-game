# ADR 0005: A pure game core, separate from the Android UI

Status: **accepted** — 2026-10-19

## Context
The specs describe the curriculum, the mastery model, the scheduler, level generation, and engine logic as pure rules (`specs/mastery.md`, the engine specs). `tools/mastery_ref.py` proves they run without a platform. ADR 0001 requires that an iOS equivalent could be generated from the specs. The cheapest way to honor both is to keep those rules out of the UI layer entirely.

## Decision
- The app is built as a **core** module and a **UI** layer.
- **core** is pure Kotlin with no Android dependencies: curriculum loading and validation, the mastery model, availability and map state, the revisit scheduler, difficulty bands, level generators per recipe, engine satisfaction and strategy-detection logic, milestones, and the data model. It is unit-tested against the same scenarios as `tools/test_mastery.py`, and the engine-logic test cases in each engine spec, with identical expected numbers.
- **UI** is Jetpack Compose: screens, gestures, rendering of kits and instruments, identities, sound, haptics, persistence adapters, notifications. The UI calls core; core never calls the UI.
- The boundary is the signals in `specs/mastery.md` §3 going in (`ItemResult`, `LevelResult`) and generated levels, map state, and stats coming out.

## Consequences
- An iOS build would reuse core as a Kotlin Multiplatform module or re-implement it from the same specs and tests; either way the UI is the only port.
- Engine *behavior* (what satisfies a target, which strategy was used) is testable without a device; engine *feel* (snap timing, haptics) is tested on devices.
- The Python reference implementation stays as the executable spec; core's tests must agree with it number for number, and a disagreement is a spec bug to fix in both.
