package com.codetothepoint.mathgame.core.engines

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/** A point on the dot grid. */
data class Pt(val x: Int, val y: Int) {
    operator fun minus(o: Pt) = Vec(x - o.x, y - o.y)
    operator fun plus(v: Vec) = Pt(x + v.dx, y + v.dy)
}

data class Vec(val dx: Int, val dy: Int) {
    fun dot(o: Vec): Int = dx * o.dx + dy * o.dy
    fun cross(o: Vec): Int = dx * o.dy - dy * o.dx
    val lengthSq: Int get() = dx * dx + dy * dy
    val length: Double get() = sqrt(lengthSq.toDouble())
    fun isParallel(o: Vec): Boolean = cross(o) == 0 && lengthSq > 0 && o.lengthSq > 0
    fun isPerpendicular(o: Vec): Boolean = dot(o) == 0 && lengthSq > 0 && o.lengthSq > 0
}

/** Strokes a player can place on the dot grid (engine-build.md §3, `strokes`). */
sealed interface Stroke {
    data class Segment(val a: Pt, val b: Pt) : Stroke { val vec: Vec get() = b - a }
    data class Ray(val from: Pt, val through: Pt) : Stroke { val vec: Vec get() = through - from }
    data class Line(val a: Pt, val b: Pt) : Stroke { val vec: Vec get() = b - a }
    data class Point(val at: Pt) : Stroke
}

/**
 * A mirror line on the grid. Axis-aligned lines are stored doubled (`x = c2/2`) so a line between
 * two dots exists; diagonals are `y = x + c` and `y = -x + c` with integer c.
 */
data class MirrorLine(val kind: Kind, val c2: Int) {
    enum class Kind { VERTICAL, HORIZONTAL, DIAGONAL_UP, DIAGONAL_DOWN }

    /** Reflects a point; every image is on the integer grid for these line families. */
    fun reflect(p: Pt): Pt = when (kind) {
        Kind.VERTICAL -> Pt(c2 - p.x, p.y)
        Kind.HORIZONTAL -> Pt(p.x, c2 - p.y)
        Kind.DIAGONAL_UP -> Pt(p.y - c2, p.x + c2)       // y = x + c, c2 = c
        Kind.DIAGONAL_DOWN -> Pt(c2 - p.y, c2 - p.x)     // y = -x + c, c2 = c
    }

    companion object {
        /** `x = x2 / 2`. */
        fun vertical(x2: Int) = MirrorLine(Kind.VERTICAL, x2)
        /** `y = y2 / 2`. */
        fun horizontal(y2: Int) = MirrorLine(Kind.HORIZONTAL, y2)
        fun diagonalUp(c: Int) = MirrorLine(Kind.DIAGONAL_UP, c)
        fun diagonalDown(c: Int) = MirrorLine(Kind.DIAGONAL_DOWN, c)

        /** Every line of symmetry of a point set, searched over the lines that can map grid points to grid points. */
        fun symmetryLinesOf(points: Set<Pt>): Set<MirrorLine> {
            if (points.isEmpty()) return emptySet()
            val xs = points.map { it.x }; val ys = points.map { it.y }
            val out = mutableSetOf<MirrorLine>()
            for (x2 in (xs.min() * 2)..(xs.max() * 2)) out += vertical(x2)
            for (y2 in (ys.min() * 2)..(ys.max() * 2)) out += horizontal(y2)
            for (c in (ys.min() - xs.max())..(ys.max() - xs.min())) out += diagonalUp(c)
            for (c in (ys.min() + xs.min())..(ys.max() + xs.max())) out += diagonalDown(c)
            return out.filter { line -> points.all { line.reflect(it) in points } }.toSet()
        }
    }
}

/** Degrees of the interior angle at [vertex] between [a] and [b]. */
fun angleDegrees(vertex: Pt, a: Pt, b: Pt): Double {
    val u = a - vertex
    val v = b - vertex
    val d = atan2(u.cross(v).toDouble(), u.dot(v).toDouble()) * 180.0 / Math.PI
    return abs(d)
}

/** A closed polygon traced from segments, when the segments form exactly one simple closed loop. */
class Polygon(val vertices: List<Pt>) {
    val n: Int get() = vertices.size
    fun side(i: Int): Vec = vertices[(i + 1) % n] - vertices[i]
    val sides: List<Vec> get() = (0 until n).map { side(it) }
    val sideLengthsSq: List<Int> get() = sides.map { it.lengthSq }
    /** Interior angles in degrees, in vertex order. */
    val angles: List<Double> get() = (0 until n).map { i -> angleDegrees(vertices[i], vertices[(i + n - 1) % n], vertices[(i + 1) % n]) }

