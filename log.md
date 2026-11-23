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

## 8. 发布流程（每个版本都这么做）

1. 改代码 → `gradle assembleDebug assembleRelease` 都通过
2. 装到模拟器上点一遍关键流程，没有崩溃
3. 更新 README 里的版本号和本文件末尾的版本日志
4. 把要发的 APK 复制到 `dist/`（已被 gitignore，不提交）
5. `git add -A && git commit -m "版本描述" && git push`
6. 给用户的交付说明里写清：装哪个 APK、这版改了什么、数据库要不要补 SQL

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



