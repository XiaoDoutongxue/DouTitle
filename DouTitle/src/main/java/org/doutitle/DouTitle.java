package org.doutitle;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.doutitle.api.DouTitleAPI;
import org.doutitle.api.Title;
import org.doutitle.api.TitleManager;
import org.doutitle.commands.DouTitleCommand;
import org.doutitle.database.DatabaseManager;
import org.doutitle.gui.OpenGUI;
import org.doutitle.gui.ShopGUI;
import org.doutitle.managers.ConfigManager;
import org.doutitle.managers.ConditionManager;
import org.doutitle.managers.PlaceholderManager;
import org.doutitle.managers.TitleManagerImpl;
import org.doutitle.listeners.ChatListener;
import net.milkbowl.vault.economy.Economy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DouTitle extends JavaPlugin implements DouTitleAPI {

    private static DouTitle instance;
    private TitleManagerImpl titleManager;
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private ConditionManager conditionManager;
    private PlaceholderManager placeholderManager;
    private ShopGUI shopGUI;
    private OpenGUI openGUI;

    // 前置插件API
    private PlayerPointsAPI playerPointsAPI;
    private Economy economy;

    // 玩家冷却时间
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        // 检查前置插件
        if (!checkDependencies()) {
            getLogger().severe("缺少必要的前置插件！插件将禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 初始化前置插件API
        setupPlayerPoints();
        setupVault();

        // 保存默认配置文件
        saveDefaultConfig();
        saveResource("shop.yml", false);
        saveResource("open.yml", false);

        // 初始化管理器
        configManager = new ConfigManager(this);
        databaseManager = new DatabaseManager(this);
        conditionManager = new ConditionManager(this);
        titleManager = new TitleManagerImpl(this);

        // 初始化GUI
        shopGUI = new ShopGUI(this);
        openGUI = new OpenGUI(this);

        // 注册命令
        getCommand("doutitle").setExecutor(new DouTitleCommand(this));

        // 注册监听器
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(shopGUI, this);
        getServer().getPluginManager().registerEvents(openGUI, this);
        getServer().getPluginManager().registerEvents(conditionManager, this);

        // 注册PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderManager = new PlaceholderManager(this);
            placeholderManager.register();
            getLogger().info("已注册PlaceholderAPI扩展");
        }

        // 加载所有称号
        loadAllTitles();

        // 启动过期检查任务
        titleManager.startExpiryChecker();

        getLogger().info("DouTitle 插件已启用");
        getLogger().info("PlayerPoints: " + (playerPointsAPI != null ? "已连接" : "未连接"));
        getLogger().info("Vault Economy: " + (economy != null ? "已连接" : "未连接"));
    }

    private boolean checkDependencies() {
        boolean hasPAPI = getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        boolean hasVault = getServer().getPluginManager().getPlugin("Vault") != null;
        boolean hasPlayerPoints = getServer().getPluginManager().getPlugin("PlayerPoints") != null;

        if (!hasPAPI) getLogger().warning("未找到 PlaceholderAPI！");
        if (!hasVault) getLogger().warning("未找到 Vault！");
        if (!hasPlayerPoints) getLogger().warning("未找到 PlayerPoints！");

        return hasPAPI && hasVault && hasPlayerPoints;
    }

    private void setupPlayerPoints() {
        if (getServer().getPluginManager().getPlugin("PlayerPoints") != null) {
            PlayerPoints playerPoints = (PlayerPoints) getServer().getPluginManager().getPlugin("PlayerPoints");
            if (playerPoints != null) {
                playerPointsAPI = playerPoints.getAPI();
                getLogger().info("成功连接 PlayerPoints API");
            }
        }
    }

    private void setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            org.bukkit.plugin.RegisteredServiceProvider<Economy> rsp =
                    getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                getLogger().info("成功连接 Vault Economy");
            }
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("DouTitle 插件已禁用");
    }

    private void loadAllTitles() {
        titleManager.loadTitlesFromConfig();
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        titleManager.reload();
        shopGUI.reload();
        openGUI.reload();
        loadAllTitles();
        getLogger().info("插件配置已重载");
    }

    // ========== API 实现 ==========

    @Override
    public void giveTitle(org.bukkit.entity.Player player, String titleId, long duration) {
        titleManager.giveTitle(player, titleId, duration);
    }

    @Override
    public void removeTitle(org.bukkit.entity.Player player, String titleId) {
        titleManager.removeTitle(player, titleId);
    }

    @Override
    public void equipTitle(org.bukkit.entity.Player player, String titleId) {
        titleManager.equipTitle(player, titleId);
    }

    @Override
    public void unequipTitle(org.bukkit.entity.Player player) {
        titleManager.unequipTitle(player);
    }

    @Override
    public Title getCurrentTitle(org.bukkit.entity.Player player) {
        return titleManager.getCurrentTitle(player);
    }

    @Override
    public boolean hasTitle(org.bukkit.entity.Player player, String titleId) {
        return titleManager.hasTitle(player, titleId);
    }

    @Override
    public List<Title> getPlayerTitles(org.bukkit.entity.Player player) {
        return titleManager.getPlayerTitles(player);
    }

    @Override
    public Title getTitleById(String titleId) {
        return titleManager.getTitleById(titleId);
    }

    @Override
    public List<Title> getAllTitles() {
        return titleManager.getAllTitles();
    }

    @Override
    public void createTitle(String id, String displayName, long duration) {
        titleManager.createTitle(id, displayName, duration);
    }

    @Override
    public void deleteTitle(String id) {
        titleManager.deleteTitle(id);
    }

    public boolean hasPlayerPointsAPI() {
        return playerPointsAPI != null;
    }

    public PlayerPointsAPI getPlayerPointsAPI() {
        return playerPointsAPI;
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    // ========== Getters ==========

    public static DouTitle getInstance() {
        return instance;
    }

    public TitleManagerImpl getTitleManager() {
        return titleManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ConditionManager getConditionManager() {
        return conditionManager;
    }

    public ShopGUI getShopGUI() {
        return shopGUI;
    }

    public OpenGUI getOpenGUI() {
        return openGUI;
    }

    public Map<UUID, Long> getCooldowns() {
        return cooldowns;
    }
}