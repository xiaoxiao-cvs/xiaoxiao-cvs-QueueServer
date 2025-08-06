package com.github.queueserver.forge.listeners;

import com.github.queueserver.forge.QueueForgePlugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.logging.Logger;

/**
 * Forge事件监听器
 * 处理Forge相关的服务器事件
 */
public class ForgeEventListener implements Listener {
    
    private final QueueForgePlugin plugin;
    private final Logger logger;
    
    public ForgeEventListener(QueueForgePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * 服务器加载完成事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.STARTUP) {
            logger.info("服务器启动加载完成");
            
            // 检查Forge模组加载情况
            checkForgeModsLoaded();
            
        } else if (event.getType() == ServerLoadEvent.LoadType.RELOAD) {
            logger.info("服务器重载完成");
            
            // 重载配置
            try {
                plugin.getConfigManager().reloadConfig();
                logger.info("插件配置已重载");
            } catch (Exception e) {
                logger.severe("重载配置失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 世界加载事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        String worldName = event.getWorld().getName();
        logger.info("世界已加载: " + worldName);
        
        // 检查是否为Forge模组世界
        if (isForgeModWorld(worldName)) {
            logger.info("检测到Forge模组世界: " + worldName);
        }
    }
    
    /**
     * 世界卸载事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        String worldName = event.getWorld().getName();
        logger.info("世界已卸载: " + worldName);
    }
    
    /**
     * 检查Forge模组加载情况
     */
    private void checkForgeModsLoaded() {
        try {
            // 检查常见的Forge模组
            checkCommonMods();
            
            // 统计加载的世界数量
            int worldCount = plugin.getServer().getWorlds().size();
            logger.info("已加载 " + worldCount + " 个世界");
            
        } catch (Exception e) {
            logger.warning("检查Forge模组时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 检查常见模组
     */
    private void checkCommonMods() {
        // 检查一些常见的Forge模组类
        String[] commonMods = {
            "net.minecraftforge.common.MinecraftForge",
            "net.minecraft.world.level.biome.Biome",
            "net.minecraft.world.item.Item",
            "net.minecraft.world.level.block.Block"
        };
        
        int detectedMods = 0;
        for (String modClass : commonMods) {
            try {
                Class.forName(modClass);
                detectedMods++;
            } catch (ClassNotFoundException e) {
                // 模组未加载
            }
        }
        
        if (detectedMods > 0) {
            logger.info("检测到 " + detectedMods + " 个Forge核心组件");
        }
        
        // 检查特定的模组
        checkSpecificMods();
    }
    
    /**
     * 检查特定模组
     */
    private void checkSpecificMods() {
        // 检查一些流行的模组
        String[] popularMods = {
            "net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate",
            "net.minecraft.world.entity.Entity",
            "net.minecraft.world.level.chunk.ChunkAccess"
        };
        
        for (String modClass : popularMods) {
            try {
                Class.forName(modClass);
                String modName = extractModName(modClass);
                if (!modName.isEmpty()) {
                    logger.info("检测到模组组件: " + modName);
                }
            } catch (ClassNotFoundException e) {
                // 模组未加载
            }
        }
    }
    
    /**
     * 从类名提取模组名称
     */
    private String extractModName(String className) {
        try {
            String[] parts = className.split("\\.");
            if (parts.length >= 3) {
                return parts[2]; // 通常模组名在第三部分
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return "";
    }
    
    /**
     * 检查是否为Forge模组世界
     */
    private boolean isForgeModWorld(String worldName) {
        // 一些常见的模组世界名称模式
        String[] modWorldPatterns = {
            "twilightforest",
            "the_twilight_forest", 
            "aether",
            "the_aether",
            "nether",
            "the_nether",
            "end",
            "the_end",
            "mining",
            "industrial",
            "tech",
            "magic",
            "dimension"
        };
        
        String lowerWorldName = worldName.toLowerCase();
        for (String pattern : modWorldPatterns) {
            if (lowerWorldName.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取服务器性能信息
     */
    public String getServerPerformanceInfo() {
        try {
            StringBuilder info = new StringBuilder();
            
            // 基本信息
            info.append("在线玩家: ").append(plugin.getServer().getOnlinePlayers().size())
                .append("/").append(plugin.getServer().getMaxPlayers()).append("\n");
            
            // 世界信息
            info.append("已加载世界: ").append(plugin.getServer().getWorlds().size()).append("\n");
            
            // TPS信息
            try {
                double[] tps = plugin.getServer().getTPS();
                if (tps != null && tps.length > 0) {
                    info.append("TPS: ").append(String.format("%.2f", tps[0])).append("\n");
                }
            } catch (Exception e) {
                info.append("TPS: 无法获取\n");
            }
            
            // 内存信息
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024;
            long totalMemory = runtime.totalMemory() / 1024 / 1024;
            long freeMemory = runtime.freeMemory() / 1024 / 1024;
            long usedMemory = totalMemory - freeMemory;
            
            info.append("内存使用: ").append(usedMemory).append("MB / ").append(maxMemory).append("MB");
            
            return info.toString();
            
        } catch (Exception e) {
            return "性能信息获取失败: " + e.getMessage();
        }
    }
}
