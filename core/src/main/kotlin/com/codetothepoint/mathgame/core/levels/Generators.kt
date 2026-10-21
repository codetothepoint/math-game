package com.codetothepoint.mathgame.core.levels

import com.codetothepoint.mathgame.core.curriculum.Engine
import com.codetothepoint.mathgame.core.curriculum.Representation
import com.codetothepoint.mathgame.core.engines.Fraction
import com.codetothepoint.mathgame.core.engines.MirrorLine
import com.codetothepoint.mathgame.core.engines.Pt
import com.codetothepoint.mathgame.core.engines.build.BuildTarget
import com.codetothepoint.mathgame.core.engines.isPrime
import com.codetothepoint.mathgame.core.engines.manipulate.AngleState
import com.codetothepoint.mathgame.core.engines.manipulate.FractionBarState
import com.codetothepoint.mathgame.core.engines.manipulate.GridState
import com.codetothepoint.mathgame.core.engines.manipulate.InstrumentState
import com.codetothepoint.mathgame.core.engines.manipulate.ManipulateTarget
import com.codetothepoint.mathgame.core.engines.manipulate.MatchState
import com.codetothepoint.mathgame.core.engines.manipulate.MirrorState
import com.codetothepoint.mathgame.core.engines.manipulate.NumberLineState
import com.codetothepoint.mathgame.core.engines.manipulate.PlaceChartState
import com.codetothepoint.mathgame.core.recipes.Band
import com.codetothepoint.mathgame.core.recipes.Params
import com.codetothepoint.mathgame.core.recipes.Recipe
import com.codetothepoint.mathgame.core.recipes.RecipeKey
import kotlin.random.Random

/** Makes one item for a recipe, band, and representation. [tolerance] is the recipe's override block. */
fun interface ItemGenerator {
    fun generate(params: Params, tolerance: Params, band: Band, representation: Representation, rng: Random): GeneratedItem
}

/**
 * Generators for every world-4 recipe (specs/mechanics/recipes/world-04.yaml), keyed by node and engine.
 * The engine specs' worked examples (§10) say what each band should produce; the recipe's parameters
 * say the ranges. Every generated item is solvable by construction; GeneratorPropertyTest checks it,
 * and checks that every parameter a recipe declares is one its generator reads ([KEYS]).
 */
object Generators {
    /** The dot grid every `strokes` figure must fit (engine-build.md §10: a 7×7 dot grid). */
    val GRID: IntRange = 0..6

    fun forRecipe(recipe: Recipe): ItemGenerator = TABLE[recipe.key] ?: throw IllegalArgumentException("No generator for ${recipe.node}/${recipe.engine.key}")
    fun has(recipe: Recipe): Boolean = recipe.key in TABLE

