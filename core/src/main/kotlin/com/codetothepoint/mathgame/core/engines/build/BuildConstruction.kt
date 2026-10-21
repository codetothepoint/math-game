package com.codetothepoint.mathgame.core.engines.build

import com.codetothepoint.mathgame.core.engines.Fraction
import com.codetothepoint.mathgame.core.engines.Pt
import com.codetothepoint.mathgame.core.engines.Stroke

/**
 * What the player has built in the workspace at lock-in, plus the parts of the build history the
 * engine spec says it can observe (engine-build.md §7). Produced by the UI, judged by [BuildEngine].
 */
sealed interface BuildConstruction

/** A rectangle of tiles with optional cuts. Cuts are offsets from the left/top edge along each dimension. */
data class TileRect(val w: Int, val h: Int, val cutsAlongW: Set<Int> = emptySet(), val cutsAlongH: Set<Int> = emptySet()) {
    val area: Int get() = w * h
    val dims: Set<Int> get() = setOf(w, h)
    /** Number of regions the cuts make. */
    val regions: Int get() = (cutsAlongW.size + 1) * (cutsAlongH.size + 1)
}

/** The tiles workspace: rectangles in placement order, plus the remainder tray and the chunks laid for a quotient. */
data class TilesConstruction(
    val rects: List<TileRect>,
    val tray: Int = 0,
    /** For quotients: each placement as (rows, columns) in the order laid. */
    val placements: List<Pair<Int, Int>> = emptyList(),
) : BuildConstruction

/** Place-value pieces by denomination in base units (1, 10, 100, 1000; decimal mode 1, 10, 100 hundredths). */
data class BlocksConstruction(val pieces: Map<Int, Int>, val bundles: Int = 0, val unbundles: Int = 0) : BuildConstruction {
    val total: Int get() = pieces.entries.sumOf { (denom, count) -> denom * count }
}

/** Fraction pieces, grouped into one or more "ways"; each way is the list of pieces it holds. */
data class PartsConstruction(val ways: List<List<Fraction>>, val duplications: Int = 0, val mergedWholes: Int = 0) : BuildConstruction {
    val sums: List<Fraction> get() = ways.map { Fraction.sum(it) }
}

/** Strokes on the dot grid, in placement order. */
data class StrokesConstruction(val strokes: List<Stroke>) : BuildConstruction {
    val segments: List<Stroke.Segment> get() = strokes.filterIsInstance<Stroke.Segment>()
    val rays: List<Stroke.Ray> get() = strokes.filterIsInstance<Stroke.Ray>()
    val lines: List<Stroke.Line> get() = strokes.filterIsInstance<Stroke.Line>()
    val points: Set<Pt> get() = strokes.flatMap {
        when (it) {
            is Stroke.Segment -> listOf(it.a, it.b)
            is Stroke.Ray -> listOf(it.from)
            is Stroke.Line -> emptyList()
            is Stroke.Point -> listOf(it.at)
        }
    }.toSet()
}

/** Marks on a line plot. */
data class MarksConstruction(val marks: List<Fraction>) : BuildConstruction

/** A gear chain: a start value and operations applied left to right to produce each next term. */
data class GearsConstruction(val start: Int, val gears: List<Gear>) : BuildConstruction {
    fun terms(n: Int): List<Int> {
        val out = mutableListOf(start)
        while (out.size < n) out += gears.fold(out.last()) { v, g -> g.apply(v) }
        return out
    }
}

data class Gear(val op: Op, val n: Int) {
    enum class Op { ADD, SUBTRACT, MULTIPLY, DIVIDE }
    fun apply(v: Int): Int = when (op) {
        Op.ADD -> v + n
        Op.SUBTRACT -> v - n
        Op.MULTIPLY -> v * n
        Op.DIVIDE -> v / n
    }
}

/** A conversion table as filled: big-unit count → small-unit count placed. */
data class UnitsConstruction(val rows: Map<Int, Int>) : BuildConstruction
