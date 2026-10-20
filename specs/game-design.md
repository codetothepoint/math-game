# Game Design Document

Status: **draft** — captures the design conversation so far. Open questions are listed at the end.

## Vision
A math game that carries a player from 1st grade through 12th grade at their own pace, where every step is a game worth playing and progression reflects actual understanding. No tests, no quizzes, no worksheets with a skin.

## Core loop
1. Player picks a skill node available to them in their current world (or any earlier world).
2. Player plays a game built for that skill. The game is fun on its own terms.
3. The game tracks what the player did: accuracy, fluency, hint use, strategies used, representations handled.
4. Stars, secret goals, and skill-graph unlocks update from that tracking.
5. Later sessions quietly revisit earlier skills inside new games (interleaved review). Mastery that holds earns the 3rd star; mastery that slips dims it.

## Worlds
- Twelve worlds, one per grade, 1st–12th.
- A parent or teacher sets the starting world. The player can always travel back to earlier worlds; forward travel is through the skill graph.
- Each world has its own visual identity, tone, music, and UI density. Worlds mature with the player — see *Sophistication tiers*.
- Worlds are a presentation layer over the skill graph. A skill belongs to a world, but prerequisites can cross world boundaries.

## Skill graph
- The curriculum is a directed acyclic graph of skills. See `specs/curriculum/`.
- A skill node unlocks when its prerequisites reach a mastery threshold (not completion).
- Multiple branches can be open at once: a player can run ahead on fractions while still working on measurement.
- Skipping ahead is just playing: if a player wants to try a node whose prerequisites are not yet mastered, they can attempt it; the game adjusts and the prerequisites get credit from what the player demonstrates. No "challenge test".

## Stars
Three stars per skill node, modeled on Angry Birds, but the 3rd star cannot be earned in a single sitting.

| Star | Meaning | Earned by |
|------|---------|-----------|
| ★ | Played it | Completed the game for this skill |
| ★★ | Got it | Accuracy threshold met, strategy shown, modest hint use |
| ★★★ | Own it | Fluency threshold met, no hints, and the skill holds up in a surprise revisit days later in a different representation |

- A failed revisit dims the 3rd star back to 2. The player can earn it back.
- Star thresholds are defined per skill node in the curriculum data, not hard-coded.

## Secret goals
Modeled on Prune: each level has one or more hidden goals revealed only after completion, so the player discovers them by playing rather than chasing them.
- Examples: solve using a specific strategy (make-ten, doubles, factor out), hit a streak, use zero hints, find a shortcut the game planted, use a representation other than the default.
- Secret goals are where "understanding over memorization" gets rewarded explicitly.
- Collecting secret goals contributes to world completion and can be a milestone for parent rewards.

## Mastery model
- **Tracked signals:** accuracy, response time trend, hint use, strategy used (when the game can observe it), representation handled, time since last success.
- **Mastery score** per skill node, 0–1, updated from signals. Thresholds in curriculum data map scores to stars and unlocks.
- **Spaced revisit:** mastered skills reappear inside later games at increasing intervals. The player does not see "review"; they see a game that happens to include an earlier idea.
- **Transfer check:** the surprise revisit uses a representation the player did not drill (practiced symbolic → revisited visual, or practiced visual → revisited as a word problem).
- **Decay:** a skill not seen for a long time, or failed on revisit, drops below the 3-star threshold.

## Sophistication tiers
The game must look and feel different as the math matures. Rough tiers (to refine):

| Tier | Worlds | Feel | Example mechanics |
|------|--------|------|-------------------|
| Playground | 1–2 | Big, bright, tactile, forgiving, characters | Tap/drag counting, feeding monsters, popping balloons, matching |
| Adventure | 3–5 | Quests, collections, maps, light narrative | Races, building things, trading, puzzles with grids |
| Workshop | 6–8 | Systems, tinkering, cause and effect | Simulations, machines, design-and-test, strategy games |
| Studio | 9–10 | Clean, abstract, elegant, player-driven | Graph sandboxes, geometry construction, optimization puzzles |
| Lab | 11–12 | Minimal, serious, open-ended | Modeling, function sculpting, limits/rates as physical systems |

Tier names are placeholders. Each tier gets its own visual language in `specs/mechanics/`.

## Mini-game engines
A small number of reusable engines, re-skinned per tier, each driven by curriculum data:
- **Fluency** — fast recall under light pressure (never framed as a test)
- **Build** — construct something with math (arrays, shapes, functions, structures)
- **Sort / match** — classify, pair, order
- **Manipulate** — number lines, fraction bars, algebra tiles, graphs you can grab
- **Story** — word problems as actual situations the player is in
- **Strategy** — choose how to solve, not just what the answer is
- **Sim** — systems with math underneath (rates, probability, growth)

Engine details and acceptance criteria live in `specs/mechanics/`.

## Parent / teacher layer
- PIN-gated mode.
- Set the player's starting world (and move it later).
- See progress by world, skill, stars, and secret goals.
- Attach a real-world reward to any milestone: a skill node, a star count, a world, a secret-goal count. The player sees a locked chest with the parent's label (e.g., "Movie night"). When unlocked, the parent gets a nudge to deliver it.
- Multiple player profiles per device.

## Open questions
- Platform and stack (see `specs/decisions/0001-platform.md`).
- World identities: twelve distinct looks, or five tier looks with per-world variation?
- How much of "strategy used" can games observe vs. the player self-reporting?
- Teacher use: classroom features, or parent-only for v1?
- Offline-first? Sync across devices?
