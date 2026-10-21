package com.codetothepoint.mathgame.core.engines.manipulate

import com.codetothepoint.mathgame.core.engines.Fraction
import kotlin.math.abs

/**
 * Satisfaction, tolerance, and strategy detection for the `manipulate` engine
 * (engine-manipulate.md §4, §6, §7). Pure.
 */
object ManipulateEngine {

    val RULE_PRIORITY = listOf(
        "midpoint-first", "zoom-then-place", "recut-only", "regroup-whole", "benchmark-compare",
        "shift-digits", "complement-first", "quarter-turns", "fold-test",
    )

    fun satisfied(target: ManipulateTarget, s: InstrumentState): Boolean = when (target) {
        is ManipulateTarget.LineValue -> s is NumberLineState && s.marker != null && onTick(s.marker, s.tick) && s.marker.compareTo(target.value) == 0
        is ManipulateTarget.Nearest -> s is NumberLineState && s.marker != null && s.marker.compareTo(Fraction.whole(target.rounded.toInt())) == 0
        is ManipulateTarget.Equal -> s is FractionBarState && s.den == target.targetDen && s.shadedPieces.size == 1 && s.amount.compareTo(target.reference) == 0
        is ManipulateTarget.Partition -> s is FractionBarState && s.den == target.den && s.shadedPieces.size == target.parts &&
            s.shadedPieces.all { it > 0 } && s.amount.compareTo(target.value) == 0
        is ManipulateTarget.BarValue -> s is FractionBarState && s.den == target.den && s.amount.compareTo(target.value) == 0
        is ManipulateTarget.GridValue -> s is GridState && s.shaded == target.hundredths
        is ManipulateTarget.AngleValue -> s is AngleState && abs((s.reading ?: s.degrees) - target.degrees) <= target.toleranceDegrees
        is ManipulateTarget.AngleUnknown -> s is AngleState && abs(s.degrees - target.unknown) <= target.toleranceDegrees
        is ManipulateTarget.ChartValue -> s is PlaceChartState && s.value == target.value
        is ManipulateTarget.Match -> s is MatchState && s.grid.shaded == target.hundredths && s.line.marker != null &&
            onTick(s.line.marker, s.line.tick) && s.line.marker.compareTo(Fraction.of(target.hundredths, 100)) == 0
        is ManipulateTarget.Symmetric -> s is MirrorState && symmetricSatisfied(target, s)
    }

    fun detectStrategy(target: ManipulateTarget, s: InstrumentState, declared: Set<String>): String? =
        RULE_PRIORITY.firstOrNull { it in declared && exhibits(it, target, s) }

    fun exhibits(rule: String, target: ManipulateTarget, s: InstrumentState): Boolean = when (rule) {
        "midpoint-first" -> target is ManipulateTarget.Nearest && s is NumberLineState &&
            s.pauses.any { it.compareTo(Fraction.whole(target.midpoint.toInt())) == 0 }
        "zoom-then-place" -> s is NumberLineState && s.zoomedBeforeFirstPlacement
        "recut-only" -> s is FractionBarState && s.den != s.initialDen && !s.edgeMoved
        "regroup-whole" -> s is FractionBarState && s.mergedWholes > 0
        "benchmark-compare" -> s is FractionBarState && s.usedHalfMarker
        "shift-digits" -> s is PlaceChartState && s.digitShifts > 0 && !s.typed
        "quarter-turns" -> s is AngleState && s.quarterTurnDetentsUsed > 0
        "complement-first" -> target is ManipulateTarget.AngleUnknown && s is AngleState && s.knownSetFirst
        "fold-test" -> s is MirrorState && s.flips > 0
        else -> false
    }

    /** A marker sits on a tick when its value is a whole number of ticks. */
    fun onTick(value: Fraction, tick: Fraction): Boolean {
        val q = value.num.toLong() * tick.den
        val r = tick.num.toLong() * value.den
        return r != 0L && q % r == 0L
    }

    private fun symmetricSatisfied(t: ManipulateTarget.Symmetric, s: MirrorState): Boolean {
        val lines = t.lines
        if (t.askAll) return s.lockedLines == lines && (lines.isNotEmpty() || s.lockedNone)
        if (lines.isEmpty()) return s.lockedNone
        return s.mirror != null && s.mirror in lines
    }
}
