package com.codetothepoint.mathgame.core.curriculum

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.Reader

/**
 * Reads specs/curriculum/generated/curriculum.json (the output of tools/curriculum.py).
 *
 * Parsing is explicit tree mapping rather than reflection so that a field the Python tool adds
 * or renames fails loudly here instead of silently becoming null.
 */
object CurriculumLoader {
    private const val RESOURCE = "/curriculum/curriculum.json"

    /** Loads the curriculum bundled with core (synced from specs/ at build time). */
    fun fromResource(): Curriculum {
        val stream = CurriculumLoader::class.java.getResourceAsStream(RESOURCE)
            ?: throw IllegalStateException("Bundled curriculum missing at $RESOURCE; run the syncSpecs task")
        return stream.reader().use { fromReader(it) }
    }

    fun fromJson(json: String): Curriculum = fromElement(JsonParser.parseString(json))

    fun fromReader(reader: Reader): Curriculum = fromElement(JsonParser.parseReader(reader))

    private fun fromElement(root: JsonElement): Curriculum {
        val obj = root.asJsonObject
        val worlds = obj.array("worlds").map { it.asJsonObject }.map { w ->
            WorldInfo(w.int("world"), w.string("name"), w.string("tier"), w.stringOr("notes", ""))
        }
        val nodes = obj.array("nodes").map { parseNode(it.asJsonObject) }
        return Curriculum(obj.string("generated"), worlds, nodes)
    }

    private fun parseNode(n: JsonObject): SkillNode {
        val m = n.obj("mastery")
        return SkillNode(
            id = n.string("id"),
            world = n.int("world"),
            tier = n.string("tier"),
            domain = n.string("domain"),
            domainName = n.string("domain_name"),
            title = n.string("title"),
            description = n.string("description"),
            source = n.string("source"),
            ccss = n.array("ccss").map { it.asString },
            prerequisites = n.array("prerequisites").map { it.asString },
            dependents = n.array("dependents").map { it.asString },
            representations = n.array("representations").map { Representation.fromKey(it.asString) },
            engines = n.array("engines").map { Engine.fromKey(it.asString) },
            mastery = MasteryThresholds(
                unlock = m.double("unlock"),
                twoStar = m.double("two_star"),
                threeStar = m.double("three_star"),
                revisitDays = m.array("revisit_days").map { it.asInt },
                fluencyMs = m.get("fluency_ms")?.takeUnless { it.isJsonNull }?.asInt?.takeIf { it > 0 },
            ),
            strategies = n.array("strategies").map { it.asString },
            secretGoals = n.array("secret_goals").map { it.asJsonObject }.map { g ->
                SecretGoal(g.string("id"), g.string("text"), g.get("kind")?.takeUnless { it.isJsonNull }?.asString)
            },
            depth = n.int("depth"),
        )
    }

    private fun JsonObject.required(key: String): JsonElement =
        get(key)?.takeUnless { it.isJsonNull } ?: throw IllegalArgumentException("curriculum.json: missing '$key' in ${keySet()}")

    private fun JsonObject.string(key: String): String = required(key).asString
    private fun JsonObject.stringOr(key: String, default: String): String = get(key)?.takeUnless { it.isJsonNull }?.asString ?: default
    private fun JsonObject.int(key: String): Int = required(key).asInt
    private fun JsonObject.double(key: String): Double = required(key).asDouble
    private fun JsonObject.obj(key: String): JsonObject = required(key).asJsonObject
    private fun JsonObject.array(key: String): JsonArray = required(key).asJsonArray
}
