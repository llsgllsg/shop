package com.gfish;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BankPlaceholderExpansion extends PlaceholderExpansion {

    private final GFishBank plugin;

    public BankPlaceholderExpansion(GFishBank plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gfishbank";
    }

    @Override
    public @NotNull String getAuthor() {
        return "YourName";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // 这个扩展应该持久存在
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        if (params.equals("balance")) {
            // 返回玩家的银行余额
            return String.format("%.2f", plugin.getBankManager().getBalance(player.getUniqueId()));
        }

        if (params.equals("formatted_balance")) {
            // 返回格式化的银行余额（添加千位分隔符）
            double balance = plugin.getBankManager().getBalance(player.getUniqueId());
            return formatNumber(balance);
        }

        if (params.equals("has_account")) {
            // 检查玩家是否有银行账户
            try {
                plugin.getBankManager().getBalance(player.getUniqueId());
                return "true";
            } catch (Exception e) {
                return "false";
            }
        }

        return null; // 如果参数不匹配任何已知的占位符，返回null
    }

    private String formatNumber(double number) {
        return String.format("%,.2f", number);
    }
}