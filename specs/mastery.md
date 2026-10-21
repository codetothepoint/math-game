# Mastery Model

Status: **v1** — the rules every engine feeds and the world map reads. Platform-neutral: pure functions over player state. The reference implementation in `tools/mastery_ref.py` executes every rule here, and `tools/test_mastery.py` runs the scenarios in §12. If this document and the reference implementation disagree, one of them is wrong and the fix goes in both.

What this model has to deliver (from the constitution and GDD):
- Progress is tracked by playing. Nothing is a test.
- Stars reflect understanding that holds up over time, across representations, and without hints. The third star cannot be earned in one sitting.
- Players move at their own pace in any direction the graph allows. A parent or teacher sets the starting world. Skipping ahead is just playing.

## 1. Vocabulary

| Term | Meaning |
|------|---------|
| **Skill node** | A node in `specs/curriculum/`. Has `prerequisites`, `representations`, `engines`, `strategies`, and `mastery` thresholds. |
| **Item** | One challenge inside a level: one question, one construction, one placement. The unit of evidence. |
| **Level** | A playable session an engine generates for one target node. Contains core items for that node and may contain revisit items for other nodes. |
| **Representation** | The form an item takes: `symbolic`, `visual`, `verbal`, `physical`. Each item has exactly one. |
| **Hint** | Engine-provided help the player asks for on an item. Counted per item. |
| **Strategy** | A named approach from the node's `strategies` list that an engine observed the player using on an item. Only some engines can observe strategies; a level says whether it can. |
| **Revisit** | Items for a node the player already reached two stars on, served later inside other levels without a label. |
| **Transfer** | A revisit in a representation the player drilled least. |
| **Reach** | Playing a node whose prerequisites are not all met yet. |
| **Assumed** | A node in a world before the player's starting world, treated as met until evidence says otherwise. |

## 2. State

**PlayerProfile**
- `startingWorld` (1–12), set by a parent or teacher.
- `nodes`: map from node id to **NodeState**.

**NodeState** (all fields start empty or zero)

| Field | Type | Meaning |
|-------|------|---------|
| `score` | 0–1 | Accuracy evidence. The single number used for unlocking. |
| `assumed` | bool | Provisional credit from the starting world (§6.3). |
| `stars` | 0–3 | Earned stars. |
| `items` | int | Items answered on this node, all kinds. |
| `recent` | last 10 ItemResults | Ring buffer, newest last. |
| `repItems` | map rep → int | Items answered per representation. |
| `strategies` | set | Strategies observed on this node. |
| `strategyObservable` | bool | True once any completed level on this node reported `observesStrategies`. |
| `fluencyMs` | int or null | Median response time of the last 10 clean correct items (the reference keeps those times in `cleanTimes`). |
| `levelsCompleted` | int | Levels finished with this node as target. |
| `lastLevelRepresentation` | enum or null | Main representation of the most recent completed level; breaks ties in §7.3. |
| `secretGoals` | set of goal ids | Secret goals achieved on this node. |
| `twoStarAt`, `threeStarAt` | timestamp or null | When each was first earned. |
| `revisit` | RevisitState or null | See §7. |
| `lastSeen` | timestamp | Last item on this node, any kind. |

**RevisitState**
| Field | Meaning |
|-------|---------|
| `dueAt` | When the next set becomes due. |
| `intervalIndex` | Index into the node's `revisit_days`. |
| `representation` | Representation chosen for the current set (§7.3). |
| `served`, `correct`, `hints` | Progress of the current set. |
| `passed` | Count of sets passed since `twoStarAt`. |
| `lastSetPassed` | Outcome of the most recent completed set; null before the first. |
| `history` | List of (at, passed) for every completed set, so the portal can count recent passes. |

## 3. Signals from engines

Engines report two things. They never compute scores or stars.

**ItemResult**
| Field | Type | Notes |
|-------|------|-------|
| `nodeId` | id | The node this item is evidence for. |
| `representation` | enum | The item's representation. |
| `engine` | enum | Engine that produced it. |
| `correct` | bool | Final outcome of the item. |
| `firstTry` | bool | Correct with no wrong attempt on this item. |
| `hints` | int | Hints requested on this item. |
| `responseMs` | int | Time from item shown to final correct answer; null if incorrect. |
| `strategy` | string or null | A strategy from the node's list, if the engine observed one. |
| `kind` | `core`, `revisit`, `reach` | Why the item was served. |
| `at` | timestamp | |

**LevelResult**
| Field | Notes |
|-------|-------|
| `nodeId`, `engine`, `representation` | Target node and the level's main representation. |
| `completed` | The engine's exit condition was met. |
| `items` | All ItemResults in play order, revisit items included. |
| `observesStrategies` | Whether this engine can report strategies for this node. |
| `secretGoalsHit` | Goal ids the engine evaluated as achieved. |
| `startedAt`, `endedAt` | |

