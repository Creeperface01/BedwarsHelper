package com.creeperface.nukkitx.bedwars

import cn.nukkit.Player
import cn.nukkit.block.BlockBed
import cn.nukkit.block.BlockID
import cn.nukkit.command.Command
import cn.nukkit.command.CommandSender
import cn.nukkit.event.EventHandler
import cn.nukkit.event.Listener
import cn.nukkit.event.player.PlayerFormRespondedEvent
import cn.nukkit.event.player.PlayerInteractEvent
import cn.nukkit.form.element.ElementButton
import cn.nukkit.form.element.ElementDropdown
import cn.nukkit.form.element.ElementInput
import cn.nukkit.form.element.ElementLabel
import cn.nukkit.form.response.FormResponseCustom
import cn.nukkit.form.response.FormResponseModal
import cn.nukkit.form.response.FormResponseSimple
import cn.nukkit.form.window.FormWindowCustom
import cn.nukkit.form.window.FormWindowModal
import cn.nukkit.form.window.FormWindowSimple
import cn.nukkit.item.Item
import cn.nukkit.item.ItemID
import cn.nukkit.level.particle.RedstoneParticle
import cn.nukkit.math.BlockFace
import cn.nukkit.math.Vector3
import cn.nukkit.plugin.PluginBase
import cn.nukkit.utils.DyeColor
import cn.nukkit.utils.TextFormat

class BedWarsHelper : PluginBase(), Listener {

    private val mapConfigurations = mutableMapOf<String, MapConfigurationProfile>()

    override fun onEnable() {
        this.server.pluginManager.registerEvents(this, this)

        this.server.scheduler.scheduleRepeatingTask(this, {
            mapConfigurations.values.forEach { profile ->
                if (!profile.player.isOnline) {
                    return@forEach
                }

                profile.mapConfiguration.teams[profile.team].name.let { name ->
                    profile.player.sendActionBar(TextFormat.GREEN.toString() + "Editing team " + TextFormat.YELLOW + name)
                }
            }
        }, 10)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) return false

