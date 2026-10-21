package com.codetothepoint.mathgame.core.engines.build

import com.codetothepoint.mathgame.core.engines.Fraction
import com.codetothepoint.mathgame.core.engines.MirrorLine
import com.codetothepoint.mathgame.core.engines.Pt
import com.codetothepoint.mathgame.core.engines.Stroke
import com.codetothepoint.mathgame.core.engines.placeValueCuts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Engine-logic cases from engine-build.md §12, numbered as there, plus the constraint vocabulary. */
class BuildEngineTest {
    private val productRules = setOf("split-place-value", "split-one-dimension", "whole-array")

    private fun seg(ax: Int, ay: Int, bx: Int, by: Int) = Stroke.Segment(Pt(ax, ay), Pt(bx, by))
    private fun shape(vararg pts: Pair<Int, Int>): StrokesConstruction =
        StrokesConstruction(pts.indices.map { i -> val a = pts[i]; val b = pts[(i + 1) % pts.size]; seg(a.first, a.second, b.first, b.second) })

    @Test
    fun case1_productAcceptsOneRectangleOfTheRightDimensionsWithAnyCuts() {
        val t = BuildTarget.Product(23, 14)
        assertTrue(BuildEngine.satisfied(t, TilesConstruction(listOf(TileRect(23, 14)))))
        assertTrue(BuildEngine.satisfied(t, TilesConstruction(listOf(TileRect(14, 23)))))
        assertTrue(BuildEngine.satisfied(t, TilesConstruction(listOf(TileRect(23, 14, cutsAlongW = setOf(7), cutsAlongH = setOf(5))))))
        assertFalse(BuildEngine.satisfied(t, TilesConstruction(listOf(TileRect(23, 7), TileRect(23, 7)))))
        assertFalse(BuildEngine.satisfied(t, TilesConstruction(listOf(TileRect(23, 13)))))
        // Cuts off the place-value lines: satisfied, but no place-value strategy.
        val offLines = TilesConstruction(listOf(TileRect(23, 14, cutsAlongW = setOf(7))))
        assertNull(BuildEngine.detectStrategy(t, offLines, productRules))
        val oneDim = TilesConstruction(listOf(TileRect(23, 14, cutsAlongH = setOf(10))))
        assertEquals("split-one-dimension", BuildEngine.detectStrategy(t, oneDim, productRules))
    }

    @Test
    fun case2_factorPairsNeedsExactlyTheSet() {
        val t = BuildTarget.FactorPairs(24)
        val all = listOf(TileRect(1, 24), TileRect(2, 12), TileRect(3, 8), TileRect(6, 4))
        assertTrue(BuildEngine.satisfied(t, TilesConstruction(all)))
        assertFalse(BuildEngine.satisfied(t, TilesConstruction(all.dropLast(1))))
        assertFalse(BuildEngine.satisfied(t, TilesConstruction(all + TileRect(5, 5))))
        assertEquals("all-pairs-ascending", BuildEngine.detectStrategy(t, TilesConstruction(all), setOf("all-pairs-ascending", "whole-array")))
        assertEquals("whole-array", BuildEngine.detectStrategy(t, TilesConstruction(all.reversed()), setOf("all-pairs-ascending", "whole-array")))
        // A prime: one rectangle only.
        assertTrue(BuildEngine.satisfied(BuildTarget.FactorPairs(29), TilesConstruction(listOf(TileRect(29, 1)))))
        assertFalse(BuildEngine.satisfied(BuildTarget.FactorPairs(29), TilesConstruction(emptyList())))
    }

    @Test
    fun case3_trapezoidConstraintRejectsParallelogramInAnyOrientation() {
        val t = BuildTarget.ShapeConstraint("trapezoid-exactly-one-pair")
        assertTrue(BuildEngine.satisfied(t, shape(0 to 0, 4 to 0, 3 to 2, 1 to 2)))
        assertTrue(BuildEngine.satisfied(t, shape(0 to 0, 2 to 1, 2 to 3, 0 to 4)))   // vertical pair, tilted
        assertFalse(BuildEngine.satisfied(t, shape(0 to 0, 4 to 0, 5 to 2, 1 to 2)))  // parallelogram
        assertFalse(BuildEngine.satisfied(t, shape(0 to 0, 3 to 0, 3 to 2, 0 to 2)))  // rectangle
        assertTrue(BuildEngine.satisfied(BuildTarget.ShapeConstraint("parallelogram"), shape(0 to 0, 4 to 0, 5 to 2, 1 to 2)))
        assertTrue(BuildEngine.satisfied(BuildTarget.ShapeConstraint("rectangle-not-square"), shape(0 to 0, 3 to 0, 3 to 2, 0 to 2)))
        assertFalse(BuildEngine.satisfied(BuildTarget.ShapeConstraint("rectangle-not-square"), shape(0 to 0, 2 to 0, 2 to 2, 0 to 2)))
        assertTrue(BuildEngine.satisfied(BuildTarget.ShapeConstraint("rhombus-not-square"), shape(0 to 0, 5 to 0, 8 to 4, 3 to 4)))
        assertTrue(BuildEngine.satisfied(BuildTarget.ShapeConstraint("right-triangle"), shape(0 to 0, 3 to 0, 0 to 4)))
        assertTrue(BuildEngine.satisfied(BuildTarget.ShapeConstraint("isosceles-right-triangle"), shape(0 to 0, 3 to 0, 0 to 3)))
        assertTrue(BuildEngine.satisfied(BuildTarget.ShapeConstraint("obtuse-triangle"), shape(0 to 0, 4 to 0, 5 to 1)))
        assertTrue(BuildEngine.satisfied(BuildTarget.ShapeConstraint("acute-triangle"), shape(0 to 0, 4 to 0, 2 to 3)))
        assertTrue(BuildEngine.satisfied(BuildTarget.ShapeConstraint("quadrilateral-perpendicular-no-parallel"), shape(0 to 0, 4 to 0, 3 to 3, 0 to 2)))
    }

