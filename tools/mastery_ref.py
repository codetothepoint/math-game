"""Reference implementation of specs/mastery.md.

Pure functions over player state. No platform code, no persistence, no UI.
Times are floats in days so tests can say "two days later" plainly.

Run the scenarios:  python3 tools/test_mastery.py
"""
from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path
from statistics import median
from typing import Optional

# ---- Tuning knobs (specs/mastery.md §13) ----
K = {
    "ALPHA_CORE": 0.15,
    "ALPHA_REVISIT": 0.20,
    "MISS_DAMPING": 0.5,
    "SLOW_FACTOR": 3,
    "RECENT_N": 10,
    "HINT_RATE_TWO_STAR": 0.3,
    "REP_MIN_ITEMS": 3,
    "STAR_HYSTERESIS": 0.15,
    "STAR_HYSTERESIS_THREE": 0.10,
    "ALPHA_REACH": 0.08,
    "INTERLEAVE_MAX": 0.2,
    "REVISIT_SET_SIZE": 5,
    "REVISIT_PASS_CORRECT": 4,
}


# ---- Curriculum ----
@dataclass
class NodeDef:
    id: str
    world: int
    prerequisites: list[str]
    representations: list[str]
    engines: list[str]
    strategies: list[str]
    mastery: dict  # unlock, two_star, three_star, revisit_days, optional fluency_ms


def load_curriculum(path: Optional[Path] = None) -> dict[str, NodeDef]:
    path = path or Path(__file__).resolve().parents[1] / "specs" / "curriculum" / "generated" / "curriculum.json"
    data = json.loads(path.read_text())
    return {
        n["id"]: NodeDef(n["id"], n["world"], n["prerequisites"], n["representations"], n["engines"], n["strategies"], n["mastery"])
        for n in data["nodes"]
    }


# ---- Signals (§3) ----
@dataclass
class ItemResult:
    node_id: str
    representation: str
    engine: str
    correct: bool
    first_try: bool = True
    hints: int = 0
    response_ms: Optional[int] = None
    strategy: Optional[str] = None
    kind: str = "core"  # core | revisit | reach
    at: float = 0.0


@dataclass
class LevelResult:
    node_id: str
    engine: str
    representation: str
    completed: bool
    items: list[ItemResult]
    observes_strategies: bool = False
    secret_goals_hit: list[str] = field(default_factory=list)
    started_at: float = 0.0
    ended_at: float = 0.0


# ---- State (§2) ----
@dataclass
class RevisitState:
    due_at: float
    interval_index: int = 0
    representation: Optional[str] = None
    served: int = 0
    correct: int = 0
    hints: int = 0
    passed: int = 0
    last_set_passed: Optional[bool] = None
    history: list[tuple[float, bool]] = field(default_factory=list)  # (at, passed) per completed set


@dataclass
class NodeState:
    score: float = 0.0
    assumed: bool = False
    stars: int = 0
    items: int = 0
    recent: list[ItemResult] = field(default_factory=list)
    rep_items: dict[str, int] = field(default_factory=dict)
    strategies: set[str] = field(default_factory=set)
    strategy_observable: bool = False
    fluency_ms: Optional[int] = None
    clean_times: list[int] = field(default_factory=list)
    levels_completed: int = 0
    last_level_representation: Optional[str] = None
    secret_goals: set[str] = field(default_factory=set)
    two_star_at: Optional[float] = None
    three_star_at: Optional[float] = None
    revisit: Optional[RevisitState] = None
    last_seen: Optional[float] = None


@dataclass
class Profile:
    starting_world: int
    nodes: dict[str, NodeState]


