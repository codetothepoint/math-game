# Engine: `build`

Status: **v1** — first of the two vertical-slice engines. Hosts 144 nodes across the graph; 15 in world 4.

## 1. Core verb and feel

**The player assembles pieces from a kit into a workspace until the thing they built satisfies a target.**

It is fun with the stars turned off because building things is fun: pieces have weight and snap, the workspace fills up, the thing you made is yours, and there is more than one way to make it. The math is in the pieces. A rectangle of 23 by 14 tiles *is* 23 × 14; splitting it along the tens line *is* the distributive property. The player is not answering a question about the area model; they are making one.

What it must never be: a worksheet where the "pieces" are just an answer box with a costume. If the construction does not embody the idea (if the pieces could be anything), the recipe is wrong.

## 2. Anatomy of a level

A level has 6–10 **items** (8 in the Adventure tier). Each item is one target.

| Part | What it is |
|------|-----------|
| **Target** | What to make. Shown in the item's representation: notation, a picture, words, or a quantity to match. Stays visible. |
| **Kit** | The tray of pieces available for this recipe. Infinite supply unless the recipe limits it. |
| **Workspace** | Where pieces go. Has a snap structure appropriate to the kit (grid, columns, bar, dot grid, plot axes, gear rail). |
| **Readout** | Live description of what has been built so far, in the item's representation (a running count, an expression, a measure). Updates on every placement. |
| **Lock-in** | How an item completes. Adventure and up: the player taps **Lock it in**. Playground: the construction locks itself when it satisfies the target. |

Flow of an item: target appears → player builds → readout tracks → lock-in → satisfied or not (§6) → next item. Revisit items from the scheduler are interleaved and look identical to core items.

A level ends when its items are done. The engine then reports a `LevelResult` (`specs/mastery.md` §3).

## 3. Kits

| Kit | Pieces | Workspace snap | Used for |
|-----|--------|----------------|----------|
| `tiles` | Unit squares; drag-to-stretch makes a rectangle in one gesture | Square grid | Arrays, area, products, factors, perimeter, quotients |
| `blocks` | Place-value pieces: 1, 10, 100, 1000; decimal mode: 1, 0.1, 0.01 as a whole, a strip, a small square | Place-value columns or a 10×10 grid | Place value, composing and decomposing numbers, decimals |
| `parts` | Fraction pieces 1/b for b in the recipe's denominators, as bars or circle sectors | A unit bar or circle outline; pieces snap edge to edge | Decomposing, adding like denominators, fraction × whole |
| `strokes` | Points, segments, rays, lines, angle arms, on a dot grid | Dot grid; segments snap to dots; rays and lines snap to directions through dots | Lines and angles, classifying shapes, symmetry, building a given angle |
| `marks` | Data marks (×) | A plot axis with the recipe's tick granularity | Line plots, bar heights |
| `gears` | Rule components: a start value and operation gears (+n, −n, ×n, ÷n); chained left to right | A rail; gears snap in order | Pattern rules, function machines (later tiers) |
| `units` | Measurement pieces of a named unit (inch, foot, yard; cm, m; minute, hour) | A length rail or table rows | Unit conversion, measurement composition |

Piece operations common to all kits: **place**, **remove**, **duplicate** (long-press, then drag the copy), **split** (drag a cut across a group where the kit allows: `tiles`, `blocks` in decimal mode, `parts`), **merge** (drop a group onto a compatible group).

## 4. Targets

| Target | Satisfied when | Example |
|--------|----------------|---------|
| `quantity` | The readout equals the target value | Make 3,400 from blocks; make 4 × 2/3 from parts |
| `product` | The workspace holds exactly one rectangle (possibly split) of the target dimensions | 23 × 14 with tiles |
| `quotient` | The target count of tiles is arranged in exactly the target number of equal rows; leftover tiles sit in the remainder tray | 84 ÷ 4 → 4 rows of 21 |
| `factor-pairs` | Every rectangle with the target area has been built (orientation does not matter) | Area 24: 1×24, 2×12, 3×8, 4×6 |
| `constraint` | The construction satisfies all listed geometric constraints (parallel, perpendicular, angle range, side count, symmetry about a given line) | A quadrilateral with exactly one pair of parallel sides |
| `angle` | The angle between two placed rays is within tolerance of the target | 65°, tolerance ±2° |
| `plot` | Every data value in the target set has a mark at its position and no extra marks | Eight lengths to the nearest eighth inch |
| `sequence` | The gear chain produces the target's first n terms | 3, 7, 11, 15 |
| `table` | Every row of the conversion table is filled with the correct count of units | 1 yd = 3 ft, 2 yd = 6 ft, 3 yd = 9 ft |
| `two-ways` | Two constructions in the workspace both satisfy the inner target and differ in structure | 5/8 two different ways |