    /** Generator parameters each recipe may use. Mirrors GENERATOR_KEYS in tools/recipes.py. */
    val KEYS: Map<RecipeKey, Set<String>> = mapOf(
        RecipeKey("w04.oa.factors-multiples-primes", Engine.BUILD) to setOf("areas", "primes_per_level", "include_squares"),
        RecipeKey("w04.oa.patterns-and-rules", Engine.BUILD) to setOf("rules", "terms_shown", "max_gears", "hidden_terms"),
        RecipeKey("w04.nbt.place-value-ten-times", Engine.BUILD) to setOf("multiplier", "base_digits", "digit_moves"),
        RecipeKey("w04.nbt.multiply-multidigit", Engine.BUILD) to setOf("factors"),
        RecipeKey("w04.nbt.divide-by-one-digit", Engine.BUILD) to setOf("dividend", "divisor", "remainder", "max_side"),
        RecipeKey("w04.nf.decompose-fractions", Engine.BUILD) to setOf("denominators", "numerator_max", "mixed_numbers"),
        RecipeKey("w04.nf.multiply-fraction-by-whole", Engine.BUILD) to setOf("denominators", "whole", "read_as_mixed"),
        RecipeKey("w04.nf.tenths-and-hundredths", Engine.BUILD) to setOf("tenths", "hundredths"),
        RecipeKey("w04.md.unit-conversion", Engine.BUILD) to setOf("systems", "rows", "skip_rows"),
        RecipeKey("w04.md.area-perimeter-formulas", Engine.BUILD) to setOf("constraints", "area", "perimeter", "unknown_side"),
        RecipeKey("w04.md.line-plots-fractions", Engine.BUILD) to setOf("tick", "values"),
        RecipeKey("w04.md.measure-angles", Engine.BUILD) to setOf("degrees", "range"),
        RecipeKey("w04.g.lines-and-angles", Engine.BUILD) to setOf("constraints"),
        RecipeKey("w04.g.classify-by-lines-angles", Engine.BUILD) to setOf("constraints"),
        RecipeKey("w04.g.line-symmetry", Engine.BUILD) to setOf("line", "half_given"),
        RecipeKey("w04.nbt.place-value-ten-times", Engine.MANIPULATE) to setOf("base_digits", "operation", "compare_digits"),
        RecipeKey("w04.nbt.round-multidigit", Engine.MANIPULATE) to setOf("digits", "place", "near_midpoint"),
        RecipeKey("w04.nf.fraction-equivalence", Engine.MANIPULATE) to setOf("pairs", "reverse"),
        RecipeKey("w04.nf.decompose-fractions", Engine.MANIPULATE) to setOf("denominators", "numerator_max", "mixed_numbers", "parts"),
        RecipeKey("w04.nf.add-sub-like-denominators", Engine.MANIPULATE) to setOf("denominators", "operations", "sum_max", "mixed_numbers"),
        RecipeKey("w04.nf.tenths-and-hundredths", Engine.MANIPULATE) to setOf("tenths", "hundredths", "express_as"),
        RecipeKey("w04.nf.decimal-notation", Engine.MANIPULATE) to setOf("granularity", "direction", "confusables"),
        RecipeKey("w04.md.angles-concept", Engine.MANIPULATE) to setOf("turns", "also_degrees", "fractions_of_turn", "ask_degrees"),
        RecipeKey("w04.md.measure-angles", Engine.MANIPULATE) to setOf("degrees", "range", "mode", "reflex"),
        RecipeKey("w04.md.additive-angles", Engine.MANIPULATE) to setOf("total", "known", "parts"),
        RecipeKey("w04.g.line-symmetry", Engine.MANIPULATE) to setOf("figures", "ask_all_lines"),
    )

    private fun build(node: String, gen: (Params, Params, Band, Random) -> BuildTarget) =
        RecipeKey(node, Engine.BUILD) to ItemGenerator { p, tol, b, r, rng -> GeneratedItem.Build(b, r, gen(p, tol, b, rng)) }

    private fun manipulate(node: String, gen: (Params, Params, Band, Random) -> Pair<ManipulateTarget, InstrumentState>) =
        RecipeKey(node, Engine.MANIPULATE) to ItemGenerator { p, tol, b, r, rng -> gen(p, tol, b, rng).let { (t, s) -> GeneratedItem.Manipulate(b, r, t, s) } }

    private fun Random.pick(range: IntRange): Int = range.first + nextInt(range.last - range.first + 1)
    private fun <T> Random.pick(list: List<T>): T = list[nextInt(list.size)]

    /**
     * Angle tolerance (engine-manipulate.md §6): ±2°, ±1° at high; a recipe's `tolerance` block overrides
     * with `degrees` and `high_band_degrees`. `build` angles default to ±2° at every band (engine-build §4).
     */
    fun angleTolerance(tol: Params, band: Band, highDefault: Int): Int = when {
        band == Band.HIGH && tol.has("high_band_degrees") -> tol.int("high_band_degrees")
        tol.has("degrees") -> tol.int("degrees")
        band == Band.HIGH -> highDefault
        else -> 2
    }

