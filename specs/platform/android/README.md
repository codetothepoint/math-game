# Platform: Android

Everything Android-specific lives here and nowhere else (ADR 0001). The game, curriculum, mechanics, mastery, map, and portal specs do not depend on anything in this folder.

## Targets
- Kotlin, Jetpack Compose, Material 3 only for primitives (no Material look; the tier spec is the design system).
- minSdk 26, targetSdk current.
- Phones and tablets. Phones are the constraint case: if a level works on a phone in landscape, it works everywhere. Tablets get the side kit tray and may play levels in either orientation.
- Orientation: levels are landscape on phones (locked), either on tablets; map, sheets, and portal adapt to either.
- Offline, no network permission (ADR 0004).

## Modules
```
app/        Android application: navigation, DI, lifecycle, notifications, persistence adapters
core/       Pure Kotlin (ADR 0005): curriculum, mastery, scheduler, generators, engine logic, data model, stats
engines/    Compose rendering and gesture handling for each engine (build, manipulate, …)
identity/   Per-world identities: skins, map art, sound sets, crew lines (one package per world)
ui/         Shared tier chrome: tokens, type, map, site sheet, level frame, portal screens
```
`core` has no Android dependency and no dependency on the other modules. `engines` depends on `core` and `ui`. `identity` depends only on `ui` tokens.

## Content shipped in the app
- `specs/curriculum/generated/curriculum.json` and the recipe files, bundled as assets and validated at build time by the same rules as `tools/curriculum.py` and `tools/recipes.py` (a Gradle task runs them, or core re-implements the checks in a unit test over the bundled assets).
- Fonts (Rubik, Atkinson Hyperlegible) bundled under their open licenses.
- Identity assets as vector drawables where possible; raster only for map paintings.

## Persistence
- One local store per device, versioned, with migrations. A Room database or a DataStore of serialized state both satisfy the spec; pick for migration ergonomics.
- Entities: profiles, node state per profile (the mastery model's `NodeState`), session log, rewards, settings, PIN and recovery hashes (salted, never plain).
- Export: one JSON file per player, self-describing with a schema version, written through the system share sheet or file picker.

## Notifications
- One local notification type: reward unlocked, to the guardian (`specs/parent-portal.md` §5.3). Posted at level end. Respects the portal's toggle and the system permission.

## Input
- Gestures as the engine specs define: drag, long-press, two-finger rotate (with a one-finger rotate handle alternative), pinch (with zoom buttons alternative on `number-line`).
- Haptics through the platform API with the three levels the tier spec names (light, medium, short-medium); honor the system haptics setting.
- Read-aloud through the platform text-to-speech in v1 (`tier-adventure.md` §9), with the crew's lines written to read well aloud.

## Accessibility
- Content descriptions on every site, piece, handle, and control.
- Reduced motion via the system animator duration scale and the tier's crossfade fallback.
- Left-handed mirror as a per-profile setting.
- Minimum touch targets per the tier spec; verified by UI tests.

## Tests
- `core`: unit tests mirroring `tools/test_mastery.py` scenario by scenario, plus the engine-logic cases in each engine spec, plus curriculum and recipe validation over bundled assets.
- `engines`: screenshot tests per kit and instrument in both themes at phone and tablet sizes; gesture tests for snap and detent behavior.
- `app`: a single end-to-end test that creates a profile in world 4, plays one `build` job and one `manipulate` job, and checks the map updated.

## Not decided yet
- Room vs DataStore for the store.
- Whether identity assets use Lottie-style animation for the map's ambient motion or Compose animations only.
- Device support for haptic patterns varies; the fallback is the system default click.
