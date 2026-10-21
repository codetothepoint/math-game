package com.codetothepoint.mathgame.core.levels

/**
 * The three-rung hint ladder every engine uses (engine-build.md §6, engine-manipulate.md §6).
 * At most three hints per item; `hints` on the ItemResult counts granted requests.
 */
class HintLadder(private val rungs: List<String>) {
    init { require(rungs.size == 3) { "A hint ladder has exactly three rungs, got $rungs" } }

    var granted: Int = 0
        private set

    val exhausted: Boolean get() = granted >= rungs.size

    /** The next rung's name, or null when all three have been used. Only granted requests count. */
    fun request(): String? {
        if (exhausted) return null
        return rungs[granted++]
    }

    fun reset() { granted = 0 }
}
