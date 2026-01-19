package com.ruolijianzhen.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruolijianzhen.app.domain.model.RecognitionAssessment
import com.ruolijianzhen.app.domain.model.RecognitionQuality
import com.ruolijianzhen.app.domain.model.RecognitionSource

/**
 * 识别质量指示器
 * 显示识别结果的可信度等级
 */
@Composable
fun QualityIndicator(
    confidence: Float,
    source: RecognitionSource,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true
) {
    val assessment = remember(confidence, source) {
        RecognitionAssessment.assess(confidence, source)
    }
    
    val quality = assessment.quality
    val qualityColor = Color(quality.color)
    
    // 动画
    val animatedConfidence by animateFloatAsState(
        targetValue = confidence,
        animationSpec = tween(500),
        label = "confidence"
    )
    
    val animatedColor by animateColorAsState(
        targetValue = qualityColor,
        animationSpec = tween(300),
        label = "color"
    )
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 主指示器
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 质量图标
            QualityIcon(quality = quality, color = animatedColor)
            
            // 进度条和百分比
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quality.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = animatedColor
                    )
                    
                    Text(
                        text = "${(animatedConfidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = animatedColor
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 进度条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedConfidence)
                            .background(animatedColor, RoundedCornerShape(3.dp))
                    )
                }
            }
        }
        
        // 详细信息
        if (showDetails && assessment.suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            
            assessment.suggestions.forEach { suggestion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * 紧凑型质量指示器
 * 用于列表项或小空间
 */
@Composable
fun CompactQualityIndicator(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val quality = RecognitionQuality.fromConfidence(confidence)
    val qualityColor = Color(quality.color)
    val percentage = (confidence * 100).toInt()
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 小进度条
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(confidence)
                    .background(qualityColor, RoundedCornerShape(2.dp))
            )
        }
        
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.labelSmall,
            color = qualityColor,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 质量徽章
 * 用于结果卡片角落
 */
@Composable
fun QualityBadge(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val quality = RecognitionQuality.fromConfidence(confidence)
    val qualityColor = Color(quality.color)
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = qualityColor.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = getQualityIcon(quality),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = qualityColor
            )
            Text(
                text = quality.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = qualityColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 质量图标
 */
@Composable
private fun QualityIcon(
    quality: RecognitionQuality,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(36.dp),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = getQualityIcon(quality),
                contentDescription = quality.displayName,
                modifier = Modifier.size(20.dp),
                tint = color
            )
        }
    }
}

/**
 * 获取质量等级对应的图标
 */
private fun getQualityIcon(quality: RecognitionQuality): ImageVector {
    return when (quality) {
        RecognitionQuality.HIGH -> Icons.Default.CheckCircle
        RecognitionQuality.MEDIUM -> Icons.Default.Check
        RecognitionQuality.LOW -> Icons.Default.Warning
        RecognitionQuality.UNCERTAIN -> Icons.Default.Help
    }
}

/**
 * 可信度说明卡片
 * 用于帮助用户理解识别结果
 */
@Composable
fun ConfidenceExplanationCard(
    confidence: Float,
    source: RecognitionSource,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val assessment = RecognitionAssessment.assess(confidence, source)
    val quality = assessment.quality
    val qualityColor = Color(quality.color)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = qualityColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = getQualityIcon(quality),
                        contentDescription = null,
                        tint = qualityColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = quality.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = qualityColor
                    )
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = quality.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (assessment.suggestions.isNotEmpty()) {
                Divider(color = qualityColor.copy(alpha = 0.2f))
                
                Text(
                    text = "建议",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                assessment.suggestions.forEach { suggestion ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "•",
                            color = qualityColor
                        )
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}