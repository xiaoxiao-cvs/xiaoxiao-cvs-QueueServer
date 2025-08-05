package com.github.queueserver.mohist.listeners;

import com.github.queueserver.mohist.QueueMohistPlugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

/**
 * Mohist环境下的服务器事件监听器
 */
public class ServerListener implements Listener {
    
    private final QueueMohistPlugin plugin;
    
    public ServerListener(QueueMohistPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 服务器加载完成事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerLoad(ServerLoadEvent event) {
        plugin.getLogger().info("服务器加载完成，队列系统准备就绪");
        
        // 检查Mohist环境状态
        checkMohistEnvironment();
        
        // 初始化Forge兼容性检查
        checkForgeCompatibility();
    }
    
    /**
     * 插件启用事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        String pluginName = event.getPlugin().getName();
        
        // 检查重要插件的启用
        if (isImportantPlugin(pluginName)) {
            plugin.getLogger().info("检测到重要插件启用: " + pluginName);
            
            // 如果是权限插件，重新初始化VIP系统
            if (isPermissionPlugin(pluginName)) {
                plugin.getVipManager().reloadConfig();
            }
        }
    }
    
    /**
     * 插件禁用事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        String pluginName = event.getPlugin().getName();
        
        // 检查重要插件的禁用
        if (isImportantPlugin(pluginName)) {
            plugin.getLogger().warning("重要插件已禁用: " + pluginName);
            
            // 如果是权限插件，可能影响VIP功能
            if (isPermissionPlugin(pluginName)) {
                plugin.getLogger().warning("权限插件已禁用，VIP功能可能受到影响");
            }
        }
    }
    
    /**
     * 检查Mohist环境
     */
    private void checkMohistEnvironment() {
        try {
            // 检查Mohist版本
            Class<?> mohistClass = Class.forName("com.mohistmc.mohist.Mohist");
            String version = getFieldValue(mohistClass, "VERSION");
            
            plugin.getLogger().info("Mohist版本: " + (version != null ? version : "未知"));
            
            // 检查Forge版本
            try {
                Class<?> forgeClass = Class.forName("net.minecraftforge.common.ForgeVersion");
                String forgeVersion = getFieldValue(forgeClass, "VERSION");
                plugin.getLogger().info("Forge版本: " + (forgeVersion != null ? forgeVersion : "未知"));
            } catch (ClassNotFoundException e) {
                plugin.getLogger().warning("未检测到Forge环境");
            }
            
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("未在Mohist环境中运行！");
        }
    }
    
    /**
     * 检查Forge兼容性
     */
    private void checkForgeCompatibility() {
        try {
            // 检查常见的Forge Mod
            checkForMod("net.minecraftforge.fml.ModContainer", "Forge Mod Loader");
            checkForMod("cpw.mods.fml.common.Mod", "旧版Forge");
            
            // 检查网络相关的Mod
            checkForMod("net.minecraftforge.network.NetworkRegistry", "Forge网络系统");
            
            plugin.getLogger().info("Forge兼容性检查完成");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Forge兼容性检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查特定Mod是否存在
     */
    private void checkForMod(String className, String modName) {
        try {
            Class.forName(className);
            plugin.getLogger().info("✓ 检测到: " + modName);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("✗ 未检测到: " + modName);
        }
    }
    
    /**
     * 获取类的静态字段值
     */
    private String getFieldValue(Class<?> clazz, String fieldName) {
        try {
            java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(null);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 检查是否为重要插件
     */
    private boolean isImportantPlugin(String pluginName) {
        String[] importantPlugins = {
            "LuckPerms",
            "PermissionsEx", 
            "GroupManager",
            "Vault",
            "PlaceholderAPI",
            "Essentials",
            "WorldEdit",
            "WorldGuard"
        };
        
        for (String important : importantPlugins) {
            if (pluginName.equalsIgnoreCase(important)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查是否为权限插件
     */
    private boolean isPermissionPlugin(String pluginName) {
        String[] permissionPlugins = {
            "LuckPerms",
            "PermissionsEx",
            "GroupManager",
            "PowerfulPerms"
        };
        
        for (String permission : permissionPlugins) {
            if (pluginName.equalsIgnoreCase(permission)) {
                return true;
            }
        }
        
        return false;
    }
}
