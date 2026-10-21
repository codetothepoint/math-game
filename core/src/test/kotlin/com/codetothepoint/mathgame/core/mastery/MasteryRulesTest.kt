package com.codetothepoint.mathgame.core.mastery

import com.codetothepoint.mathgame.core.curriculum.Curriculum
import com.codetothepoint.mathgame.core.curriculum.CurriculumLoader
import com.codetothepoint.mathgame.core.curriculum.Engine
import com.codetothepoint.mathgame.core.curriculum.MasteryThresholds
import com.codetothepoint.mathgame.core.curriculum.Representation
import com.codetothepoint.mathgame.core.curriculum.Representation.PHYSICAL
import com.codetothepoint.mathgame.core.curriculum.Representation.SYMBOLIC
import com.codetothepoint.mathgame.core.curriculum.Representation.VERBAL
import com.codetothepoint.mathgame.core.curriculum.Representation.VISUAL
import com.codetothepoint.mathgame.core.curriculum.SkillNode
import com.codetothepoint.mathgame.core.curriculum.WorldInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * One test per rule in specs/mastery.md §5–§8, written so that removing the rule fails the test
 * (the first adversarial review found most rules survived mutation). Scenarios 13–15 are here too.
 */
class MasteryRulesTest {
    private fun day(d: Double): Long = (d * DAY_MS).toLong()
    private val m = MasteryThresholds(unlock = 0.6, twoStar = 0.75, threeStar = 0.9, revisitDays = listOf(2, 7, 21))

    private fun node(id: String, world: Int, prereqs: List<String>, reps: List<Representation>, engines: List<Engine>, strategies: List<String> = emptyList(), mastery: MasteryThresholds = m) =
        SkillNode(id, world, "playground", "oa", "OA", id, id, "ccss", listOf("x"), prereqs, emptyList(), reps, engines, mastery, strategies, emptyList(), 0)

    private fun tiny(): Curriculum = Curriculum(
        "test", listOf(WorldInfo(1, "One", "playground", ""), WorldInfo(2, "Two", "playground", "")),
        listOf(
            node("w01.oa.a", 1, emptyList(), listOf(SYMBOLIC, VISUAL), listOf(Engine.BUILD, Engine.MANIPULATE), listOf("make-ten")),
            node("w01.oa.b", 1, listOf("w01.oa.a"), listOf(SYMBOLIC, VISUAL), listOf(Engine.BUILD)),
            node("w01.oa.plain", 1, emptyList(), listOf(SYMBOLIC, VISUAL), listOf(Engine.BUILD)),
            node("w02.oa.c", 2, listOf("w01.oa.b", "w01.oa.plain"), listOf(SYMBOLIC, VISUAL), listOf(Engine.BUILD)),
            node("w02.oa.fast", 2, emptyList(), listOf(SYMBOLIC, VERBAL), listOf(Engine.FLUENCY), mastery = m.copy(fluencyMs = 3000)),
            node("w02.oa.sort", 2, emptyList(), listOf(SYMBOLIC, VISUAL, PHYSICAL), listOf(Engine.SORT, Engine.BUILD)),
        ),
    )

    private fun items(nid: String, n: Int, rep: Representation, correct: Boolean = true, hints: Int = 0, strategy: String? = null, kind: ItemKind = ItemKind.CORE, engine: Engine = Engine.BUILD, responseMs: Int = 1000, at: Long = 0L) =
        (1..n).map { ItemResult(nid, rep, engine, correct, firstTry = correct, hints = hints, responseMs = if (correct) responseMs else null, strategy = strategy, kind = kind, at = at) }

    private fun play(model: MasteryModel, p: Profile, nid: String, n: Int, rep: Representation, at: Long = 0L, correct: Boolean = true, hints: Int = 0, strategy: String? = null, kind: ItemKind = ItemKind.CORE, observes: Boolean = true, engine: Engine = Engine.BUILD, responseMs: Int = 1000) {
        val its = items(nid, n, rep, correct, hints, strategy, kind, engine, responseMs, at)
        its.forEach { model.applyItem(p, it) }
        model.finishLevel(p, LevelResult(nid, engine, rep, true, its, observesStrategies = observes, startedAt = at, endedAt = at))
    }

