package com.codetothepoint.mathgame.core.mastery

import com.codetothepoint.mathgame.core.curriculum.Curriculum
import com.codetothepoint.mathgame.core.curriculum.Engine
import com.codetothepoint.mathgame.core.curriculum.Representation
import com.codetothepoint.mathgame.core.curriculum.SkillNode

/** What the recipes can serve, for revisit delivery (§7.2, §7.3). The level layer supplies one backed by its recipes. */
interface RevisitHosting {
    /** Representations any recipe for the node serves; null means all of the node's. */
    fun servable(nodeId: String): Set<Representation>?
    /** Whether this engine's recipe for the node serves the representation. */
    fun canHost(nodeId: String, engine: Engine, rep: Representation): Boolean

    companion object {
        val ALL: RevisitHosting = object : RevisitHosting {
            override fun servable(nodeId: String): Set<Representation>? = null
            override fun canHost(nodeId: String, engine: Engine, rep: Representation) = true
        }
    }
}

/** Tuning knobs (specs/mastery.md §13). Values must match tools/mastery_ref.py. */
object Knobs {
    const val ALPHA_CORE = 0.15
    const val ALPHA_REVISIT = 0.20
    const val MISS_DAMPING = 0.5
    const val SLOW_FACTOR = 3
    const val RECENT_N = 10
    const val HINT_RATE_TWO_STAR = 0.3
    const val REP_MIN_ITEMS = 3
    const val STAR_HYSTERESIS = 0.15
    const val STAR_HYSTERESIS_THREE = 0.10
    const val ALPHA_REACH = 0.08
    const val INTERLEAVE_MAX = 0.2
    const val REVISIT_SET_SIZE = 5
    const val REVISIT_PASS_CORRECT = 4
}

/**
 * The mastery model (specs/mastery.md): pure rules over [Profile]. A faithful port of
 * tools/mastery_ref.py; the two must agree number for number (ADR 0005).
 */
class MasteryModel(val curriculum: Curriculum) {

    // ---- Profiles and starting world (§6.3) ----

    fun newProfile(startingWorld: Int): Profile {
        val p = Profile(startingWorld, curriculum.nodes.keys.associateWith { NodeState() })
        setStartingWorld(p, startingWorld)
        return p
    }

    fun setStartingWorld(p: Profile, world: Int) {
        p.startingWorld = world
        for (nd in curriculum.nodes.values) {
            val s = p.nodes.getValue(nd.id)
            if (nd.world < world && s.items == 0) {
                s.assumed = true
                s.score = nd.mastery.unlock
                s.stars = 0
            } else if (nd.world >= world && s.assumed) {
                s.assumed = false
                s.score = 0.0
                s.stars = 0
            }
        }
    }

    // ---- Evidence and score (§4) ----

    fun evidence(nd: SkillNode, item: ItemResult): Double {
        if (!item.correct) return 0.0
        var e = if (item.firstTry && item.hints == 0) 1.0 else if (item.hints == 0) 0.6 else 0.4
        val fm = nd.mastery.fluencyMs
        if (fm != null && item.responseMs != null && item.responseMs > Knobs.SLOW_FACTOR * fm) e = minOf(e, 0.8)
        return e
    }

    private fun move(score: Double, e: Double, alphaIn: Double): Double {
        var alpha = alphaIn
        if (e < score) alpha *= Knobs.MISS_DAMPING
        return score + alpha * (e - score)
    }

    fun applyItem(p: Profile, item: ItemResult) {
        val nd = curriculum[item.nodeId]
        val s = p.nodes.getValue(item.nodeId)
        val e = evidence(nd, item)

        // Bookkeeping common to every kind of item.
        s.assumed = false
        s.items += 1
        s.recent.addLast(item)
        while (s.recent.size > Knobs.RECENT_N) s.recent.removeFirst()
        s.repItems[item.representation] = (s.repItems[item.representation] ?: 0) + 1
        s.lastSeen = item.at
        item.strategy?.takeIf { it.isNotBlank() }?.let { s.strategies += it }
        if (item.correct && item.hints == 0 && item.responseMs != null) {
            s.cleanTimes.addLast(item.responseMs)
            while (s.cleanTimes.size > Knobs.RECENT_N) s.cleanTimes.removeFirst()
            if (s.cleanTimes.size >= 3) s.fluencyMs = median(s.cleanTimes)
        }

        if (item.kind == ItemKind.REVISIT) {
            revisitItem(nd, s, item)
            return
        }

        s.score = move(s.score, e, Knobs.ALPHA_CORE)

        // Reach credit (§6.2): a clean item credits unmet direct prerequisites.
        if (item.kind == ItemKind.REACH && e == 1.0) {
            for (pid in nd.prerequisites) {
                if (!isMet(p, pid)) {
                    val ps = p.nodes.getValue(pid)
                    ps.score = ps.score + Knobs.ALPHA_REACH * (1 - ps.score)
                }
            }
        }
    }

