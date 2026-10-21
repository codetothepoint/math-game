package com.codetothepoint.mathgame.core.mastery

import com.codetothepoint.mathgame.core.curriculum.Engine
import com.codetothepoint.mathgame.core.curriculum.Representation

/** Milliseconds in one day. Timestamps throughout core are epoch milliseconds. */
const val DAY_MS: Long = 86_400_000L

/** Why an item was served (specs/mastery.md §3). */
enum class ItemKind { CORE, REVISIT, REACH }

/** What an engine reports for one item. Engines never score (specs/mastery.md §3). */
data class ItemResult(
    val nodeId: String,
    val representation: Representation,
    val engine: Engine,
    val correct: Boolean,
    val firstTry: Boolean = true,
    val hints: Int = 0,
    /** Target shown → final correct answer; null when incorrect. */
    val responseMs: Int? = null,
    /** A strategy from the node's list, if the engine observed one. */
    val strategy: String? = null,
    val kind: ItemKind = ItemKind.CORE,
    val at: Long = 0L,
)

/** What an engine reports when a level ends (specs/mastery.md §3). */
data class LevelResult(
    val nodeId: String,
    val engine: Engine,
    val representation: Representation,
    val completed: Boolean,
    val items: List<ItemResult>,
    val observesStrategies: Boolean = false,
    val secretGoalsHit: List<String> = emptyList(),
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
)
