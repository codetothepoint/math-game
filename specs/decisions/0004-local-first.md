# ADR 0004: Local-first, no accounts in v1

Status: **accepted** — 2026-10-19

## Context
The vertical slice and the first release are for a single device shared by a household. The parent portal needs a PIN, a few profiles, rewards, and stats. None of that needs a server. Accounts bring sign-up, privacy obligations for children's data, and sync conflicts that a first release does not need to solve.

## Decision
- All player state, portal settings, and secrets live on the device. Nothing is sent anywhere.
- The data model is the mastery model's state (`specs/mastery.md` §2) plus a session log (start, end, levels completed, world) plus portal entities (profiles, rewards, settings, PIN and recovery-phrase hashes).
- Persistence is a versioned local store with a migration path; the on-disk format is an implementation detail, but a player's data must export to and import from a single self-describing JSON file so a household can move devices.
- The only network use in v1 is none. The app works fully offline and requests no network permission.

## Consequences
- No sign-in screens, no privacy policy for data in transit, no backend to run.
- Losing the device loses the data unless the guardian exported it. The portal says so once, at first launch, next to the recovery phrase.
- Sync, accounts, and teacher mode are future ADRs; the export format is the bridge to them.
- The curriculum, recipes, and identities ship inside the app; updating content means updating the app.
