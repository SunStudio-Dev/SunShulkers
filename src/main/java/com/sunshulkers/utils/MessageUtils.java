package com.sunshulkers.utils;

import com.sunshulkers.SunShulkersPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {
    
    private final SunShulkersPlugin plugin;
    private final MiniMessage miniMessage;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    public MessageUtils(SunShulkersPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }
    
    /**
     * Отправляет сообщение игроку с префиксом
     */
    public void sendMessageWithPrefix(Player player, Component message) {
        if (message == null) {
            return;
        }
        
        Component prefix = plugin.getConfigManager().getMessageComponents().getPrefix();
        if (prefix != null) {
            Component fullMessage = prefix.append(Component.text(" ")).append(message);
            plugin.getAdventure().player(player).sendMessage(fullMessage);
        } else {
            plugin.getAdventure().player(player).sendMessage(message);
        }
    }
    
    /**
     * Отправляет сообщение игроку без префикса
     */
    public void sendMessageWithoutPrefix(Player player, Component message) {
        if (message == null) {
            return;
        }
        
        plugin.getAdventure().player(player).sendMessage(message);
    }
    
    /**
     * Отправляет сообщение игроку с префиксом и заменой плейсхолдеров
     */
    public void sendMessageWithPrefix(Player player, Component baseMessage, TagResolver... resolvers) {
        if (baseMessage == null) {
            return;
        }
        
        Component message = plugin.getConfigManager().getMessageComponents().createWithPlaceholders(baseMessage, resolvers);
        sendMessageWithPrefix(player, message);
    }
    
    /**
     * Отправляет сообщение игроку без префикса и с заменой плейсхолдеров
     */
    public void sendMessageWithoutPrefix(Player player, Component baseMessage, TagResolver... resolvers) {
        if (baseMessage == null) {
            return;
        }
        
        Component message = plugin.getConfigManager().getMessageComponents().createWithPlaceholders(baseMessage, resolvers);
        sendMessageWithoutPrefix(player, message);
    }
    
    // Методы для совместимости со старым API (принимают строки)
    /**
     * @deprecated Используйте методы с Component вместо String
     */
    @Deprecated
    public void sendMessage(Player player, String message) {
        sendMessageWithPrefix(player, message);
    }
    
    /**
     * @deprecated Используйте методы с Component вместо String
     */
    @Deprecated
    public void sendMessageWithPrefix(Player player, String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        
        // Конвертируем legacy и HEX цвета
        String convertedMessage = convertLegacyCodes(message);
        convertedMessage = convertHexColors(convertedMessage);
        
        // Добавляем префикс если это не префикс сам по себе
        String prefix = plugin.getConfigManager().getPrefix();
        if (prefix != null && !convertedMessage.equals(prefix)) {
            // Конвертируем legacy коды в префиксе тоже
            String convertedPrefix = convertLegacyCodes(prefix);
            convertedMessage = convertedPrefix + " <white>" + convertedMessage;
        }
        
        Component component = miniMessage.deserialize(convertedMessage);
        plugin.getAdventure().player(player).sendMessage(component);
    }
    
    /**
     * @deprecated Используйте методы с Component вместо String
     */
    @Deprecated
    public void sendMessageWithoutPrefix(Player player, String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        
        // Конвертируем legacy и HEX цвета
        String convertedMessage = convertLegacyCodes(message);
        convertedMessage = convertHexColors(convertedMessage);
        Component component = miniMessage.deserialize(convertedMessage);
        plugin.getAdventure().player(player).sendMessage(component);
    }
    
    /**
     * @deprecated Используйте методы с Component вместо String
     */
    @Deprecated
    public void sendRawMessageWithPrefix(Player player, String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        
        String prefix = plugin.getConfigManager().getPrefix();
        
        if (prefix != null && !prefix.trim().isEmpty()) {
            // Конвертируем legacy коды в префиксе
            String convertedPrefix = convertLegacyCodes(prefix);
            String fullMessage = convertedPrefix + " <white>" + message;
            Component component = miniMessage.deserialize(fullMessage);
            plugin.getAdventure().player(player).sendMessage(component);
        } else {
            // Отправляем без префикса, но с обработкой цветов
            String convertedMessage = convertLegacyCodes(message);
            convertedMessage = convertHexColors(convertedMessage);
            Component component = miniMessage.deserialize(convertedMessage);
            plugin.getAdventure().player(player).sendMessage(component);
        }
    }
    
    /**
     * Создает TagResolver для замены времени
     */
    public TagResolver timeResolver(int time) {
        return Placeholder.parsed("time", String.valueOf(time));
    }
    
    /**
     * Создает TagResolver для замены названия мира
     */
    public TagResolver worldResolver(String worldName) {
        return Placeholder.parsed("world_name", worldName != null ? worldName : "неизвестный");
    }
    
    /**
     * Создает TagResolver для замены имени
     */
    public TagResolver nameResolver(String name) {
        // Используем unparsed чтобы избежать проблем с парсингом специальных символов
        return Placeholder.unparsed("name", name != null ? name : "");
    }
    
    /**
     * Создает TagResolver для замены имени шалкера
     */
    public TagResolver shulkerNameResolver(String shulkerName) {
        return Placeholder.unparsed("shulker_name", shulkerName != null ? shulkerName : "");
    }
    
    /**
     * Создает TagResolver для замены поддерживаемых форматов
     */
    public TagResolver supportedFormatsResolver(String supportedFormats) {
        return Placeholder.unparsed("supported-formats", supportedFormats != null ? supportedFormats : "");
    }
    
    /**
     * Форматирует имя шалкера с плейсхолдером {shulker_name}
     */
    public String formatShulkerName(String format, String shulkerName) {
        if (format == null) {
            return shulkerName != null ? shulkerName : "";
        }
        return format.replace("{shulker_name}", shulkerName != null ? shulkerName : "");
    }
    
    /**
     * Конвертирует HEX цвета формата &#FFFFFF в MiniMessage формат <#FFFFFF>
     */
    private String convertHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            matcher.appendReplacement(buffer, "<#" + hexColor + ">");
        }
        
        matcher.appendTail(buffer);
        return buffer.toString();
    }
    
    /**
     * Конвертирует legacy & коды в MiniMessage теги
     */
    private String convertLegacyCodes(String message) {
        if (message == null) {
            return "";
        }
        
        return message
            // Цвета
            .replaceAll("&0", "<black>")
            .replaceAll("&1", "<dark_blue>")
            .replaceAll("&2", "<dark_green>")
            .replaceAll("&3", "<dark_aqua>")
            .replaceAll("&4", "<dark_red>")
            .replaceAll("&5", "<dark_purple>")
            .replaceAll("&6", "<gold>")
            .replaceAll("&7", "<gray>")
            .replaceAll("&8", "<dark_gray>")
            .replaceAll("&9", "<blue>")
            .replaceAll("&a", "<green>")
            .replaceAll("&b", "<aqua>")
            .replaceAll("&c", "<red>")
            .replaceAll("&d", "<light_purple>")
            .replaceAll("&e", "<yellow>")
            .replaceAll("&f", "<white>")
            // Форматирование
            .replaceAll("&k", "<obfuscated>")
            .replaceAll("&l", "<bold>")
            .replaceAll("&m", "<strikethrough>")
            .replaceAll("&n", "<underlined>")
            .replaceAll("&o", "<italic>")
            .replaceAll("&r", "<reset>");
    }

    /**
     * Конвертирует HEX цвета формата &#FFFFFF в формат §x§R§R§G§G§B§B
     */
    private String convertHexToSection(String message) {
        if (message == null) return null;
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder sb = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                sb.append('§').append(Character.toUpperCase(c));
            }
            matcher.appendReplacement(buffer, sb.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Конвертирует legacy & коды в §X
     */
    private String convertLegacyToSection(String message) {
        if (message == null) return null;
        return message.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }

    /**
     * Применяет цветовые коды для ItemMeta (поддерживает только legacy коды и HEX)
     * Конвертирует в формат §x§R§R§G§G§B§B и §c
     */
    public String colorizeForItemMeta(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // Конвертируем legacy коды &a -> §a
        String converted = convertLegacyToSection(input);
        
        // Конвертируем hex цвета &#FFFFFF -> §x§f§f§f§f§f§f
        return convertHexToSection(converted);
    }
    
    /**
     * Применяет все цветовые форматы (legacy коды и HEX) к строке для сообщений с поддержкой MiniMessage
     */
    public String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String converted = convertLegacyCodes(input);
        converted = convertHexColors(converted);
        
        // Конвертируем в строку для использования в ItemMeta
        try {
            Component component = miniMessage.deserialize(converted);
            return LegacyComponentSerializer.legacySection().serialize(component);
        } catch (Exception e) {
            // Если не удалось парсить как MiniMessage, возвращаем просто legacy коды
            return input.replaceAll("&([0-9a-fk-o-r])", "§$1");
        }
    }
}
