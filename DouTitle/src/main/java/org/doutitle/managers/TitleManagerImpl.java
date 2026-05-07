package org.doutitle.managers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.doutitle.DouTitle;
import org.doutitle.api.Title;
import org.doutitle.api.TitleManager;
import org.doutitle.api.events.TitleEquipEvent;
import org.doutitle.api.events.TitleGiveEvent;
import org.doutitle.api.events.TitleUnequipEvent;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TitleManagerImpl implements TitleManager {

    private final DouTitle plugin;
    private final Map<String, Title> titles = new ConcurrentHashMap<>();
    private final Map<UUID, List<Title>> playerTitleCache = new ConcurrentHashMap<>();
    private final Map<UUID, Title> playerCurrentTitle = new ConcurrentHashMap<>();

    public TitleManagerImpl(DouTitle plugin) {
        this.plugin = plugin;
    }

    @Override
    public void loadTitlesFromConfig() {
        titles.clear();

        File shopFile = new File(plugin.getDataFolder(), "shop.yml");
        if (!shopFile.exists()) {
            plugin.saveResource("shop.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);

        if (config.contains("shops")) {
            List<Map<?, ?>> shops = config.getMapList("shops");
            int index = 0;
            for (Map<?, ?> shop : shops) {
                String name = (String) shop.get("name");
                String id = String.valueOf(shop.get("ID"));
                String material = shop.containsKey("material") ? (String) shop.get("material") : "NAME_TAG";

                // 修复类型转换问题
                boolean isEnchant = false;
                Object enchantObj = shop.get("isEnchant");
                if (enchantObj instanceof Boolean) {
                    isEnchant = (Boolean) enchantObj;
                }

                List<String> lore = shop.containsKey("lore") ? (List<String>) shop.get("lore") : new ArrayList<>();

                // 修复 conditions 类型转换
                List<Map<String, Object>> conditions = new ArrayList<>();
                Object conditionsObj = shop.get("conditions");
                if (conditionsObj instanceof List) {
                    List<?> rawConditions = (List<?>) conditionsObj;
                    for (Object rawCond : rawConditions) {
                        if (rawCond instanceof Map) {
                            Map<?, ?> rawMap = (Map<?, ?>) rawCond;
                            Map<String, Object> condition = new HashMap<>();
                            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                                condition.put(String.valueOf(entry.getKey()), entry.getValue());
                            }
                            conditions.add(condition);
                        }
                    }
                }

                // 解析默认持续时间
                long defaultDuration = -1;
                for (Map<String, Object> condition : conditions) {
                    if (condition.containsKey("type")) {
                        String type = (String) condition.get("type");
                        if (type.equalsIgnoreCase("FREE")) {
                            defaultDuration = -1;
                        }
                    }
                }

                TitleImpl title = new TitleImpl(plugin, id, name, material, isEnchant, lore, conditions, index, defaultDuration);
                titles.put(id, title);
                index++;
            }
        }

        plugin.getLogger().info("已加载 " + titles.size() + " 个称号");
    }

    @Override
    public void giveTitle(Player player, String titleId, long duration) {
        Title title = titles.get(titleId);
        if (title == null) {
            player.sendMessage("§c称号不存在: " + titleId);
            return;
        }

        // 触发事件
        TitleGiveEvent event = new TitleGiveEvent(player, title, duration);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        UUID uuid = player.getUniqueId();

        // 检查是否已拥有
        if (plugin.getDatabaseManager().hasPlayerTitle(uuid, titleId)) {
            player.sendMessage("§c你已经拥有这个称号了！");
            return;
        }

        // 给予称号
        plugin.getDatabaseManager().givePlayerTitle(uuid, titleId, duration);

        // 刷新缓存
        refreshPlayerTitles(player);

        player.sendMessage("§a你获得了称号: " + title.getDisplayName());
    }

    @Override
    public void removeTitle(Player player, String titleId) {
        Title title = titles.get(titleId);
        if (title == null) return;

        UUID uuid = player.getUniqueId();

        // 如果正在佩戴，先卸下
        Title current = getCurrentTitle(player);
        if (current != null && current.getId().equals(titleId)) {
            unequipTitle(player);
        }

        plugin.getDatabaseManager().removePlayerTitle(uuid, titleId);
        refreshPlayerTitles(player);

        player.sendMessage("§c已移除称号: " + title.getDisplayName());
    }

    @Override
    public void equipTitle(Player player, String titleId) {
        Title title = titles.get(titleId);
        if (title == null) {
            player.sendMessage("§c称号不存在！");
            return;
        }

        UUID uuid = player.getUniqueId();

        // 检查是否拥有
        if (!plugin.getDatabaseManager().hasPlayerTitle(uuid, titleId)) {
            player.sendMessage("§c你没有这个称号！");
            return;
        }

        // 检查是否过期
        long expireTime = plugin.getDatabaseManager().getTitleExpireTime(uuid, titleId);
        if (expireTime != -1 && expireTime <= System.currentTimeMillis()) {
            plugin.getDatabaseManager().removePlayerTitle(uuid, titleId);
            player.sendMessage("§c该称号已过期！");
            refreshPlayerTitles(player);
            return;
        }

        // 触发事件
        TitleEquipEvent event = new TitleEquipEvent(player, title);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        plugin.getDatabaseManager().setEquippedTitle(uuid, titleId);
        playerCurrentTitle.put(uuid, title);

        player.sendMessage("§a你佩戴了称号: " + title.getDisplayName());
    }

    @Override
    public void unequipTitle(Player player) {
        Title current = getCurrentTitle(player);
        if (current == null) {
            player.sendMessage("§c你当前没有佩戴称号");
            return;
        }

        TitleUnequipEvent event = new TitleUnequipEvent(player, current);
        Bukkit.getPluginManager().callEvent(event);

        plugin.getDatabaseManager().removeEquippedTitle(player.getUniqueId());
        playerCurrentTitle.remove(player.getUniqueId());

        player.sendMessage("§a你卸下了称号");
    }

    @Override
    public Title getCurrentTitle(Player player) {
        UUID uuid = player.getUniqueId();

        if (playerCurrentTitle.containsKey(uuid)) {
            return playerCurrentTitle.get(uuid);
        }

        String titleId = plugin.getDatabaseManager().getEquippedTitle(uuid);
        if (titleId != null) {
            Title title = titles.get(titleId);
            if (title != null) {
                playerCurrentTitle.put(uuid, title);
                return title;
            }
        }

        return null;
    }

    @Override
    public boolean hasTitle(Player player, String titleId) {
        return plugin.getDatabaseManager().hasPlayerTitle(player.getUniqueId(), titleId);
    }

    @Override
    public List<Title> getPlayerTitles(Player player) {
        UUID uuid = player.getUniqueId();
        if (playerTitleCache.containsKey(uuid)) {
            return playerTitleCache.get(uuid);
        }

        List<String> titleIds = plugin.getDatabaseManager().getPlayerTitles(uuid);
        List<Title> result = new ArrayList<>();
        for (String id : titleIds) {
            Title title = titles.get(id);
            if (title != null) {
                // 检查是否过期
                long expireTime = plugin.getDatabaseManager().getTitleExpireTime(uuid, id);
                if (expireTime == -1 || expireTime > System.currentTimeMillis()) {
                    result.add(title);
                } else {
                    // 过期则删除
                    plugin.getDatabaseManager().removePlayerTitle(uuid, id);
                }
            }
        }
        playerTitleCache.put(uuid, result);
        return result;
    }

    @Override
    public Title getTitleById(String titleId) {
        return titles.get(titleId);
    }

    @Override
    public List<Title> getAllTitles() {
        return new ArrayList<>(titles.values());
    }

    @Override
    public void createTitle(String id, String displayName, long duration) {
        File shopFile = new File(plugin.getDataFolder(), "shop.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);

        List<Map<String, Object>> shops = new ArrayList<>();
        if (config.contains("shops")) {
            List<?> rawShops = config.getList("shops");
            for (Object obj : rawShops) {
                if (obj instanceof Map) {
                    Map<String, Object> shop = new HashMap<>();
                    Map<?, ?> rawMap = (Map<?, ?>) obj;
                    for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                        shop.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    shops.add(shop);
                }
            }
        }

        Map<String, Object> newTitle = new LinkedHashMap<>();
        newTitle.put("name", displayName);
        newTitle.put("ID", id);
        newTitle.put("material", "NAME_TAG");
        newTitle.put("isEnchant", false);
        newTitle.put("lore", Arrays.asList("", "&7这是一个称号", "&a获得方式:", "%doutitle_progress%"));

        List<Map<String, Object>> conditions = new ArrayList<>();
        Map<String, Object> freeCond = new HashMap<>();
        freeCond.put("type", "FREE");
        conditions.add(freeCond);
        newTitle.put("conditions", conditions);

        shops.add(newTitle);
        config.set("shops", shops);

        try {
            config.save(shopFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        loadTitlesFromConfig();
    }

    @Override
    public void deleteTitle(String id) {
        File shopFile = new File(plugin.getDataFolder(), "shop.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);

        List<Map<String, Object>> shops = new ArrayList<>();
        if (config.contains("shops")) {
            List<?> rawShops = config.getList("shops");
            for (Object obj : rawShops) {
                if (obj instanceof Map) {
                    Map<?, ?> rawMap = (Map<?, ?>) obj;
                    String shopId = String.valueOf(rawMap.get("ID"));
                    if (!shopId.equals(id)) {
                        Map<String, Object> shop = new HashMap<>();
                        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                            shop.put(String.valueOf(entry.getKey()), entry.getValue());
                        }
                        shops.add(shop);
                    }
                }
            }
        }

        config.set("shops", shops);

        try {
            config.save(shopFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        titles.remove(id);
    }

    @Override
    public void reload() {
        titles.clear();
        playerTitleCache.clear();
        playerCurrentTitle.clear();
        loadTitlesFromConfig();
    }

    public void refreshPlayerTitles(Player player) {
        playerTitleCache.remove(player.getUniqueId());

        Title current = getCurrentTitle(player);
        if (current != null && !hasTitle(player, current.getId())) {
            unequipTitle(player);
        }
    }

    public void startExpiryChecker() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().checkAndCleanExpiredTitles();

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    refreshPlayerTitles(player);
                }
            });
        }, 20 * 60 * 5, 20 * 60 * 5);
    }
}