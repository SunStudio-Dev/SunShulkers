package com.sunshulkers.commands;

import com.sunshulkers.SunShulkersPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoCollectCommand implements CommandExecutor, TabCompleter {
    
    private final SunShulkersPlugin plugin;
    
    public AutoCollectCommand(SunShulkersPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cЭта команда доступна только игрокам!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Проверяем включена ли функция в конфиге
        if (!plugin.getConfigManager().isAutoCollectEnabled()) {
            Component disabledMsg = plugin.getConfigManager().getMessageComponents().getAutocollectDisabledMessage();
            if (disabledMsg != null) {
                plugin.getMessageUtils().sendMessageWithPrefix(player, disabledMsg);
            }
            return true;
        }
        
        // Проверяем права
        if (!player.hasPermission(plugin.getConfigManager().getAutoCollectPermission())) {
            Component noPermMsg = plugin.getConfigManager().getMessageComponents().getNoPermissionMessage();
            if (noPermMsg != null) {
                plugin.getMessageUtils().sendMessageWithPrefix(player, noPermMsg);
            }
            return true;
        }
        
        if (args.length == 0) {
            // Переключаем состояние
            toggleAutoCollect(player);
        } else {
            String action = args[0].toLowerCase();
            switch (action) {
                case "on":
                case "enable":
                case "включить":
                case "вкл":
                    setAutoCollect(player, true);
                    break;
                case "off":
                case "disable":
                case "отключить":
                case "выкл":
                    setAutoCollect(player, false);
                    break;
                case "status":
                case "состояние":
                    showStatus(player);
                    break;
                default:
                    plugin.getMessageUtils().sendMessageWithPrefix(player, 
                        "§cИспользование: /" + command.getName() + " [on/off/status]");
                    break;
            }
        }
        
        return true;
    }
    
    private void toggleAutoCollect(Player player) {
        boolean currentState = plugin.getAutoCollectManager().isAutoCollectEnabled(player);
        setAutoCollect(player, !currentState);
    }
    
    private void setAutoCollect(Player player, boolean enabled) {
        plugin.getAutoCollectManager().setAutoCollectEnabled(player, enabled);
        
        Component message;
        if (enabled) {
            message = plugin.getConfigManager().getMessageComponents().getAutocollectEnabledMessage();
        } else {
            message = plugin.getConfigManager().getMessageComponents().getAutocollectDisabledMessage();
        }
        
        if (message != null) {
            plugin.getMessageUtils().sendMessageWithPrefix(player, message);
        }
    }
    
    private void showStatus(Player player) {
        boolean enabled = plugin.getAutoCollectManager().isAutoCollectEnabled(player);
        String status = enabled ? "§aВключен" : "§cОтключен";
        plugin.getMessageUtils().sendMessageWithPrefix(player, 
            "§7Автосбор предметов в шалкер: " + status);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> options = Arrays.asList("on", "off", "status", "включить", "отключить", "состояние");
            
            for (String option : options) {
                if (option.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(option);
                }
            }
        }
        
        return completions;
    }
}
