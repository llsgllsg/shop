package com.gfish;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GFishPlugin extends JavaPlugin {

    private FileConfiguration shopConfig;
    private File shopConfigFile;
    private Economy economy;

    @Override
    public void onEnable() {
        // 初始化配置文件
        saveDefaultConfig();
        
        // 初始化商店配置文件
        initShopConfig();
        
        // 初始化经济系统
        setupEconomy();
        
        getLogger().info("GFish插件已启用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("GFish插件已禁用！");
    }

    private void initShopConfig() {
        shopConfigFile = new File(getDataFolder(), "shop.yml");
        if (!shopConfigFile.exists()) {
            shopConfigFile.getParentFile().mkdirs();
            saveResource("shop.yml", false);
        }
        shopConfig = YamlConfiguration.loadConfiguration(shopConfigFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("zcshop")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("只有玩家可以使用此命令！");
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 1) {
                player.sendMessage("§c用法: /zcshop <商品名称>");
                return true;
            }

            String shopName = args[0];
            
            // 检查商品是否存在
            if (!shopConfig.contains(shopName)) {
                player.sendMessage("§c商品 " + shopName + " 不存在！");
                return true;
            }

            // 获取商品配置
            String variable = shopConfig.getString(shopName + ".变量");
            int requiredValue = shopConfig.getInt(shopName + ".购买数字");
            List<String> commands = shopConfig.getStringList(shopName + ".执行命令");

            // 获取玩家变量值（这里简化处理，实际可能需要从其他插件获取）
            int playerVariableValue = getPlayerVariableValue(player, variable);

            // 检查变量值是否满足要求
            if (playerVariableValue < requiredValue) {
                player.sendMessage("§c你的" + variable.replace("%", "") + "不足，需要" + requiredValue + "，当前只有" + playerVariableValue);
                return true;
            }
            
            // 如果是银行变量，扣除相应金额
            if (variable.equals("%bank%") && economy != null) {
                economy.withdrawPlayer(player, requiredValue);
                player.sendMessage("§a已扣除 " + requiredValue + " 金币！");
            }

            // 执行命令
            for (String cmd : commands) {
                // 替换命令中的变量
                String processedCmd = processCommandVariables(cmd, player, variable, playerVariableValue);
                getServer().dispatchCommand(getServer().getConsoleSender(), processedCmd);
            }

            player.sendMessage("§a购买成功！");
            return true;
        }

        return false;
    }

    // 处理命令中的变量替换
    private String processCommandVariables(String cmd, Player player, String variable, int variableValue) {
        String processedCmd = cmd;
        
        // 替换玩家名称
        processedCmd = processedCmd.replace("%player%", player.getName());
        
        // 替换变量
        processedCmd = processedCmd.replace(variable, String.valueOf(variableValue));
        
        return processedCmd;
    }

    // 设置经济系统
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault插件未找到！经济功能将不可用。");
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("无法找到经济服务！经济功能将不可用。");
            return false;
        }
        
        economy = rsp.getProvider();
        getLogger().info("成功连接到经济系统！");
        return true;
    }

    // 获取玩家变量值
    private int getPlayerVariableValue(Player player, String variable) {
        if (variable.equals("%bank%") && economy != null) {
            // 从Vault获取玩家的余额
            return (int) economy.getBalance(player);
        }
        
        // 默认返回0
        return 0;
    }
}