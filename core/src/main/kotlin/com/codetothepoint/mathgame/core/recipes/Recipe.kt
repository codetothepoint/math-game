package com.codetothepoint.mathgame.core.recipes

import com.codetothepoint.mathgame.core.curriculum.Curriculum
import com.codetothepoint.mathgame.core.curriculum.Engine
import com.codetothepoint.mathgame.core.curriculum.Representation
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/** Difficulty bands (specs/mechanics/README.md): item difficulty follows the player's mastery score. */
enum class Band(val key: String) {
    LOW("low"), MID("mid"), HIGH("high");

    companion object {
        fun forScore(score: Double): Band = when {
            score < 0.4 -> LOW
            score < 0.75 -> MID
            else -> HIGH
        }
    }
}

/**
 * Generator parameters for one band. Recipes are free-form here (the schema says engine-specific),
 * so access is by typed getters that accept the shapes the recipe files use: a scalar or a list.
 */
class Params(private val obj: JsonObject) {
    fun has(key: String): Boolean = obj.has(key) && !obj.get(key).isJsonNull
    fun int(key: String): Int = required(key).asInt
    fun intOr(key: String, default: Int): Int = if (has(key)) int(key) else default
    fun bool(key: String): Boolean = has(key) && required(key).asBoolean
    fun string(key: String): String = required(key).asString

    /** A `[lo, hi]` pair, or a scalar as `[n, n]`. */
    fun range(key: String): IntRange {
        val e = required(key)
        return if (e.isJsonArray) e.asJsonArray.let { it[0].asInt..it[1].asInt } else e.asInt..e.asInt
    }

    /** A list of ints, or a scalar as a one-element list. */
    fun ints(key: String): List<Int> {
        val e = required(key)
        return if (e.isJsonArray) e.asJsonArray.map { it.asInt } else listOf(e.asInt)
    }

    /** A list of strings, or a scalar as a one-element list. */
    fun strings(key: String): List<String> {
        val e = required(key)
        return if (e.isJsonArray) e.asJsonArray.map { it.asString } else listOf(e.asString)
    }

    /** A list of `[a, b]` int pairs. */
    fun pairs(key: String): List<Pair<Int, Int>> = required(key).asJsonArray.map { it.asJsonArray.let { p -> p[0].asInt to p[1].asInt } }

    /** A list of `[lo, hi]` ranges. */
    fun ranges(key: String): List<IntRange> = required(key).asJsonArray.map { it.asJsonArray.let { p -> p[0].asInt..p[1].asInt } }

    private fun required(key: String): JsonElement =
        obj.get(key)?.takeUnless { it.isJsonNull } ?: throw IllegalArgumentException("generator parameter '$key' missing in ${obj.keySet()}")
}

data class RecipeSecretGoal(val id: String, val condition: String)

/** One recipe: a node bound to an engine (specs/mechanics/README.md). */
data class Recipe(
    val node: String,
    val engine: Engine,
    /** `kit` for build, `instrument` for manipulate. */
    val binding: String,
    val instrument2: String?,
    val target: String,
    val representations: List<Representation>,
    val frame: String,
    private val generator: Map<Band, Params>,
    val tolerance: Params,
    /** Node strategy name → engine detection rule id. */
    val strategies: Map<String, String>,
    /** Hint rung name → recipe detail. */
    val hints: Map<String, String>,
    val secretGoals: List<RecipeSecretGoal>,
) {
    val key: RecipeKey get() = RecipeKey(node, engine)
    fun params(band: Band): Params = generator.getValue(band)
    val observesStrategies: Boolean get() = strategies.isNotEmpty()
    /** Rule id → node strategy name, for turning a detected rule into an ItemResult.strategy. */
    val strategyForRule: Map<String, String> get() = strategies.entries.associate { (name, rule) -> rule to name }
}

data class RecipeKey(val node: String, val engine: Engine)

