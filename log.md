# 项目记忆 + 改动日志

> 这个文件最前面是「必要记忆」：项目要做什么、定过哪些决定、为什么要这么选、怎么发布。
> 后面按版本追加每次改了什么。**每次改动都要在这里记一条**，不要只改代码不写日志。

---

# 一、项目记忆

## 1. 项目是什么

安卓 App，中文名 **建议收集**，英文名 / 仓库名 **suggestion-collection**。
一个人当管理员（提需求的人），别人在自己手机上提建议，管理员统一处理，数据云端共享。

## 2. 原始需求（用户原话，勿改）

> 1.账号系统，然后我有管理员账号，可以控制权限
> 2.按照权限开放某个项目可选
> 3.选择进入，看到已提出（包含是否接纳（不接纳有驳回理由，是否解决、是否推送（推送版本））
> 4.新增建议
> 5.管理员的账号界面就是不需要选择项目了，把建议都按提问顺序（最早的在最上面，最新的在最底下）排序，然后我可以选择接纳/驳回，选择驳回填写驳回理由，字数400字以内，接纳后可以点解决，推送，已推送就算完成，放入已解决（另一个界面

附加要求：

- 第一版版本号 **0.0.1**
- 建一个 `log.md`，文件最开始记项目需求等必要记忆，以后每次改动都记录
- 每完成一个版本，自动用 git 推送到 https://github.com/Gaiding2007/suggestion-collection
- 需求有必须先确认的内容时，一次只问一个问题；否则先用普通人语言复述需求并给出第一版「做 / 不做」清单等确认

## 3. 已确认的关键决定

| 问题 | 结论 | 理由 |
| --- | --- | --- |
| 只有管理员一个账号吗 | 不是，别人也要在自己的设备上提建议 | 所以必须共享数据，不能只存本地 |
| 数据放哪 | Supabase 免费云（Postgres + Auth） | 免费、自带账号系统和行级安全，不用自己写服务端 |
| 管理员首页要不要选项目 | 不要，直接汇总全部项目 | 原话第 5 条 |
| 管理员列表顺序 | 最早的在最上面，最新的在最下面（正序） | 原话第 5 条 |
| 驳回理由 | 必填，400 字以内 | 原话第 5 条 |
| 推送 | 填推送版本号（+ 可选说明），这条算完成，进「已解决」页 | 原话第 5 条 |

## 4. 我定的、用户没反对的规则（改动前先问）

- 新注册的人默认一个项目权限都没有，由管理员在「管理 → 成员权限」里勾选开通
- 一个权限开关同时管「能看这个项目」和「能给这个项目提建议」，不拆成两个
- 普通用户能看到同一项目里所有人提的建议，但**看不到提交人**；提交人只有管理员看得到
- 建议表单：标题必填 ≤ 50 字，内容选填 ≤ 1000 字
- 状态只有五种：待处理 `pending` / 已接纳 `accepted` / 已驳回 `rejected` / 已解决 `resolved` / 已推送 `pushed`
- 状态流转（0.0.4 起）：`pending → accepted → resolved → pushed`；`pending → rejected`；管理员可以把 `rejected` 改回 `accepted`，也可以把 `accepted` / `resolved` 改成 `rejected`——即「接纳」「驳回」之间可以互相纠正，但推送后就是终态，不再改
- 驳回的反馈直接算「结束」，和已推送一起放进「已解决」；提交人在被处理之前（`pending`）可以自己改标题和内容
- 管理台页签（0.0.4 起）：新反馈（`pending`）/ 待办（`accepted` + `resolved`）/ 已解决（`pushed` + `rejected`）/ 管理；普通用户在项目里看到的是同一套三段分组
- 推送不发系统通知，提交人在自己的项目里刷新就能看到版本号
- 界面上的标题、内容、驳回理由、推送版本都能长按选中复制（`SelectionContainer`）
- （0.0.9 起）自己提的建议在状态胶囊后面多一个 **「我的反馈」** 小标签，普通用户和管理员都能看到（管理员本来就知道是谁提的，这个标签主要是给提交人自己认）
- （0.0.9 起）建议可以带图片：最多 3 张，只收 JPG / PNG，单张 5 MB 以内；App 先压到长边 1200 px、JPEG 82 再传，服务器存 `/var/www/suggestion/uploads/<24 位 hex>.jpg|png`，数据库 `suggestions.images` 只存 `uploads/...` 相对路径
- （0.0.9 起）图片跟着建议走：删建议会把图一起删；改建议时把某张删掉，文件也会被清掉

## 5. 第一版做 / 不做（0.0.1 范围，已与用户确认）

**做**：账号注册登录、管理员/普通用户双首页、管理员新建项目、按项目授权、用户端项目列表 → 建议列表 → 提建议、管理员全量建议正序列表、接纳 / 驳回（≤400 字）/ 解决 / 推送（填版本号）、已解决页、README 与 log、debug APK、推 GitHub。

**不做**（留给后续版本）：系统推送通知、图片附件、评论/点赞、搜索筛选分页、找回密码、多管理员、删除或编辑建议、iOS/网页版、数据导出。

## 6. 技术选型与理由

| 项 | 选择 | 为什么 |
| --- | --- | --- |
| 语言 / UI | Kotlin + Jetpack Compose (Material 3) | 现代写法，代码量少，UI 好维护 |
| 网络 | 系统 `HttpURLConnection` + `org.json` | 机器上 Maven Central 不通，只有 Google 镜像和腾讯镜像可用；不引第三方库能少踩依赖坑，APK 也更小 |
| 后端 | Supabase（Postgres + GoTrue Auth + PostgREST） | 免费、不用自己写服务端；权限用数据库 RLS 控制，客户端绕不过去 |
| 构建 | Gradle 8.14 + AGP 8.13.1 + Kotlin 2.3.0，compileSdk 36 | 依赖必须用 2025 年中的版本：最新 compose / core-ktx 要求 AGP 9.1 和 compileSdk 37，本机没有 |
| minSdk | 26（Android 8.0） | 覆盖面够，且能直接用 `java.time` |

## 7. 环境与踩过的坑（换机器时看这里）

