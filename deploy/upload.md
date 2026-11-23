# 上传安装包到阿里云服务器（在 Windows 电脑上跑）

把 `dist/` 里最新的 APK、下载页的 `index.html`、`versions.json` 一起推到服务器，
然后让 nginx 立刻认到（不需要重启）。

## 用法

```powershell
cd C:\Users\14127\Desktop\suggestion-collection
powershell -ExecutionPolicy Bypass -File deploy\upload.ps1
```

只传某一个版本：

```powershell
powershell -ExecutionPolicy Bypass -File deploy\upload.ps1 -OnlyVersion 0.0.6
```

参数：`-ServerHost`（默认 39.96.113.178）、`-User`（默认 root）、`-Key`（默认
`%USERPROFILE%\.ssh\suggestion_ecs`）、`-Port`（默认 22）、`-RemoteRoot`（默认 `/var/www/suggestion`）。

## 依赖

- Windows 自带的 `ssh.exe` / `scp.exe`（Win10 1809 以后都有）
- 服务器上已经装好公钥（`~/.ssh/authorized_keys`）

## 说明

- 服务器上的目录结构见 `deploy/README.md`
- 只动 `/var/www/suggestion`，不碰 nginx 配置，所以不需要 reload
- 传完脚本会打印 `curl` 自检结果（HTTP 状态码 + 文件大小）
