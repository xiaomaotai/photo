# Implementation Plan: Home Screen, Scanning Animation & Recognition Priority

## Overview

本实现计划将分阶段完成三个功能：首页面、扫描动画、识别优先级配置。采用增量开发方式，确保每个阶段都有可测试的成果。

## Tasks

- [x] 1. 创建优先级配置数据模型和存储层
  - [x] 1.1 创建RecognitionMethod枚举和PriorityConfig数据类
    - 在`domain/model/`目录创建`PriorityConfig.kt`
    - 定义RecognitionMethod枚举（OFFLINE, BAIDU_API, USER_AI）
    - 定义RecognitionMethodConfig和PriorityConfig数据类
    - _Requirements: 2.2, 2.6_
  - [x] 1.2 创建PriorityConfigStore接口和实现
    - 在`domain/priority/`创建`PriorityConfigStore.kt`接口
    - 在`data/priority/`创建`PriorityConfigStoreImpl.kt`
    - 使用DataStore进行JSON序列化存储
    - _Requirements: 3.1, 3.2_
  - [x] 1.3 创建PriorityManager接口和实现
    - 在`domain/priority/`创建`PriorityManager.kt`接口
    - 在`data/priority/`创建`PriorityManagerImpl.kt`
    - 实现getConfig、saveConfig、getEnabledMethodsInOrder等方法
    - _Requirements: 2.4, 2.5, 3.3, 3.4_
  - [ ]* 1.4 编写PriorityConfig往返一致性属性测试
    - **Property 1: Priority Configuration Round-Trip**
    - **Validates: Requirements 2.4, 2.5, 3.2, 3.3**

- [x] 2. 更新RecognitionEngine支持优先级配置
  - [x] 2.1 修改RecognitionEngineImpl支持动态优先级
    - 注入PriorityManager依赖
    - 修改executeRecognitionStrategy方法，按配置顺序执行
    - 实现跳过禁用方法的逻辑
    - _Requirements: 2.7, 2.8, 2.10_
  - [ ]* 2.2 编写识别顺序执行属性测试
    - **Property 2: Recognition Order Execution**
    - **Validates: Requirements 2.7, 2.8**
  - [ ]* 2.3 编写禁用方法跳过属性测试
    - **Property 3: Disabled Method Skipping**
    - **Validates: Requirements 2.10**

- [ ] 3. Checkpoint - 确保优先级功能测试通过
  - 运行所有属性测试和单元测试
  - 确保配置保存/加载正常工作
  - 如有问题请询问用户

- [x] 4. 创建首页面HomeScreen
  - [x] 4.1 创建HomeScreen组件
    - 在`ui/home/`目录创建`HomeScreen.kt`
    - 实现渐变背景（浅蓝到白色）
    - 添加App Logo和标题"若里见真"
    - 添加标语"智能识别，探索万物"
    - _Requirements: 1.1, 1.2, 1.3, 1.7_
  - [x] 4.2 实现主按钮和次要入口
    - 创建大尺寸"开始识别"按钮（圆角、阴影）
    - 创建底部"历史记录"和"设置"卡片入口
    - 添加按钮点击动画效果
    - _Requirements: 1.4, 1.6_
  - [x] 4.3 添加Logo呼吸动画
    - 使用Compose动画API实现轻微缩放呼吸效果
    - 确保动画流畅不卡顿
    - _Requirements: 1.8_

- [x] 5. 更新导航结构
  - [x] 5.1 修改Navigation配置
    - 将HomeScreen设为启动页面
    - 添加HomeScreen到CameraScreen的导航
    - 更新HistoryScreen和SettingsScreen的返回导航
    - _Requirements: 1.1, 1.5_
  - [x] 5.2 重构MainScreen为CameraScreen
    - 将现有MainScreen重命名为CameraScreen
    - 移除顶部AppBar（改用返回按钮）
    - 添加返回首页的导航
    - _Requirements: 1.5_

- [x] 6. 创建扫描动画组件
  - [x] 6.1 创建ScanningAnimationState和基础组件
    - 在`ui/animation/`创建`ScanningAnimation.kt`
    - 定义ScanningAnimationState密封类
    - 创建基础动画容器
    - _Requirements: 2.1_
  - [x] 6.2 实现脉冲圆环动画（Scanning状态）
    - 从中心向外扩散的圆环效果
    - 透明度渐变消失
    - 循环播放
    - _Requirements: 2.1_
  - [x] 6.3 实现扫描线和旋转边框动画（Processing状态）
    - 水平扫描线从上到下移动
    - 四角旋转渐变边框
    - 科技感光效
    - _Requirements: 2.2, 2.3_
  - [x] 6.4 实现成功和错误状态动画
    - Success：绿色对勾 + 缩放弹出 + 淡出
    - Error：红色脉冲 + 轻微震动 + 淡出
    - _Requirements: 2.4, 2.5_

