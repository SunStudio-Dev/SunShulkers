package com.sunshulkers.managers;

import com.sunshulkers.SunShulkersPlugin;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    
    private final SunShulkersPlugin plugin;
    private Connection connection;
    private final String databasePath;
    
    public DatabaseManager(SunShulkersPlugin plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder().getAbsolutePath() + File.separator + "data.db";
    }
    
    /**
     * Инициализация базы данных
     */
    public void initialize() {
        try {
            // Создаем папку плагина если её нет
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Подключаемся к SQLite
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            
            // Создаем таблицу для настроек игроков
            createTables();
            
            plugin.getLogger().info("База данных успешно инициализирована");
            
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Ошибка при инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Создание таблиц
     */
    private void createTables() throws SQLException {
        String createPlayersTable = "CREATE TABLE IF NOT EXISTS player_settings (" +
                "uuid TEXT PRIMARY KEY, " +
                "autocollect_enabled BOOLEAN DEFAULT 1" +
                ")";
        
        try (Statement statement = connection.createStatement()) {
            statement.execute(createPlayersTable);
        }
    }
    
    /**
     * Получает состояние автосбора для игрока
     */
    public boolean getAutoCollectState(Player player) {
        String query = "SELECT autocollect_enabled FROM player_settings WHERE uuid = ?";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, player.getUniqueId().toString());
            
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getBoolean("autocollect_enabled");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка при получении состояния автосбора для " + player.getName() + ": " + e.getMessage());
        }
        
        // По умолчанию берем значение из конфига
        return plugin.getConfigManager().isAutoCollectDefaultEnabled();
    }
    
    /**
     * Устанавливает состояние автосбора для игрока
     */
    public void setAutoCollectState(Player player, boolean enabled) {
        String query = "INSERT OR REPLACE INTO player_settings (uuid, autocollect_enabled) VALUES (?, ?)";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, player.getUniqueId().toString());
            statement.setBoolean(2, enabled);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка при сохранении состояния автосбора для " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Закрытие соединения
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Соединение с базой данных закрыто");
            } catch (SQLException e) {
                plugin.getLogger().warning("Ошибка при закрытии соединения с базой данных: " + e.getMessage());
            }
        }
    }
}
