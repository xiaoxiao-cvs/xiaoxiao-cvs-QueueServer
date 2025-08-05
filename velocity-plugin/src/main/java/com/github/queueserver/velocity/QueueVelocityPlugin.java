package com.github.queueserver.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import org.slf4j.Logger;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Velocity队列服务器插件
 * 处理来自Mohist队列服务器的玩家传送请求
 */
@Plugin(
    id = "queue-velocity-plugin",
    name = "Queue Velocity Plugin",
    version = "1.0.0",
    description = "处理队列服务器的玩家传送请求",
    authors = {"QueueServer Team"}
)
public class QueueVelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    
    // 插件消息通道
    private static final String CHANNEL_NAME = "queueserver:transfer";
    private static final MinecraftChannelIdentifier TRANSFER_CHANNEL = 
        MinecraftChannelIdentifier.from(CHANNEL_NAME);
    
    // BungeeCord兼容通道
    private static final LegacyChannelIdentifier BUNGEE_CHANNEL = 
        new LegacyChannelIdentifier("BungeeCord");

    @Inject
    public QueueVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Queue Velocity Plugin 正在启动...");
        
        // 注册插件消息通道
        server.getChannelRegistrar().register(TRANSFER_CHANNEL);
        server.getChannelRegistrar().register(BUNGEE_CHANNEL);
        
        // 注册命令
        registerCommands();
        
        logger.info("Queue Velocity Plugin 已启动完成！");
        logger.info("已注册插件消息通道: " + CHANNEL_NAME);
        logger.info("支持BungeeCord兼容模式");
    }

    /**
     * 处理插件消息事件
     */
    @Subscribe
    public void onPluginMessageFromBackend(PluginMessageEvent event) {
        // 处理自定义传送消息
        if (event.getIdentifier().equals(TRANSFER_CHANNEL)) {
            handleCustomTransferMessage(event);
        }
        // 处理BungeeCord兼容消息
        else if (event.getIdentifier().equals(BUNGEE_CHANNEL)) {
            handleBungeeCordMessage(event);
        }
    }

    /**
     * 处理自定义传送消息
     */
    private void handleCustomTransferMessage(PluginMessageEvent event) {
        try {
            // 确保消息来源是服务器
            if (!(event.getSource() instanceof com.velocitypowered.api.proxy.ServerConnection)) {
                return;
            }

            byte[] data = event.getData();
            ByteArrayInputStream stream = new ByteArrayInputStream(data);
            DataInputStream in = new DataInputStream(stream);

            String action = in.readUTF();
            if ("TRANSFER_PLAYER".equals(action)) {
                String playerUuid = in.readUTF();
                String playerName = in.readUTF();
                String targetServer = in.readUTF();
                long timestamp = in.readLong();
                String token = in.readUTF();

                // 验证传送令牌（可选）
                if (validateTransferToken(playerUuid, targetServer, timestamp, token)) {
                    transferPlayer(playerUuid, playerName, targetServer);
                } else {
                    logger.warn("无效的传送令牌，拒绝传送玩家: " + playerName);
                }
            }

            // 标记事件已处理
            event.setResult(PluginMessageEvent.ForwardResult.handled());

        } catch (Exception e) {
            logger.error("处理自定义传送消息时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 处理BungeeCord兼容消息
     */
    private void handleBungeeCordMessage(PluginMessageEvent event) {
        try {
            // 确保消息来源是服务器
            if (!(event.getSource() instanceof com.velocitypowered.api.proxy.ServerConnection)) {
                return;
            }

            byte[] data = event.getData();
            ByteArrayInputStream stream = new ByteArrayInputStream(data);
            DataInputStream in = new DataInputStream(stream);

            String action = in.readUTF();
            if ("Connect".equals(action)) {
                String targetServer = in.readUTF();
                
                // 获取发送消息的玩家
                com.velocitypowered.api.proxy.ServerConnection serverConnection = 
                    (com.velocitypowered.api.proxy.ServerConnection) event.getSource();
                Player player = serverConnection.getPlayer();
                
                if (player != null) {
                    transferPlayer(player.getUniqueId().toString(), player.getUsername(), targetServer);
                }
            }

            // 标记事件已处理
            event.setResult(PluginMessageEvent.ForwardResult.handled());

        } catch (Exception e) {
            logger.error("处理BungeeCord消息时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 传送玩家到目标服务器
     */
    private void transferPlayer(String playerUuid, String playerName, String targetServerName) {
        try {
            UUID uuid = UUID.fromString(playerUuid);
            Optional<Player> playerOpt = server.getPlayer(uuid);
            
            if (!playerOpt.isPresent()) {
                logger.warn("无法找到玩家进行传送: " + playerName + " (" + playerUuid + ")");
                return;
            }

            Player player = playerOpt.get();
            Optional<RegisteredServer> targetServerOpt = server.getServer(targetServerName);
            
            if (!targetServerOpt.isPresent()) {
                logger.warn("目标服务器不存在: " + targetServerName);
                player.sendMessage(net.kyori.adventure.text.Component.text(
                    "§c传送失败：目标服务器 " + targetServerName + " 不可用"));
                return;
            }

            RegisteredServer targetServer = targetServerOpt.get();
            
            // 发送传送消息给玩家
            player.sendMessage(net.kyori.adventure.text.Component.text(
                "§a正在将您传送到游戏服务器..."));

            // 执行传送
            player.createConnectionRequest(targetServer).connect().thenAccept(result -> {
                if (result.isSuccessful()) {
                    logger.info("成功传送玩家 " + playerName + " 到服务器: " + targetServerName);
                } else {
                    logger.warn("传送玩家失败: " + playerName + " 到服务器: " + targetServerName + 
                        " 原因: " + result.getReasonComponent().map(c -> c.toString()).orElse("未知错误"));
                    player.sendMessage(net.kyori.adventure.text.Component.text(
                        "§c传送失败，目标服务器可能离线或已满"));
                }
            }).exceptionally(throwable -> {
                logger.error("传送玩家时发生异常: " + throwable.getMessage(), throwable);
                player.sendMessage(net.kyori.adventure.text.Component.text(
                    "§c传送时发生错误：" + throwable.getMessage()));
                return null;
            });

        } catch (Exception e) {
            logger.error("传送玩家时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 验证传送令牌
     */
    private boolean validateTransferToken(String playerUuid, String targetServer, long timestamp, String token) {
        try {
            // 检查时间戳是否在合理范围内（5分钟）
            long currentTime = System.currentTimeMillis();
            if (Math.abs(currentTime - timestamp) > 5 * 60 * 1000) {
                logger.warn("传送令牌已过期: " + playerUuid);
                return false;
            }

            // 这里可以添加更复杂的令牌验证逻辑
            // 例如使用共享密钥验证HMAC等
            
            // 简单验证：检查token是否为base64编码
            try {
                byte[] decoded = Base64.getDecoder().decode(token);
                String decodedStr = new String(decoded, StandardCharsets.UTF_8);
                return decodedStr.contains(playerUuid) && decodedStr.contains(targetServer);
            } catch (Exception e) {
                logger.warn("无效的传送令牌格式: " + token);
                return false;
            }

        } catch (Exception e) {
            logger.warn("验证传送令牌时发生错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 注册命令
     */
    private void registerCommands() {
        CommandManager commandManager = server.getCommandManager();
        
        // 注册队列状态命令
        commandManager.register("vqueue", new VelocityQueueCommand(this));
        
        logger.info("已注册Velocity队列命令");
    }

    /**
     * 获取在线服务器列表
     */
    public String[] getOnlineServers() {
        return server.getAllServers().stream()
            .filter(server -> server.getPlayersConnected().size() >= 0) // 简单过滤
            .map(RegisteredServer::getServerInfo)
            .map(info -> info.getName())
            .toArray(String[]::new);
    }

    /**
     * 获取服务器信息
     */
    public String getServerInfo(String serverName) {
        Optional<RegisteredServer> serverOpt = server.getServer(serverName);
        if (serverOpt.isPresent()) {
            RegisteredServer registeredServer = serverOpt.get();
            int playerCount = registeredServer.getPlayersConnected().size();
            String address = registeredServer.getServerInfo().getAddress().toString();
            return String.format("%s (%s) - %d 玩家在线", serverName, address, playerCount);
        }
        return "服务器不存在: " + serverName;
    }

    // Getter 方法
    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }
}