    fun parallelPairs(): Int = (0 until n).sumOf { i -> ((i + 1) until n).count { j -> side(i).isParallel(side(j)) } }
    fun perpendicularAdjacentCount(): Int = (0 until n).count { i -> side(i).isPerpendicular(side((i + 1) % n)) }
    fun hasRightAngle(): Boolean = perpendicularAdjacentCount() > 0
    fun isConvex(): Boolean {
        val signs = (0 until n).map { i -> side(i).cross(side((i + 1) % n)).let { if (it > 0) 1 else if (it < 0) -1 else 0 } }
        return signs.all { it >= 0 } || signs.all { it <= 0 }
    }
    val allSidesEqual: Boolean get() = sideLengthsSq.distinct().size == 1

    /** Congruence by the multiset of all pairwise squared distances (exact for triangles; sound for grid quadrilaterals in practice). */
    fun congruentTo(o: Polygon): Boolean {
        if (n != o.n) return false
        fun dists(p: Polygon) = p.vertices.indices.flatMap { i -> (i + 1 until p.n).map { j -> (p.vertices[j] - p.vertices[i]).lengthSq } }.sorted()
        return dists(this) == dists(o)
    }

    companion object {
        /**
         * Builds the polygon when [segments] form exactly one simple closed loop with no stray or repeated
         * strokes; otherwise null. Never throws: a half-built or odd construction is "not yet", not a crash.
         */
        fun from(segments: List<Stroke.Segment>): Polygon? {
            // A segment drawn twice, in either direction, is one segment.
            val unique = segments.filter { it.a != it.b }.map { setOf(it.a, it.b) }.distinct().map { it.toList() }
            if (unique.size < 3) return null
            val adj = mutableMapOf<Pt, MutableList<Pt>>()
            for ((a, b) in unique) {
                adj.getOrPut(a) { mutableListOf() } += b
                adj.getOrPut(b) { mutableListOf() } += a
            }
            if (adj.values.any { it.size != 2 }) return null
            if (adj.size != unique.size) return null
            val start = unique.first()[0]
            val order = mutableListOf(start)
            var prev: Pt? = null
            var cur = start
            while (true) {
                val next = adj.getValue(cur).firstOrNull { it != prev } ?: return null
                if (next == start) break
                if (next in order) return null
                order += next
                prev = cur
                cur = next
                if (order.size > unique.size) return null
            }
            if (order.size != unique.size) return null
            // Drop collinear vertices (a side drawn as two strokes is still one side).
            val cleaned = order.filterIndexed { i, p ->
                val a = order[(i + order.size - 1) % order.size]
                val b = order[(i + 1) % order.size]
                (p - a).cross(b - p) != 0
            }
            if (cleaned.size < 3) return null
            val poly = Polygon(cleaned)
            return if (poly.isSimple()) poly else null
        }
    }

    /** No two non-adjacent sides touch or cross: a bow-tie is not a quadrilateral. */
    fun isSimple(): Boolean {
        for (i in 0 until n) for (j in i + 1 until n) {
            if (j == i + 1 || (i == 0 && j == n - 1)) continue
            if (segmentsIntersect(vertices[i], vertices[(i + 1) % n], vertices[j], vertices[(j + 1) % n])) return false
        }
        return true
    }

    private fun segmentsIntersect(p1: Pt, p2: Pt, q1: Pt, q2: Pt): Boolean {
        fun orient(a: Pt, b: Pt, c: Pt): Int = (b - a).cross(c - a).let { if (it > 0) 1 else if (it < 0) -1 else 0 }
        fun onSegment(a: Pt, b: Pt, c: Pt): Boolean = c.x in minOf(a.x, b.x)..maxOf(a.x, b.x) && c.y in minOf(a.y, b.y)..maxOf(a.y, b.y)
        val o1 = orient(p1, p2, q1); val o2 = orient(p1, p2, q2); val o3 = orient(q1, q2, p1); val o4 = orient(q1, q2, p2)
        if (o1 != o2 && o3 != o4) return true
        if (o1 == 0 && onSegment(p1, p2, q1)) return true
        if (o2 == 0 && onSegment(p1, p2, q2)) return true
        if (o3 == 0 && onSegment(q1, q2, p1)) return true
        if (o4 == 0 && onSegment(q1, q2, p2)) return true
        return false
    }
}