Satisfaction is evaluated live after every placement for the readout, and decisively at lock-in.

## 5. Gestures

| Gesture | Meaning |
|---------|---------|
| Drag from kit | Place a piece |
| Drag a piece | Move it |
| Drag a corner handle (tiles, grids) | Stretch a rectangle |
| Drag across a group | Cut it (where splitting is allowed); the cut snaps to the kit's lines |
| Long-press a piece or group | Duplicate |
| Drag off the workspace | Remove |
| Two-finger rotate (strokes, parts as sectors) | Rotate |
| Tap target | Hear it read aloud (every tier) |
| Tap Lock it in | Complete the item |

No gesture needs precision finer than a fingertip; snapping does the precision. Every gesture has a visible, immediate physical response (a piece lifts, a cut line appears, a rectangle resizes with its dimensions shown).

## 6. Feedback and hints

**At lock-in on an unsatisfied construction**, the engine does not say "wrong". It shows what the construction *is* against what was asked, physically: the target outline overlays the workspace and the unfilled or extra region pulses; a readout of 3,040 sits beside a target of 3,400 with the differing place highlighted; a "parallel" constraint draws the two lines extended until they meet. The item stays open and the player keeps building. This counts as a wrong attempt (`firstTry = false`).

**Hints** are player-requested, from a per-recipe ladder, at most three per item:
1. **Structure** — show the target's structure on the workspace without filling it (the grid lines for 20 | 3 and 10 | 4; the place-value columns labeled; the unit bar's eighths)
2. **Piece** — place one correct piece or group
3. **Readout** — show the full target construction faintly; the player builds over it

Each hint increments `hints`. The hint button is visible but quiet; it never prompts.

**Skipping**: a player can skip an item after one wrong attempt. The item reports `correct = false`. Skips are not punished beyond that.

## 7. Signals

| `ItemResult` field | How `build` produces it |
|--------------------|-------------------------|
| `correct` | Satisfied at lock-in before skipping |
| `firstTry` | Satisfied on the first lock-in |
| `hints` | Hints requested on the item |
| `responseMs` | Target shown → satisfying lock-in |
| `representation` | The item's representation (§8) |
| `strategy` | Detected from the structure of the construction at lock-in (below) |
| `kind` | As assigned by the scheduler |

**Strategy detection.** Because the construction is a structure, several strategies are visible in it. The engine evaluates the recipe's declared detection rules at lock-in and reports at most one strategy, the most specific that matches.

| Rule id | Kit | Detects |
|---------|-----|---------|
| `split-place-value` | tiles | A product rectangle cut along place-value lines in both dimensions (the area model) |
| `split-one-dimension` | tiles | Cut along place-value lines in one dimension only (distributive, e.g. 23×10 + 23×4) |
| `whole-array` | tiles | One uncut rectangle |
| `rows-of` | tiles | Quotient built by laying equal rows one at a time |
| `chunked-quotient` | tiles | Quotient built from large equal groups first, then the rest (partial quotients) |
| `all-pairs-ascending` | tiles | Factor pairs built in increasing order of the short side |
| `bundle` | blocks | Ten like pieces merged into the next piece |
| `unbundle` | blocks | A piece split into ten of the next smaller |
| `unit-pieces` | parts | A fraction built entirely from unit fractions |
| `grouped-copies` | parts | A fraction × whole built by duplicating the fraction group |
| `regroup-whole` | parts | Pieces merged into a whole before reading the readout |
| `edge-first` | strokes | A shape built by placing all sides before any diagonal or interior stroke |
| `mirror-pairs` | strokes | Symmetric figure built point-pair by point-pair across the line |

A recipe maps node strategy names to rule ids (`strategies:` in the recipe). `LevelResult.observesStrategies` is true whenever the recipe declares at least one rule.

**What `build` cannot observe:** whether the player counted, estimated, or reasoned before placing; anything verbal. Recipes must not claim those. Secret goals it *can* evaluate are structural (what was built, in what order, with how many pieces, with which pieces) or procedural (hints, attempts, time).

## 8. Representations

