package com.codetothepoint.mathgame.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codetothepoint.mathgame.core.curriculum.CurriculumLoader

/**
 * Development launcher (specs/build-plan.md M0). Lists the screens that milestones M3 and M4 add.
 * Not a player-facing screen; it goes away when the map arrives in M5.
 */
class DevLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val curriculum = assets.open("curriculum/curriculum.json").bufferedReader().use { CurriculumLoader.fromJson(it.readText()) }
        setContent { DevLauncher(nodeCount = curriculum.nodes.size, worldFourCount = curriculum.nodesIn(4).size) }
    }
}

@Composable
private fun DevLauncher(nodeCount: Int, worldFourCount: Int) {
    Column(Modifier.padding(24.dp)) {
        Text("Math Game dev launcher")
        Text("Curriculum loaded: $nodeCount skills, $worldFourCount in world 4")
        Text("M3 adds build jobs here. M4 adds manipulate jobs.")
    }
}
