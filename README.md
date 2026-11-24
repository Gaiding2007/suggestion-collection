# 建议收集 / suggestion-collection

一个自己用的安卓 App：把想法和建议收集起来，由管理员逐条接纳、驳回、标记解决、跟着版本推送。
一台手机做管理端，其他人的手机做提交端，数据放在自己的阿里云服务器上共享。

当前版本：**v0.0.9**（可以发给同学装了，下载页 http://39.96.113.178:8080/）

---

## 它做什么

**普通用户（提交端）**

1. 用邮箱 + 密码注册 / 登录
2. 首页只看到管理员给自己开放的项目
3. 进项目 → 看这个项目里所有人提过的建议，按「新反馈 / 待办 / 已解决」三段分组，每段都是最新的在上面
4. 点「提建议」写下标题（必填，50 字内）和内容（选填，1000 字内），还可以加图片（选填，最多 3 张，直接点进去从相册选）
5. 还没被处理的建议，自己可以点「修改」改标题和内容，也可以「删除」
6. 自己提的建议会多一个 **我的反馈** 小标签（紧跟在状态后面），一眼就能在列表里认出来
7. 有人传了图，卡片里会显示缩略图，点一下就能全屏看大图
8. 自己提的建议被驳回时能看到驳回理由，被推送时能看到版本号
9. 标题、内容、驳回理由都能长按选中复制

**管理员（你的手机）**

1. 登录后没有"选项目"这一步，直接进管理台
2. 四个页签：
   - **新反馈**：还没表态的建议，按提出时间从早到晚排
   - **待办**：已经接纳、还没推送完的（含已标记解决）
   - **已解决**：推送完的 + 驳回掉的，最新的在上面
   - **管理**：建项目、分配权限、删项目
3. 每条建议可以：
   - **接纳** → 进「待办」
   - **驳回** → 必须填理由（400 字内），提交人能看到，直接进「已解决」
   - 接纳后 **标记解决** → 状态变「已解决」（还在待办里）
   - **解决后 推送** → 填推送版本号（和可选说明），这条算完成
4. 每条建议都带提交人（「来自 xxx」）和图片缩略图，点开可以看大图
5. 已经表态的还能改：`已驳回` / `已推送` 都能「退回待办」（回到已接纳，推送记录清空），`已接纳 / 已解决` 也可以「改为驳回」

**规则细节**

- 一个人只能看到、提交被授权的项目；没授权就是看不到
- 一个项目的成员能看到这个项目里所有人的建议（互相看得见，但不显示是谁提的；只有管理员看得到提交人）
- 状态流转：`待处理 → 已接纳 → 已解决 → 已推送`，`待处理 → 已驳回`，以及管理员在「已接纳 ⇄ 已驳回」之间互相纠正
- 「已推送」= 完成；「已解决」页 = 推送完的 + 被驳回的
- 提交人只在「待处理」阶段能改或删自己的建议，进了流转就锁住
- 图片：每条建议最多 3 张，只收 JPG / PNG，单张 5 MB 以内（App 会先压到长边 1200 px 再传，实际几十到几百 KB）；文件存在服务器 `/var/www/suggestion/uploads/`，数据库只存相对路径

---

## 技术栈

| 部分 | 选型 |
| --- | --- |
| 客户端 | Kotlin + Jetpack Compose（Material 3），单 Activity |
| 最低系统 | Android 8.0（API 26） |
| 服务器 | 阿里云 ECS 2 核 2 G（Ubuntu 26.04），nginx 1.28 监听 8080 |
| 后端 | PostgreSQL 18 + 自己写的 Node 22 接口（`deploy/server/`，只依赖 `pg`） |
| 网络 | 只用系统自带 `HttpURLConnection` + `org.json`，无第三方网络库；走明文 http（服务器没备案、没证书） |
| 构建 | Gradle 8.14 + AGP 8.13.1 + Kotlin 2.3.0，compileSdk 36 |

