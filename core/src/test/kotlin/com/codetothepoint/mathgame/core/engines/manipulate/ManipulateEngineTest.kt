package com.codetothepoint.mathgame.core.engines.manipulate

import com.codetothepoint.mathgame.core.engines.Fraction
import com.codetothepoint.mathgame.core.engines.MirrorLine
import com.codetothepoint.mathgame.core.engines.Pt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Engine-logic cases from engine-manipulate.md §12, numbered as there. */
class ManipulateEngineTest {
    private val tenth = Fraction.of(1, 10)
    private val hundredth = Fraction.of(1, 100)

    @Test
    fun case1_nearestAcceptsOnlyTheRoundedValue() {
        val t = ManipulateTarget.Nearest(3462, 100)
        assertEquals(3500L, t.rounded)
        assertEquals(3450L, t.midpoint)
        val one = Fraction.whole(1)
        assertTrue(ManipulateEngine.satisfied(t, NumberLineState(Fraction.whole(3500), one)))
        assertFalse(ManipulateEngine.satisfied(t, NumberLineState(Fraction.whole(3450), one)))
        assertFalse(ManipulateEngine.satisfied(t, NumberLineState(Fraction.whole(3400), one)))
        val paused = NumberLineState(Fraction.whole(3500), one, pauses = listOf(Fraction.whole(3462), Fraction.whole(3450)))
        assertEquals("midpoint-first", ManipulateEngine.detectStrategy(t, paused, setOf("midpoint-first")))
        assertNull(ManipulateEngine.detectStrategy(t, NumberLineState(Fraction.whole(3500), one), setOf("midpoint-first")))
        assertEquals(3400L, ManipulateTarget.Nearest(3449, 100).rounded)
        assertEquals(3500L, ManipulateTarget.Nearest(3450, 100).rounded)
    }

    @Test
    fun case2_equalRequiresTheTargetDenominatorAndAmount() {
        val t = ManipulateTarget.Equal(Fraction.of(3, 4), 8)
        assertEquals(6, t.answerNum)
        assertTrue(ManipulateEngine.satisfied(t, FractionBarState(8, listOf(6), initialDen = 4)))
        assertFalse(ManipulateEngine.satisfied(t, FractionBarState(4, listOf(3), initialDen = 4)))
        assertFalse(ManipulateEngine.satisfied(t, FractionBarState(8, listOf(5), initialDen = 4)))
        val recut = FractionBarState(8, listOf(6), initialDen = 4, edgeMoved = false)
        val moved = FractionBarState(8, listOf(6), initialDen = 4, edgeMoved = true)
        assertEquals("recut-only", ManipulateEngine.detectStrategy(t, recut, setOf("recut-only")))
        assertNull(ManipulateEngine.detectStrategy(t, moved, setOf("recut-only")))
    }

    @Test
    fun case3_angleToleranceByBand() {
        val loose = ManipulateTarget.AngleValue(65, toleranceDegrees = 2)
        val tight = ManipulateTarget.AngleValue(65, toleranceDegrees = 1)
        assertTrue(ManipulateEngine.satisfied(loose, AngleState(63.0)))
        assertTrue(ManipulateEngine.satisfied(loose, AngleState(67.0)))
        assertFalse(ManipulateEngine.satisfied(loose, AngleState(62.0)))
        assertTrue(ManipulateEngine.satisfied(tight, AngleState(66.0)))
        assertFalse(ManipulateEngine.satisfied(tight, AngleState(67.0)))
        // Reading mode: the protractor reading is what counts.
        assertTrue(ManipulateEngine.satisfied(loose, AngleState(degrees = 65.0, reading = 64.0)))
        assertFalse(ManipulateEngine.satisfied(loose, AngleState(degrees = 65.0, reading = 70.0)))
        assertEquals("quarter-turns", ManipulateEngine.detectStrategy(loose, AngleState(65.0, quarterTurnDetentsUsed = 1), setOf("quarter-turns")))
        val unknown = ManipulateTarget.AngleUnknown(90, listOf(35), 2)
        assertEquals(55, unknown.unknown)
        assertTrue(ManipulateEngine.satisfied(unknown, AngleState(54.0)))
        assertEquals("complement-first", ManipulateEngine.detectStrategy(unknown, AngleState(55.0, knownSetFirst = true), setOf("complement-first")))
    }

