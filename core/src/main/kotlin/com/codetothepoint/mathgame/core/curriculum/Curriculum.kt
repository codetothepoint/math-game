package com.codetothepoint.mathgame.core.curriculum

/** The forms a skill can be practiced in (specs/mechanics/README.md, Representations). */
enum class Representation(val key: String) {
    SYMBOLIC("symbolic"), VISUAL("visual"), VERBAL("verbal"), PHYSICAL("physical");

    companion object {
        fun fromKey(key: String): Representation = entries.firstOrNull { it.key == key }
            ?: throw IllegalArgumentException("Unknown representation: $key")
    }
}

/** The seven mini-game engines (specs/mechanics/README.md, Engines). */
enum class Engine(val key: String) {
    FLUENCY("fluency"), BUILD("build"), SORT("sort"), MANIPULATE("manipulate"), STORY("story"), STRATEGY("strategy"), SIM("sim");

    companion object {
        fun fromKey(key: String): Engine = entries.firstOrNull { it.key == key }
            ?: throw IllegalArgumentException("Unknown engine: $key")
    }
}

/** Mastery thresholds for one node, defaults already merged (specs/curriculum/README.md). */
data class MasteryThresholds(
    val unlock: Double,
    val twoStar: Double,
    val threeStar: Double,
    val revisitDays: List<Int>,
    val fluencyMs: Int? = null,
)

data class SecretGoal(val id: String, val text: String, val kind: String?)

/** One skill node of the curriculum graph. */
data class SkillNode(
    val id: String,
    val world: Int,
    val tier: String,
    val domain: String,
    val domainName: String,
    val title: String,
    val description: String,
    val source: String,
    val ccss: List<String>,
    val prerequisites: List<String>,
    val dependents: List<String>,
    val representations: List<Representation>,
    val engines: List<Engine>,
    val mastery: MasteryThresholds,
    val strategies: List<String>,
    val secretGoals: List<SecretGoal>,
    val depth: Int,
) {
    val isEntry: Boolean get() = prerequisites.isEmpty()
}

data class WorldInfo(val world: Int, val name: String, val tier: String, val notes: String)

/**
 * The whole skill graph (specs/curriculum/). Immutable once loaded. [validate] re-checks the
 * invariants tools/curriculum.py enforces, so a bundled file that drifted is caught at test time.
 */
class Curriculum(
    val generated: String,
    val worlds: List<WorldInfo>,
    nodeList: List<SkillNode>,
) {
    val nodes: Map<String, SkillNode> = nodeList.associateBy { it.id }
    private val byWorld: Map<Int, List<SkillNode>> = nodeList.groupBy { it.world }

    operator fun get(id: String): SkillNode = nodes[id] ?: throw NoSuchElementException("No node $id")
    fun nodesIn(world: Int): List<SkillNode> = byWorld[world].orEmpty()
    fun world(world: Int): WorldInfo = worlds.first { it.world == world }

    /** Problems with the graph; empty when it is sound. Mirrors tools/curriculum.py `validate`. */
    fun validate(): List<String> {
        val problems = mutableListOf<String>()
        val idPattern = Regex("^w(0[1-9]|1[0-2])\\.[a-z]+\\.[a-z0-9-]+$")
        for (n in nodes.values) {
            val m = idPattern.matchEntire(n.id)
            if (m == null) problems += "${n.id}: malformed id"
            else if (m.groupValues[1].toInt() != n.world) problems += "${n.id}: id world does not match node world ${n.world}"
            if (n.source == "ccss" && n.ccss.isEmpty()) problems += "${n.id}: source is ccss but lists no standards"
            if (n.source == "original" && n.ccss.isNotEmpty()) problems += "${n.id}: source is original but lists standards"
            if (n.representations.size < 2) problems += "${n.id}: fewer than two representations"
            if (n.engines.isEmpty()) problems += "${n.id}: no engines"
            if (n.prerequisites.toSet().size != n.prerequisites.size) problems += "${n.id}: duplicate prerequisites"
            for (p in n.prerequisites) {
                val pn = nodes[p]
                when {
                    p == n.id -> problems += "${n.id}: lists itself as a prerequisite"
                    pn == null -> problems += "${n.id}: unknown prerequisite $p"
                    pn.world > n.world -> problems += "${n.id}: prerequisite $p is in a later world"
                }
            }
        }
        if (problems.isNotEmpty()) return problems
        // Kahn's algorithm; anything left over is in a cycle.
        val indegree = nodes.values.associate { it.id to it.prerequisites.size }.toMutableMap()
        val dependents = mutableMapOf<String, MutableList<String>>()
        for (n in nodes.values) for (p in n.prerequisites) dependents.getOrPut(p) { mutableListOf() } += n.id
        val queue = ArrayDeque(indegree.filterValues { it == 0 }.keys.sorted())
        var visited = 0
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            visited++
            for (d in dependents[id].orEmpty()) {
                val left = indegree.getValue(d) - 1
                indegree[d] = left
                if (left == 0) queue.addLast(d)
            }
        }
        if (visited != nodes.size) {
            problems += "cycle detected involving: " + indegree.filterValues { it > 0 }.keys.sorted().take(20).joinToString(", ")
        }
        return problems
    }
}
