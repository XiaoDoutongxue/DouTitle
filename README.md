# DouTitle - Minecraft 称号插件

一个 Minecraft Bukkit 1.12 称号插件，支持称号商店、称号仓库、条件获取等功能。

## 前置插件
- PlaceholderAPI
- Vault
- PlayerPoints

## 命令
- `/doutitle open` - 打开称号仓库
- `/doutitle shop` - 打开称号商店
- `/doutitle create <id> <名称> <时间>` - 创建称号
- `/doutitle give <玩家> <id> <时间>` - 给予称号
- `/doutitle delete <id>` - 删除称号
- `/doutitle reload` - 重载插件

## API 使用

### Maven 依赖
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.XiaoDoutongxue</groupId>
    <artifactId>DouTitle</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
