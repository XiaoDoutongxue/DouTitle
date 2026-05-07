package org.doutitle.api;

import org.bukkit.inventory.ItemStack;
import java.util.List;
import java.util.Map;
import java.util.UUID;
/**
 * 称号对象
 */
public interface Title {

    /**
     * @return 称号唯一ID
     */
    String getId();

    /**
     * @return 显示名称（已格式化）
     */
    String getDisplayName();

    /**
     * @return 原始显示名称
     */
    String getRawDisplayName();

    /**
     * @return 商店显示物品材质
     */
    String getMaterial();

    /**
     * @return 是否附魔效果
     */
    boolean isEnchant();

    /**
     * @return 描述信息
     */
    List<String> getLore();

    /**
     * @return 获取条件列表
     */
    List<Map<String, Object>> getConditions();

    /**
     * @return 在商店GUI中的位置
     */
    int getShopIndex();

    /**
     * @return 默认持续时间（秒），-1为永久
     */
    long getDefaultDuration();

    /**
     * 获取称号物品展示
     * @return 展示用的ItemStack
     */
    ItemStack getDisplayItem();

    /**
     * 获取玩家对该称号的进度描述
     * @param playerUUID 玩家UUID
     * @return 进度描述
     */
    String getProgress(UUID playerUUID);

    /**
     * 检查玩家是否满足获取条件
     * @param playerUUID 玩家UUID
     * @return 是否满足
     */
    boolean checkConditions(UUID playerUUID);
}