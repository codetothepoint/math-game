# Engine: `manipulate`

Status: **v1** — second of the two vertical-slice engines. Hosts 149 nodes across the graph; 11 in world 4.

## 1. Core verb and feel

**The player grabs an instrument that already exists and adjusts it until it reads the target.**

Where `build` assembles from nothing, `manipulate` starts with a whole: a number line, a bar already cut into pieces, an angle with two arms, a chart with digits in columns. The player slides, rotates, re-cuts, shifts. The instrument responds continuously and reads itself out as it moves. The fun is the fun of a good dial: tight, responsive, with satisfying detents where the math lives (a snap at the midpoint, a click when two bars line up, a chime when the arms hit 90°).

The math is in the response. Sliding a digit one column left *is* multiplying by ten; re-cutting a bar from fourths to eighths without moving the shaded edge *is* equivalence. The player feels the relationship before they name it.

What it must never be: a multiple-choice question with a slider for an answer box. If moving the instrument does not change something the player can see and reason about, the recipe is wrong.

## 2. Anatomy of a level

6–10 items (8 in Adventure). Each item is one target on one instrument.

| Part | What it is |
|------|-----------|
| **Target** | What the instrument should read or show. Stays visible. |
| **Instrument** | The thing being adjusted, with its handles. |
| **Readout** | The instrument's live reading in the item's representation. |
| **Detents** | Snap points where the instrument settles: ticks, midpoints, right angles, aligned edges. Recipes set granularity. |
| **Lock-in** | **Lock it in** button (Adventure and up); automatic settle in Playground. |

Flow: target appears → player adjusts → readout follows → lock-in → satisfied within tolerance or not (§6) → next item. Revisit items interleave unlabelled.

## 3. Instruments

| Instrument | Handles | Reads out | World 4 use |
|------------|---------|-----------|-------------|
| `number-line` | A marker to slide; pinch to zoom the scale; drag to pan | The marker's value at the current tick granularity | Rounding, decimals on the line, placing fractions |
| `fraction-bar` | A shaded edge to slide; a slicer to re-cut the bar into b pieces; a second bar to compare against | Shaded amount as a fraction over the current denominator | Equivalence, decomposing, adding like denominators |
| `grid` | Cells to drag-shade on a 10×10 (hundredths) or n×n grid; a mirror line to slide and rotate; a figure to flip | Shaded fraction or decimal; symmetry status | Tenths and hundredths, decimal notation, line symmetry |
| `angle` | One or two arms to rotate about a vertex; a protractor overlay to rotate and read | Degrees; fraction of a full turn | Angle concept, measuring, additive angles |
| `place-chart` | Digits that slide between place-value columns; a ×10 / ÷10 lever that shifts them all | The number | Ten times bigger, decimal place value |
| `balance` | Weights to move between pans; the beam tilts | Equality or which side is heavier | Equations (world 6+) |
| `area-model` | Corner handles of a rectangle; a slicer | Dimensions and area | Fraction × fraction (world 5+) |
| `graph` | Points and curve handles on a coordinate plane | Coordinates, slope, parameters | Functions (world 8+) |

Instruments later worlds need (`vector`, `matrix`, `unit-circle`) are variations of `graph` and `angle`; they get rows here when their worlds are specified.

## 4. Targets

| Target | Satisfied when | Example |
|--------|----------------|---------|
| `value` | The readout equals the target within tolerance | Place 0.37 on the line; set the arms to 65° |
| `nearest` | The marker is at the target's rounded value at the stated place | Nearest hundred of 3,462 |
| `equal` | Two instruments read the same amount (bars aligned, two numbers equal) | Re-cut so 3/4 lines up with ?/8 |
| `partition` | The bar is cut into the target pieces and the shaded region is the target sum | 5/8 as 2/8 + 3/8 |
| `unknown` | The adjustable part reads the value that makes the whole equal the target | Two adjacent angles total 90°; one is 35°; set the other |
| `match` | The instrument's picture matches a given picture (a grid shading matches a number-line position; a figure matches its reflection) | 0.34 on the grid ↔ on the line |
| `symmetric` | The mirror line is placed so the figure reflects onto itself; or the figure is flipped to coincide with its reflection | A kite's line of symmetry |

