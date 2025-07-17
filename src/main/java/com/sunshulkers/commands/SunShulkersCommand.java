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

public class SunShulkersCommand implements CommandExecutor, TabCompleter {
    
    private final SunShulkersPlugin plugin;
    
    public SunShulkersCommand(SunShulkersPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendInfoMessage(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReload(sender);
            case "info":
                sendInfoMessage(sender);
                return true;
            case "autocollect":
                return handleAutoCollect(sender);
            default:
                sendInfoMessage(sender);
                return true;
        }
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("sunshulkers.admin")) {
            if (sender instanceof Player) {
                Component noPermMsg = plugin.getConfigManager().getMessageComponents().getNoPermissionMessage();
                if (noPermMsg != null) {
                    plugin.getMessageUtils().sendMessageWithPrefix((Player) sender, noPermMsg);
                }
            } else {
                sender.sendMessage("У вас недостаточно прав.");
            }
            return true;
        }
        
        try {
            plugin.getConfigManager().reloadConfig();
            plugin.getCooldownManager().clearAllCooldowns();
            plugin.getAutoCollectManager().clearAllStates();
            
            Component reloadMsg = plugin.getConfigManager().getMessageComponents().getReloadMessage();
            
            if (reloadMsg != null) {
                if (sender instanceof Player) {
                    plugin.getMessageUtils().sendMessageWithPrefix((Player) sender, reloadMsg);
                } else {
                    sender.sendMessage("§a" + plugin.getConfigManager().getReloadMessage());
                }
            }
            
            plugin.getLogger().info("Конфигурация перезагружена пользователем " + sender.getName());
            
        } catch (Exception e) {
            String errorMsg = "§cОшибка при перезагрузке конфигурации: " + e.getMessage();
            
            if (sender instanceof Player) {
                plugin.getMessageUtils().sendMessageWithoutPrefix((Player) sender, errorMsg);
            } else {
                sender.sendMessage(errorMsg);
            }
        }
        
        return true;
    }
    
    private boolean handleAutoCollect(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cЭта команда доступна только игрокам!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission(plugin.getConfigManager().getAutoCollectPermission())) {
            Component noPermMsg = plugin.getConfigManager().getMessageComponents().getNoPermissionMessage();
            if (noPermMsg != null) {
                plugin.getMessageUtils().sendMessageWithPrefix(player, noPermMsg);
            }
            return true;
        }
        
        if (!plugin.getConfigManager().isAutoCollectEnabled()) {
            plugin.getMessageUtils().sendMessageWithPrefix(player, "§cФункция автосбора отключена в конфигурации!");
            return true;
        }
        
        // Переключаем состояние автосбора для игрока
        boolean currentState = plugin.getAutoCollectManager().isAutoCollectEnabled(player);
        plugin.getAutoCollectManager().setAutoCollectEnabled(player, !currentState);
        
        Component message;
        if (!currentState) {
            message = plugin.getConfigManager().getMessageComponents().getAutocollectEnabledMessage();
        } else {
            message = plugin.getConfigManager().getMessageComponents().getAutocollectDisabledMessage();
        }
        
        if (message != null) {
            plugin.getMessageUtils().sendMessageWithPrefix(player, message);
        }
        
        return true;
    }
    
    private void sendInfoMessage(CommandSender sender) {
        String[] infoMessages = {
            "<dark_gray><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></dark_gray>",
            "<gradient:#9863E7:#4498DB><bold>SunShulkers</bold></gradient> <gray>- Информация о плагине</gray>",
            "<dark_gray><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></dark_gray>",
            "<gray>Версия: <yellow>" + plugin.getDescription().getVersion() + "</yellow></gray>",
            "<gray>Автор: <yellow>" + String.join(", ", plugin.getDescription().getAuthors()) + "</yellow></gray>",
            "<gray>Кулдаун: <yellow>" + plugin.getConfigManager().getCooldown() + " сек.</yellow></gray>",
            "<gray>Визуальный кулдаун: <yellow>" + (plugin.getConfigManager().isReloadingItemEnabled() ? "Включен" : "Отключен") + "</yellow></gray>",
            "<gray>Автосбор в шалкер: <yellow>" + (plugin.getConfigManager().isAutoCollectEnabled() ? "Включен" : "Отключен") + "</yellow></gray>",
            "<gray>Автосбор по умолчанию: <yellow>" + (plugin.getConfigManager().isAutoCollectDefaultEnabled() ? "Включен" : "Отключен") + "</yellow></gray>",
            "<gray>Заблокированных миров: <yellow>" + plugin.getConfigManager().getBlacklistedWorlds().size() + "</yellow></gray>",
            "<gray>Заблокированных предметов: <yellow>" + plugin.getConfigManager().getBlacklistedItems().size() + "</yellow></gray>",
            "<dark_gray><strikethrough>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</strikethrough></dark_gray>"
        };
        
        for (String message : infoMessages) {
            if (sender instanceof Player) {
                plugin.getMessageUtils().sendMessageWithoutPrefix((Player) sender, message);
            } else {
                sender.sendMessage(message);
            }
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("info", "autocollect");
            
            if (sender.hasPermission("sunshulkers.admin")) {
                subcommands = Arrays.asList("reload", "info", "autocollect");
            }
            
            for (String subcommand : subcommands) {
                if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subcommand);
                }
            }
        }
        
        return completions;
    }
}
