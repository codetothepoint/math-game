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
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The twelve scenarios of specs/mastery.md §12, with the same expected numbers as
 * tools/test_mastery.py (ADR 0005). Times are epoch milliseconds; `day(n)` keeps the tests readable.
 */
class MasteryModelTest {

    private fun day(d: Double): Long = (d * DAY_MS).toLong()

    private val m = MasteryThresholds(unlock = 0.6, twoStar = 0.75, threeStar = 0.9, revisitDays = listOf(2, 7, 21))

    private fun node(id: String, world: Int, prereqs: List<String>, reps: List<Representation>, engines: List<Engine>, strategies: List<String>, mastery: MasteryThresholds) =
        SkillNode(id, world, "playground", "oa", "OA", id, id, "ccss", listOf("x"), prereqs, emptyList(), reps, engines, mastery, strategies, emptyList(), 0)

    private fun tiny(): Curriculum = Curriculum(
        "test",
        listOf(WorldInfo(1, "One", "playground", ""), WorldInfo(2, "Two", "playground", "")),
        listOf(
            node("w01.oa.a", 1, emptyList(), listOf(SYMBOLIC, VISUAL), listOf(Engine.BUILD, Engine.MANIPULATE), listOf("make-ten"), m),
            node("w01.oa.b", 1, listOf("w01.oa.a"), listOf(SYMBOLIC, VISUAL), listOf(Engine.BUILD), emptyList(), m),
            node("w02.oa.c", 2, listOf("w01.oa.b"), listOf(SYMBOLIC, VISUAL), listOf(Engine.BUILD), emptyList(), m),
            node("w02.oa.fast", 2, emptyList(), listOf(SYMBOLIC, VERBAL), listOf(Engine.FLUENCY), emptyList(), m.copy(fluencyMs = 3000)),
        ),
    )

    private fun playLevel(
        model: MasteryModel, p: Profile, nid: String, n: Int, rep: Representation, now: Long,
        correct: Boolean = true, hints: Int = 0, strategy: String? = null, kind: ItemKind = ItemKind.CORE,
        observes: Boolean = true, engine: Engine = Engine.BUILD, responseMs: Int = 1000,
    ): List<ItemResult> {
        val items = (1..n).map {
            ItemResult(nid, rep, engine, correct, firstTry = correct, hints = hints, responseMs = if (correct) responseMs else null, strategy = strategy, kind = kind, at = now)
        }
        items.forEach { model.applyItem(p, it) }
        model.finishLevel(p, LevelResult(nid, engine, rep, true, items, observesStrategies = observes, startedAt = now, endedAt = now))
        return items
    }

    /** Serves one full revisit set inside a host level; returns the representation used. */
    private fun serveRevisitSet(model: MasteryModel, p: Profile, nid: String, now: Long, correctCount: Int, hints: Int = 0, engine: Engine = Engine.BUILD): Representation {
        val host = "w01.oa.b"
        val due = model.dueRevisits(p, now, engine, exclude = host, coreItems = 25)
        assertEquals(nid, due.first().first, "$nid should be the due node, got $due")
        val rep = due.first().second
        val items = (0 until 5).map { i ->
            val ok = i < correctCount
            ItemResult(nid, rep, engine, ok, firstTry = ok, hints = if (ok) hints else 0, responseMs = if (ok) 1000 else null, kind = ItemKind.REVISIT, at = now)
        }
        items.forEach { model.applyItem(p, it) }
        model.finishLevel(p, LevelResult(host, engine, SYMBOLIC, true, items, startedAt = now, endedAt = now))
        return rep
    }

    private fun assertApprox(expected: Double, actual: Double, tol: Double = 0.002) =
        assertTrue(kotlin.math.abs(expected - actual) <= tol, "expected $expected ± $tol, got $actual")