| Representation | Target shows | Readout shows |
|----------------|--------------|---------------|
| `physical` | A quantity of pieces to match (a tray of 34 loose tiles to organize; a pile of blocks to compose) | The organized construction's count |
| `visual` | A picture: an outlined rectangle with side labels, a shaded fraction of a circle, a drawn angle, a plotted example | The construction overlaid on the picture |
| `symbolic` | Notation: `23 × 14`, `3,400`, `5/8`, `∠ = 65°` | The construction expressed in notation, built up live: `(20 + 3) × (10 + 4)`, `3 thousands 4 hundreds`, `1/8 + 1/8 + …` |
| `verbal` | A sentence, with tap-to-hear: "Build a garden bed 23 long and 14 wide." | The construction in words: "a rectangle 23 long and 14 wide, cut into four pieces" |

The mechanic is identical across all four. Recipes list which they can serve.

## 9. Tier variations

| Tier | Pieces | Workspace | Lock-in | Readout | One worked example |
|------|--------|-----------|---------|---------|--------------------|
| Playground (1–2) | Big, soft, bouncy; characters carry them | Simple, few snap lines, large | Automatic on satisfaction, with a reaction from the world | A count, spoken | w02.oa.arrays-repeated-addition: fill a picnic blanket with rows of cupcakes; the readout counts "3 rows of 4… 12" |
| Adventure (3–5) | Crisp, chunky, crafty; a building-site feel | Grid or board; a few named zones (remainder tray, target outline) | **Lock it in** button; pieces settle with a click | Count and notation side by side | w04.nbt.multiply-multidigit: §10 |
| Workshop (6–8) | Components: algebra tiles, net panels, unit cubes | Technical: coordinates, labeled axes | Lock it in; the construction is "tested" (a net folds, a volume fills) | Expression and measure | w06.g.nets-surface-area: assemble panels into a net that folds into the box; readout sums face areas |
| Studio (9–10) | Clean, abstract; compass arcs, statements | Construction plane; proof canvas | Lock it in; the construction is checked against definitions | Formal notation | w10.g.constructions: inscribe a hexagon with compass arcs; readout lists the arcs used |
| Lab (11–12) | Minimal; functions as parts, matrices as blocks | Coordinate plane; sum rail | Lock it in; the construction is evaluated numerically | Function notation, numeric result | w12.f.accumulation-intro: lay rectangles under a curve; readout shows the sum and the gap to the true area |

## 10. Worked examples (world 4)

### w04.nbt.multiply-multidigit — kit `tiles`, target `product`
- **Target** (symbolic): `23 × 14`. (verbal: "Build a 23 by 14 garden bed." visual: an outlined 23×14 rectangle with sides labeled.)
- **Build**: the player drags a tile and stretches a rectangle to 23 × 14. The readout shows `23 × 14`. They drag a cut at the 20 line and another at the 10 line; the readout becomes `(20 + 3) × (10 + 4)` and each region labels itself `200`, `80`, `30`, `12`; a sum line reads `322`.
- **Lock it in**: satisfied when the workspace holds one 23×14 rectangle. Cuts are optional for satisfaction; they are what strategy detection and the secret goal look at.
- **Strategy**: both cuts → `split-place-value` → node strategy `area-model`; one cut → `split-one-dimension` → `distributive`; no cut → `whole-array` (no strategy reported).
- **Hints**: structure (the 20 | 3 and 10 | 4 lines appear), piece (the 20×10 region fills), readout (the full labeled model shows faintly).
- **Secret goal** `four-partials`: all four partial products were built and labeled before lock-in. Observable: four regions present at lock-in on first try.
- **Generator**: `low` 2-digit × 1-digit (12–49 × 2–5); `mid` 2-digit × 1-digit (11–99 × 2–9) and 3-digit × 1-digit; `high` 2-digit × 2-digit (11–99 × 11–29). Tile rectangles above 30 on a side render at half scale.

### w04.oa.factors-multiples-primes — kit `tiles`, target `factor-pairs`
- **Target**: "Find every rectangle with area 24." Readout lists dimensions found: `1 × 24 · 2 × 12 · 3 × 8 · 4 × 6`.
- **Prime items**: "Find every rectangle with area 29." Only `1 × 29` exists; locking in with that alone is correct, and the readout names it: "29 is prime."
- **Strategy**: `all-pairs-ascending` → node strategy `factor-pairs`; any complete set → `rectangle-arrays`.
- **Secret goal** `prime-call`: declared a number prime (locked in with a single 1×n rectangle) on the first try, no hints.
- **Generator**: `low` areas 6–30; `mid` 12–60 incl. two primes per level; `high` 24–100 incl. squares.

