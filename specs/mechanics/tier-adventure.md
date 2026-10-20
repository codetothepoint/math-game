# Tier: Adventure (worlds 3–5)

Status: **v1** — the look, feel, sound, and voice shared by worlds 3, 4, and 5, plus the full identity of world 4 for the vertical slice. Platform-neutral: tokens and rules, no component names. See ADR 0003 for how identities and themes relate.

## 1. Who it is for and what it should feel like

Players are roughly 8 to 11. They can read, they like being trusted with real tools, and they can tell when something is babyish. Adventure worlds are **places with work to do**. The player is a member of a crew. A skill is a site on the map; a level is a job at that site; what they build or measure becomes part of the place.

The feel: a well-kept field camp. Sturdy, crafty, a little rugged. Tools that click. Paper maps, string, stakes, flags. Nothing is cute for its own sake; things are charming because they are well made. The math is never dressed up as something else: tiles are tiles, a protractor is a protractor, and the crew talks about them by name.

## 2. Identity slots

Every Adventure world fills these slots and nothing else. Everything outside the slots is shared by the tier.

| Slot | What the identity supplies |
|------|---------------------------|
| `name`, `place` | Working name and a two-sentence description of the place |
| `regions` | One named region per curriculum domain present in the world, with a landmark icon |
| `crew` | The crew's role and the voice of its lead (one line of character) |
| `palette.world` | Four accent colors: primary, secondary, sky, ground (hex, both themes) |
| `skins.kits` | A skin per `build` kit used in the world |
| `skins.instruments` | A skin per `manipulate` instrument used in the world |
| `map` | The map illustration and site icons per region |
| `ambient` | Ambient sound set and a music cue |
| `rewards` | How a finished job becomes part of the place (per region) |

## 3. Shared tokens

Values are design tokens; each platform maps them to its own units and resource system.

### 3.1 Color
Light theme first; dark values alongside. Dark mode exists for play in dim rooms and must keep the same contrast relationships.

| Token | Light | Dark | Role |
|-------|-------|------|------|
| `bg` | `#F3F4F0` | `#161B21` | Page ground, a chalky off-white biased cool so the warm world accents read as objects on it |
| `surface` | `#FFFFFF` | `#1F262E` | Cards, trays, sheets |
| `ink` | `#1E2A33` | `#E9EEF2` | Text, outlines, map linework |
| `ink-muted` | `#5A6873` | `#9AA8B3` | Secondary text, tick labels |
| `line` | `#D5DBE0` | `#2E3944` | Hairlines, grid lines |
| `accent` | `#FF6A1A` | `#FF8A4A` | The one interactive highlight: selected piece, active handle, Lock it in. Survey-flag orange. |
| `ok` | `#2F8F5B` | `#5CC48C` | Satisfied, aligned, snapped |
| `attention` | `#D9A400` | `#F2C94C` | "Look here" overlay on unsatisfied lock-in; never red |
| `star` | `#F5B700` | `#FFD23F` | Stars and flags |

World accents (`palette.world`) are four more tokens per identity (§6). They color the place and the pieces, never the UI chrome. Semantic colors never double as world accents.

Contrast: `ink` on `bg` and `surface` ≥ 7:1; `ink-muted` ≥ 4.5:1; any text on `accent` is `ink` at ≥ 4.5:1.

### 3.2 Type
| Token | Face | Fallback | Use |
|-------|------|----------|-----|
| `type.display` | Rubik, 700 | system sans, bold | Site names, level titles, star moments |
| `type.ui` | Atkinson Hyperlegible, 400/700 | system sans | Targets, readouts, sheets, portal |
| `type.numeral` | Rubik, 500, tabular figures | system sans, tabular | Every number the player reads: readouts, ticks, dimensions |

Scale (density-independent px): display 28 / 22, ui 18 / 16, numeral 20 in readouts and 14 on ticks, caption 13. Targets are set in `type.ui` 18 with numerals in `type.numeral` 20 so the numbers stand out. Line height 1.3. No all-caps labels except region names on the map, with 0.06em tracking.

Both faces are open-licensed and bundled; no runtime font download.

### 3.3 Layout
- Touch targets ≥ 48 × 48; pieces in a kit tray ≥ 56.
- A level is landscape on phones (the workspace needs width for a 23-wide rectangle); landscape or portrait on tablets. Maps, sheets, and the portal adapt to either.
- Level regions: **target strip** along the top (target text, read-aloud, hint), **workspace** filling the middle (≥ 60% of the screen), **kit tray** along the bottom on phones and the left edge on tablets, **readout** docked bottom-right above **Lock it in**, which sits in the right thumb zone. A left-handed option mirrors tray and button.
- Spacing on a 4-unit rhythm: 4, 8, 12, 16, 24, 32. Cards use 12 radius; pieces use 6; the Lock it in button 16.
- Elevation is light: one soft shadow for lifted pieces and sheets; none on chrome.

