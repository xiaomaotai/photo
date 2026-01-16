package com.ruolijianzhen.app.ui.main

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruolijianzhen.app.domain.model.ObjectInfo

/**
 * 识别结果BottomSheet - 简洁设计
 * 卡片式知识展示，去掉分享和收藏功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultBottomSheet(
    objectInfo: ObjectInfo,
    capturedBitmap: Bitmap? = null,
    onDismiss: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onFavorite: ((ObjectInfo) -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") onShare: ((ObjectInfo) -> Unit)? = null,
    onRecognizeAgain: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部图片和标题区域
            HeaderSection(
                objectInfo = objectInfo,
                capturedBitmap = capturedBitmap,
                onDismiss = onDismiss
            )
            
            // 内容区域 - 根据内容多少调整间距
            val hasQuickInfo = hasQuickInfoContent(objectInfo)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = if (hasQuickInfo) 16.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (hasQuickInfo) 16.dp else 12.dp)
            ) {
                // 简介卡片
                if (objectInfo.summary.hasContent()) {
                    SummaryCard(summary = objectInfo.summary!!)
                }
                
                // 快速信息卡片
                QuickInfoCards(objectInfo = objectInfo)
                
                // 详细信息区域
                DetailSection(objectInfo = objectInfo)
                
                // 趣味知识
                if (objectInfo.funFacts.isNotEmpty()) {
                    FunFactsSection(facts = objectInfo.funFacts)
                }
                
                // 使用技巧
                if (objectInfo.tips.isNotEmpty()) {
                    TipsSection(tips = objectInfo.tips)
                }
                
                // 再识别一次按钮
                if (onRecognizeAgain != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRecognizeAgain,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("再识别一次")
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 检查是否有快速信息内容
 */
private fun hasQuickInfoContent(objectInfo: ObjectInfo): Boolean {
    return objectInfo.brand.hasContent() ||
            objectInfo.model.hasContent() ||
            objectInfo.species.hasContent() ||
            objectInfo.material.hasContent() ||
            objectInfo.color.hasContent() ||
            objectInfo.priceRange.hasContent() ||
            objectInfo.manufacturer.hasContent() ||
            objectInfo.typeSpecificInfo.any { it.value.isNotBlank() && it.value.length < 50 }
}

/**
 * 顶部区域 - 图片、标题（去掉分享和收藏按钮）
 */