    @Test
    fun case4_matchNeedsHundredthsGranularity() {
        val t = ManipulateTarget.Match(37, givenOnGrid = true)
        val atHundredths = MatchState(NumberLineState(Fraction.of(37, 100), hundredth), GridState(37))
        val atTenths = MatchState(NumberLineState(Fraction.of(4, 10), tenth), GridState(37))
        assertTrue(ManipulateEngine.satisfied(t, atHundredths))
        assertFalse(ManipulateEngine.satisfied(t, atTenths))
        assertFalse(ManipulateEngine.satisfied(t, MatchState(NumberLineState(Fraction.of(37, 100), hundredth), GridState(36))))
        val zoomed = MatchState(NumberLineState(Fraction.of(37, 100), hundredth, zoomedBeforeFirstPlacement = true), GridState(37))
        assertEquals("zoom-then-place", ManipulateEngine.detectStrategy(t, zoomed.line, setOf("zoom-then-place")))
        // LineValue is exact at the current tick: 0.37 cannot be placed at tenths.
        val lv = ManipulateTarget.LineValue(Fraction.of(37, 100), hundredth)
        assertTrue(ManipulateEngine.satisfied(lv, NumberLineState(Fraction.of(37, 100), hundredth)))
        assertFalse(ManipulateEngine.satisfied(lv, NumberLineState(Fraction.of(37, 100), tenth)))   // off-tick marker
        assertTrue(ManipulateEngine.onTick(Fraction.of(3, 10), hundredth))
        assertFalse(ManipulateEngine.onTick(Fraction.of(37, 100), tenth))
    }

    @Test
    fun case5_symmetricOnASquareNeedsFourDistinctLines() {
        val square = setOf(Pt(0, 0), Pt(2, 0), Pt(2, 2), Pt(0, 2))
        val all = ManipulateTarget.Symmetric(square, askAll = true)
        assertEquals(4, all.lines.size)
        val four = setOf(MirrorLine.vertical(2), MirrorLine.horizontal(2), MirrorLine.diagonalUp(0), MirrorLine.diagonalDown(2))
        assertTrue(ManipulateEngine.satisfied(all, MirrorState(null, lockedLines = four)))
        assertFalse(ManipulateEngine.satisfied(all, MirrorState(null, lockedLines = four - MirrorLine.diagonalUp(0))))
        // A duplicate line is the same set element; three distinct lines are not enough.
        assertFalse(ManipulateEngine.satisfied(all, MirrorState(null, lockedLines = setOf(MirrorLine.vertical(2), MirrorLine.vertical(2), MirrorLine.horizontal(2), MirrorLine.diagonalUp(0)))))
        val one = ManipulateTarget.Symmetric(square, askAll = false)
        assertTrue(ManipulateEngine.satisfied(one, MirrorState(MirrorLine.vertical(2))))
        assertFalse(ManipulateEngine.satisfied(one, MirrorState(MirrorLine.vertical(1))))
        assertEquals("fold-test", ManipulateEngine.detectStrategy(one, MirrorState(MirrorLine.vertical(2), flips = 2), setOf("fold-test")))
        // A figure with no line: lock in "none".
        val scalene = setOf(Pt(0, 0), Pt(4, 0), Pt(1, 3))
        val none = ManipulateTarget.Symmetric(scalene, askAll = false)
        assertTrue(none.lines.isEmpty())
        assertTrue(ManipulateEngine.satisfied(none, MirrorState(null, lockedNone = true)))
        assertFalse(ManipulateEngine.satisfied(none, MirrorState(MirrorLine.vertical(4))))
    }

    @Test
    fun case6_otherTargets() {
        val part = ManipulateTarget.Partition(Fraction.of(5, 8), 8, parts = 2)
        assertTrue(ManipulateEngine.satisfied(part, FractionBarState(8, listOf(2, 3), initialDen = 8)))
        assertFalse(ManipulateEngine.satisfied(part, FractionBarState(8, listOf(5), initialDen = 8)))
        assertFalse(ManipulateEngine.satisfied(part, FractionBarState(8, listOf(2, 2), initialDen = 8)))
        val bar = ManipulateTarget.BarValue(Fraction.of(5, 4), 4)
        val regrouped = FractionBarState(4, listOf(4, 1), initialDen = 4, mergedWholes = 1)
        assertTrue(ManipulateEngine.satisfied(bar, regrouped))
        assertEquals("regroup-whole", ManipulateEngine.detectStrategy(bar, regrouped, setOf("regroup-whole")))
        assertTrue(ManipulateEngine.satisfied(ManipulateTarget.GridValue(37), GridState(37)))
        val chart = ManipulateTarget.ChartValue(3400)
        assertTrue(ManipulateEngine.satisfied(chart, PlaceChartState(3400, leverPulls = 1)))
        assertEquals("shift-digits", ManipulateEngine.detectStrategy(chart, PlaceChartState(3400, digitShifts = 3), setOf("shift-digits")))
        assertNull(ManipulateEngine.detectStrategy(chart, PlaceChartState(3400, digitShifts = 3, typed = true), setOf("shift-digits")))
    }
}