- JDK 21（`javac` 可用）；Android SDK 在 `D:\applications\SDK`（platforms 33/34/35/36，build-tools 35.0.0 / 36.1.0）
- 本机没有 `gradle` 命令，用 `C:\Users\14127\.gradle\wrapper\dists\gradle-8.14-all\c2qonpi39x1mddn7hk5gh9iqj\gradle-8.14\bin\gradle.bat`
- **网络**：`repo.maven.apache.org` 在本机 PowerShell/.NET 下不通（返回 000），`dl.google.com`、`mirrors.cloud.tencent.com/nexus/repository/maven-public`、`plugins.gradle.org` 通。所以 `settings.gradle.kts` 里用 google 仓库 + 腾讯 Maven Central 镜像
- 依赖别自动升到最新：compose-bom 用 `2025.06.01`、core-ktx `1.16.0`、activity-compose `1.10.1`、lifecycle `2.9.1`、coroutines `1.10.2`。升上去会要求 AGP 9.1 / compileSdk 37 直接编译失败
- 模拟器 `emulator-5554`（API 36, x86_64），adb 在 `D:\applications\SDK\platform-tools\adb.exe`
- PowerShell 里 `curl` 可用（`curl.exe`），但别用 `function H($u){& curl.exe ...}` 这种嵌套写法，会拿到空值
- **签名**：keystore 在 `C:\Users\14127\.android-keystores\suggestion-release.jks`（alias `suggestion`，密码写在仓库根的 `keystore.properties`，这两个都**不进 git**）。
  - **一定要备份这个 jks**：丢了以后就没法给已装的 App 发升级包（包名相同但签名不同，安装会报「应用未安装」）
  - 换机器时把 jks 拷到同一个路径，或改 `keystore.properties` 里的 `storeFile`
- **打 release**：`gradle assembleRelease`（需要 `keystore.properties` 存在，否则出的是没签名的包）
  - 本机离线打 release 会卡在 `lintVital`（`lint-gradle` 没缓存），已在 `app/build.gradle.kts` 里关掉 `checkReleaseBuilds`
  - `build.gradle.kts` 里取 Properties 要写 `import java.util.Properties` + `Properties()`，直接写 `java.util.Properties()` 会报 `Unresolved reference: util`（`java` 被解析成 Gradle 扩展）
- **模拟器可能被人关掉/抢占**：本机模拟器有时被别的程序占用（截图会变成别的 App），点按自动化不可靠；验证以「装得上、能启动、无崩溃、代码走查」为准
  - 模拟器被杀掉时用后台任务重开：`emulator.exe -avd Medium_Phone -no-window -no-audio -no-boot-anim -no-snapshot-save -gpu swiftshader_indirect`，等 `getprop sys.boot_completed` 变成 1 再装
  - 想在没登录真账号的情况下看「选择账号进入」界面：`adb shell "run-as com.gaiding.suggestion cp /data/local/tmp/accounts.xml shared_prefs/accounts.xml"`（用完记得删掉这份假数据）
- **github.com 时不时连不上（重要）**：本机 DNS 会把 `github.com` 解析到 `20.247.166.64` 这类连不通的地址，直接 `git push` 报 `Failed to connect to github.com port 443` 或 `Recv failure: Connection was reset`。实测可用的 IP：`140.82.113.3`、`140.82.114.3`、`20.205.243.166`。解决办法是给 git 指定解析地址（已写进这个仓库的 `.git/config`）：
  ```
  git config http.curloptResolve "github.com:443:140.82.113.3"
  git push origin main
  ```
  （`http.curloptResolve` 要 curl 的 `host:port:ip` 格式；换机器或 IP 失效时换一个上面的 IP 试试，`curl --resolve github.com:443:<ip> https://github.com` 能先验证这个 IP 通不通）
- **Supabase 域名本机也连不上**：`pnnyujqkkdehfxnmomww.supabase.co` 的 TLS 握手会被重置（`Recv failure: Connection was reset`），而 `supabase.com` 能开。所以「数据库有没有建好、RLS 对不对」只能由用户在自己那边确认，本机只保证编译和本地逻辑
- **连服务器必须带私钥**（0.0.9 踩过）：`ssh root@39.96.113.178` 会一直卡着等密码（本机没配默认私钥）。正确写法是 `ssh -i "$env:USERPROFILE\.ssh\suggestion_ecs" root@39.96.113.178`，`scp` 同理用 `-i`；`deploy\upload.ps1` 里已经封装好了
- **远程跑 SQL / Node 不要拼嵌套引号**：PowerShell → ssh → bash → psql 四层引号一定会被剥掉（报 `syntax error near unexpected token '('` 或 `extra command-line argument ... ignored`）。稳定做法：本地写 `.sh`（`#!/usr/bin/env bash`）→ `scp` 上去 → `ssh 'bash /root/upload/x.sh'`，脚本里再用 `psql "$DATABASE_URL" -c "..."`；`sed -i 's/\r$//'` 去掉 CRLF 和 BOM
- **服务器上的 Node 脚本要带 `NODE_PATH`**：`pg` 装在 `/opt/suggestion-api/node_modules`，把脚本放 `/root/upload/` 跑会 `Cannot find module 'pg'`。用 `cd /opt/suggestion-api && NODE_PATH=/opt/suggestion-api/node_modules node /root/upload/x.js`
- **adb 自动化要点（0.0.9 补）**：一次 `adb shell "..."` 里串起多条命令（`am start; sleep; input tap; ...; screencap`）最可靠，主机端分次调用期间前台很容易被别的 App 抢走；填表单必须「点框 → input text → keyevent 4 收键盘 → 重新截图定位」，键盘弹出会让下半屏整体上移；`adb shell screencap -p /sdcard/x.png` 会被相册索引，选图时记得先 `rm -f /sdcard/*.png`，测试图往 `/sdcard/Pictures/` 放

## 8. 发布流程（每个版本都这么做）

1. 改代码 → `gradle assembleDebug assembleRelease` 都通过
2. 装到模拟器上点一遍关键流程，没有崩溃
3. 更新 README 里的版本号和本文件末尾的版本日志
4. 把要发的 APK 复制到 `dist/`（已被 gitignore，不提交）
5. `git add -A && git commit -m "版本描述" && git push`
6. 给用户的交付说明里写清：装哪个 APK、这版改了什么、数据库要不要补 SQL
7. 想让同学直接下载的话，再跑一次 `powershell -ExecutionPolicy Bypass -File deploy\upload.ps1`（会更新 `deploy/site/versions.json` 后推上去）

## 9. Supabase（0.0.7 及以前用，0.0.8 起已弃用）

- Project URL：`https://pnnyujqkkdehfxnmomww.supabase.co`，publishable key 曾写在 `data/Supa.kt`
- 管理员邮箱：`1412759076@qq.com`
- 表结构归档在 `supabase/schema.sql`（RLS 方案，新后端不用它，只当历史参考）
- **0.0.8 已把数据全部搬到自己服务器**，确认无误后这个 Supabase 项目可以整个删掉（用户已同意「用完直接删」）
- 坑（留档）：默认开着 `Confirm email`，注册后不能直接登录；本机网络连不上 `*.supabase.co`（TLS 被重置），所以当时所有验证都只能编译 + 模拟器启动

## 10. 自己的服务器（阿里云 ECS）

