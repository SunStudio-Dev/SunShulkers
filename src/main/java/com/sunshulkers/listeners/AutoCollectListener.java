package com.sunshulkers.listeners;

import com.sunshulkers.SunShulkersPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.ShulkerBox;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.Map;

public class AutoCollectListener implements Listener {
    
    private final SunShulkersPlugin plugin;
    
    public AutoCollectListener(SunShulkersPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(EntityPickupItemEvent event) {
        // Проверяем что это игрок
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Проверяем включена ли функция
        if (!plugin.getConfigManager().isAutoCollectEnabled()) {
            return;
        }
        
        // Проверяем права
        if (!player.hasPermission(plugin.getConfigManager().getAutoCollectPermission())) {
            return;
        }
        
        // Проверяем включен ли автосбор для этого игрока
        if (!plugin.getAutoCollectManager().isAutoCollectEnabled(player)) {
            return;
        }
        
        // Получаем предмет в главной руке
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        
        // Проверяем что в руке шалкер
        if (!isShulkerBox(mainHand)) {
            return;
        }
        
        // Получаем подбираемый предмет
        ItemStack pickupItem = event.getItem().getItemStack();
        
        // Проверяем что предмет не является шалкером
        if (isShulkerBox(pickupItem)) {
            return;
        }
        
        // Проверяем что предмет не в черном списке (только если у игрока нет права bypass)
        if (!player.hasPermission("sunshulkers.bypass") && 
            plugin.getConfigManager().getBlacklistedItems().contains(pickupItem.getType())) {
            // Если настроено удаление запрещенных предметов
            if (plugin.getConfigManager().isDeleteBlacklistedItems()) {
                // Удаляем предмет и уведомляем
                event.setCancelled(true);
                event.getItem().remove();
                
                // Уведомляем игрока об удалении
                Component message = plugin.getConfigManager().getMessageComponents().getItemDeletedMessage();
                if (message != null) {
                    plugin.getMessageUtils().sendMessageWithPrefix(player, message);
                }
            }
            // Если не удаляем, просто не собираем в шалкер (дефолтное поведение)
            return;
        }
        
        // Пытаемся добавить предмет в шалкер
        if (addItemToShulker(mainHand, pickupItem, player)) {
            // Предмет успешно добавлен, отменяем обычный подбор
            event.setCancelled(true);
            event.getItem().remove();
            // Воспроизводим звук автосбора
            Sound pickupSound = plugin.getConfigManager().getPickupSound();
            if (pickupSound != null) {
                player.playSound(player.getLocation(), pickupSound, 1.0f, 1.0f);
            }
        }
    }
    
    /**
     * Проверяет является ли предмет шалкером
     */
    private boolean isShulkerBox(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        Material type = item.getType();
        return type == Material.SHULKER_BOX ||
               type == Material.WHITE_SHULKER_BOX ||
               type == Material.ORANGE_SHULKER_BOX ||
               type == Material.MAGENTA_SHULKER_BOX ||
               type == Material.LIGHT_BLUE_SHULKER_BOX ||
               type == Material.YELLOW_SHULKER_BOX ||
               type == Material.LIME_SHULKER_BOX ||
               type == Material.PINK_SHULKER_BOX ||
               type == Material.GRAY_SHULKER_BOX ||
               type == Material.LIGHT_GRAY_SHULKER_BOX ||
               type == Material.CYAN_SHULKER_BOX ||
               type == Material.PURPLE_SHULKER_BOX ||
               type == Material.BLUE_SHULKER_BOX ||
               type == Material.BROWN_SHULKER_BOX ||
               type == Material.GREEN_SHULKER_BOX ||
               type == Material.RED_SHULKER_BOX ||
               type == Material.BLACK_SHULKER_BOX;
    }
    
    /**
     * Добавляет предмет в шалкер
     */
    private boolean addItemToShulker(ItemStack shulkerItem, ItemStack itemToAdd, Player player) {
        if (!(shulkerItem.getItemMeta() instanceof BlockStateMeta)) {
            return false;
        }
        
        BlockStateMeta meta = (BlockStateMeta) shulkerItem.getItemMeta();
        if (!(meta.getBlockState() instanceof ShulkerBox)) {
            return false;
        }
        
        ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
        
        // Клонируем предмет для добавления
        ItemStack clonedItem = itemToAdd.clone();
        
        // Пытаемся добавить предмет
        HashMap<Integer, ItemStack> leftover = shulkerBox.getInventory().addItem(clonedItem);
        
        // Если есть остаток, значит не все поместилось
        if (!leftover.isEmpty()) {
            // Уведомляем игрока если включено
            if (plugin.getConfigManager().isAutoCollectNotifyNoSpace()) {
                Component message = plugin.getConfigManager().getMessageComponents().getAutocollectNoSpaceMessage();
                if (message != null) {
                    plugin.getMessageUtils().sendMessageWithPrefix(player, message);
                }
            }
            return false;
        }
        
        // Сохраняем изменения в шалкере
        meta.setBlockState(shulkerBox);
        shulkerItem.setItemMeta(meta);
        
        return true;
    }
}
