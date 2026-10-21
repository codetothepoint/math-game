package com.codetothepoint.mathgame.core.levels

import com.codetothepoint.mathgame.core.engines.Fraction
import com.codetothepoint.mathgame.core.engines.MirrorLine
import com.codetothepoint.mathgame.core.engines.Pt
import com.codetothepoint.mathgame.core.engines.Stroke
import com.codetothepoint.mathgame.core.engines.build.BlocksConstruction
import com.codetothepoint.mathgame.core.engines.build.BuildConstruction
import com.codetothepoint.mathgame.core.engines.build.BuildTarget
import com.codetothepoint.mathgame.core.engines.build.Gear
import com.codetothepoint.mathgame.core.engines.build.GearsConstruction
import com.codetothepoint.mathgame.core.engines.build.MarksConstruction
import com.codetothepoint.mathgame.core.engines.build.PartsConstruction
import com.codetothepoint.mathgame.core.engines.build.StrokesConstruction
import com.codetothepoint.mathgame.core.engines.build.TileRect
import com.codetothepoint.mathgame.core.engines.build.TilesConstruction
import com.codetothepoint.mathgame.core.engines.build.UnitsConstruction
import com.codetothepoint.mathgame.core.engines.factorPairs
import com.codetothepoint.mathgame.core.engines.manipulate.AngleState
import com.codetothepoint.mathgame.core.engines.manipulate.FractionBarState
import com.codetothepoint.mathgame.core.engines.manipulate.GridState
import com.codetothepoint.mathgame.core.engines.manipulate.InstrumentState
import com.codetothepoint.mathgame.core.engines.manipulate.ManipulateTarget
import com.codetothepoint.mathgame.core.engines.manipulate.MatchState
import com.codetothepoint.mathgame.core.engines.manipulate.MirrorState
import com.codetothepoint.mathgame.core.engines.manipulate.NumberLineState
import com.codetothepoint.mathgame.core.engines.manipulate.PlaceChartState

/**
 * One satisfying construction or setting for any target. Used by the hint ladder's last rung
 * ("show the finished construction faintly") and by tests to prove every generated item is solvable.
 */
object Solutions {

    fun forBuild(t: BuildTarget): BuildConstruction = when (t) {
        is BuildTarget.Quantity -> BlocksConstruction(decompose(t.value, if (t.scale == 100) listOf(100, 10, 1) else listOf(1000, 100, 10, 1)))
        is BuildTarget.FractionQuantity -> PartsConstruction(listOf(List(t.value.withDenominator(t.denominators[0])!!.num) { Fraction(1, t.denominators[0]) }))
        is BuildTarget.Product -> TilesConstruction(listOf(TileRect(t.a, t.b)))
        is BuildTarget.Quotient -> TilesConstruction(listOf(TileRect(t.quotient, t.divisor)), tray = t.remainder, placements = List(t.divisor) { 1 to t.quotient })
        is BuildTarget.FactorPairs -> TilesConstruction(factorPairs(t.area).map { (a, b) -> TileRect(a, b) })
        is BuildTarget.RectConstraint -> TilesConstruction(listOf(rectFor(t)))
        is BuildTarget.ShapeConstraint -> StrokesConstruction(SHAPES.getValue(t.constraint))
        is BuildTarget.SymmetricCompletion -> symmetricFigure(t)
        is BuildTarget.Angle -> StrokesConstruction(listOf(Stroke.Ray(Pt(0, 0), Pt(100, 0)), Stroke.Ray(Pt(0, 0), rayEnd(t.degrees))))
        is BuildTarget.Plot -> MarksConstruction(t.values)
        is BuildTarget.Sequence -> GearsConstruction(t.terms[0], gearsFor(t.terms))
        is BuildTarget.Table -> UnitsConstruction(t.rows.associateWith { it * t.factor })
        is BuildTarget.TwoWays -> {
            val den = t.denominators[0]
            val n = t.value.withDenominator(den)!!.num
            val otherWay = if (n == 2) listOf(Fraction(2, den)) else listOf(Fraction(n - 1, den), Fraction(1, den))
            PartsConstruction(listOf(List(n) { Fraction(1, den) }, otherWay))
        }
    }

