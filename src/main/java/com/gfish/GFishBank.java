package com.gfish;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class GFishBank extends JavaPlugin implements Listener {

    private static final Logger log = Logger.getLogger("Minecraft");
    private static Economy econ = null;
    private BankManager bankManager;

    @Override
    public void onEnable() {
        // 保存默认配置文件
        saveDefaultConfig();
        
        // 初始化银行管理器
        bankManager = new BankManager(this);
        
        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // 设置经济系统
        if (!setupEconomy() ) {
            log.severe(String.format("[%s] - 无法连接到经济系统！插件将被禁用。", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 注册PlaceholderAPI扩展（如果已安装）
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BankPlaceholderExpansion(this).register();
        }
        
        log.info(String.format("[%s] 已成功启用！", getDescription().getName()));
    }

    @Override
    public void onDisable() {
        // 关闭数据库连接
        if (bankManager != null) {
            bankManager.closeConnection();
        }
        
        log.info(String.format("[%s] 已被禁用！", getDescription().getName()));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public BankManager getBankManager() {
        return bankManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 检查并应用利息
        bankManager.checkAndApplyInterest(player);
        
        // 更新玩家名称（如果玩家改名）
        bankManager.updatePlayerName(player);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以使用银行命令！");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("bank")) {
            if (args.length == 0) {
                // 显示银行余额
                double balance = bankManager.getBalance(player);
                player.sendMessage(String.format("§a你的银行余额: %.2f 金币", balance));
                return true;
            }

            if (args[0].equalsIgnoreCase("deposit") || args[0].equalsIgnoreCase("存")) {
                if (args.length < 2) {
                    player.sendMessage("§c用法: /bank deposit <金额> 或 /bank 存 <金额>");
                    return true;
                }

                try {
                    double amount = Double.parseDouble(args[1]);
                    if (amount <= 0) {
                        player.sendMessage("§c金额必须大于0！");
                        return true;
                    }

                    if (econ.getBalance(player) < amount) {
                        player.sendMessage("§c你的金币不足！");
                        return true;
                    }

                    // 从玩家钱包扣除金币
                    econ.withdrawPlayer(player, amount);
                    
                    // 存入银行
                    if (bankManager.deposit(player, amount)) {
                        player.sendMessage(String.format("§a成功存入 %.2f 金币到银行！", amount));
                        player.sendMessage(String.format("§a当前银行余额: %.2f 金币", bankManager.getBalance(player)));
                    } else {
                        // 如果存款失败，将金币退回玩家钱包
                        econ.depositPlayer(player, amount);
                        player.sendMessage("§c存款失败！");
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("§c请输入有效的金额！");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("withdraw") || args[0].equalsIgnoreCase("取")) {
                if (args.length < 2) {
                    player.sendMessage("§c用法: /bank withdraw <金额> 或 /bank 取 <金额>");
                    return true;
                }

                try {
                    double amount = Double.parseDouble(args[1]);
                    if (amount <= 0) {
                        player.sendMessage("§c金额必须大于0！");
                        return true;
                    }

                    if (bankManager.getBalance(player) < amount) {
                        player.sendMessage("§c你的银行余额不足！");
                        return true;
                    }

                    // 从银行取出金币
                    if (bankManager.withdraw(player, amount)) {
                        // 存入玩家钱包
                        econ.depositPlayer(player, amount);
                        player.sendMessage(String.format("§a成功从银行取出 %.2f 金币！", amount));
                        player.sendMessage(String.format("§a当前银行余额: %.2f 金币", bankManager.getBalance(player)));
                    } else {
                        player.sendMessage("§c取款失败！");
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage("§c请输入有效的金额！");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("balance") || args[0].equalsIgnoreCase("余额")) {
                double balance = bankManager.getBalance(player);
                player.sendMessage(String.format("§a你的银行余额: %.2f 金币", balance));
                return true;
            }

            if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("帮助")) {
                player.sendMessage("§a=== 银行帮助 ===");
                player.sendMessage("§a/bank - 查看银行余额");
                player.sendMessage("§a/bank deposit <金额> 或 /bank 存 <金额> - 存款");
                player.sendMessage("§a/bank withdraw <金额> 或 /bank 取 <金额> - 取款");
                player.sendMessage("§a/bank balance 或 /bank 余额 - 查看银行余额");
                player.sendMessage("§a/bank help 或 /bank 帮助 - 查看帮助");
                return true;
            }

            player.sendMessage("§c未知的银行命令！输入 /bank help 查看帮助。");
            return true;
        }

        return false;
    }
}