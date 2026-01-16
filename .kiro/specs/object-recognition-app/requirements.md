# Requirements Document

## Introduction

**若里见真** 是一个Android原生物品识别应用，用户通过摄像头对准物品即可获取详细信息（来历、别名、用途等）。系统采用多层识别策略，优先使用内置离线库，自动无感切换到免费API或用户自配AI，确保用户体验丝滑流畅。

## Glossary

- **Recognition_Engine**: 识别引擎，负责调度和管理所有识别方式的核心模块
- **Offline_Recognizer**: 离线识别器，基于TensorFlow Lite和MobileNet的本地识别模块
- **API_Manager**: API管理器，负责管理多个免费API的额度和自动切换
- **Result_Formatter**: 结果格式化器，将不同来源的识别结果统一为标准展示格式
- **Object_Info**: 物品信息，包含名称、别名、来历、用途等标准化数据结构
- **Quota_Tracker**: 额度追踪器，记录和监控各API的剩余免费额度
- **User_AI_Config**: 用户AI配置，用户自行配置的AI服务（如OpenAI API Key）
- **Learning_Database**: 学习数据库，存储通过API/AI识别后自动缓存的新物品数据
- **History_Manager**: 历史管理器，负责保存和管理用户的识别历史记录

## Requirements

### Requirement 1: 摄像头实时预览与拍摄

**User Story:** As a 用户, I want 打开应用后直接看到摄像头预览画面, so that 我可以快速对准物品进行识别。

#### Acceptance Criteria

1. WHEN 用户启动应用 THEN THE Recognition_Engine SHALL 自动请求摄像头权限并显示实时预览画面
2. WHEN 用户点击拍摄按钮 THEN THE Recognition_Engine SHALL 捕获当前画面并开始识别流程
3. IF 摄像头权限被拒绝 THEN THE Recognition_Engine SHALL 显示友好提示并引导用户开启权限
4. WHILE 预览画面显示中 THE Recognition_Engine SHALL 保持流畅的帧率不低于24fps

### Requirement 2: 多层识别策略与无感切换

**User Story:** As a 用户, I want 系统自动选择最佳识别方式, so that 我不需要关心技术细节就能获得识别结果。

#### Acceptance Criteria

1. WHEN 识别请求发起 THEN THE Recognition_Engine SHALL 首先尝试使用Offline_Recognizer进行本地识别
2. WHEN Offline_Recognizer识别置信度低于阈值 THEN THE Recognition_Engine SHALL 自动切换到API_Manager调用在线API
3. WHEN API_Manager调用API THEN THE Recognition_Engine SHALL 对用户完全透明无感知
4. WHEN 所有免费API额度耗尽且用户已配置User_AI_Config THEN THE Recognition_Engine SHALL 自动使用用户配置的AI服务
5. IF 所有识别方式均失败 THEN THE Recognition_Engine SHALL 显示友好的"无法识别"提示

### Requirement 3: API额度智能管理与均衡切换

**User Story:** As a 用户, I want 系统自动管理API额度, so that 我可以最大化利用所有免费资源。

#### Acceptance Criteria

1. THE Quota_Tracker SHALL 持久化存储每个API的已用额度和重置周期
2. WHEN 某个API额度即将耗尽 THEN THE API_Manager SHALL 自动切换到下一个可用API
3. WHEN API调用失败或超时 THEN THE API_Manager SHALL 自动尝试下一个API而不中断用户操作
4. WHEN 新的计费周期开始 THEN THE Quota_Tracker SHALL 自动重置对应API的额度计数
5. THE API_Manager SHALL 支持至少3个不同的免费额度API服务

### Requirement 4: 统一结果格式化与展示

**User Story:** As a 用户, I want 看到统一格式的识别结果, so that 我可以快速理解物品信息而不受识别来源影响。

#### Acceptance Criteria

1. WHEN 任意识别方式返回结果 THEN THE Result_Formatter SHALL 将结果转换为标准Object_Info格式
2. THE Object_Info SHALL 包含以下字段：名称、别名列表、来历描述、用途说明、分类标签
3. WHEN 某些字段信息缺失 THEN THE Result_Formatter SHALL 使用默认占位符而非显示空白
4. THE Result_Formatter SHALL 确保所有展示内容为中文或用户设置的语言

### Requirement 5: 用户AI配置管理

**User Story:** As a 用户, I want 配置自己的AI服务作为备选, so that 当免费API不可用时仍能使用应用。

#### Acceptance Criteria

