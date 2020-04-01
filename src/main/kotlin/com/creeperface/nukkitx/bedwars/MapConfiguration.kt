package com.creeperface.nukkitx.bedwars

import cn.nukkit.math.Vector3
import cn.nukkit.utils.Config
import cn.nukkit.utils.ConfigSection
import cn.nukkit.utils.DyeColor
import cn.nukkit.utils.TextFormat
import java.io.File

class MapConfiguration(
        val name: String,
        val bronze: MutableSet<Vector3> = mutableSetOf(),
        val iron: MutableSet<Vector3> = mutableSetOf(),
        val gold: MutableSet<Vector3> = mutableSetOf(),
        val teams: Array<TeamData>
) {

    data class TeamData(
            var name: String? = null,
            var color: DyeColor? = null,
            var chatColor: TextFormat? = null,
            var spawn: Vector3? = null,
            var villager: Vector3? = null,
            var bed1: Vector3? = null,
            var bed2: Vector3? = null
    )

    fun save(plugin: BedWarsHelper) {
        val dir = File(plugin.dataFolder, "arenas")
        dir.mkdirs()

        var fileName = name
        var i = 1
        var file: File

        while(true) {
            file = File(dir, "$fileName.yml")

            if(file.exists()) {
                fileName = "$name-${i++}"
            } else {
                break
            }
        }

        fun writeVector3(vec: Vector3): ConfigSection {
            val section = ConfigSection()

            section.set("x", vec.x)
            section.set("y", vec.y)
            section.set("z", vec.z)

            return section
        }

        val cfg = Config(file, Config.YAML)

        cfg.rootSection.let { section ->
            section.set("name", name)
            section.set("bronze", bronze.map { writeVector3(it) })
            section.set("iron", iron.map { writeVector3(it) })
            section.set("gold", gold.map { writeVector3(it) })

            section.set("teams", teams.map {team ->
                val sec = ConfigSection()
                sec.set("name", team.name)
                sec.set("color", team.color?.ordinal)
                sec.set("chat_color", team.chatColor?.ordinal)
                sec.set("spawn", writeVector3(team.spawn!!))
                sec.set("villager", writeVector3(team.villager!!))
                sec.set("bed1", writeVector3(team.bed1!!))
                sec.set("bed2", writeVector3(team.bed2!!))

                sec
            })
        }

        cfg.save()
    }
}