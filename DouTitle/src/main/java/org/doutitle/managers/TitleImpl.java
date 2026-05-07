package org.doutitle.managers;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.doutitle.DouTitle;
import org.doutitle.api.Title;

import java.util.*;

public class TitleImpl implements Title {

    private final DouTitle plugin;
    private final String id;
    private final String displayName;
    private final String material;
    private final boolean enchant;
    private final List<String> lore;
    private final List<Map<String, Object>> conditions;
    private final int shopIndex;
    private final long defaultDuration;

    @SuppressWarnings("unchecked")
    public TitleImpl(DouTitle plugin, String id, String displayName, String material,
                     boolean enchant, List<String> lore, List<Map<String, Object>> conditions,
                     int shopIndex, long defaultDuration) {
        this.plugin = plugin;
        this.id = id;
        this.displayName = ChatColor.translateAlternateColorCodes('&', displayName);
        this.material = material;
        this.enchant = enchant;

        // 修复 toList() 兼容性问题
        this.lore = new ArrayList<>();
        if (lore != null) {
            for (String line : lore) {
                this.lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
        }

        this.conditions = conditions != null ? conditions : new ArrayList<>();
        this.shopIndex = shopIndex;
        this.defaultDuration = defaultDuration;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getRawDisplayName() {
        return ChatColor.stripColor(displayName);
    }

    @Override
    public String getMaterial() {
        return material;
    }

    @Override
    public boolean isEnchant() {
        return enchant;
    }

    @Override
    public List<String> getLore() {
        return lore;
    }

    @Override
    public List<Map<String, Object>> getConditions() {
        return conditions;
    }

    @Override
    public int getShopIndex() {
        return shopIndex;
    }

    @Override
    public long getDefaultDuration() {
        return defaultDuration;
    }

    @Override
    public ItemStack getDisplayItem() {
        ItemStack item = createItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(displayName);

            List<String> finalLore = new ArrayList<>();
            for (String line : lore) {
                finalLore.add(line.replace("%doutitle_progress%", getProgress(null)));
            }
            meta.setLore(finalLore);

            if (enchant) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    @Override
    public String getProgress(UUID playerUUID) {
        if (conditions.isEmpty()) {
            return ChatColor.GREEN + "✓ 可直接获取";
        }

        StringBuilder progress = new StringBuilder();
        for (Map<String, Object> condition : conditions) {
            String type = (String) condition.get("type");
            Object block = condition.get("block");
            long required = 0;
            if (condition.containsKey("amount")) {
                Object amount = condition.get("amount");
                if (amount instanceof Number) {
                    required = ((Number) amount).longValue();
                }
            }

            long current = 0;
            if (playerUUID != null && plugin != null) {
                current = plugin.getDatabaseManager().getProgress(playerUUID, id, type);
                if (block != null) {
                    current = plugin.getDatabaseManager().getProgress(playerUUID, id, type + "_" + block);
                }
            }

            String typeName = getTypeName(type);
            String target = block != null ? block.toString() : "";

            String color = current >= required ? ChatColor.GREEN.toString() : ChatColor.RED.toString();
            progress.append("  ").append(color).append(typeName);
            if (!target.isEmpty()) progress.append(" ").append(target);
            progress.append(": ").append(current).append("/").append(required);

            if (current >= required) {
                progress.append(" ✓");
            }
            progress.append("\n");
        }

        return progress.toString();
    }

    @Override
    public boolean checkConditions(UUID playerUUID) {
        if (conditions.isEmpty()) {
            return true;
        }

        for (Map<String, Object> condition : conditions) {
            String type = (String) condition.get("type");
            Object block = condition.get("block");
            long required = 0;
            if (condition.containsKey("amount")) {
                Object amount = condition.get("amount");
                if (amount instanceof Number) {
                    required = ((Number) amount).longValue();
                }
            }

            long current = 0;
            if (playerUUID != null && plugin != null) {
                current = plugin.getDatabaseManager().getProgress(playerUUID, id, type);
                if (block != null) {
                    current = plugin.getDatabaseManager().getProgress(playerUUID, id, type + "_" + block);
                }
            }

            if (current < required) {
                return false;
            }
        }
        return true;
    }

    private String getTypeName(String type) {
        if (type == null) return "未知";
        switch (type.toUpperCase()) {
            case "BLOCK_BREAK": return "挖掘方块";
            case "WALK_DISTANCE": return "行走距离";
            case "KILL_PLAYER": return "击杀玩家";
            case "KILL_MOB": return "击杀怪物";
            case "BLOCK_PLACE": return "放置方块";
            case "FISH_CATCH": return "钓鱼";
            case "CRAFT_ITEM": return "合成物品";
            case "EXP_GAIN": return "获得经验";
            case "PLAYTIME": return "游戏时间";
            case "POINTS": return "点券";
            case "MONEY": return "金币";
            case "FREE": return "免费";
            default: return type;
        }
    }

    private ItemStack createItemStack(String materialStr) {
        try {
            // 支持数字ID（1.12及以下）
            if (materialStr.matches("\\d+")) {
                int id = Integer.parseInt(materialStr);
                Material mat = Material.getMaterial(id);
                if (mat != null) {
                    return new ItemStack(mat);
                }
            }

            // 支持数字ID:子ID 格式如 "35:14"
            if (materialStr.matches("\\d+:\\d+")) {
                String[] parts = materialStr.split(":");
                int id = Integer.parseInt(parts[0]);
                short data = Short.parseShort(parts[1]);
                Material mat = Material.getMaterial(id);
                if (mat != null) {
                    return new ItemStack(mat, 1, data);
                }
            }

            // 支持材质名称
            Material mat = Material.getMaterial(materialStr.toUpperCase());
            if (mat != null) {
                return new ItemStack(mat);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ItemStack(Material.STONE);
    }
}