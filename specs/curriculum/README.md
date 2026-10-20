# Curriculum

The curriculum is **data**, not prose. It is a directed acyclic graph of skill nodes spanning worlds 1–12. Games, levels, star thresholds, and revisit schedules are generated from it.

## Source
- Backbone: Common Core State Standards for Mathematics (CCSSM) domains and the K–12 progressions. Standards are referenced by their identifiers (e.g. `3.OA.A.1`) so the graph can be checked against them.
- Not a boundary: nodes may exist with no CCSSM reference when they make the game or the math better. Those nodes carry `source: original`.

## Layout
```
specs/curriculum/
  README.md            this file
  skill.schema.yaml    schema every node must satisfy
  worlds/
    world-01.yaml      1st grade
    ...
    world-12.yaml      12th grade
```

One file per world. Each file is a list of skill nodes.

## Skill node
See `skill.schema.yaml`. Key fields:

| Field | Purpose |
|-------|---------|
| `id` | Stable identifier, `w{NN}.{domain}.{slug}` |
| `world` | 1–12 |
| `domain` | CCSSM domain code (`OA`, `NBT`, `NF`, `MD`, `G`, `RP`, `NS`, `EE`, `F`, `SP`, `A`, `N`, …) |
| `title` | Player-facing name |
| `description` | One sentence, what the player can do |
| `ccss` | List of standard ids this node covers (may be empty) |
| `prerequisites` | List of node ids that must reach `unlock_threshold` |
| `representations` | Which forms this skill is practiced and checked in: `symbolic`, `visual`, `verbal`, `physical` |
| `engines` | Which mini-game engines can host this skill |
| `mastery` | Thresholds: `unlock`, `two_star`, `three_star`, `fluency_ms`, `revisit_days` |
| `strategies` | Named strategies the game can observe or reward (e.g. `make-ten`, `doubles`, `distribute`) |
| `secret_goals` | Candidate hidden goals for this node |

## Rules
- The graph must be acyclic. A validator will enforce this.
- Every node in world N may have prerequisites in any world ≤ N.
- Every node must have at least one engine and at least two representations.
- Mastery thresholds are per node; there is no global pass mark.

## Authoring plan
1. Write the schema.
2. Author worlds 1–5 from the CCSSM K–5 progressions (counting, operations, base ten, fractions, measurement and data, geometry).
3. Author worlds 6–8 (ratios, number system, expressions and equations, functions, statistics and probability, geometry).
4. Author worlds 9–12 from the high school conceptual categories (number and quantity, algebra, functions, modeling, geometry, statistics and probability), including the + standards for advanced work.
5. Add original nodes where the games want them.
6. Validate: acyclic, all prerequisites exist, every node reachable from a world's entry nodes.
