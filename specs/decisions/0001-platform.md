# ADR 0001: Platform and stack

Status: **accepted** — 2026-10-19

## Context
Math Game needs to run on phones and tablets. The sibling project Math Monsters is pure native Android (Kotlin, minSdk 26). Math Game is a much larger surface: twelve worlds, five visual tiers, seven engines, rich animation, and a long content pipeline driven by curriculum data. An iOS version is a real possibility later.

## Decision
- **Native Android** is the build target for now. Kotlin, Jetpack Compose, minSdk to be confirmed (Math Monsters uses 26).
- **Specs are platform-neutral.** Every spec describes behavior, data, visuals, and interaction in terms that do not depend on Android. The test: a team (or an agent) holding only `specs/` could generate an exact iOS equivalent (Swift, SwiftUI) without reverse-engineering the Android code.
- Platform-specific choices (Compose idioms, Android storage, notifications, store listing) live in a separate `specs/platform/android/` area, never inside the game, curriculum, or mechanics specs.
- Game logic (curriculum loading, mastery model, star rules, revisit scheduling, secret-goal evaluation) is specified as pure, testable rules so that the same behavior can be ported or shared.

## Consequences
- Specs cannot say "use a LazyColumn"; they must say what the player sees and does.
- Mechanics specs describe input in terms of gestures (tap, drag, pinch, long-press), not Android APIs.
- Visual specs use design tokens (color, type scale, spacing, motion durations) that map to either platform.
- A later iOS build is a second implementation of the same specs, not a port of the Android code.
- The curriculum and mastery model are candidates for a shared, platform-independent module (e.g., Kotlin Multiplatform) if iOS happens; that is a future ADR, not a current commitment.