## 5. Gestures

| Gesture | Meaning |
|---------|---------|
| Drag a marker, edge, or arm | Adjust |
| Pinch on `number-line` | Zoom (finer ticks appear) |
| Drag the scale | Pan |
| Drag the slicer across a bar | Re-cut into the nearest allowed denominator |
| Two-finger rotate on `angle` / `grid` mirror | Rotate |
| Drag a digit across a column line (`place-chart`) | Shift one place |
| Pull the lever (`place-chart`) | Shift everything one place |
| Drag-shade on `grid` | Shade or unshade cells |
| Tap target | Hear it read aloud |
| Tap Lock it in | Complete the item |

Detents are felt (a short haptic tick) and seen (the readout bolds at a detent). Between detents the readout still updates; the player is never stuck guessing what the instrument reads.

## 6. Feedback, tolerance, and hints

**Tolerance** per instrument: `angle` ±2° (±1° at `high`); `number-line` exact at the current tick granularity (the player must zoom to place 0.37); `fraction-bar` exact; `grid` exact; `place-chart` exact.

**At lock-in outside tolerance**, the instrument shows the gap: the target position glows on the line and a bracket spans the distance to the marker, labeled; the protractor shows both readings; the two bars show the mismatch strip. The item stays open. This is a wrong attempt.

**Hints**, at most three per item:
1. **Zoom** — the instrument reveals the detents that matter (the tenths ticks appear; the bar shows its eighths lightly; the quarter-turn marks show on the angle)
2. **Nudge** — the handle moves most of the way, leaving the last detent to the player
3. **Show** — the target position is shown faintly; the player moves to it

**Skipping** after one wrong attempt, reported as `correct = false`.

## 7. Signals

| `ItemResult` field | How `manipulate` produces it |
|--------------------|------------------------------|
| `correct` | Within tolerance at lock-in before skipping |
| `firstTry` | Within tolerance on the first lock-in |
| `hints` | Hints requested |
| `responseMs` | Target shown → satisfying lock-in |
| `representation` | The item's representation (§8) |
| `strategy` | Detected from the path of adjustment (below) |
| `kind` | As assigned by the scheduler |

**Strategy detection.** The adjustment path is observable: where the marker paused, how the bar was cut, which handle moved first.

| Rule id | Instrument | Detects |
|---------|------------|---------|
| `midpoint-first` | number-line | Marker paused at the midpoint between the two candidate round values before settling (rounding by midpoint) |
| `zoom-then-place` | number-line | Zoomed to the needed granularity before the first placement |
| `recut-only` | fraction-bar | Reached equivalence by re-cutting without moving the shaded edge |
| `regroup-whole` | fraction-bar | Merged pieces into a whole before locking in (the readout showed a mixed number) |
| `benchmark-compare` | fraction-bar | Used the half marker to compare before adjusting |
| `shift-digits` | place-chart | Moved digits across columns rather than typing or counting copies |
| `quarter-turns` | angle | Rotated in quarter-turn detents before fine adjustment |
| `complement-first` | angle | On an `unknown` target, set the known angle first, then adjusted the unknown |
| `fold-test` | grid | Flipped the figure across the candidate line to test symmetry before locking in |

Recipes map node strategy names to rule ids. `observesStrategies` is true iff the recipe declares rules.

**What `manipulate` cannot observe:** mental computation, estimation, whether the player read the target. Secret goals must be about the path (handles moved, detents used, hints, attempts, time) or the result.

## 8. Representations

