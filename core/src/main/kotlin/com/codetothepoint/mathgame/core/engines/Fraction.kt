package com.codetothepoint.mathgame.core.engines

/**
 * An exact rational with a positive denominator. [of] reduces to lowest terms; the primary constructor
 * keeps the denomination as given, which is how a physical piece is written: `Fraction(2, 8)` is a
 * two-eighths piece, not a quarter. Arithmetic always reduces; compare amounts with [compareTo].
 */
data class Fraction(val num: Int, val den: Int) : Comparable<Fraction> {
    init { require(den > 0) { "denominator must be positive: $num/$den" } }

    operator fun plus(o: Fraction): Fraction = of(num * o.den + o.num * den, den * o.den)
    operator fun minus(o: Fraction): Fraction = of(num * o.den - o.num * den, den * o.den)
    operator fun times(k: Int): Fraction = of(num * k, den)
    /** Orders by amount only: `Fraction(2, 8).compareTo(Fraction.of(1, 4)) == 0`. */
    override fun compareTo(other: Fraction): Int = (num.toLong() * other.den).compareTo(other.num.toLong() * den)
    fun toDouble(): Double = num.toDouble() / den
    val isUnit: Boolean get() = num == 1
    val isWhole: Boolean get() = num % den == 0
    /** The same amount over [newDen], or null when it is not expressible. */
    fun withDenominator(newDen: Int): Fraction? = if ((num.toLong() * newDen) % den == 0L) Fraction(num * newDen / den, newDen) else null
    override fun toString(): String = "$num/$den"

    companion object {
        val ZERO = Fraction(0, 1)
        fun of(num: Int, den: Int): Fraction {
            require(den != 0)
            val sign = if (den < 0) -1 else 1
            val g = gcd(kotlin.math.abs(num), kotlin.math.abs(den)).coerceAtLeast(1)
            return Fraction(sign * num / g, sign * den / g)
        }
        fun whole(n: Int) = Fraction(n, 1)
        /** Parses `3/8`, `1`, or `1 3/8`. */
        fun parse(s: String): Fraction {
            val t = s.trim()
            if (' ' in t) {
                val (w, f) = t.split(' ', limit = 2)
                return whole(w.toInt()) + parse(f)
            }
            if ('/' in t) {
                val (n, d) = t.split('/')
                return of(n.trim().toInt(), d.trim().toInt())
            }
            return whole(t.toInt())
        }
        fun sum(parts: Iterable<Fraction>): Fraction = parts.fold(ZERO) { a, b -> a + b }
    }
}

tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
fun lcm(a: Int, b: Int): Int = a / gcd(a, b) * b

fun isPrime(n: Int): Boolean {
    if (n < 2) return false
    var i = 2
    while (i * i <= n) { if (n % i == 0) return false; i++ }
    return true
}

/** Unordered factor pairs (a, b) with a ≤ b and a·b = n. */
fun factorPairs(n: Int): List<Pair<Int, Int>> = (1..n).filter { it * it <= n && n % it == 0 }.map { it to n / it }

/** Offsets at which a side of length [n] is cut along place-value lines: 23 → {20}, 345 → {300, 340}. */
fun placeValueCuts(n: Int): Set<Int> {
    if (n < 10) return emptySet()
    val cuts = mutableSetOf<Int>()
    var place = 10
    while (place <= n) place *= 10
    place /= 10
    var acc = 0
    while (place >= 10) {
        acc += ((n / place) % 10) * place
        if (acc in 1 until n) cuts += acc
        place /= 10
    }
    return cuts
}