    fun finishLevel(p: Profile, level: LevelResult) {
        val s = p.nodes.getValue(level.nodeId)
        if (level.completed) {
            s.levelsCompleted += 1
            s.lastLevelRepresentation = level.representation
            if (level.observesStrategies) s.strategyObservable = true
            s.secretGoals += level.secretGoalsHit
        }
        val touched = linkedSetOf(level.nodeId) + level.items.map { it.nodeId }
        for (nid in touched) recomputeStars(p, nid, level.endedAt)
    }

    // ---- Stars (§5) ----

    fun recomputeStars(p: Profile, nid: String, now: Long) {
        val nd = curriculum[nid]
        val s = p.nodes.getValue(nid)
        val m = nd.mastery
        var stars = s.stars
        if (s.levelsCompleted >= 1) stars = maxOf(stars, 1)

        val recent = s.recent
        val hintRate = if (recent.isEmpty()) 1.0 else recent.count { it.hints > 0 }.toDouble() / recent.size
        val repsOk = s.repItems.values.count { it >= Knobs.REP_MIN_ITEMS } >= 2
        val stratOk = nd.strategies.isEmpty() || !s.strategyObservable || s.strategies.isNotEmpty()
        val twoOk = s.score >= m.twoStar && hintRate <= Knobs.HINT_RATE_TWO_STAR && repsOk && stratOk

        if (stars >= 2 && s.score < m.twoStar - Knobs.STAR_HYSTERESIS) {
            stars = 1
        } else if (stars == 1 && twoOk) {
            stars = 2
            if (s.twoStarAt == null) {
                s.twoStarAt = now
                s.revisit = RevisitState(dueAt = now + m.revisitDays[0] * DAY_MS)
            }
        }

        if (stars == 3 && s.score < m.threeStar - Knobs.STAR_HYSTERESIS_THREE) stars = 2
        if (stars == 2) {
            val zeroHints = recent.all { it.hints == 0 }
            val fm = m.fluencyMs
            val flOk = fm == null || (s.fluencyMs != null && s.fluencyMs!! <= fm)
            val rv = s.revisit
            val rvOk = rv != null && rv.passed >= 1 && rv.lastSetPassed == true
            if (s.score >= m.threeStar && zeroHints && flOk && rvOk) {
                stars = 3
                if (s.threeStarAt == null) s.threeStarAt = now
            }
        }
        s.stars = stars
    }

    // ---- Availability (§6) ----

    fun isMet(p: Profile, nid: String): Boolean {
        val s = p.nodes.getValue(nid)
        return s.assumed || s.score >= curriculum[nid].mastery.unlock
    }

    fun availability(p: Profile, nid: String): Availability {
        val nd = curriculum[nid]
        val s = p.nodes.getValue(nid)
        if (s.levelsCompleted >= 1) return Availability.AVAILABLE
        if (nd.prerequisites.all { isMet(p, it) }) return Availability.AVAILABLE
        if (nd.prerequisites.all { isMet(p, it) || availableNoRecursion(p, it) }) return Availability.REACH
        return Availability.LOCKED
    }

    private fun availableNoRecursion(p: Profile, nid: String): Boolean {
        val nd = curriculum[nid]
        val s = p.nodes.getValue(nid)
        return s.levelsCompleted >= 1 || nd.prerequisites.all { isMet(p, it) }
    }

    // ---- Revisits (§7) ----

    fun isDue(p: Profile, nid: String, now: Long): Boolean {
        val rv = p.nodes.getValue(nid).revisit
        return rv != null && now >= rv.dueAt
    }

    /**
     * The least-drilled representation among those some recipe for the node serves (§7.3); ties go to
     * the one not used in the last level. Null when nothing is servable, so no set can start.
     */
    fun chooseTransferRepresentation(p: Profile, nid: String, servable: Set<Representation>? = null): Representation? {
        val nd = curriculum[nid]
        val s = p.nodes.getValue(nid)
        val allowed = nd.representations.filter { servable == null || it in servable }
        if (allowed.isEmpty()) return null
        return allowed.sortedWith(compareBy<Representation>({ s.repItems[it] ?: 0 }, { it == s.lastLevelRepresentation })).first()
    }

