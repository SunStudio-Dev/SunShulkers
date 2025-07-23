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
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class MessageUtils {
    
    private final SunShulkersPlugin plugin;
    private final MiniMessage miniMessage;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern TAG_PATTERN = Pattern.compile("<(/?)(\\w+)(?::([^>]+))?>");
    
    // Minecraft color codes mapping
    private static final Map<String, String> MINECRAFT_COLORS = new HashMap<>();
    private static final Map<String, String> FORMAT_CODES = new HashMap<>();
    private static final Map<String, String> HEX_COLOR_MAP = new HashMap<>();
    
    static {
        // Color codes
        MINECRAFT_COLORS.put("black", "0");
        MINECRAFT_COLORS.put("dark_blue", "1");
        MINECRAFT_COLORS.put("dark_green", "2");
        MINECRAFT_COLORS.put("dark_aqua", "3");
        MINECRAFT_COLORS.put("dark_red", "4");
        MINECRAFT_COLORS.put("dark_purple", "5");
        MINECRAFT_COLORS.put("gold", "6");
        MINECRAFT_COLORS.put("gray", "7");
        MINECRAFT_COLORS.put("grey", "7");
        MINECRAFT_COLORS.put("dark_gray", "8");
        MINECRAFT_COLORS.put("dark_grey", "8");
        MINECRAFT_COLORS.put("blue", "9");
        MINECRAFT_COLORS.put("green", "a");
        MINECRAFT_COLORS.put("aqua", "b");
        MINECRAFT_COLORS.put("red", "c");
        MINECRAFT_COLORS.put("light_purple", "d");
        MINECRAFT_COLORS.put("yellow", "e");
        MINECRAFT_COLORS.put("white", "f");
        
        // Format codes
        FORMAT_CODES.put("bold", "l");
        FORMAT_CODES.put("b", "l");
        FORMAT_CODES.put("italic", "o");
        FORMAT_CODES.put("em", "o");
        FORMAT_CODES.put("i", "o");
        FORMAT_CODES.put("underlined", "n");
        FORMAT_CODES.put("u", "n");
        FORMAT_CODES.put("strikethrough", "m");
        FORMAT_CODES.put("st", "m");
        FORMAT_CODES.put("obfuscated", "k");
        FORMAT_CODES.put("obf", "k");
        
        // Hex representations of named colors
        HEX_COLOR_MAP.put("black", "#000000");
        HEX_COLOR_MAP.put("dark_blue", "#0000AA");
        HEX_COLOR_MAP.put("dark_green", "#00AA00");
        HEX_COLOR_MAP.put("dark_aqua", "#00AAAA");
        HEX_COLOR_MAP.put("dark_red", "#AA0000");
        HEX_COLOR_MAP.put("dark_purple", "#AA00AA");
        HEX_COLOR_MAP.put("gold", "#FFAA00");
        HEX_COLOR_MAP.put("gray", "#AAAAAA");
        HEX_COLOR_MAP.put("dark_gray", "#555555");
        HEX_COLOR_MAP.put("blue", "#5555FF");
        HEX_COLOR_MAP.put("green", "#55FF55");
        HEX_COLOR_MAP.put("aqua", "#55FFFF");
        HEX_COLOR_MAP.put("red", "#FF5555");
        HEX_COLOR_MAP.put("light_purple", "#FF55FF");
        HEX_COLOR_MAP.put("yellow", "#FFFF55");
        HEX_COLOR_MAP.put("white", "#FFFFFF");
    }
    
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
     * Применяет цветовые коды для ItemMeta (поддерживает legacy коды, HEX и полный MiniMessage)
     * Конвертирует в формат §x§R§R§G§G§B§B и §c
     */
    public String colorizeForItemMeta(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // Сначала обрабатываем MiniMessage теги
        String processed = processMiniMessageTags(input);
        
        // Затем конвертируем legacy коды &a -> §a
        processed = convertLegacyToSection(processed);
        
        // И наконец конвертируем hex цвета &#FFFFFF -> §x§f§f§f§f§f§f
        return convertHexToSection(processed);
    }
    
    /**
     * Обрабатывает MiniMessage теги и конвертирует их в legacy формат
     */
    private String processMiniMessageTags(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }
        
        StringBuilder result = new StringBuilder();
        int pos = 0;
        List<String> activeFormats = new ArrayList<>();
        
        while (pos < text.length()) {
            Matcher matcher = TAG_PATTERN.matcher(text.substring(pos));
            if (!matcher.find()) {
                // Нет больше тегов, добавляем оставшийся текст
                result.append(text.substring(pos));
                break;
            }
            
            // Добавляем текст до тега
            result.append(text.substring(pos, pos + matcher.start()));
            
            boolean closing = matcher.group(1).equals("/");
            String tagName = matcher.group(2).toLowerCase();
            String tagArgs = matcher.group(3);
            
            // Обновляем позицию
            pos = pos + matcher.end();
            
            // Обрабатываем различные типы тегов
            if (MINECRAFT_COLORS.containsKey(tagName)) {
                if (!closing) {
                    result.append("§").append(MINECRAFT_COLORS.get(tagName));
                }
            } else if (tagName.equals("color") || tagName.equals("c")) {
                if (!closing && tagArgs != null) {
                    String color = tagArgs.replaceAll("['\"]", "").trim();
                    if (color.startsWith("#")) {
                        result.append(hexToMinecraftHex(color));
                    } else if (MINECRAFT_COLORS.containsKey(color)) {
                        result.append("§").append(MINECRAFT_COLORS.get(color));
                    }
                }
            } else if (FORMAT_CODES.containsKey(tagName)) {
                if (!closing) {
                    result.append("§").append(FORMAT_CODES.get(tagName));
                    activeFormats.add(tagName);
                } else {
                    // Сброс и повторное применение других активных форматов
                    result.append("§r");
                    activeFormats.remove(tagName);
                    for (String fmt : activeFormats) {
                        result.append("§").append(FORMAT_CODES.get(fmt));
                    }
                }
            } else if (tagName.equals("reset")) {
                result.append("§r");
                activeFormats.clear();
            } else if (tagName.equals("gradient")) {
                if (!closing) {
                    // Находим закрывающий тег
                    String closeTag = "</gradient>";
                    int closePos = text.indexOf(closeTag, pos);
                    if (closePos != -1) {
                        String gradientText = text.substring(pos, closePos);
                        // Сначала обрабатываем вложенные теги
                        String processedText = processMiniMessageTags(gradientText);
                        String[] colors = tagArgs != null ? tagArgs.split(":") : new String[0];
                        result.append(applyGradient(processedText, colors, activeFormats));
                        pos = closePos + closeTag.length();
                    }
                }
            } else if (tagName.equals("rainbow")) {
                if (!closing) {
                    // Находим закрывающий тег
                    String closeTag = "</rainbow>";
                    int closePos = text.indexOf(closeTag, pos);
                    if (closePos != -1) {
                        String rainbowText = text.substring(pos, closePos);
                        // Сначала обрабатываем вложенные теги
                        String processedText = processMiniMessageTags(rainbowText);
                        boolean reverse = tagArgs != null && tagArgs.startsWith("!");
                        result.append(applyRainbow(processedText, reverse, activeFormats));
                        pos = closePos + closeTag.length();
                    }
                }
            } else if (tagName.equals("br") || tagName.equals("newline")) {
                result.append("\n");
            }
        }
        
        return result.toString();
    }
    
    /**
     * Применяет градиент к тексту с сохранением форматирования
     */
    private String applyGradient(String text, String[] colors, List<String> activeFormats) {
        if (text.isEmpty() || colors.length == 0) {
            return text;
        }
        
        // Нормализуем цвета
        List<String> normalizedColors = new ArrayList<>();
        for (String color : colors) {
            if (color.startsWith("#")) {
                normalizedColors.add(color);
            } else if (HEX_COLOR_MAP.containsKey(color)) {
                normalizedColors.add(HEX_COLOR_MAP.get(color));
            }
        }
        
        if (normalizedColors.isEmpty()) {
            return text;
        }
        
        // Парсим текст для извлечения чистого текста и форматирования
        List<TextSegment> segments = parseFormattedText(text);
        if (segments.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        int totalChars = 0;
        for (TextSegment segment : segments) {
            totalChars += segment.text.length();
        }
        
        int charIndex = 0;
        for (TextSegment segment : segments) {
            for (int i = 0; i < segment.text.length(); i++) {
                char ch = segment.text.charAt(i);
                
                String color;
                if (normalizedColors.size() == 1) {
                    // Если только один цвет, используем его для всего текста
                    color = normalizedColors.get(0);
                } else {
                    // Вычисляем позицию в градиенте (0.0 - 1.0)
                    float position = (float) charIndex / Math.max(1, totalChars - 1);
                    
                    // Находим между какими цветами интерполировать
                    float segmentSize = 1.0f / (normalizedColors.size() - 1);
                    int segmentIndex = (int) (position / segmentSize);
                    segmentIndex = Math.min(segmentIndex, normalizedColors.size() - 2);
                    
                    // Вычисляем позицию внутри сегмента
                    float localPosition = (position - segmentIndex * segmentSize) / segmentSize;
                    
                    // Интерполируем цвет
                    color = interpolateColor(
                        normalizedColors.get(segmentIndex),
                        normalizedColors.get(segmentIndex + 1),
                        localPosition
                    );
                }
                
                // Применяем цвет и форматирование
                result.append(hexToMinecraftHex(color));
                result.append(segment.formatting);
                result.append(ch);
                
                charIndex++;
            }
        }
        
        return result.toString();
    }
    
    /**
     * Применяет радужный эффект к тексту с сохранением форматирования
     */
    private String applyRainbow(String text, boolean reverse, List<String> activeFormats) {
        if (text.isEmpty()) {
            return text;
        }
        
        // Парсим текст для извлечения чистого текста и форматирования
        List<TextSegment> segments = parseFormattedText(text);
        if (segments.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        int totalChars = 0;
        for (TextSegment segment : segments) {
            totalChars += segment.text.length();
        }
        
        int charIndex = 0;
        for (TextSegment segment : segments) {
            for (int i = 0; i < segment.text.length(); i++) {
                char ch = segment.text.charAt(i);
                
                // Вычисляем позицию в радуге
                float position = (float) charIndex / Math.max(1, totalChars - 1);
                if (reverse) {
                    position = 1.0f - position;
                }
                
                // Конвертируем позицию в оттенок (0-360 градусов)
                float hue = position;
                
                // Конвертируем HSV в RGB
                int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);
                String hexColor = String.format("#%06X", (rgb & 0xFFFFFF));
                
                // Применяем цвет и форматирование
                result.append(hexToMinecraftHex(hexColor));
                result.append(segment.formatting);
                result.append(ch);
                
                charIndex++;
            }
        }
        
        return result.toString();
    }
    
    /**
     * Интерполирует между двумя цветами
     */
    private String interpolateColor(String color1, String color2, float factor) {
        // Конвертируем hex в RGB
        int r1 = Integer.parseInt(color1.substring(1, 3), 16);
        int g1 = Integer.parseInt(color1.substring(3, 5), 16);
        int b1 = Integer.parseInt(color1.substring(5, 7), 16);
        
        int r2 = Integer.parseInt(color2.substring(1, 3), 16);
        int g2 = Integer.parseInt(color2.substring(3, 5), 16);
        int b2 = Integer.parseInt(color2.substring(5, 7), 16);
        
        // Интерполируем
        int r = (int) (r1 + (r2 - r1) * factor);
        int g = (int) (g1 + (g2 - g1) * factor);
        int b = (int) (b1 + (b2 - b1) * factor);
        
        return String.format("#%02x%02x%02x", r, g, b);
    }
    
    /**
     * Конвертирует #RRGGBB в §x§R§R§G§G§B§B
     */
    private String hexToMinecraftHex(String hexColor) {
        hexColor = hexColor.replace("#", "");
        if (hexColor.length() != 6) {
            return "";
        }
        
        StringBuilder result = new StringBuilder("§x");
        for (char c : hexColor.toLowerCase().toCharArray()) {
            result.append("§").append(c);
        }
        return result.toString();
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
    
    /**
     * Класс для хранения сегмента текста с форматированием
     */
    private static class TextSegment {
        String text;
        String formatting;
        
        TextSegment(String text, String formatting) {
            this.text = text;
            this.formatting = formatting;
        }
    }
    
    /**
     * Парсит форматированный текст и извлекает чистый текст с форматированием
     */
    private List<TextSegment> parseFormattedText(String text) {
        List<TextSegment> segments = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        StringBuilder currentFormatting = new StringBuilder();
        
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == '§' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                if (code == 'r') {
                    // Сброс форматирования
                    if (currentText.length() > 0) {
                        segments.add(new TextSegment(currentText.toString(), currentFormatting.toString()));
                        currentText = new StringBuilder();
                    }
                    currentFormatting = new StringBuilder();
                } else if (code == 'x' && i + 13 < text.length()) {
                    // Пропускаем hex цвет (§x§R§R§G§G§B§B)
                    i += 14;
                    continue;
                } else if ("lmnodk".indexOf(code) >= 0) {
                    // Это код форматирования
                    currentFormatting.append("§").append(code);
                } else {
                    // Это обычный цветовой код, пропускаем его
                }
                i += 2;
            } else {
                currentText.append(text.charAt(i));
                i++;
            }
        }
        
        if (currentText.length() > 0) {
            segments.add(new TextSegment(currentText.toString(), currentFormatting.toString()));
        }
        
        return segments;
    }
}
