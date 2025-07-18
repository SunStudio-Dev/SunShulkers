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
        String filename = plugin.getConfigManager().getDatabaseFilename();
        this.databasePath = plugin.getDataFolder().getAbsolutePath() + File.separator + filename;
    }
    
    /**
     * Инициализация базы данных
     */
    public boolean initialize() {
        try {
            // Создаем папку плагина если её нет
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            
            // Закрываем существующее соединение если оно есть
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            
            // Подключаемся к H2 (embedded mode)
            Class.forName("org.h2.Driver");
            
            // Получаем настройки из конфига
            String compatibilityMode = plugin.getConfigManager().getDatabaseCompatibilityMode();
            String username = plugin.getConfigManager().getDatabaseUsername();
            String password = plugin.getConfigManager().getDatabasePassword();
            
            // Используем режим совместимости из конфига
            // Добавляем AUTO_SERVER=TRUE для возможности множественных подключений
            String url = "jdbc:h2:file:" + databasePath + ";MODE=" + compatibilityMode + ";DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE";
            connection = DriverManager.getConnection(url, username, password);
            
            // Создаем таблицу для настроек игроков
            createTables();
            
            plugin.getLogger().info("База данных H2 успешно инициализирована");
            return true;
            
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Ошибка при инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Создание таблиц
     */
    private void createTables() throws SQLException {
        String createPlayersTable = "CREATE TABLE IF NOT EXISTS player_settings (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "autocollect_enabled BOOLEAN DEFAULT TRUE" +
                ")";
        
        try (Statement statement = connection.createStatement()) {
            statement.execute(createPlayersTable);
        }
    }
    
    /**
     * Проверяет, активно ли соединение с базой данных
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Получает состояние автосбора для игрока
     */
    public boolean getAutoCollectState(Player player) {
        if (!isConnected()) {
            plugin.getLogger().warning("База данных не подключена!");
            return true; // Возвращаем значение по умолчанию
        }
        
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
        if (!isConnected()) {
            plugin.getLogger().warning("База данных не подключена! Не удалось сохранить состояние автосбора.");
            return;
        }
        
        String query = "MERGE INTO player_settings (uuid, autocollect_enabled) VALUES (?, ?)";
        
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
                // Выполняем SHUTDOWN для H2, чтобы убедиться что база корректно закрыта
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("SHUTDOWN");
                } catch (SQLException e) {
                    // Игнорируем ошибки SHUTDOWN
                }
                
                connection.close();
                connection = null;
                plugin.getLogger().info("Соединение с базой данных H2 закрыто");
            } catch (SQLException e) {
                plugin.getLogger().warning("Ошибка при закрытии соединения с базой данных: " + e.getMessage());
            }
        }
    }
}
