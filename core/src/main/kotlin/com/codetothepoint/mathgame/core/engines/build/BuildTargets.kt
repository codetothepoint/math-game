package com.codetothepoint.mathgame.core.engines.build

import com.codetothepoint.mathgame.core.engines.Fraction
import com.codetothepoint.mathgame.core.engines.MirrorLine
import com.codetothepoint.mathgame.core.engines.Pt

/** What a `build` item asks for (engine-build.md §4). Each carries enough to render itself in any representation. */
sealed interface BuildTarget {
    val kind: String

    /**
     * Make a number from `blocks`; [value] in base units ([scale] 1 for whole numbers, 100 for hundredths).
     * [base] is the number the prompt starts from ("ten times 340"); [digitMoves] asks for the prompt that
     * follows one digit between places (the 4 in 340 and in 3,400).
     */
    data class Quantity(val value: Int, val scale: Int = 1, val base: Int? = null, val digitMoves: Boolean = false) : BuildTarget { override val kind get() = "quantity" }

    /** Make an amount from `parts`; [groups] × [each] is how the prompt reads ("four friends each take two thirds"). */
    data class FractionQuantity(val value: Fraction, val denominators: List<Int>, val groups: Int? = null, val each: Fraction? = null, val readAsMixed: Boolean = false) : BuildTarget { override val kind get() = "quantity" }

    /** One rectangle of [a] × [b] in `tiles`. */
    data class Product(val a: Int, val b: Int) : BuildTarget {
        override val kind get() = "product"
        val value: Int get() = a * b
    }

    /** [dividend] tiles in exactly [divisor] equal rows, leftovers in the tray. */
    data class Quotient(val dividend: Int, val divisor: Int) : BuildTarget {
        override val kind get() = "quotient"
        val quotient: Int get() = dividend / divisor
        val remainder: Int get() = dividend % divisor
    }

    /** Every rectangle with [area]. */
    data class FactorPairs(val area: Int) : BuildTarget { override val kind get() = "factor-pairs" }

    /** A rectangle meeting the listed constraints (`tiles`). */
    data class RectConstraint(val area: Int? = null, val perimeter: Int? = null, val givenSide: Int? = null) : BuildTarget { override val kind get() = "constraint" }

    /** A figure on the dot grid meeting a named constraint (`strokes`). */
    data class ShapeConstraint(val constraint: String) : BuildTarget { override val kind get() = "constraint" }

    /** The other half of a figure across [line] (`strokes`): the finished figure must be symmetric and contain [given]. */
    data class SymmetricCompletion(val line: MirrorLine, val given: List<Pt>) : BuildTarget { override val kind get() = "constraint" }

    /** Two rays from a vertex making [degrees]. */
    data class Angle(val degrees: Int, val toleranceDegrees: Int = 2) : BuildTarget { override val kind get() = "angle" }

    /** Every value marked once on a line plot with ticks of [tick]. */
    data class Plot(val values: List<Fraction>, val tick: Fraction) : BuildTarget { override val kind get() = "plot" }

    /** A gear chain producing [terms]; [minimalGears] is the fewest gears that can; [hiddenIndices] are terms the prompt leaves blank. */
    data class Sequence(val terms: List<Int>, val minimalGears: Int, val hiddenIndices: Set<Int> = emptySet()) : BuildTarget { override val kind get() = "sequence" }

    /** A conversion table: each row is (count of big units) → expected count of small units. */
    data class Table(val bigUnit: String, val smallUnit: String, val factor: Int, val rows: List<Int>) : BuildTarget { override val kind get() = "table" }

    /** Two structurally different ways to make [value] from `parts`. */
    data class TwoWays(val value: Fraction, val denominators: List<Int>) : BuildTarget { override val kind get() = "two-ways" }
}
