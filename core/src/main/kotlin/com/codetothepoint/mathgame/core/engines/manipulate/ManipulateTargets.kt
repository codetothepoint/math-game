package com.codetothepoint.mathgame.core.engines.manipulate

import com.codetothepoint.mathgame.core.engines.Fraction
import com.codetothepoint.mathgame.core.engines.MirrorLine
import com.codetothepoint.mathgame.core.engines.Pt

/** What a `manipulate` item asks for (engine-manipulate.md §4). */
sealed interface ManipulateTarget {
    val kind: String

    /** Place the marker at [value] on a number line whose finest tick is [finestTick]. */
    data class LineValue(val value: Fraction, val finestTick: Fraction) : ManipulateTarget { override val kind get() = "value" }

    /** Round [number] to [place] (10, 100, 1000, …) on a number line; the answer is [rounded]. */
    data class Nearest(val number: Long, val place: Long) : ManipulateTarget {
        override val kind get() = "nearest"
        val lower: Long get() = number / place * place
        val upper: Long get() = lower + place
        val midpoint: Long get() = lower + place / 2
        val rounded: Long get() = if (number - lower < place / 2) lower else upper
    }

    /** Re-cut the adjustable bar to [targetDen] so it shows the same amount as [reference]. */
    data class Equal(val reference: Fraction, val targetDen: Int) : ManipulateTarget {
        override val kind get() = "equal"
        val answerNum: Int get() = reference.withDenominator(targetDen)!!.num
    }

    /** Cut and shade a bar so [value] appears as [parts] shaded pieces over [den]. */
    data class Partition(val value: Fraction, val den: Int, val parts: Int) : ManipulateTarget { override val kind get() = "partition" }

    /** The result bar reads [value] (den fixed by the item). */
    data class BarValue(val value: Fraction, val den: Int) : ManipulateTarget { override val kind get() = "value" }

    /** Pour [a] and [b] together (or take [b] from [a]); the result bar must read the answer over [den]. */
    data class BarSum(val a: Fraction, val b: Fraction, val op: Op, val den: Int) : ManipulateTarget {
        override val kind get() = "value"
        enum class Op { ADD, SUBTRACT }
        val value: Fraction get() = if (op == Op.ADD) a + b else a - b
    }

    /** Shade [hundredths] of a hundredths grid; [expressAs] says whether the prompt reads as a fraction or a decimal. */
    data class GridValue(val hundredths: Int, val expressAs: String = "fraction") : ManipulateTarget { override val kind get() = "value" }

    /**
     * Set the arms to [degrees]. [turn] is the fraction of a full turn the prompt shows (null: the prompt
     * shows degrees); with [askDegrees] the prompt shows the turn and asks for its degrees.
     */
    data class AngleValue(val degrees: Int, val toleranceDegrees: Int, val turn: Fraction? = null, val askDegrees: Boolean = false) : ManipulateTarget { override val kind get() = "value" }

    /** Two or more adjacent angles total [total]; the [known] ones are given; set the unknown. */
    data class AngleUnknown(val total: Int, val known: List<Int>, val toleranceDegrees: Int) : ManipulateTarget {
        override val kind get() = "unknown"
        val unknown: Int get() = total - known.sum()
    }

    /** The place-value chart reads [value], starting from [start]; [compareDigit] is a digit to follow between the two. */
    data class ChartValue(val value: Long, val start: Long, val compareDigit: Int? = null) : ManipulateTarget { override val kind get() = "value" }

    /**
     * A grid shading of [hundredths] cells and a number-line marker at the same value must agree.
     * One instrument is given and fixed ([givenOnGrid] says which); the player sets the other.
     */
    data class Match(val hundredths: Int, val givenOnGrid: Boolean) : ManipulateTarget { override val kind get() = "match" }

    /** Place the mirror so [figure] reflects onto itself; with [askAll], lock in every line of symmetry. [lines] may be empty. */
    data class Symmetric(val figure: Set<Pt>, val askAll: Boolean) : ManipulateTarget {
        override val kind get() = "symmetric"
        val lines: Set<MirrorLine> get() = MirrorLine.symmetryLinesOf(figure)
    }
}
