package com.codetothepoint.mathgame.core.levels

import com.codetothepoint.mathgame.core.engines.Polygon
import com.codetothepoint.mathgame.core.engines.build.BuildConstruction
import com.codetothepoint.mathgame.core.engines.build.BuildEngine
import com.codetothepoint.mathgame.core.engines.build.BuildTarget
import com.codetothepoint.mathgame.core.engines.build.GearsConstruction
import com.codetothepoint.mathgame.core.engines.build.StrokesConstruction
import com.codetothepoint.mathgame.core.engines.build.TilesConstruction
import com.codetothepoint.mathgame.core.engines.isPrime
import com.codetothepoint.mathgame.core.engines.manipulate.AngleState
import com.codetothepoint.mathgame.core.engines.manipulate.FractionBarState
import com.codetothepoint.mathgame.core.engines.manipulate.InstrumentState
import com.codetothepoint.mathgame.core.engines.manipulate.ManipulateEngine
import com.codetothepoint.mathgame.core.engines.manipulate.ManipulateTarget
import com.codetothepoint.mathgame.core.engines.manipulate.MatchState
import com.codetothepoint.mathgame.core.engines.manipulate.NumberLineState
import com.codetothepoint.mathgame.core.engines.manipulate.PlaceChartState
import com.codetothepoint.mathgame.core.recipes.Band

/** What the engine observed on one core item, for secret-goal evaluation. */
data class ItemRecord(
    val item: GeneratedItem,
    val correct: Boolean,
    val firstTry: Boolean,
    val hints: Int,
    /** The construction at the first lock-in (build). */
    val construction: BuildConstruction? = null,
    /** The instrument state at the final lock-in (manipulate). */
    val state: InstrumentState? = null,
)

/**
 * Evaluates the world-4 secret goals from what engines can observe (recipes/world-04.yaml `secret_goals`).
 * Returns the goal ids achieved in this level. Never affects score or stars (specs/mastery.md §9).
 */
object SecretGoals {

    fun achieved(goalIds: List<String>, records: List<ItemRecord>): List<String> = goalIds.filter { holds(it, records) }

    fun holds(goalId: String, records: List<ItemRecord>): Boolean = when (goalId) {
        // ---- build
        "prime-call" -> records.any { r ->
            val t = r.item.buildTarget() as? BuildTarget.FactorPairs
            val c = r.construction as? TilesConstruction
            t != null && c != null && isPrime(t.area) && r.correct && r.firstTry && r.hints == 0 && c.rects.size == 1
        }
        "fewest-gears" -> records.any { r ->
            val t = r.item.buildTarget() as? BuildTarget.Sequence
            val c = r.construction as? GearsConstruction
            t != null && c != null && r.correct && c.gears.size == t.minimalGears
        }
        "bundle-up" -> anyRule(records, "bundle")
        "four-partials" -> records.any { r ->
            val t = r.item.buildTarget() as? BuildTarget.Product
            t != null && t.a >= 10 && t.b >= 10 && r.correct && r.firstTry && r.construction != null && BuildEngine.exhibits("split-place-value", t, r.construction)
        }
        "remainder-tray" -> records.filter { (it.item.buildTarget() as? BuildTarget.Quotient)?.remainder?.let { rem -> rem > 0 } == true }
            .let { withRem -> withRem.isNotEmpty() && withRem.all { it.correct && it.firstTry } }
        "two-ways-unit" -> anyRule(records, "unit-pieces")
        "copies" -> anyRule(records, "grouped-copies")
        "trade-strip" -> anyRule(records, "unbundle")
        "same-area-two-ways" -> records.any { r ->
            val t = r.item.buildTarget() as? BuildTarget.RectConstraint
            val c = r.construction as? TilesConstruction
            t?.area != null && c != null && r.correct && c.rects.size >= 2 && c.rects.map { it.dims }.distinct().size >= 2 && c.rects.all { it.area == t.area }
        }
        "different-each-time" -> records.windowed(3).any { w ->
            w.all { it.correct } && w.mapNotNull { (it.construction as? StrokesConstruction)?.let { s -> Polygon.from(s.segments) } }.let { polys ->
                polys.size == 3 && !polys[0].congruentTo(polys[1]) && !polys[1].congruentTo(polys[2]) && !polys[0].congruentTo(polys[2])
            }
        }
        // ---- manipulate
        "one-shift" -> records.any { r -> (r.state as? PlaceChartState)?.let { r.correct && it.leverPulls == 1 && it.digitShifts == 0 } == true }
        "midpoint-call" -> records.any { r -> r.item.band == Band.HIGH && r.correct && manipulateRule(r, "midpoint-first") }
        "same-edge" -> records.windowed(3).any { w -> w.all { it.correct && manipulateRule(it, "recut-only") } }
        "tenths-first" -> records.isNotEmpty() && records.all { r ->
            val line = (r.state as? MatchState)?.line ?: r.state as? NumberLineState
            line != null && r.correct && line.firstPlacement != null && line.tickAtFirstPlacement?.den == 10 && ManipulateEngine.onTick(line.firstPlacement, line.tickAtFirstPlacement)
        }
        "full-circle" -> records.any { r -> r.item.band == Band.LOW && r.correct && (r.state as? AngleState)?.let { it.maxRotation >= 360.0 } == true }
        "zero-first" -> records.isNotEmpty() && records.all { r -> r.correct && (r.state as? AngleState)?.zeroAligned == true }
        "all-the-lines" -> records.any { r ->
            val t = r.item.manipulateTarget() as? ManipulateTarget.Symmetric
            t != null && t.askAll && r.correct && t.lines.size >= 2
        }
        else -> false
    }

    private fun anyRule(records: List<ItemRecord>, rule: String): Boolean = records.any { r ->
        val t = r.item.buildTarget()
        t != null && r.construction != null && r.correct && BuildEngine.exhibits(rule, t, r.construction)
    }

    private fun manipulateRule(r: ItemRecord, rule: String): Boolean {
        val t = r.item.manipulateTarget() ?: return false
        val s = r.state ?: return false
        return ManipulateEngine.exhibits(rule, t, s)
    }

    private fun GeneratedItem.buildTarget(): BuildTarget? = (this as? GeneratedItem.Build)?.target
    private fun GeneratedItem.manipulateTarget(): ManipulateTarget? = (this as? GeneratedItem.Manipulate)?.target
}
