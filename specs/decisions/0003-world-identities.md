# ADR 0003: One identity per world, themes kept possible

Status: **accepted** — 2026-10-19

## Context
Worlds must look and feel different as the math matures (constitution §3, §5). Two models were on the table: one fixed identity per world, or player-selectable theme skins (as in Math Monsters). Fixed identities let art carry the math and give each world a real sense of place; skins give choice but spread art across variants.

## Decision
- **Each world has exactly one identity in v1**, and every world is a **place**: somewhere with a job to do, not a story with a quest line.
- An identity fills a fixed set of **slots** defined by its tier spec: name, place description, palette accents, piece and instrument skins per kit and instrument, map art, ambient sound set, crew voice. The tier spec owns everything outside the slots (type, layout density, motion rules, UI chrome, interaction sounds, copy rules).
- A **theme** is an alternate identity for the same world: a second complete slot fill. Nothing ships with themes, but no spec, recipe, or engine may reference an identity's assets directly; they reference slots. That keeps themes possible without designing for them now.
- Identity names in specs are working names until branding.

## Consequences
- Engine and recipe specs stay identity-free. A recipe's `frame` line is a suggestion the identity may rephrase.
- Each tier spec defines the slot list; each world's identity section fills it.
- Adding a theme later is content work, not engineering, as long as the slot discipline holds.
- The parent portal may expose a theme picker later; for v1 it does not.
