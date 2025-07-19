package com.sunshulkers.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Хранилище предпарсенных MiniMessage компонентов.
 * Парсинг происходит один раз при загрузке конфига,
 * а затем используется TextReplaceConfig для замены переменных.
 */
public class MessageComponents {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private final MiniMessage miniMessage;
    
    // Общие сообщения
    private Component prefix;
    private Component reloadMessage;
    private Component noPermissionMessage;
    private Component cooldownMessage;
    private Component blacklistedItemMessage;
    private Component blacklistedWorldMessage;
    private Component itemDeletedMessage;
    
    // Сообщения при открытии/закрытии
    private Component openMessage;
    private Component closeMessage;
    
    // Сообщения автосбора
    private Component autocollectEnabledMessage;
    private Component autocollectDisabledMessage;
    private Component autocollectNoSpaceMessage;
    
    // Сообщения переименования
    private Component renamePromptMessage;
    private Component renameSupportedFormatsMessage;
    private Component renameTimeoutMessage;
    private Component renameSuccessMessage;
    private Component renameInvalidMessage;
    private Component renameTooltipMessage;
    private Component renameCancelledMessage;
    // Сообщение при закрытии GUI по атаке
    private Component closeOnAttackMessage;
    // Сообщение при попытке выбросить открытый шалкер
    private Component cannotDropOpenShulker;
    
    public MessageComponents() {
        this.miniMessage = MiniMessage.miniMessage();
    }
    
    /**
     * Инициализирует все компоненты из строк конфига
     */
    public void initializeFromStrings(
            String prefix,
            String reloadMessage,
            String noPermissionMessage,
            String cooldownMessage,
            String blacklistedItemMessage,
            String blacklistedWorldMessage,
            String itemDeletedMessage,
            String openMessage,
            String closeMessage,
            String autocollectEnabledMessage,
            String autocollectDisabledMessage,
            String autocollectNoSpaceMessage,
            String renamePromptMessage,
            String renameSupportedFormatsMessage,
            String renameTimeoutMessage,
            String renameSuccessMessage,
            String renameInvalidMessage,
            String renameTooltipMessage,
            String renameCancelledMessage,
            String closeOnAttackMessage,
            String cannotDropOpenShulker
    ) {
        this.prefix = parseComponent(prefix);
        this.reloadMessage = parseComponent(reloadMessage);
        this.noPermissionMessage = parseComponent(noPermissionMessage);
        this.cooldownMessage = parseComponent(cooldownMessage);
        this.blacklistedItemMessage = parseComponent(blacklistedItemMessage);
        this.blacklistedWorldMessage = parseComponent(blacklistedWorldMessage);
        this.itemDeletedMessage = parseComponent(itemDeletedMessage);
        this.openMessage = parseComponent(openMessage);
        this.closeMessage = parseComponent(closeMessage);
        this.autocollectEnabledMessage = parseComponent(autocollectEnabledMessage);
        this.autocollectDisabledMessage = parseComponent(autocollectDisabledMessage);
        this.autocollectNoSpaceMessage = parseComponent(autocollectNoSpaceMessage);
        this.renamePromptMessage = parseComponent(renamePromptMessage);
        this.renameSupportedFormatsMessage = parseComponent(renameSupportedFormatsMessage);
        this.renameTimeoutMessage = parseComponent(renameTimeoutMessage);
        this.renameSuccessMessage = parseComponent(renameSuccessMessage);
        this.renameInvalidMessage = parseComponent(renameInvalidMessage);
        this.renameTooltipMessage = parseComponent(renameTooltipMessage);
        this.renameCancelledMessage = parseComponent(renameCancelledMessage);
        // close-on-attack message
        this.closeOnAttackMessage = parseComponent(closeOnAttackMessage);
        this.cannotDropOpenShulker = parseComponent(cannotDropOpenShulker);
    }
    
    /**
     * Парсит строку в Component с обработкой legacy кодов и HEX цветов
     */
    private Component parseComponent(String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }
        
        String converted = convertLegacyCodes(message);
        converted = convertHexColors(converted);
        // НЕ конвертируем плейсхолдеры здесь - оставляем {placeholder} как есть
        
        return miniMessage.deserialize(converted);
    }

    /**
     * Создает компонент с замененными плейсхолдерами
     */
    public Component createWithPlaceholders(Component baseComponent, TagResolver... resolvers) {
        if (baseComponent == null) {
            return null;
        }
        
        // Если нет резолверов, возвращаем оригинальный компонент
        if (resolvers.length == 0) {
            return baseComponent;
        }
        
        // Сериализуем компонент обратно в строку
        String serialized = miniMessage.serialize(baseComponent);
        
        // Конвертируем {placeholder} в <placeholder> для MiniMessage
        String converted = convertPlaceholders(serialized);
        
        // Парсим с резолверами
        return miniMessage.deserialize(converted, resolvers);
    }

    /**
     * Конвертирует плейсхолдеры {placeholder} в MiniMessage формат <placeholder>
     */
    private String convertPlaceholders(String message) {
        if (message == null) {
            return "";
        }
        
        return message
            .replaceAll("\\{time\\}", "<time>")
            .replaceAll("\\{world_name\\}", "<world_name>")
            .replaceAll("\\{name\\}", "<name>")
            .replaceAll("\\{shulker_name\\}", "<shulker_name>")
            .replaceAll("\\{supported-formats\\}", "<supported-formats>");
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
    public static String convertHexToSection(String message) {
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
    public static String convertLegacyToSection(String message) {
        if (message == null) return null;
        return message.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }

    // Геттеры для всех компонентов
    public Component getPrefix() { return prefix; }
    public Component getReloadMessage() { return reloadMessage; }
    public Component getNoPermissionMessage() { return noPermissionMessage; }
    public Component getCooldownMessage() { return cooldownMessage; }
    public Component getBlacklistedItemMessage() { return blacklistedItemMessage; }
    public Component getBlacklistedWorldMessage() { return blacklistedWorldMessage; }
    public Component getItemDeletedMessage() { return itemDeletedMessage; }
    public Component getOpenMessage() { return openMessage; }
    public Component getCloseMessage() { return closeMessage; }
    public Component getAutocollectEnabledMessage() { return autocollectEnabledMessage; }
    public Component getAutocollectDisabledMessage() { return autocollectDisabledMessage; }
    public Component getAutocollectNoSpaceMessage() { return autocollectNoSpaceMessage; }
    public Component getRenamePromptMessage() { return renamePromptMessage; }
    public Component getRenameSupportedFormatsMessage() { return renameSupportedFormatsMessage; }
    public Component getRenameTimeoutMessage() { return renameTimeoutMessage; }
    public Component getRenameSuccessMessage() { return renameSuccessMessage; }
    public Component getRenameInvalidMessage() { return renameInvalidMessage; }
    public Component getRenameTooltipMessage() { return renameTooltipMessage; }
    public Component getRenameCancelledMessage() { return renameCancelledMessage; }
    /**
     * Получает сообщение при закрытии GUI по атаке
     */
    public Component getCloseOnAttackMessage() { return closeOnAttackMessage; }
    
    /**
     * Получает сообщение при попытке выбросить открытый шалкер
     */
    public Component getCannotDropOpenShulker() { return cannotDropOpenShulker; }
} 