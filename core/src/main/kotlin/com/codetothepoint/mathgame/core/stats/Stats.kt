package com.codetothepoint.mathgame.core.stats

import com.codetothepoint.mathgame.core.curriculum.Curriculum
import com.codetothepoint.mathgame.core.curriculum.Representation
import com.codetothepoint.mathgame.core.mastery.DAY_MS
import com.codetothepoint.mathgame.core.mastery.MapNodeState
import com.codetothepoint.mathgame.core.mastery.MasteryModel
import com.codetothepoint.mathgame.core.mastery.NodeState
import com.codetothepoint.mathgame.core.mastery.Profile

/** One play session, as the app records it (specs/parent-portal.md §9, ADR 0004). */
data class SessionRecord(val world: Int, val startedAt: Long, val endedAt: Long, val levelsCompleted: Int) {
    val minutes: Int get() = ((endedAt - startedAt) / 60_000L).toInt()
}

/** Portal overview (specs/parent-portal.md §6.1). Reward counts are composed by the portal itself. */
data class Overview(
    val currentWorld: Int?,
    val currentWorldSitesWithFlags: Int,
    val currentWorldSites: Int,
    val sessionsThisWeek: Int,
    val minutesThisWeek: Int,
    val jobsThisWeek: Int,
    val flags: Int,
    val owned: Int,
    val due: Int,
    val revisitsPassedThisMonth: Int,
    val secrets: Int,
)

data class RegionRow(val domain: String, val domainName: String, val assumed: Int, val locked: Int, val reach: Int, val flags0: Int, val flags1: Int, val flags2: Int, val flags3: Int)

/** One visited world (specs/parent-portal.md §6.2). */
data class WorldRow(val world: Int, val name: String, val regions: List<RegionRow>, val flags: Int, val maxFlags: Int, val finished: Boolean)

/** How it's going, in the portal's words (§6.3): never the number. */
enum class Progress { BUILDING, GOT_IT, OWNS_IT }

enum class HintsLately { NONE, SOME, LOTS }

/** One site (specs/parent-portal.md §6.3). */
data class SiteDetail(
    val id: String,
    val title: String,
    val description: String,
    val stars: Int,
    val progress: Progress,
    val hintsLately: HintsLately,
    val waysPracticed: List<Representation>,
    val strategiesSeen: List<String>,
    val lastPlayed: Long?,
)

/** Pure stat computations for the Parent Portal. Nothing here is shown to the player. */
class Stats(private val model: MasteryModel) {
    private val c: Curriculum get() = model.curriculum

    fun overview(p: Profile, sessions: List<SessionRecord>, now: Long): Overview {
        val weekAgo = now - 7 * DAY_MS
        val monthAgo = now - 30 * DAY_MS
        val thisWeek = sessions.filter { it.endedAt >= weekAgo }
        val current = sessions.maxByOrNull { it.endedAt }?.world
        val currentNodes = current?.let { c.nodesIn(it) }.orEmpty()
        return Overview(
            currentWorld = current,
            currentWorldSitesWithFlags = currentNodes.count { p.nodes.getValue(it.id).stars >= 1 },
            currentWorldSites = currentNodes.size,
            sessionsThisWeek = thisWeek.size,
            minutesThisWeek = thisWeek.sumOf { it.minutes },
            jobsThisWeek = thisWeek.sumOf { it.levelsCompleted },
            flags = p.nodes.values.sumOf { it.stars },
            owned = model.masteredCount(p),
            due = c.nodes.keys.count { model.isDue(p, it, now) },
            revisitsPassedThisMonth = p.nodes.values.sumOf { s -> s.revisit?.history.orEmpty().count { it.passed && it.at >= monthAgo } },
            secrets = model.secretGoalCount(p),
        )
    }

    /** Rows for every world the player has evidence in, in world order. */
    fun worlds(p: Profile, now: Long): List<WorldRow> =
        c.worlds.filter { w -> c.nodesIn(w.world).any { p.nodes.getValue(it.id).items > 0 } }.map { w -> world(p, w.world, now) }

    fun world(p: Profile, world: Int, now: Long): WorldRow {
        val nodes = c.nodesIn(world)
        val regions = nodes.groupBy { it.domain }.map { (domain, group) ->
            val states = group.map { model.mapState(p, it.id, now) }
            RegionRow(
                domain = domain,
                domainName = group.first().domainName,
                assumed = states.count { it.state == MapNodeState.ASSUMED },
                locked = states.count { it.state == MapNodeState.LOCKED },
                reach = states.count { it.state == MapNodeState.REACH },
                flags0 = states.count { it.state == MapNodeState.AVAILABLE && it.stars == 0 },
                flags1 = states.count { it.state == MapNodeState.AVAILABLE && it.stars == 1 },
                flags2 = states.count { it.state == MapNodeState.AVAILABLE && it.stars == 2 },
                flags3 = states.count { it.state == MapNodeState.AVAILABLE && it.stars == 3 },
            )
        }
        return WorldRow(world, c.world(world).name, regions, model.worldStars(p, world), nodes.size * 3, model.worldComplete(p, world))
    }

    fun site(p: Profile, nid: String): SiteDetail {
        val nd = c[nid]
        val s = p.nodes.getValue(nid)
        return SiteDetail(
            id = nid,
            title = nd.title,
            description = nd.description,
            stars = s.stars,
            progress = progress(s),
            hintsLately = hintsLately(s),
            waysPracticed = nd.representations.filter { (s.repItems[it] ?: 0) >= 3 },
            strategiesSeen = nd.strategies.filter { it in s.strategies },
            lastPlayed = s.lastSeen,
        )
    }

    /** Follows the flags, which already carry the star hysteresis (parent-portal §6.3). */
    private fun progress(s: NodeState): Progress = when (s.stars) {
        3 -> Progress.OWNS_IT
        2 -> Progress.GOT_IT
        else -> Progress.BUILDING
    }

    private fun hintsLately(s: NodeState): HintsLately {
        if (s.recent.isEmpty()) return HintsLately.NONE
        val rate = s.recent.count { it.hints > 0 }.toDouble() / s.recent.size
        return when {
            rate == 0.0 -> HintsLately.NONE
            rate <= 0.3 -> HintsLately.SOME
            else -> HintsLately.LOTS
        }
    }
}
