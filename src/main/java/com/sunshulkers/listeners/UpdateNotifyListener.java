package com.sunshulkers.listeners;

import com.sunshulkers.SunShulkersPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateNotifyListener implements Listener {
    
    private final SunShulkersPlugin plugin;
    
    public UpdateNotifyListener(SunShulkersPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Проверяем права на получение уведомлений об обновлениях
        if (player.hasPermission("sunshulkers.admin") || player.hasPermission("sunshulkers.update.notify") || player.isOp()) {
            // Уведомляем об обновлении
            plugin.getUpdateChecker().notifyPlayer(player);
        }
    }
}