## 4. Evidence and the score update

Each item yields an evidence value `e`:

| Outcome | `e` |
|---------|-----|
| Correct, first try, no hints | 1.0 |
| Correct, no hints, after a wrong attempt | 0.6 |
| Correct with one or more hints | 0.4 |
| Incorrect | 0.0 |

If the node has `fluency_ms` and a correct item took longer than `SLOW_FACTOR × fluency_ms`, `e` is capped at 0.8. Slow and right is still mostly right.

The score moves toward the evidence:

```
alpha = ALPHA_CORE
if e < score: alpha = alpha × MISS_DAMPING
score = score + alpha × (e − score)
```

This applies to `core` and `reach` items. `revisit` items do not move the score one by one; the completed set does (§7.4), because a transfer check is one piece of evidence, not five.

A miss moves the score half as far as a success. Mastery is supposed to be sticky; one bad item is not a verdict.

Every item of every kind also updates `items`, `recent`, `repItems`, `lastSeen`, `strategies` (if `strategy` is set), and `fluencyMs` (median `responseMs` over the last 10 items that were correct with no hints; null until there are 3).

Worked example, `ALPHA_CORE = 0.15`, from zero: nine clean items give `1 − 0.85⁹ = 0.768`; fifteen give `0.913`. A node with `three_star: 0.9` therefore needs at least fifteen clean items before the third star is even arithmetically possible.

Constants (tuning knobs, §13): `ALPHA_CORE 0.15`, `ALPHA_REVISIT 0.20`, `MISS_DAMPING 0.5`, `SLOW_FACTOR 3`, `RECENT_N 10`.

## 5. Stars

Stars are recomputed after every level from NodeState. They only go up through these rules and only go down through the demotions listed.

**★ Played it** — `levelsCompleted ≥ 1`. Never removed.

**★★ Got it** — all of:
- ★ held
- `score ≥ mastery.two_star`
- hint rate over `recent` ≤ `HINT_RATE_TWO_STAR` (0.3): items with hints ÷ items in `recent`
- evidence in at least two representations: at least two entries in `repItems` with `≥ REP_MIN_ITEMS` (3) items each
- if the node lists strategies and any completed level on it had `observesStrategies = true`, then `strategies` is non-empty

Demotion: ★★ → ★ only if `score < two_star − STAR_HYSTERESIS` (0.15). Not from a single miss.

**★★★ Own it** — all of:
- ★★ held
- `score ≥ mastery.three_star`
- zero hints in `recent`
- if the node has `fluency_ms`: `fluencyMs ≤ fluency_ms`
- `revisit.passed ≥ 1` and the most recent revisit set passed (§7.4)

Because the first revisit cannot be served before `revisit_days[0]` days after ★★, the third star cannot be earned in one sitting, whatever the player does.

Demotion: ★★★ → ★★ when a revisit set fails (§7.4), or if `score < three_star − STAR_HYSTERESIS_THREE` (0.10).

**Time alone never removes a star.** A node whose revisit is overdue is shown as *due* on the map (§11). Fading is a prompt, not a penalty; the star comes off only on evidence. (This supersedes the GDD draft line about decay from time.)

## 6. Availability

### 6.1 States
For each node, derived from prerequisites' states:
- **met** prerequisite: `score ≥ its mastery.unlock`, or `assumed`.
- **available**: every prerequisite is met (entry nodes are always available).
- **reach**: not available, but every prerequisite is either met or itself available. One hop beyond the frontier.
- **locked**: anything else.

Availability is monotonic: once a node has `levelsCompleted ≥ 1`, it is treated as available forever, whatever happens to its prerequisites' scores.

### 6.2 Reach play
A reach node can be played. Its items are `kind: reach` and update the node normally. In addition, each clean item (`e = 1.0`) gives every unmet direct prerequisite credit:

```
prereq.score = prereq.score + ALPHA_REACH × (1 − prereq.score)      ALPHA_REACH = 0.08
```

Misses on a reach node do not touch prerequisites. A player who can already multiply two-digit numbers is demonstrating the facts underneath; a player who cannot is just learning something new.

### 6.3 Starting world and assumed nodes
When a parent sets `startingWorld = W`:
- every node in a world `< W` with no evidence (`items = 0`) becomes `assumed`: `score = its mastery.unlock`, `stars = 0`.
- every node in a world `≥ W` that is `assumed` (a leftover from an earlier, higher start) stops being assumed: `score = 0`, `stars = 0`. It has no evidence, so nothing is lost.
- nodes with evidence are left alone.
- moving the starting world later re-runs this rule; it never removes evidence.

