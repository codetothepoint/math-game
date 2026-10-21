package com.codetothepoint.mathgame.core.curriculum

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The bundled curriculum.json is exactly what tools/curriculum.py validated (specs/build-plan.md M1). */
class CurriculumTest {
    private val c = CurriculumLoader.fromResource()

    @Test
    fun bundledGraphIsSound() {
        assertEquals(emptyList(), c.validate())
    }

    @Test
    fun shapeMatchesTheSpecReadme() {
        assertEquals(344, c.nodes.size)
        assertEquals(12, c.worlds.size)
        assertEquals(listOf(24, 27, 28, 28, 24, 29, 24, 25, 40, 31, 35, 29), (1..12).map { c.nodesIn(it).size })
        assertEquals(557, c.nodes.values.sumOf { it.prerequisites.size })
        assertEquals(7, c.nodes.values.count { it.source == "original" })
        val entries = c.nodes.values.filter { it.isEntry }
        assertEquals(4, entries.size)
        assertTrue(entries.all { it.world == 1 })
        assertEquals(36, c.nodes.values.maxOf { it.depth })
    }

    @Test
    fun defaultsAreMergedAndOverridesKept() {
        val facts = c["w03.oa.multiply-fluently-100"]
        assertEquals(0.6, facts.mastery.unlock)
        assertEquals(0.95, facts.mastery.threeStar)
        assertEquals(3000, facts.mastery.fluencyMs)
        assertEquals(listOf(1, 3, 7, 21, 60), facts.mastery.revisitDays)
        val plain = c["w04.oa.multiplicative-comparison"]
        assertEquals(0.9, plain.mastery.threeStar)
        assertEquals(null, plain.mastery.fluencyMs)
        assertEquals(listOf(2, 7, 21), plain.mastery.revisitDays)
    }

    @Test
    fun dependentsMirrorPrerequisites() {
        for (n in c.nodes.values) for (p in n.prerequisites) assertTrue(n.id in c[p].dependents, "${c[p].id} should list ${n.id}")
        for (n in c.nodes.values) for (d in n.dependents) assertTrue(n.id in c[d].prerequisites, "${c[d].id} should need ${n.id}")
    }

    @Test
    fun validatorCatchesABrokenGraph() {
        val a = c["w01.oa.count-on"]
        val cycleA = a.copy(prerequisites = listOf("w01.oa.add-within-10"))
        val b = c["w01.oa.add-within-10"]
        val broken = Curriculum(c.generated, c.worlds, listOf(cycleA, b))
        assertTrue(broken.validate().any { "cycle" in it })
        val dangling = Curriculum(c.generated, c.worlds, listOf(b))
        assertTrue(dangling.validate().any { "unknown prerequisite" in it })
    }
}
