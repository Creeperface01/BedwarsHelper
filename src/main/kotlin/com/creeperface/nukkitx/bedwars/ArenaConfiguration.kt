package com.creeperface.nukkitx.bedwars

import cn.nukkit.math.Vector3
import cn.nukkit.utils.DyeColor
import cn.nukkit.utils.TextFormat

class ArenaConfiguration(
        var name: String,
        var timeLimit: Int,
        var startTime: Int,
        var endingTime: Int,
        var startPlayers: Int,
        var bronzeDropInterval: Int,
        var ironDropInterval: Int,
        var goldDropInterval: Int,
        var fastStartTime: Int,
        var fastStartPlayers: Int,
        var teamPlayers: Int,
        var maxPlayers: Int,
        var multiPlatform: Boolean,
        var lobby: Vector3
)