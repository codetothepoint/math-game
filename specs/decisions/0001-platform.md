# ADR 0001: Platform and stack

Status: **proposed** — decision pending.

## Context
Math Game needs to run on phones and tablets. The sibling project Math Monsters is pure native Android (Kotlin, minSdk 26) with an iOS version parked for later. Math Game is a much larger surface: twelve worlds, five visual tiers, seven engines, rich animation, and a long content pipeline driven by curriculum data.

## Options
1. **Native Android** (Kotlin, Jetpack Compose) — same stack as Math Monsters; iOS later as a second codebase.
2. **Kotlin Multiplatform + Compose Multiplatform** — shared logic and UI across Android and iOS from one codebase.
3. **Game engine** (Godot, Unity, Flutter + Flame) — purpose-built for animation-heavy, physics-y mini-games; exports to Android and iOS.
4. **Web** (TypeScript, canvas/WebGL) wrapped for stores — widest reach, easiest content iteration.

## Decision
Not yet made.

## Consequences
TBD.
