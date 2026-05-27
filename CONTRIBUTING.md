# Contributing

感谢你愿意改进 Brightness Curve Controller。这个项目关注的是一个很具体的问题：让 Android 屏幕亮度更符合个人舒适感。

## 可以贡献什么

- 设备兼容性反馈，尤其是不同 Android 厂商和系统版本。
- 亮度曲线、响应速度、校准方式的体验建议。
- UI/文案改进。
- Bug 修复和测试补充。
- 文档、截图、安装说明。

## 开发环境

- Android Studio
- JDK 17 或更高版本
- Android SDK
- 一台可 USB 调试的 Android 设备

构建和测试：

```bash
./gradlew testDebugUnitTest assembleDebug
```

Windows 也可以使用：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

## 提交 Pull Request 前

请确认：

- 代码可以构建。
- 单元测试通过。
- 没有提交 `local.properties`、APK、IDE 缓存或个人路径。
- 如果修改亮度控制逻辑，请说明真机测试设备、系统版本和观察结果。
- 如果修改核心亮度链路，请按 `CORE_FUNCTION_VERIFICATION.md` 记录 lux -> 写入 -> 回读 -> UI 状态，并尽量附上 `scripts/collect-core-verification.ps1 -Interactive` 输出摘要。
- 如果修改 UI，请附上截图或说明主要变化。

## 亮度控制相关注意事项

- 不要在 UI 层直接调用 `Settings.System` 或 `SensorManager`。
- 系统亮度写入应保持节流，避免频繁写入。
- 环境光读取可以更频繁，但亮度变化需要平滑。
- 任何会明显影响用户屏幕亮度的改动，都应该在 PR 中写明测试方式。

## Issue 建议格式

Bug 反馈请尽量包含：

- 设备型号
- Android/ColorOS/MIUI/One UI 等系统版本
- 是否已授予“修改系统设置”
- 操作步骤
- 期望结果
- 实际结果
- 日志或截图
