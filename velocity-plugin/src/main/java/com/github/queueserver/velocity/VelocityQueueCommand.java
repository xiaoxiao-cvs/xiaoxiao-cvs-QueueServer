package com.github.queueserver.velocity;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Velocity队列命令
 */
public class VelocityQueueCommand implements SimpleCommand {

    private final QueueVelocityPlugin plugin;

    public VelocityQueueCommand(QueueVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            invocation.source().sendMessage(Component.text("此命令只能由玩家执行！").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            sendHelpMessage(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "status":
                sendServerStatus(player);
                break;
            case "servers":
                sendServerList(player);
                break;
            case "info":
                sendQueueInfo(player);
                break;
            default:
                sendHelpMessage(player);
                break;
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(
            List.of("status", "servers", "info")
        );
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("queue.command");
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(Component.text("=== 队列系统命令帮助 ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/vqueue status - 查看服务器状态").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/vqueue servers - 查看所有服务器").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/vqueue info - 查看队列信息").color(NamedTextColor.YELLOW));
    }

    private void sendServerStatus(Player player) {
        player.sendMessage(Component.text("=== 服务器状态 ===").color(NamedTextColor.GOLD));
        
        // 获取当前所在服务器
        player.getCurrentServer().ifPresent(serverConnection -> {
            String serverName = serverConnection.getServerInfo().getName();
            player.sendMessage(Component.text("当前服务器: " + serverName).color(NamedTextColor.GREEN));
        });

        // 显示代理服务器信息
        int totalPlayers = plugin.getServer().getPlayerCount();
        player.sendMessage(Component.text("代理服务器总在线人数: " + totalPlayers).color(NamedTextColor.AQUA));
    }

    private void sendServerList(Player player) {
        player.sendMessage(Component.text("=== 可用服务器列表 ===").color(NamedTextColor.GOLD));
        
        String[] servers = plugin.getOnlineServers();
        if (servers.length == 0) {
            player.sendMessage(Component.text("暂无可用服务器").color(NamedTextColor.RED));
            return;
        }

        for (String server : servers) {
            String serverInfo = plugin.getServerInfo(server);
            player.sendMessage(Component.text("• " + serverInfo).color(NamedTextColor.YELLOW));
        }
    }

    private void sendQueueInfo(Player player) {
        player.sendMessage(Component.text("=== 队列系统信息 ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("版本: 1.0.0").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("状态: 运行中").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("支持的传送方式:").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  • 自定义插件消息").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("  • BungeeCord兼容模式").color(NamedTextColor.GRAY));
        
        // 显示插件消息通道信息
        player.sendMessage(Component.text("已注册的消息通道:").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  • queueserver:transfer").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("  • BungeeCord").color(NamedTextColor.GRAY));
    }
}
