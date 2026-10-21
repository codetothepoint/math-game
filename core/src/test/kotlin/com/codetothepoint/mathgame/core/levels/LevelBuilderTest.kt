package com.codetothepoint.mathgame.core.levels

import com.codetothepoint.mathgame.core.curriculum.CurriculumLoader
import com.codetothepoint.mathgame.core.curriculum.Engine
import com.codetothepoint.mathgame.core.curriculum.Representation
import com.codetothepoint.mathgame.core.mastery.DAY_MS
import com.codetothepoint.mathgame.core.mastery.ItemKind
import com.codetothepoint.mathgame.core.mastery.ItemResult
import com.codetothepoint.mathgame.core.mastery.LevelResult
import com.codetothepoint.mathgame.core.mastery.MasteryModel
import com.codetothepoint.mathgame.core.recipes.Band
import com.codetothepoint.mathgame.core.recipes.RecipeLoader
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LevelBuilderTest {
    private val model = MasteryModel(CurriculumLoader.fromResource())
    private val recipes = RecipeLoader.fromResources()
    private val builder = LevelBuilder(model, recipes)
    private val node = "w04.nbt.multiply-multidigit"

    @Test
    fun aFreshLevelIsEightCoreItemsInTheFirstRepresentationAtLowBand() {
        val p = model.newProfile(4)
        val plan = builder.build(p, node, Engine.BUILD, now = 0L, rng = Random(1))
        assertEquals(8, plan.items.size)
        assertEquals(8, plan.coreItems)
        assertEquals(Band.LOW, plan.band)
        assertEquals(Representation.VISUAL, plan.representation)   // recipe lists [visual, symbolic]
        assertTrue(plan.observesStrategies)
        // multiply-multidigit is one hop beyond the day-one frontier: reach items credit the prerequisites.
        assertTrue(plan.items.all { it.kind == ItemKind.REACH })
        assertTrue(plan.items.all { it.item.representation == Representation.VISUAL })
    }

    @Test
    fun representationRotatesAndBandFollowsScore() {
        val p = model.newProfile(4)
        val s = p.nodes.getValue(node)
        s.levelsCompleted = 1
        s.score = 0.5
        val plan = builder.build(p, node, Engine.BUILD, now = 0L, rng = Random(2))
        assertEquals(Representation.SYMBOLIC, plan.representation)
        assertEquals(Band.MID, plan.band)
        assertTrue(plan.items.all { it.kind == ItemKind.CORE })   // played once: available forever
    }

    @Test
    fun dueRevisitsAreMixedInAndNeverLast() {
        val p = model.newProfile(4)
        // Earn two stars on a build-hosted world-4 node so a revisit becomes due.
        val other = "w04.oa.factors-multiples-primes"
        fun play(rep: Representation, n: Int) {
            val items = (1..n).map { ItemResult(other, rep, Engine.BUILD, true, responseMs = 1000, strategy = "factor-pairs", at = 0L) }
            items.forEach { model.applyItem(p, it) }
            model.finishLevel(p, LevelResult(other, Engine.BUILD, rep, true, items, observesStrategies = true, endedAt = 0L))
        }
        play(Representation.VISUAL, 6); play(Representation.SYMBOLIC, 6)
        assertEquals(2, p.nodes.getValue(other).stars)
        val beforeDue = builder.build(p, node, Engine.BUILD, now = DAY_MS, rng = Random(3))
        assertEquals(0, beforeDue.items.count { it.kind == ItemKind.REVISIT })
        val plan = builder.build(p, node, Engine.BUILD, now = 3 * DAY_MS, rng = Random(3))
        val revisits = plan.items.filter { it.kind == ItemKind.REVISIT }
        assertEquals(1, revisits.size)                                   // floor(0.2 × 8) = 1
        assertEquals(other, revisits[0].nodeId)
        assertEquals(Representation.PHYSICAL, revisits[0].item.representation)   // least drilled of [visual, symbolic, physical]
        assertEquals(node, plan.items.last().nodeId)
        assertTrue(plan.items.first().nodeId == node)
        assertEquals(9, plan.items.size)
    }

    @Test
    fun anAssumedPrerequisiteCanBeInterleaved() {
        val p = model.newProfile(4)
        // w04.nbt.multiply-multidigit needs w03.oa.multiply-fluently-100 (fluency only), w03.md.area-distributive-additive
        // (build, strategy) and w04.nbt.place-value-ten-times. Only world-3 nodes are assumed; the build one has no
        // world-3 recipe yet, so nothing is interleaved today.
        val plan = builder.build(p, node, Engine.BUILD, now = 0L, rng = Random(4))
        assertNull(plan.items.firstOrNull { it.nodeId.startsWith("w03") })
    }

    @Test
    fun hintLadderCountsGrantedRequestsOnly() {
        val h = HintLadder(listOf("structure", "piece", "readout"))
        assertEquals("structure", h.request())
        assertEquals("piece", h.request())
        assertEquals("readout", h.request())
        assertNull(h.request())
        assertNull(h.request())
        assertEquals(3, h.granted)
        h.reset()
        assertEquals(0, h.granted)
    }
}
