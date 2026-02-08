# ZCShop 插件

这是一个适用于 Minecraft Java 版服务器的简单商店插件，允许服务器管理员通过配置文件定义商品，玩家可以使用命令购买这些商品。

## 功能特点

- 通过配置文件定义商店商品
- 支持变量检查（例如检查玩家的银行余额）
- 购买成功后执行自定义命令
- 简单易用的命令界面

## 安装方法

1. 下载插件的 JAR 文件
2. 将 JAR 文件放入服务器的 `plugins` 文件夹中
3. 重启服务器
4. 配置 `plugins/ZCShop/config.yml` 文件

## 配置文件说明

配置文件位于 `plugins/ZCShop/config.yml`，格式如下：

```yaml
物品名称:
  变量: "%bank%" # 要检查的变量，例如 %bank% 表示玩家的银行余额
  购买数字: 100 # 需要的变量值
  超过执行的命令: "give %player% diamond 1" # 购买成功后执行的命令，%player% 会被替换为玩家名
```

## 使用方法

玩家可以使用以下命令购买商品：

```
/zcshop <物品名称>
```

例如，要购买名为"钻石"的商品，玩家可以输入：

```
/zcshop 钻石
```

## 注意事项

- 插件默认使用 `%bank%` 变量来检查玩家的余额，但这需要你手动在 `getPlayerVariableValue` 方法中实现获取玩家余额的逻辑
- 你需要根据你使用的经济插件来修改 `getPlayerVariableValue` 方法
- 命令中的 `%player%` 会被自动替换为购买商品的玩家名

## 示例配置

```yaml
钻石:
  变量: "%bank%"
  购买数字: 100
  超过执行的命令: "give %player% diamond 1"

铁剑:
  变量: "%bank%"
  购买数字: 50
  超过执行的命令: "give %player% iron_sword 1"

金苹果:
  变量: "%bank%"
  购买数字: 200
  超过执行的命令: "give %player% golden_apple 1"
```