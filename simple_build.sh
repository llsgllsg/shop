#!/bin/bash

# 简化的编译脚本 - 不依赖Maven下载
echo "开始编译ZCShop插件..."

# 创建输出目录
mkdir -p output

# 编译Java文件（不依赖外部下载）
echo "编译Java源代码..."
javac -d output -cp "src/main/java" src/main/java/com/gfish/Main.java

if [ $? -ne 0 ]; then
    echo "编译失败！请确保Java环境正确配置"
    echo "提示：你可能需要手动下载依赖并放入lib目录，然后使用以下命令："
    echo "javac -d output -cp \"lib/*:src/main/java\" src/main/java/com/gfish/Main.java"
    exit 1
fi

# 复制资源文件
echo "复制资源文件..."
cp src/main/resources/plugin.yml output/
cp src/main/resources/config.yml output/

# 创建MANIFEST.MF文件
echo "创建MANIFEST文件..."
echo "Manifest-Version: 1.0" > MANIFEST.MF
echo "Main-Class: com.gfish.Main" >> MANIFEST.MF
echo "Class-Path: ." >> MANIFEST.MF

# 打包成JAR文件
echo "打包JAR文件..."
jar -cvfm output/ZCShop.jar MANIFEST.MF -C output .

if [ $? -ne 0 ]; then
    echo "打包失败！"
    exit 1
fi

# 清理临时文件
rm MANIFEST.MF

echo "========================================="
echo "编译成功！插件已生成到 output/ZCShop.jar"
echo "========================================="
echo "使用说明："
echo "1. 将ZCShop.jar放入服务器的plugins文件夹"
echo "2. 确保服务器已安装Vault插件以支持经济功能"
echo "3. 启动服务器，插件会自动生成配置文件"
echo "4. 编辑plugins/ZCShop/config.yml添加商品"
echo "5. 玩家使用/zcshop <商品名称>命令购买"
echo "========================================="