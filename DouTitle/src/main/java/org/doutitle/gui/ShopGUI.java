package org.doutitle.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.doutitle.DouTitle;
import org.doutitle.api.Title;

import java.lang.reflect.Field;
import java.util.*;

public class ShopGUI implements Listener {

    private final DouTitle plugin;
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private int itemsPerPage;
    private String title;
    private int size;
    private List<Integer> slotIndexes;

    public ShopGUI(DouTitle plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        reload();
    }

    public void reload() {
        YamlConfiguration config = plugin.getConfigManager().getShopConfig();
        title = ChatColor.translateAlternateColorCodes('&', config.getString("title", "&7[&b称号商店&7]"));
        size = config.getInt("size", 54);

        String indexStr = config.getString("index", "");
        slotIndexes = new ArrayList<>();
        for (String s : indexStr.split(",")) {
            try {
                slotIndexes.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {}
        }
        itemsPerPage = slotIndexes.size();
    }

    public void open(Player player) {
        openPage(player, 0);
    }

    private void openPage(Player player, int page) {
        YamlConfiguration config = plugin.getConfigManager().getShopConfig();
        List<Title> allTitles = plugin.getTitleManager().getAllTitles();

        int totalPages = (int) Math.ceil((double) allTitles.size() / itemsPerPage);
        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;

        playerPages.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, size, title);

        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, allTitles.size());

        for (int i = start; i < end; i++) {
            Title title = allTitles.get(i);
            int slotIndex = i - start;
            if (slotIndex < slotIndexes.size()) {
                int slot = slotIndexes.get(slotIndex);
                inv.setItem(slot, title.getDisplayItem());
            }
        }

        // 添加上一页按钮
        if (config.contains("previouspage") && config.getBoolean("previouspage.enable", true)) {
            ItemStack prevItem = createButton(config.getConfigurationSection("previouspage"), page, totalPages);
            inv.setItem(config.getInt("previouspage.index", 45), prevItem);
        }

        // 添加下一页按钮
        if (config.contains("nextpage") && config.getBoolean("nextpage.enable", true)) {
            ItemStack nextItem = createButton(config.getConfigurationSection("nextpage"), page, totalPages);
            inv.setItem(config.getInt("nextpage.index", 53), nextItem);
        }

        // 添加称号仓库按钮
        if (config.contains("open") && config.getBoolean("open.enable", true)) {
            ItemStack openItem = createCustomButton(config.getConfigurationSection("open"));
            inv.setItem(config.getInt("open.index", 4), openItem);
        }

