package com.github.queueserver.forge.commands;

import com.github.queueserver.forge.QueueForgePlugin;
import com.github.queueserver.forge.queue.QueueManager;
import com.github.queueserver.forge.vip.VIPManager;
import com.github.queueserver.forge.monitor.ServerMonitor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 队列命令处理器
 * 处理所有队列相关的命令
 */
public class QueueCommands implements CommandExecutor, TabCompleter {
    
    private final QueueForgePlugin plugin;
    
    public QueueCommands(QueueForgePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();
        
        switch (commandName) {
            case "queue":
                return handleQueueCommand(sender, args);
            case "queueinfo":
                return handleQueueInfoCommand(sender, args);
            case "leave":
                return handleLeaveCommand(sender, args);
            case "queueadmin":
                return handleQueueAdminCommand(sender, args);
            case "qstats":
                return handleQueueStatsCommand(sender, args);
            case "qreload":
                return handleQueueReloadCommand(sender, args);
            default:
                return false;
        }
    }
    
    /**
     * 处理 /queue 命令
     */
    private boolean handleQueueCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        
        // 检查玩家是否已在队列中
        if (plugin.getQueueManager().isPlayerInQueue(playerId)) {
            int position = plugin.getQueueManager().getPlayerPosition(playerId);
            boolean isVip = plugin.getVipManager().isVIP(player);
            
            player.sendMessage("§e§l队列状态");
            player.sendMessage("§7您当前在队列中");
            player.sendMessage("§a位置: §f" + position);
            player.sendMessage("§a类型: §f" + (isVip ? "VIP" : "普通"));
            player.sendMessage("§7使用 /leave 离开队列");
            return true;
        }
        
        // 检查队列是否启用
        if (!plugin.getConfigManager().isQueueMode()) {
            player.sendMessage("§c队列功能当前未启用");
            return true;
        }
        
        // 尝试加入队列
        boolean isVip = plugin.getVipManager().isVIP(player);
        if (plugin.getQueueManager().addPlayer(player, isVip)) {
            int position = plugin.getQueueManager().getPlayerPosition(playerId);
            
            player.sendMessage("§a§l成功加入队列！");
            player.sendMessage("§e您的位置: §f" + position);
            player.sendMessage("§e队列类型: §f" + (isVip ? "VIP优先" : "普通"));
            player.sendMessage("§7请保持在线等待传送");
            
            // 记录到数据库
            plugin.getDatabaseManager().recordPlayerJoinQueue(playerId, player.getName(), isVip);
        } else {
            player.sendMessage("§c加入队列失败，请重试");
        }
        
