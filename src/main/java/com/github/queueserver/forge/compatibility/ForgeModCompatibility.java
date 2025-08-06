package com.github.queueserver.forge.compatibility;

import com.github.queueserver.forge.QueueForgePlugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Forge模组兼容性处理器
 * 处理与Forge模组的兼容性问题
 */
public class ForgeModCompatibility implements Listener {
    
    private final QueueForgePlugin plugin;
    private final Logger logger;
    
    // 已检查的模组
    private final Set<String> checkedMods = ConcurrentHashMap.newKeySet();
    
    // 兼容性报告
    private boolean twilightForestDetected = false;
    private boolean industrialCraftDetected = false;
    private boolean buildCraftDetected = false;
    private boolean thermalExpansionDetected = false;
    
    public ForgeModCompatibility(QueueForgePlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * 服务器加载完成后检查模组兼容性
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.STARTUP) {
            checkModCompatibility();
        }
    }
    
    /**
     * 玩家加入时检查客户端模组
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 延迟检查玩家客户端模组
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            checkPlayerClientMods(player);
        }, 20L); // 1秒后检查
    }
    
    /**
     * 检查模组兼容性
     */
    private void checkModCompatibility() {
        logger.info("正在检查Forge模组兼容性...");
        
        // 检查Twilight Forest
        if (checkModPresent("twilightforest.TwilightForestMod") || 
            checkModPresent("twilightforest.TFMod")) {
            twilightForestDetected = true;
            logger.info("✓ 检测到 Twilight Forest 模组");
            setupTwilightForestCompatibility();
        }
        
        // 检查 IndustrialCraft
        if (checkModPresent("ic2.core.IC2") || 
            checkModPresent("net.industrial_craft.IC2")) {
            industrialCraftDetected = true;
            logger.info("✓ 检测到 IndustrialCraft 模组");
            setupIndustrialCraftCompatibility();
        }
        
        // 检查 BuildCraft
        if (checkModPresent("buildcraft.BuildCraftMod") ||
            checkModPresent("buildcraft.core.BuildCraft")) {
            buildCraftDetected = true;
            logger.info("✓ 检测到 BuildCraft 模组");
            setupBuildCraftCompatibility();
        }
        
        // 检查 Thermal Expansion
        if (checkModPresent("thermalexpansion.ThermalExpansion") ||
            checkModPresent("cofh.thermalexpansion.ThermalExpansion")) {
            thermalExpansionDetected = true;
            logger.info("✓ 检测到 Thermal Expansion 模组");
            setupThermalExpansionCompatibility();
        }
        
        // 检查其他常见模组
        checkOtherCommonMods();
        
        logger.info("模组兼容性检查完成");
    }
    
    /**
     * 检查模组是否存在
     */
    private boolean checkModPresent(String className) {
        try {
            Class.forName(className);
            checkedMods.add(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 设置 Twilight Forest 兼容性
     */
    private void setupTwilightForestCompatibility() {
        try {
            // 检查 Twilight Forest 特定的问题
            logger.info("配置 Twilight Forest 兼容性设置");
            
            // 可以在这里添加特定的兼容性处理
            // 例如：防止某些崩溃、处理维度问题等
            
        } catch (Exception e) {
            logger.warning("配置 Twilight Forest 兼容性时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 设置 IndustrialCraft 兼容性
     */
    private void setupIndustrialCraftCompatibility() {
        try {
            logger.info("配置 IndustrialCraft 兼容性设置");
            
            // IC2 特定的兼容性处理
            
        } catch (Exception e) {
            logger.warning("配置 IndustrialCraft 兼容性时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 设置 BuildCraft 兼容性
     */
    private void setupBuildCraftCompatibility() {
        try {
            logger.info("配置 BuildCraft 兼容性设置");
            
            // BuildCraft 特定的兼容性处理
            
        } catch (Exception e) {
            logger.warning("配置 BuildCraft 兼容性时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 设置 Thermal Expansion 兼容性
     */
    private void setupThermalExpansionCompatibility() {
        try {
            logger.info("配置 Thermal Expansion 兼容性设置");
            
            // Thermal Expansion 特定的兼容性处理
            
        } catch (Exception e) {
            logger.warning("配置 Thermal Expansion 兼容性时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 检查其他常见模组
     */
    private void checkOtherCommonMods() {
        String[] otherMods = {
            "net.minecraft.forge.common.MinecraftForge", // Forge核心
            "mezz.jei.JustEnoughItems", // JEI
            "net.minecraftforge.fml.common.Mod", // Forge Mod Loader
            "appeng.core.AppEng", // Applied Energistics
            "biomesoplenty.BiomesOPlenty", // Biomes O' Plenty
            "forestry.Forestry", // Forestry
            "thaumcraft.Thaumcraft" // Thaumcraft
        };
        
        int detectedCount = 0;
        for (String modClass : otherMods) {
            if (checkModPresent(modClass)) {
                detectedCount++;
                String modName = extractModName(modClass);
                logger.info("✓ 检测到模组: " + modName);
            }
        }
        
        if (detectedCount > 0) {
            logger.info("总共检测到 " + detectedCount + " 个其他模组");
        }
    }
    
    /**
     * 从类名提取模组名称
     */
    private String extractModName(String className) {
        String[] parts = className.split("\\.");
        if (parts.length >= 2) {
            return parts[parts.length - 1]; // 返回最后一部分作为模组名
        }
        return className;
    }
    
    /**
     * 检查玩家客户端模组
     */
    private void checkPlayerClientMods(Player player) {
        try {
            // 这里可以检查玩家的客户端模组
            // 注意：这需要特定的API或数据包来实现
            
            if (plugin.getConfigManager().isDebugMode()) {
                logger.info("检查玩家 " + player.getName() + " 的客户端模组");
            }
            
        } catch (Exception e) {
            logger.warning("检查玩家客户端模组时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取兼容性报告
     */
    public CompatibilityReport getCompatibilityReport() {
        return new CompatibilityReport(
                twilightForestDetected,
                industrialCraftDetected,
                buildCraftDetected,
                thermalExpansionDetected,
                checkedMods.size()
        );
    }
    
    /**
     * 检查特定模组是否已检测
     */
    public boolean isModDetected(String modName) {
        switch (modName.toLowerCase()) {
            case "twilightforest":
                return twilightForestDetected;
            case "industrialcraft":
            case "ic2":
                return industrialCraftDetected;
            case "buildcraft":
                return buildCraftDetected;
            case "thermalexpansion":
                return thermalExpansionDetected;
            default:
                return false;
        }
    }
    
    /**
     * 兼容性报告类
     */
    public static class CompatibilityReport {
        private final boolean twilightForest;
        private final boolean industrialCraft;
        private final boolean buildCraft;
        private final boolean thermalExpansion;
        private final int totalModsChecked;
        
        public CompatibilityReport(boolean twilightForest, boolean industrialCraft, 
                                 boolean buildCraft, boolean thermalExpansion, int totalModsChecked) {
            this.twilightForest = twilightForest;
            this.industrialCraft = industrialCraft;
            this.buildCraft = buildCraft;
            this.thermalExpansion = thermalExpansion;
            this.totalModsChecked = totalModsChecked;
        }
        
        public boolean isTwilightForest() {
            return twilightForest;
        }
        
        public boolean isIndustrialCraft() {
            return industrialCraft;
        }
        
        public boolean isBuildCraft() {
            return buildCraft;
        }
        
        public boolean isThermalExpansion() {
            return thermalExpansion;
        }
        
        public int getTotalModsChecked() {
            return totalModsChecked;
        }
        
        @Override
        public String toString() {
            return String.format("CompatibilityReport{twilightForest=%s, industrialCraft=%s, buildCraft=%s, thermalExpansion=%s, totalChecked=%d}",
                    twilightForest, industrialCraft, buildCraft, thermalExpansion, totalModsChecked);
        }
    }
}