| Representation | Target shows | Readout shows |
|----------------|--------------|---------------|
| `physical` | The instrument itself to set: "set this to match that" with no notation (a second, fixed instrument shows the target) | The instrument's state |
| `visual` | A picture: a shaded grid, a drawn angle, a marked line | The instrument overlaid on the picture |
| `symbolic` | Notation: `0.37`, `3/4 = ?/8`, `65°`, `10 × 340` | The reading in notation, updating live |
| `verbal` | Words, tap-to-hear: "Round three thousand four hundred sixty-two to the nearest hundred." | The reading in words |

## 9. Tier variations

| Tier | Instruments feel like | Detents | Lock-in | One worked example |
|------|-----------------------|---------|---------|--------------------|
| Playground (1–2) | Toys: a slide, a pizza, a clock with a big hand | Few, loud, celebratory | Automatic settle | w01.md.tell-time-hour-half: drag the big hand; the clock chimes at o'clock and half past |
| Adventure (3–5) | Tools from a kit bag: ruler, protractor, bar cutter | Clear ticks; a haptic click | Lock it in | w04.nf.fraction-equivalence: §10 |
| Workshop (6–8) | Lab gear: balance, graph paper, calipers | Fine; readouts to two decimals | Lock it in; the setting is "tested" | w06.ee.one-step-equations: move weights until the balance levels; readout shows `x = 7` |
| Studio (9–10) | Precise instruments: unit circle, transformation sliders | Exact | Lock it in | w09.f.transformations-of-functions: drag k sliders until the graph matches the target |
| Lab (11–12) | Scientific: zoomable curves, parameter dials | Continuous with named limits | Lock it in | w12.f.instantaneous-rate-of-change: pinch to zoom on a curve until it looks straight; the readout shows the secant slope converging |

## 10. Worked examples (world 4)

### w04.nf.fraction-equivalence — instrument `fraction-bar`, target `equal`
- **Target** (symbolic): `3/4 = ?/8`. (visual: a bar shaded 3/4 above an empty bar in eighths; verbal: "Cut the bar into eighths so the shaded part stays the same.")
- **Adjust**: the top bar shows 3/4 shaded, fixed. The player drags the slicer across the bottom bar; it re-cuts to eighths. They drag the shaded edge to line up with the top bar; a click when the edges align. Readout: `6/8`.
- **Lock it in**: satisfied when the two shaded amounts are equal and the bottom denominator is the target's.
- **Strategy**: if the player re-cut the bottom bar *with the shade already at 3/4 of its length* and did not move the edge → `recut-only`; the node lists no strategies, so none is reported, but the secret goal uses it.
- **Secret goal** `same-edge`: reached equivalence by re-cutting only, three items in a row.
- **Generator**: `low` halves/fourths/eighths and thirds/sixths; `mid` adds fifths/tenths and twelfths; `high` asks the reverse (`6/8 = ?/4`) and non-unit scalings (`2/3 = ?/12`).

### w04.md.measure-angles — instrument `angle`, target `value`
- **Target** (visual): a drawn angle; "How big is it?" The player rotates the protractor overlay to the vertex and arm, then slides a reading marker to the second arm. Readout: `65°`. (symbolic variant: "Make 65°": rotate the free arm instead.)
- **Lock it in**: within ±2°.
- **Strategy**: `quarter-turns` → no node strategy listed.
- **Secret goal** `zero-first`: aligned the protractor's zero to an arm before reading, every item in the level.
- **Generator**: `low` multiples of 10 from 20° to 160°; `mid` multiples of 5; `high` any whole degree, including reflex angles shown with the small-angle marker.

### w04.nbt.round-multidigit — instrument `number-line`, target `nearest`
- **Target** (verbal): "Round 3,462 to the nearest hundred." The line shows 3,000 to 4,000 at hundreds. The player slides the marker to 3,462 (zooming reveals tens), then to the nearest hundred detent. Readout: `3,462 → 3,500`.
- **Lock it in**: marker at 3,500.
- **Strategy**: `midpoint-first` → node lists none; recorded for the secret goal.
- **Secret goal** `midpoint-call`: paused at the midpoint (3,450) before settling, on a `high` item.
- **Generator**: `low` round 3-digit to tens; `mid` 4-digit to hundreds; `high` 5–6-digit to any place, including values within 5 of a midpoint.

