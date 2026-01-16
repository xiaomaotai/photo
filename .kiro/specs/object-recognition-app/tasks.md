# Implementation Plan: 若里见真

## Overview

本实现计划将 **若里见真** 物品识别应用分解为可执行的开发任务，采用增量开发方式，确保每个阶段都有可验证的成果。优先实现核心识别功能，再逐步完善UI和高级特性。

## Tasks

- [x] 1. 项目初始化与基础架构
  - [x] 1.1 创建Android项目，配置Gradle依赖
    - 配置Kotlin、Jetpack Compose、CameraX、Hilt、Room、DataStore
    - 设置minSdk=26，targetSdk=34
    - 添加TensorFlow Lite依赖
    - _Requirements: 9.1_
  - [x] 1.2 创建项目目录结构
    - 创建ui/、domain/、data/、di/包结构
    - 配置Hilt Application类
    - _Requirements: 9.1_
  - [x] 1.3 定义核心数据模型
    - 创建ObjectInfo、ObjectDetails、RecognitionState等数据类
    - 创建RecognitionSource枚举
    - _Requirements: 4.1, 4.2_

- [x] 2. 离线识别模块
  - [x] 2.1 集成TensorFlow Lite和MobileNet模型
    - 下载并添加MobileNet V2 tflite模型到assets
    - 创建TFLiteClassifier类封装模型推理
    - _Requirements: 2.1, 6.1_
  - [x] 2.2 实现OfflineRecognizer接口
    - 实现图片预处理（缩放、归一化）
    - 实现模型推理和结果解析
    - 返回标签和置信度
    - _Requirements: 2.1, 6.2_
  - [x] 2.3 编写OfflineRecognizer属性测试
    - **Property 8: 离线识别数据完整性**
    - **Validates: Requirements 6.2**
  - [x] 2.4 创建内置物品数据库
    - 使用Room创建ObjectEntity表
    - 预填充500+常见物品的中文信息
    - 实现标签到详情的查询
    - _Requirements: 6.1, 6.2, 6.3_

- [x] 3. Checkpoint - 离线识别验证
  - 确保TFLite模型加载成功
  - 确保内置数据库查询正常
  - 确保所有测试通过，如有问题请询问用户

- [x] 4. 结果格式化模块
  - [x] 4.1 实现ResultFormatter
    - 实现OfflineResult到ObjectInfo的转换
    - 实现ApiResult到ObjectInfo的转换
    - 实现AiResult到ObjectInfo的转换
    - 处理缺失字段，使用默认占位符
    - _Requirements: 4.1, 4.2, 4.3_
  - [x] 4.2 编写ResultFormatter属性测试
    - **Property 6: 结果格式化完整性**
    - **Validates: Requirements 4.1, 4.2, 4.3**

- [x] 5. API额度管理模块
  - [x] 5.1 实现QuotaTracker
    - 使用Room存储API额度状态
    - 实现额度查询、扣减、重置逻辑
    - 实现日/月周期自动重置
    - _Requirements: 3.1, 3.4_
  - [x] 5.2 编写QuotaTracker属性测试
    - **Property 4: 额度持久化round-trip**
    - **Property 5: 额度重置正确性**
    - **Validates: Requirements 3.1, 3.4**
  - [x] 5.3 实现ApiManager
    - 实现API优先级选择逻辑
    - 实现额度耗尽自动切换
    - 实现调用失败自动重试下一个API
    - _Requirements: 3.2, 3.3, 3.5_
  - [x] 5.4 编写ApiManager属性测试
    - **Property 2: API额度切换正确性**
    - **Validates: Requirements 3.2, 3.3**


- [x] 6. 在线API集成
  - [x] 6.1 实现百度AI图像识别客户端
    - 封装百度AI通用物体识别API
    - 实现Token获取和刷新
    - 实现图片Base64编码上传
    - _Requirements: 3.5_
  - [x] 6.2 实现腾讯云图像识别客户端
    - 封装腾讯云图像识别API
    - 实现签名认证
    - _Requirements: 3.5_
  - [x] 6.3 实现阿里云图像识别客户端
    - 封装阿里云图像识别API
    - 实现签名认证
    - _Requirements: 3.5_

- [x] 7. Checkpoint - API集成验证
  - 确保三个API客户端可正常调用
  - 确保额度管理正常工作
  - 确保所有测试通过，如有问题请询问用户

- [x] 8. 用户AI服务模块
  - [x] 8.1 实现UserAiConfig数据存储
    - 使用DataStore加密存储API Key
    - 实现配置的保存和读取
    - _Requirements: 5.2, 5.3_
  - [x] 8.2 编写UserAiConfig属性测试
    - **Property 7: 用户配置持久化round-trip**
    - **Validates: Requirements 5.3**
  - [x] 8.3 实现Google Gemini客户端
    - 封装Gemini Vision API调用
    - 实现图片识别请求
    - 解析返回的物品描述
    - _Requirements: 5.1, 5.2_
  - [x] 8.4 实现OpenAI兼容客户端
    - 封装OpenAI Vision API格式
    - 支持自定义API地址（兼容通义千问、DeepSeek等）
    - _Requirements: 5.1, 5.2_
  - [x] 8.5 实现UserAiService
    - 根据配置类型选择对应客户端
    - 实现配置验证
    - _Requirements: 5.1, 5.3, 5.4_

