# 基于大模型与规则引擎混合驱动的 IntelliJ IDEA 智能代码评审插件

面向 Java 企业开发场景的智能代码评审插件：以 IntelliJ PSI 结构感知为基础，规则引擎与大模型双路评审、互相校正，为每条评审意见生成证据链，并支持一键修复。

## 参赛信息

- 赛事：2026 年第 22 届湖南省大学生计算机程序设计竞赛 · 应用开发竞赛
- 作品类别：桌面应用类

## 核心特性

1. **双引擎混合评审**：PMD 规则引擎负责确定性缺陷，大模型负责语义级问题，二者双向增强、互相校正
2. **框架感知评审**：基于 IntelliJ PSI 做结构级分析，检测 Spring 事务失效等框架级缺陷
3. **证据链**：每条评审意见附带"问题行 + 判定依据"，降低误报带来的不信任
4. **一键修复闭环**：生成修复补丁，Diff 预览确认后一键应用

## 技术栈

| 模块 | 选型 |
| --- | --- |
| 插件端 | Java + IntelliJ Platform SDK（Gradle 构建，目标 IDEA 2024.2+） |
| 规则引擎 | PMD 7 |
| 大模型 | OpenAI 兼容 API（DeepSeek / 通义千问，密钥由用户自行配置） |
| 代码分析 | IntelliJ PSI / Inspection API |

## 目录结构

```
├── docs/                          # 项目文档（系统设计说明书）
├── src/
│   └── main/
│       ├── java/com/aireview/     # 插件源码
│       │   ├── model/             # IssueModel 契约（三方共用）
│       │   ├── engine/            # 评审引擎与融合裁决
│       │   ├── analysis/          # PSI 框架感知分析
│       │   └── ui/                # 插件交互
│       └── resources/META-INF/    # plugin.xml 插件描述文件
├── gradle/                        # Gradle Wrapper
├── build.gradle.kts               # 构建配置（目标平台 IDEA 2024.2+）
├── README.md
└── .gitignore
```

## 模块构成

| 模块 | 职责 |
| --- | --- |
| 插件端与交互 | 入口、结果面板、定位、一键修复 UI |
| 评审引擎与融合 | PMD 集成、大模型服务、融合裁决 |
| PSI 框架感知分析 | 框架分析器、事务失效检测规则、测试样本集 |

模块间只通过统一缺陷模型 `IssueModel` 与冻结接口通信。

## 开发环境

- JDK：**JDK 17**（工程工具链统一要求，全组安装 17 即可），产出 17 字节码与目标平台匹配
- IntelliJ IDEA：目标平台 2024.2+，开发用任意较新版本即可
- Gradle Wrapper：工程内自带（Gradle 9.5）

## 运行与调试

1. 用 IDEA 打开本工程目录，等待 Gradle 同步完成
2. 打开 Gradle 工具窗口 → `任务 → intellij platform → runIde`
3. 首次运行会自动下载目标平台 IDEA 沙箱（约数百 MB），之后弹出沙箱窗口即为就绪

## 相关文档

- [系统设计说明书](docs/系统设计说明书.md)