        // 添加自定义按钮
        if (config.contains("custom")) {
            Set<String> keys = config.getConfigurationSection("custom").getKeys(false);
            for (String key : keys) {
                if (config.getBoolean("custom." + key + ".enable", true)) {
                    ItemStack customItem = createCustomButton(config.getConfigurationSection("custom." + key));
                    inv.setItem(config.getInt("custom." + key + ".index", 0), customItem);
                }
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createButton(ConfigurationSection section, int page, int totalPages) {
        String materialStr = section.getString("material", "PAPER");
        String name = section.getString("name", "按钮");
        List<String> lore = section.getStringList("lore");
        boolean isEnchant = section.getBoolean("isEnchant", false);

        ItemStack item = createItemStack(materialStr);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            name = name.replace("${pageNum}", String.valueOf(page + 1))
                    .replace("${count}", String.valueOf(totalPages));
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            List<String> finalLore = new ArrayList<>();
            for (String line : lore) {
                line = line.replace("${pageNum}", String.valueOf(page + 1))
                        .replace("${count}", String.valueOf(totalPages));
                finalLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(finalLore);

            if (isEnchant) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createCustomButton(ConfigurationSection section) {
        String materialStr = section.getString("material", "PAPER");
        String name = section.getString("name", "按钮");
        List<String> lore = section.getStringList("lore");
        boolean isEnchant = section.getBoolean("isEnchant", false);
        String headBase = section.getString("headBase", null);

        ItemStack item = createItemStackWithHead(materialStr, headBase);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            List<String> finalLore = new ArrayList<>();
            for (String line : lore) {
                finalLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(finalLore);

            if (isEnchant) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createItemStackWithHead(String materialStr, String headBase) {
        if (headBase != null && (materialStr.equalsIgnoreCase("PLAYER_HEAD") || materialStr.equals("397") || materialStr.equals("SKULL_ITEM"))) {
            return createSkullItem(headBase);
        }
        return createItemStack(materialStr);
    }

    @SuppressWarnings("deprecation")
    private ItemStack createSkullItem(String base64) {
        try {
            // 1.12 使用 SKULL_ITEM 材质，data 值为 3
            ItemStack skull = new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();

            if (meta != null && base64 != null && !base64.isEmpty()) {
                // 使用反射设置 GameProfile（避免直接依赖 authlib）
                try {
                    Class<?> skullMetaClass = meta.getClass();
                    Field profileField = skullMetaClass.getDeclaredField("profile");
                    profileField.setAccessible(true);

                    // 动态创建 GameProfile - 使用反射避免编译依赖
                    Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
                    Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");

                    java.lang.reflect.Constructor<?> profileConstructor = gameProfileClass.getDeclaredConstructor(UUID.class, String.class);
                    Object profile = profileConstructor.newInstance(UUID.randomUUID(), "");

                    // 创建 Property
                    java.lang.reflect.Constructor<?> propertyConstructor = propertyClass.getDeclaredConstructor(String.class, String.class);
                    Object property = propertyConstructor.newInstance("textures", base64);

                    // 获取 properties 并添加
                    java.lang.reflect.Method getProperties = gameProfileClass.getDeclaredMethod("getProperties");
                    Object properties = getProperties.invoke(profile);
                    java.lang.reflect.Method put = properties.getClass().getDeclaredMethod("put", String.class, propertyClass);
                    put.invoke(properties, "textures", property);

                    profileField.set(meta, profile);
                    skull.setItemMeta(meta);
                } catch (Exception e) {
                    // 反射失败，忽略
                    e.printStackTrace();
                }
            }

            return skull;
        } catch (Exception e) {
            return new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (short) 3);
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack createItemStack(String materialStr) {
        try {
            // 支持数字ID（1.12）
            if (materialStr.matches("\\d+")) {
                int id = Integer.parseInt(materialStr);
                Material mat = Material.getMaterial(id);
                if (mat != null) return new ItemStack(mat);
            }
            // 支持数字ID:子ID
            if (materialStr.matches("\\d+:\\d+")) {
                String[] parts = materialStr.split(":");
                int id = Integer.parseInt(parts[0]);
                short data = Short.parseShort(parts[1]);
                Material mat = Material.getMaterial(id);
                if (mat != null) return new ItemStack(mat, 1, data);
            }
            // 支持材质名称
            Material mat = Material.valueOf(materialStr.toUpperCase());
            return new ItemStack(mat);
        } catch (Exception ignored) {}
        return new ItemStack(Material.STONE);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().equals(title)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        YamlConfiguration config = plugin.getConfigManager().getShopConfig();

        // 检查上一页
        if (config.contains("previouspage") && config.getBoolean("previouspage.enable") &&
                slot == config.getInt("previouspage.index")) {
            int page = playerPages.getOrDefault(player.getUniqueId(), 0);
            openPage(player, page - 1);
            return;
        }

        // 检查下一页
        if (config.contains("nextpage") && config.getBoolean("nextpage.enable") &&
                slot == config.getInt("nextpage.index")) {
            int page = playerPages.getOrDefault(player.getUniqueId(), 0);
            openPage(player, page + 1);
            return;
        }

        // 检查称号商城按钮
        if (config.contains("open") && config.getBoolean("open.enable") &&
                slot == config.getInt("open.index")) {
            executeCommands(config.getStringList("open.command"), player);
            return;
        }

        // 检查自定义按钮
        if (config.contains("custom")) {
            Set<String> keys = config.getConfigurationSection("custom").getKeys(false);
            for (String key : keys) {
                if (config.getBoolean("custom." + key + ".enable") &&
                        slot == config.getInt("custom." + key + ".index")) {
                    executeCommands(config.getStringList("custom." + key + ".command"), player);
                    return;
                }
            }
        }

        // 检查称号点击
        int page = playerPages.getOrDefault(player.getUniqueId(), 0);
        int start = page * itemsPerPage;
        List<Title> allTitles = plugin.getTitleManager().getAllTitles();

        for (int i = 0; i < slotIndexes.size(); i++) {
            if (slotIndexes.get(i) == slot) {
                int titleIndex = start + i;
                if (titleIndex < allTitles.size()) {
                    Title title = allTitles.get(titleIndex);
                    handleTitlePurchase(player, title);
                }
                break;
            }
        }
    }

    private void handleTitlePurchase(Player player, Title title) {
        // 检查是否已拥有
        if (plugin.getTitleManager().hasTitle(player, title.getId())) {
            player.sendMessage("§c你已经拥有这个称号了！");
            player.closeInventory();
            return;
        }

        // 检查条件
        if (title.checkConditions(player.getUniqueId())) {
            plugin.getTitleManager().giveTitle(player, title.getId(), title.getDefaultDuration());
            player.closeInventory();
        } else {
            player.sendMessage("§c你尚未满足获取条件！");
            player.sendMessage(title.getProgress(player.getUniqueId()));
        }
    }

    private void executeCommands(List<String> commands, Player player) {
        if (commands == null) return;
        for (String cmd : commands) {
            Bukkit.dispatchCommand(player, cmd);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(title)) {
            playerPages.remove(event.getPlayer().getUniqueId());
        }
    }
}