### 3.4 Motion
| Moment | Duration | Shape |
|--------|----------|-------|
| Piece pickup | 90 ms | Lift 4 px, scale 1.04, shadow appears |
| Piece settle (place, snap) | 150 ms | Ease out with a 1.03 overshoot; detents feel like a shallow click |
| Cut line drawn | follows the finger; snaps in 80 ms |
| Lock it in, satisfied | 420 ms | Pieces settle together, a stamp flashes `ok`, the readout bolds |
| Lock it in, not yet | 300 ms | Target outline fades in over the workspace in `attention`; the differing region pulses twice; nothing shakes |
| Level end stars | 1.2 s total | Stars arrive one at a time with 250 ms between; the third has a longer glow |
| Secret goal reveal | 700 ms | A folded card turns over |
| Map site unlock | 500 ms | Trail draws itself from the prerequisite site |

Ambient motion lives only in the world backdrop (flags, dust, water) and stops under reduced-motion. UI chrome never idles or bounces. Everything above respects the platform's reduce-motion setting by collapsing to a 120 ms crossfade.

### 3.5 Sound and haptics
| Event | Sound | Haptic |
|-------|-------|--------|
| Pickup | Soft wooden tock | light |
| Place | Click | light |
| Snap / detent | Tick, slightly brighter | medium, short |
| Cut | Short saw stroke | none |
| Lock it in, satisfied | A two-note rising stamp | medium |
| Lock it in, not yet | A flat, low "hm"; never a buzzer | none |
| Hint | Page turn | none |
| Star | Chime; the three-star chime adds a third voice | none |
| Secret goal | A distinct four-note discovery motif | light |
| Chest unlock | Latch, then the discovery motif | medium |

Ambient loop on by default at a low level; music cue on at a low level; both mutable from the map and the portal. Spoken targets duck both. Haptics follow the platform setting.

### 3.6 Voice and copy
The crew lead speaks. Short, warm, practical, specific. Never the voice of a test.

- Targets are jobs: "Lay tile for a bed 23 long and 14 wide." Not "What is 23 × 14?"
- Not-yet moments name what to look at, not what went wrong: "Not yet. Look at the short side." Never "incorrect", "wrong", "try again!", or exclamation marks in general.
- Done moments say what the work did: "That bed's ready for planting." Stars come after, with no fanfare copy; the sound and motion carry it.
- Hints are offered as tools, in the crew's voice: "Want the cut lines?"
- Numbers are always read aloud on tap, in the same voice.
- No scores, percentages, or timers are ever shown to the player. Fluency targets are invisible; the only visible time is in the Parent Portal.

### 3.7 Accessibility
- Color is never the only signal: stars count, regions have icons, states have shapes (§world-map).
- Read-aloud on every target and readout; captions for the crew's spoken lines.
- Dyslexia-friendly UI face; numerals never in a decorative face.
- Reduced motion, left-handed mirror, haptics off: all honored.
- Tap targets and hit areas as in §3.3; no gesture needs two hands on a phone; two-finger rotate has a one-finger alternative (a rotate handle).

## 4. Engines in this tier
`build` and `manipulate` dominate worlds 3–5 (26 of 28 world-4 recipes). In Adventure both use **Lock it in** rather than automatic settle, and both show the readout in words and notation side by side. Kit and instrument *behavior* comes from the engine specs; their *appearance* comes from the identity's skins.

## 5. The map in this tier
A hand-drawn expedition map in `ink` on `bg`, with world accents for terrain. Regions are labeled in the display face. Sites are small landmark icons; trails are inked paths. The map is the Adventure tier's hero: the first thing a player sees in a world and where their work accumulates. Mechanics in `specs/world-map.md`.

## 6. World 4 identity: Canyon Camp

**`name`**: Canyon Camp (working name).

**`place`**: An expedition base camp on a ledge in a red-rock canyon. Surveyors measure the cliffs, builders lay stone and plank, kites fly off the rim on windy afternoons. The camp grows as the crew finishes jobs.

**`regions`** (one per domain in world 4):

| Domain | Region | Landmark | Skills that live there |
|--------|--------|----------|------------------------|
| Operations & Algebraic Thinking | **The Yard** | A stamping machine under a tarp | Factors and multiples, patterns and rules, times-as-many, multi-step jobs |
| Number & Operations in Base Ten | **The Supply Tent** | Shelves of crates and pallets | Place value, big numbers, rounding, column adding, multiplying, dividing |
| Fractions | **The Mill** | A saw bench with planks | Equivalence, comparing, decomposing, adding, fraction × whole, tenths and hundredths, decimals |
| Measurement & Data | **Survey Ridge** | A transit on a tripod | Angles, measuring angles, conversions, measurement jobs, line plots, area and perimeter |
| Geometry | **Kite Field** | A kite on the rim | Lines and angles, classifying shapes, symmetry |