- 实例 `iZ2zeg4t2pzrtumjxq41i5Z`，华北2（北京），Ubuntu 26.04 LTS，2 核 2 GB，40 GB 系统盘，1 Mbps 按流量计费，公网 `39.96.113.178`，到期 2027-11-23
- 登录：`ssh -i C:\Users\14127\.ssh\suggestion_ecs root@39.96.113.178`（私钥只在这台电脑；公钥在服务器 `~/.ssh/authorized_keys`）
- **A 计划（下载页）已上线**：nginx 1.28.3 监听 8080（国内未备案时 80/443 容易被拦），站点根目录 `/var/www/suggestion`，页面 → `http://39.96.113.178:8080/`
  - 服务器上的文件：`index.html`、`versions.json`、`apk/*.apk`（都是 `deploy/` 里那套，靠 `deploy\upload.ps1` 上传）
  - 配置：`/etc/nginx/sites-available/suggestion`（软链到 `sites-enabled/`），日志 `/var/log/nginx/suggestion.access.log`
  - 安全组只放开了 22 与 8080；**不要**对公网开 5432 / 3000
- **B 计划（后端搬到这台机器）已完成（2026-11-23，0.0.8）**：
  - 组件：PostgreSQL 18.6（只听 `127.0.0.1:5432`，库/用户都叫 `suggestion`）+ 自写 Node 22 接口（`/opt/suggestion-api/server.js`，只依赖 `pg`；密码 scrypt 加盐、令牌 HMAC-SHA256 自签，access 12 小时 / refresh 60 天）+ nginx `location /api/` 反代 3000
  - 服务：systemd `suggestion-api`（`Restart=always`），环境变量在 `/etc/suggestion-api.env`（600），连库串另存 `/root/.suggestion-db.env`（600）
  - 表：`users / projects / project_access / suggestions`（无 RLS，权限全在接口层；`users.password_hash`、`users.claimed` 用来标记「从 Supabase 搬来但还没认领」的账号）
  - 接口：`/auth/register|login|refresh`、`/me`、`/home`、`/projects`(POST/DELETE)、`/projects/:id/access`(PUT)、`/suggestions`(POST)、`/suggestions/:id`(PATCH/DELETE)、`/suggestions/:id/{accept|reject|resolve|push|reopen}`
  - 数据搬迁：服务器上 `curl` 得到 Supabase（那边能连通）→ `deploy/migrate-dump.sh` 导出 JSON 到 `/root/migration` → `deploy/server/import.js` 灌库；结果 3 账号（1 管理员）/ 4 项目 / 6 授权 / 31 条建议，`claimed` 全为 false
  - App 侧改动：`Supa.BASE = "http://39.96.113.178:8080/api"`（去掉 apikey）、`Auth` 换 `/auth/*`、`Repo` 换新端点、Manifest 加 `android:usesCleartextTraffic="true"`
- 每次发新版：`deploy\upload.ps1` 会把 `dist` 里的 APK 和下载页一起推上去并 curl 自检
- **传命令的坑**：PowerShell 5.1 会剥掉远端命令里的双引号、把反引号当命令替换、把 `\$VAR` 吃掉。稳的做法是把脚本写成本地文件 → `scp` → `bash /path/x.sh`；一次性命令用 PS 单引号包整体、内部只用单引号给 bash。写 env 文件别用 PS 的 `printf "...\n"`（`\n` 会变成一个字母 `n`，曾把库名写成 `suggestionn`）

## 11. 现在的待办 / 以后可能要做

- [ ] 找回密码（发重置邮件）
- [ ] 推送后给提交人发通知（本地或 FCM）
- [ ] 建议详情页 + 评论讨论
- [ ] 项目归档（表里已有 `is_archived`，界面还没做；现在只有彻底删除）
- [ ] 建议列表按状态筛选、按项目筛选
- [ ] 搜索、分页（建议多了以后必须做）
- [ ] 多管理员 / 给管理员的权限分级
- [x] 管理员删除项目（0.0.2）
- [x] 记住多个登录过的账号、一键切换（0.0.2，0.0.3 修正了「退出就忘记账号」的问题）
- [x] 正式签名的 release APK（0.0.2）
- [x] 顶部栏一键切换账号按钮（0.0.3）
- [x] 管理台四页签（新反馈 / 待办 / 已解决 / 管理）（0.0.4）
- [x] 提交人在被处理前可自己改建议；管理员可在接纳/驳回之间改回（0.0.4）
- [x] 标题/内容/理由长按可复制（0.0.4）
- [ ] 驳回后提交人「申诉」或管理员写补充说明
- [ ] 反馈详情页（现在长内容会截到 6 行，点进去看全文）
- [x] 服务器放安装包下载页（A 计划，2026-11-23：`http://39.96.113.178:8080/`）
- [x] 后端从 Supabase 搬到自己的服务器（B 计划，2026-11-23，0.0.8 起生效）
- [ ] 老账号认领后，如果忘了新密码只能靠管理员在库里改（还没有找回密码）
- [ ] 以后有域名 + 证书就把 8080/http 换成 443/https（现在明文，同 wifi 下有被抓包的风险）
- [ ] 数据库定时备份（`pg_dump` + 传到对象存储，现在还没有）

---

# 二、版本日志

## v0.0.1 — 2026-11-17

第一个能跑通的版本：从空仓库到「登录 → 建项目 → 授权 → 提建议 → 接纳/驳回/解决/推送 → 已解决页」全流程。

### 新增

- **工程骨架**：Gradle 8.14 + AGP 8.13.1 + Kotlin 2.3.0，compileSdk 36 / minSdk 26 / targetSdk 36，versionName `0.0.1`
  - `settings.gradle.kts` 配置 google + 腾讯 Maven Central 镜像（本机访问不到 Maven Central）
  - 应用 ID `com.gaiding.suggestion`，桌面名「建议收集」，自适应图标（蓝底白色对话气泡 + 三点）
- **账号系统**（`data/Auth.kt`、`ui/LoginScreen.kt`）
  - 邮箱 + 密码注册 / 登录，登录页「登录 / 注册」切换，密码可显示
  - access token 过期前自动用 refresh token 续期，续期失败自动回登录页
  - 常见英文报错翻成中文（密码错误、邮箱已注册、邮箱未验证、操作太频繁……）
  - 注册后自动生成 `profiles` 资料（数据库触发器）
- **路由**（`AppRoot.kt`）：启动读本地会话 → 有会话查身份 → 管理员进管理台，普通用户进用户首页；无会话进登录页
- **普通用户端**（`ui/UserScreens.kt`）
  - 首页只列出被授权的项目（卡片：项目名、说明、建议数、待处理数）
  - 项目详情：按时间正序列出这个项目的建议，带状态胶囊、驳回理由、推送版本号
  - 右下角「提建议」→ 标题（必填 ≤50 字）+ 内容（选填 ≤1000 字）
