# Mechanics

Mini-game engines and the visual language of each sophistication tier. Each engine gets its own spec with acceptance criteria. Each tier gets a style spec.

## Engines
| Engine | What the player does | Hosts skills like |
|--------|----------------------|-------------------|
| `fluency` | Fast recall with light, playful pressure — never framed as a test | Facts, conversions, simplifying, derivatives of basic functions |
| `build` | Construct something with math | Arrays, shapes, equivalent fractions, functions that fit a target |
| `sort` | Classify, pair, order | Odd/even, equivalent expressions, function families |
| `manipulate` | Grab and move representations | Number lines, fraction bars, algebra tiles, graphs, vectors |
| `story` | Word problems as situations the player is actually in | Rates, proportions, systems, optimization |
| `strategy` | Choose how to solve, not just the answer | Make-ten vs count-on, factor vs quadratic formula, substitution vs elimination |
| `sim` | Systems with math underneath | Growth, probability, rates of change, limits as physical behavior |

Specs to write: `engine-fluency.md`, `engine-build.md`, … one per engine.

## Tiers
| Tier | Worlds | Style spec |
|------|--------|------------|
| Playground | 1–2 | `tier-playground.md` |
| Adventure | 3–5 | `tier-adventure.md` |
| Workshop | 6–8 | `tier-workshop.md` |
| Studio | 9–10 | `tier-studio.md` |
| Lab | 11–12 | `tier-lab.md` |

Each tier spec defines palette, type, motion, sound, UI density, tone of voice, and which engines dominate. The rule from the constitution: a game about addition must look and feel different from a game about limits.

## Acceptance criteria template
Every engine spec ends with:
- **Fun check:** would a player choose this with the stars turned off?
- **Tracking:** which mastery signals does the engine emit (accuracy, time, hints, strategy, representation)?
- **Tier fit:** which tiers can host it, and how does it change across them?
- **Data contract:** what it needs from a curriculum node to generate a level.
