package com.codetothepoint.mathgame.core.engines.build

import com.codetothepoint.mathgame.core.engines.Fraction
import com.codetothepoint.mathgame.core.engines.MirrorLine
import com.codetothepoint.mathgame.core.engines.Polygon
import com.codetothepoint.mathgame.core.engines.Stroke
import com.codetothepoint.mathgame.core.engines.angleDegrees
import com.codetothepoint.mathgame.core.engines.factorPairs
import com.codetothepoint.mathgame.core.engines.placeValueCuts
import kotlin.math.abs

/**
 * Satisfaction and strategy detection for the `build` engine (engine-build.md §4, §7).
 * Pure: a target and a construction in, a verdict out. The UI never decides either.
 */
object BuildEngine {

    /** Rule ids in priority order; the first match wins (engine-build.md §7). */
    val RULE_PRIORITY = listOf(
        "split-place-value", "split-one-dimension", "chunked-quotient", "rows-of", "all-pairs-ascending", "whole-array",
        "bundle", "unbundle", "grouped-copies", "regroup-whole", "unit-pieces", "mirror-pairs", "edge-first",
    )

    fun satisfied(target: BuildTarget, c: BuildConstruction): Boolean = when (target) {
        is BuildTarget.Quantity -> c is BlocksConstruction && c.total == target.value
        is BuildTarget.FractionQuantity -> c is PartsConstruction && c.ways.size == 1 && c.sums[0].compareTo(target.value) == 0 && c.ways[0].all { it.den in target.denominators }
        is BuildTarget.Product -> c is TilesConstruction && c.rects.size == 1 && c.rects[0].dims == setOf(target.a, target.b) && c.tray == 0
        is BuildTarget.Quotient -> c is TilesConstruction && quotientSatisfied(target, c)
        is BuildTarget.FactorPairs -> c is TilesConstruction && c.rects.map { minOf(it.w, it.h) to maxOf(it.w, it.h) }.toSet() == factorPairs(target.area).toSet() && c.rects.size == factorPairs(target.area).size
        is BuildTarget.RectConstraint -> c is TilesConstruction && c.rects.isNotEmpty() && c.rects.all { rectMeets(target, it) }
        is BuildTarget.ShapeConstraint -> c is StrokesConstruction && Constraints.holds(target.constraint, c)
        is BuildTarget.SymmetricCompletion -> c is StrokesConstruction && symmetricCompletion(target, c)
        is BuildTarget.Angle -> c is StrokesConstruction && angleSatisfied(target, c)
        is BuildTarget.Plot -> c is MarksConstruction && c.marks.sorted() == target.values.sorted()
        is BuildTarget.Sequence -> c is GearsConstruction && c.gears.isNotEmpty() && c.terms(target.terms.size) == target.terms
        is BuildTarget.Table -> c is UnitsConstruction && target.rows.all { c.rows[it] == it * target.factor } && c.rows.keys.all { it in target.rows }
        is BuildTarget.TwoWays -> c is PartsConstruction && c.ways.size == 2 && c.sums.all { it.compareTo(target.value) == 0 } &&
            c.ways.all { w -> w.all { it.den in target.denominators } } && !sameMultiset(c.ways[0], c.ways[1])
    }

    /** The highest-priority rule the construction exhibits, among [declared]; null when none. */
    fun detectStrategy(target: BuildTarget, c: BuildConstruction, declared: Set<String>): String? =
        RULE_PRIORITY.firstOrNull { it in declared && exhibits(it, target, c) }

    /** Whether [rule] holds for this construction, regardless of what the recipe declared. */
    fun exhibits(rule: String, target: BuildTarget, c: BuildConstruction): Boolean = when (rule) {
        "split-place-value" -> target is BuildTarget.Product && c is TilesConstruction && c.rects.size == 1 && placeValueSplit(c.rects[0]) == Split.BOTH
        "split-one-dimension" -> target is BuildTarget.Product && c is TilesConstruction && c.rects.size == 1 && placeValueSplit(c.rects[0]) == Split.ONE
        "whole-array" -> c is TilesConstruction && c.rects.isNotEmpty() && c.rects.all { it.cutsAlongW.isEmpty() && it.cutsAlongH.isEmpty() }
        "rows-of" -> target is BuildTarget.Quotient && c is TilesConstruction && c.placements.isNotEmpty() && c.placements.all { it.first == 1 }
        "chunked-quotient" -> target is BuildTarget.Quotient && c is TilesConstruction && c.placements.isNotEmpty() &&
            c.placements.first().let { it.first == target.divisor && it.second >= 10 }
        "all-pairs-ascending" -> target is BuildTarget.FactorPairs && c is TilesConstruction && c.rects.size >= 2 &&
            c.rects.map { minOf(it.w, it.h) }.zipWithNext().all { (a, b) -> a < b }
        "bundle" -> c is BlocksConstruction && c.bundles > 0
        "unbundle" -> c is BlocksConstruction && c.unbundles > 0
        "unit-pieces" -> c is PartsConstruction && c.ways.any { w -> w.isNotEmpty() && w.all { it.isUnit } }
        "grouped-copies" -> c is PartsConstruction && c.duplications > 0
        "regroup-whole" -> c is PartsConstruction && c.mergedWholes > 0
        "edge-first" -> c is StrokesConstruction && edgeFirst(c)
        "mirror-pairs" -> target is BuildTarget.SymmetricCompletion && c is StrokesConstruction && mirrorPairs(target.line, c)
        else -> false
    }

