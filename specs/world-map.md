# World Map and Player Flow

Status: **v1** — every screen the player sees outside a level, and how the mastery model's states appear without ever looking like a test. Platform-neutral. Appearance per tier comes from the tier spec; this document is mechanics and content.

## 1. Flow

```
Launch ─▶ Profile picker (only if more than one profile) ─▶ World map (the profile's current world)
                                                                 │
                                           ┌─────────────────────┼──────────────────────┐
                                           ▼                     ▼                      ▼
                                      Site sheet            World overview         Grown-ups (PIN) ─▶ Parent Portal
                                           │
                                           ▼
                                        Level ─▶ Level end ─▶ back to the map (the site updated)
```

- The player's **current world** is the one they last played in; the map opens there.
- **Back** from a level mid-way confirms ("Leave this job? Your work here stays.") and returns to the map; unfinished items are discarded (`specs/mastery.md` §13).
- There is no main menu. The map is home.

## 2. The map

One map per world. It shows every skill node in the world as a **site**, grouped into **regions** by domain (the identity names them; `tier-adventure.md` §6 for world 4). Prerequisite links between nodes in the same world are **trails**. Prerequisites in earlier worlds are not drawn; they are the trail into the world (§6).

The map is not a graph diagram. A nine-year-old should read it as a place with things to do, and the structure should be *felt*: you walk the trail from a site you finished to the next one.

### 2.1 Site states
Derived from `specs/mastery.md` §11. Each state has a distinct shape, not only a color.

| Map state | Looks like | Tap does |
|-----------|-----------|----------|
| **assumed** | A finished-looking site drawn in muted ink, no flags, a small "already built" mark | Opens the site sheet; playable like any available site |
| **locked** | A faint outline where a site will be; no trail reaches it yet | Shows which sites lead here (their trails glow) and nothing else |
| **reach** | The site drawn fully but the trail to it dotted | Opens the site sheet with a note: "You can try this. It leans on [site] and [site]." |
| **available**, 0 flags | A full site with an empty flagpole | Opens the site sheet |
| **available**, 1–3 flags | Flags on the pole, one per star | Opens the site sheet |
| **due** (overlay) | A lantern lit at the site | Opens the site sheet; the sheet says the crew could use a hand here |

*Due* is an invitation. The lantern is small and warm; it never counts down, never badges the app, never blocks anything. Revisits also happen inside other levels whether or not the player goes back (`specs/mastery.md` §7.2); the lantern just lets a player who wants to go straight there do so.

### 2.2 What the map accumulates
The identity's `rewards` slot says what a finished job becomes (planks join a footbridge, flags appear on a cliff). The map shows all of it: a world the player has worked in looks worked in. This is the reward loop that does not need stars.

### 2.3 Chests
Parent rewards (`specs/parent-portal.md` §5) appear on the map as **chests** placed at the milestone:
- `node_stars` — at that site
- `world_stars`, `world_complete`, `secret_goals(world)`, `mastered` — at the camp (the world's central landmark)

| Chest state | Looks like | Tap shows |
|-------------|-----------|-----------|
| **locked** | A closed chest with the parent's label on a tag ("Movie night") | The label and the milestone in kid terms: "Three flags at the Mill" |
| **unlocked** | Open, glowing | "You earned it. Ask your grown-up." |
| **claimed** | Open, empty, the tag kept | The label, as a memory |

Unlocking happens at level end (§5). The portal is told (`specs/parent-portal.md` §5.3).

## 3. World overview

A pull-back from any map shows all twelve worlds as destinations along a route. Each is one of:
- **visited** — the player has played there; tap to travel
- **open** — at least one node is available or reach; tap to travel
- **ahead** — nothing available yet; drawn in the distance; tap shows "The trail to [world] starts at [site]" naming the nearest reach-state site in an earlier world
- **behind the start** — worlds before the parent-set starting world; all assumed; tap to travel (they are playable)

Travel is free in every direction the states allow. A player can always go back.

## 4. Site sheet

A sheet over the map for one node.

| Part | Content |
|------|---------|
| Title | The node's `title` in the display face; the region name as an eyebrow |
| Flags | 0–3, with the next one's meaning in one line: ★ "Finish a job here" · ★★ "Build it clean, more than one way" · ★★★ "Comes with time" |
| Jobs | One button per recipe the node has for a specified engine, named by its `frame` line in the identity's words ("Lay tile", "Measure the ramp"). The player picks the job. |
| Note | For **reach**: "You can try this. It leans on [titles]." For **assumed**: "Already built. Want to check it?" For **due**: "The crew could use a hand here." |
| Secrets | "Secrets found: 1 of 2" — counts only; never what they are until found |
| Leads to | The titles of up to three dependents in this world, so the trail ahead is visible |

No scores, no percentages, no timers (`tier-adventure.md` §3.6).

### 4.1 Choosing the job
- Representation rotation is per node (`specs/mastery.md` §8) and applies within whichever job the player picks.
- The difficulty band comes from the node's score; the player never sees it.
- The scheduler attaches due revisit items that fit the chosen engine and up to one assumed-node item (`specs/mastery.md` §7.2, §7.5). They are indistinguishable from core items.

## 5. Level end

In order, with the motion and sound from the tier spec:
1. **The work lands.** The construction or setting from the last item becomes part of the place (the identity's `rewards` slot). One line from the crew lead about the work. No "Level complete".
2. **Flags.** If a star was earned this level, it rises on the site's pole on the sheet. Nothing is shown if none was earned; the sheet simply updates.
3. **Secrets.** Each secret goal achieved this level turns over as a card: its `text`. Unachieved ones stay as face-down counts.
4. **Chest.** If a milestone was met, the chest unlocks here, before returning to the map.
5. **Next.** Two buttons: "Another job here" and "Back to the map".

Revisit items are never mentioned at level end. If a revisit set passed and a third star rose on *another* site, the map shows it when the player returns; the lantern at that site goes out.

## 6. Entering a world for the first time
- A world becomes **open** the moment any node in it is available or reach (as prerequisites in earlier worlds are met).
- First arrival plays a short establishing moment: the map draws itself, the crew lead names the place in two lines, the first available sites get their flagpoles.
- For the parent-set starting world this is the player's first screen after the profile picker.

## 7. Profile picker
Shown only when the device has more than one profile. Each profile: name, avatar (chosen in the portal), current world name. Tapping opens that profile's map. A small "Grown-ups" button leads to the PIN screen (`specs/parent-portal.md` §2); on a single-profile device the same button sits in the map's corner.

## 8. What the map never does
- Never shows a test, placement, or quiz.
- Never locks a site the player has already played (`specs/mastery.md` §6.1).
- Never nags: no streak counters, no daily goals, no notifications to the player. (Notifications go to the parent, for chests only.)
- Never shows mastery scores, hint counts, or times.

## 9. Acceptance criteria
- Every site state in §2.1 is distinguishable by shape alone, in both themes, on a phone.
- A locked site's tap shows its trails within one tap; no state needs explanation text to be understood after the first world.
- From the map, a player reaches any available site's level in two taps (site, job).
- Level end never exceeds eight seconds with no input; every step is skippable by tap.
- The world-4 map holds 28 sites in five regions legibly on a phone in landscape and portrait.

## 10. Open questions
- Whether "Another job here" should rotate to the other engine automatically when the node has two recipes. v1: no; the sheet reopens with both jobs.
- How much of the map to show a new player at once: the whole world faint, or regions revealing as they unlock. v1: the whole world, locked sites faint, so the place is always whole.
