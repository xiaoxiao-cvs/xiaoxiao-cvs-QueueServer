package com.github.queueserver.mohist.monitor;

import com.github.queueserver.mohist.QueueMohistPlugin;
import com.github.queueserver.mohist.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * 服务器状态监控器
 * 负责监控目标服务器的在线人数和状态
 */
public class ServerMonitor {
    
    private final QueueMohistPlugin plugin;
    private final ConfigManager configManager;
    private final Logger logger;
    
    private int lastKnownPlayers = -1;
    private boolean lastKnownStatus = false;
    private long lastCheckTime = 0;
    
    public ServerMonitor(QueueMohistPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.logger = plugin.getLogger();
    }
    
    /**
     * 启动监控任务
     */
    public void startMonitoring() {
        // 每30秒检查一次目标服务器状态
        new BukkitRunnable() {
            @Override
            public void run() {
                checkTargetServerStatus();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 10, 20L * 30); // 10秒后开始，每30秒检查一次
        
        // 每5分钟向公屏广播服务器状态
        new BukkitRunnable() {
            @Override
            public void run() {
                broadcastServerStatus();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60, 20L * 300); // 1分钟后开始，每5分钟广播一次
        
        logger.info("服务器监控器已启动");
    }
    
    /**
     * 检查目标服务器状态
     */
    private void checkTargetServerStatus() {
        if (configManager.isStandaloneMode()) {
            // 独立模式：直接获取当前服务器信息
            updateLocalServerStatus();
        } else {
            // 代理模式：查询目标服务器状态
            queryTargetServerStatus();
        }
    }
    
    /**
     * 更新本地服务器状态（独立模式）
     */
    private void updateLocalServerStatus() {
        int currentPlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = configManager.getMaxPlayers();
        
        if (currentPlayers != lastKnownPlayers) {
            lastKnownPlayers = currentPlayers;
            lastKnownStatus = true;
            lastCheckTime = System.currentTimeMillis();
            
            // 如果人数变化较大，立即广播
            if (Math.abs(currentPlayers - lastKnownPlayers) >= 5) {
                broadcastServerStatus();
            }
        }
    }
    
    /**
     * 查询目标服务器状态（代理模式）
     */
    private void queryTargetServerStatus() {
        String host = configManager.getTargetServerHost();
        int port = configManager.getTargetServerPort();
        
        try {
            ServerPingResult result = pingServer(host, port);
            
            if (result != null) {
                boolean statusChanged = (lastKnownStatus != true) || 
                                      (Math.abs(lastKnownPlayers - result.onlinePlayers) >= 5);
                
                lastKnownPlayers = result.onlinePlayers;
                lastKnownStatus = true;
                lastCheckTime = System.currentTimeMillis();
                
                // 如果状态有重大变化，立即广播
                if (statusChanged) {
                    Bukkit.getScheduler().runTask(plugin, this::broadcastServerStatus);
                }
                
            } else {
                // 服务器离线
                if (lastKnownStatus) {
                    lastKnownStatus = false;
                    lastCheckTime = System.currentTimeMillis();
                    Bukkit.getScheduler().runTask(plugin, this::broadcastServerOffline);
                }
            }
            
        } catch (Exception e) {
            logger.warning("查询目标服务器状态失败: " + e.getMessage());
        }
    }
    
    /**
     * Ping服务器获取状态
     */
    private ServerPingResult pingServer(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000); // 5秒超时
            
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            
            // 发送握手包
            ByteArrayOutputStream handshake = new ByteArrayOutputStream();
            DataOutputStream handshakeOut = new DataOutputStream(handshake);
            
            handshakeOut.writeByte(0x00); // 握手包ID
            writeVarInt(handshakeOut, 760); // 协议版本 (1.19.2)
            writeString(handshakeOut, host);
            handshakeOut.writeShort(port);
            writeVarInt(handshakeOut, 1); // 状态查询
            
            byte[] handshakeData = handshake.toByteArray();
            writeVarInt(out, handshakeData.length);
            out.write(handshakeData);
            
            // 发送状态请求包
            writeVarInt(out, 1); // 包长度
            out.writeByte(0x00); // 状态请求包ID
            
            // 读取响应
            int responseLength = readVarInt(in);
            int packetId = readVarInt(in);
            
            if (packetId == 0x00) {
                String jsonResponse = readString(in);
                return parseServerStatus(jsonResponse);
            }
            
        } catch (Exception e) {
            // 连接失败，服务器可能离线
            return null;
        }
        
        return null;
    }
    
