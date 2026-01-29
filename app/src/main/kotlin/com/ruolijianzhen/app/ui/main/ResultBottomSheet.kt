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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruolijianzhen.app.domain.model.ObjectInfo
import com.ruolijianzhen.app.domain.model.ObjectType
import com.ruolijianzhen.app.domain.model.RecognitionQuality

/**
 * 获取ObjectType对应的Material Icon
 */
private fun ObjectType.getIcon(): ImageVector = when (this) {
    ObjectType.GENERAL -> Icons.Default.Inventory2
    ObjectType.ELECTRONICS -> Icons.Default.Smartphone
    ObjectType.ANIMAL -> Icons.Default.Pets
    ObjectType.PLANT -> Icons.Default.Eco
    ObjectType.FOOD -> Icons.Default.Restaurant
    ObjectType.DAILY_USE -> Icons.Default.Home
    ObjectType.ARTWORK -> Icons.Default.Palette
    ObjectType.LANDMARK -> Icons.Default.AccountBalance
    ObjectType.VEHICLE -> Icons.Default.DirectionsCar
    ObjectType.CLOTHING -> Icons.Default.Checkroom
    ObjectType.BOOK -> Icons.Default.AutoStories
}

/**
 * 识别结果BottomSheet - 简洁设计
 * 卡片式知识展示，集成质量指示器
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val quality = remember(objectInfo.confidence) {
        RecognitionQuality.fromConfidence(objectInfo.confidence)
    }
    
    var showQualityExplanation by remember { mutableStateOf(false) }
    
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
                quality = quality,
                onDismiss = onDismiss,
                onQualityClick = { showQualityExplanation = !showQualityExplanation }
            )
            
            val hasQuickInfo = hasQuickInfoContent(objectInfo)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = if (hasQuickInfo) 16.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (hasQuickInfo) 16.dp else 12.dp)
            ) {
                // 质量说明卡片
                AnimatedVisibility(
                    visible = showQualityExplanation,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    QualityExplanationCard(
                        quality = quality,
                        confidence = objectInfo.confidence,
                        onDismiss = { showQualityExplanation = false }
                    )
                }
                
                if (objectInfo.summary.hasContent()) {
                    SummaryCard(summary = objectInfo.summary!!)
                }
                
                QuickInfoCards(objectInfo = objectInfo)
                DetailSection(objectInfo = objectInfo)
                
                if (objectInfo.funFacts.isNotEmpty()) {
                    FunFactsSection(facts = objectInfo.funFacts)
                }
                
                if (objectInfo.tips.isNotEmpty()) {
                    TipsSection(tips = objectInfo.tips)
                }
                
                if (onRecognizeAgain != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (quality == RecognitionQuality.LOW || quality == RecognitionQuality.UNCERTAIN) {
                        LowConfidenceWarning(quality = quality)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    OutlinedButton(
                        onClick = onRecognizeAgain,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, null, Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("再识别一次")
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun LowConfidenceWarning(quality: RecognitionQuality) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(quality.color).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Info, null, tint = Color(quality.color), modifier = Modifier.size(20.dp))
            Text(
                text = "识别结果可信度较低，建议重新拍摄",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QualityExplanationCard(
    quality: RecognitionQuality,
    confidence: Float,
    onDismiss: () -> Unit
) {
    val qualityColor = Color(quality.color)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = qualityColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = when (quality) {
                            RecognitionQuality.HIGH -> Icons.Default.CheckCircle
                            RecognitionQuality.MEDIUM -> Icons.Default.Check
                            RecognitionQuality.LOW -> Icons.Default.Warning
                            RecognitionQuality.UNCERTAIN -> Icons.Default.Help
                        },
                        contentDescription = null,
                        tint = qualityColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${quality.displayName} (${(confidence * 100).toInt()}%)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = qualityColor
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "关闭", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            Text(text = quality.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun hasQuickInfoContent(objectInfo: ObjectInfo): Boolean {
    return objectInfo.brand.hasContent() || objectInfo.model.hasContent() ||
            objectInfo.species.hasContent() || objectInfo.material.hasContent() ||
            objectInfo.color.hasContent() || objectInfo.priceRange.hasContent() ||
            objectInfo.manufacturer.hasContent() ||
            objectInfo.typeSpecificInfo.any { it.value.isNotBlank() && it.value.length < 50 }
}

@Composable
private fun HeaderSection(
    objectInfo: ObjectInfo,
    capturedBitmap: Bitmap?,
    quality: RecognitionQuality,
    onDismiss: () -> Unit,
    onQualityClick: () -> Unit
) {
    val qualityColor = Color(quality.color)
    
    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        if (capturedBitmap != null) {
            Image(capturedBitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.7f)))
            ))
        } else {
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFA855F7)))
            )) {
                Box(modifier = Modifier.align(Alignment.Center).size(120.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(60.dp)))
                Icon(
                    imageVector = objectInfo.objectType.getIcon(),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(64.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)) {
                Icon(Icons.Default.Close, "关闭", tint = Color.White)
            }
            Surface(onClick = onQualityClick, shape = RoundedCornerShape(16.dp), color = qualityColor.copy(alpha = 0.2f)) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = when (quality) {
                            RecognitionQuality.HIGH -> Icons.Default.CheckCircle
                            RecognitionQuality.MEDIUM -> Icons.Default.Check
                            RecognitionQuality.LOW -> Icons.Default.Warning
                            RecognitionQuality.UNCERTAIN -> Icons.Default.Help
                        },
                        contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White
                    )
                    Text("${(objectInfo.confidence * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
        
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = objectInfo.objectType.getIcon(),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Text(objectInfo.category.ifBlank { "未分类" }, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(objectInfo.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                EnhancedConfidenceIndicator(objectInfo.confidence, quality)
                Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.2f)) {
                    Text(objectInfo.source.displayName, style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun EnhancedConfidenceIndicator(confidence: Float, quality: RecognitionQuality) {
    val qualityColor = Color(quality.color)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.3f))) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(confidence).background(qualityColor, RoundedCornerShape(3.dp)))
        }
        Text("${(confidence * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SummaryCard(summary: String) {
    val isExpandable = summary.length >= 80
    var expanded by remember { mutableStateOf(!isExpandable) }
    
    Card(
        modifier = Modifier.fillMaxWidth().then(if (isExpandable) Modifier.clickable { expanded = !expanded } else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("简介", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                if (isExpandable) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "收起" else "展开", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (isExpandable) {
                AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
                }
                if (!expanded) {
                    Text(summary.take(60) + "...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
            }
        }
    }
}

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
        objectInfo.typeSpecificInfo.forEach { (key, value) ->
            if (value.isNotBlank() && value.length < 50) add(key to value)
        }
    }
    if (infoItems.isEmpty()) return
    
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(infoItems) { (label, value) -> QuickInfoCard(label, value) }
    }
}

@Composable
private fun QuickInfoCard(label: String, value: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.width(100.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DetailSection(objectInfo: ObjectInfo) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (objectInfo.aliases.isNotEmpty()) DetailItem(Icons.Outlined.Label, "别名", objectInfo.aliases.joinToString("、"))
        if (objectInfo.origin.hasContent() && objectInfo.origin != "暂无信息") DetailItem(Icons.Outlined.History, "来历", objectInfo.origin)
        if (objectInfo.usage.hasContent() && objectInfo.usage != "暂无信息") DetailItem(Icons.Outlined.Lightbulb, "用途", objectInfo.usage)
        if (objectInfo.description.hasContent()) DetailItem(Icons.Outlined.Description, "详细介绍", objectInfo.description!!)
        if (objectInfo.features.isNotEmpty()) DetailItem(Icons.Outlined.Checklist, "特征", objectInfo.features.joinToString("\n• ", prefix = "• "))
    }
}

@Composable
private fun DetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, content: String) {
    val isExpandable = content.length >= 100
    var expanded by remember { mutableStateOf(!isExpandable) }
    
    Card(
        modifier = Modifier.fillMaxWidth().then(if (isExpandable) Modifier.clickable { expanded = !expanded } else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                if (isExpandable) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, if (expanded) "收起" else "展开", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (isExpandable) {
                AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Text(content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
                }
                if (!expanded) Text(content.take(80) + "...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            } else {
                Text(content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
            }
        }
    }
}

@Composable
private fun FunFactsSection(facts: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                Text("知识卡片", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            facts.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp, modifier = Modifier.padding(vertical = 4.dp)) }
        }
    }
}

@Composable
private fun TipsSection(tips: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Text("小贴士", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            tips.forEachIndexed { index, tip ->
                Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                    Text(tip, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun String?.hasContent(): Boolean = !this.isNullOrBlank() && this.lowercase() != "null" && this != "暂无信息"