**`crew`**: The Survey Crew. The lead is the Surveyor: calm, exact, dry. "Twenty-three by fourteen. Let's see it."

**`palette.world`**:

| Token | Light | Dark | Where |
|-------|-------|------|-------|
| `world.primary` | `#B5602A` | `#D27A41` | Canyon rock, tiles, crates |
| `world.secondary` | `#3F6B50` | `#5E9A74` | Pine, planks, tents |
| `world.sky` | `#4F73A8` | `#2C4468` | Sky bands, water, string |
| `world.ground` | `#E6D7C0` | `#3A3128` | Sand, map terrain |

**`skins.kits`**:

| Kit | Skin |
|-----|------|
| `tiles` | Cut sandstone pavers; a stretched rectangle shows mortar lines; place-value cuts are chalk lines |
| `blocks` | Crates (1), bundles of ten crates (10), pallets (100), shipping containers (1000); decimal mode: a courtyard of 100 paving stones, strips of ten |
| `parts` | Planks cut at the Mill; sectors are slices of a canvas tarp |
| `strokes` | Survey stakes and string on a peg board; rays are string run off the board |
| `marks` | Flags clipped to a measuring tape |
| `gears` | The Yard's stamping machine: a start crate and operation stamps on a rail |
| `units` | Coils of rope (length), sacks (mass), canteens (volume), an hourglass rail (time) |

**`skins.instruments`**:

| Instrument | Skin |
|------------|------|
| `number-line` | A surveyor's measuring tape that pinches to reveal finer marks |
| `fraction-bar` | A plank on the saw bench; the slicer is the saw; the shade is paint |
| `grid` | The paved courtyard (hundredths); the peg board with a mirror pole for symmetry |
| `angle` | The transit on Survey Ridge: two sight arms, a brass protractor ring; the crane boom for building angles |
| `place-chart` | Supply Tent shelving: digits are crates on shelves labeled by place; the lever is a hoist |

**`map`**: A ledge seen from above, the five regions arranged around the camp's central fire: Supply Tent and Yard by the fire, the Mill downstream, Survey Ridge up the slope, Kite Field on the rim. Site icons are small ink drawings in the regional landmark's family.

**`ambient`**: Canyon wind, distant hammering, a river below, ravens. Music cue: a slow, open guitar figure.

**`rewards`** (what a finished job becomes):
- The Yard: stamped parts stack in a crate by the machine.
- Supply Tent: shelves fill; a full shelf gets a hand-lettered label.
- The Mill: planks join a growing footbridge across the stream.
- Survey Ridge: survey flags appear on the cliff; a completed set draws a contour line on the map.
- Kite Field: finished shapes become kites that fly on the rim; symmetric ones fly straight.

## 7. Worlds 3 and 5: identity sketches

**World 3: Harbor Market** (working name). A fishing harbor with a market: crates in equal rows on the dock, catch shared out fairly, baskets of fruit cut into equal parts, a clock tower, a tide chart. Regions: the Dock (multiplication and division), the Counting House (place value, rounding), the Market (fractions), the Harbormaster's Office (time, mass, volume, graphs, area), the Boatyard (shapes). Crew: dockhands; the lead is the Harbormaster, brisk and cheerful. Palette: teal water, rope tan, net green, lantern yellow.

**World 5: High Station** (working name). A mountain weather and observing station: instruments reading to thousandths, water tanks (volume), a star chart (coordinate plane), a telescope that zooms by powers of ten. Regions: the Instrument Room (decimals, powers of ten), the Tank House (volume), the Chart Room (coordinates, patterns), the Galley (fraction operations), the Rock Garden (classifying shapes). Crew: station keepers; the lead is the Keeper, quiet and curious. Palette: snow white, slate, cold blue, lamp amber.

Both get full slot fills when their worlds are built.

## 8. Acceptance criteria
- A screenshot of any world-4 level with the target hidden is recognizably Canyon Camp and recognizably the math (the engine fun check holds under the skin).
- Every token in §3 is defined for both themes; no component uses a literal color.
- Every slot in §2 is filled for world 4; no engine or recipe references a slot's content directly.
- Reduced motion, left-handed mirror, and read-aloud work on every level screen.
- No screen the player sees shows a score, percentage, timer, or the word "test".

## 9. Open questions
- Whether the crew lead is voiced (recorded lines) or text-to-speech in v1. Recorded is better and costs more; v1 can ship text-to-speech with the copy written to be read aloud.
- Whether world accents should also tint the kit tray and readout chrome lightly, to make worlds feel more different at a glance. v1: no; the workspace carries the identity.