class Mastery:
    def __init__(self, curriculum: dict[str, NodeDef]):
        self.c = curriculum

    # ---- Profiles and starting world (§6.3) ----
    def new_profile(self, starting_world: int) -> Profile:
        p = Profile(starting_world, {nid: NodeState() for nid in self.c})
        self.set_starting_world(p, starting_world)
        return p

    def set_starting_world(self, p: Profile, world: int) -> None:
        p.starting_world = world
        for nid, nd in self.c.items():
            s = p.nodes[nid]
            if nd.world < world and s.items == 0:
                s.assumed = True
                s.score = nd.mastery["unlock"]
                s.stars = 0
            elif nd.world >= world and s.assumed:
                s.assumed = False
                s.score = 0.0
                s.stars = 0

    # ---- Evidence and score (§4) ----
    def evidence(self, nd: NodeDef, item: ItemResult) -> float:
        if not item.correct:
            return 0.0
        e = 1.0 if (item.first_try and item.hints == 0) else (0.6 if item.hints == 0 else 0.4)
        fm = nd.mastery.get("fluency_ms")
        if fm and item.response_ms is not None and item.response_ms > K["SLOW_FACTOR"] * fm:
            e = min(e, 0.8)
        return e

    @staticmethod
    def _move(score: float, e: float, alpha: float) -> float:
        if e < score:
            alpha *= K["MISS_DAMPING"]
        return score + alpha * (e - score)

    def apply_item(self, p: Profile, item: ItemResult) -> None:
        nd = self.c[item.node_id]
        s = p.nodes[item.node_id]
        e = self.evidence(nd, item)

        # Bookkeeping common to every kind of item.
        s.assumed = False
        s.items += 1
        s.recent.append(item)
        del s.recent[: -K["RECENT_N"]]
        s.rep_items[item.representation] = s.rep_items.get(item.representation, 0) + 1
        s.last_seen = item.at
        if item.strategy:
            s.strategies.add(item.strategy)
        if item.correct and item.hints == 0 and item.response_ms is not None:
            s.clean_times.append(item.response_ms)
            del s.clean_times[: -K["RECENT_N"]]
            if len(s.clean_times) >= 3:
                s.fluency_ms = int(median(s.clean_times))

        if item.kind == "revisit":
            self._revisit_item(p, nd, s, item)
            return

        s.score = self._move(s.score, e, K["ALPHA_CORE"])

        # Reach credit (§6.2): a clean item credits unmet direct prerequisites.
        if item.kind == "reach" and e == 1.0:
            for pid in nd.prerequisites:
                if not self.is_met(p, pid):
                    ps = p.nodes[pid]
                    ps.score = ps.score + K["ALPHA_REACH"] * (1 - ps.score)

    def finish_level(self, p: Profile, level: LevelResult) -> None:
        s = p.nodes[level.node_id]
        if level.completed:
            s.levels_completed += 1
            s.last_level_representation = level.representation
            if level.observes_strategies:
                s.strategy_observable = True
            s.secret_goals.update(level.secret_goals_hit)
        touched = {level.node_id} | {i.node_id for i in level.items}
        for nid in touched:
            self.recompute_stars(p, nid, level.ended_at)

    # ---- Stars (§5) ----
    def recompute_stars(self, p: Profile, nid: str, now: float) -> None:
        nd, s, m = self.c[nid], p.nodes[nid], self.c[nid].mastery
        stars = s.stars
        if s.levels_completed >= 1:
            stars = max(stars, 1)

        recent = s.recent
        hint_rate = (sum(1 for i in recent if i.hints > 0) / len(recent)) if recent else 1.0
        reps_ok = sum(1 for v in s.rep_items.values() if v >= K["REP_MIN_ITEMS"]) >= 2
        strat_ok = (not nd.strategies) or (not s.strategy_observable) or bool(s.strategies)
        two_ok = s.score >= m["two_star"] and hint_rate <= K["HINT_RATE_TWO_STAR"] and reps_ok and strat_ok

        if stars >= 2 and s.score < m["two_star"] - K["STAR_HYSTERESIS"]:
            stars = 1
        elif stars == 1 and two_ok:
            stars = 2
            if s.two_star_at is None:
                s.two_star_at = now
                s.revisit = RevisitState(due_at=now + m["revisit_days"][0])

        if stars == 3 and s.score < m["three_star"] - K["STAR_HYSTERESIS_THREE"]:
            stars = 2
        if stars == 2:
            zero_hints = all(i.hints == 0 for i in recent)
            fm = m.get("fluency_ms")
            fl_ok = fm is None or (s.fluency_ms is not None and s.fluency_ms <= fm)
            rv = s.revisit
            rv_ok = rv is not None and rv.passed >= 1 and rv.last_set_passed is True
            if s.score >= m["three_star"] and zero_hints and fl_ok and rv_ok:
                stars = 3
                s.three_star_at = s.three_star_at or now
        s.stars = stars

    # ---- Availability (§6) ----
    def is_met(self, p: Profile, nid: str) -> bool:
        s = p.nodes[nid]
        return s.assumed or s.score >= self.c[nid].mastery["unlock"]

    def availability(self, p: Profile, nid: str) -> str:
        nd, s = self.c[nid], p.nodes[nid]
        if s.levels_completed >= 1:
            return "available"
        if all(self.is_met(p, q) for q in nd.prerequisites):
            return "available"
        if all(self.is_met(p, q) or self._available_no_recursion(p, q) for q in nd.prerequisites):
            return "reach"
        return "locked"

    def _available_no_recursion(self, p: Profile, nid: str) -> bool:
        nd, s = self.c[nid], p.nodes[nid]
        return s.levels_completed >= 1 or all(self.is_met(p, q) for q in nd.prerequisites)

    # ---- Revisits (§7) ----
    def is_due(self, p: Profile, nid: str, now: float) -> bool:
        rv = p.nodes[nid].revisit
        return rv is not None and now >= rv.due_at

    def choose_transfer_representation(self, p: Profile, nid: str, servable: Optional[set] = None) -> Optional[str]:
        """Least-drilled representation among those some recipe for the node serves (§7.3); None if none is servable."""
        nd, s = self.c[nid], p.nodes[nid]
        allowed = [r for r in nd.representations if servable is None or r in servable]
        if not allowed:
            return None
        reps = sorted(allowed, key=lambda r: (s.rep_items.get(r, 0), r == s.last_level_representation))
        return reps[0]

    def due_revisits(self, p: Profile, now: float, engine: str, exclude: Optional[str] = None, core_items: int = 0,
                     servable=None, can_host=None) -> list[tuple[str, str]]:
        """Nodes due for a revisit that this engine can host, most overdue first, as (node_id, representation).

        servable(nid) -> set of representations any recipe for the node serves (default: all of the node's).
        can_host(nid, engine, rep) -> whether this engine's recipe serves rep (default: True).
        Both filters run before the interleave cap (§7.2).
        """
        out = []
        for nid, nd in self.c.items():
            if nid == exclude or engine not in nd.engines or not self.is_due(p, nid, now):
                continue
            rv = p.nodes[nid].revisit
            if rv.representation is None:
                rv.representation = self.choose_transfer_representation(p, nid, servable(nid) if servable else None)
            rep = rv.representation
            if rep is None or rep not in nd.representations:
                continue
            if can_host is not None and not can_host(nid, engine, rep):
                rv.representation = None  # a representation no recipe serves is never kept (§7.3): a later call re-chooses
                continue
            out.append((rv.due_at, nid, rep))
        out.sort()
        limit = max(1, int(K["INTERLEAVE_MAX"] * core_items)) if core_items else len(out)
        return [(nid, rep) for _, nid, rep in out[:limit]]

    def _revisit_item(self, p: Profile, nd: NodeDef, s: NodeState, item: ItemResult) -> None:
        rv = s.revisit
        if rv is None:
            return
        rv.served += 1
        rv.correct += 1 if item.correct else 0
        rv.hints += item.hints
        if rv.served < K["REVISIT_SET_SIZE"]:
            return
        passed = rv.correct >= K["REVISIT_PASS_CORRECT"] and rv.hints == 0
        days = nd.mastery["revisit_days"]
        if passed:
            rv.passed += 1
            rv.interval_index = min(rv.interval_index + 1, len(days) - 1)
            s.score = s.score + K["ALPHA_REVISIT"] * (1 - s.score)
        else:
            if s.stars == 3:
                s.stars = 2
            rv.interval_index = 0
            s.score = s.score + K["ALPHA_REVISIT"] * K["MISS_DAMPING"] * (rv.correct / K["REVISIT_SET_SIZE"] - s.score)
        rv.last_set_passed = passed
        rv.history.append((item.at, passed))
        rv.due_at = item.at + days[rv.interval_index]
        rv.served = rv.correct = rv.hints = 0
        rv.representation = None

    # ---- Map state (§11) and milestones (§10) ----
    def map_state(self, p: Profile, nid: str, now: float) -> dict:
        s = p.nodes[nid]
        state = "assumed" if s.assumed else self.availability(p, nid)
        return {"state": state, "stars": s.stars, "due": self.is_due(p, nid, now)}

    def world_stars(self, p: Profile, world: int) -> int:
        return sum(p.nodes[nid].stars for nid, nd in self.c.items() if nd.world == world)

    def world_complete(self, p: Profile, world: int) -> bool:
        return all(p.nodes[nid].stars >= 2 for nid, nd in self.c.items() if nd.world == world)

    def secret_goal_count(self, p: Profile, world: Optional[int] = None) -> int:
        return sum(len(p.nodes[nid].secret_goals) for nid, nd in self.c.items() if world is None or nd.world == world)

    def mastered_count(self, p: Profile) -> int:
        return sum(1 for s in p.nodes.values() if s.stars == 3)
