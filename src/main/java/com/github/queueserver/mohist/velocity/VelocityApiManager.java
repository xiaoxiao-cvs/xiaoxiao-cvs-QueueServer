package com.github.queueserver.mohist.velocity;

import com.github.queueserver.mohist.QueueMohistPlugin;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Velocity API管理器
 * 提供与Velocity代理服务器的直接通信能力
 * 绕过Mohist的代理协议限制
 */
public class VelocityApiManager {
    
    private final QueueMohistPlugin plugin;
    private final String velocityHost;
    private final int velocityApiPort;
    private final String apiSecret;
    private final boolean useWebApi;
    
    public VelocityApiManager(QueueMohistPlugin plugin) {
        this.plugin = plugin;
        this.velocityHost = plugin.getConfigManager().getVelocityApiHost();
        this.velocityApiPort = plugin.getConfigManager().getVelocityApiPort();
        this.apiSecret = plugin.getConfigManager().getVelocityApiSecret();
        this.useWebApi = plugin.getConfigManager().isVelocityWebApiEnabled();
        
        plugin.getLogger().info("VelocityApiManager 已初始化");
        plugin.getLogger().info("API地址: " + velocityHost + ":" + velocityApiPort);
        plugin.getLogger().info("使用Web API: " + useWebApi);
        
        // 注册插件消息通道
        registerPluginChannels();
    }
    
