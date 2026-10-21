package com.codetothepoint.mathgame.core.levels

import com.codetothepoint.mathgame.core.curriculum.CurriculumLoader
import com.codetothepoint.mathgame.core.engines.Fraction
import com.codetothepoint.mathgame.core.engines.Pt
import com.codetothepoint.mathgame.core.engines.Stroke
import com.codetothepoint.mathgame.core.engines.build.BuildEngine
import com.codetothepoint.mathgame.core.engines.build.BuildTarget
import com.codetothepoint.mathgame.core.engines.build.Constraints
import com.codetothepoint.mathgame.core.engines.build.StrokesConstruction
import com.codetothepoint.mathgame.core.engines.isPrime
import com.codetothepoint.mathgame.core.engines.manipulate.FractionBarState
import com.codetothepoint.mathgame.core.engines.manipulate.ManipulateEngine
import com.codetothepoint.mathgame.core.engines.manipulate.ManipulateTarget
import com.codetothepoint.mathgame.core.engines.manipulate.MatchState
import com.codetothepoint.mathgame.core.recipes.Band
import com.codetothepoint.mathgame.core.recipes.RecipeLoader
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The M2 gate (specs/build-plan.md): every recipe generates 1,000 items per band without an invalid one.
 * "Valid" means: the engine accepts the reference solution for the item, and the item respects its band's ranges.
 */
class GeneratorPropertyTest {
    private val curriculum = CurriculumLoader.fromResource()
    private val recipes = RecipeLoader.fromResources()
    private val perBand = 1000

    @Test
    fun everyRecipeHasAGenerator() {
        val missing = recipes.all.filter { !Generators.has(it) }.map { "${it.node}/${it.engine.key}" }
        assertTrue(missing.isEmpty(), "no generator for $missing")
    }

    @Test
    fun everyGeneratedItemIsSolvable() {
        val failures = mutableListOf<String>()
        for (recipe in recipes.all) {
            val gen = Generators.forRecipe(recipe)
            for (band in Band.entries) {
                val rng = Random("${recipe.node}/${recipe.engine.key}/$band".hashCode())
                repeat(perBand) { i ->
                    val rep = recipe.representations[i % recipe.representations.size]
                    val item = try { gen.generate(recipe.params(band), recipe.tolerance, band, rep, rng) } catch (e: Exception) {
                        failures += "${recipe.node}/${recipe.engine.key} $band #$i threw ${e::class.simpleName}: ${e.message}"; return@repeat
                    }
                    val ok = when (item) {
                        is GeneratedItem.Build -> BuildEngine.satisfied(item.target, Solutions.forBuild(item.target))
                        is GeneratedItem.Manipulate -> ManipulateEngine.satisfied(item.target, Solutions.forManipulate(item.target, item.initial)) &&
                            !ManipulateEngine.satisfied(item.target, item.initial) &&   // the item is not already done
                            playerHalfSolves(item)                                     // the given instrument stays as generated (review #3)
                    }
                    if (!ok) failures += "${recipe.node}/${recipe.engine.key} $band #$i unsolvable: $item"
                    if (item is GeneratedItem.Build) (Solutions.forBuild(item.target) as? StrokesConstruction)?.let { s ->
                        offGrid(s)?.let { failures += "${recipe.node}/${recipe.engine.key} $band #$i solution leaves the 7×7 grid at $it: ${item.target}" }
                    }
                    rangeCheck(recipe.node, band, item)?.let { failures += "${recipe.node}/${recipe.engine.key} $band #$i $it" }
                }
            }
        }
        if (failures.isNotEmpty()) fail(failures.take(25).joinToString("\n") + if (failures.size > 25) "\n… ${failures.size} failures" else "")
    }

    /** The first point of any stroke outside [Generators.GRID], or null when the whole construction fits (engine-build §10). */
    private fun offGrid(c: StrokesConstruction): Pt? = c.strokes.flatMap { s ->
        when (s) {
            is Stroke.Segment -> listOf(s.a, s.b)
            is Stroke.Ray -> listOf(s.from, s.through)
            is Stroke.Line -> listOf(s.a, s.b)
            is Stroke.Point -> listOf(s.at)
        }
    }.firstOrNull { it.x !in Generators.GRID || it.y !in Generators.GRID }

