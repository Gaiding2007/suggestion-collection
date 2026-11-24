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

### A. 放安装包下载页（✅ 2026-11-23 已上线）

手机浏览器打开 **http://39.96.113.178:8080/** 就能选版本下载，不用加微信、不用收文件。

服务器上已经装好的东西：

| 项 | 值 |
| --- | --- |
| nginx | 1.28.3（Ubuntu 包），`systemctl status nginx` 查看 |
| 站点配置 | `/etc/nginx/sites-available/suggestion` → 软链到 `sites-enabled/` |
| 站点根目录 | `/var/www/suggestion` |
| 访问日志 | `/var/log/nginx/suggestion.access.log` |
| 监听端口 | 8080（安全组已放行，22 是 ssh） |
| 已上传的包 | 0.0.9、0.0.8、0.0.7、0.0.6、0.0.5、0.0.4 |

服务器上的目录：

```
/var/www/suggestion/
├── index.html                  下载页（来自 deploy/site/index.html）
├── versions.json               版本清单（下载页读这个）
├── uploads/                    建议配图（0.0.9 起，App 上传后存这里）
└── apk/
    ├── suggestion-collection-0.0.9.apk
    └── suggestion-collection-0.0.8.apk …
```

- 发布新版：在电脑上跑 `deploy\upload.ps1`，它会把 APK + 下载页一起推上去，然后 curl 自检
- 改了 `deploy/site/versions.json` 之后不用动 nginx，页面会立刻看到新版本
- 排错：`curl -I http://39.96.113.178:8080/apk/<文件名>.apk` 看是不是 200；再看访问日志有没有请求进来

### B. 把后端从 Supabase 搬到这台机器（✅ 2026-11-23 已上线，App 0.0.8 起用新后端）

账号、项目、授权、建议全部存在自己的 PostgreSQL 里，App 连 `http://39.96.113.178:8080/api`。

| 项 | 值 |
| --- | --- |
| PostgreSQL | 18.6，集群 `18/main`，只监听 `127.0.0.1:5432`；库和用户都叫 `suggestion` |
| 接口服务 | `/opt/suggestion-api/server.js`（Node 22，只依赖 `pg`） |
| 服务管理 | `systemctl status suggestion-api` / `journalctl -u suggestion-api -n 50` |
| 环境变量 | `/etc/suggestion-api.env`（`DATABASE_URL`、`API_SECRET`、`PORT=3000`，600 权限） |
| 连库串 | `/root/.suggestion-db.env`（600 权限，备份用） |
| 对外入口 | nginx `location /api/` → `127.0.0.1:3000` |
| 已迁数据 | 3 个账号（1 个管理员）、4 个项目、6 条授权、31 条建议 |

接口一览（都在 `server.js` 顶部注释里）：`/auth/register|login|refresh`、`/me`、`/home`、
`/projects`（POST/DELETE）、`/projects/:id/access`、`/suggestions`（POST）、
`/suggestions/:id`（PATCH/DELETE）、`/suggestions/:id/{accept|reject|resolve|push|reopen}`、
`/upload`（POST base64 图片，0.0.9 起）。

> 图片文件的对外地址是 `http://39.96.113.178:8080/uploads/<24位hex>.jpg|png`（nginx 静态目录，
> 对应服务器上的 `/var/www/suggestion/uploads/`）。`schema.sql` 里的 `suggestions.images` 只存这个相对路径。

数据搬迁是一次性的，用的 `deploy/migrate-dump.sh`（从 Supabase 拉 JSON，secret key 走环境变量，不写进仓库）
+ `deploy/server/import.js`（灌进本地库）。搬完确认无误后 Supabase 项目可以整个删掉。

> 新增表结构：改 `deploy/server/schema.sql`，`scp` 上去后 `psql -f` 执行（脚本可重复执行）。

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
