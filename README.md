# Brightness Curve Controller

一个非 root Android 亮度曲线控制器。它读取环境光传感器的 lux，根据个人化亮度曲线写入系统屏幕亮度，让亮度调节更接近“我现在看着舒服”，而不是只依赖系统默认自动亮度。

> 当前项目是实验性工具。它已经可以在一台 OPPO/ColorOS 平板上读取环境光并写入系统亮度，但还没有完成多设备验证。Android 8.0+ 是技术安装目标，不是广泛兼容承诺。

## 功能特性

- 非 root 控制系统亮度，使用 Android `WRITE_SETTINGS` 能力。
- 前台服务持续读取 `TYPE_LIGHT` 环境光传感器。
- 使用 log lux 曲线映射环境光和亮度百分比。
- 支持预设曲线、自定义曲线、快速校准和历史版本回滚。
- 首页以日常使用为主：环境判断、当前感觉、太暗/刚好/太亮快速反馈。
- 调试信息默认折叠，保留 raw lux、平滑 lux、目标亮度、写入亮度。
- 响应策略包含 EMA 平滑、写入节流、死区和亮度渐变，避免频繁跳变。
- 包含 adaptive launcher icon。
- 支持在 App 设置页手动检查 GitHub Release 更新、下载 APK，并调起系统安装确认页。更新功能是可选的，亮度控制本身不需要联网。

## 适用设备

本项目的 `minSdk` 是 Android 8.0，但真实可用性取决于设备光线传感器、系统亮度实现、前台服务策略和厂商电池限制。请把它视为“已在少量设备验证的实验性工具”，不要把 Android 8.0+ 理解为完整支持范围。

已验证环境：

- OPPO/ColorOS 平板
- Android 设备已开启 USB 调试
- 用户已手动授予“修改系统设置”权限

更多设备记录和手动测试项见 [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)。

支持状态按证据分级：

- `Installs`：可以安装和启动，但没有验证核心亮度闭环。
- `Sensor Verified`：可以注册 `TYPE_LIGHT` 并收到 lux。
- `Brightness Write Verified`：手动调节能写入并回读系统亮度。
- `Core Loop Verified`：自动控制完成 lux -> 曲线 -> 写入 -> 回读 -> UI 状态。
- `Stable Candidate`：核心闭环经过重启、息屏/亮屏和日常使用验证。

## 权限说明

本项目不需要 root，但需要用户手动授予：

- `WRITE_SETTINGS`：写入 `Settings.System.SCREEN_BRIGHTNESS`
- `POST_NOTIFICATIONS`：Android 13+ 显示前台服务通知
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE`：后台持续亮度控制
- `RECEIVE_BOOT_COMPLETED`：可选，开机后恢复控制
- `INTERNET`：检查 GitHub Release 是否有新版本，下载 APK
- `REQUEST_INSTALL_PACKAGES`：用户确认后安装 App 下载的更新 APK

启用控制器后，App 会把系统亮度模式切换到手动模式，并由前台服务持续调整亮度。停止服务时会尝试恢复启用前的亮度模式和亮度值。

`INTERNET` 和 `REQUEST_INSTALL_PACKAGES` 只用于可选的应用内更新。你可以完全不使用更新功能，亮度控制仍然在本地工作。应用内更新不会静默安装 APK；Android 会要求用户允许“安装未知应用”，并在系统安装器里确认安装。

隐私说明见 [PRIVACY.md](PRIVACY.md)。

## 核心功能验证

本项目最重要的验收标准不是 UI 是否好看，而是这条链路是否成立：

```text
环境光 lux -> 曲线计算 -> 目标亮度 -> 写入系统亮度 -> 回读确认 -> UI 状态
```

修复或报告亮度控制问题时，请优先检查：

- `WRITE_SETTINGS` 是否真的授权。
- 当前系统亮度模式是手动还是自动。
- 当前系统亮度值是否是 0-255。
- 是否检测到 `TYPE_LIGHT` 光线传感器。
- 最近一次 lux、目标亮度、写入目标值和回读值。
- 如果写入失败，UI 是否显示具体原因。

完整验收清单见 [CORE_FUNCTION_VERIFICATION.md](CORE_FUNCTION_VERIFICATION.md)。严格审计和后续修复计划见 [STRICT_REVIEW.md](STRICT_REVIEW.md) 与 [NEXT_10_FIXES.md](NEXT_10_FIXES.md)。

## 快速开始

### 1. Clone

```bash
git clone https://github.com/ideas-no996/brightness-curve-controller.git
cd brightness-curve-controller
```

### 2. 用 Android Studio 打开

用 Android Studio 打开项目根目录，等待 Gradle Sync 完成，然后连接 Android 设备运行 `app`。

首次启动后：

1. 授予“修改系统设置”权限。
2. Android 13+ 允许前台服务通知。
3. 回到 App，打开首页开关。

### 3. 命令行构建

Windows PowerShell：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

macOS / Linux：

```bash
./gradlew testDebugUnitTest assembleDebug
```

生成的 debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## USB 安装调试

Windows PowerShell：

```powershell
$env:ANDROID_HOME = "C:\DevTools\Android\sdk"
$env:PATH = "$env:ANDROID_HOME\platform-tools;$env:PATH"