    // ---- tiles ----

    enum class Split { NONE, ONE, BOTH }

    /** Whether a product rectangle is cut on place-value lines in both dimensions that have them, one, or none/other. */
    fun placeValueSplit(r: TileRect): Split {
        val pvW = placeValueCuts(r.w)
        val pvH = placeValueCuts(r.h)
        val wCut = r.cutsAlongW.isNotEmpty()
        val hCut = r.cutsAlongH.isNotEmpty()
        val wOnPv = pvW.isNotEmpty() && r.cutsAlongW == pvW
        val hOnPv = pvH.isNotEmpty() && r.cutsAlongH == pvH
        // Any cut off the place-value lines means no place-value strategy.
        if ((wCut && !wOnPv) || (hCut && !hOnPv)) return Split.NONE
        val dimsWithPv = listOf(pvW.isNotEmpty(), pvH.isNotEmpty()).count { it }
        val dimsCutOnPv = listOf(wOnPv, hOnPv).count { it }
        return when {
            dimsCutOnPv == 0 -> Split.NONE
            dimsCutOnPv == dimsWithPv -> Split.BOTH
            else -> Split.ONE
        }
    }

    private fun quotientSatisfied(t: BuildTarget.Quotient, c: TilesConstruction): Boolean {
        if (c.rects.size != 1) return false
        val r = c.rects[0]
        val rows = r.h
        val perRow = r.w
        return rows == t.divisor && perRow == t.quotient && c.tray == t.remainder && c.tray < t.divisor
    }

    private fun rectMeets(t: BuildTarget.RectConstraint, r: TileRect): Boolean =
        (t.area == null || r.area == t.area) &&
            (t.perimeter == null || 2 * (r.w + r.h) == t.perimeter) &&
            (t.givenSide == null || t.givenSide in r.dims)

    // ---- strokes ----

    private fun angleSatisfied(t: BuildTarget.Angle, c: StrokesConstruction): Boolean {
        val rays = c.rays
        if (rays.size != 2 || rays[0].from != rays[1].from) return false
        val deg = angleDegrees(rays[0].from, rays[0].through, rays[1].through)
        return abs(deg - t.degrees) <= t.toleranceDegrees
    }

    private fun symmetricCompletion(t: BuildTarget.SymmetricCompletion, c: StrokesConstruction): Boolean {
        val pts = c.points
        if (!t.given.all { it in pts }) return false
        val segs = c.segments.map { setOf(it.a, it.b) }.toSet()
        return pts.all { t.line.reflect(it) in pts } && segs.all { s -> s.map { t.line.reflect(it) }.toSet() in segs }
    }

    private fun edgeFirst(c: StrokesConstruction): Boolean {
        val segs = c.segments
        val poly = Polygon.from(segs) ?: return false
        if (segs.size != poly.n) return false
        // Every stroke is a side (no diagonals or interior strokes), placed before anything else.
        return c.strokes.take(segs.size).all { it is Stroke.Segment }
    }

    private fun mirrorPairs(line: MirrorLine, c: StrokesConstruction): Boolean {
        val segs = c.strokes.filterIsInstance<Stroke.Segment>()
        if (segs.size < 2 || segs.size % 2 != 0) return false
        return segs.chunked(2).all { (a, b) -> setOf(line.reflect(a.a), line.reflect(a.b)) == setOf(b.a, b.b) }
    }

    private fun sameMultiset(a: List<Fraction>, b: List<Fraction>): Boolean = a.sorted() == b.sorted()
}

/** Named figure constraints a `strokes` recipe can ask for (engine-build.md §10; recipes/world-04.yaml). */
object Constraints {
    val ALL: Set<String> = setOf(
        "one-segment", "one-ray", "one-line", "acute-angle", "right-angle", "obtuse-angle",
        "parallel-pair", "perpendicular-pair", "two-rays-sharing-endpoint",
        "parallel-pair-with-transversal", "perpendicular-through-point", "obtuse-angle-between-parallels",
        "right-triangle", "acute-triangle", "obtuse-triangle",
        "parallelogram", "rectangle-not-square", "trapezoid-exactly-one-pair",
        "rhombus-not-square", "quadrilateral-perpendicular-no-parallel", "isosceles-right-triangle",
    )

