# ADR 0002: Curriculum is data

Status: **accepted**

## Context
The curriculum spans twelve grades and several hundred skills. Levels, star thresholds, revisit schedules, and secret goals all depend on it. Writing it as prose would make it unverifiable and impossible to generate from.

## Decision
The curriculum lives in `specs/curriculum/worlds/*.yaml` as a graph of skill nodes conforming to `specs/curriculum/skill.schema.yaml`. A validator checks the schema, the absence of cycles, and that every prerequisite exists. Game code reads the curriculum data; it does not embed curriculum knowledge.

## Consequences
- Adding or changing a skill is a data change, reviewable in a diff.
- Engines must be data-driven: a node's `engines`, `representations`, `mastery`, and `strategies` fields are the contract.
- Common Core alignment is checkable by querying `ccss` fields.
- Original (non-CCSS) nodes are first-class; they carry `source: original`.
