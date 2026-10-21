package com.codetothepoint.mathgame.core.levels

import com.codetothepoint.mathgame.core.curriculum.Engine
import com.codetothepoint.mathgame.core.curriculum.Representation
import com.codetothepoint.mathgame.core.engines.build.BuildTarget
import com.codetothepoint.mathgame.core.engines.manipulate.InstrumentState
import com.codetothepoint.mathgame.core.engines.manipulate.ManipulateTarget
import com.codetothepoint.mathgame.core.mastery.ItemKind
import com.codetothepoint.mathgame.core.recipes.Band

/** An item an engine can present: the target, plus an initial instrument setting for `manipulate`. */
sealed interface GeneratedItem {
    val band: Band
    val representation: Representation

    data class Build(override val band: Band, override val representation: Representation, val target: BuildTarget) : GeneratedItem

    data class Manipulate(override val band: Band, override val representation: Representation, val target: ManipulateTarget, val initial: InstrumentState) : GeneratedItem
}

/** One slot of a level: which node it is evidence for, why it is here, and what to present. */
data class PlannedItem(val nodeId: String, val kind: ItemKind, val item: GeneratedItem)

/** A level ready to play (engine-build.md §2, engine-manipulate.md §2). */
data class LevelPlan(
    val nodeId: String,
    val engine: Engine,
    val representation: Representation,
    val band: Band,
    val items: List<PlannedItem>,
    val observesStrategies: Boolean,
) {
    val coreItems: Int get() = items.count { it.nodeId == nodeId }
}
