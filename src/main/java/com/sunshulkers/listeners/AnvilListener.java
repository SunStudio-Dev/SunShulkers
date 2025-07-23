package com.sunshulkers.listeners;

import com.sunshulkers.SunShulkersPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AnvilListener implements Listener {
    
    private final SunShulkersPlugin plugin;
    private final Map<UUID, RenameSession> renameSessions = new HashMap<>();
    private final Map<UUID, Boolean> lastButtonState = new HashMap<>();
    
    public AnvilListener(SunShulkersPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Данные сессии переименования
     */
    private static class RenameSession {
        private final ItemStack originalShulker;
        private final int taskId;
        
        public RenameSession(ItemStack originalShulker, int taskId) {
            this.originalShulker = originalShulker.clone();
            this.taskId = taskId;
        }
        
        public ItemStack getOriginalShulker() {
            return originalShulker;
        }
        
        public int getTaskId() {
            return taskId;
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        // Проверяем что это наковальня
        if (!(event.getInventory() instanceof AnvilInventory)) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Проверяем что система переименования включена
        if (!plugin.getConfigManager().isAnvilEnabled()) {
            return;
        }
        
        AnvilInventory anvil = (AnvilInventory) event.getInventory();
        
        // Проверка клика по результату (слот 2)
        if (event.getSlot() == 2 && event.getCurrentItem() != null) {
            // СНАЧАЛА проверяем кнопку переименования
            if (isRenameButton(event.getCurrentItem())) {
                handleButtonClick(event, player, anvil);
                return;
            }
        }
        

        
        // ЗАЩИТА: Запрещаем любые попытки взять кнопку переименования из других слотов
        if (isRenameButton(event.getCurrentItem())) {
            event.setCancelled(true);
            return;
        }
        
        // PrepareAnvilEvent автоматически обновит кнопку при необходимости
    }
    
    /**
     * Автоматически обновляет кнопку при изменении содержимого наковальни
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        AnvilInventory anvil = event.getInventory();
        
        // Проверяем что система включена
        if (!plugin.getConfigManager().isAnvilEnabled()) {
            return;
        }
        

        
        // Получаем игрока
        if (anvil.getViewers().isEmpty() || !(anvil.getViewers().get(0) instanceof Player)) {
            return;
        }
        
        Player player = (Player) anvil.getViewers().get(0);
        UUID playerId = player.getUniqueId();
        
        // Проверяем права
        if (!player.hasPermission(plugin.getConfigManager().getRenamePermission())) {
            return;
        }
        
        // Проверяем текущее состояние для кнопки переименования
        ItemStack firstItem = anvil.getItem(0);
        ItemStack secondItem = anvil.getItem(1);
        
        // Кнопка должна появляться только если:
        // 1. В первом слоте есть шалкер
        // 2. Второй слот пустой
        boolean shouldHaveButton = firstItem != null && 
                                 isShulkerBox(firstItem.getType()) && 
                                 (secondItem == null || secondItem.getType() == Material.AIR);
        
        if (shouldHaveButton) {
            // Добавляем кнопку в слот результата
            ItemStack button = createRenameButton();
            event.setResult(button);
        }
    }
    
    /**
     * Обработка клика по кнопке переименования
     */
    private void handleButtonClick(InventoryClickEvent event, Player player, AnvilInventory anvil) {
        ItemStack clickedItem = event.getCurrentItem();
        
        // Проверяем что это наша кнопка
        if (!isRenameButton(clickedItem)) {
            return;
        }
        
        // Отменяем стандартное поведение ВСЕГДА для кнопки
        event.setCancelled(true);
        
        // Проверяем права
        if (!player.hasPermission(plugin.getConfigManager().getRenamePermission())) {
            Component noPermMsg = plugin.getConfigManager().getMessageComponents().getNoPermissionMessage();
            if (noPermMsg != null) {
                plugin.getMessageUtils().sendMessageWithPrefix(player, noPermMsg);
            }
            return;
        }
        
        // Получаем шалкер из первого слота
        ItemStack shulkerItem = anvil.getItem(0);
        if (shulkerItem == null || !isShulkerBox(shulkerItem.getType())) {
            return;
        }
        
        // Проверяем что второй слот пустой (для переименования)
        ItemStack secondItem = anvil.getItem(1);
        if (secondItem != null && secondItem.getType() != Material.AIR) {
            return;
        }
        
        // Обрабатываем любой клик как начало переименования
        startRenaming(player, shulkerItem);
    }
    
    /**
     * Начинает процесс переименования
     */
    private void startRenaming(Player player, ItemStack shulkerItem) {
        // Создаем таймаут
        int timeoutTicks = plugin.getConfigManager().getInputTimeout() * 20; // секунды в тики
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                // Таймаут истек
                renameSessions.remove(player.getUniqueId());
                Component timeoutMsg = plugin.getConfigManager().getMessageComponents().getRenameTimeoutMessage();
                if (timeoutMsg != null) {
                    plugin.getMessageUtils().sendMessageWithPrefix(player, timeoutMsg);
                }
            }
        }.runTaskLater(plugin, timeoutTicks).getTaskId();
        
        // Сохраняем сессию
        renameSessions.put(player.getUniqueId(), new RenameSession(shulkerItem, taskId));
        
        // Закрываем наковальню
        player.closeInventory();
        
        // Отправляем сообщение с инструкцией
        Component promptMsg = plugin.getConfigManager().getMessageComponents().getRenamePromptMessage();
        if (promptMsg != null) {
            // Получаем НЕФОРМАТИРОВАННОЕ сообщение о поддерживаемых форматах
            String supportedFormats = plugin.getConfigManager().getRenameSupportedFormatsMessage();
            // Используем TagResolver для замены плейсхолдера
            plugin.getMessageUtils().sendMessageWithPrefix(player, promptMsg, 
                plugin.getMessageUtils().supportedFormatsResolver(supportedFormats));
        }
    }
    
    /**
     * Создает кнопку переименования
     */
    private ItemStack createRenameButton() {
        Material material = plugin.getConfigManager().getButtonMaterial();
        ItemStack button = new ItemStack(material);
        
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            // Устанавливаем имя
            String name = plugin.getConfigManager().getButtonName();
            if (name != null) {
                meta.setDisplayName(plugin.getMessageUtils().colorizeForItemMeta(name));
            }
            
            // Устанавливаем лор
            java.util.List<String> lore = plugin.getConfigManager().getButtonLore();
            if (lore != null && !lore.isEmpty()) {
                java.util.List<String> colorizedLore = new java.util.ArrayList<>();
                for (String line : lore) {
                    colorizedLore.add(plugin.getMessageUtils().colorizeForItemMeta(line));
                }
                meta.setLore(colorizedLore);
            }
            
            // Добавляем зачарование для свечения если нужно
            if (plugin.getConfigManager().isButtonEnchanted()) {
                meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            
            button.setItemMeta(meta);
        }
        
        return button;
    }
    
    /**
     * Проверяет является ли предмет кнопкой переименования
     */
    private boolean isRenameButton(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        // Проверяем материал
        if (item.getType() != plugin.getConfigManager().getButtonMaterial()) {
            return false;
        }
        
        // Проверяем имя
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        
        String expectedName = plugin.getMessageUtils().colorizeForItemMeta(plugin.getConfigManager().getButtonName());
        String actualName = item.getItemMeta().getDisplayName();
        
        return expectedName.equals(actualName);
    }
    
    /**
     * Обработчик чата для ввода имени шалкера
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Проверяем что игрок в процессе переименования
        RenameSession session = renameSessions.get(playerId);
        if (session == null) {
            return;
        }
        
        // Отменяем отправку сообщения в чат
        event.setCancelled(true);
        
        String newName = event.getMessage().trim();
        
        // Проверяем что имя не пустое
        if (newName.isEmpty()) {
            Component cancelMsg = plugin.getConfigManager().getMessageComponents().getRenameCancelledMessage();
            if (cancelMsg != null) {
                plugin.getMessageUtils().sendMessageWithPrefix(player, cancelMsg);
            }
            cleanupSession(playerId);
            return;
        }
        
        // Выполняем переименование в основном потоке
        new BukkitRunnable() {
            @Override
            public void run() {
                performRename(player, session, newName);
                cleanupSession(playerId);
            }
        }.runTask(plugin);
    }
    
    /**
     * Выполняет переименование шалкера
     */
    private void performRename(Player player, RenameSession session, String newName) {
        // Обрабатываем цвета если у игрока есть права
        String processedName = newName;
        if (player.hasPermission(plugin.getConfigManager().getColorPermission())) {
            processedName = plugin.getMessageUtils().colorizeForItemMeta(newName);
        }
        
        // Ищем оригинальный шалкер в инвентаре игрока
        ItemStack originalShulker = session.getOriginalShulker();
        ItemStack foundShulker = findShulkerInInventory(player, originalShulker);
        
        if (foundShulker != null) {
            // Переименовываем найденный шалкер
            ItemMeta meta = foundShulker.getItemMeta();
            if (meta != null) {
                // Используем только §-коды для ItemMeta
                meta.setDisplayName(processedName);
                foundShulker.setItemMeta(meta);
                
                // Отправляем сообщение об успехе
                Component successMsg = plugin.getConfigManager().getMessageComponents().getRenameSuccessMessage();
                if (successMsg != null) {
                    // Используем оригинальное имя (без обработки цветов) для сообщения
                    plugin.getMessageUtils().sendMessageWithPrefix(player, successMsg,
                        plugin.getMessageUtils().nameResolver(newName));
                }
            }
        } else {
            // Шалкер не найден
            Component cancelMsg = plugin.getConfigManager().getMessageComponents().getRenameCancelledMessage();
            if (cancelMsg != null) {
                plugin.getMessageUtils().sendMessageWithPrefix(player, cancelMsg);
            }
        }
    }
    
    /**
     * Ищет шалкер в инвентаре игрока
     */
    private ItemStack findShulkerInInventory(Player player, ItemStack originalShulker) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(originalShulker)) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * Очищает сессию переименования
     */
    private void cleanupSession(UUID playerId) {
        RenameSession session = renameSessions.remove(playerId);
        if (session != null) {
            // Отменяем таймаут
            plugin.getServer().getScheduler().cancelTask(session.getTaskId());
        }
        
        // Очищаем состояние кнопки
        lastButtonState.remove(playerId);
    }
    
    /**
     * Убирает кнопку переименования при закрытии наковальни
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getType() == InventoryType.ANVIL) {
            AnvilInventory anvil = (AnvilInventory) event.getInventory();
            
            // Удаляем кнопку переименования если она есть в слоте результата
            ItemStack resultItem = anvil.getItem(2);
            if (isRenameButton(resultItem)) {
                anvil.setItem(2, null);
            }
            
            // Очищаем состояние кнопки для игрока
            if (event.getPlayer() instanceof Player) {
                lastButtonState.remove(event.getPlayer().getUniqueId());
            }
            
            // Сессия переименования должна продолжаться даже после закрытия наковальни
        }
    }
    
    /**
     * Очистка при выходе игрока
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupSession(event.getPlayer().getUniqueId());
    }
    
    /**
     * Проверяет является ли материал шалкером
     */
    private boolean isShulkerBox(Material material) {
        return material == Material.SHULKER_BOX || material.name().endsWith("_SHULKER_BOX");
    }

    
    /**
     * Получает отображаемое имя предмета
     */
    private String getItemDisplayName(ItemStack item) {
        if (item == null) {
            return "Unknown";
        }
        
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        
        // Преобразуем MATERIAL_NAME в Material Name
        String materialName = item.getType().name();
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder displayName = new StringBuilder();
        
        for (String word : words) {
            if (displayName.length() > 0) {
                displayName.append(" ");
            }
            displayName.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        
        return displayName.toString();
    }
} 