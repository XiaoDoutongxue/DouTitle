package org.doutitle.api;

import org.bukkit.entity.Player;
import java.util.List;

/**
 * DouTitle插件API接口
 * 附属插件可以通过此接口与主插件交互
 */
public interface DouTitleAPI {

    /**
     * 给予玩家一个称号
     * @param player 目标玩家
     * @param titleId 称号ID
     * @param duration 持续时间（秒），-1表示永久
     */
    void giveTitle(Player player, String titleId, long duration);

    /**
     * 移除玩家的称号
     * @param player 目标玩家
     * @param titleId 称号ID
     */
    void removeTitle(Player player, String titleId);

    /**
     * 让玩家佩戴称号
     * @param player 目标玩家
     * @param titleId 称号ID
     */
    void equipTitle(Player player, String titleId);

    /**
     * 让玩家卸下称号
     * @param player 目标玩家
     */
    void unequipTitle(Player player);

    /**
     * 获取玩家当前佩戴的称号
     * @param player 目标玩家
     * @return 当前称号，若未佩戴返回null
     */
    Title getCurrentTitle(Player player);

    /**
     * 检查玩家是否拥有某个称号
     * @param player 目标玩家
     * @param titleId 称号ID
     * @return 是否拥有
     */
    boolean hasTitle(Player player, String titleId);

    /**
     * 获取玩家拥有的所有称号
     * @param player 目标玩家
     * @return 称号列表
     */
    List<Title> getPlayerTitles(Player player);

    /**
     * 根据ID获取称号
     * @param titleId 称号ID
     * @return 称号对象，不存在返回null
     */
    Title getTitleById(String titleId);

    /**
     * 获取所有已配置的称号
     * @return 所有称号列表
     */
    List<Title> getAllTitles();

    /**
     * 创建新称号
     * @param id 称号ID
     * @param displayName 显示名称（支持颜色代码）
     * @param duration 持续时间（秒），-1表示永久
     */
    void createTitle(String id, String displayName, long duration);

    /**
     * 删除称号
     * @param id 称号ID
     */
    void deleteTitle(String id);
}