- **管理端**（`ui/AdminScreens.kt`）
  - 顶部三个页签：待办 / 已解决 / 管理
  - 待办：**所有项目**的建议，按提出时间正序（最早的在上），每条显示项目、建议号、时间、标题、内容、提交人
    - 待处理：接纳 / 驳回（弹窗必填理由，400 字内并实时计数）
    - 已接纳：标记为已解决
    - 已解决：推送新版本（必填版本号，可选说明）
    - 已驳回：显示驳回理由
  - 已解决：所有已推送的建议，带版本号和推送时间
  - 管理：新建项目（名称 ≤30 字 + 一句话说明）；成员列表点开勾选可见项目（勾选即写入/删除授权，即时生效）
  - 右上角刷新、退出登录
- **数据库**（`supabase/schema.sql`，可重复执行）
  - 四张表：`profiles`、`projects`、`project_access`、`suggestions`，字段带长度和状态约束
  - 全表开启 RLS：普通用户只能读自己有权限的项目和建议，只有管理员能建项目、改授权、改状态；管理员能看到全部
  - `is_admin()` / `has_project_access()` 用 `security definer` 避免 RLS 递归
  - 提建议只能走 `create_suggestion()` 函数，函数内强制 `author_id = auth.uid()` 并校验权限，客户端无法伪造提交人
  - 附带把管理员设为管理员的 SQL 提示
- **文档**：`README.md`（三步跑起来、目录说明、构建方法、安全说明）、`log.md`（本文件）

### 验证

- `gradle assembleDebug` 通过，产出 `app-debug.apk`（约 10 MB）
- 装到 API 36 模拟器（emulator-5554）能正常启动，登录页 UI 正常，无崩溃
- 数据库还没初始化时不会甩英文报错，而是提示「数据库还没初始化：请到 Supabase 的 SQL Editor 执行 supabase/schema.sql」
- 数据库脚本尚未在真实 Supabase 执行，等用户跑完后联调（下一步）

### 已知限制

- 管理员列表不分页，建议多了会一次性拉全部（当前量级没问题）
- 普通用户在建议卡片上看不到提交人（按设计），管理员能看到
- 建议一旦推送就不能改，也没有「删除建议」
- 没有找回密码，忘了只能去 Supabase 后台重置

## v0.0.2 — 2026-11-17

### 新增 / 改动

- **登录页记住多个账号、一键切换**（`data/SessionStore.kt`、`ui/LoginScreen.kt`、`AppRoot.kt`）
  - 登录过的账号都留在本地，每个账号独立存 access / refresh token；登录页直接列出「选择账号进入」
  - 点账号就切过去，不用重新输密码；token 过期自动续期，续不上才回登录页
  - 每个账号右侧「移除」可单独删；底部「用其他账号登录」回到邮箱密码表单，表单里也有「返回账号列表」
  - 退出登录会把当前账号从记住的列表里一起删掉（安全起见，下次必须重输密码）——**0.0.3 起改了：退出不再删账号，只有点「移除」才删，见下方 v0.0.3**
  - 从 0.0.1 升级上来的旧会话会自动搬进新结构，不用重新登录
- **管理员可以彻底删除项目**（`data/Repo.kt` 的 `deleteProject`、`ui/AdminScreens.kt` 的 `DeleteProjectDialog`）
  - 管理页每个项目卡片右下角「删除项目」→ 弹窗写清「这个项目里有 N 条建议，删除后一起消失，没办法恢复」
  - 必须手打项目名才能按「永久删除」，防手滑
  - 数据库：删项目连带删建议和授权（外键 `on delete cascade`），只有管理员能删（新增 `projects_admin_delete` 策略）
- **正式签名的 release 包**（`app/build.gradle.kts`）
  - keystore 在仓库外 `C:\Users\14127\.android-keystores\suggestion-release.jks`，密码在 `keystore.properties`（已 gitignore）
  - `assembleRelease` 出可分发 APK；debug 包照旧
  - 版本号 `0.0.2`（versionCode 2）；登录页底部版本号改为读 `BuildConfig.VERSION_NAME`
- 数据库脚本 `supabase/schema.sql`：`projects` 增加管理员删除策略（**已经在跑 0.0.1 的库要重新执行一次这个脚本**，脚本可重复执行，不会清数据）

### 验证

- `assembleDebug` + `assembleRelease` 都通过；release 包 `apksigner verify` 通过，`versionName=0.0.2`、`versionCode=2`、`minSdk 26`
- 模拟器在这次验证中途被外部关掉，多账号选择界面没能在机器上点一遍，靠编译 + 代码走查确认；等模拟器恢复后补测

### 已知限制

- 建议仍然不能删除 / 编辑
- 删除项目不可撤销（弹窗里已写明）
- 还没有「管理员给自己开项目权限」以外的批量授权操作，人多时得一个个勾

## v0.0.3 — 2026-11-17

修用户报的两个问题，并加一个入口。

### 1. 「项目删除显示成功但没删」——不是网络问题，是数据库静默拒绝

- 原因：`data/Repo.kt` 的 `deleteProject` 当年用 `Prefer: return=minimal`。PostgREST 在 RLS 拒绝删除时**不报错**，只是删了 0 行、回 `204`，客户端就当成成功了。真正的原因是库里没有 `projects_admin_delete` 策略（0.0.2 才加的，库没重跑脚本），或者当前账号不是管理员。
- 改法（`data/Repo.kt`）：
  - `Prefer: return=representation`，把删掉的行要回来；`rows.length() == 0` 就抛 `Supa.ApiException(403, "项目没被删掉：数据库没有同意删除...")`
  - `ui/AdminScreens.kt` 的 `onDeleteProject` 再补一层端到端确认：删完重新查一遍项目列表，项目还在就抛 409「项目还在：数据库没有真正删除...」，绝不让界面说「已删除」
- **用户侧要做的**：去 Supabase 的 SQL Editor 把 `supabase/schema.sql` 重新执行一次（可重复执行，不会清数据）

### 2. 「选择账号进入没看见」——退出登录把账号忘了

- 原因：`AppSession.signOut()` → `Auth.signOut()` → `SessionStore.clear()`，而 `clear()` 的实现是 `forget(uid)`，等于退出时把账号从记住列表里删掉，列表永远是空的，登录页只能显示表单。
- 改法：
  - `data/SessionStore.kt` 新增 `leaveCurrent()`：只去掉「当前账号」，账号和 token 都留着；`clear()` 保留原语义（彻底忘掉）供 401 用
  - `data/Auth.kt`：`signOut(keepAccounts: Boolean = true)`，true 时只 `leaveCurrent()` 且**不**调服务端 `/auth/v1/logout`（否则 refresh token 被吊销，快速切换就废了）；false 时维持旧行为
  - `AppRoot.kt`：`signOut()` 改成 `keepAccounts = true`；新增 `signOutToForm()`（退出并直接打开邮箱密码表单）；新增 `AppSession.wantForm` 让「用其他账号登录」能一次性跳转
  - 退出弹窗文案改为「退出后会回到登录页，这个账号还留在账号列表里，点一下就能直接回来。不想让别人进去，就在登录页点「移除」」