- [x] 7. 集成扫描动画到CameraScreen
  - [x] 7.1 在CameraScreen中集成ScanningAnimation
    - 替换现有LoadingOverlay
    - 根据识别状态切换动画状态
    - 确保动画不阻塞取消按钮
    - _Requirements: 2.6, 2.7_
  - [x] 7.2 优化动画性能
    - 使用remember和derivedStateOf优化重组
    - 确保60fps流畅运行
    - _Requirements: 4.1, 4.2, 4.3_

- [ ] 8. Checkpoint - 确保首页和动画功能正常
  - 测试首页导航流程
  - 测试扫描动画各状态切换
  - 如有问题请询问用户

- [x] 9. 创建优先级设置UI
  - [x] 9.1 创建PrioritySettingsDialog组件
    - 在`ui/settings/`创建`PrioritySettingsDialog.kt`
    - 显示三种识别方式列表
    - 每项显示拖拽手柄和启用/禁用开关
    - _Requirements: 2.1, 2.2, 2.9_
  - [x] 9.2 实现拖拽排序功能
    - 使用Compose的拖拽API实现列表排序
    - 拖拽时显示视觉反馈
    - _Requirements: 2.3_
  - [x] 9.3 实现保存和验证逻辑
    - 保存时验证至少有一个方法启用
    - 保存成功后关闭对话框
    - 显示保存成功Toast
    - _Requirements: 2.4_

- [x] 10. 集成优先级设置到SettingsScreen
  - [x] 10.1 在SettingsScreen添加优先级设置入口
    - 添加"识别优先级"设置项
    - 显示当前优先级顺序预览
    - 点击打开PrioritySettingsDialog
    - _Requirements: 2.1_
  - [x] 10.2 创建SettingsViewModel优先级相关方法
    - 添加priorityConfig状态
    - 添加savePriorityConfig方法
    - 处理保存成功/失败状态
    - _Requirements: 2.4_

- [x] 11. 更新Hilt依赖注入配置
  - [-] 11.1 添加PriorityManager相关依赖注入
    - 在AppModule中添加PriorityConfigStore绑定
    - 添加PriorityManager绑定
    - 更新RecognitionEngine依赖
    - _Requirements: 2.7_

- [x] 12. 多AI模型配置功能
  - [x] 12.1 扩展AI配置数据模型支持多个配置
    - 修改UserAiConfig添加id和name字段
    - 创建UserAiConfigList数据类存储多个配置
    - 添加activeConfigId字段标记当前使用的配置
    - _Requirements: 新增_
  - [x] 12.2 更新UserAiConfigStore支持多配置存储
    - 修改saveConfig为addConfig（添加新配置）
    - 添加updateConfig（更新现有配置）
    - 添加deleteConfig（删除配置）
    - 添加setActiveConfig（设置当前使用的配置）
    - 添加getAllConfigs（获取所有配置列表）
    - _Requirements: 新增_
  - [x] 12.3 更新UserAiService使用当前激活的配置
    - 修改recognize方法使用activeConfig
    - 添加getActiveConfig方法
    - _Requirements: 新增_
  - [x] 12.4 重构AiConfigDialog支持多配置管理
    - 显示已配置的AI模型列表
    - 支持添加新配置、编辑、删除
    - 支持选择当前使用的配置（单选）
    - 高亮显示当前激活的配置
    - _Requirements: 新增_
  - [x] 12.5 优化输入框占位符样式
    - 将默认提示文案颜色调淡（使用onSurfaceVariant颜色）
    - 点击输入框时清除占位符（使用placeholder而非默认值）
    - 确保占位符不会被当作实际输入
    - _Requirements: 新增_

- [ ] 13. Final Checkpoint - 完整功能测试
  - 测试完整用户流程：首页→识别→查看结果
  - 测试优先级配置：修改顺序→保存→验证识别顺序
  - 测试多AI配置：添加多个AI→切换→验证使用正确配置
  - 测试动画效果：各状态切换流畅
  - 确保所有测试通过，如有问题请询问用户
  - 测试完整用户流程：首页→识别→查看结果
  - 测试优先级配置：修改顺序→保存→验证识别顺序
  - 测试动画效果：各状态切换流畅
  - 确保所有测试通过，如有问题请询问用户

## Notes

- 任务标记 `*` 的为可选测试任务，可根据时间跳过
- 每个Checkpoint都是验证点，确保阶段性成果可用
- 属性测试使用Kotest框架
- 所有UI组件使用Material 3设计规范