    @Test
    fun everyNamedShapeSolutionFitsTheGridAndSatisfiesItsConstraint() {
        for (name in Constraints.ALL) {
            val target = BuildTarget.ShapeConstraint(name)
            val solution = Solutions.forBuild(target) as StrokesConstruction
            assertNull(offGrid(solution), "$name leaves the 7×7 grid: $solution")
            assertTrue(BuildEngine.satisfied(target, solution), "$name reference solution is rejected: $solution")
        }
    }

    @Test
    fun everyAngleOnTheGridIsReachableWithinTheTolerance() {
        // The reference solution for an angle target is two grid rays; the grid must reach every angle the build recipe can ask for (5°–175°, ±2°).
        for (degrees in 5..175) {
            val target = BuildTarget.Angle(degrees, toleranceDegrees = 2)
            val solution = Solutions.forBuild(target) as StrokesConstruction
            assertNull(offGrid(solution), "$degrees° leaves the 7×7 grid: $solution")
            assertTrue(BuildEngine.satisfied(target, solution), "$degrees° is not reachable on the grid within ±2°: $solution")
        }
    }

    /** Band-specific expectations from the engine specs' worked examples (§10). Null when fine. */
    private fun rangeCheck(node: String, band: Band, item: GeneratedItem): String? {
        val t = (item as? GeneratedItem.Build)?.target
        val m = (item as? GeneratedItem.Manipulate)?.target
        return when (node) {
            "w04.nbt.multiply-multidigit" -> (t as BuildTarget.Product).let {
                if (maxOf(it.a, it.b) > 99) "a side over the 99-tile cap (engine-build §13): $it"
                else when (band) {
                    Band.LOW -> if (it.a !in 12..49 || it.b !in 2..5) "out of low range: $it" else null
                    Band.MID -> if (!(it.a in 11..99 && it.b in 2..9)) "out of mid range: $it" else null
                    Band.HIGH -> if (it.a !in 11..99 || it.b !in 11..29) "out of high range: $it" else null
                }
            }
            "w04.nbt.divide-by-one-digit" -> (t as BuildTarget.Quotient).let {
                if (band == Band.LOW && it.remainder != 0) "low band must divide evenly: $it"
                else if (it.quotient < 1) "quotient under 1: $it"
                else if (it.quotient > 99 || it.divisor > 99) "tiles kit shows at most 99 on a side: $it" else null
            }
            "w04.oa.factors-multiples-primes" -> (t as BuildTarget.FactorPairs).let {
                if (band == Band.LOW && it.area !in 6..30) "out of range: $it" else null
            }
            "w04.nbt.round-multidigit" -> (m as ManipulateTarget.Nearest).let {
                if (it.number < 0 || it.rounded < 0) "negative: $it" else if (band == Band.LOW && it.place != 10L) "low rounds to tens: $it" else null
            }
            "w04.md.measure-angles" -> {
                val deg = (t as? BuildTarget.Angle)?.degrees ?: (m as ManipulateTarget.AngleValue).degrees
                val tol = (t as? BuildTarget.Angle)?.toleranceDegrees ?: (m as ManipulateTarget.AngleValue).toleranceDegrees
                if (band == Band.LOW && deg % 10 != 0) "low band needs multiples of 10: $deg"
                else if (t == null && band == Band.HIGH && tol != 1) "high band manipulate tolerance must be ±1: $tol"
                else if (t == null && band != Band.HIGH && tol != 2) "tolerance must be ±2: $tol" else null
            }
            "w04.md.angles-concept", "w04.md.additive-angles" -> {
                val tol = (m as? ManipulateTarget.AngleValue)?.toleranceDegrees ?: (m as ManipulateTarget.AngleUnknown).toleranceDegrees
                val degenerate = (m as? ManipulateTarget.AngleUnknown)?.let { if (it.unknown < 1 || it.known.any { k -> k < 1 }) "degenerate: $it" else null }
                if (tol != (if (band == Band.HIGH) 1 else 2)) "tolerance by band: got $tol at $band" else degenerate
            }
            "w04.g.line-symmetry" -> (t as? BuildTarget.SymmetricCompletion)?.let { sc ->
                val all = sc.given + sc.given.map { sc.line.reflect(it) }
                if (all.any { it.x !in Generators.GRID || it.y !in Generators.GRID }) "leaves the 7×7 grid: $all" else null
            }
            "w04.nf.add-sub-like-denominators" -> (m as ManipulateTarget.BarSum).let {
                if (it.value.num <= 0 || it.a.den != it.den || it.b.den != it.den) "degenerate sum: $it" else null
            }
            "w04.nf.fraction-equivalence" -> (m as ManipulateTarget.Equal).let {
                val bar = (item as GeneratedItem.Manipulate).initial as FractionBarState
                if (it.reference.withDenominator(it.targetDen) == null) "not expressible: $it"
                else if (it.reference.den != bar.initialDen) "reference lost the bar's denomination (${bar.initialDen}): $it" else null
            }
            "w04.nf.multiply-fraction-by-whole" -> (t as BuildTarget.FractionQuantity).let {
                if (it.each!!.den !in it.denominators) "each lost its denomination: $it"
                else if (it.value.den !in it.denominators) "total lost its denomination: $it" else null
            }
            "w04.nf.decompose-fractions" -> when (item) {
                is GeneratedItem.Build -> (t as BuildTarget.TwoWays).let {
                    if (it.value < Fraction.of(2, it.denominators[0])) "too small for two ways: $it"
                    else if (it.value.den !in it.denominators) "value lost its denomination: $it" else null
                }
                is GeneratedItem.Manipulate -> (m as ManipulateTarget.Partition).let { if (it.value.withDenominator(it.den)!!.num < it.parts) "fewer eighths than parts: $it" else null }
            }
            "w04.oa.patterns-and-rules" -> (t as BuildTarget.Sequence).let {
                val diffs = it.terms.zipWithNext().map { (a, b) -> b - a }
                if (diffs.any { d -> d == 0 } || (diffs.any { d -> d > 0 } && diffs.any { d -> d < 0 })) "not monotonic: $it"
                else if (it.terms.any { v -> v > 5000 || v < 0 }) "out of range: $it" else null
            }
            else -> null
        }
    }

