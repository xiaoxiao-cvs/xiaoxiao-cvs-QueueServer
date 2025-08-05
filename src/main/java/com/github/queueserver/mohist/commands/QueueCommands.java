package com.github.queueserver.mohist.commands;

import com.github.queueserver.mohist.QueueMohistPlugin;
import com.github.queueserver.mohist.queue.SimpleQueueManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.UUID;

/**
 * Mohist环境下的队列命令处理器
 */
public class QueueCommands implements CommandExecutor {
    
    private final QueueMohistPlugin plugin;
    
    public QueueCommands(QueueMohistPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();
        
        switch (commandName) {
            case "queue":
            case "q":
                return handleQueueCommand(sender, args);
                
            case "queueinfo":
            case "qi":
            case "qinfo":
                return handleQueueInfoCommand(sender, args);
                
            case "leave":
            case "lq":
                return handleLeaveCommand(sender, args);
                
            case "queueadmin":
            case "qa":
            case "qadmin":
                return handleQueueAdminCommand(sender, args);
                
            case "qstats":
            case "queuestats":
                return handleQueueStatsCommand(sender, args);
                
            case "whitelist":
            case "wl":
                return handleWhitelistCommand(sender, args);
                
            default:
                return false;
        }
    }
    
    /**
     * 处理 /queue 命令
     */
    private boolean handleQueueCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    int position = plugin.getQueueManager().getQueuePosition(playerId);
                    int totalInQueue = plugin.getQueueManager().getQueueSize();
                    