        when (command.name.toLowerCase()) {
            "bwconfigure" -> {
                configureMap(sender)
            }
            "bwsave" -> {
                mapConfigurations[sender.name]?.let { profile ->
                    profile.mapConfiguration.save(this)
                    profile.modified = false
                    sender.sendMessage("${TextFormat.GREEN}Configuration saved")
                }
            }
            "bwhistory" -> {
                showHistory(sender)
            }
            "bwexit" -> {
                exit(sender)
            }
        }
        return true
    }

    private fun exit(player: Player) {
        val profile = mapConfigurations[player.name] ?: return

        if (checkTeam(profile.mapConfiguration.teams[profile.team], player)) {
            profile.mapConfiguration.save(this)
            mapConfigurations.remove(player.name)
            player.sendMessage("${TextFormat.GREEN}Configuration saved")
        }
    }

    private fun configureMap(player: Player) {
        val window = FormWindowCustom("Configure a new map")

        window.addElement(ElementInput("Name of the map"))
        window.addElement(ElementInput("Number of teams"))

        player.showFormWindow(window, MAP_BASE_ID)
    }

    private fun showTeamWindow(profile: MapConfigurationProfile, errors: List<String> = emptyList()) {
        val teamWindow = FormWindowCustom("Team ${profile.team} configuration")
        val team = profile.mapConfiguration.teams[profile.team]

        errors.forEach { error ->
            teamWindow.addElement(ElementLabel(error))
        }

        teamWindow.addElement(ElementInput("Team name", "", team.name ?: ""))
        teamWindow.addElement(ElementInput("Team color", "blue, cyan, magenta, brown", team.color?.getName() ?: ""))
        teamWindow.addElement(ElementInput("Team chat color", "blue, red, §9, &c", team.chatColor?.name?.toLowerCase()
                ?: ""))

        profile.player.showFormWindow(teamWindow, MAP_TEAM_ID)
    }

    private fun checkTeam(team: MapConfiguration.TeamData, player: Player): Boolean {
        var error = false

        if (team.bed1 == null || team.bed2 == null) {
            player.sendMessage("${TextFormat.RED}Bed position is not set")
            error = true
        }

        if (team.spawn == null) {
            player.sendMessage("${TextFormat.RED}Player spawn position is not set")
            error = true
        }

        if (team.villager == null) {
            player.sendMessage("${TextFormat.RED}Villager position is not set")
            error = true
        }

        return !error
    }

    @EventHandler
    fun onInteract(e: PlayerInteractEvent) {
        val p = e.player
        val b = e.block

        val profile = mapConfigurations[p.name] ?: return
        val item = e.item
        val map = profile.mapConfiguration
        val team = map.teams[profile.team]

        e.setCancelled()

        if (e.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            p.sendMessage("${b.id}:${b.damage}")

            when (item.customName) {
                "Bronze spawn" -> {
                    b.getSide(e.face).let { side ->
                        if (map.bronze.add(side)) {
                            profile.doStep("bronze - $side") {
                                profile.modified = true
                                map.bronze.remove(side)
                            }
                            profile.modified = true
                            showParticle(p, side)
                            p.sendMessage("${TextFormat.GREEN}Added")
                        }
                    }
                }
                "Iron spawn" -> {
                    b.getSide(e.face).let { side ->
                        if (map.iron.add(side)) {
                            profile.doStep("iron - $side") {
                                profile.modified = true
                                map.iron.remove(side)
                            }
                            profile.modified = true
                            showParticle(p, side)
                            p.sendMessage("${TextFormat.GREEN}Added")
                        }
                    }
                }
                "Gold spawn" -> {
                    b.getSide(e.face).let { side ->
                        if (map.gold.add(side)) {
                            profile.doStep("gold - $side") {
                                profile.modified = true
                                map.gold.remove(side)
                            }
                            profile.modified = true
                            showParticle(p, side)
                            p.sendMessage("${TextFormat.GREEN}Added")
                        }
                    }
                }
                "Players spawn" -> {
                    b.getSide(e.face).let { side ->
                        team.spawn = side
                        profile.modified = true
                        showParticle(p, side)
                        p.sendMessage("${TextFormat.GREEN}Position set")
                    }
                }
                "Team bed" -> {
                    var secondPart: Vector3? = null

                    if (b is BlockBed) {
                        for (face in BlockFace.Plane.HORIZONTAL) {
                            (b.getSide(face) as? BlockBed)?.let { bed ->
                                secondPart = bed
                            }

                            if (secondPart != null) {
                                break
                            }
                        }
                    }

                    if (secondPart == null) {
                        p.sendMessage("${TextFormat.RED}Bed not found")
                    } else {
                        team.bed1 = b
                        team.bed2 = secondPart
                        profile.modified = true
                        p.sendMessage("${TextFormat.GREEN}Position set")
                    }
                }
                "Villager Position" -> {
                    b.getSide(e.face).let { side ->
                        team.villager = side
                        profile.modified = true
                        showParticle(p, side)
                        p.sendMessage("${TextFormat.GREEN}Position set")
                    }
                }
            }
        } else if (e.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR) {
            when (item.customName) {
                "${TextFormat.YELLOW}Next team" -> {
                    if (checkTeam(team, p)) {
                        if (++profile.team < map.teams.size) {
                            showTeamWindow(profile)
                        } else {
                            profile.team = 0
                            p.sendMessage("${TextFormat.YELLOW}All teams have been configured. Now you can run /bwsave to save your configuration")
                        }
                    }
                }
                "${TextFormat.YELLOW}Edit team" -> {
                    showEditTeam(p)
                }
            }
        }
    }

    @EventHandler
    fun onFormSubmit(e: PlayerFormRespondedEvent) {
        val p = e.player
        val response = e.response

        when (e.formID) {
            ARENA_BASE_ID -> {

            }
            MAP_BASE_ID -> {
                if (response !is FormResponseCustom) return

                val profile = MapConfigurationProfile(p, MapConfiguration(
                        name = response.getInputResponse(0),
                        teams = Array(response.getInputResponse(1).toInt()) { MapConfiguration.TeamData() }
                ))

                profile.team = 0

                p.inventory.setItem(0, Item.get(ItemID.BRICK).setCustomName("Bronze spawn"))
                p.inventory.setItem(1, Item.get(ItemID.IRON_INGOT).setCustomName("Iron spawn"))
                p.inventory.setItem(2, Item.get(ItemID.GOLD_INGOT).setCustomName("Gold spawn"))
                p.inventory.setItem(3, Item.get(ItemID.WOODEN_SHOVEL).setCustomName("Players spawn"))
                p.inventory.setItem(4, Item.get(ItemID.WOODEN_AXE).setCustomName("Team bed"))
                p.inventory.setItem(5, Item.get(ItemID.WOODEN_SWORD).setCustomName("Villager Position"))
                p.inventory.clear(6)
                p.inventory.setItem(7, Item.get(ItemID.GOLDEN_SWORD).setCustomName("${TextFormat.YELLOW}Edit team"))
                p.inventory.setItem(8, Item.get(ItemID.DIAMOND_SWORD).setCustomName("${TextFormat.YELLOW}Next team"))
                p.inventory.setHeldItemIndex(0, true)

                showTeamWindow(profile)

                mapConfigurations[p.name] = profile
            }
            MAP_TEAM_ID -> {
                if (response !is FormResponseCustom) return
                val profile = mapConfigurations[p.name] ?: return

                val team = profile.mapConfiguration.teams[profile.team]

                val errors = mutableListOf<String>()

                team.name = response.getInputResponse(response.responses.size - 3)
                team.color = try {
                    DyeColor.valueOf(response.getInputResponse(response.responses.size - 2).toUpperCase().trim())
                } catch (e: IllegalArgumentException) {
                    errors.add(TextFormat.RED.toString() + "Invalid color " + response.getInputResponse(response.responses.size - 2))
                    null
                }
                team.chatColor = try {
                    val color = response.getInputResponse(response.responses.size - 1).trim()

                    when (color.length) {
                        1 -> {
                            val char = color[0].toLowerCase()

                            if (char in '0'..'9' && char in 'a'..'b') {
                                TextFormat.values().first { it.char == char }
                            } else {
                                errors.add(TextFormat.RED.toString() + "No such chat color character '$char'")
                                null
                            }
                        }
                        2 -> {
                            val char = color[1].toLowerCase()

                            if (char in '0'..'9' && char in 'a'..'b') {
                                TextFormat.values().first { it.char == char }
                            } else {
                                errors.add(TextFormat.RED.toString() + "No such chat color character '$char'")
                                null
                            }
                        }
                        else -> {
                            TextFormat.valueOf(color.toUpperCase())
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    errors.add(TextFormat.RED.toString() + "Invalid color " + response.getInputResponse(1))
                    null
                }

                if (errors.isNotEmpty()) {
                    showTeamWindow(profile, errors)
                    return
                }

                p.sendMessage("${TextFormat.GREEN}Now set all the positions")
            }
            HISTORY_ID -> {
                if (response !is FormResponseSimple) return
                val profile = mapConfigurations[p.name] ?: return

                val step = profile.history[response.clickedButtonId]
                profile.deleteStep = step

                showHistoryDelete(p, step)
            }
            HISTORY_UNDO_ID -> {
                if (response !is FormResponseModal) return
                val profile = mapConfigurations[p.name] ?: return

                if (response.clickedButtonId == 0) {
                    profile.deleteStep?.let { step ->
                        profile.history.remove(step)
                        step.undoAction()
                        p.sendMessage("${TextFormat.GREEN}Action reverted")
                    }
                }
            }
            EDIT_TEAM_ID -> {
                if (response !is FormResponseCustom) return
                val profile = mapConfigurations[p.name] ?: return

                val data = response.getDropdownResponse(0)
                profile.team = data.elementID
                val team = profile.mapConfiguration.teams[data.elementID]
                p.sendMessage("${TextFormat.GREEN}Now editing team ${data.elementID}(${team.chatColor}${team.name})")
            }
        }
    }

    private fun showHistory(player: Player) {
        val profile = mapConfigurations[player.name] ?: return

        val window = FormWindowSimple(
                "History",
                "Click on the entry you want to revert"
        )

        profile.history.forEach {
            window.addButton(ElementButton("${TextFormat.YELLOW}Team(${it.team}) ${TextFormat.GRAY}- ${it.description}"))
        }

        player.showFormWindow(window, HISTORY_ID)
    }

    private fun showHistoryDelete(player: Player, step: MapConfigurationProfile.HistoryStep) {
        val window = FormWindowModal(
                "Undo change",
                "${TextFormat.YELLOW}Team(${step.team}) ${TextFormat.GRAY}- ${step.description}",
                "Undo operation",
                "Cancel"
        )

        player.showFormWindow(window, HISTORY_UNDO_ID)
    }

    private fun showEditTeam(player: Player) {
        val profile = mapConfigurations[player.name] ?: return

        val window = FormWindowCustom("Select team")
        window.addElement(ElementDropdown(
                "Team",
                profile.mapConfiguration.teams.map { it.chatColor.toString() + it.name }
        ))

        player.showFormWindow(window, EDIT_TEAM_ID)
    }

    private fun showParticle(p: Player, pos: Vector3) {
        p.level.addParticle(RedstoneParticle(pos.add(0.5, 0.5, 0.5)), p)
    }

    companion object {
        const val ARENA_BASE_ID = 56203845
        const val MAP_BASE_ID = 56703846
        const val MAP_TEAM_ID = 56703847
        const val HISTORY_ID = 56703848
        const val HISTORY_UNDO_ID = 56703849
        const val EDIT_TEAM_ID = 56703850
    }
}