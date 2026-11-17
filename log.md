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
- 状态流转不可逆：`pending → accepted → resolved → pushed`，或 `pending → rejected`；不做「撤销驳回」「退回」
- 「已推送」= 完成；「已解决」页 = 所有 `pushed` 的建议
- 推送不发系统通知，提交人在自己的项目里刷新就能看到版本号

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

## 8. 发布流程（每个版本都这么做）

1. 改代码 → `gradle assembleDebug` 通过
2. 装到模拟器上点一遍关键流程，没有崩溃
3. 更新 README 里的版本号和本文件末尾的版本日志
4. `git add -A && git commit -m "版本描述" && git push`

## 9. Supabase 关键信息

- Project URL：`https://pnnyujqkkdehfxnmomww.supabase.co`（写在 `data/Supa.kt` 的 `BASE`）
- publishable key：写在 `data/Supa.kt` 的 `API_KEY`（这种 key 设计上就是公开给客户端的，真正保护靠 RLS）
- 管理员邮箱：`1412759076@qq.com`
- 数据库结构在 `supabase/schema.sql`，改表结构时同步改这个文件，保证别人能一键重建
- **坑**：Supabase 默认开着 `Confirm email`，注册后不能直接登录。已在 README 里写明要去 Authentication → Providers → Email 关掉

## 10. 现在的待办 / 以后可能要做

- [ ] 找回密码（发重置邮件）
- [ ] 推送后给提交人发通知（本地或 FCM）
- [ ] 建议详情页 + 评论讨论
- [ ] 管理员删除项目 / 归档项目（表里已有 `is_archived`，界面还没做）
- [ ] 建议列表按状态筛选、按项目筛选
- [ ] 搜索、分页（建议多了以后必须做）
- [ ] 正式签名的 release APK

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
