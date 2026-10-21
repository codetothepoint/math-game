package com.codetothepoint.mathgame.core.mastery

import com.codetothepoint.mathgame.core.curriculum.Representation

/** One pass or fail of a revisit set, kept so the portal can count recent passes. */
data class RevisitOutcome(val at: Long, val passed: Boolean)

/** Progress of spaced revisits for a node that reached two stars (specs/mastery.md §2, §7). */
class RevisitState(
    var dueAt: Long,
    var intervalIndex: Int = 0,
    var representation: Representation? = null,
    var served: Int = 0,
    var correct: Int = 0,
    var hints: Int = 0,
    var passed: Int = 0,
    var lastSetPassed: Boolean? = null,
    val history: MutableList<RevisitOutcome> = mutableListOf(),
)

/** Everything the game knows about one player on one node (specs/mastery.md §2). */
class NodeState {
    var score: Double = 0.0
    var assumed: Boolean = false
    var stars: Int = 0
    var items: Int = 0
    val recent: ArrayDeque<ItemResult> = ArrayDeque()
    val repItems: MutableMap<Representation, Int> = mutableMapOf()
    val strategies: MutableSet<String> = mutableSetOf()
    var strategyObservable: Boolean = false
    var fluencyMs: Int? = null
    val cleanTimes: ArrayDeque<Int> = ArrayDeque()
    var levelsCompleted: Int = 0
    var lastLevelRepresentation: Representation? = null
    val secretGoals: MutableSet<String> = mutableSetOf()
    var twoStarAt: Long? = null
    var threeStarAt: Long? = null
    var revisit: RevisitState? = null
    var lastSeen: Long? = null
}

/** A player's whole state across the graph. */
class Profile(var startingWorld: Int, val nodes: Map<String, NodeState>)

/** Availability of a node from its prerequisites (specs/mastery.md §6.1). */
enum class Availability { LOCKED, REACH, AVAILABLE }

/** What the world map shows for a node (specs/mastery.md §11). */
enum class MapNodeState { ASSUMED, LOCKED, REACH, AVAILABLE }

data class MapState(val state: MapNodeState, val stars: Int, val due: Boolean)
