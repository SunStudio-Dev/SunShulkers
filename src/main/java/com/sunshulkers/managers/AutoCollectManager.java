package com.sunshulkers.managers;

import com.sunshulkers.SunShulkersPlugin;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoCollectManager {
    
    private final SunShulkersPlugin plugin;
    private final Map<UUID, Boolean> cachedStates = new HashMap<>();
    
    public AutoCollectManager(SunShulkersPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Проверяет включен ли автосбор для игрока
     */
    public boolean isAutoCollectEnabled(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Проверяем кеш
        if (cachedStates.containsKey(uuid)) {
            return cachedStates.get(uuid);
        }
        
        // Получаем из базы данных
        boolean state = plugin.getDatabaseManager().getAutoCollectState(player);
        cachedStates.put(uuid, state);
        
        return state;
    }
    
    /**
     * Устанавливает состояние автосбора для игрока
     */
    public void setAutoCollectEnabled(Player player, boolean enabled) {
        UUID uuid = player.getUniqueId();
        
        // Обновляем кеш
        cachedStates.put(uuid, enabled);
        
        // Сохраняем в базу данных
        plugin.getDatabaseManager().setAutoCollectState(player, enabled);
    }
    
    /**
     * Очищает кеш всех игроков (например, при перезагрузке)
     */
    public void clearAllStates() {
        cachedStates.clear();
    }
    
    /**
     * Удаляет данные игрока из кеша (например, при выходе с сервера)
     */
    public void removePlayer(Player player) {
        cachedStates.remove(player.getUniqueId());
    }
}
