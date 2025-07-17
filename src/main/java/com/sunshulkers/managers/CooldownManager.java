package com.sunshulkers.managers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    
    public void setCooldown(Player player, int seconds) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
    }
    
    public boolean hasCooldown(Player player) {
        Long cooldownTime = cooldowns.get(player.getUniqueId());
        if (cooldownTime == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= cooldownTime) {
            cooldowns.remove(player.getUniqueId());
            return false;
        }
        
        return true;
    }
    
    public int getRemainingCooldown(Player player) {
        Long cooldownTime = cooldowns.get(player.getUniqueId());
        if (cooldownTime == null) {
            return 0;
        }
        
        long remaining = cooldownTime - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(player.getUniqueId());
            return 0;
        }
        
        return (int) Math.ceil(remaining / 1000.0);
    }
    
    public void removeCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
    
    public void clearAllCooldowns() {
        cooldowns.clear();
    }
}