adb devices -l
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell monkey -p com.evan.brightnesscurve 1
```

确认服务和传感器：

```powershell
adb shell dumpsys sensorservice | Select-String "com.evan.brightnesscurve|Ambient Light|samplingPeriod"
adb shell dumpsys activity services com.evan.brightnesscurve | Select-String "BrightnessControlService|isForeground"
```

查看崩溃日志：

```powershell
adb logcat -d -t 500 | Select-String "AndroidRuntime|FATAL EXCEPTION|com.evan.brightnesscurve"
```

## 发布 Release APK

普通 push 和 pull request 会运行 debug 构建验证。推送符合 `v*.*.*` 格式的 tag 时，GitHub Actions 会构建 release APK，使用仓库 Secrets 中的签名信息签名，并上传到 GitHub Release。

需要先在 GitHub 仓库的 `Settings -> Secrets and variables -> Actions` 中添加：

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

`ANDROID_KEYSTORE_BASE64` 是 release keystore 文件的 Base64 内容。Windows PowerShell 生成方式示例：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\release.keystore")) | Set-Clipboard
```

发布第一个版本示例：

```bash
git tag v1.0.0
git push origin v1.0.0
```

生成的 Release asset 文件名类似：

```text
BrightnessCurveController-1.0.0.apk
```

App 设置页的“软件更新”会读取最新 GitHub Release，并查找这个命名格式的 APK。发布新版本时继续使用 `v*.*.*` tag，例如 `v1.0.1`。

Release 会同时上传 `SHA256SUMS.txt`，用于核对 APK 下载完整性。Release notes 会从 [CHANGELOG.md](CHANGELOG.md) 中对应版本段落生成。

手动下载 APK 后，建议核对 SHA-256。

Windows PowerShell：

```powershell
Get-FileHash .\BrightnessCurveController-<version>.apk -Algorithm SHA256
```

macOS / Linux：

```bash
sha256sum BrightnessCurveController-<version>.apk
```

将输出值与 Release 中的 `SHA256SUMS.txt` 对比后再安装。

不要提交 keystore、密码、alias 或本地 signing 配置文件。

更详细的一步一步说明见 [docs/RELEASE.md](docs/RELEASE.md)。

版本修改记录见 [CHANGELOG.md](CHANGELOG.md)。

常见问题见 [docs/FAQ.md](docs/FAQ.md)。

## 项目结构

```text
app/src/main/java/com/evan/brightnesscurve/
  brightness/   系统亮度读写、曲线映射、渐变策略、权限检查
  sensor/       环境光传感器监听和 lux 平滑
  data/         Room、DataStore、预设、校准数据
  service/      前台服务、开机启动、运行时状态
  update/       GitHub Release 检查、APK 下载、系统安装入口
  ui/           Compose 页面、主题、ViewModel
  domain/       兼容旧测试的领域封装
```

## 开发脚本

本仓库保留了本机辅助脚本：

```powershell
.\scripts\test-and-build.ps1
.\scripts\install-debug.ps1
.\scripts\collect-core-verification.ps1 -Interactive
```

这些脚本仅作为 Windows 本机开发便利入口。`collect-core-verification.ps1` 会把 adb 采集结果写入本地 `diagnostics/` 目录，该目录不会提交到 git。通用构建请优先使用 Gradle Wrapper：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

## 成熟度和路线图

当前 `v1.0.x` 是已经发布的历史版本号，但项目成熟度仍按实验性工具处理。后续版本是否继续使用 `v1.0.x`，还是转为 `v0.2.x` 直到多设备验证完成，会在后续 release policy 中明确。

优先路线：

- 增加核心闭环验收脚本和真机测试记录。
- 增加一键复制/导出诊断报告。
- 收缩并明确设备兼容性声明。
- 加固 Release APK 校验、签名和安装风险说明。
- 根据真实设备反馈优化亮度响应策略。
- 增加导入/导出亮度曲线。
- 增加可视化曲线编辑体验。

## 贡献

欢迎提交 issue、讨论设备兼容性、亮度曲线体验和 UI 改进。提交代码前请先运行：

```bash
./gradlew testDebugUnitTest assembleDebug
```

详细说明见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 许可证

本项目使用 MIT License，见 [LICENSE](LICENSE)。
