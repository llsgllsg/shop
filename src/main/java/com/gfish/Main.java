package com.gfish;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;

public class Main extends JavaPlugin {

    private FileConfiguration config;
    private Map<String, ShopItem> shopItems = new HashMap<>();

    @Override
    public void onEnable() {
        // 加载配置文件
        this.saveDefaultConfig();
        this.config = this.getConfig();
        
        // 加载商店物品
        this.loadShopItems();
        
        this.getLogger().info(ChatColor.GREEN + "ZCShop 插件已启用！");
    }

    @Override
    public void onDisable() {
        this.getLogger().info(ChatColor.RED + "ZCShop 插件已禁用！");
    }

    private void loadShopItems() {
        for (String key : this.config.getKeys(false)) {
            if (this.config.isConfigurationSection(key)) {
                String variable = this.config.getString(key + ".变量");
                int requiredValue = this.config.getInt(key + ".购买数字", 0);
                String command = this.config.getString(key + ".超过执行的命令");
                
                if (variable != null && command != null) {
                    this.shopItems.put(key, new ShopItem(key, variable, requiredValue, command));
                    this.getLogger().info("已加载商店物品: " + key);
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("zcshop")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "此命令只能由玩家使用！");
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 1) {
                player.sendMessage(ChatColor.YELLOW + "用法: /zcshop <物品名称>");
                return true;
            }

            String itemName = args[0];
            ShopItem item = this.shopItems.get(itemName);

            if (item == null) {
                player.sendMessage(ChatColor.RED + "未找到名为 " + itemName + " 的物品！");
                return true;
            }

            // 获取玩家变量值（这里需要根据实际情况修改，例如从经济插件获取）
            int playerVariableValue = this.getPlayerVariableValue(player, item.getVariable());

            if (playerVariableValue >= item.getRequiredValue()) {
                // 执行命令
                this.executeCommand(player, item.getCommand());
                player.sendMessage(ChatColor.GREEN + "购买成功！");
            } else {
                player.sendMessage(ChatColor.RED + "你的 " + item.getVariable() + " 不足！需要 " + item.getRequiredValue() + "，当前仅有 " + playerVariableValue);
            }

            return true;
        }
        return false;
    }

    private int getPlayerVariableValue(Player player, String variable) {
        // 这里需要根据实际情况修改，例如从经济插件获取玩家的货币
        // 这里支持多种常见的经济插件
        if (variable.equals("%bank%")) {
            // Vault经济API支持（推荐）
            try {
                net.milkbowl.vault.economy.Economy economy = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class).getProvider();
                if (economy != null) {
                    return (int) economy.getBalance(player);
                }
            } catch (Exception e) {
                // Vault不可用，尝试其他经济插件
            }
            
            // Essentials经济支持
            try {
                com.earth2me.essentials.Essentials essentials = (com.earth2me.essentials.Essentials) getServer().getPluginManager().getPlugin("Essentials");
                if (essentials != null && essentials.getEconomy() != null) {
                    return (int) essentials.getEconomy().getMoney(player.getName());
                }
            } catch (Exception e) {
                // Essentials不可用
            }
            
            // 如果没有经济插件，返回示例值
            return 0;
        } else if (variable.startsWith("%scoreboard:")) {
            // 支持计分板变量，格式: %scoreboard:objectiveName%
            try {
                String objectiveName = variable.substring(12, variable.length() - 1);
                return player.getScoreboard().getObjective(objectiveName).getScore(player).getScore();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    private void executeCommand(Player player, String command) {
        // 替换命令中的变量
        command = command.replace("%player%", player.getName());
        
        // 执行命令
        this.getServer().dispatchCommand(this.getServer().getConsoleSender(), command);
    }

    public class ShopItem {
        private String name;
        private String variable;
        private int requiredValue;
        private String command;

        public ShopItem(String name, String variable, int requiredValue, String command) {
            this.name = name;
            this.variable = variable;
            this.requiredValue = requiredValue;
            this.command = command;
        }

        public String getName() {
            return name;
        }

        public String getVariable() {
            return variable;
        }

        public int getRequiredValue() {
            return requiredValue;
        }

        public String getCommand() {
            return command;
        }
    }
}