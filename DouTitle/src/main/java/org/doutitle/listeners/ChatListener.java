package org.doutitle.listeners;

import org.doutitle.DouTitle;
import org.doutitle.api.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final DouTitle plugin;

    public ChatListener(DouTitle plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfigManager().isChatEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        Title currentTitle = plugin.getTitleManager().getCurrentTitle(player);

        if (currentTitle != null) {
            String format = event.getFormat();
            String titleName = currentTitle.getDisplayName();
            String newFormat = format.replace("%doutitle_state%", titleName + " ");
            event.setFormat(newFormat);
        } else {
            event.setFormat(event.getFormat().replace("%doutitle_state%", ""));
        }
    }
}