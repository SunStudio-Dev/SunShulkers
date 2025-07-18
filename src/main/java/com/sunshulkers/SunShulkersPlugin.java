package com.sunshulkers;

import com.sunshulkers.commands.AutoCollectCommand;
import com.sunshulkers.commands.SunShulkersCommand;
import com.sunshulkers.listeners.AnvilListener;
import com.sunshulkers.listeners.AutoCollectListener;
import com.sunshulkers.listeners.ShulkerListener;
import com.sunshulkers.listeners.UpdateNotifyListener;
import com.sunshulkers.managers.AutoCollectManager;
import com.sunshulkers.managers.ConfigManager;
import com.sunshulkers.managers.CooldownManager;
import com.sunshulkers.managers.DatabaseManager;
import com.sunshulkers.utils.MessageUtils;
import com.sunshulkers.utils.UpdateChecker;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class SunShulkersPlugin extends JavaPlugin {
    
    private static SunShulkersPlugin instance;
    private ConfigManager configManager;
    private CooldownManager cooldownManager;
    private MessageUtils messageUtils;
    private BukkitAudiences adventure;
    private AutoCollectManager autoCollectManager;
    private DatabaseManager databaseManager;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        instance = this;
        
        // Инициализация Adventure
        this.adventure = BukkitAudiences.create(this);
        
        // Инициализация менеджеров
        this.configManager = new ConfigManager(this);
        // Загрузка конфигурации - важно сделать это сразу после создания ConfigManager
        configManager.loadConfig();
        
        this.cooldownManager = new CooldownManager();
        this.databaseManager = new DatabaseManager(this);
        this.autoCollectManager = new AutoCollectManager(this);
        this.messageUtils = new MessageUtils(this);
        this.updateChecker = new UpdateChecker(this);
        
        // Инициализация базы данных
        if (!databaseManager.initialize()) {
            getLogger().severe("Не удалось инициализировать базу данных! Плагин будет отключен.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Регистрация событий
        getServer().getPluginManager().registerEvents(new ShulkerListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(this), this);
        getServer().getPluginManager().registerEvents(new AutoCollectListener(this), this);
        getServer().getPluginManager().registerEvents(new UpdateNotifyListener(this), this);
        
        // Регистрация команд
        getCommand("sunshulkers").setExecutor(new SunShulkersCommand(this));
        getCommand("autocollect").setExecutor(new AutoCollectCommand(this));
        
        // Красивое сообщение о запуске
        printStartupMessage();
        
        // Проверка обновлений
        updateChecker.checkForUpdates().thenRun(() -> {
            // Выводим статус в консоль
            updateChecker.logUpdateStatus();
            // Уведомляем онлайн админов
            updateChecker.notifyOnlineAdmins();
        });
        
        getLogger().info("SunShulkers плагин успешно загружен!");
    }
    
    /**
     * Выводит красивое сообщение о запуске плагина
     */
    private void printStartupMessage() {
        getLogger().info("SunShulkers загружен! Версия: " + getDescription().getVersion() +
                ", Автор: " + String.join(", ", getDescription().getAuthors()));
        getLogger().info("Кулдаун: " + configManager.getCooldown() + " сек., Автосбор: " +
                (configManager.isAutoCollectEnabled() ? "Вкл" : "Выкл") +
                ", Запрещено предметов: " + configManager.getBlacklistedItems().size());
    }
    
    @Override
    public void onDisable() {
        // Закрываем базу данных
        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
        
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        
        // Красивое сообщение выключения
        printShutdownMessage();
    }
    
    /**
     * Выводит красивое сообщение о выключении плагина
     */
    private void printShutdownMessage() {
        getLogger().info("");
        getLogger().info("👋  Плагин §eSunShulkers отключается...");
        getLogger().info("📊  Статистика сессии:");
        getLogger().info("  ├ Версия: " + getDescription().getVersion());
        getLogger().info("  ├ Время работы: завершено");
        getLogger().info("💾  Все данные сохранены в базу данных");
        getLogger().info("✅  Спасибо за использование SunShulkers!");
        getLogger().info("");
    }

    public static SunShulkersPlugin getInstance() {
        return instance;
    }

    public BukkitAudiences getAdventure() {
        return this.adventure;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public MessageUtils getMessageUtils() {
        return messageUtils;
    }

    public AutoCollectManager getAutoCollectManager() {
        return autoCollectManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}
