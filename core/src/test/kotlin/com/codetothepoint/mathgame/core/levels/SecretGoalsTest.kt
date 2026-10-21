package com.codetothepoint.mathgame.core.levels

import com.codetothepoint.mathgame.core.curriculum.Representation.VISUAL
import com.codetothepoint.mathgame.core.engines.Fraction
import com.codetothepoint.mathgame.core.engines.Pt
import com.codetothepoint.mathgame.core.engines.Stroke
import com.codetothepoint.mathgame.core.engines.build.BuildTarget
import com.codetothepoint.mathgame.core.engines.build.GearsConstruction
import com.codetothepoint.mathgame.core.engines.build.Gear
import com.codetothepoint.mathgame.core.engines.build.StrokesConstruction
import com.codetothepoint.mathgame.core.engines.build.TileRect
import com.codetothepoint.mathgame.core.engines.build.TilesConstruction
import com.codetothepoint.mathgame.core.engines.manipulate.AngleState
import com.codetothepoint.mathgame.core.engines.manipulate.FractionBarState
import com.codetothepoint.mathgame.core.engines.manipulate.ManipulateTarget
import com.codetothepoint.mathgame.core.engines.manipulate.MatchState
import com.codetothepoint.mathgame.core.engines.manipulate.GridState
import com.codetothepoint.mathgame.core.engines.manipulate.NumberLineState
import com.codetothepoint.mathgame.core.engines.manipulate.PlaceChartState
import com.codetothepoint.mathgame.core.recipes.Band
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecretGoalsTest {
    private fun build(t: BuildTarget, band: Band = Band.MID) = GeneratedItem.Build(band, VISUAL, t)
    private fun manip(t: ManipulateTarget, band: Band = Band.MID) = GeneratedItem.Manipulate(band, VISUAL, t, AngleState(0.0))

    @Test
    fun fourPartialsNeedsBothCutsOnFirstTry() {
        val t = BuildTarget.Product(23, 14)
        val both = TilesConstruction(listOf(TileRect(23, 14, setOf(20), setOf(10))))
        assertTrue(SecretGoals.holds("four-partials", listOf(ItemRecord(build(t), true, true, 0, both))))
        assertFalse(SecretGoals.holds("four-partials", listOf(ItemRecord(build(t), true, false, 0, both))))
        assertFalse(SecretGoals.holds("four-partials", listOf(ItemRecord(build(BuildTarget.Product(23, 4)), true, true, 0, TilesConstruction(listOf(TileRect(23, 4, setOf(20))))))))
    }

    @Test
    fun primeCallAndFewestGears() {
        val prime = ItemRecord(build(BuildTarget.FactorPairs(29)), true, true, 0, TilesConstruction(listOf(TileRect(1, 29))))
        val hinted = prime.copy(hints = 1)
        assertTrue(SecretGoals.holds("prime-call", listOf(prime)))
        assertFalse(SecretGoals.holds("prime-call", listOf(hinted)))
        val seq = build(BuildTarget.Sequence(listOf(3, 7, 11, 15), minimalGears = 1))
        assertTrue(SecretGoals.holds("fewest-gears", listOf(ItemRecord(seq, true, true, 0, GearsConstruction(3, listOf(Gear(Gear.Op.ADD, 4)))))))
        assertFalse(SecretGoals.holds("fewest-gears", listOf(ItemRecord(seq, true, true, 0, GearsConstruction(3, listOf(Gear(Gear.Op.ADD, 2), Gear(Gear.Op.ADD, 2)))))))
    }

    @Test
    fun remainderTrayIsLevelWide() {
        val withRem = ItemRecord(build(BuildTarget.Quotient(86, 4)), true, true, 0, TilesConstruction(listOf(TileRect(21, 4)), tray = 2))
        val exact = ItemRecord(build(BuildTarget.Quotient(84, 4)), true, false, 0, TilesConstruction(listOf(TileRect(21, 4))))
        assertTrue(SecretGoals.holds("remainder-tray", listOf(withRem, exact)))   // the exact one needed a retry, but it has no remainder
        assertFalse(SecretGoals.holds("remainder-tray", listOf(withRem.copy(firstTry = false))))
        assertFalse(SecretGoals.holds("remainder-tray", listOf(exact)))
    }

    @Test
    fun differentEachTimeNeedsThreeNonCongruentShapes() {
        fun tri(vararg p: Pair<Int, Int>) = StrokesConstruction(p.indices.map { i -> Stroke.Segment(Pt(p[i].first, p[i].second), Pt(p[(i + 1) % p.size].first, p[(i + 1) % p.size].second)) })
        val t = build(BuildTarget.ShapeConstraint("right-triangle"))
        val a = ItemRecord(t, true, true, 0, tri(0 to 0, 3 to 0, 0 to 4))
        val b = ItemRecord(t, true, true, 0, tri(0 to 0, 6 to 0, 0 to 8))
        val c = ItemRecord(t, true, true, 0, tri(0 to 0, 5 to 0, 0 to 12))
        val aAgain = ItemRecord(t, true, true, 0, tri(1 to 1, 1 to 5, 4 to 1))   // congruent to a, moved and flipped
        assertTrue(SecretGoals.holds("different-each-time", listOf(a, b, c)))
        assertFalse(SecretGoals.holds("different-each-time", listOf(a, b, aAgain)))
        assertFalse(SecretGoals.holds("different-each-time", listOf(a, b)))
    }

    @Test
    fun manipulateGoals() {
        val chart = ItemRecord(manip(ManipulateTarget.ChartValue(3400)), true, true, 0, state = PlaceChartState(3400, leverPulls = 1))
        assertTrue(SecretGoals.holds("one-shift", listOf(chart)))
        assertFalse(SecretGoals.holds("one-shift", listOf(chart.copy(state = PlaceChartState(3400, leverPulls = 1, digitShifts = 1)))))

        val near = ManipulateTarget.Nearest(3462, 100)
        val paused = NumberLineState(Fraction.whole(3500), Fraction.whole(1), pauses = listOf(Fraction.whole(3450)))
        assertTrue(SecretGoals.holds("midpoint-call", listOf(ItemRecord(manip(near, Band.HIGH), true, true, 0, state = paused))))
        assertFalse(SecretGoals.holds("midpoint-call", listOf(ItemRecord(manip(near, Band.MID), true, true, 0, state = paused))))

        val eq = ManipulateTarget.Equal(Fraction.of(3, 4), 8)
        val recut = ItemRecord(manip(eq), true, true, 0, state = FractionBarState(8, listOf(6), initialDen = 4))
        assertTrue(SecretGoals.holds("same-edge", listOf(recut, recut, recut)))
        assertFalse(SecretGoals.holds("same-edge", listOf(recut, recut, recut.copy(state = FractionBarState(8, listOf(6), initialDen = 4, edgeMoved = true)))))

        val match = ManipulateTarget.Match(37, givenOnGrid = true)
        val tenthsFirst = MatchState(NumberLineState(Fraction.of(37, 100), Fraction.of(1, 100), firstPlacement = Fraction.of(3, 10), tickAtFirstPlacement = Fraction.of(1, 10)), GridState(37))
        val zoomedFirst = MatchState(NumberLineState(Fraction.of(37, 100), Fraction.of(1, 100), firstPlacement = Fraction.of(37, 100), tickAtFirstPlacement = Fraction.of(1, 100)), GridState(37))
        assertTrue(SecretGoals.holds("tenths-first", listOf(ItemRecord(manip(match), true, true, 0, state = tenthsFirst))))
        assertFalse(SecretGoals.holds("tenths-first", listOf(ItemRecord(manip(match), true, true, 0, state = tenthsFirst), ItemRecord(manip(match), true, true, 0, state = zoomedFirst))))

        val angle = ManipulateTarget.AngleValue(90, 2)
        assertTrue(SecretGoals.holds("zero-first", listOf(ItemRecord(manip(angle), true, true, 0, state = AngleState(90.0, zeroAligned = true)))))
        assertFalse(SecretGoals.holds("zero-first", listOf(ItemRecord(manip(angle), true, true, 0, state = AngleState(90.0)))))
        assertTrue(SecretGoals.holds("full-circle", listOf(ItemRecord(manip(ManipulateTarget.AngleValue(360, 2), Band.LOW), true, true, 0, state = AngleState(360.0, maxRotation = 360.0)))))

        val square = ManipulateTarget.Symmetric(setOf(Pt(0, 0), Pt(2, 0), Pt(2, 2), Pt(0, 2)), askAll = true)
        assertTrue(SecretGoals.holds("all-the-lines", listOf(ItemRecord(manip(square), true, true, 0, state = AngleState(0.0)))))
        assertEquals(listOf("one-shift"), SecretGoals.achieved(listOf("one-shift", "zero-first"), listOf(chart)))
    }
}
