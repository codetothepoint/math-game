package com.codetothepoint.mathgame.core.recipes

import com.codetothepoint.mathgame.core.curriculum.Engine
import com.codetothepoint.mathgame.core.curriculum.Representation
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Reads specs/mechanics/recipes/generated/world-NN.json (emitted by tools/recipes.py). */
object RecipeLoader {
    private val WORLDS = 1..12

    /** Every bundled world file that exists. */
    fun fromResources(): Recipes = Recipes(WORLDS.flatMap { fromResource(it)?.all.orEmpty() })

    fun fromResource(world: Int): Recipes? {
        val path = "/recipes/world-%02d.json".format(world)
        val stream = RecipeLoader::class.java.getResourceAsStream(path) ?: return null
        return stream.reader().use { fromJson(it.readText()) }
    }

    fun fromJson(json: String): Recipes {
        val root = JsonParser.parseString(json).asJsonObject
        return Recipes(root.getAsJsonArray("recipes").map { parse(it.asJsonObject) })
    }

    private fun parse(r: JsonObject): Recipe {
        val engine = Engine.fromKey(r.get("engine").asString)
        val binding = when (engine) {
            Engine.BUILD -> r.get("kit")?.asString
            Engine.MANIPULATE -> r.get("instrument")?.asString
            else -> null
        } ?: throw IllegalArgumentException("recipe ${r.get("node").asString}/${engine.key}: missing kit or instrument")
        val gen = r.getAsJsonObject("generator")
        val bands = Band.entries.associateWith { band ->
            val obj = gen.get(band.key)?.asJsonObject ?: gen   // a single parameter set serves all bands
            Params(obj)
        }
        return Recipe(
            node = r.get("node").asString,
            engine = engine,
            binding = binding,
            instrument2 = r.get("instrument2")?.takeUnless { it.isJsonNull }?.asString,
            target = r.get("target").asString,
            representations = r.getAsJsonArray("representations").map { Representation.fromKey(it.asString) },
            frame = r.get("frame")?.takeUnless { it.isJsonNull }?.asString.orEmpty(),
            generator = bands,
            tolerance = Params(r.get("tolerance")?.takeUnless { it.isJsonNull }?.asJsonObject ?: JsonObject()),
            strategies = r.get("strategies")?.takeUnless { it.isJsonNull }?.asJsonObject?.entrySet()?.associate { it.key to it.value.asString }.orEmpty(),
            hints = r.getAsJsonObject("hints").entrySet().associate { it.key to it.value.asString },
            secretGoals = r.get("secret_goals")?.takeUnless { it.isJsonNull }?.asJsonArray?.map { it.asJsonObject }
                ?.map { RecipeSecretGoal(it.get("id").asString, it.get("condition").asString) }.orEmpty(),
        )
    }
}
