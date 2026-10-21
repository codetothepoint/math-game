package com.codetothepoint.mathgame.core.levels

import com.codetothepoint.mathgame.core.curriculum.Engine
import com.codetothepoint.mathgame.core.curriculum.Representation
import com.codetothepoint.mathgame.core.mastery.Availability
import com.codetothepoint.mathgame.core.mastery.ItemKind
import com.codetothepoint.mathgame.core.mastery.MasteryModel
import com.codetothepoint.mathgame.core.mastery.Profile
import com.codetothepoint.mathgame.core.mastery.RevisitHosting
import com.codetothepoint.mathgame.core.recipes.Band
import com.codetothepoint.mathgame.core.recipes.Recipe
import com.codetothepoint.mathgame.core.recipes.Recipes
import kotlin.random.Random

/**
 * Assembles a playable level for a node and engine (specs/mastery.md §7.2, §7.5, §8; engine specs §2):
 * core items in the rotated representation and the score's band, due revisit items the scheduler
 * offers, and at most one item from an assumed prerequisite. Mixed items never sit at the end.
 */
class LevelBuilder(private val model: MasteryModel, private val recipes: Recipes, private val coreItemsPerLevel: Int = 8) {
    init { require(coreItemsPerLevel >= 2) { "A level needs at least two core items so mixed items are never first or last" } }

    /** Revisit hosting backed by the recipes (specs/mastery.md §7.2–§7.3). */
    val hosting: RevisitHosting = object : RevisitHosting {
        override fun servable(nodeId: String): Set<Representation> = recipes.forNode(nodeId).flatMap { it.representations }.toSet()
        override fun canHost(nodeId: String, engine: Engine, rep: Representation): Boolean = recipes[nodeId, engine]?.representations?.contains(rep) == true
    }

    fun build(p: Profile, nodeId: String, engine: Engine, now: Long, rng: Random): LevelPlan {
        val recipe = recipes[nodeId, engine] ?: throw IllegalArgumentException("No recipe for $nodeId/${engine.key}")
        val node = model.curriculum[nodeId]
        val state = p.nodes.getValue(nodeId)
        val representation = model.representationForLevel(nodeId, state.levelsCompleted, recipe.representations.toSet())
        val band = Band.forScore(state.score)
        val coreKind = if (model.availability(p, nodeId) == Availability.REACH && state.levelsCompleted == 0) ItemKind.REACH else ItemKind.CORE
        val generator = Generators.forRecipe(recipe)

        val core = List(coreItemsPerLevel) { PlannedItem(nodeId, coreKind, generator.generate(recipe.params(band), recipe.tolerance, band, representation, rng)) }

        val extras = mutableListOf<PlannedItem>()
        for ((dueNode, rep) in model.dueRevisits(p, now, engine, exclude = nodeId, coreItems = coreItemsPerLevel, hosting = hosting)) {
            val r = recipes.getValue(dueNode, engine)   // hosting guaranteed it exists and serves rep
            extras += PlannedItem(dueNode, ItemKind.REVISIT, Generators.forRecipe(r).generate(r.params(bandFor(p, dueNode)), r.tolerance, bandFor(p, dueNode), rep, rng))
        }
        assumedPrerequisite(p, nodeId, engine, rng)?.let { r ->
            val rep = r.representations.random(rng)
            extras += PlannedItem(r.node, ItemKind.CORE, Generators.forRecipe(r).generate(r.params(Band.MID), r.tolerance, Band.MID, rep, rng))
        }

        val items = core.toMutableList()
        for (extra in extras) items.add(rng.nextInt(1, items.size), extra)   // never first, never last
        return LevelPlan(nodeId, engine, representation, band, items, observesStrategies = recipe.observesStrategies).also {
            check(it.items.last().nodeId == nodeId)
            check(node.engines.contains(engine))
        }
    }

    private fun bandFor(p: Profile, nodeId: String): Band = Band.forScore(p.nodes.getValue(nodeId).score)

    /** An assumed direct prerequisite with a recipe on this engine, if any (§7.5). */
    private fun assumedPrerequisite(p: Profile, nodeId: String, engine: Engine, rng: Random): Recipe? {
        val candidates = model.curriculum[nodeId].prerequisites
            .filter { p.nodes.getValue(it).assumed }
            .mapNotNull { recipes[it, engine] }
        return if (candidates.isEmpty()) null else candidates.random(rng)
    }

    /** Representations a level for [nodeId] on [engine] may use, from the recipe. */
    fun representationsFor(nodeId: String, engine: Engine): List<Representation> = recipes[nodeId, engine]?.representations.orEmpty()
}
