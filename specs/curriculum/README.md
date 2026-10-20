# Curriculum

The curriculum is **data**, not prose. It is a directed acyclic graph of skill nodes spanning worlds 1–12. Games, levels, star thresholds, and revisit schedules are generated from it.

## Current graph (v1)

| | |
|---|---|
| Skill nodes | 344 (337 Common Core, 7 original) |
| Prerequisite links | 557 (241 cross-world) |
| Standards referenced | 392 |
| Entry nodes (no prerequisites) | 4, all in world 1 |
| Longest prerequisite chain | 36 nodes |

Per world: 24 · 27 · 28 · 28 · 24 · 29 · 24 · 25 · 40 · 31 · 35 · 29.

Validate and rebuild the generated files:
```
python3 tools/curriculum.py          # validate, then write generated/curriculum.json and generated/graph.html
python3 tools/curriculum.py --check  # validate only
```
`generated/graph.html` is a self-contained interactive explorer of the whole graph (tap a skill to trace what it needs and what it unlocks). Both generated files are committed so the graph can be reviewed in a diff.

## Source
- Backbone: Common Core State Standards for Mathematics (CCSSM). Grades 1–8 map one world per grade. Standards are referenced by identifier (e.g. `3.OA.A.1`) so the graph can be checked against them.
- High school: CCSSM does not assign high-school standards to grades. Worlds 9–12 follow the traditional pathway from CCSSM Appendix A: Algebra I (9), Geometry (10), Algebra II (11), and a fourth year (12) of precalculus topics from the (+) standards plus statistics.
- Not a boundary: nodes may exist with no CCSSM reference when they make the game or the math better. Those nodes carry `source: original`. World 12 currently has seven: sequences and limits, limits of functions, continuity, instantaneous rate of change, first derivative rules, accumulation, and parametric/polar curves.
- Modeling (the ★ standards) is treated as a representation (the `story` and `sim` engines) rather than as separate nodes.

## Layout
```
specs/curriculum/
  README.md               this file
  skill.schema.yaml       JSON Schema for a world file
  worlds/world-01.yaml    1st grade
  ...
  worlds/world-12.yaml    4th year of high school
  generated/              built by tools/curriculum.py; do not edit by hand
    curriculum.json       flattened graph with defaults merged, dependents, depth, stats
    graph.html            interactive explorer
```

## World file
```yaml
world: 3
name: Grade 3
tier: adventure
notes: optional authoring notes
defaults:
  mastery: { unlock: 0.6, two_star: 0.75, three_star: 0.9, revisit_days: [2, 7, 21] }
nodes:
  - id: w03.oa.multiply-fluently-100
    title: Multiplication facts
    description: Multiply within 100 fluently, with all products of two one-digit numbers known from memory.
    ccss: [3.OA.C.7]
    prerequisites: [w03.oa.properties-of-multiplication]
    representations: [symbolic, verbal, visual]
    engines: [fluency]
    mastery: { fluency_ms: 3000, three_star: 0.95, revisit_days: [1, 3, 7, 21, 60] }
    strategies: [doubling, tens-neighbor, square-neighbor]
```

## Skill node fields
| Field | Purpose |
|-------|---------|
| `id` | Stable identifier, `w{NN}.{domain}.{slug}` |
| `title` | Short, player-facing name |
| `description` | One sentence: what the player can do when this is mastered |
| `source` | `ccss` (default) or `original` |
| `ccss` | Standard ids covered; empty for original nodes |
| `prerequisites` | Node ids that must reach `mastery.unlock`; same or earlier world only |
| `representations` | At least two of `symbolic`, `visual`, `verbal`, `physical`; the 3-star revisit uses one the player did not drill |
| `engines` | Mini-game engines that can host this skill: `fluency`, `build`, `sort`, `manipulate`, `story`, `strategy`, `sim` |
| `mastery` | Overrides merged over the world defaults: `unlock`, `two_star`, `three_star`, `fluency_ms`, `revisit_days` |
| `strategies` | Named strategies the game can observe or reward |
| `secret_goals` | Hidden goals revealed after completion (`strategy`, `streak`, `no-hints`, `representation`, `shortcut`, `custom`) |

Domain codes: grades 1–8 use `oa`, `nbt`, `nf`, `md`, `g`, `rp`, `ns`, `ee`, `f`, `sp`; high school uses `n`, `a`, `f`, `g`, `s`.

## Rules (enforced by the validator)
- World files conform to `skill.schema.yaml`.
- Ids are unique and their world prefix matches the file.
- Every prerequisite exists and lives in the same or an earlier world.
- The graph is acyclic.
- `ccss` nodes list at least one standard; `original` nodes list none.

## What is deliberately thin in v1
- **Secret goals** exist on roughly a third of nodes. They need an authoring pass once the engines are specified, because a secret goal has to be something an engine can observe.
- **Strategies** are named but not yet defined. A strategy registry (what each one looks like when a player uses it, and which engines can detect it) belongs in `specs/mechanics/`.
- **Mastery thresholds** are tier defaults with a handful of overrides. They should be tuned per node once the mastery model is specified.
- **Kindergarten** is not a world. World 1 entry nodes assume counting to 20, comparing small groups, and naming basic shapes.