                    if (position > 0) {
                        player.sendMessage("§a§l队列信息:");
                        player.sendMessage("§f  您的位置: §e#" + position + " / " + totalInQueue);
                        
                        // 显示VIP状态
                        if (plugin.getVipManager().isVIP(player)) {
                            int vipLevel = plugin.getVipManager().getVIPPriority(player);
                            player.sendMessage("§f  VIP等级: §6★" + vipLevel + " §7(优先排队)");
                        }
                        
                        // 显示服务器状态
                        int maxPlayers = plugin.getConfigManager().getMaxPlayers();
                        int currentPlayers = plugin.getServer().getOnlinePlayers().size();
                        player.sendMessage("§f  目标服务器: §e" + currentPlayers + "/" + maxPlayers);
                        
                    } else {
                        player.sendMessage("§c您当前不在队列中！");
                        player.sendMessage("§e使用 §f/queue join §e加入队列");
                    }
                    
                } catch (Exception e) {
                    player.sendMessage("§c获取队列信息失败，请稍后重试");
                    plugin.getLogger().severe("获取队列信息失败: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return true;
    }
    
    /**
     * 处理 /queueinfo 命令
     */
    private boolean handleQueueInfoCommand(CommandSender sender, String[] args) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    int totalInQueue = plugin.getQueueManager().getQueueSize();
                    int vipCount = plugin.getVipManager().getVIPPlayerCount();
                    int regularCount = totalInQueue - vipCount;
                    
                    sender.sendMessage("§6§l=== 队列服务器信息 ===");
                    sender.sendMessage("§f队列总人数: §e" + totalInQueue);
                    sender.sendMessage("§f  - VIP玩家: §6" + vipCount);
                    sender.sendMessage("§f  - 普通玩家: §7" + regularCount);
                    
                    // 服务器信息
                    int maxPlayers = plugin.getConfigManager().getMaxPlayers();
                    int currentPlayers = plugin.getServer().getOnlinePlayers().size();
                    sender.sendMessage("§f目标服务器: §e" + currentPlayers + "/" + maxPlayers);
                    
                    // 系统状态
                    boolean queueEnabled = plugin.getConfigManager().isQueueEnabled();
                    sender.sendMessage("§f队列状态: " + (queueEnabled ? "§a启用" : "§c禁用"));
                    
                } catch (Exception e) {
                    sender.sendMessage("§c获取服务器信息失败");
                    plugin.getLogger().severe("获取服务器信息失败: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return true;
    }
    
    /**
     * 处理 /leave 命令
     */
    private boolean handleLeaveCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c此命令只能由玩家执行！");
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        
        try {
            if (plugin.getQueueManager().isPlayerInQueue(playerId)) {
                plugin.getQueueManager().removeFromQueue(playerId);
                player.sendMessage("§a您已离开队列！");
                
                // 踢出玩家
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.kickPlayer("§a您已离开队列服务器");
                    }
                }.runTask(plugin);
                
            } else {
                player.sendMessage("§c您当前不在队列中！");
            }
            
        } catch (Exception e) {
            player.sendMessage("§c离开队列失败，请稍后重试");
            plugin.getLogger().severe("离开队列失败: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * 处理 /queueadmin 命令
     */
    private boolean handleQueueAdminCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("queue.admin")) {
            sender.sendMessage("§c您没有权限执行此命令！");
            return true;
        }
        
        if (args.length == 0) {
            sendAdminHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReloadCommand(sender);
                
            case "clear":
                return handleClearCommand(sender);
                
            case "kick":
                return handleKickCommand(sender, args);
                
            case "setvip":
                return handleSetVipCommand(sender, args);
                
            case "info":
                return handleAdminInfoCommand(sender);
                
            case "monitor":
            case "status":
                return handleServerMonitorCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                
            default:
                sendAdminHelp(sender);
                return true;
        }
    }
    
    /**
     * 发送管理员帮助信息
     */
    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage("§6§l=== 队列管理命令 ===");
        sender.sendMessage("§f/queueadmin reload §7- 重载配置");
        sender.sendMessage("§f/queueadmin clear §7- 清空队列");
        sender.sendMessage("§f/queueadmin kick <玩家> §7- 踢出队列");
        sender.sendMessage("§f/queueadmin setvip <玩家> <等级> §7- 设置VIP");
        sender.sendMessage("§f/queueadmin info §7- 详细信息");
        sender.sendMessage("§f/queueadmin monitor check §7- 检查服务器状态");
        sender.sendMessage("§f/queueadmin monitor broadcast §7- 广播服务器状态");
    }
    
    /**
     * 处理重载命令
     */
    private boolean handleReloadCommand(CommandSender sender) {
        try {
            plugin.getConfigManager().reloadConfig();
            plugin.getVipManager().reloadConfig();
            
            sender.sendMessage("§a配置文件已重载！");
            plugin.getLogger().info("管理员 " + sender.getName() + " 重载了配置");
            
        } catch (Exception e) {
            sender.sendMessage("§c重载配置失败: " + e.getMessage());
            plugin.getLogger().severe("重载配置失败: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * 处理清空队列命令
     */
    private boolean handleClearCommand(CommandSender sender) {
        try {
            int clearedCount = plugin.getQueueManager().getQueueSize();
            plugin.getQueueManager().clearAllQueues();
            
            sender.sendMessage("§a已清空队列，共移除 " + clearedCount + " 名玩家");
            plugin.getLogger().info("管理员 " + sender.getName() + " 清空了队列");
            
        } catch (Exception e) {
            sender.sendMessage("§c清空队列失败: " + e.getMessage());
            plugin.getLogger().severe("清空队列失败: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * 处理踢出玩家命令
     */
    private boolean handleKickCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /queueadmin kick <玩家名>");
            return true;
        }
        
        String playerName = args[1];
        Player target = plugin.getServer().getPlayer(playerName);
        
        if (target == null) {
            sender.sendMessage("§c玩家 " + playerName + " 不在线！");
            return true;
        }
        
        try {
            UUID targetId = target.getUniqueId();
            
            if (plugin.getQueueManager().isPlayerInQueue(targetId)) {
                plugin.getQueueManager().removeFromQueue(targetId);
                target.kickPlayer("§c您已被管理员从队列中移除");
                sender.sendMessage("§a已将玩家 " + playerName + " 从队列中移除");
                
                plugin.getLogger().info("管理员 " + sender.getName() + " 将玩家 " + playerName + " 从队列中移除");
                
            } else {
                sender.sendMessage("§c玩家 " + playerName + " 不在队列中！");
            }
            
        } catch (Exception e) {
            sender.sendMessage("§c踢出玩家失败: " + e.getMessage());
            plugin.getLogger().severe("踢出玩家失败: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * 处理设置VIP命令
     */
    private boolean handleSetVipCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c用法: /queueadmin setvip <玩家名> <VIP等级>");
            return true;
        }
        
        String playerName = args[1];
        
        try {
            int vipLevel = Integer.parseInt(args[2]);
            Player target = plugin.getServer().getPlayer(playerName);
            
            if (target == null) {
                sender.sendMessage("§c玩家 " + playerName + " 不在线！");
                return true;
            }
            
            plugin.getVipManager().setVIPStatus(target.getUniqueId(), vipLevel);
            
            if (vipLevel > 0) {
                sender.sendMessage("§a已将玩家 " + playerName + " 设置为VIP等级 " + vipLevel);
                target.sendMessage("§a您的VIP等级已设置为: §6★" + vipLevel);
            } else {
                sender.sendMessage("§a已移除玩家 " + playerName + " 的VIP状态");
                target.sendMessage("§c您的VIP状态已被移除");
            }
            
            plugin.getLogger().info("管理员 " + sender.getName() + " 设置玩家 " + playerName + " VIP等级为 " + vipLevel);
            
        } catch (NumberFormatException e) {
            sender.sendMessage("§c无效的VIP等级，请输入数字！");
        } catch (Exception e) {
            sender.sendMessage("§c设置VIP失败: " + e.getMessage());
            plugin.getLogger().severe("设置VIP失败: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * 处理管理员信息命令
     */
    private boolean handleAdminInfoCommand(CommandSender sender) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    sender.sendMessage("§6§l=== 队列系统详细信息 ===");
                    
                    // 基本信息
                    sender.sendMessage("§f插件版本: §e" + plugin.getDescription().getVersion());
                    sender.sendMessage("§f服务器类型: §eMohist混合服务器");
                    
                    // 队列统计
                    int totalQueue = plugin.getQueueManager().getQueueSize();
                    int vipCount = plugin.getVipManager().getVIPPlayerCount();
                    sender.sendMessage("§f队列统计:");
                    sender.sendMessage("§f  - 总人数: §e" + totalQueue);
                    sender.sendMessage("§f  - VIP玩家: §6" + vipCount);
                    sender.sendMessage("§f  - 普通玩家: §7" + (totalQueue - vipCount));
                    
                    // 配置状态
                    boolean queueEnabled = plugin.getConfigManager().isQueueEnabled();
                    boolean vipEnabled = plugin.getConfigManager().isVipEnabled();
                    boolean whitelistEnabled = plugin.getConfigManager().isWhitelistEnabled();
                    
                    sender.sendMessage("§f系统状态:");
                    sender.sendMessage("§f  - 队列系统: " + (queueEnabled ? "§a启用" : "§c禁用"));
                    sender.sendMessage("§f  - VIP系统: " + (vipEnabled ? "§a启用" : "§c禁用"));
                    sender.sendMessage("§f  - 白名单: " + (whitelistEnabled ? "§a启用" : "§c禁用"));
                    
                } catch (Exception e) {
                    sender.sendMessage("§c获取详细信息失败");
                    plugin.getLogger().severe("获取详细信息失败: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return true;
    }
    
    /**
     * 处理 /qstats 命令
     */
    private boolean handleQueueStatsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("queue.stats")) {
            sender.sendMessage("§c您没有权限执行此命令！");
            return true;
        }
        
        return handleQueueInfoCommand(sender, args);
    }
    
    /**
     * 处理 /whitelist 命令
     */
    private boolean handleWhitelistCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("queue.whitelist")) {
            sender.sendMessage("§c您没有权限执行此命令！");
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage("§c用法: /whitelist <add|remove|list> [玩家名]");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "add":
                if (args.length < 2) {
                    sender.sendMessage("§c用法: /whitelist add <玩家名>");
                    return true;
                }
                // 实现白名单添加逻辑
                sender.sendMessage("§a已将玩家 " + args[1] + " 添加到白名单");
                break;
                
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage("§c用法: /whitelist remove <玩家名>");
                    return true;
                }
                // 实现白名单移除逻辑
                sender.sendMessage("§a已将玩家 " + args[1] + " 从白名单移除");
                break;
                
            case "list":
                // 实现白名单列表显示逻辑
                sender.sendMessage("§a白名单玩家列表: (功能开发中)");
                break;
                
            default:
                sender.sendMessage("§c用法: /whitelist <add|remove|list> [玩家名]");
                break;
        }
        
        return true;
    }
    
    /**
     * 处理服务器监控相关命令
     */
    private boolean handleServerMonitorCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("queue.admin")) {
            sender.sendMessage("§c您没有权限使用此命令！");
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage("§6§l[服务器监控] §f用法:");
            sender.sendMessage("§f/queueadmin monitor check §7- 立即检查服务器状态");
            sender.sendMessage("§f/queueadmin monitor broadcast §7- 立即广播服务器状态");
            sender.sendMessage("§f/queueadmin monitor info §7- 显示监控信息");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "check":
                return handleMonitorCheckCommand(sender);
                
            case "broadcast":
                return handleMonitorBroadcastCommand(sender);
                
            case "info":
                return handleMonitorInfoCommand(sender);
                
            default:
                sender.sendMessage("§c未知的监控命令: " + args[0]);
                return true;
        }
    }
    
    /**
     * 处理立即检查服务器状态命令
     */
    private boolean handleMonitorCheckCommand(CommandSender sender) {
        sender.sendMessage("§e正在检查服务器状态...");
        
        if (plugin.getServerMonitor() != null) {
            plugin.getServerMonitor().forceCheck();
            
            // 延迟2秒后显示结果
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (plugin.getServerMonitor().isTargetServerOnline()) {
                        int players = plugin.getServerMonitor().getCurrentKnownPlayers();
                        sender.sendMessage("§a✓ 目标服务器在线，当前玩家数: " + players);
                    } else {
                        sender.sendMessage("§c✗ 目标服务器离线或无法连接");
                    }
                }
            }.runTaskLater(plugin, 40L); // 2秒后执行
            
        } else {
            sender.sendMessage("§c服务器监控器未初始化！");
        }
        
        return true;
    }
    
    /**
     * 处理立即广播服务器状态命令
     */
    private boolean handleMonitorBroadcastCommand(CommandSender sender) {
        if (plugin.getServerMonitor() != null) {
            if (plugin.getServerMonitor().isTargetServerOnline()) {
                // 手动触发广播
                int currentPlayers = plugin.getServerMonitor().getCurrentKnownPlayers();
                int queueSize = plugin.getQueueManager().getQueueSize();
                
                String message;
                if (plugin.getConfigManager().isStandaloneMode()) {
                    message = String.format(
                        "§6§l[服务器状态] §a当前在线: §e%d§7/%d §8| §a队列中: §e%d人",
                        currentPlayers, plugin.getConfigManager().getMaxPlayers(), queueSize
                    );
                } else {
                    String targetName = plugin.getConfigManager().getTargetServer();
                    message = String.format(
                        "§6§l[服务器状态] §a%s服务器在线: §e%d人 §8| §a队列服务器: §e%d人排队",
                        targetName, currentPlayers, queueSize
                    );
                }
                
                // 向所有在线玩家广播
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    player.sendMessage(message);
                }
                
                sender.sendMessage("§a已广播服务器状态到所有在线玩家");
            } else {
                sender.sendMessage("§c目标服务器当前离线，无法广播状态");
            }
        } else {
            sender.sendMessage("§c服务器监控器未初始化！");
        }
        
        return true;
    }
    
    /**
     * 处理显示监控信息命令
     */
    private boolean handleMonitorInfoCommand(CommandSender sender) {
        if (plugin.getServerMonitor() != null) {
            sender.sendMessage("§6§l[服务器监控信息]");
            sender.sendMessage("§f监控模式: §e" + 
                (plugin.getConfigManager().isStandaloneMode() ? "独立模式" : "代理模式"));
            
            if (!plugin.getConfigManager().isStandaloneMode()) {
                sender.sendMessage("§f目标服务器: §e" + plugin.getConfigManager().getTargetServer());
                sender.sendMessage("§f目标地址: §e" + 
                    plugin.getConfigManager().getTargetServerHost() + ":" + 
                    plugin.getConfigManager().getTargetServerPort());
            }
            
            sender.sendMessage("§f服务器状态: " + 
                (plugin.getServerMonitor().isTargetServerOnline() ? "§a在线" : "§c离线"));
            
            if (plugin.getServerMonitor().isTargetServerOnline()) {
                sender.sendMessage("§f当前玩家数: §e" + plugin.getServerMonitor().getCurrentKnownPlayers());
            }
            
            sender.sendMessage("§f队列人数: §e" + plugin.getQueueManager().getQueueSize());
            
            // 显示VIP队列信息
            SimpleQueueManager.QueueStats stats = plugin.getQueueManager().getQueueStats();
            sender.sendMessage("§f  - VIP队列: §6" + stats.getVipSize() + "人");
            sender.sendMessage("§f  - 普通队列: §7" + stats.getNormalSize() + "人");
            
        } else {
            sender.sendMessage("§c服务器监控器未初始化！");
        }
        
        return true;
    }
}
