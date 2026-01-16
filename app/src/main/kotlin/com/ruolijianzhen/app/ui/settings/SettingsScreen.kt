package com.ruolijianzhen.app.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruolijianzhen.app.data.preferences.ThemeMode
import com.ruolijianzhen.app.domain.model.PriorityConfig

/**
 * 设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val aiConfigList by viewModel.aiConfigList.collectAsState()
    val learningDataCount by viewModel.learningDataCount.collectAsState()
    val priorityConfig by viewModel.priorityConfig.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    
    var showAiConfigDialog by remember { mutableStateOf(false) }
    var showClearLearningDialog by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCleanDataDialog by remember { mutableStateOf(false) }
    
    // 处理保存成功
    LaunchedEffect(uiState) {
        if (uiState is SettingsUiState.SaveSuccess) {
            viewModel.resetState()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 外观设置
            SettingsSection(title = "外观") {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "主题模式",
                    subtitle = themeMode.displayName,
                    onClick = { showThemeDialog = true }
                )
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsSwitchItem(
                        icon = Icons.Default.Palette,
                        title = "动态颜色",
                        subtitle = "使用系统壁纸颜色",
                        checked = dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 识别设置
            SettingsSection(title = "识别设置") {
                SettingsItem(
                    icon = Icons.Default.Sort,
                    title = "识别优先级",
                    subtitle = getPriorityPreview(priorityConfig),
                    onClick = { showPriorityDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // AI配置
            SettingsSection(title = "AI配置") {
                SettingsItem(
                    icon = Icons.Default.SmartToy,
                    title = "自定义AI服务",
                    subtitle = getAiConfigSubtitle(aiConfigList),
                    onClick = { showAiConfigDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 数据管理
            SettingsSection(title = "数据管理") {
                SettingsItem(
                    icon = Icons.Default.School,
                    title = "学习数据",
                    subtitle = "已学习 $learningDataCount 个物品",
                    onClick = { }
                )
                
                SettingsItem(
                    icon = Icons.Default.CleaningServices,
                    title = "清理旧数据",
                    subtitle = "删除30天前的非收藏记录",
                    onClick = { showCleanDataDialog = true }
                )
                
                if (learningDataCount > 0) {
                    SettingsItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "清除学习数据",
                        subtitle = "删除所有通过API/AI学习的物品数据",
                        onClick = { showClearLearningDialog = true }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 关于
            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "若里见真",
                    subtitle = "版本 1.0.0",
                    onClick = { }
                )
            }
            
            // 错误提示
            if (uiState is SettingsUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = (uiState as SettingsUiState.Error).message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // 主题选择对话框
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = themeMode,
            onModeSelected = { viewModel.setThemeMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }
    
    // 优先级设置对话框
    if (showPriorityDialog) {
        PrioritySettingsDialog(
            currentConfig = priorityConfig,
            onConfigChanged = { viewModel.savePriorityConfig(it) },
            onDismiss = { showPriorityDialog = false }
        )
    }
    
    // AI配置对话框
    if (showAiConfigDialog) {
        AiConfigDialog(
            configs = aiConfigList.configs,
            activeConfigId = aiConfigList.activeConfigId,
            isValidating = viewModel.isValidating.collectAsState().value,
            onDismiss = { showAiConfigDialog = false },
            onAddConfig = { viewModel.addAiConfig(it) },
            onUpdateConfig = { viewModel.updateAiConfig(it) },
            onDeleteConfig = { viewModel.deleteAiConfig(it) },
            onSetActiveConfig = { viewModel.setActiveAiConfig(it) }
        )
    }
    
    // 清除学习数据确认
    if (showClearLearningDialog) {
        AlertDialog(
            onDismissRequest = { showClearLearningDialog = false },
            title = { Text("清除学习数据") },
            text = { Text("确定要清除所有学习数据吗？此操作不可恢复。") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearLearningData()
                    showClearLearningDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showClearLearningDialog = false }) { Text("取消") }
            }
        )
    }
    
    // 清理旧数据确认
    if (showCleanDataDialog) {
        AlertDialog(
            onDismissRequest = { showCleanDataDialog = false },
            title = { Text("清理旧数据") },
            text = { Text("将删除30天前的历史记录（收藏的记录会保留）。") },
            confirmButton = {
                Button(onClick = {
                    viewModel.cleanOldData(30)
                    showCleanDataDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCleanDataDialog = false }) { Text("取消") }
            }
        )
    }
}

/**
 * 主题选择对话框
 */
@Composable
private fun ThemeSelectionDialog(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onModeSelected(mode)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == currentMode,
                            onClick = {
                                onModeSelected(mode)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/**
 * 获取AI配置副标题
 */
private fun getAiConfigSubtitle(configList: com.ruolijianzhen.app.domain.model.UserAiConfigList): String {
    val activeConfig = configList.getActiveConfig()
    val configCount = configList.configs.size
    
    return when {
        configCount == 0 -> "未配置"
        activeConfig != null -> {
            val name = activeConfig.name.ifEmpty { activeConfig.apiType.displayName }
            if (configCount > 1) "$name (共${configCount}个配置)" else "$name · ${activeConfig.modelName}"
        }
        else -> "已配置${configCount}个"
    }
}

/**
 * 获取优先级预览
 */
private fun getPriorityPreview(config: PriorityConfig): String {
    val enabledMethods = config.methods
        .filter { it.enabled }
        .sortedBy { it.priority }
        .map { it.method.displayName }
    
    return if (enabledMethods.isEmpty()) "未启用任何识别方式" else enabledMethods.joinToString(" → ")
}

/**
 * 设置区块
 */
@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

/**
 * 设置项
 */
@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 带开关的设置项
 */
@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * AiApiType扩展属性
 */
val com.ruolijianzhen.app.domain.model.AiApiType.displayName: String
    get() = when (this) {
        com.ruolijianzhen.app.domain.model.AiApiType.GOOGLE_GEMINI -> "Google Gemini"
        com.ruolijianzhen.app.domain.model.AiApiType.OPENAI_COMPATIBLE -> "OpenAI兼容"
    }