    /**
     * Nodes due for a revisit that [engine] can host, most overdue first, with the representation
     * for the current set (§7.2). [hosting] says which representations recipes serve; its filters
     * run before the interleave cap, so an un-hostable node never takes a slot. With [coreItems] > 0
     * the list is capped at the interleave limit.
     */
    fun dueRevisits(p: Profile, now: Long, engine: Engine, exclude: String? = null, coreItems: Int = 0, hosting: RevisitHosting = RevisitHosting.ALL): List<Pair<String, Representation>> {
        val out = mutableListOf<Triple<Long, String, Representation>>()
        for (nd in curriculum.nodes.values) {
            if (nd.id == exclude || engine !in nd.engines || !isDue(p, nd.id, now)) continue
            val rv = p.nodes.getValue(nd.id).revisit!!
            if (rv.representation == null) rv.representation = chooseTransferRepresentation(p, nd.id, hosting.servable(nd.id))
            val rep = rv.representation ?: continue
            if (rep !in nd.representations || !hosting.canHost(nd.id, engine, rep)) continue
            out += Triple(rv.dueAt, nd.id, rep)
        }
        out.sortWith(compareBy({ it.first }, { it.second }))
        val limit = if (coreItems > 0) maxOf(1, (Knobs.INTERLEAVE_MAX * coreItems).toInt()) else out.size
        return out.take(limit).map { it.second to it.third }
    }

    private fun revisitItem(nd: SkillNode, s: NodeState, item: ItemResult) {
        val rv = s.revisit ?: return
        rv.served += 1
        if (item.correct) rv.correct += 1
        rv.hints += item.hints
        if (rv.served < Knobs.REVISIT_SET_SIZE) return
        val passed = rv.correct >= Knobs.REVISIT_PASS_CORRECT && rv.hints == 0
        val days = nd.mastery.revisitDays
        if (passed) {
            rv.passed += 1
            rv.intervalIndex = minOf(rv.intervalIndex + 1, days.size - 1)
            s.score = s.score + Knobs.ALPHA_REVISIT * (1 - s.score)
        } else {
            if (s.stars == 3) s.stars = 2
            rv.intervalIndex = 0
            s.score = s.score + Knobs.ALPHA_REVISIT * Knobs.MISS_DAMPING * (rv.correct.toDouble() / Knobs.REVISIT_SET_SIZE - s.score)
        }
        rv.lastSetPassed = passed
        rv.history += RevisitOutcome(item.at, passed)
        rv.dueAt = item.at + days[rv.intervalIndex] * DAY_MS
        rv.served = 0
        rv.correct = 0
        rv.hints = 0
        rv.representation = null
    }

    // ---- Representation rotation (§8) ----

    /** Representation for level [k] (0-based) of [nid], skipping any the engine cannot host. */
    fun representationForLevel(nid: String, k: Int, supported: Set<Representation>): Representation {
        val reps = curriculum[nid].representations.filter { it in supported }
        require(reps.isNotEmpty()) { "Engine supports none of ${curriculum[nid].representations} for $nid" }
        return reps[k % reps.size]
    }

    // ---- Map state (§11) and milestones (§10) ----

    fun mapState(p: Profile, nid: String, now: Long): MapState {
        val s = p.nodes.getValue(nid)
        val state = when {
            s.assumed -> MapNodeState.ASSUMED
            else -> when (availability(p, nid)) {
                Availability.LOCKED -> MapNodeState.LOCKED
                Availability.REACH -> MapNodeState.REACH
                Availability.AVAILABLE -> MapNodeState.AVAILABLE
            }
        }
        return MapState(state, s.stars, isDue(p, nid, now))
    }

    fun worldStars(p: Profile, world: Int): Int = curriculum.nodesIn(world).sumOf { p.nodes.getValue(it.id).stars }

    fun worldComplete(p: Profile, world: Int): Boolean = curriculum.nodesIn(world).all { p.nodes.getValue(it.id).stars >= 2 }

    fun secretGoalCount(p: Profile, world: Int? = null): Int =
        curriculum.nodes.values.filter { world == null || it.world == world }.sumOf { p.nodes.getValue(it.id).secretGoals.size }

    fun masteredCount(p: Profile): Int = p.nodes.values.count { it.stars == 3 }

    private fun median(values: Collection<Int>): Int {
        val sorted = values.sorted()
        val n = sorted.size
        return if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2
    }
}