Evidence means `items > 0`. Reach credit on a node that has never been played (§6.2) is provisional, not evidence: raising the start above that node replaces it with the assumed score, and lowering it again returns the node to zero.

An assumed node behaves as met for unlocking, so the starting world's entry nodes are available. The first real item on an assumed node clears `assumed`; its score continues from the provisional value. Assumed nodes are eligible for light interleaving (§7.5) so gaps surface during play rather than through a test.

## 7. Revisits

### 7.1 Scheduling
When ★★ is first earned at time `t`: `revisit = { dueAt: t + revisit_days[0], intervalIndex: 0, passed: 0 }`. A node is **due** when `now ≥ dueAt`.

### 7.2 Delivery
Revisit items are served inside other levels. When an engine builds a level of `n` core items for target `T`, the scheduler offers up to `max(1, floor(INTERLEAVE_MAX × n))` revisit items (`INTERLEAVE_MAX = 0.2`), from due nodes that:
- list this engine in `engines`, and
- have a recipe on this engine that serves the set's chosen representation, and
- are not `T` itself.

The cap applies **after** these filters, most overdue first, so a due node this engine cannot host never takes a slot from one it can. Items are mixed into the level, never grouped at the end, never labelled. If no due node fits the engine, the revisit waits for one that does.

### 7.3 Transfer representation
When a set starts, its representation is the one, among the node's `representations` that **some recipe for the node can serve**, with the fewest entries in `repItems`. Ties go to the representation not used in the node's most recent level. The point is to check the idea, not the drill. A representation no recipe serves is never chosen, because a set in it could never be delivered. Once a set has served its first item, its representation is fixed until the set completes (§7.4), whichever engine serves the rest.

### 7.4 Sets and outcomes
A set is `REVISIT_SET_SIZE` (5) items. It completes when the fifth item is answered, however many levels that spans.
- **Pass**: `correct ≥ REVISIT_PASS_CORRECT` (4) and `hints = 0`. Then `passed += 1`, `intervalIndex = min(intervalIndex + 1, len(revisit_days) − 1)`, `dueAt = now + revisit_days[intervalIndex]`, and the score takes one clean step at revisit weight: `score += ALPHA_REVISIT × (1 − score)`. After the last interval, revisits repeat at that interval.
- **Fail**: otherwise. If `stars = 3`, demote to 2. `intervalIndex = 0`, `dueAt = now + revisit_days[0]`, and the score takes one damped step toward the set's accuracy: `score += ALPHA_REVISIT × MISS_DAMPING × (correct ÷ size − score)`. The damping is unconditional: a failed set is a miss even when its accuracy happens to exceed the score. The player earns the star back by passing again.

Individual revisit items do not move the score (§4); everything else about them (`recent`, `repItems`, `fluencyMs`, strategies) is recorded normally, so a hinted revisit item still counts against the zero-hints rule for ★★★.

### 7.5 Assumed-node interleaving
At most one item per level may come from an assumed node whose dependents the player is currently playing, in any representation the engine supports. It counts as a core item for that node and clears `assumed`. This is how the game finds out what a player who started in world 4 actually has from world 3.

## 8. Representation rotation

Within core play, engines rotate representations so that ★★ is reachable: level `k` for a node (0-based) uses `representations[k mod len]`, skipping any the engine cannot host (engine specs declare which they support). Within a level an engine may mix representations; the level's `representation` field is its main one.

## 9. Strategies and secret goals

- Strategies are recorded, never required for the score. They gate ★★ only where an engine could have observed them (§5).
- Secret goals are evaluated by the engine from what it observed during the level and recorded in `secretGoals`. They never affect `score` or stars. They count toward milestones (§10).

## 10. Milestones

Evaluated after every level, for parent-set rewards and world completion:

| Milestone | Met when |
|-----------|----------|
| `node_stars(node, n)` | `stars ≥ n` on that node |
| `world_stars(world, n)` | sum of stars across the world's nodes ≥ n |
| `world_complete(world)` | every node in the world has `stars ≥ 2` |
| `secret_goals(n, world?)` | at least n secret goals achieved, optionally within one world |
| `mastered(n)` | at least n nodes at ★★★ |

A milestone, once met, stays met. The parent layer (its own spec, later) attaches rewards and notifications to these.

## 11. Derived map state

What the world map shows for a node, in precedence order:
1. `assumed` (shown as quietly open, no stars)
2. `locked`
3. `reach`
4. `available` with `stars` 0–3
plus a `due` flag when `revisit` exists and `now ≥ dueAt`.

## 12. Test scenarios

Each is an executable test in `tools/test_mastery.py`.