    /** Plays enough to hold ★★ on w01.oa.a with a strategy observed. */
    private fun twoStars(model: MasteryModel, p: Profile, nid: String = "w01.oa.a", at: Long = 0L) {
        play(model, p, nid, 8, SYMBOLIC, at, strategy = "make-ten")
        play(model, p, nid, 7, VISUAL, at)
        assertEquals(2, p.nodes.getValue(nid).stars)
    }

    private fun revisitSet(model: MasteryModel, p: Profile, nid: String, at: Long, correct: Int, hints: Int = 0, engine: Engine = Engine.BUILD, responseMs: Int = 1000) {
        val due = model.dueRevisits(p, at, engine, exclude = "w01.oa.b", coreItems = 25)
        val rep = due.first { it.first == nid }.second
        val its = (0 until 5).map { i -> val ok = i < correct; ItemResult(nid, rep, engine, ok, firstTry = ok, hints = if (ok) hints else 0, responseMs = if (ok) responseMs else null, kind = ItemKind.REVISIT, at = at) }
        its.forEach { model.applyItem(p, it) }
        model.finishLevel(p, LevelResult("w01.oa.b", engine, SYMBOLIC, true, its, startedAt = at, endedAt = at))
    }

    private fun approx(e: Double, a: Double) = assertTrue(kotlin.math.abs(e - a) <= 0.002, "expected $e got $a")

    // ---- §5 ★★

