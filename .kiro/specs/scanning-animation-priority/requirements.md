# Requirements Document

## Introduction

本文档定义了"若里见真"物品识别应用的三个新功能需求：
1. 首页面设计 - 大气简洁的应用入口页面，提供清晰的功能导航
2. 扫描动画效果 - 在识别过程中显示炫酷的扫描动画，提升用户体验
3. 识别优先级设置 - 允许用户自定义识别方式的优先级顺序

## Glossary

- **Home_Screen**: 首页面，应用启动后的第一个页面，提供功能入口
- **Scanning_Animation**: 扫描动画组件，在识别过程中显示的视觉反馈效果
- **Recognition_Priority**: 识别优先级，定义离线识别、API识别、AI识别的执行顺序
- **Priority_Manager**: 优先级管理器，负责存储和读取用户的优先级配置
- **Recognition_Engine**: 识别引擎，根据优先级配置执行识别策略
- **Settings_Screen**: 设置页面，用户配置识别优先级的界面

## Requirements

### Requirement 1: 首页面设计

**User Story:** As a user, I want to see a beautiful and simple home page when I open the app, so that I can easily understand the app's purpose and navigate to different features.

#### Acceptance Criteria

1. WHEN the app launches, THE Home_Screen SHALL be displayed as the first screen
2. THE Home_Screen SHALL display the app logo and name "若里见真" prominently in the center-top area
3. THE Home_Screen SHALL display a tagline/slogan describing the app's purpose (e.g., "智能识别，探索万物")
4. THE Home_Screen SHALL provide a large, prominent "开始识别" button as the primary action
5. WHEN the user taps the "开始识别" button, THE System SHALL navigate to the camera/recognition screen
6. THE Home_Screen SHALL display quick access icons for "历史记录" and "设置" in a secondary area
7. THE Home_Screen SHALL use a clean, minimalist design with ample white space
8. THE Home_Screen SHALL display a subtle animated background or gradient effect to enhance visual appeal
9. THE Home_Screen SHALL adapt to different screen sizes while maintaining visual balance

### Requirement 2: 扫描动画效果

**User Story:** As a user, I want to see an engaging scanning animation when recognizing objects, so that I have visual feedback that the app is working and the experience feels more polished.

#### Acceptance Criteria

1. WHEN the user taps the capture button, THE Scanning_Animation SHALL display a pulsing circular ring animation around the center of the screen
2. WHEN the recognition process starts, THE Scanning_Animation SHALL show a scanning line effect moving vertically across the preview area
3. WHILE the recognition is in progress, THE Scanning_Animation SHALL display a rotating border effect with gradient colors
4. WHEN the recognition completes successfully, THE Scanning_Animation SHALL fade out smoothly within 300ms
5. IF the recognition fails, THEN THE Scanning_Animation SHALL display a brief error pulse effect before dismissing
6. THE Scanning_Animation SHALL NOT block user interaction with the cancel button during processing
7. THE Scanning_Animation SHALL maintain 60fps performance on devices running Android 8.0+

### Requirement 2: 识别优先级配置

**User Story:** As a user, I want to customize the order of recognition methods, so that I can prioritize AI recognition for more detailed results when I have configured my AI service.

#### Acceptance Criteria

1. THE Settings_Screen SHALL display a "识别优先级" (Recognition Priority) section with draggable list items
2. WHEN the user opens the priority settings, THE System SHALL show three recognition methods: "本地识别", "百度API", "自定义AI"
3. THE System SHALL allow users to drag and reorder the recognition methods to set their preferred priority
4. WHEN the user changes the priority order, THE Priority_Manager SHALL persist the configuration to local storage immediately
5. WHEN the app starts, THE Priority_Manager SHALL load the saved priority configuration
6. IF no priority configuration exists, THEN THE System SHALL use the default order: 本地识别 → 百度API → 自定义AI
7. WHEN recognition is triggered, THE Recognition_Engine SHALL execute recognition methods in the user-configured priority order
8. IF a higher-priority method fails or returns low confidence, THEN THE Recognition_Engine SHALL fall back to the next method in the priority list
9. THE Settings_Screen SHALL display a toggle to enable/disable each recognition method individually
10. IF a recognition method is disabled, THEN THE Recognition_Engine SHALL skip that method during recognition

### Requirement 3: 优先级配置持久化

**User Story:** As a user, I want my priority settings to be saved, so that I don't have to reconfigure them every time I open the app.

#### Acceptance Criteria

1. WHEN the user modifies priority settings, THE Priority_Manager SHALL save the configuration using DataStore
2. THE Priority_Manager SHALL store both the order and enabled/disabled state of each recognition method
3. WHEN the app launches, THE Priority_Manager SHALL restore the last saved configuration
4. IF the stored configuration is corrupted, THEN THE Priority_Manager SHALL reset to default settings and log the error

### Requirement 4: 动画性能优化

**User Story:** As a user, I want the scanning animation to be smooth and not affect app performance, so that the recognition process remains fast.

#### Acceptance Criteria

1. THE Scanning_Animation SHALL use hardware-accelerated rendering via Compose animations
2. THE Scanning_Animation SHALL NOT cause frame drops below 30fps during recognition
3. WHILE the animation is running, THE System SHALL continue processing recognition in the background without blocking
4. THE Scanning_Animation SHALL automatically adapt to different screen sizes and aspect ratios

### Requirement 5: 多AI模型配置

**User Story:** As a user, I want to configure multiple AI services and choose which one to use, so that I can switch between different AI providers based on my needs.

#### Acceptance Criteria

1. THE Settings_Screen SHALL allow users to add multiple AI configurations
2. WHEN adding a new AI configuration, THE System SHALL display a form with API type, URL, key, and model name fields
3. THE input fields SHALL display placeholder text in a lighter color (not as default values)
4. WHEN the user taps an input field, THE System SHALL clear any placeholder text immediately
5. THE Settings_Screen SHALL display a list of all configured AI services
6. THE System SHALL allow users to select one AI configuration as the active/default one
7. WHEN recognition uses User AI, THE System SHALL use the currently active AI configuration
8. THE System SHALL allow users to edit or delete existing AI configurations
9. THE System SHALL persist all AI configurations and the active selection to local storage
