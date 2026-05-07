package org.doutitle.managers;

import org.doutitle.DouTitle;
import org.doutitle.api.Title;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PlaceholderManager extends PlaceholderExpansion {

    private final DouTitle plugin;

    public PlaceholderManager(DouTitle plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "doutitle";
    }

    @Override
    public String getAuthor() {
        return "DouTitle Author";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        switch (identifier.toLowerCase()) {
            case "state":
                Title current = plugin.getTitleManager().getCurrentTitle(player);
                return current != null ? current.getDisplayName() : "";

            case "titlename":
                List<Title> titles = plugin.getTitleManager().getPlayerTitles(player);
                if (titles.isEmpty()) return "无";
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < titles.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(titles.get(i).getDisplayName());
                }
                return sb.toString();

            case "progress":
                // 这里需要根据上下文获取具体称号的进度，比较复杂
                return "请查看称号详情";

            case "expirationtime":
                Title equipped = plugin.getTitleManager().getCurrentTitle(player);
                if (equipped == null) return "无";
                long expireTime = plugin.getDatabaseManager().getTitleExpireTime(
                        player.getUniqueId(), equipped.getId());
                if (expireTime == -1) return "永久";
                if (expireTime <= System.currentTimeMillis()) return "已过期";
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
                return sdf.format(new Date(expireTime));

            case "time":
                // 获取玩家第一个称号的获得时间
                List<Title> playerTitles = plugin.getTitleManager().getPlayerTitles(player);
                if (playerTitles.isEmpty()) return "无";
                // 简化处理，实际需要从数据库获取获得时间
                return "2024年1月1日";

            case "number":
                return String.valueOf(plugin.getTitleManager().getPlayerTitles(player).size());

            default:
                return null;
        }
    }
}