# Mechanics

Mini-game engines, the recipes that bind them to curriculum nodes, and the visual language of each sophistication tier.

## How the pieces fit
```
curriculum node  --(engines: [build, manipulate])-->  recipe (per node, per engine)  -->  engine  -->  level
                                                        kit/instrument, target type,
                                                        generator parameters, strategy
                                                        detection, hint ladder, secret goals
```
- An **engine** is a generic, reusable game mechanic with its own spec (`engine-*.md`). It knows nothing about any particular node.
- A **recipe** binds one node to one engine: which kit or instrument, which target type, how to generate items, which strategies the engine should watch for, and which secret goals it can evaluate. Recipes live in `recipes/world-NN.yaml` and are validated by `tools/recipes.py`.
- A **level** is what the engine produces from a recipe, a representation, a difficulty band, and the revisit items the scheduler hands it (`specs/mastery.md` §7).

## Engines
| Engine | What the player does | Hosts skills like |
|--------|----------------------|-------------------|
| `build` | Assemble pieces from a kit into a workspace until a target is satisfied | Arrays, area models, place-value blocks, fraction pieces, shapes on a geoboard, line plots, rule machines |
| `manipulate` | Grab and adjust an existing instrument until it reads a target | Number lines, fraction bars, angle arms, place-value charts, grids, balances, graphs |
| `story` | Word problems as situations the player is actually in | Rates, proportions, systems, optimization |
| `fluency` | Fast recall with light, playful pressure, never framed as a test | Facts, conversions, simplifying, basic derivatives |
| `strategy` | Choose *how* to solve, not just the answer | Make-ten vs count-on, factor vs quadratic formula |
| `sort` | Classify, pair, order | Odd/even, equivalent expressions, function families |
| `sim` | Systems with math underneath | Growth, probability, rates of change, limits as physical behavior |

Specs written: `engine-build.md`, `engine-manipulate.md`. The other five follow the same template once the vertical slice has exercised these two.

## Representations
A curriculum node lists which of these it can be practiced in; a recipe lists which of those it can serve; every item has exactly one.

| Representation | The item's content is… | In a touch game it looks like… |
|----------------|------------------------|--------------------------------|
| `physical` | The manipulative itself | Blocks, tiles, fraction pieces, angle arms, counters: things with mass and snap |
| `visual` | A picture or diagram of the idea | A drawn rectangle with labeled sides, a number line, a grid, a plot |
| `symbolic` | Numerals and notation | `23 × 14`, `3/4 = 6/8`, `∠ABC = 65°` as the target and as the readout |
| `verbal` | Words | "Make a number with four hundreds and two tens"; a one-line situation |

The same engine serves several representations by changing what the *target* and *readout* are, not the mechanic: in `build` with tiles, a `symbolic` item shows `23 × 14` and the readout counts in notation; a `visual` item shows an outlined rectangle to fill; a `verbal` item says "Build a garden bed 23 long and 14 wide."

## Difficulty bands
Within a node, item difficulty follows the player's mastery score: **low** below 0.4, **mid** from 0.4 to 0.75, **high** at 0.75 and above. Recipes may give generator parameters per band; a recipe with one parameter set serves all bands with it.

## Tiers
| Tier | Worlds | Style spec |
|------|--------|------------|
| Playground | 1–2 | `tier-playground.md` |
| Adventure | 3–5 | `tier-adventure.md` |
| Workshop | 6–8 | `tier-workshop.md` |
| Studio | 9–10 | `tier-studio.md` |
| Lab | 11–12 | `tier-lab.md` |

Each tier spec defines palette, type, motion, sound, UI density, tone of voice, and which engines dominate. The rule from the constitution: a game about addition must look and feel different from a game about limits.

## Engine spec template
Every `engine-*.md` has these sections, in this order:
1. **Core verb and feel** — what the player physically does; why it is fun with the stars turned off
2. **Anatomy of a level** — target, workspace, readout, completion; items within a level
3. **Kits / instruments** — the piece families or controls, with snapping rules
4. **Targets** — target types and their satisfaction rules
5. **Gestures** — the input vocabulary (tap, drag, long-press, pinch, two-finger rotate), never platform APIs
6. **Feedback and hints** — what the player sees when a construction does not satisfy; the hint ladder
7. **Signals** — how `ItemResult` fields are produced; which strategies the engine can detect and how; what it cannot observe
8. **Representations** — how each supported representation changes target and readout
9. **Tier variations** — one worked example per tier
10. **Worked examples** — deep examples for the vertical-slice world
11. **Recipe contract** — the fields a recipe for this engine must supply
12. **Acceptance criteria** — fun check, tracking, tier fit, data contract, plus engine-logic test cases
13. **Open questions**
