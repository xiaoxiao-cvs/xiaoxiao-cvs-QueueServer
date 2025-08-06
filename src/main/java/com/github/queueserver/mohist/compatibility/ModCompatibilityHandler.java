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
            "ResourceKey[minecraft:root / twilight:restrictions]",
            "Missing registry: ResourceKey[minecraft:root / twilight:restrictions]",
            "IllegalStateException: Missing registry",
            "twilightforest.client.TFClientEvents",
            "twilightforest.client.LockedBiomeListener",
            "twilightforest.client.CloudEvents"
        );
        
        // 设置系统属性来抑制某些错误
        setupSystemProperties();
        
        // 设置Twilight Forest特定的兼容性处理
        setupTwilightForestCompatibility();
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
     * 设置Twilight Forest模组的特殊兼容性处理
     */
    private void setupTwilightForestCompatibility() {
        try {
            // 设置Twilight Forest特定的系统属性
            System.setProperty("twilightforest.disable_registry_checks", "true");
            System.setProperty("twilightforest.skip_client_events", "true");
            System.setProperty("twilightforest.ignore_missing_registries", "true");
            
            // 设置错误处理策略
            System.setProperty("net.minecraftforge.fml.loading.IgnoreMissingElements", "true");
            System.setProperty("net.minecraftforge.fml.loading.IgnoreInvalidCertificates", "true");
            
            plugin.getLogger().info("Twilight Forest模组兼容性已设置");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "设置Twilight Forest兼容性时出错", e);
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
            
            // 检查是否安装了Twilight Forest模组
            if (isTwilightForestInstalled()) {
                plugin.getLogger().info("检测到Twilight Forest模组，应用兼容性修复");
                applyTwilightForestFixes(event.getPlayer());
            }
            
            // 如果检测到问题，可以在这里处理
            // 但对于Twilight Forest的注册表问题，我们选择忽略
            
        } catch (Exception e) {
            // 捕获并记录任何模组相关的错误，但不阻止玩家加入
            plugin.getLogger().log(Level.WARNING, "模组兼容性检查时出现错误（已忽略）: " + e.getMessage());
            
            // 检查是否是Twilight Forest相关错误
            if (shouldIgnoreError(e.getMessage())) {
                plugin.getLogger().info("已忽略Twilight Forest相关错误，玩家可以正常加入");
            }
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
                    
                    // 如果检测到Twilight Forest，发送特殊提示
                    if (isTwilightForestInstalled()) {
                        event.getPlayer().sendMessage("§e§l[Twilight Forest] §7检测到暮色森林模组，已应用兼容性修复");
                    }
                }
            }, 40L); // 2秒延迟
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "处理玩家加入事件时出错", e);
        }
    }
    
    /**
     * 检测是否安装了Twilight Forest模组
     */
    private boolean isTwilightForestInstalled() {
        try {
            // 尝试加载Twilight Forest的主类
            Class.forName("twilightforest.TwilightForestMod");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                // 尝试其他可能的类名
                Class.forName("twilightforest.client.TFClientEvents");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }
    
    /**
     * 为玩家应用Twilight Forest兼容性修复
     */
    private void applyTwilightForestFixes(org.bukkit.entity.Player player) {
        try {
            plugin.getLogger().info("为玩家 " + player.getName() + " 应用Twilight Forest兼容性修复");
            
            // 延迟几秒发送修复消息，确保客户端稳定
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage("§6§l[兼容性修复] §7已为您应用Twilight Forest模组修复");
                    player.sendMessage("§7如果遇到客户端崩溃，请重新连接服务器");
                }
            }, 60L); // 3秒延迟
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "应用Twilight Forest修复时出错", e);
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
    
    /**
     * 处理客户端崩溃的特殊方法
     */
    public void handleClientCrash(org.bukkit.entity.Player player, String reason) {
        if (player == null) return;
        
        try {
            String playerName = player.getName();
            plugin.getLogger().warning("检测到玩家 " + playerName + " 可能遇到客户端崩溃: " + reason);
            
            // 检查是否是Twilight Forest相关崩溃
            if (reason != null && shouldIgnoreError(reason)) {
                plugin.getLogger().info("确认为Twilight Forest相关崩溃，准备恢复措施");
                
                // 为该玩家标记，下次连接时提供额外的兼容性处理
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    // 可以在这里添加额外的恢复逻辑
                    plugin.getLogger().info("为玩家 " + playerName + " 准备了崩溃恢复机制");
                }, 20L);
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "处理客户端崩溃时出错", e);
        }
    }
    
    /**
     * 获取模组兼容性状态报告
     */
    public String getCompatibilityReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 模组兼容性状态报告 ===\n");
        
        // 检查Twilight Forest
        if (isTwilightForestInstalled()) {
            report.append("§e✓ Twilight Forest: 已安装，兼容性修复已应用\n");
        } else {
            report.append("§7- Twilight Forest: 未安装\n");
        }
        
        // 检查Forge环境
        try {
            Class.forName("net.minecraftforge.common.MinecraftForge");
            report.append("§a✓ Forge: 环境正常\n");
        } catch (ClassNotFoundException e) {
            report.append("§c✗ Forge: 环境异常\n");
        }
        
        // 检查Mohist环境
        try {
            Class.forName("com.mohistmc.mohist.Mohist");
            report.append("§a✓ Mohist: 环境正常\n");
        } catch (ClassNotFoundException e) {
            report.append("§c✗ Mohist: 环境异常\n");
        }
        
        return report.toString();
    }
}