    private val TABLE: Map<RecipeKey, ItemGenerator> = mapOf(
        // ---------------------------------------------------------------- build
        build("w04.oa.factors-multiples-primes") { p, _, _, rng ->
            val areas = p.range("areas")
            val wantPrime = rng.nextInt(8) < p.int("primes_per_level")   // about primes_per_level of eight items
            val wantSquare = !wantPrime && p.bool("include_squares") && rng.nextInt(6) == 0
            val candidates = areas.filter { n ->
                when {
                    wantPrime -> isPrime(n)
                    wantSquare -> n > 1 && isSquare(n)
                    else -> n > 1 && !isPrime(n)
                }
            }.ifEmpty { areas.filter { it > 1 } }
            BuildTarget.FactorPairs(rng.pick(candidates))
        },
        build("w04.oa.patterns-and-rules") { p, _, _, rng ->
            val ops = p.strings("rules")
            val terms = p.int("terms_shown")
            val gearCount = if (p.int("max_gears") >= 2 && rng.nextBoolean()) 2 else 1
            val chosen = List(gearCount) { rng.pick(ops) }.toMutableList()
            if (chosen.count { it == "multiply" } == 2) chosen[1] = "add"
            val hasMultiply = "multiply" in chosen
            if (hasMultiply) chosen.replaceAll { if (it == "subtract") "add" else it }   // keeps the sequence increasing
            var steps: List<Step>
            do {
                steps = chosen.map { op ->
                    when (op) {
                        "add" -> Step.Add(rng.pick(2..9))
                        "subtract" -> Step.Subtract(rng.pick(1..5))
                        else -> Step.Multiply(if (terms >= 6) 2 else rng.pick(2..3))
                    }
                }
            } while (steps.fold(0) { v, s -> s.apply(v) } == 0 && steps.none { it is Step.Multiply })   // add 3 then subtract 3 is no rule at all
            val start = if (hasMultiply) rng.pick(1..4) else if (steps.any { it is Step.Subtract }) rng.pick(60..90) else rng.pick(1..12)
            val seq = mutableListOf(start)
            while (seq.size < terms) seq += steps.fold(seq.last()) { v, s -> s.apply(v) }
            val hidden = (1..p.intOr("hidden_terms", 0)).map { rng.pick(1..terms - 2) }.toSet()   // never the first or last term
            BuildTarget.Sequence(seq, minimalGears = minimalGears(steps), hiddenIndices = hidden)
        },
        build("w04.nbt.place-value-ten-times") { p, _, _, rng ->
            val digits = rng.pick(p.ints("base_digits"))
            val base = rng.pick(pow10(digits - 1)..(pow10(digits) - 1))
            BuildTarget.Quantity(base * p.int("multiplier"), base = base, digitMoves = p.bool("digit_moves"))
        },
        build("w04.nbt.multiply-multidigit") { p, _, _, rng ->
            val ranges = p.ranges("factors")
            BuildTarget.Product(rng.pick(ranges[0]), rng.pick(ranges[1]))
        },
        build("w04.nbt.divide-by-one-digit") { p, _, _, rng ->
            val divisor = rng.pick(p.range("divisor"))
            val maxSide = p.intOr("max_side", 99)
            val range = p.range("dividend").let { it.first..minOf(it.last, maxSide * divisor + divisor - 1) }   // quotient ≤ max_side
            var dividend = rng.pick(range)
            if (!p.bool("remainder")) dividend -= dividend % divisor
            if (dividend < maxOf(range.first, divisor)) dividend += divisor
            BuildTarget.Quotient(dividend, divisor)
        },
        build("w04.nf.decompose-fractions") { p, _, _, rng ->
            val mixed = p.bool("mixed_numbers")
            val den = rng.pick(p.ints("denominators").filter { mixed || it >= 3 })   // a half has no two ways over halves
            val maxNum = if (mixed) den + den / 2 else minOf(p.intOr("numerator_max", den - 1), den - 1)
            BuildTarget.TwoWays(Fraction(rng.pick(2..maxOf(2, maxNum)), den), listOf(den))   // 6/8 stays 6/8: a piece keeps its denomination
        },
        build("w04.nf.multiply-fraction-by-whole") { p, _, _, rng ->
            val den = rng.pick(p.ints("denominators"))
            val num = rng.pick(1 until den)
            val groups = rng.pick(p.range("whole"))
            // 2/8 × 3 reads "six eighths", not three quarters: both the piece and the total keep the denominator.
            BuildTarget.FractionQuantity(Fraction(num * groups, den), listOf(den), groups = groups, each = Fraction(num, den), readAsMixed = p.bool("read_as_mixed"))
        },
        build("w04.nf.tenths-and-hundredths") { p, _, _, rng ->
            BuildTarget.Quantity(rng.pick(p.range("tenths")) * 10 + rng.pick(p.range("hundredths")), scale = 100)
        },
        build("w04.md.unit-conversion") { p, _, _, rng ->
            val (big, small, factor) = rng.pick(CONVERSIONS.getValue(rng.pick(p.strings("systems"))))
            val rows = p.int("rows")
            val chosen = if (p.bool("skip_rows")) (1..rows + 2).shuffled(rng).take(rows).sorted() else (1..rows).toList()
            BuildTarget.Table(big, small, factor, chosen)
        },
        build("w04.md.area-perimeter-formulas") { p, _, _, rng ->
            val constraints = p.strings("constraints")
            if ("area" in constraints) {
                val area = rng.pick(composites(p.range("area")))
                if ("perimeter" in constraints) {
                    val w = rng.pick((1..area).filter { it * it <= area && area % it == 0 })
                    BuildTarget.RectConstraint(area = area, perimeter = 2 * (w + area / w))
                } else BuildTarget.RectConstraint(area = area)
            } else {
                val perimeter = rng.pick(p.range("perimeter").filter { it % 2 == 0 && it >= 8 })
                val side = rng.pick(1 until perimeter / 2)
                BuildTarget.RectConstraint(perimeter = perimeter, givenSide = if (p.bool("unknown_side")) side else null)
            }
        },
        build("w04.md.line-plots-fractions") { p, _, _, rng ->
            val tick = Fraction.parse(p.string("tick"))
            val maxTicks = 4 * tick.den   // values up to 4 units
            BuildTarget.Plot(List(p.int("values")) { Fraction.of(rng.pick(1..maxTicks) * tick.num, tick.den) }, tick)
        },
        build("w04.md.measure-angles") { p, tol, b, rng -> BuildTarget.Angle(pickDegrees(p, rng, reflex = false), toleranceDegrees = angleTolerance(tol, b, highDefault = 2)) },
        build("w04.g.lines-and-angles") { p, _, _, rng -> BuildTarget.ShapeConstraint(rng.pick(p.strings("constraints"))) },
        build("w04.g.classify-by-lines-angles") { p, _, _, rng -> BuildTarget.ShapeConstraint(rng.pick(p.strings("constraints"))) },
        build("w04.g.line-symmetry") { p, _, _, rng ->
            require(p.bool("half_given")) { "line-symmetry build items give half the figure" }
            val line = when (rng.pick(p.strings("line"))) {
                "vertical" -> MirrorLine.vertical(rng.pick(4..8))
                "horizontal" -> MirrorLine.horizontal(rng.pick(4..8))
                else -> if (rng.nextBoolean()) MirrorLine.diagonalUp(0) else MirrorLine.diagonalDown(rng.pick(4..6))   // keeps both halves on the 7×7 grid
            }
            BuildTarget.SymmetricCompletion(line, halfFigure(line, rng))
        },
        // ---------------------------------------------------------------- manipulate
        manipulate("w04.nbt.place-value-ten-times") { p, _, _, rng ->
            val digits = rng.pick(p.ints("base_digits"))
            val base = rng.pick(pow10(digits - 1)..(pow10(digits) - 1)).toLong()
            val (start, target) = if (rng.pick(p.strings("operation")) == "divide-by-ten") base * 10 to base else base to base * 10
            val compareDigit = if (p.bool("compare_digits")) base.toString().first { it != '0' }.digitToInt() else null
            ManipulateTarget.ChartValue(target, start, compareDigit) to PlaceChartState(start)
        },
        manipulate("w04.nbt.round-multidigit") { p, _, _, rng ->
            val digits = rng.pick(p.ints("digits"))
            val place = when (val pl = p.string("place")) {
                "tens" -> 10L; "hundreds" -> 100L; "thousands" -> 1000L
                "any" -> rng.pick(listOf(10L, 100L, 1000L, 10_000L)).coerceAtMost(pow10(digits - 1).toLong())
                else -> throw IllegalArgumentException("place $pl")
            }
            var number = rng.pick(pow10(digits - 1)..(pow10(digits) - 1)).toLong()
            if (p.bool("near_midpoint")) {
                val lower = number / place * place
                number = lower + place / 2 + rng.pick(-5..5).toLong().coerceIn(-(place / 2 - 1), place / 2 - 1)
            }
            ManipulateTarget.Nearest(number, place) to NumberLineState(null, Fraction.whole(place.toInt()))
        },
        manipulate("w04.nf.fraction-equivalence") { p, _, _, rng ->
            var (from, to) = rng.pick(p.pairs("pairs"))
            if (p.bool("reverse") && from < to && rng.nextBoolean()) { val t = from; from = to; to = t }
            val numerators = (1 until from).filter { Fraction.of(it, from).withDenominator(to) != null }
            val num = rng.pick(numerators)
            ManipulateTarget.Equal(Fraction(num, from), to) to FractionBarState(from, listOf(num), initialDen = from)   // "3/4 = ?/8" shows 3/4 even when num/from reduces
        },
        manipulate("w04.nf.decompose-fractions") { p, _, _, rng ->
            val mixed = p.bool("mixed_numbers")
            val den = rng.pick(p.ints("denominators").filter { mixed || it >= 3 })
            val parts = rng.pick(p.ints("parts"))
            val maxNum = if (mixed) den + den / 2 else minOf(p.intOr("numerator_max", den - 1), den - 1)
            val num = rng.pick(maxOf(parts, 2)..maxOf(parts, maxNum))
            ManipulateTarget.Partition(Fraction(num, den), den, parts) to FractionBarState(den, listOf(num), initialDen = den)   // keeps its denomination, like the other displayed targets
        },
        manipulate("w04.nf.add-sub-like-denominators") { p, _, _, rng ->
            val den = rng.pick(p.ints("denominators"))
            val maxTotal = if (p.bool("mixed_numbers")) 2 * den else p.int("sum_max") * den
            val op = if (rng.pick(p.strings("operations")) == "add") ManipulateTarget.BarSum.Op.ADD else ManipulateTarget.BarSum.Op.SUBTRACT
            val (a, b) = if (op == ManipulateTarget.BarSum.Op.ADD) {
                val x = rng.pick(1..maxTotal - 1); x to rng.pick(1..maxTotal - x)
            } else {
                val x = rng.pick(2..maxTotal); x to rng.pick(1 until x)
            }
            ManipulateTarget.BarSum(Fraction(a, den), Fraction(b, den), op, den) to FractionBarState(den, emptyList(), initialDen = den)
        },
        manipulate("w04.nf.tenths-and-hundredths") { p, _, _, rng ->
            val expressAs = if (p.has("express_as")) rng.pick(p.strings("express_as")) else "fraction"
            ManipulateTarget.GridValue(rng.pick(p.range("tenths")) * 10 + rng.pick(p.range("hundredths")), expressAs) to GridState(0)
        },
        manipulate("w04.nf.decimal-notation") { p, _, _, rng ->
            val tenthsOnly = p.string("granularity") == "tenths"
            val value = when {
                tenthsOnly -> rng.pick(1..9) * 10
                p.bool("confusables") && rng.nextBoolean() -> rng.pick(1..9).let { d -> if (rng.nextBoolean()) d else d * 10 }   // 0.04 vs 0.40
                else -> rng.pick(1..99)
            }
            val givenOnGrid = !p.has("direction") || rng.pick(p.strings("direction")) == "grid-to-line"
            val tenths = Fraction.of(1, 10)   // the player's line opens at tenths and is zoomed as needed
            val hundredths = Fraction.of(1, 100)
            val line = if (givenOnGrid) NumberLineState(null, tenths) else NumberLineState(Fraction.of(value, 100), hundredths)   // a given marker is drawn at its own tick
            ManipulateTarget.Match(value, givenOnGrid) to MatchState(line, GridState(if (givenOnGrid) value else 0))
        },
        manipulate("w04.md.angles-concept") { p, tol, b, rng ->
            val tolerance = angleTolerance(tol, b, highDefault = 1)
            val askDegrees = p.bool("ask_degrees")
            val (degrees, turn) = when {
                p.has("fractions_of_turn") -> rng.pick(p.pairs("fractions_of_turn")).let { (n, d) -> 360 * n / d to Fraction.of(n, d) }
                p.has("also_degrees") && rng.nextBoolean() -> rng.pick(p.ints("also_degrees")) to null
                else -> Fraction.parse(rng.pick(p.strings("turns"))).let { 360 * it.num / it.den to it }
            }
            ManipulateTarget.AngleValue(degrees, tolerance, turn, askDegrees) to AngleState(0.0)
        },
        manipulate("w04.md.measure-angles") { p, tol, b, rng ->
            val degrees = pickDegrees(p, rng, reflex = p.bool("reflex"))
            val initial = if (rng.pick(p.strings("mode")) == "read") AngleState(degrees.toDouble(), reading = 0.0) else AngleState(0.0)
            ManipulateTarget.AngleValue(degrees, angleTolerance(tol, b, highDefault = 1)) to initial
        },
        manipulate("w04.md.additive-angles") { p, tol, b, rng ->
            val total = rng.pick(p.strings("total")).let { if (it == "any") rng.pick(60..180) else it.toInt() }
            val parts = p.intOr("parts", 2)
            val step = when (p.string("known")) { "multiples-of-10" -> 10; "multiples-of-5" -> 5; else -> 1 }
            val units = total / step
            val minUnits = (5 + step - 1) / step   // every angle at least 5°, clear of the tolerance
            val unknownUnits = rng.pick(minUnits..units - (parts - 1) * minUnits)
            var remaining = units - unknownUnits
            val known = mutableListOf<Int>()
            repeat(parts - 1) { i ->
                val left = parts - 1 - i
                val k = if (left == 1) remaining else rng.pick(minUnits..remaining - (left - 1) * minUnits)
                known += k * step
                remaining -= k
            }
            ManipulateTarget.AngleUnknown(total, known, angleTolerance(tol, b, highDefault = 1)) to AngleState(0.0)
        },
        manipulate("w04.g.line-symmetry") { p, _, _, rng ->
            val figure = rng.pick(FIGURES.getValue(rng.pick(p.strings("figures"))))
            ManipulateTarget.Symmetric(figure, askAll = p.bool("ask_all_lines")) to MirrorState(null)
        },
    )

