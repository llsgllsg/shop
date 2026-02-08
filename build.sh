#!/bin/bash

# 编译脚本 - 适用于Minecraft Java版插件
echo "开始编译ZCShop插件..."

# 创建输出目录
mkdir -p output

# 检查lib目录中是否有必要的依赖
if [ ! -f "lib/spigot-api.jar" ]; then
    echo "警告: lib/spigot-api.jar 不存在，请将Spigot API的JAR文件放入lib目录"
    echo "你可以从 https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/ 下载"
fi

if [ ! -f "lib/VaultAPI.jar" ]; then
    echo "警告: lib/VaultAPI.jar 不存在，请将Vault API的JAR文件放入lib目录"
    echo "你可以从 https://jitpack.io/com/github/MilkBowl/VaultAPI/1.7.1/VaultAPI-1.7.1.jar 下载"
fi

# 编译Java文件
javac -d output -cp "lib/*" src/main/java/com/gfish/*.java

if [ $? -ne 0 ]; then
    echo "编译失败！请确保所有依赖都已正确添加到lib目录"
    exit 1
fi

# 创建插件目录结构
mkdir -p output/plugin.yml
mkdir -p output/config.yml

# 复制资源文件
cp src/main/resources/plugin.yml output/
cp src/main/resources/config.yml output/

# 创建MANIFEST.MF文件
echo "Manifest-Version: 1.0" > MANIFEST.MF
echo "Main-Class: com.gfish.Main" >> MANIFEST.MF
echo "Class-Path: ." >> MANIFEST.MF

# 打包成JAR文件
jar -cvfm output/ZCShop.jar MANIFEST.MF -C output .

if [ $? -ne 0 ]; then
    echo "打包失败！"
    exit 1
fi

# 清理临时文件
rm MANIFEST.MF
rm -rf output/plugin.yml output/config.yml

echo "编译成功！插件已生成到 output/ZCShop.jar"
echo "请确保服务器已安装Vault插件以获得最佳体验"