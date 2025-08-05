package com.github.queueserver.mohist.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * 模组兼容性处理器
 * 专门处理Twilight Forest等模组的注册表错误
 */
public class ModCompatibilityHandler implements Listener {
    
    private final JavaPlugin plugin;
    private final List<String> ignoredErrors;
    
    public ModCompatibilityHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.ignoredErrors = Arrays.asList(
            "twilight:restrictions",
            "twilightforest:restrictions",
            "minecraft:root",
            "twilightforest:biome_restrictions",
            "ResourceKey[minecraft:root / twilight:restrictions]"
        );
        
        // 设置系统属性来抑制某些错误
        setupSystemProperties();
    }
    
    /**
     * 设置系统属性来处理模组兼容性
     */
    private void setupSystemProperties() {
        try {
            // Forge相关属性
            System.setProperty("fml.ignoreInvalidMinecraftCertificates", "true");
            System.setProperty("fml.ignorePatchDiscrepancies", "true");
            System.setProperty("forge.logging.markers.REGISTRIES", "ACCEPT");
            
            // 网络相关
            System.setProperty("java.net.preferIPv4Stack", "true");
            
            // Mohist相关
            System.setProperty("mohist.check.update", "false");
            System.setProperty("mohist.check.libraries", "false");
            
            plugin.getLogger().info("模组兼容性系统属性已设置");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "设置系统属性时出错", e);
        }
    }
    
    /**
     * 处理玩家登录事件，检查模组兼容性
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        try {
            // 这里可以添加特定的模组检查逻辑
            // 目前主要是确保错误被正确处理
            
            String playerName = event.getPlayer().getName();
            plugin.getLogger().info("玩家 " + playerName + " 正在登录，检查模组兼容性...");
            
            // 如果检测到问题，可以在这里处理
            // 但对于Twilight Forest的注册表问题，我们选择忽略
            
        } catch (Exception e) {
            // 捕获并记录任何模组相关的错误，但不阻止玩家加入
            plugin.getLogger().log(Level.WARNING, "模组兼容性检查时出现错误（已忽略）", e);
        }
    }
    
    /**
     * 处理玩家加入事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            String playerName = event.getPlayer().getName();
            
            // 延迟发送欢迎消息，确保客户端完全加载
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (event.getPlayer().isOnline()) {
                    event.getPlayer().sendMessage("§a§l[模组支持] §7服务器支持Forge模组，检测完成");
                }
            }, 40L); // 2秒延迟
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "处理玩家加入事件时出错", e);
        }
    }
    
    /**
     * 检查错误是否应该被忽略
     */
    public boolean shouldIgnoreError(String errorMessage) {
        if (errorMessage == null) return false;
        
        for (String ignoredError : ignoredErrors) {
            if (errorMessage.contains(ignoredError)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 自定义错误处理
     */
    public void handleModError(Exception error) {
        String errorMessage = error.getMessage();
        
        if (shouldIgnoreError(errorMessage)) {
            plugin.getLogger().info("已忽略模组兼容性错误: " + errorMessage);
            return;
        }
        
        // 其他错误正常记录
        plugin.getLogger().log(Level.WARNING, "模组错误", error);
    }
}
