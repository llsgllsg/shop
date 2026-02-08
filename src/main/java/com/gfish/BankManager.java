package com.gfish;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.UUID;

public class BankManager {

    private final GFishBank plugin;
    private Connection connection;

    public BankManager(GFishBank plugin) {
        this.plugin = plugin;
        setupDatabase();
    }

    private void setupDatabase() {
        try {
            // 初始化H2数据库连接
            connection = DriverManager.getConnection("jdbc:h2:" + plugin.getDataFolder() + "/bank", "sa", "");
            
            // 创建账户表
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS bank_accounts (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "player_name VARCHAR(16) NOT NULL," +
                        "balance DOUBLE DEFAULT 0.0," +
                        "last_interest_time BIGINT DEFAULT 0" +
                        ")");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("无法初始化银行数据库: " + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("关闭数据库连接时出错: " + e.getMessage());
        }
    }

    // 获取玩家银行余额
    public double getBalance(Player player) {
        return getBalance(player.getUniqueId());
    }

    public double getBalance(UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance FROM bank_accounts WHERE uuid = ?")) {
            statement.setString(1, uuid.toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("balance");
                } else {
                    // 如果账户不存在，创建一个新账户
                    createAccount(uuid, Bukkit.getPlayer(uuid).getName());
                    return 0.0;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取银行余额时出错: " + e.getMessage());
            return 0.0;
        }
    }

    // 存款
    public boolean deposit(Player player, double amount) {
        if (amount <= 0) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        double currentBalance = getBalance(uuid);
        double newBalance = currentBalance + amount;

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE bank_accounts SET balance = ? WHERE uuid = ?")) {
            statement.setDouble(1, newBalance);
            statement.setString(2, uuid.toString());
            
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                // 如果更新失败，可能是账户不存在，创建新账户
                createAccount(uuid, player.getName());
                return deposit(player, amount);
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("存款时出错: " + e.getMessage());
            return false;
        }
    }

    // 取款
    public boolean withdraw(Player player, double amount) {
        if (amount <= 0) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        double currentBalance = getBalance(uuid);

        if (currentBalance < amount) {
            return false; // 余额不足
        }

        double newBalance = currentBalance - amount;

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE bank_accounts SET balance = ? WHERE uuid = ?")) {
            statement.setDouble(1, newBalance);
            statement.setString(2, uuid.toString());
            
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("取款时出错: " + e.getMessage());
            return false;
        }
    }

    // 创建新账户
    public void createAccount(UUID uuid, String playerName) {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO bank_accounts (uuid, player_name, balance, last_interest_time) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, playerName);
            statement.setDouble(3, 0.0);
            statement.setLong(4, System.currentTimeMillis());
            
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("创建银行账户时出错: " + e.getMessage());
        }
    }

    // 更新玩家最后一次获得利息的时间
    public void updateLastInterestTime(Player player) {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE bank_accounts SET last_interest_time = ? WHERE uuid = ?")) {
            statement.setLong(1, System.currentTimeMillis());
            statement.setString(2, player.getUniqueId().toString());
            
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("更新利息时间时出错: " + e.getMessage());
        }
    }

    // 获取玩家最后一次获得利息的时间
    public long getLastInterestTime(Player player) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT last_interest_time FROM bank_accounts WHERE uuid = ?")) {
            statement.setString(1, player.getUniqueId().toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("last_interest_time");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("获取利息时间时出错: " + e.getMessage());
        }
        return 0;
    }

    // 检查并发放利息
    public void checkAndApplyInterest(Player player) {
        long lastInterestTime = getLastInterestTime(player);
        long currentTime = System.currentTimeMillis();
        long interestInterval = plugin.getConfig().getLong("interest.interval", 86400000); // 默认24小时
        
        if (currentTime - lastInterestTime >= interestInterval) {
            double balance = getBalance(player);
            double interestRate = plugin.getConfig().getDouble("interest.rate", 0.01); // 默认1%利率
            double interest = balance * interestRate;
            
            if (interest > 0) {
                deposit(player, interest);
                updateLastInterestTime(player);
                
                player.sendMessage(String.format("§a你获得了 %.2f 金币的银行利息！", interest));
                player.sendMessage(String.format("§a当前银行余额: %.2f 金币", getBalance(player)));
            }
        }
    }

    // 更新玩家名称（如果玩家改名）
    public void updatePlayerName(Player player) {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE bank_accounts SET player_name = ? WHERE uuid = ?")) {
            statement.setString(1, player.getName());
            statement.setString(2, player.getUniqueId().toString());
            
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("更新玩家名称时出错: " + e.getMessage());
        }
    }
}