1. **Nine clean items** on a fresh node (two representations, ≥ 3 items each, one strategy observed) → `score = 0.768` and ★★, revisit due in `revisit_days[0]`.
2. **Hint rate** — fifteen clean items in one representation (★ only), then four hinted items in a second → score ≥ 0.75 but hint rate 0.4 → still ★.
3. **Third star needs time** — fifteen clean items (`score 0.913`) → ★★ at the end of the session; nothing due the same day; a 5-item set 2 days later at 4/5, 0 hints, in the less-drilled representation → ★★★, next revisit in `revisit_days[1]`.
4. **Failed revisit** — a ★★★ node, set at 2/5 → ★★, `dueAt = now + revisit_days[0]`; a 5/5 set at that date restores ★★★.
5. **Reach credit** — a node with one unmet prerequisite at `score 0.5` is `reach`; one clean item on it raises the prerequisite to `0.54`; a miss leaves it at `0.54`.
6. **Starting world 4** with the real curriculum → every node in worlds 1–3 is assumed at its `unlock` with no stars; on day one `w04.oa.multiplicative-comparison` and `w04.nbt.place-value-ten-times` are available, `w04.nbt.multiply-multidigit` is `reach`, `w04.nbt.divide-by-one-digit` and `w04.oa.multistep-word-problems` are locked, and the first real item on an assumed world-3 node clears `assumed`.
7. **Monotonic availability** — a played node stays available when a prerequisite's score drops below `unlock`.
8. **Miss damping** — from `score 0.8`, one incorrect core item → `0.74`.
9. **Transfer representation** — drilled `symbolic` 12 times and `visual` 3 times → the set starts in `visual`; at 12 and 12, the representation not used in the last level.
10. **One sitting is not enough** — twenty clean items in one session → ★★, and nothing is due that day, so ★★★ is unreachable.
11. **Fluency gate** — on a node with `fluency_ms 3000`, twenty slow correct items (10 s each) cap at `e = 0.8`, so the score stays under 0.81 at ★★; fifteen fast items raise `score ≥ 0.9` and `fluencyMs = 1500`, but the star still waits for the revisit.
12. **Milestones** — two world-1 nodes at ★★ → `world_stars(1) = 4`, `world_complete(1)` true, `world_complete(2)` false; a secret goal reported by a level is counted.
13. **Failed set above the score** — a ★★ node at `score 0.6`; a set at 4/5 with one hint fails; `score → 0.62` (the damped step applies even though 0.8 > 0.6).
14. **Starting world moved down** — start 4, then start 2: world-3 nodes with no evidence lose `assumed` and return to `score 0`; a world-3 node that was played keeps its evidence.
15. **Un-hostable transfer representation** — a node whose least-drilled representation no recipe serves gets its set in the least-drilled representation that one does, and a due node this engine cannot host never blocks one it can.

**Rule tests.** Beyond the scenarios, every rule in §5–§8 has a test that fails when the rule is removed (the first review found most of them untested): ★★ needs `score ≥ two_star`; the strategy gate; ★★→★ demotion at `two_star − 0.15` and not above it; ★★★ needs `score ≥ three_star`, zero hints in `recent`, and the fluency gate; ★★★→★★ at `three_star − 0.10`; a 5/5 set with a hint fails; `intervalIndex` caps at the last interval; reach credit goes only to unmet prerequisites; `dueRevisits` honors the engine, the exclusion, most-overdue-first, and the interleave cap (`8 → 1`, `10 → 2`); re-running the starting world keeps evidence; `fluencyMs` needs three clean items; re-earning ★★ keeps the original revisit schedule.

## 13. Tuning knobs and open questions

All constants are in one table in the reference implementation. The initial values are reasoned, not measured:

| Knob | v1 | What it trades |
|------|----|----------------|
| `ALPHA_CORE` | 0.15 | Speed to ★★ (≈9 clean items) vs noise |
| `MISS_DAMPING` | 0.5 | Forgiveness vs sensitivity to real gaps |
| `HINT_RATE_TWO_STAR` | 0.3 | Hint tolerance for "got it" |
| `REVISIT_SET_SIZE`, `REVISIT_PASS_CORRECT` | 5, 4 | Reliability of the transfer check vs its length |
| `INTERLEAVE_MAX` | 0.2 | How much of a level is review |
| `ALPHA_REACH` | 0.08 | How much skipping ahead credits the skills underneath |

Open:
- Whether `fluencyMs` should use the last 10 clean items across all representations or only the drilled one.
- Whether an item left unfinished (level abandoned) counts as incorrect, is discarded, or is scored by the engine. v1: discarded.
- Whether `world_complete` should require ★★★ on a subset of "keystone" nodes per world.
