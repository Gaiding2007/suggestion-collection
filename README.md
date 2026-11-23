# 建议收集 / suggestion-collection

一个自己用的安卓 App：把想法和建议收集起来，由管理员逐条接纳、驳回、标记解决、跟着版本推送。
一台手机做管理端，其他人的手机做提交端，数据放在 Supabase 免费云端共享。

当前版本：**v0.0.4**（可以发给同学装了）

---

## 它做什么

**普通用户（提交端）**

1. 用邮箱 + 密码注册 / 登录
2. 首页只看到管理员给自己开放的项目
3. 进项目 → 看这个项目里所有人提过的建议，按「新反馈 / 待办 / 已解决」三段分组
4. 点「提建议」写下标题（必填，50 字内）和内容（选填，1000 字内）
5. 还没被处理的建议，自己可以点「修改」改标题和内容
6. 自己提的建议被驳回时能看到驳回理由，被推送时能看到版本号
7. 标题、内容、驳回理由都能长按选中复制

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
   - 解决后 **推送** → 填推送版本号（和可选说明），这条算完成
4. 已经表态的还能改：`已驳回` 可以「改为已接纳」，`已接纳 / 已解决` 也可以「改为驳回」；推送后是终态，不再改

**规则细节**

- 一个人只能看到、提交被授权的项目；没授权就是看不到
- 一个项目的成员能看到这个项目里所有人的建议（互相看得见，但不显示是谁提的；只有管理员看得到提交人）
- 状态流转：`待处理 → 已接纳 → 已解决 → 已推送`，`待处理 → 已驳回`，以及管理员在「已接纳 ⇄ 已驳回」之间互相纠正
- 「已推送」= 完成；「已解决」页 = 推送完的 + 被驳回的
- 提交人只在「待处理」阶段能改自己的建议，进了流转就锁住

---

## 技术栈

| 部分 | 选型 |
| --- | --- |
| 客户端 | Kotlin + Jetpack Compose（Material 3），单 Activity |
| 最低系统 | Android 8.0（API 26） |
| 后端 | Supabase 免费版（Postgres + Auth + PostgREST） |
| 网络 | 只用系统自带 `HttpURLConnection` + `org.json`，无第三方网络库 |
| 构建 | Gradle 8.14 + AGP 8.13.1 + Kotlin 2.3.0，compileSdk 36 |

不引入 Retrofit / Ktor / Supabase SDK，APK 小、离线构建也稳。

---

## 目录结构

```
app/src/main/java/com/gaiding/suggestion/
├── MainActivity.kt          入口
├── AppRoot.kt               登录态路由：启动 → 登录页 / 管理台 / 用户首页
├── SuggestionApp.kt         Application，初始化本地会话
├── data/
│   ├── Supa.kt              极简 Supabase HTTP 客户端（含错误中文化）
│   ├── Auth.kt              注册、登录、access token 自动续期、退出
│   ├── SessionStore.kt      SharedPreferences 存 token
│   ├── Models.kt            Profile / Project / Suggestion / Status
│   └── Repo.kt              所有数据读写（项目、授权、建议、状态流转）
└── ui/
    ├── Common.kt            时间格式、状态胶囊、按钮、空状态等公共组件
    ├── LoginScreen.kt       登录 / 注册
    ├── UserScreens.kt       普通用户：项目列表 → 项目详情 → 提建议
    ├── AdminScreens.kt      管理员：新反馈 / 待办 / 已解决 / 管理（项目 + 权限）
    └── theme/Theme.kt       配色和字体
supabase/schema.sql          数据库结构 + 权限规则（一次性执行）
log.md                       项目记忆 + 每个版本的改动日志
```

---

## 第一次跑起来：三步

### 1. 建数据库

打开 [Supabase 控制台](https://supabase.com/dashboard/project/pnnyujqkkdehfxnmomww) →
**SQL Editor** → New query → 把 [`supabase/schema.sql`](supabase/schema.sql) 整段粘贴 → **Run**。
脚本可以重复执行；**升级到新版本后也重跑一次**（比如 0.0.4 新增了 `update_suggestion` 函数，不跑的话提交人点「修改」会报错）。

### 2. 关掉邮箱验证

**Authentication → Providers → Email**，把 **Confirm email** 关掉并保存。
（开着的话注册完不能直接登录，还得去邮箱点链接。）

### 3. 把自己变成管理员

先在手机上用管理员邮箱注册一个账号（先别管它是不是管理员），然后回到 SQL Editor 单独执行：

```sql
update public.profiles set is_admin = true where email = '你的管理员邮箱';
```

重新打开 App 就是管理台了。

> 其他人在他们手机装同一个 APK、注册自己的账号，你就能在「管理 → 成员权限」里给他们勾项目。

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
4. 多人同时在用同一个 Supabase 项目，看到的是同一份数据

---

## 数据安全说明

- 所有读写走 Supabase 的行级安全（RLS）：普通用户只能读自己有权限的项目和建议，不能改状态、不能删除、不能给自己开权限
- 提建议只能走 `create_suggestion()` 函数，函数里强制 `author_id = 当前登录用户`
- 管理员身份存在 `profiles.is_admin`，只有管理员自己能在 SQL Editor 里改
- 客户端里写的是 Supabase 的 publishable key（设计上就是给客户端用的公开 key），真正的保护在 RLS

---

## 版本

| 版本 | 内容 |
| --- | --- |
| 0.0.4 | 页签拆成「新反馈 / 待办 / 已解决 / 管理」、驳回的进已解决、提交人在被处理前能改建议、管理员可在接纳↔驳回间改回、文字长按可复制 |
| 0.0.3 | 修复「删除项目显示成功但没删」（现在删不掉会明确报错）、退出登录不再忘记账号、顶栏新增「切换」一键换账号 |
| 0.0.2 | 登录页记住多个账号一键切换、管理员彻底删除项目、正式签名 release 包 |
| 0.0.1 | 账号系统、项目与权限、提建议、接纳/驳回/解决/推送、已解决页、管理台 |

详细改动见 [log.md](log.md)。
