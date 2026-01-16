package com.ruolijianzhen.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ruolijianzhen.app.domain.model.AiApiType
import com.ruolijianzhen.app.domain.model.UserAiConfig

/**
 * AI配置管理对话框
 * 支持多个AI配置的添加、编辑、删除和选择
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConfigDialog(
    configs: List<UserAiConfig>,
    activeConfigId: String?,
    isValidating: Boolean,
    onDismiss: () -> Unit,
    onAddConfig: (UserAiConfig) -> Unit,
    onUpdateConfig: (UserAiConfig) -> Unit,
    onDeleteConfig: (String) -> Unit,
    onSetActiveConfig: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<UserAiConfig?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    
    Dialog(
        onDismissRequest = { if (!isValidating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI服务配置",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                // 内容区域
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (configs.isEmpty()) {
                        // 空状态
                        EmptyConfigState(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        // 配置列表
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(configs, key = { it.id }) { config ->
                                AiConfigCard(
                                    config = config,
                                    isActive = config.id == activeConfigId,
                                    onSelect = { onSetActiveConfig(config.id) },
                                    onEdit = {
                                        editingConfig = config
                                        showEditDialog = true
                                    },
                                    onDelete = { showDeleteConfirm = config.id }
                                )
                            }
                        }
                    }
                }
                
                // 底部按钮区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 提示信息
                    Text(
                        text = "支持 Google Gemini 和 OpenAI 兼容格式",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // 添加按钮
                    Button(
                        onClick = {
                            editingConfig = null
                            showEditDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("添加AI配置")
                    }
                }
            }
        }
    }
    
    // 编辑/添加对话框
    if (showEditDialog) {
        AiConfigEditDialog(
            config = editingConfig,
            isValidating = isValidating,
            onDismiss = { showEditDialog = false },
            onSave = { config ->
                if (editingConfig != null) {
                    onUpdateConfig(config)
                } else {
                    onAddConfig(config)
                }
                showEditDialog = false
            }
        )
    }
    
    // 删除确认对话框
    showDeleteConfirm?.let { configId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("删除配置") },
            text = { Text("确定要删除这个AI配置吗？此操作无法撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteConfig(configId)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 空状态视图
 */
@Composable
private fun EmptyConfigState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无AI配置",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "添加AI服务以获得更智能的识别结果",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * AI配置卡片
 */
