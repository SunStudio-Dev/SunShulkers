package com.sunshulkers.listeners;

import com.sunshulkers.SunShulkersPlugin;
import com.sunshulkers.utils.OpenMode;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShulkerListener implements Listener {
    
    private final SunShulkersPlugin plugin;
    // Отслеживаем какие игроки открыли шалкеры через плагин
    private final Map<UUID, ShulkerData> openedShulkers = new HashMap<>();
    // Уникальный namespace key для отслеживания шалкеров
    private final NamespacedKey shulkerIdKey;
    
    // Класс для хранения данных об открытом шалкере
    private static class ShulkerData {
        final ItemStack originalItem;
        final UUID shulkerId;
        final long openTime;
        final int originalSlot; // Слот в инвентаре игрока где был шалкер
        
        ShulkerData(ItemStack originalItem, UUID shulkerId, int originalSlot) {
            this.originalItem = originalItem.clone();
            this.shulkerId = shulkerId;
            this.openTime = System.currentTimeMillis();
            this.originalSlot = originalSlot;
        }
    }
    
    public ShulkerListener(SunShulkersPlugin plugin) {
        this.plugin = plugin;
        this.shulkerIdKey = new NamespacedKey(plugin, "shulker_id");
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        // КРИТИЧЕСКАЯ ЗАЩИТА ОТ ДЮПА: Проверяем действия когда открыт шалкер
        if (openedShulkers.containsKey(playerId)) {
            ShulkerData shulkerData = openedShulkers.get(playerId);
            
            // Проверяем, что клик происходит в открытом инвентаре шалкера
            boolean clickInShulkerInventory = event.getClickedInventory() != null && 
                                            event.getClickedInventory() != player.getInventory();
            
            // ЗАЩИТА ОТ ВСЕХ ВИДОВ ПОМЕЩЕНИЯ ШАЛКЕРОВ В ШАЛКЕР
            if (clickInShulkerInventory) {
                // Получаем открытый шалкер для проверки
                ItemStack shulkerItem = shulkerData.originalItem;
                
                // Блокируем помещение шалкеров через числовые клавиши
                if (event.getClick() == ClickType.NUMBER_KEY) {
                    int hotbarButton = event.getHotbarButton();
                    if (hotbarButton >= 0 && hotbarButton <= 8) {
                        ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                        if (isShulkerBox(hotbarItem)) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
                
                // Блокируем любые действия с шалкерами в инвентаре шалкера
                if (isShulkerBox(clickedItem) || isShulkerBox(cursorItem)) {
                    event.setCancelled(true);
                    return;
                }
                
                // Блокируем swap с шалкером
                if (event.getAction() == InventoryAction.HOTBAR_SWAP || 
                    event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                    int hotbarButton = event.getHotbarButton();
                    if (hotbarButton >= 0 && hotbarButton <= 8) {
                        ItemStack swapItem = player.getInventory().getItem(hotbarButton);
                        if (isShulkerBox(swapItem)) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
                
                // ПОЛНАЯ ЗАЩИТА ОТ OFFHAND: Блокируем ЛЮБЫЕ действия если в offhand есть шалкер или blacklist предмет
                ItemStack offhandItem = player.getInventory().getItemInOffHand();
                if (offhandItem != null && offhandItem.getType() != Material.AIR) {
                    // Блокируем ВСЕ действия помещения предметов в шалкер если в offhand что-то есть
                    if (event.getClick() == ClickType.SWAP_OFFHAND || 
                        (event.getClick().isKeyboardClick() && event.getHotbarButton() == 40)) { // 40 = F key
                        event.setCancelled(true);
                        return;
                    }
                    
                    // Если в offhand запрещенный предмет или шалкер - блокируем
                    if (isShulkerBox(offhandItem) || isBlacklistedItem(player, offhandItem)) {
                        event.setCancelled(true);
                        if (isBlacklistedItem(player, offhandItem)) {
                            handleBlacklistedItem(player, offhandItem);
                        }
                        return;
                    }
                }
                
                // Проверяем blacklist для курсора
                if (isBlacklistedItem(player, cursorItem)) {
                    event.setCancelled(true);
                    handleBlacklistedItem(player, cursorItem);
                    return;
                }
                

            }
            
            // Проверка SHIFT+CLICK из инвентаря игрока в шалкер
            if (event.isShiftClick() && event.getClickedInventory() == player.getInventory()) {
                ItemStack openShulker = shulkerData.originalItem;
                    }
                }
            }
            
            // Блокируем перемещение шалкеров в открытый шалкер (но разрешаем перемещение по инвентарю игрока)
            if (isShulkerBox(clickedItem) && event.getClickedInventory() == player.getInventory()) {
                // Проверяем, пытается ли игрок переместить шалкер в открытый инвентарь
                if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                    (event.getClick().isShiftClick() && clickInShulkerInventory)) {
                    event.setCancelled(true);
                    return;
                }
                
                // Блокируем клики по открытому шалкеру
                if (isSameShulker(clickedItem, shulkerData.originalItem)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        
        // ПРОВЕРКА ОТКРЫТИЯ ШАЛКЕРА
        if (isShulkerBox(clickedItem)) {
            // Проверяем клик: если требуется Shift, то Shift + ПКМ, иначе просто ПКМ
            List<OpenMode> inventoryModes = plugin.getConfigManager().getInventoryModes();
            boolean isValidClick = false;
            
            // Проверяем, соответствует ли клик одному из разрешенных режимов
            for (OpenMode mode : inventoryModes) {
                if (mode.matchesInventoryClick(event.getClick())) {
                    isValidClick = true;
                    break;
                }
            }
            
            // Если это НЕ попытка открыть шалкер - разрешаем действие (перемещение, взятие и т.д.)
            if (!isValidClick) {
                return;
            }
            
            // ТЕПЕРЬ проверяем ограничения только для ОТКРЫТИЯ шалкера
            
            // Проверяем права
            if (!player.hasPermission("sunshulkers.use")) {
                Component noPermMsg = plugin.getConfigManager().getMessageComponents().getNoPermissionMessage();
                if (noPermMsg != null) {
                    plugin.getMessageUtils().sendMessageWithPrefix(player, noPermMsg);
                }
                event.setCancelled(true);
                return;
            }
            
            // Проверяем мир
            if (plugin.getConfigManager().getBlacklistedWorlds().contains(player.getWorld().getName())) {
                Component blacklistedWorldMsg = plugin.getConfigManager().getMessageComponents().getBlacklistedWorldMessage();
                if (blacklistedWorldMsg != null) {
                    plugin.getMessageUtils().sendMessageWithPrefix(player, blacklistedWorldMsg,
                        plugin.getMessageUtils().worldResolver(player.getWorld().getName()));
                }
                return;
            }
            
            // Проверяем кулдаун (если у игрока нет права на обход)
            if (!player.hasPermission("sunshulkers.bypass") && 
                plugin.getCooldownManager().hasCooldown(player)) {
                
                int remaining = plugin.getCooldownManager().getRemainingCooldown(player);
                Component cooldownMsg = plugin.getConfigManager().getMessageComponents().getCooldownMessage();
                
                if (cooldownMsg != null) {
                    plugin.getMessageUtils().sendMessageWithPrefix(player, cooldownMsg,
                        plugin.getMessageUtils().timeResolver(remaining));
                }
                
                event.setCancelled(true);
                return;
            }
            
            // Отменяем стандартное действие и открываем шалкер
            event.setCancelled(true);
            openShulkerBox(player, clickedItem);
        }
    }
    
    /**
     * НОВЫЙ ОБРАБОТЧИК: Блокируем F в открытых инвентарях
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClickF(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Блокируем F клавишу (swap с offhand) во ВСЕХ шалкерах
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            // Проверяем, что открыт какой-либо шалкер
            if (event.getInventory().getType() == InventoryType.SHULKER_BOX || 
                openedShulkers.containsKey(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    // НОВЫЙ ОБРАБОТЧИК: Блокируем swap hands (F клавиша) с шалкерами когда открыт шалкер
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Если у игрока открыт шалкер - ВСЕГДА блокируем смену рук
        if (openedShulkers.containsKey(playerId)) {
            event.setCancelled(true);
            return;
        }
    }
    
    // Обработка взаимодействия с шалкером в руке
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        ItemStack item = event.getItem();
        
        // Проверяем, что игрок держит шалкер
        if (!isShulkerBox(item)) {
            return;
        }
        

        
        // КРИТИЧЕСКАЯ ЗАЩИТА: Если у игрока уже открыт шалкер через наш плагин, 
        // запрещаем взаимодействие с другими шалкерами
        if (openedShulkers.containsKey(playerId)) {
            event.setCancelled(true);
            return;
        }
        
        // Проверяем действие: клик по воздуху или по блоку
        if (event.getAction() != Action.RIGHT_CLICK_AIR && 
            event.getAction() != Action.RIGHT_CLICK_BLOCK &&
            event.getAction() != Action.LEFT_CLICK_AIR &&
            event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        // УМНОЕ РАЗМЕЩЕНИЕ: проверяем настройку и на что смотрит игрок
        if (plugin.getConfigManager().isSmartPlacement()) {
            Block targetBlock = player.getTargetBlockExact(5); // Проверяем в радиусе 5 блоков
            
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && targetBlock != null) {
                // Игрок смотрит на блок и кликает ПКМ - позволяем размещение
                return;
            }
            
            if (event.getAction() == Action.LEFT_CLICK_BLOCK && targetBlock != null) {
                // Игрок смотрит на блок и кликает ЛКМ - позволяем размещение (ломание)
                return;
            }
            
            // Если игрок смотрит в воздух или настройка требует открытия - продолжаем обработку
        }
        
        // Проверяем, соответствует ли действие одному из разрешенных режимов
        List<OpenMode> handModes = plugin.getConfigManager().getHandModes();
        boolean isValidAction = false;
        
        for (OpenMode mode : handModes) {
            if (mode.matchesHandAction(event.getAction(), player.isSneaking())) {
                isValidAction = true;
                break;
            }
        }
        
        if (!isValidAction) {
            return;
        }
        
        // Проверяем права
        if (!player.hasPermission("sunshulkers.use")) {
            String noPermMsg = plugin.getConfigManager().getNoPermissionMessage();
            if (noPermMsg != null) {
                plugin.getMessageUtils().sendMessageWithPrefix(player, noPermMsg);
            }
            return;
        }
        
        // Проверяем мир
        if (plugin.getConfigManager().getBlacklistedWorlds().contains(player.getWorld().getName())) {
            Component blacklistedWorldMsg = plugin.getConfigManager().getMessageComponents().getBlacklistedWorldMessage();
            if (blacklistedWorldMsg != null) {
                plugin.getMessageUtils().sendMessageWithPrefix(player, blacklistedWorldMsg,
                    plugin.getMessageUtils().worldResolver(player.getWorld().getName()));
            }
            return;
        }
        
        // Проверяем кулдаун
        if (!player.hasPermission("sunshulkers.bypass") && 
            plugin.getCooldownManager().hasCooldown(player)) {
            
            int remaining = plugin.getCooldownManager().getRemainingCooldown(player);
            Component cooldownMsg = plugin.getConfigManager().getMessageComponents().getCooldownMessage();
            
            if (cooldownMsg != null) {
                plugin.getMessageUtils().sendMessageWithPrefix(player, cooldownMsg,
                    plugin.getMessageUtils().timeResolver(remaining));
            }
            
            event.setCancelled(true);
            return;
        }
        
        // Отменяем стандартное действие
        event.setCancelled(true);
        
        // Открываем шалкер
        openShulkerBox(player, item);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Проверяем, что закрывается шалкер, открытый через наш плагин
        if (openedShulkers.containsKey(playerId)) {
            
            // Получаем оригинальный предмет шалкера
            ShulkerData shulkerData = openedShulkers.get(playerId);
            
            // ВАЖНО: Сохраняем изменения обратно в предмет
            if (shulkerData != null) {
                // Финальное сохранение при закрытии
                saveShulkerContents(player, shulkerData.originalItem, event.getInventory());
            }
            
            // Безопасно очищаем данные игрока
            cleanupPlayerData(playerId);
            
            // Играем звук закрытия
            Sound closeSound = plugin.getConfigManager().getCloseSound();
            if (closeSound != null) {
                player.playSound(player.getLocation(), closeSound, 1.0f, 1.0f);
            }
            
            // Отправляем сообщение о закрытии
            Component closeMessage = plugin.getConfigManager().getMessageComponents().getCloseMessage();
            if (closeMessage != null) {
                plugin.getMessageUtils().sendMessageWithPrefix(player, closeMessage);
            }
            
            // ВАЖНО: Устанавливаем кулдаун при закрытии шалкера
            if (!player.hasPermission("sunshulkers.bypass")) {
                int cooldownTime = plugin.getConfigManager().getCooldown();
                plugin.getCooldownManager().setCooldown(player, cooldownTime);
                
                // Показываем визуальный кулдаун если включено
                if (plugin.getConfigManager().isReloadingItemEnabled() && shulkerData != null) {
                    showItemCooldown(player, shulkerData.originalItem.getType(), cooldownTime);
                }
            }
        }
    }
    
    /**
     * Обработчик кликов в ОБЫЧНЫХ шалкерах для проверки blacklist
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onRegularShulkerClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        
        // Если это шалкер открытый через наш плагин - пропускаем (обрабатывается в другом методе)
        if (openedShulkers.containsKey(playerId)) {
            return;
        }
        
        // Проверяем, что это обычный шалкер
        if (event.getInventory().getType() != InventoryType.SHULKER_BOX) {
            return;
        }
        
        // БЛОКИРУЕМ SWAP С OFFHAND В ОБЫЧНЫХ ШАЛКЕРАХ
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
            return;
        }
        
        // Получаем предметы для проверки
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        ItemStack offhandItem = player.getInventory().getItemInOffHand();
        
        // ЗАЩИТА: Запрещаем помещение шалкеров внутрь шалкера
        if (isShulkerBox(clickedItem) || isShulkerBox(cursorItem) || isShulkerBox(offhandItem)) {
            event.setCancelled(true);
            return;
        }
        
        // ПРОВЕРКА НА ЗАПРЕЩЕННЫЕ ПРЕДМЕТЫ: Проверяем предмет в руках игрока
        if (isBlacklistedItem(player, cursorItem)) {
            event.setCancelled(true);
            handleBlacklistedItem(player, cursorItem);
            return;
        }
        
        // ПРОВЕРКА НА ЗАПРЕЩЕННЫЕ ПРЕДМЕТЫ: Проверяем offhand
        if (isBlacklistedItem(player, offhandItem)) {
            event.setCancelled(true);
            handleBlacklistedItem(player, offhandItem);
            return;
        }
        
        // Также проверяем предмет, на который кликают (в случае swap или hotbar)
        if (isBlacklistedItem(player, clickedItem)) {
            // Проверяем действия, которые могут поместить предмет в шалкер
            switch (event.getAction()) {
                case PLACE_ALL:
                case PLACE_ONE:
                case PLACE_SOME:
                case SWAP_WITH_CURSOR:
                case HOTBAR_SWAP:
                case HOTBAR_MOVE_AND_READD:
                case MOVE_TO_OTHER_INVENTORY:
                case COLLECT_TO_CURSOR:
                    event.setCancelled(true);
                    handleBlacklistedItem(player, clickedItem);
                    return;
                default:
                    break;
            }
        }
        
        // Дополнительная проверка для числовых клавиш (1-9)
        if (event.getAction() == InventoryAction.HOTBAR_SWAP || 
            event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
            
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0 && hotbarButton <= 8) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                if (isBlacklistedItem(player, hotbarItem)) {
                    event.setCancelled(true);
                    handleBlacklistedItem(player, hotbarItem);
                    return;
                }
            }
        }
    }
    
    /**
     * Обработчик перетаскивания в ОБЫЧНЫХ шалкерах для проверки blacklist
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onRegularShulkerDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        
        // Если это шалкер открытый через наш плагин - пропускаем (обрабатывается в другом методе)
        if (openedShulkers.containsKey(playerId)) {
            return;
        }
        
        // Проверяем, что это обычный шалкер
        if (event.getInventory().getType() != InventoryType.SHULKER_BOX) {
            return;
        }
        
        // ЗАЩИТА: Запрещаем перетаскивание шалкеров внутрь шалкера
        ItemStack draggedItem = event.getOldCursor();
        
        if (isShulkerBox(draggedItem)) {
            event.setCancelled(true);
            return;
        }
        
        // ПРОВЕРКА НА ЗАПРЕЩЕННЫЕ ПРЕДМЕТЫ: Проверяем перетаскиваемый предмет
        // ТОЛЬКО если перетаскиваем в область шалкера
        boolean dragIntoShulker = false;
        for (Integer slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                dragIntoShulker = true;
                break;
            }
        }
        
        if (dragIntoShulker && isBlacklistedItem(player, draggedItem)) {
            event.setCancelled(true);
            handleBlacklistedItem(player, draggedItem);
            return;
        }
    }
    
    /**
     * Обработчик Shift+click в ОБЫЧНЫХ шалкерах для проверки blacklist
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onRegularShulkerShiftClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        
        // Если это шалкер открытый через наш плагин - пропускаем (обрабатывается в другом методе)
        if (openedShulkers.containsKey(playerId)) {
            return;
        }
        
        // Проверяем что это Shift+click и один из инвентарей - шалкер
        if (!event.getClick().isShiftClick()) {
            return;
        }
        
        boolean topIsShulker = event.getView().getTopInventory().getType() == InventoryType.SHULKER_BOX;
        boolean bottomIsShulker = event.getView().getBottomInventory().getType() == InventoryType.SHULKER_BOX;
        
        if (!topIsShulker && !bottomIsShulker) {
            return;
        }
        
        // Проверяем, что Shift+click по шалкеру в инвентаре игрока (для предотвращения помещения шалкера в шалкер)
        if (event.getClickedInventory() == player.getInventory() &&
            isShulkerBox(event.getCurrentItem())) {
            
            event.setCancelled(true);
            return;
        }
        
        // ПРОВЕРКА НА ЗАПРЕЩЕННЫЕ ПРЕДМЕТЫ: Проверяем Shift+click по запрещенному предмету
        if (event.getClickedInventory() == player.getInventory() &&
            isBlacklistedItem(player, event.getCurrentItem())) {
            
            event.setCancelled(true);
            handleBlacklistedItem(player, event.getCurrentItem());
            return;
        }
    }
    
    /**
     * Обработчик изменений в инвентаре шалкера - сохраняет изменения в реальном времени
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onShulkerInventoryChange(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        
        // Проверяем, что это инвентарь шалкера, открытого через наш плагин
        if (!openedShulkers.containsKey(playerId)) {
            return;
        }
        
        // КРИТИЧЕСКАЯ ЗАЩИТА: Блокируем все действия с шалкерами в открытом инвентаре
        if (event.getClick() == ClickType.NUMBER_KEY) {
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0 && hotbarButton <= 8) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                if (isShulkerBox(hotbarItem)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        
        // Не обрабатываем отмененные события далее
        if (event.isCancelled()) {
            return;
        }
        
        // ДОПОЛНИТЕЛЬНАЯ ЗАЩИТА: Запрещаем помещение шалкеров внутрь открытого шалкера
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        ItemStack offhandItem = player.getInventory().getItemInOffHand();
        
        // БЛОКИРУЕМ SWAP С OFFHAND ПОЛНОСТЬЮ
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
            return;
        }
        
        if (isShulkerBox(clickedItem) || isShulkerBox(cursorItem) || isShulkerBox(offhandItem)) {
            // Отменяем операцию если пытаются поместить шалкер в шалкер
            event.setCancelled(true);
            return;
        }
        
        // ПРОВЕРКА НА ЗАПРЕЩЕННЫЕ ПРЕДМЕТЫ: Проверяем предмет в руках игрока
        if (isBlacklistedItem(player, cursorItem)) {
            event.setCancelled(true);
            handleBlacklistedItem(player, cursorItem);
            return;
        }
        
        // ПРОВЕРКА НА ЗАПРЕЩЕННЫЕ ПРЕДМЕТЫ: Проверяем offhand
        if (isBlacklistedItem(player, offhandItem)) {
            event.setCancelled(true);
            handleBlacklistedItem(player, offhandItem);
            return;
        }
        
        // Также проверяем предмет, на который кликают (в случае swap или hotbar)
        if (isBlacklistedItem(player, clickedItem)) {
            // Проверяем действия, которые могут поместить предмет в шалкер
            switch (event.getAction()) {
                case PLACE_ALL:
                case PLACE_ONE:
                case PLACE_SOME:
                case SWAP_WITH_CURSOR:
                case HOTBAR_SWAP:
                case HOTBAR_MOVE_AND_READD:
                case MOVE_TO_OTHER_INVENTORY:
                case COLLECT_TO_CURSOR:
                    event.setCancelled(true);
                    handleBlacklistedItem(player, clickedItem);
                    return;
                default:
                    break;
            }
        }
        
        // Дополнительная проверка для числовых клавиш (1-9)
        if (event.getAction() == InventoryAction.HOTBAR_SWAP || 
            event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
            
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0 && hotbarButton <= 8) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                if (isBlacklistedItem(player, hotbarItem) || isShulkerBox(hotbarItem)) {
                    event.setCancelled(true);
                    if (isBlacklistedItem(player, hotbarItem)) {
                        handleBlacklistedItem(player, hotbarItem);
                    }
                    return;
                }
            }
        }
        
        // Получаем оригинальный предмет шалкера
        ShulkerData shulkerData = openedShulkers.get(playerId);
        if (shulkerData == null) {
            return;
        }
        
        // НЕМЕДЛЕННОЕ СОХРАНЕНИЕ: Сохраняем изменения сразу же (без задержки)
        // Используем инвентарь из топ-уровня view, а не event.getInventory()
        Inventory topInventory = player.getOpenInventory().getTopInventory();
        saveShulkerContentsImmediate(player, shulkerData.originalItem, topInventory);
    }
    
    /**
     * Обработчик открытия ОБЫЧНЫХ шалкеров (размещенных на земле) - проверяем blacklist
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRegularShulkerOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // Проверяем, что это инвентарь шалкера (обычного, не через наш плагин)
        if (event.getInventory().getType() != InventoryType.SHULKER_BOX) {
            return;
        }
        
        // Если это шалкер открытый через наш плагин - не обрабатываем
        if (openedShulkers.containsKey(player.getUniqueId())) {
            return;
        }
        
        // Проверяем права на bypass - если есть, то разрешаем всё
        if (player.hasPermission("sunshulkers.bypass")) {
            return;
        }
        
        // Больше ничего не делаем - обработка blacklist происходит в отдельных обработчиках кликов
    }
    
    /**
     * Обработчик перетаскивания предметов в инвентаре шалкера
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onShulkerInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        // Не обрабатываем отмененные события
        if (event.isCancelled()) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        
        // Проверяем, что это инвентарь шалкера, открытого через наш плагин
        if (!openedShulkers.containsKey(playerId)) {
            return;
        }
        
        // ДОПОЛНИТЕЛЬНАЯ ЗАЩИТА: Запрещаем перетаскивание шалкеров внутрь открытого шалкера
        ItemStack oldCursorItem = event.getOldCursor();
        
        if (isShulkerBox(oldCursorItem)) {
            // Отменяем операцию если пытаются перетащить шалкер в шалкер
            event.setCancelled(true);
            return;
        }
        
        // ПРОВЕРКА НА ЗАПРЕЩЕННЫЕ ПРЕДМЕТЫ: Проверяем перетаскиваемый предмет
        // ТОЛЬКО если перетаскиваем в область шалкера
        boolean dragIntoShulker = false;
        for (Integer slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                dragIntoShulker = true;
                break;
            }
        }
        
        if (dragIntoShulker && isBlacklistedItem(player, oldCursorItem)) {
            event.setCancelled(true);
            handleBlacklistedItem(player, oldCursorItem);
            return;
        }
        
        // Получаем оригинальный предмет шалкера
        ShulkerData shulkerData = openedShulkers.get(playerId);
        if (shulkerData == null) {
            return;
        }
        
        // НЕМЕДЛЕННОЕ СОХРАНЕНИЕ: Сохраняем изменения сразу же (без задержки)
        // Используем топ инвентарь
        Inventory topInventory = player.getOpenInventory().getTopInventory();
        saveShulkerContentsImmediate(player, shulkerData.originalItem, topInventory);
    }
    
    /**
     * Дополнительный обработчик для предотвращения Shift+click перемещения шалкеров
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onShulkerShiftClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID playerId = player.getUniqueId();
        
        // Проверяем, что у игрока открыт шалкер через наш плагин
        if (!openedShulkers.containsKey(playerId)) {
            return;
        }
        
        // Проверяем только Shift+click
        if (!event.getClick().isShiftClick()) {
            return;
        }
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) {
            return;
        }
        
        // Проверяем, что кликаем в инвентаре игрока и хотим переместить в шалкер
        if (event.getClickedInventory() == player.getInventory()) {
            // Блокируем перемещение шалкера в открытый шалкер
            if (isShulkerBox(clickedItem)) {
                event.setCancelled(true);
                return;
            }
            
            // Блокируем перемещение запрещенных предметов
            if (isBlacklistedItem(player, clickedItem)) {
                event.setCancelled(true);
                handleBlacklistedItem(player, clickedItem);
                return;
            }
        }
    }
    
    /**
     * Обработчик выхода игрока - очищаем данные для предотвращения утечек памяти
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cleanupPlayerData(playerId);
    }
    
    /**
     * Обработчик выбрасывания предметов - предотвращаем выбрасывание открытого шалкера
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Проверяем, есть ли у игрока открытый шалкер
        if (!openedShulkers.containsKey(playerId)) {
            return;
        }
        
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        
        // Проверяем, что выбрасывается шалкер
        if (!isShulkerBox(droppedItem)) {
            return;
        }
        
        ShulkerData shulkerData = openedShulkers.get(playerId);
        if (shulkerData != null && isSameShulker(droppedItem, shulkerData.originalItem)) {
            // Игрок пытается выбросить открытый шалкер - запрещаем
            event.setCancelled(true);
            
            Component msg = plugin.getConfigManager().getMessageComponents().getCannotDropOpenShulker();
            if (msg == null) {
                // Если сообщение не настроено, используем дефолтное
                msg = Component.text("§cВы не можете выбросить открытый шалкер!");
            }
            plugin.getMessageUtils().sendMessageWithPrefix(player, msg);
        }
    }
    
    /**
     * Закрывает интерфейс шалкера при атаке игрока, если включено в настройках
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getDamager();
        if (!plugin.getConfigManager().isCloseOnAttack()) {
            return;
        }
        if (!openedShulkers.containsKey(player.getUniqueId())) {
            return;
        }
        player.closeInventory();
        Component msg = plugin.getConfigManager().getMessageComponents().getCloseOnAttackMessage();
        if (msg != null) {
            plugin.getMessageUtils().sendMessageWithPrefix(player, msg);
        }
    }

    private boolean isShulkerBox(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        return item.getType().name().contains("SHULKER_BOX");
    }

    
    private String getItemDisplayName(Material material) {
        // Преобразуем MATERIAL_NAME в Material Name
        String materialName = material.name();
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
    
    private void openShulkerBox(Player player, ItemStack shulkerItem) {
        if (!(shulkerItem.getItemMeta() instanceof BlockStateMeta)) {
            return;
        }
        
        // ДОПОЛНИТЕЛЬНАЯ ЗАЩИТА: Проверяем, что у игрока уже нет открытого шалкера
        UUID playerId = player.getUniqueId();
        if (openedShulkers.containsKey(playerId)) {
            // Игрок уже имеет открытый шалкер - не открываем новый
            return;
        }
        
        BlockStateMeta meta = (BlockStateMeta) shulkerItem.getItemMeta();
        if (!(meta.getBlockState() instanceof ShulkerBox)) {
            return;
        }
        
        ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
        
        // Получаем имя шалкера из конфига
        String shulkerName = getShulkerDisplayName(shulkerItem);
        
        // Находим позицию шалкера в инвентаре игрока
        int shulkerPosition = findShulkerPosition(player, shulkerItem);
        if (shulkerPosition == -1) {
            // Шалкер не найден в инвентаре
            return;
        }
        
        // Добавляем в карту открытых шалкеров с информацией о слоте
        openedShulkers.put(playerId, new ShulkerData(shulkerItem, UUID.randomUUID(), shulkerPosition));
        
        // Создаем новый инвентарь с кастомным названием
        Inventory customInventory = Bukkit.createInventory(null, 27, shulkerName);
        
        // Копируем содержимое оригинального шалкера в новый инвентарь
        Inventory originalInventory = shulker.getInventory();
        for (int i = 0; i < Math.min(originalInventory.getSize(), customInventory.getSize()); i++) {
            ItemStack item = originalInventory.getItem(i);
            if (item != null) {
                customInventory.setItem(i, item.clone());
            }
        }
        
        // Открываем кастомный инвентарь вместо оригинального
        player.openInventory(customInventory);
        
        // Играем звук открытия
        Sound openSound = plugin.getConfigManager().getOpenSound();
        if (openSound != null) {
            player.playSound(player.getLocation(), openSound, 1.0f, 1.0f);
        }
        
        // Отправляем сообщение
        Component openMessage = plugin.getConfigManager().getMessageComponents().getOpenMessage();
        if (openMessage != null) {
            plugin.getMessageUtils().sendMessageWithPrefix(player, openMessage);
        }
    }
    
    /**
     * Получает или создает уникальный ID для шалкера
     */
    private UUID getOrCreateShulkerId(ItemStack shulkerItem) {
        if (shulkerItem == null || !shulkerItem.hasItemMeta()) {
            return UUID.randomUUID();
        }
        
        ItemMeta meta = shulkerItem.getItemMeta().clone();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        
        // Проверяем, есть ли уже ID
        if (container.has(shulkerIdKey, PersistentDataType.STRING)) {
            String idString = container.get(shulkerIdKey, PersistentDataType.STRING);
            try {
                return UUID.fromString(idString);
            } catch (IllegalArgumentException e) {
                // Неверный UUID, создаем новый
            }
        }
        
        // Создаем новый ID и сохраняем его
        UUID newId = UUID.randomUUID();
        container.set(shulkerIdKey, PersistentDataType.STRING, newId.toString());
        shulkerItem.setItemMeta(meta);
        
        return newId;
    }
    
    private String getShulkerDisplayName(ItemStack shulkerItem) {
        String nameFormat = plugin.getConfigManager().getShulkerName();
        
        if (shulkerItem.hasItemMeta() && shulkerItem.getItemMeta().hasDisplayName()) {
            String displayName = shulkerItem.getItemMeta().getDisplayName();
            return plugin.getMessageUtils().formatShulkerName(nameFormat, displayName);
        } else {
            // Используем стандартное название типа шалкера
            String materialName = shulkerItem.getType().name().replace("_", " ");
            String formattedName = capitalizeWords(materialName.toLowerCase());
            return plugin.getMessageUtils().formatShulkerName(nameFormat, formattedName);
        }
    }
    
    private String capitalizeWords(String str) {
        String[] words = str.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    private void showItemCooldown(Player player, Material shulkerMaterial, int cooldownSeconds) {
        // Устанавливаем визуальный кулдаун при закрытии шалкера
        int cooldownTicks = cooldownSeconds * 20; // 20 тиков = 1 секунда
        
        // Устанавливаем кулдаун на конкретный тип шалкера
        player.setCooldown(shulkerMaterial, cooldownTicks);
        
        // Также устанавливаем на все типы шалкеров для единообразия
        Material[] shulkerTypes = {
            Material.SHULKER_BOX,
            Material.WHITE_SHULKER_BOX,
            Material.ORANGE_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX,
            Material.LIGHT_BLUE_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX,
            Material.LIME_SHULKER_BOX,
            Material.PINK_SHULKER_BOX,
            Material.GRAY_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX,
            Material.CYAN_SHULKER_BOX,
            Material.PURPLE_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX,
            Material.BROWN_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX,
            Material.RED_SHULKER_BOX,
            Material.BLACK_SHULKER_BOX
        };
        
        for (Material type : shulkerTypes) {
            player.setCooldown(type, cooldownTicks);
        }
    }
    
    /**
     * Сохраняет содержимое инвентаря шалкера обратно в предмет в инвентаре игрока
     */
    private void saveShulkerContents(Player player, ItemStack originalShulker, org.bukkit.inventory.Inventory shulkerInventory) {
        if (originalShulker == null || !(originalShulker.getItemMeta() instanceof BlockStateMeta)) {
            return;
        }
        
        // Дополнительная проверка: убеждаемся, что это действительно шалкер
        if (!isShulkerBox(originalShulker)) {
            return;
        }
        
        BlockStateMeta meta = (BlockStateMeta) originalShulker.getItemMeta();
        if (!(meta.getBlockState() instanceof ShulkerBox)) {
            return;
        }
        
        // Получаем шалкер из мета данных
        ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
        
        // Очищаем старое содержимое
        shulker.getInventory().clear();
        
        // Копируем новое содержимое из открытого инвентаря
        for (int i = 0; i < Math.min(shulkerInventory.getSize(), shulker.getInventory().getSize()); i++) {
            ItemStack item = shulkerInventory.getItem(i);
            if (item != null) {
                // Создаем точную копию предмета для избежания проблем с ссылками
                shulker.getInventory().setItem(i, item.clone());
            }
        }
        
        // Обновляем мета данные
        meta.setBlockState(shulker);
        originalShulker.setItemMeta(meta);
        
        // Ищем оригинальный шалкер в инвентаре игрока и обновляем его
        updateShulkerInPlayerInventory(player, originalShulker);
    }
    
    /**
     * НЕМЕДЛЕННОЕ СОХРАНЕНИЕ: сохраняет содержимое без задержки
     */
    private void saveShulkerContentsImmediate(Player player, ItemStack originalShulker, org.bukkit.inventory.Inventory shulkerInventory) {
        // Используем ту же логику, что и обычное сохранение
        saveShulkerContents(player, originalShulker, shulkerInventory);
    }
    
    /**
     * Проверяет, является ли предмет тем же шалкером (по типу и имени)
     */
    private boolean isSameShulker(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return false;
        }
        
        if (item1.getType() != item2.getType()) {
            return false;
        }
        
        if (!isShulkerBox(item1) || !isShulkerBox(item2)) {
            return false;
        }
        
        // Сравниваем имена
        String name1 = null;
        String name2 = null;
        
        if (item1.hasItemMeta() && item1.getItemMeta().hasDisplayName()) {
            name1 = item1.getItemMeta().getDisplayName();
        }
        
        if (item2.hasItemMeta() && item2.getItemMeta().hasDisplayName()) {
            name2 = item2.getItemMeta().getDisplayName();
        }
        
        // Если оба null - это один и тот же шалкер
        if (name1 == null && name2 == null) {
            return true;
        }
        
        // Если только один null - разные шалкеры
        if (name1 == null || name2 == null) {
            return false;
        }
        
        // Сравниваем строки
        return name1.equals(name2);
    }
    
    /**
     * Находит позицию шалкера в инвентаре игрока
     */
    private int findShulkerPosition(Player player, ItemStack shulkerItem) {
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
        
        // Ищем в основном инвентаре
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && isSameShulker(item, shulkerItem)) {
                return i;
            }
        }
        
        // Проверяем руки (используем специальные значения)
        if (isSameShulker(inventory.getItemInMainHand(), shulkerItem)) {
            return -100; // Специальное значение для главной руки
        } else if (isSameShulker(inventory.getItemInOffHand(), shulkerItem)) {
            return -101; // Специальное значение для второй руки
        }
        
        return -1; // Не найден
    }
    
    /**
     * Обновляет шалкер в инвентаре игрока с новым содержимым
     */
    private void updateShulkerInPlayerInventory(Player player, ItemStack updatedShulker) {
        UUID playerId = player.getUniqueId();
        ShulkerData shulkerData = openedShulkers.get(playerId);
        
        if (shulkerData == null) {
            plugin.getLogger().warning("ShulkerData is null for player " + player.getName());
            return;
        }
        
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
        boolean found = false;
        
        // Сначала проверяем руки
        if (isSameShulker(inventory.getItemInMainHand(), shulkerData.originalItem)) {
            inventory.setItemInMainHand(updatedShulker);
            found = true;
        } else if (isSameShulker(inventory.getItemInOffHand(), shulkerData.originalItem)) {
            inventory.setItemInOffHand(updatedShulker);
            found = true;
        } else {
            // Проверяем все слоты инвентаря
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null && isSameShulker(item, shulkerData.originalItem)) {
                    inventory.setItem(i, updatedShulker);
                    found = true;
                    break;
                }
            }
        }
        
        if (!found) {
            plugin.getLogger().warning("Could not find shulker to update for player " + player.getName());
        }
    }
    
    /**
     * Проверяет, имеет ли шалкер указанный UUID
     */
    private boolean hasShulkerId(ItemStack shulkerItem, UUID targetId) {
        if (shulkerItem == null || !shulkerItem.hasItemMeta() || targetId == null) {
            return false;
        }
        
        ItemMeta meta = shulkerItem.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        
        if (container.has(shulkerIdKey, PersistentDataType.STRING)) {
            String idString = container.get(shulkerIdKey, PersistentDataType.STRING);
            try {
                UUID itemId = UUID.fromString(idString);
                return itemId.equals(targetId);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Старый метод обновления шалкера (для совместимости)
     */
    private void updateShulkerInPlayerInventoryLegacy(Player player, ItemStack updatedShulker) {
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
        
        // Ищем шалкер в основном инвентаре
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && isSameShulker(item, updatedShulker)) {
                inventory.setItem(i, updatedShulker);
                return;
            }
        }
        
        // Проверяем руку
        if (isSameShulker(inventory.getItemInMainHand(), updatedShulker)) {
            inventory.setItemInMainHand(updatedShulker);
        } else if (isSameShulker(inventory.getItemInOffHand(), updatedShulker)) {
            inventory.setItemInOffHand(updatedShulker);
        }
    }

    /**
     * Безопасно очищает данные игрока
     */
    private void cleanupPlayerData(UUID playerId) {
        openedShulkers.remove(playerId);
    }
    
    /**
     * Проверяет, открыт ли у игрока шалкер через плагин
     */
    public boolean hasOpenShulker(Player player) {
        return openedShulkers.containsKey(player.getUniqueId());
    }
    
    /**
     * Обновляет содержимое шалкера из открытого инвентаря (для автосбора)
     */
    public void updateShulkerFromOpenInventory(Player player, ItemStack shulkerItem) {
        UUID playerId = player.getUniqueId();
        if (!openedShulkers.containsKey(playerId)) {
            return;
        }
        
        ShulkerData shulkerData = openedShulkers.get(playerId);
        if (shulkerData == null) {
            return;
        }
        
        // Получаем открытый инвентарь
        org.bukkit.inventory.Inventory topInventory = player.getOpenInventory().getTopInventory();
        
        // Сохраняем содержимое инвентаря в шалкер
        saveShulkerContents(player, shulkerItem, topInventory);
    }
    
    /**
     * Проверяет, является ли предмет запрещенным для помещения в шалкер
     */
    private boolean isBlacklistedItem(Player player, ItemStack item) {
        // Игроки с правом bypass могут обходить ограничения
        if (player.hasPermission("sunshulkers.bypass")) {
            return false;
        }
        
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        List<Material> blacklistedItems = plugin.getConfigManager().getBlacklistedItems();
        return blacklistedItems.contains(item.getType());
    }
    
    /**
     * Обрабатывает запрещенный предмет: отправляет сообщение и удаляет если настройка включена
     */
    private void handleBlacklistedItem(Player player, ItemStack item) {
        // Отправляем сообщение о запрещенном предмете
        Component blacklistedMsg = plugin.getConfigManager().getMessageComponents().getBlacklistedItemMessage();
        if (blacklistedMsg != null) {
            plugin.getMessageUtils().sendMessageWithPrefix(player, blacklistedMsg);
        }
        
        // Если включено удаление - удаляем предмет
        if (plugin.getConfigManager().isDeleteBlacklistedItems() && item != null) {
            player.setItemOnCursor(null);
            
            // Отправляем сообщение об удалении
            Component deletedMsg = plugin.getConfigManager().getMessageComponents().getItemDeletedMessage();
            if (deletedMsg != null) {
                plugin.getMessageUtils().sendMessageWithPrefix(player, deletedMsg);
            }
        }
    }
}