    fun forManipulate(t: ManipulateTarget, initial: InstrumentState): InstrumentState = when (t) {
        is ManipulateTarget.LineValue -> NumberLineState(t.value, t.finestTick)
        is ManipulateTarget.Nearest -> NumberLineState(Fraction.whole(t.rounded.toInt()), Fraction.whole(t.place.toInt()))
        is ManipulateTarget.Equal -> FractionBarState(t.targetDen, listOf(t.answerNum), initialDen = (initial as FractionBarState).initialDen)
        is ManipulateTarget.Partition -> {
            val n = t.value.withDenominator(t.den)!!.num
            FractionBarState(t.den, List(t.parts - 1) { 1 } + (n - (t.parts - 1)), initialDen = t.den)
        }
        is ManipulateTarget.BarValue -> FractionBarState(t.den, listOf(t.value.withDenominator(t.den)!!.num), initialDen = t.den)
        is ManipulateTarget.GridValue -> GridState(t.hundredths)
        is ManipulateTarget.AngleValue -> (initial as AngleState).let { if (it.reading != null) it.copy(reading = t.degrees.toDouble()) else AngleState(t.degrees.toDouble()) }
        is ManipulateTarget.AngleUnknown -> AngleState(t.unknown.toDouble())
        is ManipulateTarget.ChartValue -> PlaceChartState(t.value, leverPulls = 1)
        is ManipulateTarget.Match -> MatchState(NumberLineState(Fraction.of(t.hundredths, 100), Fraction.of(1, 100)), GridState(t.hundredths))
        is ManipulateTarget.Symmetric -> when {
            t.askAll -> MirrorState(null, lockedLines = t.lines, lockedNone = t.lines.isEmpty())
            t.lines.isEmpty() -> MirrorState(null, lockedNone = true)
            else -> MirrorState(t.lines.first())
        }
    }

    private fun decompose(value: Int, denominations: List<Int>): Map<Int, Int> {
        var left = value
        val out = mutableMapOf<Int, Int>()
        for (d in denominations) { if (left / d > 0) out[d] = left / d; left %= d }
        return out
    }

    private fun rectFor(t: BuildTarget.RectConstraint): TileRect = when {
        t.area != null && t.perimeter != null -> {
            val half = t.perimeter / 2
            val w = (1..t.area).first { t.area % it == 0 && it + t.area / it == half }
            TileRect(w, t.area / w)
        }
        t.area != null -> TileRect(t.area, 1)
        t.perimeter != null -> {
            val half = t.perimeter / 2
            val w = t.givenSide ?: 1
            TileRect(w, half - w)
        }
        else -> TileRect(1, 1)
    }

    private fun seg(ax: Int, ay: Int, bx: Int, by: Int) = Stroke.Segment(Pt(ax, ay), Pt(bx, by))
    private fun poly(vararg p: Pair<Int, Int>) = p.indices.map { i -> seg(p[i].first, p[i].second, p[(i + 1) % p.size].first, p[(i + 1) % p.size].second) }