    private sealed interface Step {
        fun apply(v: Int): Int
        data class Add(val n: Int) : Step { override fun apply(v: Int) = v + n }
        data class Subtract(val n: Int) : Step { override fun apply(v: Int) = v - n }
        data class Multiply(val n: Int) : Step { override fun apply(v: Int) = v * n }
    }

    /** The composed step is affine, v → a·v + b; one gear suffices when a = 1 or b = 0. */
    private fun minimalGears(steps: List<Step>): Int {
        var a = 1; var b = 0
        for (s in steps) when (s) {
            is Step.Add -> b += s.n
            is Step.Subtract -> b -= s.n
            is Step.Multiply -> { a *= s.n; b *= s.n }
        }
        return if (a == 1 || b == 0) 1 else 2
    }

    private fun pow10(n: Int): Int = (1..n).fold(1) { acc, _ -> acc * 10 }
    private fun isSquare(n: Int): Boolean = kotlin.math.sqrt(n.toDouble()).toInt().let { it * it == n }
    private fun composites(range: IntRange): List<Int> = range.filter { it > 1 && !isPrime(it) }.ifEmpty { range.toList() }

    private fun pickDegrees(p: Params, rng: Random, reflex: Boolean): Int {
        val range = p.range("range").let { if (reflex) it else it.first..minOf(it.last, 180) }
        return when (p.string("degrees")) {
            "multiples-of-10" -> rng.pick(range.filter { it % 10 == 0 })
            "multiples-of-5" -> rng.pick(range.filter { it % 5 == 0 })
            else -> rng.pick(range)
        }
    }

