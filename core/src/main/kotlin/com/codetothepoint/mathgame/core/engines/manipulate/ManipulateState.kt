package com.codetothepoint.mathgame.core.engines.manipulate

import com.codetothepoint.mathgame.core.engines.Fraction
import com.codetothepoint.mathgame.core.engines.MirrorLine
import com.codetothepoint.mathgame.core.engines.Pt

/**
 * An instrument's setting at lock-in plus the parts of the adjustment path the engine spec says it
 * can observe (engine-manipulate.md §7). Produced by the UI, judged by [ManipulateEngine].
 */
sealed interface InstrumentState

/** Marker on a number line. [marker] is null until placed. [tick] is the current granularity. */
data class NumberLineState(
    val marker: Fraction?,
    val tick: Fraction,
    /** Values the marker paused at, in order. */
    val pauses: List<Fraction> = emptyList(),
    /** Whether the player zoomed before the first placement. */
    val zoomedBeforeFirstPlacement: Boolean = false,
    /** Tick granularity at the first placement. */
    val tickAtFirstPlacement: Fraction? = null,
    /** The first placement itself. */
    val firstPlacement: Fraction? = null,
) : InstrumentState

/** A fraction bar: cut into [den] pieces, with [shadedPieces] as the shaded run (or runs, for partitions). */
data class FractionBarState(
    val den: Int,
    val shadedPieces: List<Int>,
    val initialDen: Int,
    val edgeMoved: Boolean = false,
    val usedHalfMarker: Boolean = false,
    val mergedWholes: Int = 0,
) : InstrumentState {
    val shadedNum: Int get() = shadedPieces.sum()
    val amount: Fraction get() = Fraction.of(shadedNum, den)
}

/** A hundredths grid with [shaded] cells. */
data class GridState(val shaded: Int) : InstrumentState

/** The mirror on the symmetry grid, and which lines were locked in during the item. */
data class MirrorState(val mirror: MirrorLine?, val lockedLines: Set<MirrorLine> = emptySet(), val flips: Int = 0, val lockedNone: Boolean = false) : InstrumentState

/** Angle arms (or a protractor reading). */
data class AngleState(
    val degrees: Double,
    val zeroAligned: Boolean = false,
    val quarterTurnDetentsUsed: Int = 0,
    val maxRotation: Double = 0.0,
    /** For unknown targets: the known angle was set before the unknown was adjusted. */
    val knownSetFirst: Boolean = false,
    val reading: Double? = null,
) : InstrumentState

/** Place-value chart. */
data class PlaceChartState(val value: Long, val leverPulls: Int = 0, val digitShifts: Int = 0, val typed: Boolean = false) : InstrumentState

/** The two instruments of a match item. */
data class MatchState(val line: NumberLineState, val grid: GridState) : InstrumentState