    private val SHAPES: Map<String, List<Stroke>> = mapOf(
        "one-segment" to listOf(seg(0, 0, 3, 1)),
        "one-ray" to listOf(Stroke.Ray(Pt(0, 0), Pt(3, 1))),
        "one-line" to listOf(Stroke.Line(Pt(0, 0), Pt(3, 1))),
        "acute-angle" to listOf(Stroke.Ray(Pt(0, 0), Pt(4, 0)), Stroke.Ray(Pt(0, 0), Pt(3, 2))),
        "right-angle" to listOf(Stroke.Ray(Pt(0, 0), Pt(4, 0)), Stroke.Ray(Pt(0, 0), Pt(0, 3))),
        "obtuse-angle" to listOf(Stroke.Ray(Pt(0, 0), Pt(4, 0)), Stroke.Ray(Pt(0, 0), Pt(-2, 3))),
        "parallel-pair" to listOf(Stroke.Line(Pt(0, 0), Pt(3, 1)), Stroke.Line(Pt(0, 2), Pt(3, 3))),
        "perpendicular-pair" to listOf(Stroke.Line(Pt(0, 0), Pt(3, 1)), Stroke.Line(Pt(0, 0), Pt(-1, 3))),
        "two-rays-sharing-endpoint" to listOf(Stroke.Ray(Pt(1, 1), Pt(4, 1)), Stroke.Ray(Pt(1, 1), Pt(2, 4))),
        "parallel-pair-with-transversal" to listOf(Stroke.Line(Pt(0, 0), Pt(4, 0)), Stroke.Line(Pt(0, 3), Pt(4, 3)), Stroke.Line(Pt(0, 0), Pt(2, 3))),
        "perpendicular-through-point" to listOf(Stroke.Line(Pt(0, 0), Pt(4, 0)), Stroke.Point(Pt(2, 2)), Stroke.Line(Pt(2, 2), Pt(2, 0))),
        "obtuse-angle-between-parallels" to listOf(Stroke.Line(Pt(0, 0), Pt(4, 0)), Stroke.Line(Pt(0, 3), Pt(4, 3)), Stroke.Line(Pt(0, 0), Pt(1, 3))),
        "right-triangle" to poly(0 to 0, 3 to 0, 0 to 4),
        "acute-triangle" to poly(0 to 0, 4 to 0, 2 to 3),
        "obtuse-triangle" to poly(0 to 0, 4 to 0, 5 to 1),
        "isosceles-right-triangle" to poly(0 to 0, 3 to 0, 0 to 3),
        "parallelogram" to poly(0 to 0, 4 to 0, 5 to 2, 1 to 2),
        "rectangle-not-square" to poly(0 to 0, 3 to 0, 3 to 2, 0 to 2),
        "trapezoid-exactly-one-pair" to poly(0 to 0, 4 to 0, 3 to 2, 1 to 2),
        "rhombus-not-square" to poly(0 to 0, 5 to 0, 8 to 4, 3 to 4),
        "quadrilateral-perpendicular-no-parallel" to poly(0 to 0, 4 to 0, 3 to 3, 0 to 2),
    )

    private fun symmetricFigure(t: BuildTarget.SymmetricCompletion): StrokesConstruction {
        val given = t.given
        val mirrored = given.map { t.line.reflect(it) }
        val strokes = mutableListOf<Stroke>()
        for (i in 0 until given.size - 1) {
            strokes += Stroke.Segment(given[i], given[i + 1])
            strokes += Stroke.Segment(mirrored[i], mirrored[i + 1])
        }
        strokes += Stroke.Segment(given.first(), mirrored.first())
        strokes += Stroke.Segment(given.last(), mirrored.last())
        return StrokesConstruction(strokes)
    }

    private fun rayEnd(degrees: Int): Pt {
        val r = Math.toRadians(degrees.toDouble())
        return Pt((1000 * Math.cos(r)).toInt(), (1000 * Math.sin(r)).toInt())
    }

    private fun gearsFor(terms: List<Int>): List<Gear> {
        val d1 = terms[1] - terms[0]
        val d2 = terms[2] - terms[1]
        if (d1 == d2) return listOf(if (d1 >= 0) Gear(Gear.Op.ADD, d1) else Gear(Gear.Op.SUBTRACT, -d1))
        // Affine v → a·v + b with integer a: a = (t2 - t1) / (t1 - t0).
        val a = d2 / d1
        val b = terms[1] - a * terms[0]
        return listOf(Gear(Gear.Op.MULTIPLY, a)) + (if (b > 0) listOf(Gear(Gear.Op.ADD, b)) else if (b < 0) listOf(Gear(Gear.Op.SUBTRACT, -b)) else emptyList())
    }

    /** Lines of symmetry helper exposed for tests. */
    fun symmetryLines(points: Set<Pt>): Set<MirrorLine> = MirrorLine.symmetryLinesOf(points)
}