@Composable
private fun HeaderSection(
    objectInfo: ObjectInfo,
    capturedBitmap: Bitmap?,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // 背景图片或渐变
        if (capturedBitmap != null) {
            Image(
                bitmap = capturedBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // 渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
        } else {
            // 无图时使用更鲜明的渐变背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6366F1), // 紫蓝色
                                Color(0xFF8B5CF6), // 紫色
                                Color(0xFFA855F7)  // 浅紫色
                            )
                        )
                    )
            ) {
                // 添加装饰性图案
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(120.dp)
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(60.dp)
                        )
                )
                // 物品图标
                Text(
                    text = objectInfo.objectType.icon,
                    fontSize = 64.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        
        // 顶部关闭按钮
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .padding(12.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White
            )
        }
        
        // 底部标题区域
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            // 物品类型图标和分类
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = objectInfo.objectType.icon,
                    fontSize = 16.sp
                )
                Text(
                    text = objectInfo.category.ifBlank { "未分类" },
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 物品名称
            Text(
                text = objectInfo.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 置信度和来源
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConfidenceIndicator(confidence = objectInfo.confidence)
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = objectInfo.source.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 置信度指示器
 */
@Composable
private fun ConfidenceIndicator(confidence: Float) {
    val percentage = (confidence * 100).toInt()
    val color = when {
        percentage >= 80 -> Color(0xFF4CAF50)
        percentage >= 60 -> Color(0xFFFFC107)
        else -> Color(0xFFFF5722)
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 进度条
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(confidence)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
        
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 简介卡片 - 支持展开/收起
 */
@Composable
private fun SummaryCard(summary: String) {
    // 判断是否需要展开功能
    val isExpandable = summary.length >= 80
    var expanded by remember { mutableStateOf(!isExpandable) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isExpandable) {
                    Modifier.clickable { expanded = !expanded }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "简介",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // 只有可展开的内容才显示展开图标
                if (isExpandable) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isExpandable) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
                
                if (!expanded) {
                    Text(
                        text = summary.take(60) + "...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

/**
 * 快速信息卡片组
 */
@Composable
private fun QuickInfoCards(objectInfo: ObjectInfo) {
    val infoItems = buildList {
        if (objectInfo.brand.hasContent()) add("品牌" to objectInfo.brand!!)
        if (objectInfo.model.hasContent()) add("型号" to objectInfo.model!!)
        if (objectInfo.species.hasContent()) add("物种" to objectInfo.species!!)
        if (objectInfo.material.hasContent()) add("材质" to objectInfo.material!!)
        if (objectInfo.color.hasContent()) add("颜色" to objectInfo.color!!)
        if (objectInfo.priceRange.hasContent()) add("参考价" to objectInfo.priceRange!!)
        if (objectInfo.manufacturer.hasContent()) add("产地" to objectInfo.manufacturer!!)
        
        // 添加类型特定信息
        objectInfo.typeSpecificInfo.forEach { (key, value) ->
            if (value.isNotBlank() && value.length < 50) {
                add(key to value)
            }
        }
    }
    
    if (infoItems.isEmpty()) return
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(infoItems) { (label, value) ->
            QuickInfoCard(label = label, value = value)
        }
    }
}

/**
 * 单个快速信息卡片
 */
@Composable
private fun QuickInfoCard(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 详细信息区域
 */
@Composable
private fun DetailSection(objectInfo: ObjectInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 别名
        if (objectInfo.aliases.isNotEmpty()) {
            DetailItem(
                icon = Icons.Outlined.Label,
                title = "别名",
                content = objectInfo.aliases.joinToString("、")
            )
        }
        
        // 来历
        if (objectInfo.origin.hasContent() && objectInfo.origin != "暂无信息") {
            DetailItem(
                icon = Icons.Outlined.History,
                title = "来历",
                content = objectInfo.origin
            )
        }
        
        // 用途
        if (objectInfo.usage.hasContent() && objectInfo.usage != "暂无信息") {
            DetailItem(
                icon = Icons.Outlined.Lightbulb,
                title = "用途",
                content = objectInfo.usage
            )
        }
        
        // 详细描述
        if (objectInfo.description.hasContent()) {
            DetailItem(
                icon = Icons.Outlined.Description,
                title = "详细介绍",
                content = objectInfo.description!!
            )
        }
        
        // 特征列表
        if (objectInfo.features.isNotEmpty()) {
            DetailItem(
                icon = Icons.Outlined.Checklist,
                title = "特征",
                content = objectInfo.features.joinToString("\n• ", prefix = "• ")
            )
        }
    }
}

/**
 * 详细信息项 - 卡片式设计，与简介、知识卡片样式统一
 * 只有内容超过100字符时才支持展开/收起
 */
@Composable
private fun DetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String
) {
    // 判断是否需要展开功能
    val isExpandable = content.length >= 100
    var expanded by remember { mutableStateOf(!isExpandable) } // 短内容默认展开
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isExpandable) {
                    Modifier.clickable { expanded = !expanded }
                } else {
                    Modifier // 短内容不可点击
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // 只有可展开的内容才显示展开图标
                if (isExpandable) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isExpandable) {
                // 可展开内容：使用动画
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
                
                if (!expanded) {
                    Text(
                        text = content.take(80) + "...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                // 短内容：直接显示全部
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

/**
 * 趣味知识区域
 */
@Composable
private fun FunFactsSection(facts: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "💡", fontSize = 18.sp)
                Text(
                    text = "知识卡片",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            facts.forEach { fact ->
                Text(
                    text = fact,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * 使用技巧区域
 */
@Composable
private fun TipsSection(tips: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "📝", fontSize = 18.sp)
                Text(
                    text = "小贴士",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            tips.forEachIndexed { index, tip ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 检查字符串是否有实际内容
 */
private fun String?.hasContent(): Boolean {
    return !this.isNullOrBlank() && this.lowercase() != "null" && this != "暂无信息"
}
