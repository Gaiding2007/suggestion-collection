# 自己的服务器（阿里云 ECS）怎么用

## 这台机器的现状

| 项目 | 值 |
| --- | --- |
| 实例 | `iZ2zeg4t2pzrtumjxq41i5Z`（华北2 · 北京） |
| 系统 | Ubuntu 26.04 64 位 |
| 配置 | 2 核 2 GB，系统盘 40 GB ESSD，带宽峰值 1 Mbps（按流量计费） |
| 公网 IP | `39.96.113.178` |
| 到期 | 2027-11-23 |

## 用它干什么（分两步走）

### A. 放安装包下载页（先做）

同学不用加微信、不用收文件，手机浏览器打开 `http://39.96.113.178:8080/` 就能下 APK。

- 服务器上的目录：

```
/var/www/suggestion/
├── index.html                  下载页（来自 deploy/site/index.html）
├── versions.json               版本清单（下载页读这个）
└── apk/
    ├── suggestion-collection-0.0.6.apk
    └── suggestion-collection-0.0.5.apk …
```

- nginx 配置：`/etc/nginx/sites-available/suggestion`（软链到 `sites-enabled/`），内容就是仓库里的 `deploy/nginx-suggestion.conf`
- 监听端口：**8080**（不用 80 端口的原因：国内服务器没备案时 80/443 容易被拦，8080 这种高位端口不涉及备案）
- 发布新版：在电脑上跑 `deploy\upload.ps1`，它会把 APK + 下载页一起推上去，然后 curl 自检

### B. 把后端从 Supabase 搬到这台机器（后做）

目标：账号、项目、建议全部存在自己的 PostgreSQL 里，走 nginx 的 `/api/` 反代，App 换成指向自己的服务器。

- 组件：PostgreSQL（14+，只监听 127.0.0.1）+ 一个自写的 HTTP 接口服务（Node.js，只依赖 `pg` 一个包）
- 表结构沿用现在的四张表（profiles / projects / project_access / suggestions），权限从「数据库 RLS」改成「接口层校验」
- 端口：API 只听 127.0.0.1:3000，由 nginx 把 `http://39.96.113.178:8080/api/*` 转过去；数据库端口**不**对外开放
- App 侧要改：`Supa.BASE` 换成自己的域名/IP、`Auth` 换成自己的登录接口、AndroidManifest 允许 http 明文（`usesCleartextTraffic`）

## 登录服务器

Windows 电脑上有 OpenSSH，直接用：

```powershell
ssh -i "$env:USERPROFILE\.ssh\suggestion_ecs" root@39.96.113.178
```

这把私钥只在这台电脑上（`C:\Users\14127\.ssh\suggestion_ecs`）。公钥已经写进服务器的
`~/.ssh/authorized_keys`。想收回权限，把服务器上那一行删掉即可。

没有密码也能进；如果哪天浏览器 Workbench 进不去，就用这条命令。

## 安全组（阿里云控制台的防火墙）

默认只开 22（SSH）。需要手动加：

| 端口 | 协议 | 授权对象 | 干什么 |
| --- | --- | --- | --- |
| 8080 | TCP | 0.0.0.0/0 | 下载页 + 以后的后端接口 |

添加路径：实例 → 「安全组」→ 安全组 ID → 「入方向」→ 「手动添加」→ 端口 8080/8080 → 授权对象 0.0.0.0/0 → 保存。

**不要**对公网开放 5432（PostgreSQL）、3000（后端内部端口）。

## 日常维护

```bash
# 看 nginx 状态
systemctl status nginx

# 看下载页有没有人访问
tail -n 50 /var/log/nginx/suggestion.access.log

# 服务器更新（一个月一次就够）
apt update && apt upgrade -y
```