不引入 Retrofit / Ktor / 任何官方 SDK，APK 小、离线构建也稳。

---

## 目录结构

```
app/src/main/java/com/gaiding/suggestion/
├── MainActivity.kt          入口
├── AppRoot.kt               登录态路由：启动 → 登录页 / 管理台 / 用户首页
├── SuggestionApp.kt         Application，初始化本地会话
├── data/
│   ├── Supa.kt              极简 HTTP 客户端（BASE = 服务器 /api，含错误中文化）
│   ├── Auth.kt              注册、登录、token 自动续期、退出
│   ├── SessionStore.kt      SharedPreferences 多账号 token
│   ├── Models.kt            Profile / Project / Suggestion / Status
│   └── Repo.kt              所有数据读写（项目、授权、建议、状态流转）
└── ui/
    ├── Common.kt            时间格式、状态胶囊、按钮、空状态等公共组件
    ├── LoginScreen.kt       登录 / 注册（含「选择账号进入」）
    ├── UserScreens.kt       普通用户：项目列表 → 项目详情 → 提建议
    ├── AdminScreens.kt      管理员：新反馈 / 待办 / 已解决 / 管理（项目 + 权限）
    ├── AccountSwitch.kt     顶栏「切换」账号弹窗
    ├── ImageBits.kt         图片：压缩上传、缩略图、点开看大图
    └── theme/Theme.kt       配色和字体
deploy/
├── server/                  自己的后端：schema.sql、server.js、import.js、部署脚本
├── site/                    下载页 + versions.json
├── nginx-suggestion.conf    nginx 配置（8080 静态 + /api/ 反代）
├── upload.ps1               一条命令发新版（传 APK + 更新清单）
└── migrate-dump.sh          从 Supabase 搬数据用（一次性）
supabase/schema.sql          0.0.7 及以前的 Supabase 方案（留作归档，新后端不用它）
log.md                       项目记忆 + 每个版本的改动日志
```

---

## 服务器（已经跑起来了，平时只需要三件事）

后端在自己的阿里云 ECS 上（`39.96.113.178`，Ubuntu 26.04，nginx 监听 **8080**）：

| 东西 | 位置 |
| --- | --- |
| 下载页 + APK | `/var/www/suggestion/`（`http://39.96.113.178:8080/`） |
| 建议图片 | `/var/www/suggestion/uploads/`（`http://39.96.113.178:8080/uploads/<名字>.jpg`） |
| 接口代码 | `/opt/suggestion-api/`（systemd 服务 `suggestion-api`） |
| 数据库 | PostgreSQL 18，库/用户都是 `suggestion`，只监听 `127.0.0.1` |
| 配置 | `/etc/suggestion-api.env`（连库串 + 签名密钥，600 权限） |
| nginx | `/etc/nginx/sites-available/suggestion`（8080 静态 + `/api/` 反代 3000） |

日常操作就三条：

1. **发新版本**：`dist/` 里放好 APK、`deploy/site/versions.json` 加一行 → `powershell -File deploy\upload.ps1 -OnlyVersion 0.0.x`
2. **看服务状态**：`systemctl status suggestion-api`、`journalctl -u suggestion-api -n 50`
3. **改表结构**：改 `deploy/server/schema.sql` 后在服务器上 `psql -f schema.sql`（脚本可重复执行）

> 详细步骤见 [`deploy/README.md`](deploy/README.md)。0.0.7 及以前用的是 Supabase 免费云，
> 0.0.8 起数据和账号都在自己服务器上，Supabase 那边可以删掉了。

### 老账号要认领一次

0.0.8 之前注册过的账号（包括管理员自己）密码存在 Supabase 里，搬不过来。
第一次用新版本时：登录页点 **注册** → 填 **原来的邮箱** + 新密码 → 历史上的建议还挂在原来那个账号下。
认领之后用「登录」就行。