### 3. 新增「切换」按钮（退出键旁边）

- 新文件 `app/src/main/java/com/gaiding/suggestion/ui/AccountSwitch.kt`：`AccountSwitchDialog(saved, currentUid, onSwitch, onUseOther, onDismiss)`
  - 列出记住的账号，当前账号标「当前」并高亮；点一个直接进去（不用输密码）
  - 底部「用其他账号登录」→ 退出当前账号并打开邮箱密码表单（当前账号会留在列表里）
  - 系统图标集里没有 `Icons.*SwitchAccount`（只在 material-icons-extended 里，没引这个依赖），所以按钮用文字「切换」
- `ui/AdminScreens.kt` / `ui/UserScreens.kt`：顶栏 actions 最前面加「切换」按钮，新增 `savedAccounts` / `onSwitchAccount` / `onUseOtherAccount` 三个参数
- 一个副作用要知道：退出时不再吊销服务端 refresh token，token 会随账号一起留在本机（下次一键登录靠它）。公共设备上请点「移除」把账号彻底抹掉

### 验证

- `assembleDebug` + `assembleRelease` 通过；`aapt2 dump badging` 确认 `versionCode=3`、`versionName=0.0.3`
- release 包 `SHA-256 E961BC9D2650D947233D1FEE3672F16A7381B32AFCC90C8144CC4BCEFA5BA36C`（0.0.2 的是 `FBFCC2A2…`）；解包 `classes2.dex` 里能搜到 `AccountSwitchDialog` / `leaveCurrent` / `wantForm` / `signOutToForm`，确认新代码确实打进包了
- 模拟器上 `adb install -r` + 启动正常，登录页页脚显示 `v0.0.3`，无崩溃
- 本机仍然连不上 `*.supabase.co`（TLS 被重置），真实账号的「删除项目成功/失败提示」「账号切换」没能联网点一遍，靠编译 + 代码走查 + 单元级判断确认

### 已知限制

- 删除项目仍然不可撤销
- 本机无法联调 Supabase，线上行为要等用户装 0.0.3 后实测


## v0.0.4 — 2026-11-23

用户提了 5 条：驳回的要进「已完成」、待办/已解决拆成三个模块、提交人在被处理前能改、管理员能在接纳/驳回之间改回、文字能长按复制。这一版把「建议流转」从单向流水线改成了可纠正的三段式。

### 1. 页签拆成「新反馈 / 待办 / 已解决 / 管理」

- `ui/AdminScreens.kt`：`rows` 拆成三组
  - `fresh` = `pending`
  - `doing` = `accepted` + `resolved`
  - `done` = `pushed` + `rejected`，按 `pushedAt ?: rejectedAt ?: createdAt` 倒序（最新结束的在上面）
- `TabRow` 从三个变四个，标签用新写的 `TabLabel()`（`labelLarge` + `maxLines = 1` + 省略号，防止「已解决 12」把下标挤歪）
- 原来的 `PendingList` 泛化成 `AdminList(rows, numbered, emptyTitle, emptyDesc, emptyIcon, onAccept, onReject, onResolve, onPush, onUnreject)`，三个页签共用；只有「新反馈」显示 `#序号`
- 删掉了不再使用的 `SolvedList`
- 圆点：`Icons.Default.List` 在 core 图标集里其实存在，但需要 `import androidx.compose.material.icons.filled.List`，为了让语义更清楚改用 `Star`（新反馈）/ `Check`（待办）/ `Done`（已解决）

### 2. 驳回的直接进「已解决」

- 这是第 1 条的直接结果：`done` 包含 `rejected`，所以驳回后卡片立刻从「新反馈」消失、出现在「已解决」
- 卡片上仍然显示驳回理由（红框），方便以后回看为什么没做
- 排序用 `rejectedAt`，所以刚驳回的排在最上面

### 3. 管理员可以在「已接纳」和「已驳回」之间改回

- `ui/AdminScreens.kt` 的 `AdminSuggestionCard` 按状态出按钮：
  - `pending`：驳回 / 接纳
  - `accepted`：改为驳回 / 标记为已解决
  - `resolved`：改为驳回 / 推送新版本
  - `rejected`：改为已接纳
  - `pushed`：没有操作（终态）
- `data/Repo.kt` 的 `accept()` 顺手把 `reject_reason`、`rejected_at` 清成 `null`——否则「驳回→接纳」之后卡片上会残留一条驳回理由
- 状态机因此变成：`pending → accepted ⇄ rejected`，`accepted → resolved → pushed`，`resolved → rejected` 也允许（推之前反悔）。推送后的 `pushed` 仍是终态

### 4. 提交人在被处理前可以自己改

- `supabase/schema.sql` 新增 RPC `public.update_suggestion(p_id uuid, p_title text, p_content text default null)`（`security definer`）
  - 只允许 `author_id = auth.uid()` 且 `status = 'pending'`；用 `get diagnostics ... row_count` 判断，改到 0 行就 `raise exception '只能修改自己提的、还没被处理的建议'`
  - 这样既不用给 `suggestions` 开 UPDATE 策略（那是管理员专用），也不怕客户端乱改状态字段
- `data/Repo.kt` 新增 `updateSuggestion(id, title, content)`，调 `/rest/v1/rpc/update_suggestion`
- `ui/UserScreens.kt`
  - `ProjectDetail` 多收一个 `meUid`，把列表分成「新反馈 / 待办 / 已解决」三段（和后台一致），段头显示条数
  - `SuggestionCard(suggestion, canEdit, onEdit)`：`canEdit` 时右上角出现「修改」
  - `NewSuggestionDialog` 改造成 `SuggestionEditorDialog(project, editing, onDismiss, onDone)`——`editing == null` 是新建，否则预填标题/内容、按钮变「保存修改」
- `Suggestion` 模型新增 `rejectedAt`（`rejected_at`），给「已解决」排序用

### 5. 文字长按可复制

- 用 Compose 的 `SelectionContainer` 包住：管理台卡片的标题、内容、推送版本行；用户端卡片的标题、内容、推送版本行；以及两边共用的 `Surface_ReasonBox` 正文（驳回理由）

### 验证

