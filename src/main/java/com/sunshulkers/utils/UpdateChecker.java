package com.sunshulkers.utils;

import com.sunshulkers.SunShulkersPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class UpdateChecker {
    
    private final SunShulkersPlugin plugin;
    private final String currentVersion;
    private final String checkUrl = "https://api.sunworld.pro/sunshulkers/version";
    private final String downloadUrl = "https://spigotmc.ru/resources/sunshulkers-menedzher-shalkerov.4007/";
    
    private String latestVersion = null;
    private boolean updateAvailable = false;
    private boolean checkFailed = false;
    
    public UpdateChecker(SunShulkersPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }
    
    /**
     * Асинхронно проверяет наличие обновлений
     */
    public CompletableFuture<Void> checkForUpdates() {
        return CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(checkUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                // Настраиваем соединение
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000); // 5 секунд таймаут
                connection.setReadTimeout(5000);
                connection.setRequestProperty("User-Agent", "SunShulkers/" + currentVersion);
                connection.setRequestProperty("X-Server-Version", Bukkit.getVersion());
                connection.setRequestProperty("X-Bukkit-Version", Bukkit.getBukkitVersion());
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()))) {
                        
                        String response = reader.readLine();
                        if (response != null && !response.isEmpty()) {
                            // Ожидаем простой текстовый ответ с версией
                            // Или JSON: {"version": "1.0.1", "download": "url", "changelog": "..."}
                            latestVersion = response.trim();
                            
                            // Сравниваем версии
                            updateAvailable = isNewerVersion(latestVersion, currentVersion);
                            checkFailed = false;
                        }
                    }
                } else {
                    checkFailed = true;
                    plugin.getLogger().warning("Не удалось проверить обновления. Код ответа: " + responseCode);
                }
                
                connection.disconnect();
            } catch (Exception e) {
                checkFailed = true;
                plugin.getLogger().log(Level.WARNING, "Ошибка при проверке обновлений", e);
            }
        });
    }
    
    /**
     * Проверяет, является ли версия более новой
     */
    private boolean isNewerVersion(String newVersion, String currentVersion) {
        try {
            // Убираем префиксы v если есть
            newVersion = newVersion.replaceFirst("^v", "");
            currentVersion = currentVersion.replaceFirst("^v", "");
            
            String[] newParts = newVersion.split("\\.");
            String[] currentParts = currentVersion.split("\\.");
            
            int length = Math.max(newParts.length, currentParts.length);
            
            for (int i = 0; i < length; i++) {
                int newPart = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                
                if (newPart > currentPart) {
                    return true;
                } else if (newPart < currentPart) {
                    return false;
                }
            }
            
            return false;
        } catch (NumberFormatException e) {
            // Если не можем сравнить версии, считаем что обновление не требуется
            return false;
        }
    }
    
    /**
     * Уведомляет всех онлайн админов об обновлении
     */
    public void notifyOnlineAdmins() {
        if (!updateAvailable || latestVersion == null) {
            return;
        }
        
        // Уведомляем всех онлайн игроков с правами админа через 3 секунды после загрузки
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Проверяем права на получение уведомлений об обновлениях
                    if (player.hasPermission("sunshulkers.admin") || 
                        player.hasPermission("sunshulkers.update.notify") || 
                        player.isOp()) {
                        notifyPlayer(player);
                    }
                }
            }
        }.runTaskLater(plugin, 60L); // 3 секунды после загрузки
    }
    
    /**
     * Уведомляет игрока об обновлении
     */
    public void notifyPlayer(Player player) {
        if (!updateAvailable || latestVersion == null) {
            return;
        }
        
        // Отправляем красивое сообщение через 2 секунды после входа
        new BukkitRunnable() {
            @Override
            public void run() {
                // Получаем префикс из конфига
                Component prefix = plugin.getConfigManager().getMessageComponents().getPrefix();
                
                // Создаем сообщение с кликабельной ссылкой
                Component fullMessage = Component.text()
                    .append(prefix)
                    .append(Component.space())
                    .append(Component.text("Доступна новая версия ", NamedTextColor.WHITE))
                    .append(Component.text(latestVersion, NamedTextColor.YELLOW, TextDecoration.BOLD))
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text("https://spigotmc.ru/resources/sunshulkers-menedzher-shalkerov.4007/", NamedTextColor.GREEN)
                        .decorate(TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(downloadUrl))
                        .hoverEvent(HoverEvent.showText(Component.text("Нажмите чтобы скачать обновление", NamedTextColor.GRAY))))
                    .build();
                
                // Отправляем сообщение
                plugin.getAdventure().player(player).sendMessage(fullMessage);
            }
        }.runTaskLater(plugin, 40L); // 2 секунды
    }
    
    /**
     * Выводит информацию об обновлении в консоль
     */
    public void logUpdateStatus() {
        if (checkFailed) {
            plugin.getLogger().info("Не удалось проверить наличие обновлений");
        } else if (updateAvailable && latestVersion != null) {
            plugin.getLogger().info("");
            plugin.getLogger().info("§e╔══════════════════════════════════════════════╗");
            plugin.getLogger().info("§e║        §6§lДОСТУПНО ОБНОВЛЕНИЕ!             §e║");
            plugin.getLogger().info("§e║                                              ║");
            plugin.getLogger().info("§e║ §fТекущая версия: §c" + String.format("%-27s", currentVersion) + " §e║");
            plugin.getLogger().info("§e║ §fНовая версия:   §a§l" + String.format("%-27s", latestVersion) + " §e║");
            plugin.getLogger().info("§e║                                              ║");
            plugin.getLogger().info("§e║ §7Скачать: §b§n" + downloadUrl + "§e ║");
            plugin.getLogger().info("§e╚══════════════════════════════════════════════╝");
            plugin.getLogger().info("");
        } else {
            plugin.getLogger().info("Вы используете последнюю версию плагина (v" + currentVersion + ")");
        }
    }
    
    // Геттеры
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    public String getLatestVersion() {
        return latestVersion;
    }
    
    public boolean isCheckFailed() {
        return checkFailed;
    }
}