---

## 自己构建

需要 JDK 17+ 和 Android SDK（compileSdk 36）。

```bash
./gradlew assembleDebug
./gradlew assembleRelease     # 需要仓库根的 keystore.properties，否则出的是未签名包
```

产物：

- 调试包：`app/build/outputs/apk/debug/app-debug.apk`
- 正式包：`app/build/outputs/apk/release/app-release.apk`（可以发给别人装）

`local.properties` 里写你的 SDK 路径（这个文件不进仓库）：

```properties
sdk.dir=D:\\applications\\SDK
```

签名信息放在仓库根的 `keystore.properties`（也不进仓库）：

```properties
storeFile=C:/Users/你的用户名/.android-keystores/suggestion-release.jks
storePassword=你的密码
keyAlias=suggestion
keyPassword=你的密码
```

> **keystore 一定要备份**。以后要发升级包，必须用同一个 jks 签名；换了签名的包装到旧版本上会提示「应用未安装」，只能卸载重装（数据在云端，不会丢）。

---

## 发给别人装（不用连电脑）

1. 把 `app-release.apk`（或 `dist/` 里的包）通过微信 / QQ / 网盘发给对方
2. 对方点开安装，系统可能提示「未知来源应用」→ 允许这一个来源安装即可
3. 对方在 App 里注册自己的账号，然后你在「管理 → 成员权限」里给他勾上能看到/能提建议的项目
4. 多人同时连同一个服务器，看到的是同一份数据

---

## 数据安全说明

- 接口在服务端做权限判断：普通用户只能拿到自己有权进入的项目和建议，改状态、删项目、开权限这些只有管理员能调
- 登录用 scrypt 加盐哈希存密码，会话是 HMAC-SHA256 自签的 token（12 小时），刷新 token 60 天
- 提建议时 `author_id` 由服务端填，客户端传不进来
- 上传图片走 `/api/upload`，必须登录才能传；只认 JPG / PNG 文件头，单张 5 MB 上限，超过 3 张直接拒；删建议或删项目时会把对应图片文件一起删掉
- PostgreSQL 只监听 `127.0.0.1`，安全组只开 8080、22，数据库端口不对公网开放
- 明文 http 的代价：同一 wifi 下懂行的人能抓包看到 token，自己小范围用够了；以后有域名 + 证书再换 https

---

## 版本

| 版本 | 内容 |
| --- | --- |
| 0.0.9 | 自己提的建议标「我的反馈」；提建议可上传图片（最多 3 张，JPG/PNG），列表看缩略图、点开看大图，管理员也能看到图 |
| 0.0.8 | 后端从 Supabase 搬到自己的服务器（历史 3 账号 / 4 项目 / 31 条建议全部迁好）；老账号点「注册」用原邮箱设新密码即可认领 |
| 0.0.7 | 管理台四个页签改成等宽分段控件，带数字的标签不会再被截成「…」 |
| 0.0.6 | 提交人可在被处理前删除自己的建议（确认一次）、普通用户列表改成最新的在上面 |
| 0.0.5 | 「已解决」可退回待办（已推送/已驳回都能退）、管理动作改成「改到 0 行就算失败」、接口报错全部翻成中文提示 |
| 0.0.4 | 页签拆成「新反馈 / 待办 / 已解决 / 管理」、驳回的进已解决、提交人在被处理前能改建议、管理员可在接纳↔驳回间改回、文字长按可复制 |
| 0.0.3 | 修复「删除项目显示成功但没删」（现在删不掉会明确报错）、退出登录不再忘记账号、顶栏新增「切换」一键换账号 |
| 0.0.2 | 登录页记住多个账号一键切换、管理员彻底删除项目、正式签名 release 包 |
| 0.0.1 | 账号系统、项目与权限、提建议、接纳/驳回/解决/推送、已解决页、管理台 |

详细改动见 [log.md](log.md)。