- [x] 9. 识别引擎核心
  - [x] 9.1 实现RecognitionEngine
    - 实现三层识别策略调度
    - 离线优先，置信度低于0.7时切换API
    - API耗尽时切换用户AI
    - 实现RecognitionState状态流
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_
  - [x] 9.2 编写RecognitionEngine属性测试
    - **Property 1: 识别优先级保证**
    - **Property 3: 降级策略完整性**
    - **Validates: Requirements 2.1, 2.2, 2.4**

- [x] 10. Checkpoint - 核心识别功能验证
  - 确保识别引擎完整工作
  - 确保三层策略正确切换
  - 确保所有测试通过，如有问题请询问用户

- [x] 11. 摄像头模块
  - [x] 11.1 实现CameraX预览
    - 配置CameraX Provider
    - 实现PreviewView绑定
    - 确保30fps流畅预览
    - _Requirements: 1.1, 8.1_
  - [x] 11.2 实现图像捕获
    - 实现ImageCapture用例
    - 确保200ms内完成捕获
    - 实现Bitmap转换
    - _Requirements: 1.2, 8.2_
  - [x] 11.3 实现摄像头增强功能
    - 实现自动对焦和点击对焦
    - 实现弱光自动曝光调整
    - 实现前后摄像头切换
    - _Requirements: 8.3, 8.4, 8.6_

- [x] 12. UI主界面
  - [x] 12.1 实现MainScreen
    - 使用Jetpack Compose创建主界面
    - 集成CameraPreview组件
    - 添加拍摄按钮（底部居中）
    - 添加设置和历史入口
    - _Requirements: 7.1, 9.4_
  - [x] 12.2 实现识别结果弹窗
    - 创建BottomSheet样式结果卡片
    - 显示物品名称、别名、来历、用途
    - 支持展开查看完整详情
    - 弹窗不关闭摄像头预览
    - _Requirements: 7.2, 7.3, 7.4_
  - [x] 12.3 实现加载动画
    - 创建简洁的识别中动画
    - 动画过渡时间控制在300ms内
    - _Requirements: 7.5, 7.8_
  - [x] 12.4 实现深色/浅色模式
    - 配置Material Design 3主题
    - 实现跟随系统主题切换
    - _Requirements: 7.6, 7.7_

- [x] 13. 设置页面
  - [x] 13.1 实现SettingsScreen
    - 创建设置页面布局
    - 添加AI配置入口
    - 添加主题切换选项
    - _Requirements: 5.1_
  - [x] 13.2 实现AI配置界面
    - 创建API类型选择（Google Gemini / OpenAI兼容）
    - 创建API地址和Key输入表单
    - 实现配置验证和保存
    - _Requirements: 5.1, 5.2, 5.3_

- [x] 14. Checkpoint - UI功能验证
  - 确保摄像头预览流畅
  - 确保识别流程完整
  - 确保UI交互正常，如有问题请询问用户

- [x] 15. 学习数据库模块
  - [x] 15.1 实现LearningDatabase
    - 创建学习数据表（与内置数据库格式一致）
    - 实现新物品自动存储
    - 实现查询优先级（学习库优先）
    - _Requirements: 10.1, 10.2, 10.3, 10.4_
  - [x] 15.2 实现学习数据管理
    - 添加清理学习数据功能
    - 在设置页面添加管理入口
    - _Requirements: 10.5_

- [x] 16. 历史记录模块
  - [x] 16.1 实现HistoryManager
    - 创建历史记录数据表
    - 实现自动保存识别结果
    - 实现按日期分组查询
    - _Requirements: 11.1, 11.2_
  - [x] 16.2 实现HistoryScreen
    - 创建历史记录列表页面
    - 按日期分组显示（今天、昨天、更早）
    - 显示缩略图、名称、时间
    - _Requirements: 11.2, 11.3, 11.5_
  - [x] 16.3 实现历史详情和管理
    - 点击查看完整详情
    - 支持删除单条记录
    - 支持清空全部历史
    - _Requirements: 11.4, 11.6_

- [x] 17. 兼容性适配
  - [x] 17.1 实现屏幕适配
    - 使用WindowInsets适配异形屏
    - 适配刘海屏、挖孔屏、折叠屏
    - 适配不同屏幕尺寸
    - _Requirements: 9.4, 9.6_
  - [x] 17.2 实现权限兼容处理
    - 封装权限请求逻辑
    - 处理各品牌权限差异
    - 实现权限被拒绝的引导
    - _Requirements: 1.3, 9.5_

- [x] 18. 最终集成与优化
  - [x] 18.1 性能优化
    - 优化TFLite推理性能（GPU/NNAPI加速）
    - 优化图片处理内存占用
    - 确保UI流畅度
    - _Requirements: 1.4, 8.1_
  - [x] 18.2 错误处理完善
    - 实现全局异常处理
    - 完善各模块错误提示
    - _Requirements: 2.5_

- [x] 19. Final Checkpoint - 完整功能验证
  - 确保所有功能正常工作
  - 确保在不同设备上兼容
  - 确保所有测试通过，如有问题请询问用户

## Notes

- 所有测试任务均为必需，确保代码质量
- 每个Checkpoint是验证阶段性成果的节点
- 属性测试使用Kotest框架，每个测试运行100+次迭代
- 优先完成核心识别功能（任务1-10），再完善UI和高级特性