### w04.nf.decimal-notation — instrument `grid` + `number-line`, target `match`
- **Target** (visual): a 10×10 grid with 37 cells shaded. The player slides the marker on a 0–1 line; it starts at tenths, pinch reveals hundredths. Readout: `0.37 = 37/100`.
- **Lock it in**: marker at 0.37 exactly.
- **Strategy**: `zoom-then-place`.
- **Secret goal** `tenths-first`: placed the marker at 0.3 before zooming, every item in the level.
- **Generator**: `low` tenths only; `mid` hundredths; `high` reverse direction (given 0.37, shade the grid by dragging) and values like 0.40 vs 0.04.

### w04.g.line-symmetry — instrument `grid`, target `symmetric`
- **Target** (visual): a figure on a dot grid. The player slides and rotates the mirror line; the reflection renders live. Lock in when the reflection coincides with the figure. Readout: "Reflection matches" or "Off by n dots."
- **Strategy**: `fold-test`.
- **Secret goal** `all-the-lines` (from the node): found every line of symmetry on a regular polygon: the item asks for all lines; satisfied when each line has been locked in once.
- **Generator**: `low` figures with one vertical line; `mid` diagonal lines, two-line figures; `high` regular polygons, figures with no line (lock in "none").

## 11. Recipe contract

A `manipulate` recipe supplies:
- `instrument` — one of §3 (a second instrument may be listed for `match` targets)
- `target` — one of §4
- `representations` — subset of the node's
- `generator` — ranges, denominators, places, angle sets, figure sets; optionally per band
- `tolerance` — override of the instrument default where the recipe needs it
- `strategies` — node strategy name → rule id from §7
- `hints` — the three rungs (`zoom`, `nudge`, `show`) with recipe detail
- `secret_goals` — goal ids with engine conditions
- `frame` — optional one-line framing

## 12. Acceptance criteria

- **Fun check**: moving the instrument changes something visible that the player can reason about on every W4 recipe; the readout at every position is a true statement.
- **Tracking**: emits every `ItemResult` field; strategy detection is deterministic from the recorded adjustment path.
- **Tier fit**: the same recipe runs with Playground's automatic settle and Adventure's button; instrument art and haptics come from the tier spec.
- **Data contract**: a level is generated from node + recipe + representation + band + revisit list.

Engine-logic test cases:
1. `nearest` for 3,462 to hundreds accepts 3,500 only; 3,450 is a midpoint pause, not a result.
2. `equal` for `3/4 = ?/8` requires denominator 8 and shaded 6; `6/8` reached by re-cutting with the edge unmoved reports `recut-only`.
3. `value` 65° accepts 63–67 at `low`/`mid`, 64–66 at `high`.
4. `match` grid 37 cells ↔ line 0.37 at hundredths granularity; 0.4 at tenths granularity is outside tolerance.
5. `symmetric` on a square requires four distinct lines when the item asks for all; a duplicate line does not count twice.
6. Hint rungs are ordered; `hints` counts requests.

## 13. Open questions
- `number-line` zoom on a phone: pinch is natural but conflicts with page pinch-zoom in some containers; the tier spec may add zoom buttons. v1: pinch plus buttons.
- Whether `physical` items (match a fixed instrument with no notation) should be the default first representation for every W4 recipe, since they are the most "feel it" form. v1: yes, where the node lists `physical`.
- Reflex angles in the `angle` instrument need a convention for which side the protractor reads. v1: the protractor reads the smaller angle; reflex targets show the arc explicitly.
- `additive-angles` generators keep every angle at least 5°, so an unset arm (0°) is never already within tolerance of the unknown.
- `fraction-bar` items start with the shade already at the reference amount in the starting denominator, which is what makes `recut-only` possible; `add-sub` items start with an empty result bar.
