package com.ruolijianzhen.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ruolijianzhen.app.domain.model.PriorityConfig
import com.ruolijianzhen.app.domain.model.RecognitionMethod
import com.ruolijianzhen.app.domain.model.RecognitionMethodConfig

/**
 * 优先级设置对话框
 * 支持拖拽排序和启用/禁用切换
 */
@Composable
fun PrioritySettingsDialog(
    currentConfig: PriorityConfig,
    onConfigChanged: (PriorityConfig) -> Unit,
    onDismiss: () -> Unit
) {
    // 本地编辑状态
    var editingMethods by remember(currentConfig) {
        mutableStateOf(currentConfig.methods.sortedBy { it.priority })
    }
    
    // 检查是否至少有一个方法启用
    val hasEnabledMethod = editingMethods.any { it.enabled }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // 标题
                Text(
                    text = "识别优先级设置",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 说明
                Text(
                    text = "长按拖拽调整顺序，优先使用排在前面的方式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 方法列表
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = editingMethods,
                        key = { _, item -> item.method.name }
                    ) { index, methodConfig ->
                        PriorityMethodItem(
                            index = index + 1,
                            methodConfig = methodConfig,
                            onEnabledChanged = { enabled ->
                                editingMethods = editingMethods.map {
                                    if (it.method == methodConfig.method) {
                                        it.copy(enabled = enabled)
                                    } else it
                                }
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    val newList = editingMethods.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index - 1]
                                    newList[index - 1] = temp
                                    editingMethods = newList.mapIndexed { i, config ->
                                        config.copy(priority = i)
                                    }
                                }
                            },
                            onMoveDown = {
                                if (index < editingMethods.size - 1) {
                                    val newList = editingMethods.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index + 1]
                                    newList[index + 1] = temp
                                    editingMethods = newList.mapIndexed { i, config ->
                                        config.copy(priority = i)
                                    }
                                }
                            },
                            canMoveUp = index > 0,
                            canMoveDown = index < editingMethods.size - 1
                        )
                    }
                }
                
                // 错误提示
                if (!hasEnabledMethod) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "至少需要启用一种识别方式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = {
                            val newConfig = PriorityConfig(
                                methods = editingMethods.mapIndexed { index, config ->
                                    config.copy(priority = index)
                                }
                            )
                            onConfigChanged(newConfig)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = hasEnabledMethod
                    ) {
                        Text("保存设置")
                    }
                }
            }
        }
    }
}

/**
 * 优先级方法项
 */
@Composable
private fun PriorityMethodItem(
    index: Int,
    methodConfig: RecognitionMethodConfig,
    onEnabledChanged: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (methodConfig.enabled) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 排序按钮
            Column {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "上移",
                        tint = if (canMoveUp) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "下移",
                        tint = if (canMoveDown) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 序号
            Text(
                text = "$index.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 方法名称
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = methodConfig.method.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = getMethodDescription(methodConfig.method),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 启用开关
            Switch(
                checked = methodConfig.enabled,
                onCheckedChange = onEnabledChanged
            )
        }
    }
}

/**
 * 获取方法描述
 */
private fun getMethodDescription(method: RecognitionMethod): String {
    return when (method) {
        RecognitionMethod.OFFLINE -> "使用本地模型，无需网络"
        RecognitionMethod.BAIDU_API -> "使用百度AI接口"
        RecognitionMethod.USER_AI -> "使用自定义AI服务"
    }
}