/** All recipes for the worlds loaded, with the cross-checks tools/recipes.py performs. */
class Recipes(list: List<Recipe>) {
    val all: List<Recipe> = list
    private val byKey: Map<RecipeKey, Recipe> = list.associateBy { it.key }

    operator fun get(node: String, engine: Engine): Recipe? = byKey[RecipeKey(node, engine)]
    fun getValue(node: String, engine: Engine): Recipe = get(node, engine) ?: throw NoSuchElementException("No recipe for $node/${engine.key}")
    fun forNode(node: String): List<Recipe> = all.filter { it.node == node }
    fun forEngine(engine: Engine): List<Recipe> = all.filter { it.engine == engine }

    fun validate(curriculum: Curriculum): List<String> {
        val problems = mutableListOf<String>()
        val seen = mutableSetOf<RecipeKey>()
        for (r in all) {
            val tag = "${r.node}/${r.engine.key}"
            val node = curriculum.nodes[r.node]
            if (node == null) { problems += "$tag: unknown node"; continue }
            if (r.engine !in node.engines) problems += "$tag: node does not list engine ${r.engine.key}"
            if (!seen.add(r.key)) problems += "$tag: duplicate recipe"
            val extra = r.representations - node.representations.toSet()
            if (extra.isNotEmpty()) problems += "$tag: representations not on node: $extra"
            val spec = EngineSpec.of(r.engine)
            if (spec == null) { problems += "$tag: engine has no spec yet"; continue }
            if (r.binding !in spec.bindings) problems += "$tag: unknown ${spec.bindingName} ${r.binding}"
            if (r.instrument2 != null && r.instrument2 !in spec.bindings) problems += "$tag: unknown instrument2 ${r.instrument2}"
            if (r.target !in spec.targets) problems += "$tag: unknown target ${r.target}"
            if (r.hints.keys != spec.hints) problems += "$tag: hint rungs must be ${spec.hints}, got ${r.hints.keys}"
            for ((name, rule) in r.strategies) {
                if (name !in node.strategies) problems += "$tag: strategy $name is not on the node"
                if (rule !in spec.rules) problems += "$tag: unknown detection rule $rule"
            }
            val goalIds = node.secretGoals.map { it.id }.toSet()
            for (g in r.secretGoals) if (g.id !in goalIds) problems += "$tag: secret goal ${g.id} is not on the node"
        }
        return problems
    }
}

/** What each engine spec defines (engine-build.md, engine-manipulate.md). Mirrors ENGINES in tools/recipes.py. */
class EngineSpec(val bindingName: String, val bindings: Set<String>, val targets: Set<String>, val rules: Set<String>, val hints: Set<String>) {
    companion object {
        val BUILD = EngineSpec(
            bindingName = "kit",
            bindings = setOf("tiles", "blocks", "parts", "strokes", "marks", "gears", "units"),
            targets = setOf("quantity", "product", "quotient", "factor-pairs", "constraint", "angle", "plot", "sequence", "table", "two-ways"),
            rules = setOf(
                "split-place-value", "split-one-dimension", "whole-array", "rows-of", "chunked-quotient", "all-pairs-ascending",
                "bundle", "unbundle", "unit-pieces", "grouped-copies", "regroup-whole", "edge-first", "mirror-pairs",
            ),
            hints = setOf("structure", "piece", "readout"),
        )
        val MANIPULATE = EngineSpec(
            bindingName = "instrument",
            bindings = setOf("number-line", "fraction-bar", "grid", "angle", "place-chart", "balance", "area-model", "graph"),
            targets = setOf("value", "nearest", "equal", "partition", "unknown", "match", "symmetric"),
            rules = setOf(
                "midpoint-first", "zoom-then-place", "recut-only", "regroup-whole", "benchmark-compare", "shift-digits",
                "quarter-turns", "complement-first", "fold-test",
            ),
            hints = setOf("zoom", "nudge", "show"),
        )

        fun of(engine: Engine): EngineSpec? = when (engine) {
            Engine.BUILD -> BUILD
            Engine.MANIPULATE -> MANIPULATE
            else -> null
        }
    }
}