- `assembleDebug` + `assembleRelease` 通过；`aapt2 dump badging` 确认 `versionCode=4`、`versionName=0.0.4`
- 模拟器（emulator-5554）`adb install -r` + 启动正常，登录页页脚显示 `v0.0.4`，无崩溃
- 仍然连不上 `*.supabase.co`（TLS 被重置），没有真实账号可以点一遍四个页签；这一版的界面改动靠编译 + 代码走查确认
- **用户侧要做的**：去 SQL Editor 把 `supabase/schema.sql` 再跑一次（新增了 `update_suggestion` 函数；不跑的话提交人点「修改」会报函数不存在）

### 已知限制

- 只做了长按复制，没有做「一键复制」按钮
- 提交人只能改标题和内容，不能撤销建议、不能改自己选的项目
- 「已解决」里驳回和已推送混在一起，暂时只能靠状态胶囊区分


## v0.0.5 — 2026-11-23

用户反馈两条：「已解决的可以退回到待解决（已接纳）」、「修改有报错」。第二条不是代码写错了，是**数据库没升级**——截图里那句 `Could not find the function public.update_suggestion(...)` 就是 0.0.4 新增的函数还没建。这一版把两件事都处理了：加了退回入口，顺便让这类「数据库没跟上」的报错说人话。

### 1. 「已解决」可以退回「待办」

- `data/Repo.kt` 新增 `reopen(id)`：`status` 改回 `accepted`，同时把 `resolved_at`、`reject_reason`、`rejected_at`、`pushed_version`、`pushed_note`、`pushed_at` 全部清 `null`——退回去就是真的重来一遍，不留「已推送 1.2.0」这种自相矛盾的残迹
- `ui/AdminScreens.kt` 的卡片：
  - `rejected`：「改为已接纳」→ 改名「退回待办」，走同一个 `reopen`
  - `pushed`：新增「退回待办」（0.0.4 里 `pushed` 是终态，现在不是了）
  - 三个页签共用的 `AdminList` 参数 `onUnreject` 一起改名 `onReopen`，提示语「已退回待办」
- 状态机因此变成：`pushed` 也能回到 `accepted`，只有数据库里被删除才算彻底结束

### 2. 「修改有报错」= 数据库缺函数，现在会说人话

- 根因：0.0.4 的修改走 RPC `public.update_suggestion`，那是要用户自己在 SQL Editor 重跑 `supabase/schema.sql` 才有的；没跑就会收到 PostgREST 的 `PGRST202`（函数不在 schema cache 里）
- `data/Supa.kt` 新增 `friendly(e)`：把接口英文报错翻成一句中文+该怎么做，覆盖登录类错误、表不存在、函数不存在、权限不足、CHECK 约束（标题 50 / 内容 1000 / 理由 400）、网络不通
- `AppRoot.kt` 原来的私有 `friendly()` 改成直接调 `Supa.friendly(e)`；`ui/AdminScreens.kt`、`ui/UserScreens.kt` 里所有 `error = e.message ?: "..."`、`snackbar.showSnackbar(e.message ?: "操作失败")` 之类全部换成 `Supa.friendly(e)`——弹窗里写英文报错的地方基本清干净了
- 所以现在没跑 SQL 时点「保存修改」，红框会直接写：「数据库还没升级：请到 Supabase 的 SQL Editor 把 supabase/schema.sql 重新执行一次（新版本加了修改建议用的函数），然后重开这个页面」

### 3. 顺手修掉别的「假成功」

- `data/Repo.kt` 的 `patchSuggestion()` 从 `Prefer: return=minimal` 改成 `return=representation`，改到 0 行就抛 403 并提示「数据库没有同意这次修改」——和 0.0.3 修 `deleteProject` 是同一类坑：**RLS 拒绝时 PostgREST 不报错，只是 0 行**，minimal 就什么都看不出来
- 影响所有管理动作（接纳、驳回、解决、推送、退回）

### 验证

- `assembleDebug`（10,600,183 B）+ `assembleRelease`（7,173,773 B）通过；`aapt2 dump badging` 确认 `versionCode=5`、`versionName=0.0.5`；release 包 SHA-256 `47A18A39DCF912219FA7B30EBDD7E34859C8A4ABC2F7D370737E3292B8A7E4DA`
- `adb install -r` 成功、启动无崩溃；本机依旧连不上 `*.supabase.co`，「退回待办」按钮要有真实被推送/被驳回的数据才能点到，这一版靠编译 + 代码走查确认
- **用户侧要做的**：还是那件事——SQL Editor 里把 `supabase/schema.sql` 完整跑一遍（这一次必须跑，否则「修改」还是用不了）

### 已知限制

- 退回会丢掉推送记录（版本号、说明、时间），log 里不会留痕；要留痕得另建一张状态流水表
- 退回统一落到「已接纳」，不能直接退回「待处理」；想重新排队得驳回后再接纳
- 提交人看不到「这条被退回过」

### 4. 顺手记一条环境坑（github 推送）

- 2026-11-23 这次 git push 又报 Recv failure: Connection was reset。本机 DNS 解析到的 github IP 经常不通，解决办法还是那条：用 `curl --resolve github.com:443:<ip> https://github.com` 逐个试，把返回 200 的写进仓库配置 `git config http.curloptResolve "github.com:443:<ip>"`。当天可用的是 `20.205.243.166`（140.82.113.3 / 114.3 / 112.3 都返回 000）。

## v0.0.6 — 2026-11-23

用户三条：提交人在被处理前能自己删掉建议、普通用户看到列表要「最新的在上面」、以及他在阿里云买了一台服务器（第三条只聊，不进代码）。

### 1. 提交人可以在被处理前删掉自己的建议

- `supabase/schema.sql` 新增 RPC `public.delete_suggestion(p_id uuid)`（`security definer`）：只删 `author_id = auth.uid() and status = 'pending'` 的行，`get diagnostics ... row_count` 为 0 就 `raise exception '只能删除自己提的、还没被处理的建议'`
  - 没走「给 suggestions 开作者 DELETE 策略」的老路：RPC 能把「只有 pending 才能删」写死在数据库里，客户端改不了
- `data/Repo.kt` 新增 `deleteSuggestion(id)`，调 `/rest/v1/rpc/delete_suggestion`
- `ui/UserScreens.kt`
  - `SuggestionCard` 多收一个 `onDelete: (() -> Unit)?`，和「修改」并排显示红色的「删除」（只有「新反馈」里自己的建议才出现）
  - `ProjectDetail` 加 `deleteTarget`，弹出 `AlertDialog`：标题「删除这条建议？」，正文明写「删掉之后就找不回来了，别人也看不到了。只想改内容的话，点「修改」更合适。」——只确认一次，不用输入标题
  - 删成功 `snackbar.showSnackbar("已删除")` 并刷新列表；失败走 `Supa.friendly(e)`

### 2. 普通用户的列表改成时间逆序

- `ProjectDetail` 的三段（新反馈 / 待办 / 已解决）都加了 `sortedByDescending { it.createdAt }`，最新的在最上面
- 管理台保持原样（按提问顺序，最早的在最上面）——这是 0.0.1 就定下的需求，两边现在有意不同

