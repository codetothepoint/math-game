"""Executable scenarios from specs/mastery.md §12. Run: python3 tools/test_mastery.py"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from mastery_ref import ItemResult, LevelResult, Mastery, NodeDef, load_curriculum  # noqa: E402

M = {"unlock": 0.6, "two_star": 0.75, "three_star": 0.9, "revisit_days": [2, 7, 21]}


def tiny_curriculum() -> dict[str, NodeDef]:
    return {
        "w01.oa.a": NodeDef("w01.oa.a", 1, [], ["symbolic", "visual"], ["build", "manipulate"], ["make-ten"], M),
        "w01.oa.b": NodeDef("w01.oa.b", 1, ["w01.oa.a"], ["symbolic", "visual"], ["build"], [], M),
        "w02.oa.c": NodeDef("w02.oa.c", 2, ["w01.oa.b"], ["symbolic", "visual"], ["build"], [], M),
        "w02.oa.fast": NodeDef("w02.oa.fast", 2, [], ["symbolic", "verbal"], ["fluency"], [], {**M, "fluency_ms": 3000}),
    }


def play_level(m, p, nid, n, rep, now, *, correct=True, hints=0, strategy=None, kind="core", observes=True, engine="build", response_ms=1000):
    items = []
    for _ in range(n):
        it = ItemResult(nid, rep, engine, correct, first_try=correct, hints=hints, response_ms=response_ms if correct else None, strategy=strategy, kind=kind, at=now)
        m.apply_item(p, it)
        items.append(it)
    m.finish_level(p, LevelResult(nid, engine, rep, True, items, observes_strategies=observes, started_at=now, ended_at=now))
    return items


def serve_revisit_set(m, p, nid, now, correct_count, hints=0, engine="build"):
    """Serve one full revisit set inside a host level and return the representation used."""
    host = "w01.oa.b"
    due = m.due_revisits(p, now, engine, exclude=host, core_items=25)
    assert (nid, due[0][1]) == due[0], f"{nid} should be the due node, got {due}"
    rep = due[0][1]
    items = []
    for i in range(5):
        ok = i < correct_count
        it = ItemResult(nid, rep, engine, ok, first_try=ok, hints=hints if ok else 0, response_ms=1000 if ok else None, kind="revisit", at=now)
        m.apply_item(p, it)
        items.append(it)
    m.finish_level(p, LevelResult(host, engine, "symbolic", True, items, started_at=now, ended_at=now))
    return rep


def approx(a, b, tol=0.002):
    return abs(a - b) <= tol


def test_1_nine_clean_items_two_stars():
    m = Mastery(tiny_curriculum()); p = m.new_profile(1)
    play_level(m, p, "w01.oa.a", 5, "symbolic", now=0.0, strategy="make-ten")
    play_level(m, p, "w01.oa.a", 4, "visual", now=0.0)
    s = p.nodes["w01.oa.a"]
    assert approx(s.score, 0.768), s.score
    assert s.stars == 2, s.stars
    assert s.revisit is not None and s.revisit.due_at == 2.0


def test_2_hint_rate_blocks_two_stars():
    m = Mastery(tiny_curriculum()); p = m.new_profile(1)
    play_level(m, p, "w01.oa.a", 15, "symbolic", now=0.0, strategy="make-ten")   # one representation: stays at 1 star
    assert p.nodes["w01.oa.a"].stars == 1
    play_level(m, p, "w01.oa.a", 4, "visual", now=0.0, hints=1)                   # 4 hinted of the last 10
    s = p.nodes["w01.oa.a"]
    assert s.score >= 0.75, s.score  # score alone would qualify
    assert s.stars == 1, s.stars     # hint rate 0.4 blocks the second star


def test_3_third_star_needs_a_passed_revisit():
    m = Mastery(tiny_curriculum()); p = m.new_profile(1)
    play_level(m, p, "w01.oa.a", 8, "symbolic", now=0.0, strategy="make-ten")
    play_level(m, p, "w01.oa.a", 7, "visual", now=0.0)
    s = p.nodes["w01.oa.a"]
    assert approx(s.score, 0.913), s.score
    assert s.stars == 2
    assert m.due_revisits(p, 0.5, "build") == []          # not due the same day
    assert m.due_revisits(p, 1.9, "build") == []
    rep = serve_revisit_set(m, p, "w01.oa.a", now=2.0, correct_count=4)
    assert s.stars == 3, (s.stars, s.score)
    assert s.revisit.passed == 1 and s.revisit.interval_index == 1 and s.revisit.due_at == 9.0
    assert rep == "visual"  # the less-drilled representation


def test_4_failed_revisit_demotes():
    m = Mastery(tiny_curriculum()); p = m.new_profile(1)
    play_level(m, p, "w01.oa.a", 8, "symbolic", now=0.0, strategy="make-ten")
    play_level(m, p, "w01.oa.a", 7, "visual", now=0.0)
    serve_revisit_set(m, p, "w01.oa.a", now=2.0, correct_count=4)
    s = p.nodes["w01.oa.a"]
    assert s.stars == 3
    serve_revisit_set(m, p, "w01.oa.a", now=9.0, correct_count=2)
    assert s.stars == 2, s.stars
    assert s.revisit.interval_index == 0 and s.revisit.due_at == 11.0
    assert s.revisit.last_set_passed is False
    # Passing again restores it.
    serve_revisit_set(m, p, "w01.oa.a", now=11.0, correct_count=5)
    assert s.stars == 3
    assert [passed for _, passed in s.revisit.history] == [True, False, True]


def test_5_reach_credit():
    m = Mastery(tiny_curriculum()); p = m.new_profile(1)
    p.nodes["w01.oa.a"].score = 0.5
    assert m.availability(p, "w01.oa.b") == "reach"
    assert m.availability(p, "w02.oa.c") == "locked"
    play_level(m, p, "w01.oa.b", 1, "symbolic", now=0.0, kind="reach")
    assert approx(p.nodes["w01.oa.a"].score, 0.54), p.nodes["w01.oa.a"].score
    # A miss on the reach node leaves the prerequisite alone.
    play_level(m, p, "w01.oa.b", 1, "symbolic", now=0.0, kind="reach", correct=False)
    assert approx(p.nodes["w01.oa.a"].score, 0.54)


def test_6_starting_world_four_real_curriculum():
    m = Mastery(load_curriculum()); p = m.new_profile(4)
    for nid, nd in m.c.items():
        s = p.nodes[nid]
        if nd.world < 4:
            assert s.assumed and s.score == nd.mastery["unlock"] and s.stars == 0, nid
        else:
            assert not s.assumed and s.score == 0.0, nid
    # Frontier on day one in world 4: entry nodes available, one hop beyond them reachable, two hops locked.
    assert m.availability(p, "w04.oa.multiplicative-comparison") == "available"   # all prerequisites in world 3
    assert m.availability(p, "w04.nbt.place-value-ten-times") == "available"
    assert m.availability(p, "w04.nbt.multiply-multidigit") == "reach"            # needs place-value-ten-times (available)
    assert m.availability(p, "w04.nbt.divide-by-one-digit") == "locked"           # needs multiply-multidigit (only reach)
    assert m.availability(p, "w04.oa.multistep-word-problems") == "locked"        # needs divide-by-one-digit
    assert m.availability(p, "w05.oa.order-of-operations") == "locked"
    assert m.map_state(p, "w03.oa.multiply-fluently-100", 0.0)["state"] == "assumed"
    # First real item on an assumed node clears the flag; score continues from the provisional value.
    m.apply_item(p, ItemResult("w03.oa.multiply-fluently-100", "symbolic", "fluency", True, response_ms=1500, at=0.0))
    s = p.nodes["w03.oa.multiply-fluently-100"]
    assert not s.assumed and s.score > 0.6


def test_7_availability_is_monotonic():
    m = Mastery(tiny_curriculum()); p = m.new_profile(1)
    play_level(m, p, "w01.oa.a", 9, "symbolic", now=0.0)
    assert m.availability(p, "w01.oa.b") == "available"
    play_level(m, p, "w01.oa.b", 3, "symbolic", now=0.0)
    p.nodes["w01.oa.a"].score = 0.2
    assert m.availability(p, "w01.oa.b") == "available"
    assert m.availability(p, "w02.oa.c") == "reach"


def test_8_miss_damping():
    m = Mastery(tiny_curriculum()); p = m.new_profile(1)
    p.nodes["w01.oa.a"].score = 0.8
    m.apply_item(p, ItemResult("w01.oa.a", "symbolic", "build", False, first_try=False, at=0.0))
    assert approx(p.nodes["w01.oa.a"].score, 0.74), p.nodes["w01.oa.a"].score


def test_9_transfer_representation_is_least_drilled():
    m = Mastery(tiny_curriculum()); p = m.new_profile(1)
    play_level(m, p, "w01.oa.a", 12, "symbolic", now=0.0, strategy="make-ten")
    play_level(m, p, "w01.oa.a", 3, "visual", now=0.0)
    assert m.choose_transfer_representation(p, "w01.oa.a") == "visual"
    # Tie goes to the representation not used in the most recent level.
    play_level(m, p, "w01.oa.a", 9, "visual", now=0.0)
    assert p.nodes["w01.oa.a"].rep_items == {"symbolic": 12, "visual": 12}
    assert m.choose_transfer_representation(p, "w01.oa.a") == "symbolic"


def test_10_one_sitting_is_not_enough():
    m = Mastery(tiny_curriculum()); p = m.new_profile(1)
    play_level(m, p, "w01.oa.a", 10, "symbolic", now=0.0, strategy="make-ten")
    play_level(m, p, "w01.oa.a", 10, "visual", now=0.0)
    s = p.nodes["w01.oa.a"]
    assert s.score > 0.95 and s.stars == 2
    assert m.due_revisits(p, 0.9, "build") == []
    assert s.stars == 2


def test_11_fluency_gate_and_slow_cap():
    m = Mastery(tiny_curriculum()); p = m.new_profile(1)
    nid = "w02.oa.fast"
    # Slow but correct: evidence capped at 0.8, so the score never reaches 0.9.
    play_level(m, p, nid, 10, "symbolic", now=0.0, engine="fluency", response_ms=10000)
    play_level(m, p, nid, 10, "verbal", now=0.0, engine="fluency", response_ms=10000)
    s = p.nodes[nid]
    assert s.score < 0.81 and s.stars == 2, (s.score, s.stars)
    # Fast items raise the score and fluency; still no third star without a revisit.
    play_level(m, p, nid, 15, "symbolic", now=0.0, engine="fluency", response_ms=1500)
    assert s.score >= 0.9 and s.fluency_ms == 1500 and s.stars == 2
    host_due = m.due_revisits(p, 2.0, "fluency", core_items=25)
    assert host_due == [(nid, "verbal")], host_due


def test_12_milestones():
    m = Mastery(tiny_curriculum()); p = m.new_profile(1)
    play_level(m, p, "w01.oa.a", 9, "symbolic", now=0.0, strategy="make-ten")
    play_level(m, p, "w01.oa.a", 4, "visual", now=0.0)
    play_level(m, p, "w01.oa.b", 9, "symbolic", now=0.0)
    play_level(m, p, "w01.oa.b", 4, "visual", now=0.0)
    assert m.world_stars(p, 1) == 4
    assert m.world_complete(p, 1)
    assert not m.world_complete(p, 2)
    m.finish_level(p, LevelResult("w01.oa.a", "build", "symbolic", True, [], secret_goals_hit=["doubles-three"], ended_at=0.0))
    assert m.secret_goal_count(p) == 1 and m.secret_goal_count(p, world=2) == 0



# ---- Scenarios 13–15 and the §5–§8 rule tests (specs/mastery.md §12). Mirrors core/src/test/.../MasteryRulesTest.kt.

def _rules_curriculum():
    c = tiny_curriculum()
    c["w01.oa.plain"] = NodeDef("w01.oa.plain", 1, [], ["symbolic", "visual"], ["build"], [], M)
    c["w02.oa.c"] = NodeDef("w02.oa.c", 2, ["w01.oa.b", "w01.oa.plain"], ["symbolic", "visual"], ["build"], [], M)
    c["w02.oa.sort"] = NodeDef("w02.oa.sort", 2, [], ["symbolic", "visual", "physical"], ["sort", "build"], [], M)
    return c


def _two_stars(m, p, nid="w01.oa.a", at=0.0):
    play_level(m, p, nid, 8, "symbolic", now=at, strategy="make-ten")
    play_level(m, p, nid, 7, "visual", now=at)
    assert p.nodes[nid].stars == 2


def _set(m, p, nid, at, correct, hints=0, engine="build", response_ms=1000):
    due = m.due_revisits(p, at, engine, exclude="w01.oa.b", core_items=25)
    rep = next(r for n, r in due if n == nid)
    for i in range(5):
        ok = i < correct
        m.apply_item(p, ItemResult(nid, rep, engine, ok, first_try=ok, hints=hints if ok else 0, response_ms=response_ms if ok else None, kind="revisit", at=at))
    m.finish_level(p, LevelResult("w01.oa.b", engine, "symbolic", True, [], started_at=at, ended_at=at))
    m.recompute_stars(p, nid, at)


def test_13_failed_set_above_the_score_is_damped():
    m = Mastery(_rules_curriculum()); p = m.new_profile(1)
    _two_stars(m, p)
    p.nodes["w01.oa.a"].score = 0.6
    _set(m, p, "w01.oa.a", 2.0, correct=4, hints=1)
    assert approx(p.nodes["w01.oa.a"].score, 0.62), p.nodes["w01.oa.a"].score


def test_14_starting_world_moved_down_keeps_evidence():
    m = Mastery(load_curriculum()); p = m.new_profile(4)
    played = "w03.oa.multiply-fluently-100"
    m.apply_item(p, ItemResult(played, "symbolic", "fluency", True, response_ms=1500))
    score = p.nodes[played].score
    m.set_starting_world(p, 2)
    for nid, nd in m.c.items():
        if nd.world == 3:
            s = p.nodes[nid]
            assert not s.assumed
            assert s.score == (score if nid == played else 0.0), nid
    assert all(p.nodes[nid].assumed for nid, nd in m.c.items() if nd.world == 1)
    m.set_starting_world(p, 4)
    assert p.nodes[played].score == score
    assert p.nodes["w03.nbt.round-to-10-100"].assumed


def test_15_unhostable_representation_never_starves_the_queue():
    m = Mastery(_rules_curriculum()); p = m.new_profile(1)
    play_level(m, p, "w02.oa.sort", 8, "symbolic", now=0.0)
    play_level(m, p, "w02.oa.sort", 7, "visual", now=0.0)
    _two_stars(m, p, "w01.oa.a", at=0.5)
    servable = lambda nid: {"symbolic", "visual"} if nid == "w02.oa.sort" else None
    served = m.due_revisits(p, 3.0, "build", core_items=8, servable=servable)
    assert len(served) == 1 and served[0][0] == "w02.oa.sort" and served[0][1] in {"symbolic", "visual"}, served
    only_a = lambda nid, engine, rep: nid == "w01.oa.a"
    assert [n for n, _ in m.due_revisits(p, 3.0, "build", core_items=8, can_host=only_a)] == ["w01.oa.a"]
    assert m.choose_transfer_representation(p, "w02.oa.sort", servable=set()) is None


def test_rule_two_stars_needs_score():
    m = Mastery(_rules_curriculum()); p = m.new_profile(1)
    play_level(m, p, "w01.oa.a", 3, "symbolic", now=0.0, strategy="make-ten")
    play_level(m, p, "w01.oa.a", 3, "visual", now=0.0)
    assert p.nodes["w01.oa.a"].stars == 1


def test_rule_strategy_gate_only_when_observable():
    m = Mastery(_rules_curriculum()); p = m.new_profile(1)
    play_level(m, p, "w01.oa.a", 8, "symbolic", now=0.0, observes=True)
    play_level(m, p, "w01.oa.a", 7, "visual", now=0.0, observes=True)
    assert p.nodes["w01.oa.a"].stars == 1
    q = m.new_profile(1)
    play_level(m, q, "w01.oa.a", 8, "symbolic", now=0.0, observes=False)
    play_level(m, q, "w01.oa.a", 7, "visual", now=0.0, observes=False)
    assert q.nodes["w01.oa.a"].stars == 2


def test_rule_demotions_with_hysteresis():
    m = Mastery(_rules_curriculum()); p = m.new_profile(1)
    _two_stars(m, p)
    s = p.nodes["w01.oa.a"]
    s.score = 0.61; m.recompute_stars(p, "w01.oa.a", 0.0); assert s.stars == 2
    s.score = 0.59; m.recompute_stars(p, "w01.oa.a", 0.0); assert s.stars == 1
    s.score = 0.9; m.recompute_stars(p, "w01.oa.a", 0.0); assert s.stars == 2
    _set(m, p, "w01.oa.a", 2.0, correct=5)
    assert s.stars == 3
    s.score = 0.81; m.recompute_stars(p, "w01.oa.a", 2.0); assert s.stars == 3
    s.score = 0.79; m.recompute_stars(p, "w01.oa.a", 2.0); assert s.stars == 2


def test_rule_three_stars_needs_score_zero_hints_and_fluency():
    m = Mastery(_rules_curriculum()); p = m.new_profile(1)
    _two_stars(m, p)
    s = p.nodes["w01.oa.a"]
    s.score = 0.8
    _set(m, p, "w01.oa.a", 2.0, correct=5)
    assert s.revisit.passed == 1 and s.stars == 2          # score under three_star
    q = m.new_profile(1)
    _two_stars(m, q)
    play_level(m, q, "w01.oa.a", 1, "symbolic", now=1.0, hints=1)
    q.nodes["w01.oa.a"].score = 0.95
    _set(m, q, "w01.oa.a", 2.0, correct=5)
    assert q.nodes["w01.oa.a"].stars == 2                   # a hinted item in recent
    r = m.new_profile(1); nid = "w02.oa.fast"
    play_level(m, r, nid, 8, "symbolic", now=0.0, engine="fluency", response_ms=4000)
    play_level(m, r, nid, 7, "verbal", now=0.0, engine="fluency", response_ms=4000)
    _set(m, r, nid, 2.0, correct=5, engine="fluency", response_ms=4000)
    assert r.nodes[nid].stars == 2 and r.nodes[nid].fluency_ms == 4000
    r.nodes[nid].fluency_ms = 2500; m.recompute_stars(r, nid, 2.0)
    assert r.nodes[nid].stars == 3


def test_rule_hinted_set_fails_and_interval_caps():
    m = Mastery(_rules_curriculum()); p = m.new_profile(1)
    _two_stars(m, p)
    _set(m, p, "w01.oa.a", 2.0, correct=5, hints=1)
    rv = p.nodes["w01.oa.a"].revisit
    assert rv.passed == 0 and rv.last_set_passed is False and rv.due_at == 4.0
    for at in (4.0, 11.0, 32.0, 53.0):
        _set(m, p, "w01.oa.a", at, correct=5)
    assert rv.interval_index == 2 and rv.due_at == 74.0 and rv.passed == 4


def test_rule_re_earning_two_stars_keeps_schedule():
    m = Mastery(_rules_curriculum()); p = m.new_profile(1)
    _two_stars(m, p)
    s = p.nodes["w01.oa.a"]; first_due = s.revisit.due_at
    s.score = 0.3; m.recompute_stars(p, "w01.oa.a", 1.0); assert s.stars == 1
    s.score = 0.9; m.recompute_stars(p, "w01.oa.a", 1.5); assert s.stars == 2
    assert s.two_star_at == 0.0 and s.revisit.due_at == first_due


def test_rule_due_revisits_engine_exclude_order_cap():
    m = Mastery(_rules_curriculum()); p = m.new_profile(1)
    _two_stars(m, p, "w01.oa.a", at=0.0)
    _two_stars(m, p, "w01.oa.plain", at=1.0)
    assert m.due_revisits(p, 5.0, "sort") == []
    assert [n for n, _ in m.due_revisits(p, 5.0, "build")] == ["w01.oa.a", "w01.oa.plain"]
    assert [n for n, _ in m.due_revisits(p, 5.0, "build", exclude="w01.oa.a")] == ["w01.oa.plain"]
    assert len(m.due_revisits(p, 5.0, "build", core_items=8)) == 1
    assert len(m.due_revisits(p, 5.0, "build", core_items=10)) == 2


def test_rule_reach_credit_only_unmet_and_fluency_needs_three():
    m = Mastery(_rules_curriculum()); p = m.new_profile(1)
    p.nodes["w01.oa.a"].score = 0.7
    p.nodes["w01.oa.b"].score = 0.5
    p.nodes["w01.oa.plain"].score = 0.7
    assert m.availability(p, "w02.oa.c") == "reach"
    m.apply_item(p, ItemResult("w02.oa.c", "symbolic", "build", True, response_ms=900, kind="reach"))
    assert approx(p.nodes["w01.oa.b"].score, 0.54) and p.nodes["w01.oa.plain"].score == 0.7
    nid = "w02.oa.fast"
    for _ in range(2):
        m.apply_item(p, ItemResult(nid, "symbolic", "fluency", True, response_ms=1000))
    assert p.nodes[nid].fluency_ms is None
    m.apply_item(p, ItemResult(nid, "symbolic", "fluency", True, response_ms=1000))
    assert p.nodes[nid].fluency_ms == 1000

if __name__ == "__main__":
    tests = [(k, v) for k, v in sorted(globals().items()) if k.startswith("test_") and callable(v)]
    failed = 0
    for name, fn in tests:
        try:
            fn()
            print(f"ok   {name}")
        except AssertionError as e:
            failed += 1
            print(f"FAIL {name}: {e}")
    print(f"{len(tests) - failed}/{len(tests)} passed")
    sys.exit(1 if failed else 0)