    @Test
    fun case4_quantityFromBlocksInAnyDenomination() {
        val t = BuildTarget.Quantity(3400)
        assertTrue(BuildEngine.satisfied(t, BlocksConstruction(mapOf(100 to 34))))
        assertTrue(BuildEngine.satisfied(t, BlocksConstruction(mapOf(1000 to 3, 100 to 4))))
        assertFalse(BuildEngine.satisfied(t, BlocksConstruction(mapOf(1000 to 3, 10 to 4))))
        assertEquals("bundle", BuildEngine.detectStrategy(t, BlocksConstruction(mapOf(1000 to 3, 100 to 4), bundles = 2), setOf("bundle")))
        assertNull(BuildEngine.detectStrategy(t, BlocksConstruction(mapOf(100 to 34)), setOf("bundle")))
    }

    @Test
    fun case5_strategyPriority() {
        val t = BuildTarget.Product(23, 14)
        val both = TilesConstruction(listOf(TileRect(23, 14, cutsAlongW = setOf(20), cutsAlongH = setOf(10))))
        val one = TilesConstruction(listOf(TileRect(23, 14, cutsAlongW = setOf(20))))
        val none = TilesConstruction(listOf(TileRect(23, 14)))
        assertEquals("split-place-value", BuildEngine.detectStrategy(t, both, productRules))
        assertEquals("split-one-dimension", BuildEngine.detectStrategy(t, one, productRules))
        assertEquals("whole-array", BuildEngine.detectStrategy(t, none, productRules))
        // With a one-digit factor, cutting the only place-value dimension is the full area model.
        assertEquals("split-place-value", BuildEngine.detectStrategy(BuildTarget.Product(23, 4), TilesConstruction(listOf(TileRect(23, 4, cutsAlongW = setOf(20)))), productRules))
        // Three-digit sides have two place-value cuts.
        assertEquals(setOf(300, 340), placeValueCuts(345))
        assertEquals(setOf(20), placeValueCuts(23))
        assertEquals(emptySet(), placeValueCuts(7))
        assertEquals(setOf(100), placeValueCuts(107))
    }

    @Test
    fun quotientsAndTheirStrategies() {
        val t = BuildTarget.Quotient(86, 4)   // 21 r 2
        assertTrue(BuildEngine.satisfied(t, TilesConstruction(listOf(TileRect(21, 4)), tray = 2)))
        assertFalse(BuildEngine.satisfied(t, TilesConstruction(listOf(TileRect(21, 4)), tray = 1)))
        assertFalse(BuildEngine.satisfied(t, TilesConstruction(listOf(TileRect(4, 21)), tray = 2)))   // rows must be the divisor
        val chunked = TilesConstruction(listOf(TileRect(21, 4)), tray = 2, placements = listOf(4 to 10, 4 to 10, 4 to 1))
        val rows = TilesConstruction(listOf(TileRect(21, 4)), tray = 2, placements = listOf(1 to 21, 1 to 21, 1 to 21, 1 to 21))
        val rules = setOf("chunked-quotient", "rows-of")
        assertEquals("chunked-quotient", BuildEngine.detectStrategy(t, chunked, rules))
        assertEquals("rows-of", BuildEngine.detectStrategy(t, rows, rules))
    }

    @Test
    fun partsTargets() {
        val f = Fraction.of(5, 8)
        val two = BuildTarget.TwoWays(f, listOf(8))
        val unit = List(5) { Fraction.of(1, 8) }
        val grouped = listOf(Fraction(2, 8), Fraction(3, 8))   // pieces keep their denomination: a two-eighths piece is not a quarter
        assertTrue(BuildEngine.satisfied(two, PartsConstruction(listOf(unit, grouped))))
        assertFalse(BuildEngine.satisfied(two, PartsConstruction(listOf(unit, unit))))
        assertFalse(BuildEngine.satisfied(two, PartsConstruction(listOf(grouped, listOf(Fraction(3, 8), Fraction(2, 8))))))
        assertEquals("unit-pieces", BuildEngine.detectStrategy(two, PartsConstruction(listOf(unit, grouped)), setOf("unit-pieces")))
        val q = BuildTarget.FractionQuantity(Fraction.of(8, 3), listOf(3))
        val copies = PartsConstruction(listOf(List(8) { Fraction.of(1, 3) }), duplications = 3)
        assertTrue(BuildEngine.satisfied(q, copies))
        assertEquals("grouped-copies", BuildEngine.detectStrategy(q, copies, setOf("grouped-copies")))
    }

