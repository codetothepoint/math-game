package com.codetothepoint.mathgame.core.recipes

import com.codetothepoint.mathgame.core.curriculum.CurriculumLoader
import com.codetothepoint.mathgame.core.curriculum.Engine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RecipeTest {
    private val curriculum = CurriculumLoader.fromResource()
    private val recipes = RecipeLoader.fromResources()

    @Test
    fun bundledRecipesAreValidAgainstTheCurriculum() {
        assertEquals(emptyList(), recipes.validate(curriculum))
        assertEquals(26, recipes.all.size)
        assertEquals(15, recipes.forEngine(Engine.BUILD).size)
        assertEquals(11, recipes.forEngine(Engine.MANIPULATE).size)
    }

    @Test
    fun everyWorldFourNodeHostedByASpecifiedEngineHasARecipe() {
        for (n in curriculum.nodesIn(4)) for (e in n.engines) {
            if (EngineSpec.of(e) != null) assertNotNull(recipes[n.id, e], "${n.id}/${e.key}")
        }
    }

    @Test
    fun bandsAndStrategyMapsAreReadable() {
        val r = assertNotNull(recipes["w04.nbt.multiply-multidigit", Engine.BUILD])
        assertEquals("tiles", r.binding)
        assertEquals("product", r.target)
        assertEquals(listOf(12..49, 2..5), r.params(Band.LOW).ranges("factors"))
        assertEquals(listOf(11..99, 11..29), r.params(Band.HIGH).ranges("factors"))
        assertEquals(listOf(11..99, 2..9), r.params(Band.MID).ranges("factors"))
        assertFalse(r.params(Band.MID).has("also"))   // no 3-digit × 1-digit products: sides cap at 99 (engine-build §13)
        assertEquals(mapOf("area-model" to "split-place-value", "distributive" to "split-one-dimension"), r.strategies)
        assertEquals("area-model", r.strategyForRule["split-place-value"])
        assertTrue(r.observesStrategies)
        assertEquals(setOf("structure", "piece", "readout"), r.hints.keys)
        val m = assertNotNull(recipes["w04.md.measure-angles", Engine.MANIPULATE])
        assertEquals(2, m.tolerance.int("degrees"))
        assertEquals(1, m.tolerance.int("high_band_degrees"))
        assertEquals("grid", assertNotNull(recipes["w04.nf.decimal-notation", Engine.MANIPULATE]).instrument2)
    }

    @Test
    fun bandFollowsTheScore() {
        assertEquals(Band.LOW, Band.forScore(0.0))
        assertEquals(Band.LOW, Band.forScore(0.39))
        assertEquals(Band.MID, Band.forScore(0.4))
        assertEquals(Band.MID, Band.forScore(0.749))
        assertEquals(Band.HIGH, Band.forScore(0.75))
    }
}