    @Test
    fun scenario1_nineCleanItemsTwoStars() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        playLevel(model, p, "w01.oa.a", 5, SYMBOLIC, day(0.0), strategy = "make-ten")
        playLevel(model, p, "w01.oa.a", 4, VISUAL, day(0.0))
        val s = p.nodes.getValue("w01.oa.a")
        assertApprox(0.768, s.score)
        assertEquals(2, s.stars)
        assertEquals(day(2.0), assertNotNull(s.revisit).dueAt)
    }

    @Test
    fun scenario2_hintRateBlocksTwoStars() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        playLevel(model, p, "w01.oa.a", 15, SYMBOLIC, day(0.0), strategy = "make-ten")
        assertEquals(1, p.nodes.getValue("w01.oa.a").stars)
        playLevel(model, p, "w01.oa.a", 4, VISUAL, day(0.0), hints = 1)
        val s = p.nodes.getValue("w01.oa.a")
        assertTrue(s.score >= 0.75, "score ${s.score}")
        assertEquals(1, s.stars)
    }

    @Test
    fun scenario3_thirdStarNeedsAPassedRevisit() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        playLevel(model, p, "w01.oa.a", 8, SYMBOLIC, day(0.0), strategy = "make-ten")
        playLevel(model, p, "w01.oa.a", 7, VISUAL, day(0.0))
        val s = p.nodes.getValue("w01.oa.a")
        assertApprox(0.913, s.score)
        assertEquals(2, s.stars)
        assertEquals(emptyList(), model.dueRevisits(p, day(0.5), Engine.BUILD))
        assertEquals(emptyList(), model.dueRevisits(p, day(1.9), Engine.BUILD))
        val rep = serveRevisitSet(model, p, "w01.oa.a", day(2.0), correctCount = 4)
        assertEquals(3, s.stars, "score ${s.score}")
        val rv = assertNotNull(s.revisit)
        assertEquals(1, rv.passed)
        assertEquals(1, rv.intervalIndex)
        assertEquals(day(9.0), rv.dueAt)
        assertEquals(VISUAL, rep)
    }

    @Test
    fun scenario4_failedRevisitDemotes() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        playLevel(model, p, "w01.oa.a", 8, SYMBOLIC, day(0.0), strategy = "make-ten")
        playLevel(model, p, "w01.oa.a", 7, VISUAL, day(0.0))
        serveRevisitSet(model, p, "w01.oa.a", day(2.0), correctCount = 4)
        val s = p.nodes.getValue("w01.oa.a")
        assertEquals(3, s.stars)
        serveRevisitSet(model, p, "w01.oa.a", day(9.0), correctCount = 2)
        assertEquals(2, s.stars)
        val rv = assertNotNull(s.revisit)
        assertEquals(0, rv.intervalIndex)
        assertEquals(day(11.0), rv.dueAt)
        assertEquals(false, rv.lastSetPassed)
        serveRevisitSet(model, p, "w01.oa.a", day(11.0), correctCount = 5)
        assertEquals(3, s.stars)
        assertEquals(listOf(true, false, true), rv.history.map { it.passed })
    }

    @Test
    fun scenario5_reachCredit() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        p.nodes.getValue("w01.oa.a").score = 0.5
        assertEquals(Availability.REACH, model.availability(p, "w01.oa.b"))
        assertEquals(Availability.LOCKED, model.availability(p, "w02.oa.c"))
        playLevel(model, p, "w01.oa.b", 1, SYMBOLIC, day(0.0), kind = ItemKind.REACH)
        assertApprox(0.54, p.nodes.getValue("w01.oa.a").score)
        playLevel(model, p, "w01.oa.b", 1, SYMBOLIC, day(0.0), kind = ItemKind.REACH, correct = false)
        assertApprox(0.54, p.nodes.getValue("w01.oa.a").score)
    }

    @Test
    fun scenario6_startingWorldFourRealCurriculum() {
        val model = MasteryModel(CurriculumLoader.fromResource()); val p = model.newProfile(4)
        for (nd in model.curriculum.nodes.values) {
            val s = p.nodes.getValue(nd.id)
            if (nd.world < 4) {
                assertTrue(s.assumed && s.score == nd.mastery.unlock && s.stars == 0, nd.id)
            } else {
                assertTrue(!s.assumed && s.score == 0.0, nd.id)
            }
        }
        assertEquals(Availability.AVAILABLE, model.availability(p, "w04.oa.multiplicative-comparison"))
        assertEquals(Availability.AVAILABLE, model.availability(p, "w04.nbt.place-value-ten-times"))
        assertEquals(Availability.REACH, model.availability(p, "w04.nbt.multiply-multidigit"))
        assertEquals(Availability.LOCKED, model.availability(p, "w04.nbt.divide-by-one-digit"))
        assertEquals(Availability.LOCKED, model.availability(p, "w04.oa.multistep-word-problems"))
        assertEquals(Availability.LOCKED, model.availability(p, "w05.oa.order-of-operations"))
        assertEquals(MapNodeState.ASSUMED, model.mapState(p, "w03.oa.multiply-fluently-100", 0L).state)
        model.applyItem(p, ItemResult("w03.oa.multiply-fluently-100", SYMBOLIC, Engine.FLUENCY, true, responseMs = 1500, at = 0L))
        val s = p.nodes.getValue("w03.oa.multiply-fluently-100")
        assertFalse(s.assumed)
        assertTrue(s.score > 0.6)
    }

    @Test
    fun scenario7_availabilityIsMonotonic() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        playLevel(model, p, "w01.oa.a", 9, SYMBOLIC, day(0.0))
        assertEquals(Availability.AVAILABLE, model.availability(p, "w01.oa.b"))
        playLevel(model, p, "w01.oa.b", 3, SYMBOLIC, day(0.0))
        p.nodes.getValue("w01.oa.a").score = 0.2
        assertEquals(Availability.AVAILABLE, model.availability(p, "w01.oa.b"))
        assertEquals(Availability.REACH, model.availability(p, "w02.oa.c"))
    }

    @Test
    fun scenario8_missDamping() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        p.nodes.getValue("w01.oa.a").score = 0.8
        model.applyItem(p, ItemResult("w01.oa.a", SYMBOLIC, Engine.BUILD, false, firstTry = false, at = 0L))
        assertApprox(0.74, p.nodes.getValue("w01.oa.a").score)
    }

    @Test
    fun scenario9_transferRepresentationIsLeastDrilled() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        playLevel(model, p, "w01.oa.a", 12, SYMBOLIC, day(0.0), strategy = "make-ten")
        playLevel(model, p, "w01.oa.a", 3, VISUAL, day(0.0))
        assertEquals(VISUAL, model.chooseTransferRepresentation(p, "w01.oa.a"))
        playLevel(model, p, "w01.oa.a", 9, VISUAL, day(0.0))
        assertEquals(mapOf(SYMBOLIC to 12, VISUAL to 12), p.nodes.getValue("w01.oa.a").repItems)
        assertEquals(SYMBOLIC, model.chooseTransferRepresentation(p, "w01.oa.a"))
    }

    @Test
    fun scenario10_oneSittingIsNotEnough() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        playLevel(model, p, "w01.oa.a", 10, SYMBOLIC, day(0.0), strategy = "make-ten")
        playLevel(model, p, "w01.oa.a", 10, VISUAL, day(0.0))
        val s = p.nodes.getValue("w01.oa.a")
        assertTrue(s.score > 0.95 && s.stars == 2)
        assertEquals(emptyList(), model.dueRevisits(p, day(0.9), Engine.BUILD))
        assertEquals(2, s.stars)
    }

    @Test
    fun scenario11_fluencyGateAndSlowCap() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        val nid = "w02.oa.fast"
        playLevel(model, p, nid, 10, SYMBOLIC, day(0.0), engine = Engine.FLUENCY, responseMs = 10000)
        playLevel(model, p, nid, 10, VERBAL, day(0.0), engine = Engine.FLUENCY, responseMs = 10000)
        val s = p.nodes.getValue(nid)
        assertTrue(s.score < 0.81 && s.stars == 2, "score ${s.score} stars ${s.stars}")
        playLevel(model, p, nid, 15, SYMBOLIC, day(0.0), engine = Engine.FLUENCY, responseMs = 1500)
        assertTrue(s.score >= 0.9)
        assertEquals(1500, s.fluencyMs)
        assertEquals(2, s.stars)
        assertEquals(listOf(nid to VERBAL), model.dueRevisits(p, day(2.0), Engine.FLUENCY, coreItems = 25))
    }

    @Test
    fun scenario12_milestones() {
        val model = MasteryModel(tiny()); val p = model.newProfile(1)
        playLevel(model, p, "w01.oa.a", 9, SYMBOLIC, day(0.0), strategy = "make-ten")
        playLevel(model, p, "w01.oa.a", 4, VISUAL, day(0.0))
        playLevel(model, p, "w01.oa.b", 9, SYMBOLIC, day(0.0))
        playLevel(model, p, "w01.oa.b", 4, VISUAL, day(0.0))
        assertEquals(4, model.worldStars(p, 1))
        assertTrue(model.worldComplete(p, 1))
        assertFalse(model.worldComplete(p, 2))
        model.finishLevel(p, LevelResult("w01.oa.a", Engine.BUILD, SYMBOLIC, true, emptyList(), secretGoalsHit = listOf("doubles-three"), endedAt = 0L))
        assertEquals(1, model.secretGoalCount(p))
        assertEquals(0, model.secretGoalCount(p, world = 2))
    }

    @Test
    fun representationRotationSkipsUnsupported() {
        val model = MasteryModel(tiny())
        val supported = setOf(SYMBOLIC, VISUAL, PHYSICAL)
        assertEquals(SYMBOLIC, model.representationForLevel("w01.oa.a", 0, supported))
        assertEquals(VISUAL, model.representationForLevel("w01.oa.a", 1, supported))
        assertEquals(SYMBOLIC, model.representationForLevel("w01.oa.a", 2, supported))
        assertEquals(SYMBOLIC, model.representationForLevel("w01.oa.a", 5, setOf(SYMBOLIC)))
    }

    /** Property: once played, a node never leaves AVAILABLE whatever happens to its prerequisites (§6.1). */
    @Test
    fun playedNodesNeverRelock() {
        val model = MasteryModel(CurriculumLoader.fromResource())
        val rng = Random(20261020)
        val p = model.newProfile(4)
        val world4 = model.curriculum.nodesIn(4).map { it.id }
        val played = mutableSetOf<String>()
        repeat(400) {
            val nid = world4[rng.nextInt(world4.size)]
            if (model.availability(p, nid) != Availability.LOCKED) {
                playLevel(model, p, nid, 1, model.curriculum[nid].representations.first(), day(rng.nextDouble() * 30), correct = rng.nextBoolean())
                played += nid
            }
            // Random damage to prerequisite scores.
            val victim = model.curriculum.nodes.keys.random(rng)
            p.nodes.getValue(victim).score = rng.nextDouble()
            for (id in played) assertEquals(Availability.AVAILABLE, model.availability(p, id), id)
        }
        assertTrue(played.size > 5)
    }
}