    @Test
    fun twoStarsNeedsTheScoreEvenWithRepsAndStrategy() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        play(model, p, "w01.oa.a", 3, SYMBOLIC, strategy = "make-ten")
        play(model, p, "w01.oa.a", 3, VISUAL)
        val s = p.nodes.getValue("w01.oa.a")
        assertTrue(s.score < 0.75 && s.repItems.values.all { it >= 3 } && s.strategies.isNotEmpty())
        assertEquals(1, s.stars)
    }

    @Test
    fun strategyGateBlocksTwoStarsOnlyWhenObservable() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        play(model, p, "w01.oa.a", 8, SYMBOLIC, observes = true)   // could have seen a strategy, saw none
        play(model, p, "w01.oa.a", 7, VISUAL, observes = true)
        assertEquals(1, p.nodes.getValue("w01.oa.a").stars)
        val q = model.newProfile(1)
        play(model, q, "w01.oa.a", 8, SYMBOLIC, observes = false)  // engine cannot observe: no gate
        play(model, q, "w01.oa.a", 7, VISUAL, observes = false)
        assertEquals(2, q.nodes.getValue("w01.oa.a").stars)
    }

    /**
     * 15 clean items in one representation: ★ with a score far past two_star, but only one representation has evidence,
     * so ★★ waits for the final level (one level of [clean] clean and [hinted] hinted items in [rep]).
     */
    private fun heldAtOneStarThenFinalLevel(model: MasteryModel, rep: Representation, clean: Int, hinted: Int): NodeState {
        val p = model.newProfile(1)
        val nid = "w01.oa.a"
        play(model, p, nid, 15, SYMBOLIC, strategy = "make-ten")
        val s = p.nodes.getValue(nid)
        assertEquals(1, s.stars)
        assertTrue(s.score >= 0.75)
        val last = items(nid, clean, rep) + items(nid, hinted, rep, hints = 1)
        last.forEach { model.applyItem(p, it) }
        model.finishLevel(p, LevelResult(nid, Engine.BUILD, rep, true, last, observesStrategies = true, startedAt = 0L, endedAt = 0L))
        assertTrue(s.score >= 0.75, "score ${s.score} must not be what blocks the star")
        return s
    }

    @Test
    fun hintRateOfExactlyThirtyPercentStillEarnsTwoStarsAndFortyDoesNot() {
        val model = MasteryModel(tiny())
        val three = heldAtOneStarThenFinalLevel(model, VISUAL, clean = 7, hinted = 3)   // recent holds exactly these ten
        assertEquals(10, three.recent.size)
        assertEquals(3, three.recent.count { it.hints > 0 })
        assertEquals(2, three.stars)
        val four = heldAtOneStarThenFinalLevel(model, VISUAL, clean = 6, hinted = 4)
        assertEquals(10, four.recent.size)
        assertEquals(4, four.recent.count { it.hints > 0 })
        assertEquals(1, four.stars)
    }

    @Test
    fun twoStarsNeedsThreeItemsInTheSecondRepresentationNotTwo() {
        val model = MasteryModel(tiny())
        val two = heldAtOneStarThenFinalLevel(model, VISUAL, clean = 2, hinted = 0)
        assertEquals(2, two.repItems.getValue(VISUAL))
        assertEquals(1, two.stars)
        val three = heldAtOneStarThenFinalLevel(model, VISUAL, clean = 3, hinted = 0)
        assertEquals(3, three.repItems.getValue(VISUAL))
        assertEquals(2, three.stars)
    }

    @Test
    fun twoStarDemotionOnlyBelowTheHysteresisLine() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        twoStars(model, p)
        val s = p.nodes.getValue("w01.oa.a")
        s.score = 0.61
        model.recomputeStars(p, "w01.oa.a", 0L)
        assertEquals(2, s.stars)
        s.score = 0.59
        model.recomputeStars(p, "w01.oa.a", 0L)
        assertEquals(1, s.stars)
    }

    // ---- §5 ★★★

    @Test
    fun threeStarsNeedsTheScore() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        twoStars(model, p)
        val s = p.nodes.getValue("w01.oa.a")
        s.score = 0.8   // passes the set, still under three_star
        revisitSet(model, p, "w01.oa.a", day(2.0), correct = 5)
        assertEquals(1, assertNotNull(s.revisit).passed)
        assertTrue(s.score < 0.9)
        assertEquals(2, s.stars)
    }

    @Test
    fun threeStarsNeedsZeroHintsInRecent() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        twoStars(model, p)
        play(model, p, "w01.oa.a", 1, SYMBOLIC, at = day(1.0), hints = 1)   // one hinted core item lingers in recent
        val s = p.nodes.getValue("w01.oa.a")
        s.score = 0.95
        revisitSet(model, p, "w01.oa.a", day(2.0), correct = 5)
        assertEquals(1, assertNotNull(s.revisit).passed)
        assertEquals(2, s.stars)
    }

    @Test
    fun fluencyGateBlocksThirdStarAlone() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        val nid = "w02.oa.fast"
        play(model, p, nid, 8, SYMBOLIC, engine = Engine.FLUENCY, responseMs = 4000)   // under the 3× cap, so e = 1.0
        play(model, p, nid, 7, VERBAL, engine = Engine.FLUENCY, responseMs = 4000)
        val s = p.nodes.getValue(nid)
        assertEquals(2, s.stars)
        assertTrue(s.score >= 0.9)
        assertEquals(4000, s.fluencyMs)
        revisitSet(model, p, nid, day(2.0), correct = 5, engine = Engine.FLUENCY, responseMs = 4000)
        assertEquals(4000, s.fluencyMs)
        assertEquals(2, s.stars)   // only the fluency gate is failing
        s.fluencyMs = 2500
        model.recomputeStars(p, nid, day(2.0))
        assertEquals(3, s.stars)
    }

    @Test
    fun threeStarDemotionOnScore() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        twoStars(model, p)
        revisitSet(model, p, "w01.oa.a", day(2.0), correct = 5)
        val s = p.nodes.getValue("w01.oa.a")
        assertEquals(3, s.stars)
        s.score = 0.81
        model.recomputeStars(p, "w01.oa.a", day(2.0))
        assertEquals(3, s.stars)
        s.score = 0.79
        model.recomputeStars(p, "w01.oa.a", day(2.0))
        assertEquals(2, s.stars)
    }

    // ---- §7 sets

    @Test
    fun aHintedSetFailsEvenAtFiveOfFive() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        twoStars(model, p)
        revisitSet(model, p, "w01.oa.a", day(2.0), correct = 5, hints = 1)
        val rv = assertNotNull(p.nodes.getValue("w01.oa.a").revisit)
        assertEquals(0, rv.passed)
        assertEquals(false, rv.lastSetPassed)
        assertEquals(day(4.0), rv.dueAt)
    }

    @Test
    fun scenario13_failedSetAboveTheScoreIsStillDamped() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        twoStars(model, p)
        val s = p.nodes.getValue("w01.oa.a")
        s.score = 0.6
        revisitSet(model, p, "w01.oa.a", day(2.0), correct = 4, hints = 1)
        approx(0.62, s.score)
    }

    @Test
    fun intervalIndexCapsAtTheLastInterval() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        twoStars(model, p)
        val rv = assertNotNull(p.nodes.getValue("w01.oa.a").revisit)
        revisitSet(model, p, "w01.oa.a", day(2.0), correct = 5)
        revisitSet(model, p, "w01.oa.a", day(9.0), correct = 5)
        revisitSet(model, p, "w01.oa.a", day(30.0), correct = 5)
        revisitSet(model, p, "w01.oa.a", day(51.0), correct = 5)
        assertEquals(2, rv.intervalIndex)
        assertEquals(day(72.0), rv.dueAt)
        assertEquals(4, rv.passed)
    }

    @Test
    fun reEarningTwoStarsKeepsTheOriginalSchedule() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        twoStars(model, p, at = day(0.0))
        val s = p.nodes.getValue("w01.oa.a")
        val firstDue = assertNotNull(s.revisit).dueAt
        s.score = 0.3
        model.recomputeStars(p, "w01.oa.a", day(1.0))
        assertEquals(1, s.stars)
        s.score = 0.9
        model.recomputeStars(p, "w01.oa.a", day(1.5))
        assertEquals(2, s.stars)
        assertEquals(day(0.0), s.twoStarAt)
        assertEquals(firstDue, assertNotNull(s.revisit).dueAt)
    }

    @Test
    fun revisitItemsDoNotMoveTheScoreOneByOne() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        twoStars(model, p)
        val s = p.nodes.getValue("w01.oa.a")
        val before = s.score
        model.applyItem(p, ItemResult("w01.oa.a", VISUAL, Engine.BUILD, false, firstTry = false, kind = ItemKind.REVISIT, at = day(2.0)))
        assertEquals(before, s.score)
    }

    // ---- §7.2 delivery

    @Test
    fun dueRevisitsHonorEngineExclusionOrderAndCap() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        twoStars(model, p, "w01.oa.a", at = day(0.0))
        twoStars(model, p, "w01.oa.plain", at = day(1.0))   // due a day later
        assertEquals(emptyList(), model.dueRevisits(p, day(5.0), Engine.SORT))                              // neither lists sort
        assertEquals(listOf("w01.oa.a", "w01.oa.plain"), model.dueRevisits(p, day(5.0), Engine.BUILD).map { it.first })   // most overdue first
        assertEquals(listOf("w01.oa.plain"), model.dueRevisits(p, day(5.0), Engine.BUILD, exclude = "w01.oa.a").map { it.first })
        assertEquals(1, model.dueRevisits(p, day(5.0), Engine.BUILD, coreItems = 8).size)
        assertEquals(2, model.dueRevisits(p, day(5.0), Engine.BUILD, coreItems = 10).size)
        assertEquals(listOf("w01.oa.a"), model.dueRevisits(p, day(5.0), Engine.MANIPULATE).map { it.first })
    }

    @Test
    fun scenario15_unhostableRepresentationNeverStarvesTheQueue() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        // sort-node lists physical, which no recipe serves; a lists both of its reps.
        play(model, p, "w02.oa.sort", 8, SYMBOLIC, at = day(0.0))
        play(model, p, "w02.oa.sort", 7, VISUAL, at = day(0.0))
        twoStars(model, p, "w01.oa.a", at = day(0.5))
        assertEquals(2, p.nodes.getValue("w02.oa.sort").stars)
        val hosting = object : RevisitHosting {
            override fun servable(nodeId: String) = if (nodeId == "w02.oa.sort") setOf(SYMBOLIC, VISUAL) else null
            override fun canHost(nodeId: String, engine: Engine, rep: Representation) = true
        }
        // With no hosting information the least-drilled rep (physical) is chosen and nothing can serve it.
        val naive = model.dueRevisits(MasteryModel(tiny()).newProfile(1).also { copyInto(model, p, it) }, day(3.0), Engine.BUILD, coreItems = 8)
        assertEquals(listOf("w02.oa.sort" to PHYSICAL), naive)
        // With hosting, the set uses a servable representation and the queue is delivered in order.
        val served = model.dueRevisits(p, day(3.0), Engine.BUILD, coreItems = 8, hosting = hosting)
        assertEquals("w02.oa.sort", served.single().first)
        assertTrue(served.single().second in setOf(SYMBOLIC, VISUAL))
        // A node this engine cannot host at all does not take the single slot from one it can.
        val onlyA = object : RevisitHosting {
            override fun servable(nodeId: String): Set<Representation>? = null
            override fun canHost(nodeId: String, engine: Engine, rep: Representation) = nodeId == "w01.oa.a"
        }
        assertEquals(listOf("w01.oa.a"), model.dueRevisits(p, day(3.0), Engine.BUILD, coreItems = 8, hosting = onlyA).map { it.first })
        assertNull(model.chooseTransferRepresentation(p, "w02.oa.sort", servable = emptySet()))
    }

    @Test
    fun eachSetReChoosesItsRepresentation() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        val nid = "w01.oa.a"
        twoStars(model, p)   // 8 symbolic, 7 visual: visual is the least drilled
        val first = model.dueRevisits(p, day(2.0), Engine.BUILD).single().second
        assertEquals(VISUAL, first)
        revisitSet(model, p, nid, day(2.0), correct = 5)
        val rv = assertNotNull(p.nodes.getValue(nid).revisit)
        assertEquals(1, rv.passed)
        assertNull(rv.representation)   // the set is over, so the choice is dropped
        play(model, p, nid, 20, first, at = day(3.0))   // drill the first set's representation, the other not at all
        val other = if (first == VISUAL) SYMBOLIC else VISUAL
        assertEquals(listOf(nid to other), model.dueRevisits(p, day(9.0), Engine.BUILD))
    }

    @Test
    fun aRepresentationNoRecipeHostsIsDroppedSoALaterCallReChooses() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        val nid = "w02.oa.sort"   // lists physical, which no recipe serves
        play(model, p, nid, 8, SYMBOLIC)
        play(model, p, nid, 7, VISUAL)
        assertEquals(2, p.nodes.getValue(nid).stars)
        // Without hosting information the least-drilled representation (physical) is chosen and fixed for the set.
        assertEquals(listOf(nid to PHYSICAL), model.dueRevisits(p, day(3.0), Engine.BUILD, hosting = RevisitHosting.ALL))
        assertEquals(PHYSICAL, assertNotNull(p.nodes.getValue(nid).revisit).representation)
        val hosting = object : RevisitHosting {
            override fun servable(nodeId: String): Set<Representation> = setOf(SYMBOLIC, VISUAL)
            override fun canHost(nodeId: String, engine: Engine, rep: Representation) = rep != PHYSICAL
        }
        // The rejected choice is skipped and forgotten...
        assertEquals(emptyList(), model.dueRevisits(p, day(3.0), Engine.BUILD, hosting = hosting))
        assertNull(assertNotNull(p.nodes.getValue(nid).revisit).representation)
        // ...so the next call chooses among what is servable.
        val again = model.dueRevisits(p, day(3.0), Engine.BUILD, hosting = hosting).single()
        assertEquals(nid, again.first)
        assertTrue(again.second in setOf(SYMBOLIC, VISUAL))
    }

    private fun copyInto(model: MasteryModel, from: Profile, to: Profile) {
        for ((nid, s) in from.nodes) {
            val t = to.nodes.getValue(nid)
            t.score = s.score; t.stars = s.stars; t.items = s.items; t.levelsCompleted = s.levelsCompleted
            t.repItems.putAll(s.repItems); t.twoStarAt = s.twoStarAt
            t.revisit = s.revisit?.let { RevisitState(it.dueAt, it.intervalIndex, null, passed = it.passed) }
        }
        assertEquals(model.curriculum.nodes.size, to.nodes.size)
    }

    // ---- §6

    @Test
    fun reachCreditGoesOnlyToUnmetPrerequisites() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        p.nodes.getValue("w01.oa.a").score = 0.7      // so b is available, which makes c reach
        p.nodes.getValue("w01.oa.b").score = 0.5      // unmet
        p.nodes.getValue("w01.oa.plain").score = 0.7  // met
        assertEquals(Availability.REACH, model.availability(p, "w02.oa.c"))
        model.applyItem(p, ItemResult("w02.oa.c", SYMBOLIC, Engine.BUILD, true, responseMs = 900, kind = ItemKind.REACH))
        approx(0.54, p.nodes.getValue("w01.oa.b").score)
        assertEquals(0.7, p.nodes.getValue("w01.oa.plain").score)
    }

    @Test
    fun scenario14_startingWorldMovedDownAndReRunKeepsEvidence() {
        val model = MasteryModel(CurriculumLoader.fromResource()); val p = model.newProfile(4)
        val played = "w03.oa.multiply-fluently-100"
        model.applyItem(p, ItemResult(played, SYMBOLIC, Engine.FLUENCY, true, responseMs = 1500))
        val scoreAfterPlay = p.nodes.getValue(played).score
        model.setStartingWorld(p, 2)
        for (nd in model.curriculum.nodesIn(3)) {
            val s = p.nodes.getValue(nd.id)
            if (nd.id == played) { assertEquals(scoreAfterPlay, s.score); assertFalse(s.assumed) }
            else { assertFalse(s.assumed, nd.id); assertEquals(0.0, s.score, nd.id) }
        }
        assertTrue(model.curriculum.nodesIn(1).all { p.nodes.getValue(it.id).assumed })
        model.setStartingWorld(p, 4)
        assertEquals(scoreAfterPlay, p.nodes.getValue(played).score)   // evidence survives the re-run
        assertTrue(p.nodes.getValue("w03.nbt.round-to-10-100").assumed)
    }

    @Test
    fun fluencyNeedsThreeCleanItems() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        val nid = "w02.oa.fast"
        model.applyItem(p, ItemResult(nid, SYMBOLIC, Engine.FLUENCY, true, responseMs = 1000))
        model.applyItem(p, ItemResult(nid, SYMBOLIC, Engine.FLUENCY, true, responseMs = 1000))
        assertNull(p.nodes.getValue(nid).fluencyMs)
        model.applyItem(p, ItemResult(nid, SYMBOLIC, Engine.FLUENCY, true, responseMs = 1000))
        assertEquals(1000, p.nodes.getValue(nid).fluencyMs)
    }

    @Test
    fun blankStrategiesAreIgnored() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        model.applyItem(p, ItemResult("w01.oa.a", SYMBOLIC, Engine.BUILD, true, responseMs = 1000, strategy = ""))
        assertTrue(p.nodes.getValue("w01.oa.a").strategies.isEmpty())
    }
}
