package org.doutitle.api.events;

import org.doutitle.api.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 玩家获得称号事件
 */
public class TitleGiveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Title title;
    private final long duration;
    private boolean cancelled;

    public TitleGiveEvent(Player player, Title title, long duration) {
        this.player = player;
        this.title = title;
        this.duration = duration;
        this.cancelled = false;
    }

    public Player getPlayer() {
        return player;
    }

    public Title getTitle() {
        return title;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}