    @Test
    fun strokesAnglesSymmetryAndEdgeFirst() {
        val angle = BuildTarget.Angle(90)
        val rays = StrokesConstruction(listOf(Stroke.Ray(Pt(0, 0), Pt(3, 0)), Stroke.Ray(Pt(0, 0), Pt(0, 3))))
        assertTrue(BuildEngine.satisfied(angle, rays))
        assertFalse(BuildEngine.satisfied(BuildTarget.Angle(45), rays))
        assertTrue(BuildEngine.satisfied(BuildTarget.Angle(45), StrokesConstruction(listOf(Stroke.Ray(Pt(0, 0), Pt(3, 0)), Stroke.Ray(Pt(0, 0), Pt(3, 3))))))

        val line = MirrorLine.vertical(4)  // x = 2
        val completion = BuildTarget.SymmetricCompletion(line, listOf(Pt(0, 0), Pt(1, 2)))
        val kite = StrokesConstruction(listOf(seg(0, 0, 1, 2), seg(4, 0, 3, 2), seg(1, 2, 2, 4), seg(3, 2, 2, 4), seg(0, 0, 2, -1), seg(4, 0, 2, -1)))
        assertTrue(BuildEngine.satisfied(completion, kite))
        assertEquals("mirror-pairs", BuildEngine.detectStrategy(completion, kite, setOf("mirror-pairs")))
        assertFalse(BuildEngine.satisfied(completion, StrokesConstruction(listOf(seg(0, 0, 1, 2), seg(4, 0, 3, 1)))))

        val square = shape(0 to 0, 2 to 0, 2 to 2, 0 to 2)
        assertEquals("edge-first", BuildEngine.detectStrategy(BuildTarget.ShapeConstraint("parallelogram"), square, setOf("edge-first")))
        assertEquals(setOf(MirrorLine.vertical(2), MirrorLine.horizontal(2), MirrorLine.diagonalUp(0), MirrorLine.diagonalDown(2)), MirrorLine.symmetryLinesOf(square.points))
    }

    @Test
    fun plotsGearsAndTables() {
        val plot = BuildTarget.Plot(listOf(Fraction.of(1, 2), Fraction.of(3, 4), Fraction.of(1, 2)), Fraction.of(1, 4))
        assertTrue(BuildEngine.satisfied(plot, MarksConstruction(listOf(Fraction.of(3, 4), Fraction.of(1, 2), Fraction.of(1, 2)))))
        assertFalse(BuildEngine.satisfied(plot, MarksConstruction(listOf(Fraction.of(3, 4), Fraction.of(1, 2)))))

        val seq = BuildTarget.Sequence(listOf(3, 7, 11, 15), minimalGears = 1)
        assertTrue(BuildEngine.satisfied(seq, GearsConstruction(3, listOf(Gear(Gear.Op.ADD, 4)))))
        assertTrue(BuildEngine.satisfied(seq, GearsConstruction(3, listOf(Gear(Gear.Op.ADD, 2), Gear(Gear.Op.ADD, 2)))))
        assertFalse(BuildEngine.satisfied(seq, GearsConstruction(3, listOf(Gear(Gear.Op.MULTIPLY, 2)))))

        val table = BuildTarget.Table("yard", "foot", 3, listOf(1, 2, 3))
        assertTrue(BuildEngine.satisfied(table, UnitsConstruction(mapOf(1 to 3, 2 to 6, 3 to 9))))
        assertFalse(BuildEngine.satisfied(table, UnitsConstruction(mapOf(1 to 3, 2 to 6))))
        assertFalse(BuildEngine.satisfied(table, UnitsConstruction(mapOf(1 to 3, 2 to 6, 3 to 10))))
    }

    @Test
    fun rectConstraints() {
        val t = BuildTarget.RectConstraint(area = 24, perimeter = 20)
        assertTrue(BuildEngine.satisfied(t, TilesConstruction(listOf(TileRect(6, 4)))))
        assertFalse(BuildEngine.satisfied(t, TilesConstruction(listOf(TileRect(8, 3)))))
        assertFalse(BuildEngine.satisfied(t, TilesConstruction(emptyList())))
        assertTrue(BuildEngine.satisfied(BuildTarget.RectConstraint(perimeter = 20, givenSide = 7), TilesConstruction(listOf(TileRect(7, 3)))))
    }
}
