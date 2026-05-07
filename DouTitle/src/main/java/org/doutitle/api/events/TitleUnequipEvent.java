package org.doutitle.api.events;

import org.doutitle.api.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 玩家卸下称号事件
 */
public class TitleUnequipEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Title title;

    public TitleUnequipEvent(Player player, Title title) {
        this.player = player;
        this.title = title;
    }

    public Player getPlayer() {
        return player;
    }

    public Title getTitle() {
        return title;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}