    /**
     * 注册插件消息通道
     */
    private void registerPluginChannels() {
        try {
            // 注册自定义通道
            String customChannel = "queueserver:transfer";
            if (!plugin.getServer().getMessenger().isOutgoingChannelRegistered(plugin, customChannel)) {
                plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, customChannel);
                plugin.getLogger().info("已注册插件消息通道: " + customChannel);
            }
            
            // 注册BungeeCord兼容通道
            String bungeeCordChannel = "BungeeCord";
            if (!plugin.getServer().getMessenger().isOutgoingChannelRegistered(plugin, bungeeCordChannel)) {
                plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, bungeeCordChannel);
                plugin.getLogger().info("已注册BungeeCord兼容通道: " + bungeeCordChannel);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "注册插件消息通道失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将玩家转发到目标服务器
     */
    public CompletableFuture<Boolean> transferPlayer(Player player, String targetServer) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 优先使用插件消息方式
                boolean success = transferPlayerViaPluginMessage(player, targetServer);
                if (success) {
                    return true;
                }
                
                // 如果插件消息失败，尝试Web API（如果启用）
                if (useWebApi) {
                    success = transferPlayerViaWebApi(player, targetServer);
                    if (success) {
                        return true;
                    }
                }
                
                // 所有方法都失败，使用备用方案
                plugin.getLogger().warning("所有传送方法都失败，使用踢出重连方案");
                transferPlayerWithKick(player, targetServer);
                return false;
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "玩家转发失败: " + e.getMessage(), e);
                return false;
            }
        });
    }
    
    /**
     * 通过Web API转发玩家
     */
    private boolean transferPlayerViaWebApi(Player player, String targetServer) {
        try {
            String apiUrl = String.format("http://%s:%d/api/v1/transfer", velocityHost, velocityApiPort);
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // 设置请求头
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiSecret);
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            // 构建请求体
            String requestBody = String.format(
                "{\"player\":\"%s\",\"server\":\"%s\",\"reason\":\"Queue transfer\"}", 
                player.getUniqueId().toString(), 
                targetServer
            );
            
            // 发送请求
            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            
            // 检查响应
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                plugin.getLogger().info("玩家 " + player.getName() + " 已通过Web API转发到服务器: " + targetServer);
                return true;
            } else {
                String error = readErrorResponse(connection);
                plugin.getLogger().warning("Web API转发失败 (" + responseCode + "): " + error);
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Web API转发异常: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 通过插件消息转发玩家
     */
    private boolean transferPlayerViaPluginMessage(Player player, String targetServer) {
        try {
            // 先尝试自定义通道
            if (sendCustomPluginMessage(player, targetServer)) {
                return true;
            }
            
            // 如果自定义通道失败，尝试BungeeCord兼容通道
            return sendBungeeCordPluginMessage(player, targetServer);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "插件消息转发异常: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 发送自定义插件消息
     */
    private boolean sendCustomPluginMessage(Player player, String targetServer) {
        try {
            String channelName = "queueserver:transfer";
            
            // 构建消息数据
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            
            out.writeUTF("TRANSFER_PLAYER");
            out.writeUTF(player.getUniqueId().toString());
            out.writeUTF(player.getName());
            out.writeUTF(targetServer);
            out.writeLong(System.currentTimeMillis());
            out.writeUTF(generateTransferToken(player, targetServer));
            
            // 发送消息
            player.sendPluginMessage(plugin, channelName, b.toByteArray());
            
            plugin.getLogger().info("已发送自定义转发消息给玩家 " + player.getName() + " 到服务器: " + targetServer);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "发送自定义插件消息失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 发送BungeeCord兼容插件消息
     */
    private boolean sendBungeeCordPluginMessage(Player player, String targetServer) {
        try {
            String channelName = "BungeeCord";
            
            // 构建BungeeCord格式的消息
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            
            out.writeUTF("Connect");
            out.writeUTF(targetServer);
            
            // 发送消息
            player.sendPluginMessage(plugin, channelName, b.toByteArray());
            
            plugin.getLogger().info("已发送BungeeCord兼容转发消息给玩家 " + player.getName() + " 到服务器: " + targetServer);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "发送BungeeCord插件消息失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 生成转发令牌
     */
    private String generateTransferToken(Player player, String targetServer) {
        String data = player.getUniqueId() + ":" + targetServer + ":" + System.currentTimeMillis() + ":" + apiSecret;
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 使用备用方案：踢出玩家并提供连接信息
     */
    public void transferPlayerWithKick(Player player, String targetServer) {
        // 在主线程执行踢出操作
        new BukkitRunnable() {
            @Override
            public void run() {
                String message = buildTransferMessage(targetServer);
                player.kickPlayer(message);
                plugin.getLogger().info("玩家 " + player.getName() + " 已被踢出并提供转移信息到: " + targetServer);
            }
        }.runTask(plugin);
    }
    
    /**
     * 构建转移消息
     */
    private String buildTransferMessage(String targetServer) {
        String serverInfo = getServerConnectionInfo(targetServer);
        
        return "§6§l=== 队列转移通知 ===\n\n" +
               "§a§l恭喜！您已通过队列验证！\n\n" +
               "§e请重新连接服务器，您将自动进入游戏服务器：\n" +
               "§f" + serverInfo + "\n\n" +
               "§7§l提示：\n" +
               "§7• 如果您通过代理连接，直接重连即可\n" +
               "§7• 如果直连，请连接到上述地址\n" +
               "§7• 连接时请保持相同的用户名\n\n" +
               "§a§l感谢您的耐心等待！";
    }
    
    /**
     * 获取服务器连接信息
     */
    private String getServerConnectionInfo(String serverName) {
        // 从配置中获取服务器地址信息
        String host = plugin.getConfigManager().getServerHost(serverName);
        int port = plugin.getConfigManager().getServerPort(serverName);
        
        if (host != null && port > 0) {
            return host + ":" + port;
        } else {
            return plugin.getConfigManager().getTargetServerHost() + ":" + 
                   plugin.getConfigManager().getTargetServerPort();
        }
    }
    
    /**
     * 检查Velocity连接状态
     */
    public CompletableFuture<Boolean> checkVelocityConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (useWebApi) {
                    return checkWebApiConnection();
                } else {
                    return checkSocketConnection();
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "检查Velocity连接失败: " + e.getMessage(), e);
                return false;
            }
        });
    }
    
    /**
     * 检查Web API连接
     */
    private boolean checkWebApiConnection() {
        try {
            String apiUrl = String.format("http://%s:%d/api/v1/health", velocityHost, velocityApiPort);
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + apiSecret);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            
            int responseCode = connection.getResponseCode();
            boolean connected = responseCode == 200;
            
            if (connected) {
                plugin.getLogger().info("Velocity Web API连接正常");
            } else {
                plugin.getLogger().warning("Velocity Web API连接失败，响应码: " + responseCode);
            }
            
            return connected;
            
        } catch (Exception e) {
            plugin.getLogger().warning("无法连接到Velocity Web API: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查Socket连接
     */
    private boolean checkSocketConnection() {
        try {
            // 简单的socket连接测试
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(velocityHost, velocityApiPort), 3000);
            socket.close();
            
            plugin.getLogger().info("Velocity Socket连接正常");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().warning("无法连接到Velocity Socket: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 读取错误响应
     */
    private String readErrorResponse(HttpURLConnection connection) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            return "无法读取错误响应: " + e.getMessage();
        }
    }
    
    /**
     * 启动连接监控
     */
    public void startConnectionMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkVelocityConnection().thenAccept(connected -> {
                    if (!connected) {
                        plugin.getLogger().warning("与Velocity的连接已断开，将使用备用传输方案");
                    }
                });
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L * 60L); // 每分钟检查一次
    }
    
    /**
     * 获取在线服务器列表
     */
    public CompletableFuture<String[]> getOnlineServers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (useWebApi) {
                    return getOnlineServersViaWebApi();
                } else {
                    // Socket方式获取服务器列表
                    return new String[]{"survival", "creative"}; // 默认返回
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "获取在线服务器列表失败: " + e.getMessage(), e);
                return new String[0];
            }
        });
    }
    
    /**
     * 通过Web API获取在线服务器
     */
    private String[] getOnlineServersViaWebApi() {
        try {
            String apiUrl = String.format("http://%s:%d/api/v1/servers", velocityHost, velocityApiPort);
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + apiSecret);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            
            if (connection.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    // 简单的JSON解析（实际项目中建议使用JSON库）
                    String jsonResponse = response.toString();
                    // 这里需要根据实际的API响应格式来解析
                    return parseServerListFromJson(jsonResponse);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("获取服务器列表失败: " + e.getMessage());
        }
        
        return new String[0];
    }
    
    /**
     * 解析JSON响应中的服务器列表
     */
    private String[] parseServerListFromJson(String json) {
        // 简单的JSON解析示例
        // 实际实现时建议使用Gson或Jackson等JSON库
        try {
            java.util.List<String> servers = new java.util.ArrayList<>();
            if (json.contains("\"servers\"")) {
                // 基础的字符串解析
                String[] parts = json.split("\"");
                for (String part : parts) {
                    if (part.matches("[a-zA-Z0-9_-]+") && part.length() > 2) {
                        servers.add(part);
                    }
                }
            }
            return servers.toArray(new String[0]);
        } catch (Exception e) {
            plugin.getLogger().warning("解析服务器列表JSON失败: " + e.getMessage());
            return new String[]{"survival", "creative"}; // 默认返回
        }
    }
}
