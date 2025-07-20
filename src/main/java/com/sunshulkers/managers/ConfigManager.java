package com.sunshulkers.managers;

import com.sunshulkers.SunShulkersPlugin;
import com.sunshulkers.utils.MessageComponents;
import com.sunshulkers.utils.OpenMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {
    
    private final SunShulkersPlugin plugin;
    private FileConfiguration config;
    private MessageComponents messageComponents;
    
    public ConfigManager(SunShulkersPlugin plugin) {
        this.plugin = plugin;
        this.messageComponents = new MessageComponents();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        initializeMessageComponents();
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        initializeMessageComponents();
    }
    
    /**
     * Инициализирует компоненты сообщений из конфига
     */
    private void initializeMessageComponents() {
        messageComponents.initializeFromStrings(
            getPrefix(),
            getReloadMessage(),
            getNoPermissionMessage(), 
            getCooldownMessage(),
            getBlacklistedItemMessage(),
            getBlacklistedWorldMessage(),
            getItemDeletedMessage(),
            getOpenMessage(),
            getCloseMessage(),
            getAutoCollectEnabledMessage(),
            getAutoCollectDisabledMessage(),
            getAutoCollectNoSpaceMessage(),
            getRenamePromptMessage(),
            getRenameSupportedFormatsMessage(),
            getRenameTimeoutMessage(),
            getRenameSuccessMessage(),
            null, // renameInvalidMessage - нет в конфиге
            null, // renameTooltipMessage - нет в конфиге
            getRenameCancelledMessage(),
            getCloseOnAttackMessage(),
            null // cannotDropOpenShulker - используем значение по умолчанию
        );
    }
    
    /**
     * Получает экземпляр MessageComponents
     */
    public MessageComponents getMessageComponents() {
        return messageComponents;
    }
    
    // Общие настройки сообщений
    public String getPrefix() {
        String prefix = config.getString("messages.prefix", "<gradient:#9863E7:#4498DB>SunShulkers</gradient> &7-");
        return prefix != null && !prefix.trim().isEmpty() ? prefix : null;
    }
    
    public String getReloadMessage() {
        String message = config.getString("messages.reload-config", "Конфигурация плагина перезагружена.");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    public String getNoPermissionMessage() {
        String message = config.getString("messages.no-permission", "У вас недостаточно прав.");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    public String getCooldownMessage() {
        String message = config.getString("messages.cooldown", "Подождите {time}с. перед открытием шалкера.");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    public String getBlacklistedItemMessage() {
        String message = config.getString("messages.blacklisted-item", "Этот предмет нельзя помещать в шалкер!");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    public String getBlacklistedWorldMessage() {
        String message = config.getString("messages.blacklisted-world", "В мире {world_name} нельзя открывать шалкеры!");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    public String getItemDeletedMessage() {
        String message = config.getString("messages.item-deleted", "Предмет удален из-за нарушения правил!");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    // Сообщения при открытии/закрытии
    public String getOpenMessage() {
        String message = config.getString("messages.open-message", "Вы открыли шалкер!");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    public String getCloseMessage() {
        String message = config.getString("messages.close-message", "Вы закрыли шалкер!");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    // Звуки
    public Sound getOpenSound() {
        String soundName = config.getString("sounds.open", "BLOCK_SHULKER_BOX_OPEN");
        if (soundName == null || soundName.trim().isEmpty()) {
            return null;
        }
        
        try {
            return Sound.valueOf(soundName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неверный звук открытия: " + soundName);
            return null;
        }
    }
    
    public Sound getCloseSound() {
        String soundName = config.getString("sounds.close", "BLOCK_SHULKER_BOX_CLOSE");
        if (soundName == null || soundName.trim().isEmpty()) {
            return null;
        }
        
        try {
            return Sound.valueOf(soundName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неверный звук закрытия: " + soundName);
            return null;
        }
    }
    
    // Звук при автоматическом сборе в шалкер
    public Sound getPickupSound() {
        String soundName = config.getString("sounds.pickup", "ENTITY_ITEM_PICKUP");
        if (soundName == null || soundName.trim().isEmpty()) {
            return null;
        }
        try {
            return Sound.valueOf(soundName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неверный звук автосбора: " + soundName);
            return null;
        }
    }

    // Настройка закрытия GUI при атаке игрока
    public boolean isCloseOnAttack() {
        return config.getBoolean("settings.close-on-attack", false);
    }

    public String getCloseOnAttackMessage() {
        String message = config.getString("messages.close-on-attack", null);
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    // Имя шалкера
    public String getShulkerName() {
        return config.getString("settings.shulker-name-format", "{shulker_name}");
    }
    
    // Настройки
    public List<Material> getBlacklistedItems() {
        return config.getStringList("settings.blacklisted-items").stream()
                .map(item -> {
                    try {
                        return Material.valueOf(item.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Неверный материал в blacklisted-items: " + item);
                        return null;
                    }
                })
                .filter(material -> material != null)
                .collect(Collectors.toList());
    }
    
    public List<String> getBlacklistedWorlds() {
        return config.getStringList("settings.blacklisted-worlds");
    }
    
    public int getCooldown() {
        return config.getInt("settings.cooldown", 5);
    }
    
    public boolean isReloadingItemEnabled() {
        return config.getBoolean("settings.visual-cooldown", true);
    }
    
    public boolean isShiftRequired() {
        return config.getBoolean("settings.require-shift", true);
    }
    
    public boolean isDeleteBlacklistedItems() {
        return config.getBoolean("settings.delete-blacklisted-items", false);
    }
    
    public boolean isSmartPlacement() {
        return config.getBoolean("settings.smart-placement", true);
    }
    
    // Настройки наковальни
    public boolean isAnvilEnabled() {
        return config.getBoolean("anvil.enabled", true);
    }
    
    public String getRenamePermission() {
        return config.getString("anvil.rename-permission", "sunshulkers.rename");
    }
    
    public String getColorPermission() {
        return config.getString("anvil.color-permission", "sunshulkers.color");
    }
    
    public Material getButtonMaterial() {
        String materialName = config.getString("anvil.rename-button.material", "NAME_TAG");
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неверный материал кнопки: " + materialName);
            return Material.NAME_TAG;
        }
    }
    
    public String getButtonName() {
        return config.getString("anvil.rename-button.name", "&e&lПереименовать шалкер");
    }
    
    public List<String> getButtonLore() {
        return config.getStringList("anvil.rename-button.lore");
    }
    
    public boolean isButtonEnchanted() {
        return config.getBoolean("anvil.rename-button.enchanted", true);
    }
    
    public int getInputTimeout() {
        return config.getInt("anvil.input-timeout", 30);
    }
    
    // Сообщения для переименования
    public String getRenamePromptMessage() {
        String message = config.getString("messages.rename-prompt", "Введите новое имя для шалкера в чат:");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    public String getRenameSupportedFormatsMessage() {
        // Возвращаем НЕФОРМАТИРОВАННОЕ сообщение (без обработки цветов)
        String message = config.getString("messages.rename-supported-formats", 
            "Поддерживаются цвета: &a, &c, &f (legacy коды), &#FF0000 (HEX), <red>, <blue> (MiniMessage)");
        return message != null && !message.trim().isEmpty() ? message : "";
    }
    
    public String getRenameSuccessMessage() {
        String message = config.getString("messages.rename-success", "Шалкер переименован в: {name}");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    public String getRenameCancelledMessage() {
        String message = config.getString("messages.rename-cancelled", "Переименование отменено.");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    public String getRenameTimeoutMessage() {
        String message = config.getString("messages.rename-timeout", "Время ожидания истекло. Переименование отменено.");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    // Настройки автосбора
    public boolean isAutoCollectEnabled() {
        return config.getBoolean("settings.auto-collect.enabled", true);
    }
    
    public String getAutoCollectPermission() {
        return config.getString("settings.auto-collect.permission", "sunshulkers.autocollect");
    }
    
    public boolean isAutoCollectNotifyNoSpace() {
        return config.getBoolean("settings.auto-collect.notify-no-space", true);
    }
    
    public int getAutoCollectNotifyNoSpaceCooldown() {
        return config.getInt("settings.auto-collect.notify-no-space-cooldown", 3);
    }
    
    public boolean isAutoCollectDefaultEnabled() {
        return config.getBoolean("settings.auto-collect.default-enabled", true);
    }
    
    // Сообщения для автосбора
    public String getAutoCollectEnabledMessage() {
        String message = config.getString("messages.autocollect-enabled", "Автосбор предметов в шалкер включен!");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    public String getAutoCollectDisabledMessage() {
        String message = config.getString("messages.autocollect-disabled", "Автосбор предметов в шалкер отключен!");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    public String getAutoCollectNoSpaceMessage() {
        String message = config.getString("messages.autocollect-no-space", "В шалкере нет места для предмета!");
        return message != null && !message.trim().isEmpty() ? message : null;
    }
    
    public List<OpenMode> getHandModes() {
        List<String> modeNames = config.getStringList("settings.open-modes.hand-modes");
        
        // Если список пуст, используем старую настройку require-shift для обратной совместимости
        if (modeNames.isEmpty()) {
            List<OpenMode> modes = new ArrayList<>();
            if (isShiftRequired()) {
                modes.add(OpenMode.SHIFT_RIGHT_CLICK);
            } else {
                modes.add(OpenMode.RIGHT_CLICK);
            }
            return modes;
        }
        
        // Конвертируем строки в режимы
        return modeNames.stream()
                .map(OpenMode::fromString)
                .filter(mode -> mode != null)
                .collect(Collectors.toList());
    }
    
    public List<OpenMode> getInventoryModes() {
        List<String> modeNames = config.getStringList("settings.open-modes.inventory-modes");
        
        // Если список пуст, используем старую настройку require-shift для обратной совместимости
        if (modeNames.isEmpty()) {
            List<OpenMode> modes = new ArrayList<>();
            if (isShiftRequired()) {
                modes.add(OpenMode.SHIFT_RIGHT_CLICK);
            } else {
                modes.add(OpenMode.RIGHT_CLICK);
            }
            return modes;
        }
        
        // Конвертируем строки в режимы
        return modeNames.stream()
                .map(OpenMode::fromString)
                .filter(mode -> mode != null)
                .collect(Collectors.toList());
    }
    
    // Настройки базы данных
    public String getDatabaseCompatibilityMode() {
        return config.getString("settings.database.compatibility-mode", "MySQL");
    }
    
    public String getDatabaseFilename() {
        return config.getString("settings.database.filename", "data");
    }
    
    public String getDatabaseUsername() {
        return config.getString("settings.database.username", "sa");
    }
    
    public String getDatabasePassword() {
        return config.getString("settings.database.password", "");
    }
}