        return true;
    }
    
    /**
     * 处理 /queueinfo 命令
     */
    private boolean handleQueueInfoCommand(CommandSender sender, String[] args) {
        QueueManager.QueueStats stats = plugin.getQueueManager().getQueueStats();
        
        sender.sendMessage("§6§l队列信息");
        sender.sendMessage("§7总队列大小: §f" + stats.getTotalSize());
        sender.sendMessage("§7VIP队列: §f" + stats.getVipSize());
        sender.sendMessage("§7普通队列: §f" + stats.getRegularSize());
        sender.sendMessage("§7今日处理: §f" + stats.getProcessedToday());
        
        if (stats.getLastProcessTime() > 0) {
            long timeSince = System.currentTimeMillis() - stats.getLastProcessTime();
            sender.sendMessage("§7上次处理: §f" + (timeSince / 1000) + "秒前");
        }
        
        // 服务器状态
        ServerMonitor.ServerStatus serverStatus = plugin.getServerMonitor().getServerStatus();
        sender.sendMessage("§7服务器TPS: §f" + String.format("%.1f", serverStatus.getTps()));
        sender.sendMessage("§7在线玩家: §f" + serverStatus.getCurrentPlayers() + "/" + serverStatus.getMaxPlayers());
        sender.sendMessage("§7负载状态: §f" + serverStatus.getLoadLevel().getDescription());
        
        return true;
    }
    
    /**
     * 处理 /leave 命令
     */
    private boolean handleLeaveCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行");
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        
        if (plugin.getQueueManager().removePlayer(playerId)) {
            player.sendMessage("§a您已离开队列");
            
            // 记录到数据库
            plugin.getDatabaseManager().recordPlayerLeaveQueue(playerId, "COMMAND");
        } else {
            player.sendMessage("§c您不在队列中");
        }
        
        return true;
    }
    
    /**
     * 处理 /queueadmin 命令
     */
    private boolean handleQueueAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("queue.admin")) {
            sender.sendMessage("§c您没有权限执行此命令");
            return true;
        }
        
        if (args.length == 0) {
            sendAdminHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "clear":
                plugin.getQueueManager().clearAllQueues();
                sender.sendMessage("§a队列已清空");
                break;
                
            case "reload":
                try {
                    plugin.getConfigManager().reloadConfig();
                    sender.sendMessage("§a配置已重载");
                } catch (Exception e) {
                    sender.sendMessage("§c重载配置失败: " + e.getMessage());
                }
                break;
                
            case "info":
                sendDetailedInfo(sender);
                break;
                
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage("§c用法: /queueadmin remove <玩家名>");
                    return true;
                }
                handleRemovePlayer(sender, args[1]);
                break;
                
            case "setvip":
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /queueadmin setvip <玩家名> <true/false>");
                    return true;
                }
                handleSetVip(sender, args[1], args[2]);
                break;
                
            default:
                sendAdminHelp(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * 处理 /qstats 命令
     */
    private boolean handleQueueStatsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("queue.stats")) {
            sender.sendMessage("§c您没有权限执行此命令");
            return true;
        }
        
        // 队列统计
        QueueManager.QueueStats queueStats = plugin.getQueueManager().getQueueStats();
        
        // VIP缓存统计
        VIPManager.VipCacheStats vipStats = plugin.getVipManager().getCacheStats();
        
        // 服务器状态
        ServerMonitor.ServerStatus serverStatus = plugin.getServerMonitor().getServerStatus();
        
        sender.sendMessage("§6§l=== 队列统计信息 ===");
        sender.sendMessage("§e队列状态:");
        sender.sendMessage("  §7总计: §f" + queueStats.getTotalSize());
        sender.sendMessage("  §7VIP: §f" + queueStats.getVipSize());
        sender.sendMessage("  §7普通: §f" + queueStats.getRegularSize());
        sender.sendMessage("  §7今日处理: §f" + queueStats.getProcessedToday());
        
        sender.sendMessage("§eVIP缓存:");
        sender.sendMessage("  §7缓存大小: §f" + vipStats.getSize());
        sender.sendMessage("  §7命中率: §f" + String.format("%.2f%%", vipStats.getHitRate() * 100));
        sender.sendMessage("  §7清理次数: §f" + vipStats.getEvictionCount());
        
        sender.sendMessage("§e服务器性能:");
        sender.sendMessage("  §7TPS: §f" + String.format("%.2f", serverStatus.getTps()));
        sender.sendMessage("  §7负载: §f" + serverStatus.getLoadLevel().getDescription());
        sender.sendMessage("  §7玩家: §f" + serverStatus.getCurrentPlayers() + "/" + serverStatus.getMaxPlayers());
        
        return true;
    }
    
    /**
     * 处理 /qreload 命令
     */
    private boolean handleQueueReloadCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("queue.reload")) {
            sender.sendMessage("§c您没有权限执行此命令");
            return true;
        }
        
        try {
            plugin.getConfigManager().reloadConfig();
            sender.sendMessage("§a§l队列插件配置已重载！");
        } catch (Exception e) {
            sender.sendMessage("§c重载失败: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * 发送管理员帮助信息
     */
    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("§6§l队列管理命令:");
        sender.sendMessage("§e/queueadmin clear §7- 清空所有队列");
        sender.sendMessage("§e/queueadmin reload §7- 重载配置");
        sender.sendMessage("§e/queueadmin info §7- 查看详细信息");
        sender.sendMessage("§e/queueadmin remove <玩家> §7- 从队列移除玩家");
        sender.sendMessage("§e/queueadmin setvip <玩家> <true/false> §7- 设置VIP状态");
    }
    
    /**
     * 发送详细信息
     */
    private void sendDetailedInfo(CommandSender sender) {
        sender.sendMessage("§6§l=== 队列系统详细信息 ===");
        
        // 基本配置
        sender.sendMessage("§e配置信息:");
        sender.sendMessage("  §7队列模式: §f" + (plugin.getConfigManager().isQueueMode() ? "启用" : "禁用"));
        sender.sendMessage("  §7最大队列: §f" + plugin.getConfigManager().getMaxQueueSize());
        sender.sendMessage("  §7处理间隔: §f" + plugin.getConfigManager().getQueueProcessInterval() + "秒");
        sender.sendMessage("  §7批量大小: §f" + plugin.getConfigManager().getTransferBatchSize());
        
        // HTTP配置
        sender.sendMessage("§eHTTP配置:");
        sender.sendMessage("  §7代理地址: §f" + plugin.getConfigManager().getProxyServerUrl());
        sender.sendMessage("  §7心跳间隔: §f" + plugin.getConfigManager().getHeartbeatInterval() + "秒");
        
        // 安全配置
        sender.sendMessage("§e安全配置:");
        sender.sendMessage("  §7安全检查: §f" + (plugin.getConfigManager().isSecurityEnabled() ? "启用" : "禁用"));
        sender.sendMessage("  §7反破解: §f" + (plugin.getConfigManager().isAntiCrackEnabled() ? "启用" : "禁用"));
        sender.sendMessage("  §7VIP功能: §f" + (plugin.getConfigManager().isVipEnabled() ? "启用" : "禁用"));
    }
    
    /**
     * 处理移除玩家
     */
    private void handleRemovePlayer(CommandSender sender, String playerName) {
        Player targetPlayer = plugin.getServer().getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage("§c玩家 " + playerName + " 不在线");
            return;
        }
        
        UUID playerId = targetPlayer.getUniqueId();
        if (plugin.getQueueManager().removePlayer(playerId)) {
            sender.sendMessage("§a已将玩家 " + playerName + " 从队列中移除");
            targetPlayer.sendMessage("§c您已被管理员从队列中移除");
            
            // 记录到数据库
            plugin.getDatabaseManager().recordPlayerLeaveQueue(playerId, "ADMIN_REMOVE");
        } else {
            sender.sendMessage("§c玩家 " + playerName + " 不在队列中");
        }
    }
    
    /**
     * 处理设置VIP
     */
    private void handleSetVip(CommandSender sender, String playerName, String vipStatus) {
        Player targetPlayer = plugin.getServer().getPlayer(playerName);
        if (targetPlayer == null) {
            sender.sendMessage("§c玩家 " + playerName + " 不在线");
            return;
        }
        
        boolean isVip = Boolean.parseBoolean(vipStatus);
        UUID playerId = targetPlayer.getUniqueId();
        
        plugin.getVipManager().setVipStatus(playerId, isVip);
        plugin.getDatabaseManager().updateVipRecord(playerId, playerName, isVip);
        
        sender.sendMessage("§a已设置玩家 " + playerName + " 的VIP状态为: " + (isVip ? "VIP" : "普通"));
        targetPlayer.sendMessage("§e您的VIP状态已更新为: " + (isVip ? "§aVIP" : "§7普通"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        String commandName = command.getName().toLowerCase();
        
        if ("queueadmin".equals(commandName) && sender.hasPermission("queue.admin")) {
            if (args.length == 1) {
                completions.addAll(Arrays.asList("clear", "reload", "info", "remove", "setvip"));
            } else if (args.length == 2 && ("remove".equals(args[0]) || "setvip".equals(args[0]))) {
                // 添加在线玩家名称
                plugin.getServer().getOnlinePlayers().forEach(player -> 
                    completions.add(player.getName()));
            } else if (args.length == 3 && "setvip".equals(args[0])) {
                completions.addAll(Arrays.asList("true", "false"));
            }
        }
        
        return completions;
    }
}
