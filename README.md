# 建议收集 / suggestion-collection

一个自己用的安卓 App：把想法和建议收集起来，由管理员逐条接纳、驳回、标记解决、跟着版本推送。
一台手机做管理端，其他人的手机做提交端，数据放在 Supabase 免费云端共享。

当前版本：**v0.0.1**（第一个能跑通的版本）

---

## 它做什么

**普通用户（提交端）**

1. 用邮箱 + 密码注册 / 登录
2. 首页只看到管理员给自己开放的项目
3. 进项目 → 看这个项目里所有人提过的建议和它们的状态
4. 点「提建议」写下标题（必填，50 字内）和内容（选填，1000 字内）
5. 自己提的建议被驳回时能看到驳回理由，被推送时能看到版本号

**管理员（你的手机）**

1. 登录后没有"选项目"这一步，直接进管理台
2. 「待办」页汇总所有项目的建议，按提出时间从早到晚排列，一条都不会漏
3. 每条建议可以：
   - **接纳** → 状态变「已接纳」
   - **驳回** → 必须填理由（400 字内），提交人能看到
   - 接纳后 **标记解决** → 状态变「已解决」
   - 解决后 **推送** → 填推送版本号（和可选说明），这条算完成
4. 「已解决」页列出所有推送过的建议，带版本号和推送时间
5. 「管理」页可以新建项目、给某个成员勾选他能进入的项目

**规则细节**

- 一个人只能看到、提交被授权的项目；没授权就是看不到
- 一个项目的成员能看到这个项目里所有人的建议（互相看得见，但不显示是谁提的；只有管理员看得到提交人）
- 状态流转固定为：`待处理 → 已接纳 → 已解决 → 已推送`，或者 `待处理 → 已驳回`
- 「已推送」= 完成，进入已解决页

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
    ├── AdminScreens.kt      管理员：待办 / 已解决 / 管理（项目 + 权限）
    └── theme/Theme.kt       配色和字体
supabase/schema.sql          数据库结构 + 权限规则（一次性执行）
log.md                       项目记忆 + 每个版本的改动日志
```

---

## 第一次跑起来：三步

### 1. 建数据库

打开 [Supabase 控制台](https://supabase.com/dashboard/project/pnnyujqkkdehfxnmomww) →
**SQL Editor** → New query → 把 [`supabase/schema.sql`](supabase/schema.sql) 整段粘贴 → **Run**。
脚本可以重复执行。

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
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

`local.properties` 里写你的 SDK 路径（这个文件不进仓库）：

```properties
sdk.dir=D:\\applications\\SDK
```

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
| 0.0.1 | 账号系统、项目与权限、提建议、接纳/驳回/解决/推送、已解决页、管理台 |

详细改动见 [log.md](log.md)。