### w04.g.classify-by-lines-angles — kit `strokes`, target `constraint`
- **Target** (verbal): "Make a quadrilateral with exactly one pair of parallel sides." (visual: a faint example shape to beat; symbolic: the constraint list.)
- **Build**: segments between dots on a 7×7 dot grid. The readout names what has been built so far: "3 sides… 4 sides, closed. Parallel pairs: 1."
- **Lock it in**: all constraints hold. Unsatisfied: the readout highlights the failing constraint and extends the offending lines.
- **Strategy**: `edge-first`. No node strategies listed; the engine reports none.
- **Secret goal** `different-each-time`: three consecutive items satisfied with no two constructions congruent.
- **Generator**: constraint sets drawn from the node: right/acute/obtuse triangle; parallelogram; trapezoid (exactly one pair); rectangle that is not a square; quadrilateral with perpendicular sides but no parallel sides.

### w04.nbt.place-value-ten-times — kit `blocks`, target `quantity`
- **Target** (symbolic): "Make 10 × 340." (verbal: "Make ten times three hundred forty.")
- **Build**: the player drags 3 hundreds and 4 tens, long-presses to duplicate the group until there are ten copies, or bundles: ten tens become a hundred, ten hundreds a thousand. The readout: `3 thousands 4 hundreds = 3,400`.
- **Strategy**: `bundle` is detected. The node lists no strategies; the engine reports none but records nothing wrong with that.
- **Secret goal** `bundle-up`: used bundling (not ten separate copies) to reach the target.
- **Generator**: `low` ×10 of a 2-digit number; `mid` ×10 of a 3-digit number; `high` "what is ten times bigger" targets expressed as a digit moving place (`4 in 340 vs 4 in 3,400`).

## 11. Recipe contract

A `build` recipe supplies (`recipes/recipe.schema.yaml`):
- `kit` — one of §3
- `target` — one of §4
- `representations` — subset of the node's
- `generator` — parameters the engine needs for this target (ranges, denominators, constraint sets, data sets), optionally per difficulty band
- `strategies` — map of node strategy name → detection rule id from §7
- `hints` — the three ladder rungs, as the engine's hint kinds (`structure`, `piece`, `readout`) with recipe-specific detail
- `secret_goals` — the node's goal ids this recipe can evaluate, each with the engine condition
- `frame` — optional one-line framing for the tier ("building a garden bed")

## 12. Acceptance criteria

- **Fun check**: every W4 recipe's construction embodies the idea; the pieces could not be anything else. A reviewer can name the math from a screenshot of the workspace with the target hidden.
- **Tracking**: emits every `ItemResult` field; strategy detection is deterministic from the final construction; `observesStrategies` is true iff the recipe declares rules.
- **Tier fit**: the same recipe renders in Playground with automatic lock-in and in Adventure with the button; piece art and sound come from the tier spec, never from the recipe.
- **Data contract**: a level can be generated from node + recipe + representation + band + revisit list with no other input.

Engine-logic test cases (to implement with the engine, platform-neutral):
1. `product` satisfied by one 23×14 rectangle with any cuts; not by two rectangles; not by 14×23 split into unequal non-place-value pieces? — yes satisfied (dimensions are what matter); strategy then `split-one-dimension` only if cuts fall on place-value lines.
2. `factor-pairs` for 24 requires exactly the set {1×24, 2×12, 3×8, 4×6}; 6×4 counts as 4×6; a 5×5 extra rectangle fails.
3. `constraint` "exactly one pair of parallel sides" rejects a parallelogram and accepts a trapezoid in any orientation.
4. `quantity` 3,400 from 34 hundreds is satisfied; readout normalizes to `3 thousands 4 hundreds`.
5. Strategy priority: `split-place-value` beats `split-one-dimension` beats `whole-array`.
6. Hint ladder never exceeds three; `hints` counts requests, not rungs shown twice.

## 13. Open questions
- Should Adventure lock-in auto-trigger after a short settle when the construction is satisfied, to reduce taps for younger players in world 3?
- Tile rectangles larger than ~40 on a side are cramped on a phone; the generator caps or the kit auto-scales. v1: auto-scale, cap at 99.
- `two-ways` targets and the `grouped-copies` rule need a definition of "structurally different" that is not fooled by reordering identical pieces. v1: different multiset of piece sizes.