@Composable
private fun AiConfigCard(
    config: UserAiConfig,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        border = if (isActive) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 选中指示器
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = if (isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "已选中",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 配置信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.name.ifEmpty { config.apiType.displayName },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = config.modelName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // 操作按钮
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "编辑",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            // API类型标签
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = config.apiType.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}


/**
 * AI配置编辑对话框
 * 用于添加或编辑单个AI配置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiConfigEditDialog(
    config: UserAiConfig?,
    isValidating: Boolean,
    onDismiss: () -> Unit,
    onSave: (UserAiConfig) -> Unit
) {
    var configName by remember { mutableStateOf(config?.name ?: "") }
    var selectedApiType by remember { 
        mutableStateOf(config?.apiType ?: AiApiType.GOOGLE_GEMINI) 
    }
    var apiUrl by remember { mutableStateOf(config?.apiUrl ?: "") }
    var apiKey by remember { mutableStateOf(config?.apiKey ?: "") }
    var modelName by remember { mutableStateOf(config?.modelName ?: "") }
    var showApiKey by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = { if (!isValidating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 标题
                Text(
                    text = if (config != null) "编辑配置" else "添加AI配置",
                    style = MaterialTheme.typography.titleLarge
                )
                
                // 配置名称
                OutlinedTextField(
                    value = configName,
                    onValueChange = { configName = it },
                    label = { Text("配置名称") },
                    placeholder = { 
                        Text(
                            text = "例如：我的Gemini",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isValidating,
                    shape = RoundedCornerShape(12.dp)
                )
                
                // API类型选择
                Column {
                    Text(
                        text = "API类型",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AiApiType.entries.forEach { apiType ->
                            FilterChip(
                                selected = selectedApiType == apiType,
                                onClick = { selectedApiType = apiType },
                                label = { Text(apiType.displayName) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
                
                // API地址
                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = { Text("API地址") },
                    placeholder = { 
                        Text(
                            text = getDefaultUrl(selectedApiType),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isValidating,
                    shape = RoundedCornerShape(12.dp)
                )
                
                // API密钥
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API密钥") },
                    placeholder = { 
                        Text(
                            text = "输入你的API Key",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isValidating,
                    visualTransformation = if (showApiKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (showApiKey) "隐藏" else "显示"
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                
                // 模型名称
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("模型名称") },
                    placeholder = { 
                        Text(
                            text = getDefaultModel(selectedApiType),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isValidating,
                    shape = RoundedCornerShape(12.dp)
                )
                
                // 提示信息
                Text(
                    text = getHelpText(selectedApiType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                // 验证中提示
                if (isValidating) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "验证配置中...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isValidating,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = {
                            val finalApiUrl = apiUrl.ifBlank { getDefaultUrl(selectedApiType) }
                            val finalModelName = modelName.ifBlank { getDefaultModel(selectedApiType) }
                            val finalName = configName.ifBlank { getDefaultName(selectedApiType) }
                            
                            val newConfig = UserAiConfig(
                                id = config?.id ?: java.util.UUID.randomUUID().toString(),
                                name = finalName,
                                apiType = selectedApiType,
                                apiUrl = finalApiUrl.trim(),
                                apiKey = apiKey.trim(),
                                modelName = finalModelName.trim()
                            )
                            onSave(newConfig)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isValidating && apiKey.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

/**
 * 获取默认API地址
 */
private fun getDefaultUrl(apiType: AiApiType): String {
    return when (apiType) {
        AiApiType.GOOGLE_GEMINI -> "https://generativelanguage.googleapis.com"
        AiApiType.OPENAI_COMPATIBLE -> "https://api.openai.com"
    }
}

/**
 * 获取默认模型名称
 */
private fun getDefaultModel(apiType: AiApiType): String {
    return when (apiType) {
        AiApiType.GOOGLE_GEMINI -> "gemini-pro-vision"
        AiApiType.OPENAI_COMPATIBLE -> "gpt-4-vision-preview"
    }
}

/**
 * 获取默认配置名称
 */
private fun getDefaultName(apiType: AiApiType): String {
    return when (apiType) {
        AiApiType.GOOGLE_GEMINI -> "Gemini"
        AiApiType.OPENAI_COMPATIBLE -> "OpenAI"
    }
}

/**
 * 获取帮助文本
 */
private fun getHelpText(apiType: AiApiType): String {
    return when (apiType) {
        AiApiType.GOOGLE_GEMINI -> 
            "使用 Google Gemini API，需要在 Google AI Studio 获取密钥"
        AiApiType.OPENAI_COMPATIBLE -> 
            "支持 OpenAI 及兼容格式（通义千问、DeepSeek 等）"
    }
}

// ========== 兼容旧版单配置接口 ==========

/**
 * AI配置对话框（兼容旧版单配置接口）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConfigDialog(
    currentConfig: UserAiConfig?,
    isValidating: Boolean,
    onDismiss: () -> Unit,
    onSave: (AiApiType, String, String, String) -> Unit
) {
    var selectedApiType by remember { 
        mutableStateOf(currentConfig?.apiType ?: AiApiType.GOOGLE_GEMINI) 
    }
    var apiUrl by remember { mutableStateOf(currentConfig?.apiUrl ?: "") }
    var apiKey by remember { mutableStateOf(currentConfig?.apiKey ?: "") }
    var modelName by remember { mutableStateOf(currentConfig?.modelName ?: "") }
    var showApiKey by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = { if (!isValidating) onDismiss() },
        title = { Text("配置AI服务") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // API类型选择
                Text(
                    text = "API类型",
                    style = MaterialTheme.typography.labelLarge
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiApiType.entries.forEach { apiType ->
                        FilterChip(
                            selected = selectedApiType == apiType,
                            onClick = { selectedApiType = apiType },
                            label = { Text(apiType.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // API地址
                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = { Text("API地址") },
                    placeholder = { 
                        Text(
                            text = getDefaultUrl(selectedApiType),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isValidating
                )
                
                // API密钥
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API密钥") },
                    placeholder = { 
                        Text(
                            text = "输入你的API Key",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isValidating,
                    visualTransformation = if (showApiKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (showApiKey) "隐藏" else "显示"
                            )
                        }
                    }
                )
                
                // 模型名称
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("模型名称") },
                    placeholder = { 
                        Text(
                            text = getDefaultModel(selectedApiType),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isValidating
                )
                
                // 提示信息
                Text(
                    text = getHelpText(selectedApiType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                
                // 验证中提示
                if (isValidating) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("验证配置中...")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val finalApiUrl = apiUrl.ifBlank { getDefaultUrl(selectedApiType) }
                    val finalModelName = modelName.ifBlank { getDefaultModel(selectedApiType) }
                    onSave(selectedApiType, finalApiUrl, apiKey, finalModelName) 
                },
                enabled = !isValidating && apiKey.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isValidating
            ) {
                Text("取消")
            }
        }
    )
}
