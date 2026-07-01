# 智能记账助手

一款基于 Android 原生开发的个人财务管理应用，支持记账、预算控制、债务管理、统计分析、账单导出以及账单图片识别等能力。项目采用 Kotlin + Jetpack 构建前端，依赖后端 REST API 提供用户、账本与统计数据服务。

## 项目描述

智能记账助手是一个面向个人日常财务管理场景的 Android 应用，目标是帮助用户以更低的操作成本完成收支记录、预算控制与消费分析。项目围绕“记账 + 统计 + 财务管理”展开，提供登录注册、月度账本、预算预警、债务跟踪、年度报告、数据导出和账单识别等功能，并通过图表化方式展示收支趋势与分类占比，提升财务数据的可读性与实用性。

## 核心功能

- 用户系统
  - 手机号注册与登录
  - 本地会话保存与登录状态恢复
  - 个人资料查看与编辑
- 记账管理
  - 按月查看账单记录
  - 新增、编辑、删除收支记录
  - 展示当月总收入与总支出
  - 支持账单图片识别并回填表单
- 预算管理
  - 按分类设置月度预算
  - 展示已使用金额与预算进度
  - 支持超支预警
- 债务管理
  - 记录借入 / 借出
  - 维护到期时间、已还金额与状态
  - 支持还款操作
- 数据分析
  - 收支趋势折线图
  - 支出分类饼图
  - 年度财务报告
  - 简单财务洞察文案生成
- 数据导出
  - 支持导出 `Excel`、`CSV`、`TXT`
  - 支持文件分享
- 个性化设置
  - 深色模式开关
  - 关于页、退出登录等基础设置

## 当前实现状态

已完成模块：
- 登录 / 注册 / 会话恢复
- 记账 CRUD 与月度账单查询
- 预算管理
- 债务管理
- 统计图表与年度报告
- 数据导出工具
- 深色模式基础切换
- 图片识别接口接入

开发中或占位模块：
- 周期账单管理 `RecurringBillActivity`
- 账户管理 `AccountActivity`
- 云备份、通知设置等部分设置项

## 技术栈

- 语言：`Kotlin`
- 平台：`Android` 原生应用
- UI：`Material Design`、`ViewBinding`
- 架构组件：`Navigation Component`、`ViewModel`、`LiveData`
- 图表：`MPAndroidChart`
- 网络通信：`HttpURLConnection`
- 数据解析：`org.json`、`Gson`
- 并发：`Thread`、`Kotlin Coroutines`（已引入依赖）
- 文件导出：`Apache POI`
- 本地存储：`SharedPreferences`

## 项目结构

```text
app/src/main/java/com/example/myapplication/
├── models/                  # 数据模型
├── ui/
│   ├── home/                # 首页账单列表
│   ├── dashboard/           # 快速记账
│   ├── statistics/          # 统计分析
│   └── notifications/       # 我的 / 设置
├── utils/                   # 导出、分类建议等工具
├── ApiClient.kt             # 后端接口封装
├── MainActivity.kt          # 主页面与底部导航
├── LoginActivity.kt         # 登录
├── RegisterActivity.kt      # 注册
├── BudgetActivity.kt        # 预算管理
├── DebtActivity.kt          # 债务管理
├── YearlyReportActivity.kt  # 年度报告
├── RecurringBillActivity.kt # 周期账单（开发中）
└── AccountActivity.kt       # 账户管理（开发中）
```

## 后端依赖

应用依赖后端 API，默认地址为：

```text
http://127.0.0.1:8084/api
```

已接入的接口类型包括：
- 用户管理
- 记账管理
- 预算管理
- 债务管理
- 周期账单
- 账户管理
- 统计分析

详细接口说明见 `BACKEND_API_REQUIREMENTS.md`。

## 运行环境

- Android Studio
- Android SDK `26+`
- `compileSdk 34`
- JDK 8+
- 可用的后端服务，监听 `8084` 端口

## 快速开始

1. 克隆项目并使用 Android Studio 打开。
2. 等待 Gradle 同步完成。
3. 启动后端服务，确保接口地址可访问。
4. 连接真机或启动模拟器。
5. 运行应用。

项目已在 Gradle 中配置 `adb reverse tcp:8084 tcp:8084`，调试构建前会尝试自动反向代理本地后端端口。

## 构建命令

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Windows：

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

## 特色亮点

- 围绕个人财务管理设计，功能覆盖记账、预算、债务与分析全流程
- 提供图表化统计与年度报告，便于观察消费结构与收支趋势
- 支持账单导出与分享，方便留档与二次处理
- 预留图片识别、周期账单、账户体系等扩展方向

## 后续优化方向

- 将网络层升级为 `Retrofit + OkHttp`
- 引入 `Room` 支持本地缓存与离线能力
- 完善周期账单与账户管理页面
- 增加通知提醒、云同步与多端协作能力
- 优化异常处理、加载状态与测试覆盖率

## 说明

当前仓库主要为 Android 客户端工程，完整业务能力依赖对应后端接口配合运行。
