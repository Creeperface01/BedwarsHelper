package com.creeperface.nukkitx.bedwars

import cn.nukkit.Player

class MapConfigurationProfile(val player: Player, val mapConfiguration: MapConfiguration) {

    var team = 0
    var deleteStep: HistoryStep? = null
    var modified = false

    val history = mutableListOf<HistoryStep>()

    fun doStep(description: String, undoAction: () -> Unit) {
        history.add(HistoryStep(team, description, undoAction))
    }

    fun revert(step: Int) {
        val historyStep = history.removeAt(step)
        historyStep.undoAction()
    }

    class HistoryStep(
            val team: Int,
            val description: String,
            val undoAction: () -> Unit
    )
}