package org.doutitle.managers;

import org.doutitle.DouTitle;
import org.doutitle.api.Title;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;

import java.util.Map;

public class ConditionManager implements Listener {

    private final DouTitle plugin;
    private final Map<Player, Double> lastLocation = new java.util.HashMap<>();

    public ConditionManager(DouTitle plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().name();

        for (Title title : plugin.getTitleManager().getAllTitles()) {
            for (Map<String, Object> condition : ((TitleImpl) title).getConditions()) {
                String type = (String) condition.get("type");
                Object block = condition.get("block");

                if (type.equalsIgnoreCase("BLOCK_BREAK")) {
                    if (block == null || block.toString().equalsIgnoreCase(blockType)) {
                        plugin.getDatabaseManager().addProgress(
                                player.getUniqueId(), title.getId(),
                                "BLOCK_BREAK" + (block != null ? "_" + block : ""), 1);
                        checkAndGrantTitle(player, title);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().name();

        for (Title title : plugin.getTitleManager().getAllTitles()) {
            for (Map<String, Object> condition : ((TitleImpl) title).getConditions()) {
                String type = (String) condition.get("type");
                Object block = condition.get("block");

                if (type.equalsIgnoreCase("BLOCK_PLACE")) {
                    if (block == null || block.toString().equalsIgnoreCase(blockType)) {
                        plugin.getDatabaseManager().addProgress(
                                player.getUniqueId(), title.getId(),
                                "BLOCK_PLACE" + (block != null ? "_" + block : ""), 1);
                        checkAndGrantTitle(player, title);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();

            for (Title title : plugin.getTitleManager().getAllTitles()) {
                for (Map<String, Object> condition : ((TitleImpl) title).getConditions()) {
                    String type = (String) condition.get("type");

                    if (type.equalsIgnoreCase("KILL_PLAYER")) {
                        plugin.getDatabaseManager().addProgress(
                                killer.getUniqueId(), title.getId(), "KILL_PLAYER", 1);
                        checkAndGrantTitle(killer, title);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        double distance = event.getFrom().distance(event.getTo());
        lastLocation.merge(player, distance, Double::sum);

        double total = lastLocation.get(player);
        if (total >= 1.0) {
            lastLocation.put(player, total % 1.0);
            int walked = (int) total;

            for (Title title : plugin.getTitleManager().getAllTitles()) {
                for (Map<String, Object> condition : ((TitleImpl) title).getConditions()) {
                    String type = (String) condition.get("type");

                    if (type.equalsIgnoreCase("WALK_DISTANCE")) {
                        plugin.getDatabaseManager().addProgress(
                                player.getUniqueId(), title.getId(), "WALK_DISTANCE", walked);
                        checkAndGrantTitle(player, title);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onExpGain(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        int amount = event.getAmount();

        for (Title title : plugin.getTitleManager().getAllTitles()) {
            for (Map<String, Object> condition : ((TitleImpl) title).getConditions()) {
                String type = (String) condition.get("type");

                if (type.equalsIgnoreCase("EXP_GAIN")) {
                    plugin.getDatabaseManager().addProgress(
                            player.getUniqueId(), title.getId(), "EXP_GAIN", amount);
                    checkAndGrantTitle(player, title);
                }
            }
        }
    }

    private void checkAndGrantTitle(Player player, Title title) {
        if (!plugin.getDatabaseManager().hasPlayerTitle(player.getUniqueId(), title.getId())) {
            if (title.checkConditions(player.getUniqueId())) {
                plugin.getTitleManager().giveTitle(player, title.getId(), title.getDefaultDuration());
                player.sendMessage("§a恭喜你完成了称号条件！获得了称号: " + title.getDisplayName());
            }
        }
    }
}