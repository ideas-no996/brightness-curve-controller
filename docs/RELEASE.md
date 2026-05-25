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
```

## 5. 如果发布失败

先检查：

- 4 个 Secrets 是否分别添加。
- Secret 名称是否完全一致。
- `ANDROID_KEYSTORE_BASE64` 是否来自 keystore 文件本身，不是文件路径。
- tag 是否匹配 `v*.*.*`，例如 `v1.0.0`。