### 验证

- `assembleDebug` + `assembleRelease` 通过；`aapt2 dump badging` 确认 `versionCode=6`、`versionName=0.0.6`
- 本机依旧连不上 `*.supabase.co`，「删除」按钮要有真实账号 + pending 建议才能点，这版靠编译 + 代码走查
- **用户侧要做的**：SQL Editor 里把 `supabase/schema.sql` 完整重跑一遍（这次新增 `delete_suggestion`；不跑的话点「删除」会提示「数据库还没升级」）

### 已知限制

- 只有提交人本人能删，管理员删不掉别人的建议（要删得先驳回）
- 删除是真删，不留在「已解决」里，也没有回收站

## v0.0.7 — 2026-11-23

用户一条：管理台「已解决」后面的数字被截成了「…」。

### 1. 页签从 TabRow 换成等宽分段控件

- 原因：Material3 的 `TabRow` 四个页签等分宽度，每个约 90dp，`labelLarge`(14sp) 的「已解决 12」加上 Tab 默认左右 padding 会溢出，于是被 `TextOverflow.Ellipsis` 截成「…」
- 改法：删掉 `TabRow`/`Tab`/`TabLabel`，新写 `AdminTabs(tab, labels, onSelect)`（`ui/AdminScreens.kt`）
  - `Row` + 圆角底 `Color(0xFFEDEFF7)`，每个页签 `Modifier.weight(1f)` 等分，选中项白底 + `Brand` 字 + `SemiBold`，未选中 `InkSoft`
  - 文字 `fontSize = 13.sp`、`maxLines = 1`、`softWrap = false`，不再用省略号：宽度够放「已解决 999」
  - 和登录页的分段控件（登录/注册）视觉统一
- `versionCode = 7`、`versionName = "0.0.7"`

### 验证

- `assembleDebug` + `assembleRelease` 通过；`aapt2 dump badging` 确认 `versionCode=7`、`versionName=0.0.7`；`dist/suggestion-collection-0.0.7.apk`（7,173,773 B，SHA-256 `DE9B82A8…`）
- 已通过 `deploy\upload.ps1` 传到服务器，下载页 `<http://39.96.113.178:8080/>` 显示 v0.0.7
- 页签实际效果要靠管理员账号看（本机连不上 Supabase，没法登进去截图），用户装完瞄一眼即可

### 已知限制

- 只有装饰变化，没有改任何数据逻辑，数据库不用重跑

## v0.0.8 — 2026-11-23

一次大搬家：后端从 Supabase 免费云搬到自己买的阿里云 ECS 上（用户要求「AB 都做，带我一步步做」），
历史数据一条不丢，账号体系换成自己签发的 token。

### 1. 服务器侧（`deploy/server/`）

- `schema.sql`：`users / projects / project_access / suggestions` 四张表，没有 RLS，权限全在接口层
  - `users`：`email`（唯一，`lower(email)` 索引）、`password_hash`（`scrypt$salt$hash`，salt 16 字节 hex、hash 64 字节 hex）、`display_name`、`is_admin`、`claimed`、`created_at`
  - 其余三张表和旧 Supabase 版字段一致，`suggestions` 保留五种状态和各个时间戳，`project_id` / `author_id` 都是 `on delete cascade`
- `server.js`：Node 22 原生 `http`，只依赖 `pg`
  - 鉴权：HMAC-SHA256 自签 token（payload `{uid,typ,exp}`），access 12 小时、refresh 60 天；`/auth/refresh` 要求把 refresh token 当 Bearer
  - 首个注册的用户自动成为管理员（库里为空时）；同邮箱若 `claimed = false`，注册就是「认领」——设新密码、保留原 id，历史建议不用改
  - `/home`：管理员拿全量（项目/成员/建议/授权），普通用户只拿自己有权进入的项目和建议
  - 状态流转和旧版一致：`accept / reject(理由≤400) / resolve / push(版本号≤30、说明≤200) / reopen`；`PATCH` 和 `DELETE` 只允许作者、且只在 `pending`
- `import.js`：把一个目录里的 4 个 JSON 灌进库，认领状态统一写 `claimed = false`；作者/项目缺失时跳过并打印
- `deploy-server.sh`：装 Node、装依赖、建表、写 env、装 systemd、自检，可重复执行

### 2. App 侧

- `data/Supa.kt`：`BASE = "http://39.96.113.178:8080/api"`，去掉了 Supabase 的 `apikey`；`friendly()` 改成兜我们自己服务端的中文报错 + 网络异常
- `data/Auth.kt`：`/auth/login`、`/auth/register`、`/auth/refresh`；退出不再需要通知服务端（token 自校验，不入库）
- `data/Repo.kt`：全部换成新端点，`projects()/suggestionCounts()/suggestionsOfProject()/adminBundle()` 统一从 `/home` 一次拿完再在本地分组
- `AndroidManifest.xml`：加 `android:usesCleartextTraffic="true"`（服务器没备案、没证书，只能走明文 http）
- `versionCode = 8`、`versionName = "0.0.8"`

### 3. 数据搬迁

- Supabase 从服务器上能连通（本机连不上），所以 `deploy/migrate-dump.sh` 在服务器上跑，secret key 走环境变量（**没有写进仓库**）
- 导出结果：`profiles=3 projects=4 project_access=6 suggestions=31`；`import.js` 导入 `users:3 projects:4 access:6 suggestions:31 skipped:[]`
- 导入后 3 个账号都是 `claimed = false`，需要在 App 里用原邮箱「注册」一次设新密码

### 4. 验证（这次是真的端到端，不是只看编译）

- 服务器：`curl http://127.0.0.1:3000/api/health` → `{"ok":true}`；`systemctl is-active suggestion-api` → active
- 公网：`/api/health` 200；注册/登录/`/me`/`/home` 全通；假账号登录返回 401 `{"error":"邮箱或密码不对"}`
- 接口层把状态机跑了一遍：建项目 → 提建议 → 接纳 → 解决 → 推送 9.9.9 → 退回 → 驳回（带理由）→ 删项目（建议跟着 cascade 删掉），每一步的状态都对
- 模拟器 emulator-5554 装 0.0.8：登录临时测试管理员后，管理台显示「新反馈 0 / 待办 3 / 已解决 28 / 管理」，四个页签数字完整不截断；「已解决」里 28 条真实历史建议都在，每条都有「退回待办」；「管理」页 4 个项目（1/6/12/12 条建议）和 3 个成员权限也都在
- 截图：`dist/admin-home-0.0.8.png`、`dist/admin-manage-0.0.8.png`
- 验证完把临时的 `test-admin@example.com` 和之前误建的 `smoke-test@example.com` 从库里删掉，库里只剩 3 个真实账号