    /**
     * 解析服务器状态JSON
     */
    private ServerPingResult parseServerStatus(String json) {
        try {
            // 简单的JSON解析（生产环境建议使用JSON库）
            int playersStart = json.indexOf("\"online\":");
            if (playersStart != -1) {
                playersStart += 9;
                int playersEnd = json.indexOf(",", playersStart);
                if (playersEnd == -1) playersEnd = json.indexOf("}", playersStart);
                
                String onlineStr = json.substring(playersStart, playersEnd).trim();
                int onlinePlayers = Integer.parseInt(onlineStr);
                
                int maxStart = json.indexOf("\"max\":");
                if (maxStart != -1) {
                    maxStart += 6;
                    int maxEnd = json.indexOf(",", maxStart);
                    if (maxEnd == -1) maxEnd = json.indexOf("}", maxStart);
                    
                    String maxStr = json.substring(maxStart, maxEnd).trim();
                    int maxPlayers = Integer.parseInt(maxStr);
                    
                    return new ServerPingResult(onlinePlayers, maxPlayers);
                }
            }
        } catch (Exception e) {
            logger.warning("解析服务器状态失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 向公屏广播服务器状态
     */
    private void broadcastServerStatus() {
        if (!lastKnownStatus) {
            return; // 服务器离线时不广播正常状态
        }
        
        String message;
        if (configManager.isStandaloneMode()) {
            int queueSize = plugin.getQueueManager().getQueueSize();
            message = String.format(
                "§6§l[服务器状态] §a当前在线: §e%d§7/%d §8| §a队列中: §e%d人",
                lastKnownPlayers, configManager.getMaxPlayers(), queueSize
            );
        } else {
            int queueSize = plugin.getQueueManager().getQueueSize();
            String targetName = configManager.getTargetServer();
            message = String.format(
                "§6§l[服务器状态] §a%s服务器在线: §e%d人 §8| §a队列服务器: §e%d人排队",
                targetName, lastKnownPlayers, queueSize
            );
        }
        
        // 向所有在线玩家广播
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
        
        // 记录到日志
        logger.info("广播服务器状态: " + message.replaceAll("§.", ""));
    }
    
    /**
     * 广播服务器离线消息
     */
    private void broadcastServerOffline() {
        String targetName = configManager.getTargetServer();
        String message = String.format(
            "§c§l[服务器状态] §c%s服务器暂时离线，请稍后再试", 
            targetName
        );
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
        
        logger.warning("目标服务器离线: " + targetName);
    }
    
    /**
     * 获取当前已知的目标服务器玩家数
     */
    public int getCurrentKnownPlayers() {
        return lastKnownPlayers;
    }
    
    /**
     * 获取目标服务器是否在线
     */
    public boolean isTargetServerOnline() {
        return lastKnownStatus;
    }
    
    /**
     * 手动触发状态检查
     */
    public void forceCheck() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::checkTargetServerStatus);
    }
    
    // 辅助方法
    private void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0) {
            out.writeByte(value & 0x7F | 0x80);
            value >>>= 7;
        }
        out.writeByte(value & 0x7F);
    }
    
    private int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int length = 0;
        byte currentByte;
        
        do {
            currentByte = in.readByte();
            value |= (currentByte & 0x7F) << (length * 7);
            length++;
            if (length > 5) {
                throw new RuntimeException("VarInt太长");
            }
        } while ((currentByte & 0x80) == 0x80);
        
        return value;
    }
    
    private void writeString(DataOutputStream out, String string) throws IOException {
        byte[] bytes = string.getBytes("UTF-8");
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }
    
    private String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, "UTF-8");
    }
    
    /**
     * 服务器Ping结果类
     */
    private static class ServerPingResult {
        public final int onlinePlayers;
        public final int maxPlayers;
        
        public ServerPingResult(int onlinePlayers, int maxPlayers) {
            this.onlinePlayers = onlinePlayers;
            this.maxPlayers = maxPlayers;
        }
    }
}