    private val CONVERSIONS: Map<String, List<Triple<String, String, Int>>> = mapOf(
        "length-customary" to listOf(Triple("foot", "inch", 12), Triple("yard", "foot", 3)),
        "length-metric" to listOf(Triple("meter", "centimeter", 100), Triple("kilometer", "meter", 1000)),
        "mass-metric" to listOf(Triple("kilogram", "gram", 1000)),
        "volume-metric" to listOf(Triple("liter", "milliliter", 1000)),
        "time" to listOf(Triple("hour", "minute", 60), Triple("minute", "second", 60), Triple("day", "hour", 24)),
    )

    /** Figures on the dot grid by family, for the symmetry instrument. Each family's line count is what its name says. */
    private val FIGURES: Map<String, List<Set<Pt>>> = mapOf(
        "one-vertical-line" to listOf(
            setOf(Pt(1, 0), Pt(3, 0), Pt(2, 3)),                       // isosceles triangle
            setOf(Pt(0, 0), Pt(4, 0), Pt(3, 2), Pt(1, 2)),             // isosceles trapezoid
            setOf(Pt(2, 0), Pt(4, 2), Pt(2, 5), Pt(0, 2)),             // kite
        ),
        "diagonal-line" to listOf(
            setOf(Pt(0, 0), Pt(3, 0), Pt(0, 3)),                       // right isosceles: y = -x + 3
            setOf(Pt(0, 0), Pt(2, 1), Pt(1, 2)),                       // kite on y = x
        ),
        "two-lines" to listOf(
            setOf(Pt(0, 0), Pt(4, 0), Pt(4, 2), Pt(0, 2)),             // rectangle
            setOf(Pt(3, 0), Pt(6, 2), Pt(3, 4), Pt(0, 2)),             // rhombus that is not a square: exactly two lines
        ),
        "regular-polygons" to listOf(
            setOf(Pt(0, 0), Pt(2, 0), Pt(2, 2), Pt(0, 2)),             // square: four lines
        ),
        "no-line" to listOf(
            setOf(Pt(0, 0), Pt(4, 0), Pt(1, 3)),                       // scalene
            setOf(Pt(0, 0), Pt(4, 0), Pt(5, 2), Pt(1, 2)),             // parallelogram
        ),
    )

    /** Two or three points strictly on one side of [line]; the player draws their mirror images. Both halves stay on [GRID]. */
    private fun halfFigure(line: MirrorLine, rng: Random): List<Pt> {
        val base = when (line.kind) {
            MirrorLine.Kind.VERTICAL -> listOf(Pt(line.c2 / 2 - 2, 0), Pt(line.c2 / 2 - 1, 2), Pt(line.c2 / 2 - 2, 4))
            MirrorLine.Kind.HORIZONTAL -> listOf(Pt(0, line.c2 / 2 - 2), Pt(2, line.c2 / 2 - 1), Pt(4, line.c2 / 2 - 2))
            MirrorLine.Kind.DIAGONAL_UP -> listOf(Pt(3, 0), Pt(5, 2), Pt(6, 1))          // below y = x
            MirrorLine.Kind.DIAGONAL_DOWN -> listOf(Pt(0, 0), Pt(1, 2), Pt(2, 1))        // below y = -x + c, c in 4..6
        }
        return base.take(rng.pick(2..3))
    }
}
