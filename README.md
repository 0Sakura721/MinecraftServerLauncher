# Minecraft Server Launcher (MCServer Launcher)

在 Android 设备上运行 Minecraft Java 版服务器的原生启动器。

## 功能特性

-   **JAR 文件运行** — 选择并启动 Minecraft 服务器 JAR（Paper、Spigot、Vanilla 等）
-   **Java 运行时管理** — 自动检测架构并下载适配的 JRE（ARM64 v7a / v8a）
-   **实时控制台** — 终端风格控制台，支持命令输入
-   **三种主题** — 明亮（白）、暗色（黑）、AMOLED 纯黑（省电）
-   **前台服务** — 服务器后台运行，通知栏状态显示
-   **服务器配置** — 内存分配、JVM 参数、端口等全面可配

## 兼容性

| 项目 | 支持 |
|------|------|
| 最低 Android | 8.0 (API 26) |
| 目标 Android | 16 (API 36) |
| CPU 架构 | `arm64-v8a`、`armeabi-v7a` |
| Java 版本 | JRE 21 (Adoptium) |

## 构建

```bash
./gradlew assembleDebug
```

或在 Android Studio 中打开项目直接构建。

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **架构**: ViewModel + StateFlow + DataStore
- **JRE**: Eclipse Temurin (Adoptium) ARM 构建

## 使用方式

1. 安装应用后，先在「设置」页下载安装 JRE
2. 在「配置」页选择 Minecraft 服务器 JAR 文件
3. 调整内存和其他参数
4. 回到首页点击「启动服务器」
5. 在「控制台」查看日志并输入命令

## License

MIT