1. WHEN 用户进入设置页面 THEN THE User_AI_Config SHALL 提供AI服务配置入口
2. THE User_AI_Config SHALL 支持配置API地址和API Key
3. WHEN 用户保存配置 THEN THE User_AI_Config SHALL 验证配置有效性并安全存储
4. WHEN 用户未配置AI服务且所有免费API不可用 THEN THE Recognition_Engine SHALL 提示用户配置自定义AI

### Requirement 6: 内置物品信息数据库

**User Story:** As a 用户, I want 应用内置常见物品的详细信息, so that 离线状态下也能获取丰富的物品描述。

#### Acceptance Criteria

1. THE Offline_Recognizer SHALL 内置包含至少500种常见物品的信息数据库
2. WHEN 离线识别成功匹配 THEN THE Offline_Recognizer SHALL 直接返回内置数据库中的详细信息
3. THE 内置数据库 SHALL 包含日常用品、食物、电子产品、植物等常见类别
4. THE 内置数据库 SHALL 支持应用更新时同步更新

### Requirement 7: 友好的用户界面与交互

**User Story:** As a 用户, I want 简洁直观的操作界面, so that 我可以轻松使用应用而无需学习。

#### Acceptance Criteria

1. THE Recognition_Engine SHALL 提供单一主界面完成拍摄和查看结果
2. WHEN 识别完成 THEN THE Recognition_Engine SHALL 在摄像头预览页面上方弹出结果弹窗，不关闭摄像头
3. THE 结果弹窗 SHALL 清晰标注物品名称、关键信息，支持显示物品图片
4. WHEN 用户关闭弹窗 THEN THE Recognition_Engine SHALL 返回摄像头预览状态可继续识别
5. THE Recognition_Engine SHALL 在识别过程中显示简洁的加载动画
6. THE Recognition_Engine SHALL 支持深色模式和浅色模式
7. THE UI设计 SHALL 采用Material Design 3现代化设计语言
8. THE 交互动画 SHALL 流畅自然，过渡时间控制在300ms以内

### Requirement 8: 摄像头丝滑体验

**User Story:** As a 用户, I want 摄像头预览和拍摄过程流畅无卡顿, so that 我可以快速准确地对准物品。

#### Acceptance Criteria

1. THE Camera_Preview SHALL 保持至少30fps的流畅帧率
2. WHEN 用户点击拍摄 THEN THE Recognition_Engine SHALL 在200ms内完成图像捕获
3. THE Camera_Preview SHALL 支持自动对焦和点击对焦
4. THE Camera_Preview SHALL 在弱光环境下自动调整曝光
5. WHILE 识别进行中 THE Camera_Preview SHALL 保持预览不中断
6. THE Camera_Preview SHALL 支持前后摄像头切换

### Requirement 9: Android系统与品牌兼容性

**User Story:** As a 用户, I want 应用在我的手机上正常运行, so that 无论使用什么品牌和系统版本都能使用。

#### Acceptance Criteria

1. THE Application SHALL 支持Android 8.0 (API 26)及以上版本
2. THE Application SHALL 兼容主流品牌（华为、小米、OPPO、vivo、三星、一加等）
3. THE Camera_Preview SHALL 使用CameraX API确保跨设备兼容性
4. THE Application SHALL 适配不同屏幕尺寸和分辨率（手机和平板）
5. THE Application SHALL 正确处理各品牌的权限管理差异
6. THE Application SHALL 适配刘海屏、挖孔屏、折叠屏等异形屏幕

### Requirement 10: 识别结果自动学习与缓存

**User Story:** As a 用户, I want 系统自动记住新识别的物品, so that 下次识别相同物品时更快更准确。

#### Acceptance Criteria

1. WHEN API或用户AI识别到新物品 THEN THE Recognition_Engine SHALL 自动将结果存入Learning_Database
2. THE Learning_Database SHALL 与内置数据库格式完全一致
3. WHEN 下次识别相同物品 THEN THE Offline_Recognizer SHALL 优先匹配Learning_Database
4. THE 自动学习过程 SHALL 对用户完全透明无感知
5. THE Learning_Database SHALL 支持用户手动清理或管理

### Requirement 11: 识别历史记录与回看

**User Story:** As a 用户, I want 查看历史识别记录, so that 我可以回顾之前识别过的物品信息。

#### Acceptance Criteria

1. THE History_Manager SHALL 自动保存每次识别的结果到历史记录
2. THE 历史记录 SHALL 按日期分组展示（今天、昨天、更早日期）
3. WHEN 用户点击历史记录入口 THEN THE History_Manager SHALL 显示按日期归类的识别列表
4. WHEN 用户点击某条历史记录 THEN THE History_Manager SHALL 展示完整的Object_Info详情
5. THE 历史记录 SHALL 显示识别时间、物品缩略图和物品名称
6. THE History_Manager SHALL 支持用户删除单条或清空全部历史记录