### 5. 已知限制

- 明文 http：同一 wifi 下懂行的人能抓到 token；没有域名/证书之前就这样
- 没有找回密码：忘了新密码只能由管理员在库里重置
- 密码哈希和 token 都自写自签，没有做「主动吊销」（改密码后旧 token 到 12 小时才失效）
- 数据库没有定时备份，目前靠手动 `pg_dump`

### 用户要做的

1. 手机上装 0.0.8（覆盖安装即可，会提示「应用未安装」时才需要先卸载）
2. **点「注册」，用 `1412759076@qq.com` + 新密码**认领管理员账号（历史建议还在名下）
3. 其他两个老账号（aaa@qq.com、hby8848666@163.com）同样各自「注册」一次设新密码
4. 确认新后端用着没问题后，去 Supabase 后台把整个项目删掉

## v0.0.9 — 2026-11-24

用户提了两件事：**自己提的建议要能一眼认出来**、**提建议时能传图片**。

### 1. 服务器（`deploy/server/`）

- `schema.sql` 末尾加一列（可重复执行，不动老数据）：
  `alter table suggestions add column if not exists images jsonb not null default '[]'::jsonb;`
- `server.js` 新增：
  - `POST /api/upload`（必须登录）——body 是 `{data: "<base64>"}`，允许带 `data:image/...;base64,` 前缀；按文件头判断真实类型（`ffd8` = jpg、`89504e47` = png），其它一律拒；单张上限 5 MB；文件名 `crypto.randomBytes(12).toString('hex')`；写到 `/var/www/suggestion/uploads/`；返回 `{path, size}`（`path` 就是 `uploads/xxx.jpg`）
  - 建建议 / 改建议都收 `images` 数组：最多 3 张、必须是 `uploads/<24位hex>.jpg|png`、自动去重；`images` 传 `null` 或干脆不传 = 不动原来的图
  - 删建议、删项目（cascade 之前先把图路径查出来）会顺手把图片文件删掉
  - `readBody` 支持每条路由自己的上限：`/api/upload` 8 MB，其它还是 256 KB
- 部署到服务器：`scp` 三个文件到 `/root/upload/` → 复制进 `/opt/suggestion-api/` → `npm install --omit=dev` → `mkdir -p /var/www/suggestion/uploads` → `psql -f schema.sql`（结果 `images | jsonb | NO`，表里 40 行没动）→ `systemctl restart suggestion-api`

### 2. App 侧

- `data/Supa.kt`：`SITE = "http://39.96.113.178:8080"`、`imageUrl(path)`、`imageBytes(path)`
- `data/Models.kt`：`Suggestion` 加 `images: List<String>`
- `data/Repo.kt`：`createSuggestion(..., images)`、`updateSuggestion(..., images)`、`uploadImage(bytes)`
- `ui/Common.kt`：`MineChip()` —— 状态胶囊后面那个「我的反馈」小标签
- 新文件 `ui/ImageBits.kt`：
  - `ImageStore`：内存里缓存已下载的图（最多 24 张），列表滚动不会反复下载
  - `prepareImageBytes()`：解析尺寸算采样率 → 解码 → 按 EXIF 转正 → 长边压到 1200 px → JPEG 82
  - `RemoteImage` / `ImageStrip`（88 dp 方形缩略图，最多一排 3 个）/ `ImageViewer`（全屏深色看图，点一下关）/ `EditableImageTile`（编辑器里带 ✕ 的小格子）/ `AddImageButton`
- `ui/UserScreens.kt`：卡片在状态胶囊后加「我的反馈」（只在自己提的上面显示）；卡片底部显示缩略图，点开看大图；提建议 / 修改的编辑器加「图片（选填，最多 3 张）」，选图后立刻压缩上传，传完显示缩略图，可以点 ✕ 去掉
- `ui/AdminScreens.kt`：建议卡片在「来自 xxx」下面显示缩略图，同样可以点开看大图
- `versionCode = 9`、`versionName = "0.0.9"`

### 3. 验证

- 接口层（服务器上跑 Node 脚本，用临时管理员账号）：登录 → 传一张 179 B 的 PNG 得到 `uploads/48b1fc4467681f4682047037.png` → `GET /uploads/...` 返回 200 `image/png` → 带图建建议 → `/home` 读回来图还在 → 传 4 张被拒「一条建议最多 3 张图」→ 删建议后图片地址变 404（文件确实删了）
- 模拟器（装的是 debug 包，普通用户 + 临时管理员各走一遍）：
  - 登录页版本号 `v0.0.9`；普通用户「我的项目」只显示被授权的项目
  - 提建议编辑器出现「图片（选填，最多 3 张）」+「+ 加图片 0/3」，点开是系统相册；选一张后显示缩略图和 `1/3`，格子是正方形、✕ 在右上角
  - 提交后卡片：`待处理` + **`我的反馈`** + `修改` / `删除` + 88 dp 缩略图；点缩略图弹出全屏看图
  - 切到管理员「管理台」→「新反馈」：卡片带 `#序号` + 项目名 + `来自 user-test` + 缩略图 + `驳回` / `接纳`
- 截图：`dist/user-card-0.0.9.png`、`dist/editor-image-0.0.9.png`、`dist/viewer-0.0.9.png`、`dist/admin-card-0.0.9.png`
- 收尾：删掉临时账号 `img-test@example.com`、`user-test@example.com` 和它们的测试建议，uploads 里的孤立图片也清掉了；库里剩 4 个真实账号、40 条建议

### 4. 踩坑

- `EditableImageTile` 第一版把 `IconButton` 塞进 22 dp 的盒子，结果缩略图被压成 90×60 真实像素、✕ 也偏了。改成 `Box(Modifier.size(88.dp))` + `RemoteImage(Modifier.matchParentSize())` + 一个 20 dp 圆形 `clickable` 的 `Text("×")` 就正常了
- 模拟器验证被别的 App 抢前台 + 软键盘遮挡，来来回回折腾很久，经验已写进第 7 节
- 临时账号/测试数据的清理脚本要带 `NODE_PATH`（见第 7 节），否则 `require('pg')` 找不到

### 5. 已知限制

- 图片是静态公开目录：**知道文件名的人不登录也能访问**。文件名是 24 位随机 hex，猜不到，但严格说不是「鉴权后才能看」；以后要更严可以改成 `/api/image/:id` 走服务端校验
- 没有做图片总量上限和清理策略，用久了要看看 `/var/www/suggestion/uploads/` 占多大
- 仍是明文 http（和 0.0.8 一样）

### 用户要做的

1. 手机上装 0.0.9（覆盖安装即可）
2. 数据库这一列**我已经在服务器上跑过了**，不用你动手
3. 打开随便一条建议，试试传图、点开大图、看看「我的反馈」标签对不对

