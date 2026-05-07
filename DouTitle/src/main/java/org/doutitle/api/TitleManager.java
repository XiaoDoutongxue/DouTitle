package org.doutitle.api;

import org.bukkit.entity.Player;
import java.util.List;

/**
 * 称号管理器接口
 */
public interface TitleManager {

    /**
     * 给予玩家称号
     */
    void giveTitle(Player player, String titleId, long duration);

    /**
     * 移除玩家称号
     */
    void removeTitle(Player player, String titleId);

    /**
     * 佩戴称号
     */
    void equipTitle(Player player, String titleId);

    /**
     * 卸下称号
     */
    void unequipTitle(Player player);

    /**
     * 获取当前佩戴称号
     */
    Title getCurrentTitle(Player player);

    /**
     * 检查玩家是否拥有称号
     */
    boolean hasTitle(Player player, String titleId);

    /**
     * 获取玩家所有称号
     */
    List<Title> getPlayerTitles(Player player);

    /**
     * 根据ID获取称号
     */
    Title getTitleById(String titleId);

    /**
     * 获取所有称号
     */
    List<Title> getAllTitles();

    /**
     * 创建称号
     */
    void createTitle(String id, String displayName, long duration);

    /**
     * 删除称号
     */
    void deleteTitle(String id);

    /**
     * 加载配置文件中的称号
     */
    void loadTitlesFromConfig();

    /**
     * 重载所有称号
     */
    void reload();
}