package com.codetothepoint.mathgame.core.stats

import com.codetothepoint.mathgame.core.curriculum.CurriculumLoader
import com.codetothepoint.mathgame.core.curriculum.Engine
import com.codetothepoint.mathgame.core.curriculum.Representation.SYMBOLIC
import com.codetothepoint.mathgame.core.curriculum.Representation.VISUAL
import com.codetothepoint.mathgame.core.mastery.DAY_MS
import com.codetothepoint.mathgame.core.mastery.ItemResult
import com.codetothepoint.mathgame.core.mastery.LevelResult
import com.codetothepoint.mathgame.core.mastery.MasteryModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every stat in specs/parent-portal.md §6 computes from NodeState, the curriculum, and sessions (§9). */
class StatsTest {
    private val model = MasteryModel(CurriculumLoader.fromResource())
    private val stats = Stats(model)

    private fun play(p: com.codetothepoint.mathgame.core.mastery.Profile, nid: String, n: Int, rep: com.codetothepoint.mathgame.core.curriculum.Representation, at: Long, hints: Int = 0, strategy: String? = null) {
        val items = (1..n).map { ItemResult(nid, rep, Engine.BUILD, true, hints = hints, responseMs = 1200, strategy = strategy, at = at) }
        items.forEach { model.applyItem(p, it) }
        model.finishLevel(p, LevelResult(nid, Engine.BUILD, rep, true, items, observesStrategies = true, startedAt = at, endedAt = at))
    }

    @Test
    fun overviewCountsTheWeekAndTheWorld() {
        val p = model.newProfile(4)
        val now = 40 * DAY_MS
        val nid = "w04.nbt.multiply-multidigit"
        play(p, nid, 6, SYMBOLIC, now - DAY_MS, strategy = "area-model")
        play(p, nid, 4, VISUAL, now - DAY_MS)
        val sessions = listOf(
            SessionRecord(4, now - 10 * DAY_MS, now - 10 * DAY_MS + 600_000, 2),  // last week: excluded
            SessionRecord(4, now - DAY_MS, now - DAY_MS + 900_000, 2),
        )
        val o = stats.overview(p, sessions, now)
        assertEquals(4, o.currentWorld)
        assertEquals(1, o.currentWorldSitesWithFlags)
        assertEquals(28, o.currentWorldSites)
        assertEquals(1, o.sessionsThisWeek)
        assertEquals(15, o.minutesThisWeek)
        assertEquals(2, o.jobsThisWeek)
        assertEquals(2, o.flags)
        assertEquals(0, o.owned)
        assertEquals(0, o.due)
        assertEquals(1, stats.worlds(p, now).size)
    }

    @Test
    fun worldRowsSplitSitesByStateAndRegion() {
        val p = model.newProfile(4)
        val now = 0L
        val row = stats.world(p, 4, now)
        assertEquals("Grade 4", row.name)
        assertEquals(5, row.regions.size)
        assertEquals(28 * 3, row.maxFlags)
        assertEquals(0, row.flags)
        assertTrue(!row.finished)
        val total = row.regions.sumOf { it.assumed + it.locked + it.reach + it.flags0 + it.flags1 + it.flags2 + it.flags3 }
        assertEquals(28, total)
        val w3 = stats.world(p, 3, now)
        assertEquals(28, w3.regions.sumOf { it.assumed })
    }

    @Test
    fun siteDetailUsesWordsNotNumbers() {
        val p = model.newProfile(4)
        val nid = "w04.nbt.multiply-multidigit"
        var d = stats.site(p, nid)
        assertEquals(Progress.BUILDING, d.progress)
        assertEquals(HintsLately.NONE, d.hintsLately)
        assertEquals(emptyList(), d.waysPracticed)
        play(p, nid, 9, SYMBOLIC, 1000L, strategy = "area-model")
        play(p, nid, 4, VISUAL, 2000L)
        d = stats.site(p, nid)
        assertEquals(2, d.stars)
        assertEquals(Progress.GOT_IT, d.progress)
        assertEquals(HintsLately.NONE, d.hintsLately)
        play(p, nid, 4, VISUAL, 3000L, hints = 1)     // 4 of the last 10 hinted; score stays above the demotion line
        d = stats.site(p, nid)
        assertEquals(Progress.GOT_IT, d.progress)
        assertEquals(HintsLately.LOTS, d.hintsLately)
        assertEquals(listOf(VISUAL, SYMBOLIC).sorted(), d.waysPracticed.sorted())
        assertEquals(listOf("area-model"), d.strategiesSeen)
        assertEquals(3000L, d.lastPlayed)
    }
}