    fun holds(constraint: String, c: StrokesConstruction): Boolean {
        val poly = Polygon.from(c.segments)
        val onlyPolygon = poly != null && c.strokes.size == c.segments.size
        return when (constraint) {
            "one-segment" -> c.strokes.size == 1 && c.segments.size == 1
            "one-ray" -> c.strokes.size == 1 && c.rays.size == 1
            "one-line" -> c.strokes.size == 1 && c.lines.size == 1
            "acute-angle" -> rayAngle(c)?.let { it < 90.0 - 1e-9 && it > 0 } == true
            "right-angle" -> rayAngle(c)?.let { abs(it - 90.0) < 1e-9 } == true
            "obtuse-angle" -> rayAngle(c)?.let { it > 90.0 + 1e-9 && it < 180.0 } == true
            "two-rays-sharing-endpoint" -> c.strokes.size == 2 && c.rays.size == 2 && c.rays[0].from == c.rays[1].from && !c.rays[0].vec.isParallel(c.rays[1].vec)
            "parallel-pair" -> c.strokes.size == 2 && c.lines.size == 2 && c.lines[0].vec.isParallel(c.lines[1].vec) && !collinear(c.lines[0], c.lines[1])
            "perpendicular-pair" -> c.strokes.size == 2 && c.lines.size == 2 && c.lines[0].vec.isPerpendicular(c.lines[1].vec)
            "parallel-pair-with-transversal" -> c.strokes.size == 3 && c.lines.size == 3 && c.lines.indices.any { k ->
                val (p, q) = c.lines.filterIndexed { i, _ -> i != k }
                p.vec.isParallel(q.vec) && !collinear(p, q) && !c.lines[k].vec.isParallel(p.vec)
            }
            "perpendicular-through-point" -> c.lines.size == 2 && c.strokes.size == 3 && c.strokes.filterIsInstance<Stroke.Point>().size == 1 &&
                c.lines[0].vec.isPerpendicular(c.lines[1].vec) && c.lines.any { onLine(it, c.strokes.filterIsInstance<Stroke.Point>()[0].at) }
            "obtuse-angle-between-parallels" -> c.lines.size == 3 && c.strokes.size == 3 && c.lines.indices.any { k ->
                val (p, q) = c.lines.filterIndexed { i, _ -> i != k }
                val t = c.lines[k]
                p.vec.isParallel(q.vec) && !collinear(p, q) && !t.vec.isParallel(p.vec) && !t.vec.isPerpendicular(p.vec)
            }
            "right-triangle" -> onlyPolygon && poly!!.n == 3 && poly.hasRightAngle()
            "acute-triangle" -> onlyPolygon && poly!!.n == 3 && poly.angles.all { it < 90.0 - 1e-9 }
            "obtuse-triangle" -> onlyPolygon && poly!!.n == 3 && poly.angles.any { it > 90.0 + 1e-9 }
            "isosceles-right-triangle" -> onlyPolygon && poly!!.n == 3 && poly.hasRightAngle() && poly.sideLengthsSq.groupBy { it }.any { it.value.size == 2 }
            "parallelogram" -> onlyPolygon && poly!!.n == 4 && poly.parallelPairs() == 2
            "rectangle-not-square" -> onlyPolygon && poly!!.n == 4 && poly.perpendicularAdjacentCount() == 4 && !poly.allSidesEqual
            "rhombus-not-square" -> onlyPolygon && poly!!.n == 4 && poly.parallelPairs() == 2 && poly.allSidesEqual && !poly.hasRightAngle()
            "trapezoid-exactly-one-pair" -> onlyPolygon && poly!!.n == 4 && poly.parallelPairs() == 1
            "quadrilateral-perpendicular-no-parallel" -> onlyPolygon && poly!!.n == 4 && poly.parallelPairs() == 0 && poly.hasRightAngle()
            else -> throw IllegalArgumentException("Unknown constraint $constraint")
        }
    }

    private fun rayAngle(c: StrokesConstruction): Double? {
        if (c.strokes.size != 2 || c.rays.size != 2 || c.rays[0].from != c.rays[1].from) return null
        return angleDegrees(c.rays[0].from, c.rays[0].through, c.rays[1].through)
    }

    private fun collinear(p: Stroke.Line, q: Stroke.Line): Boolean = (q.a - p.a).cross(p.vec) == 0
    private fun onLine(l: Stroke.Line, pt: com.codetothepoint.mathgame.core.engines.Pt): Boolean = (pt - l.a).cross(l.vec) == 0
}