    /** For a match item, only the player's instrument may change; the given one stays exactly as generated. */
    private fun playerHalfSolves(item: GeneratedItem.Manipulate): Boolean {
        val t = item.target as? ManipulateTarget.Match ?: return true
        val initial = item.initial as MatchState
        val solved = Solutions.forManipulate(t, initial) as MatchState
        val played = if (t.givenOnGrid) MatchState(solved.line, initial.grid) else MatchState(initial.line, solved.grid)
        return ManipulateEngine.satisfied(t, played)
    }

    @Test
    fun everyRecipeParameterIsReadByItsGenerator() {
        for (r in recipes.all) {
            val allowed = Generators.KEYS[r.key] ?: fail("no key allow-list for ${r.node}/${r.engine.key}")
            for (band in Band.entries) {
                val extra = r.params(band).keys() - allowed
                assertTrue(extra.isEmpty(), "${r.node}/${r.engine.key} $band declares parameters no generator reads: $extra")
            }
        }
    }

    @Test
    fun primesAppearInFactorLevels() {
        val recipe = recipes.all.first { it.node == "w04.oa.factors-multiples-primes" }
        val gen = Generators.forRecipe(recipe)
        val rng = Random(7)
        val areas = List(400) { (gen.generate(recipe.params(Band.MID), recipe.tolerance, Band.MID, recipe.representations[0], rng) as GeneratedItem.Build).target as BuildTarget.FactorPairs }
        val primes = areas.count { isPrime(it.area) }
        assertTrue(primes in 50..150, "expected roughly a quarter primes, got $primes of 400")
    }

    @Test
    fun recipeNodesAllExist() {
        for (r in recipes.all) assertTrue(r.node in curriculum.nodes, r.node)
    }
}
