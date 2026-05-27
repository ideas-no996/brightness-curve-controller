# Release APK 发布说明

GitHub Actions 会在推送 `v*.*.*` 格式的 tag 时自动构建 signed release APK，并上传到 GitHub Release。

## 1. 准备 release keystore

如果你还没有 release keystore，可以在本机生成一个。不要把 keystore 提交到 git。

Windows PowerShell 示例：

```powershell
keytool -genkeypair `
  -v `
  -keystore "$env:USERPROFILE\brightness-curve-release.keystore" `
  -alias brightness_curve_release `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000
```

请妥善保存：

- keystore 文件
- keystore password
- key alias
- key password

如果这些信息丢失，以后同一个包名的正式版本就无法用同一把 key 更新。

## 2. 生成 Base64

```powershell
[Convert]::ToBase64String(
  [IO.File]::ReadAllBytes("$env:USERPROFILE\brightness-curve-release.keystore")
) | Set-Clipboard
```

这会把 keystore 的 Base64 内容复制到剪贴板。

## 3. 在 GitHub 添加 4 个 Secrets

进入仓库：

```text
Settings -> Secrets and variables -> Actions -> New repository secret
```

需要添加 4 次，每次只添加一个 secret。

### ANDROID_KEYSTORE_BASE64

- `Name`: `ANDROID_KEYSTORE_BASE64`
- `Secret`: 上一步复制出来的 Base64 内容

### ANDROID_KEYSTORE_PASSWORD

- `Name`: `ANDROID_KEYSTORE_PASSWORD`
- `Secret`: keystore password

### ANDROID_KEY_ALIAS

- `Name`: `ANDROID_KEY_ALIAS`
- `Secret`: key alias，例如 `brightness_curve_release`

### ANDROID_KEY_PASSWORD

- `Name`: `ANDROID_KEY_PASSWORD`
- `Secret`: key password

不要创建名为仓库名的 secret，也不要把这四个变量名写进同一个 Secret 内容框。

## 4. 发布版本

确认本地 main 已经同步后：

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions 会创建或更新 `v1.0.0` 对应的 Release，并上传：

```text
BrightnessCurveController-1.0.0.apk
SHA256SUMS.txt
```

Release notes 会从 `CHANGELOG.md` 中匹配 tag 的段落提取。例如 tag `v1.0.0` 会读取 `## v1.0.0` 到下一个 `##` 标题之间的内容。

## 5. 发布前验收

发布前至少完成：

- `./gradlew testDebugUnitTest assembleDebug`
- `git diff --check`
- 更新 `CHANGELOG.md`
- 确认 README 没有扩大设备兼容性承诺
- 如果改动影响亮度控制，按 [../CORE_FUNCTION_VERIFICATION.md](../CORE_FUNCTION_VERIFICATION.md) 做一次真机核心闭环验收

Release notes 应写清楚：

- 修复或新增了什么
- 是否改动亮度控制链路
- 是否做过真机核心闭环验收
- 已验证设备型号和 Android/vendor 版本

如果没有做真机核心闭环验收，Release notes 应明确说明。

## 6. 校验 Release APK

Release 会上传 `SHA256SUMS.txt`。手动安装 APK 前建议核对 SHA-256。

Windows PowerShell：

```powershell
Get-FileHash .\BrightnessCurveController-<version>.apk -Algorithm SHA256
```

macOS / Linux：

```bash
sha256sum BrightnessCurveController-<version>.apk
```

把输出值和 `SHA256SUMS.txt` 中的值对比。值一致只能证明下载完整性，不等于证明设备兼容性。

## 7. 如果发布失败

先检查：

- 4 个 Secrets 是否分别添加。
- Secret 名称是否完全一致。
- `ANDROID_KEYSTORE_BASE64` 是否来自 keystore 文件本身，不是文件路径。
- tag 是否匹配 `v*.*.*`，例如 `v1.0.0`。
- `CHANGELOG.md` 是否包含对应的 `## vX.Y.Z` 段落；缺失时会使用默认 release note。
