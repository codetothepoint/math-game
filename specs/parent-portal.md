# Parent Portal

Status: **v1** — the PIN-protected area where a guardian sets up the basics and sees how play is going. Deliberately small; it grows later (§8). Platform-neutral.

The portal is the only place the game shows numbers about a player. It is honest, plain, and written for an adult who has thirty seconds.

## 1. Principles
- A guardian can set up a player in under two minutes without reading anything.
- Stats describe what the player can do, not how they rank. No grades, no percentiles, no comparisons to other children.
- The portal never changes what the player sees except through the three levers it owns: starting world, rewards, and sound.
- Local to the device in v1. Nothing leaves the device (ADR 0004).

## 2. Access

- **Entry**: the "Grown-ups" button on the profile picker or the map corner.
- **PIN**: 4–6 digits, set on first launch before the first profile is created. Entry is a numeric pad; three wrong entries pause the pad for 30 seconds, doubling on each further miss, with no lockout beyond that.
- **Recovery**: at PIN setup the portal shows a one-time **recovery phrase** (four common words) and asks the guardian to write it down. "Forgot PIN" accepts the phrase and lets them set a new PIN. There is no other recovery; the phrase and the PIN are the only secrets, and both live only on the device.
- **Session**: the portal stays open until the guardian leaves it or two minutes pass without a touch.

## 3. First launch
1. Set a PIN; see and confirm the recovery phrase.
2. Create the first player: name, avatar (a small set of crew badges), **starting world** (1–12, with the world names and a one-line description of what each covers).
3. Done. The map for that world opens for the player.

Everything else has defaults.

## 4. Setup basics

### 4.1 Players
- Add, rename, change avatar, remove (with confirmation; removal deletes that player's data).
- Up to six profiles per device.

### 4.2 Starting world
- Per player. Changing it re-runs the assumed rule (`specs/mastery.md` §6.3): worlds before the new start with no evidence become assumed; nothing with evidence changes. The portal says so in one line before confirming.
- The description for each world is the curriculum's world `name` plus its regions' skills in a sentence, so a guardian who does not know what "Grade 4" covers can still pick.

### 4.3 Sound
- Ambient, music, and spoken lines: on or off per device.

### 4.4 Theme
Not in v1. The slot exists (ADR 0003); the portal shows nothing until a second identity ships.

## 5. Rewards

A reward ties a real-world thing the guardian will give to a milestone in the game (`specs/mastery.md` §10). The player sees it as a chest on the map (`specs/world-map.md` §2.3).

### 5.1 Creating one
- **Label** (what the player sees on the tag): free text, up to 24 characters. "Movie night", "Trip to the pool".
- **Milestone**, one of:
  - **Flags at a site** — pick a world, a site, and 1, 2, or 3 flags
  - **Flags in a world** — pick a world and a total (the portal shows the maximum, three per site)
  - **Finish a world** — pick a world (every site at two flags or more)
  - **Secrets found** — a count, optionally within one world
  - **Sites owned** — a count of three-flag sites anywhere
- Up to five locked rewards per player at a time.

### 5.2 Lifecycle
| State | Set by | Seen by the player as |
|-------|--------|-----------------------|
| locked | Guardian creates it | Closed chest with the label |
| unlocked | The game, at level end when the milestone is met | Open, glowing chest |
| claimed | Guardian taps "Delivered" | Open, empty chest with the tag kept |

A guardian can delete a locked reward; an unlocked one can only be claimed.

### 5.3 Notification
When a reward unlocks, the device shows one notification to the guardian: the player's name, the label, and the milestone. It is the only notification the app ever sends. It can be turned off in the portal.

## 6. Stats

One page per player, scannable in thirty seconds, then detail on tap.

### 6.1 Overview
| Item | What it shows |
|------|---------------|
| Current world | Name and how many sites have at least one flag |
| This week | Sessions, minutes played, jobs finished |
| Flags | Total flags, and three-flag sites ("owned") |
| Coming back around | How many sites are due for a revisit (`due`) and how many revisits passed this month |
| Secrets | Found so far |
| Rewards | Locked, unlocked, claimed counts |

### 6.2 By world
For each visited world: a row per region with a small bar of its sites by state (assumed · locked · reach · available 0–3 flags), the world's flag total out of its maximum, and whether it is finished.

### 6.3 By site
Tapping a site shows:
- its description from the curriculum, in plain words
- flags, with what each one meant
- **how it's going**: one of *building* (fewer than two flags), *got it* (two flags), *owns it* (three), in words; it follows the flags, which already carry the star hysteresis, so it never flickers; never the number
- **hints lately**: none · some · lots, from the hint rate over the last ten items
- **ways it was practiced**: the representations with at least three items, named plainly (with blocks, as numbers, in words, from a picture)
- **strategies seen**: the node's strategies the game observed, with a one-line definition each
- last played

### 6.4 What the portal does not show
- Mastery scores, response times, or fluency numbers. (Fluency gates the third star silently.)
- Any comparison to other players, grade-level expectations, or norms.
- Anything about a child the guardian has not opened the profile of.

## 7. Data
- All portal data (PIN hash, recovery phrase hash, profiles, rewards, settings) and all player state live on the device (ADR 0004).
- **Export**: a player's data as a single file the guardian can save or share, for moving to a new device. **Import** restores it. v1 ships export; import follows in the first update that needs it.
- **Delete player** removes everything about that player.

## 8. Later (not v1)
- Guardian accounts and sync across devices.
- Teacher mode: a class of players, assigning starting worlds in bulk, a class view of §6.2.
- Theme picker per player.
- A weekly summary notification, opt-in.
- Recommended "next site" suggestions for the guardian to talk about with the player.

## 9. Acceptance criteria
- First launch to a playable map in under two minutes with no help text read.
- Every stat in §6 can be computed from `NodeState`, the curriculum, and session records; no stat needs data the mastery model does not keep. (`sessions`, `minutes`, and `jobs finished` need a small session log: start, end, levels completed; it is specified in the platform data model.)
- The portal is reachable in two taps from the map and locks itself after two minutes idle.
- The only notification the app sends is §5.3.
