package com.ruolijianzhen.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruolijianzhen.app.domain.model.RecognitionProgress
import com.ruolijianzhen.app.domain.model.RecognitionStage

/**
 * 识别进度指示器
 * 显示当前识别阶段和进度
 */
@Composable
fun RecognitionProgressIndicator(
    progress: RecognitionProgress?,
    modifier: Modifier = Modifier
) {
    if (progress == null || progress.isCompleted || progress.isFailed) {
        return
    }
    
    // 动画
    val infiniteTransition = rememberInfiniteTransition(label = "progress")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    
    // 进度条动画
    val animatedProgress by animateFloatAsState(
        targetValue = progress.stage.progress,
        animationSpec = tween(300, easing = EaseInOutCubic),
        label = "progressBar"
    )
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.7f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(min = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 阶段图标和名称
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 阶段图标
                Text(
                    text = getStageIcon(progress.stage),
                    fontSize = 20.sp
                )
                
                // 阶段名称
                Text(
                    text = progress.stage.displayName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                // 动态省略号
                Text(
                    text = "...",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.alpha(dotAlpha)
                )
            }
            
            // 进度条
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = getStageColor(progress.stage),
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            
            // 详细描述
            Text(
                text = progress.message,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
            
            // 进度百分比
            Text(
                text = "${progress.progressPercent}%",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
    }
}

/**
 * 紧凑型进度指示器
 * 用于底部按钮区域
 */
@Composable
fun CompactProgressIndicator(
    progress: RecognitionProgress?,
    modifier: Modifier = Modifier
) {
    if (progress == null || progress.isCompleted || progress.isFailed) {
        return
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "compact")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot"
    )
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 小型进度环
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = getStageColor(progress.stage),
            strokeWidth = 2.dp
        )
        
        // 阶段名称
        Text(
            text = progress.stage.displayName,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "...",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.alpha(dotAlpha)
        )
    }
}

/**
 * 步骤指示器
 * 显示所有识别步骤的状态
 */
@Composable
fun StepProgressIndicator(
    progress: RecognitionProgress?,
    modifier: Modifier = Modifier
) {
    if (progress == null) return
    
    val stages = listOf(
        RecognitionStage.PREPARING,
        RecognitionStage.OFFLINE_RECOGNITION,
        RecognitionStage.API_RECOGNITION,
        RecognitionStage.KNOWLEDGE_ENHANCEMENT
    )
    
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stages.forEachIndexed { index, stage ->
            val isCompleted = progress.stage.progress > stage.progress
            val isCurrent = progress.stage == stage
            val isPending = progress.stage.progress < stage.progress
            
            // 步骤点
            StepDot(
                isCompleted = isCompleted,
                isCurrent = isCurrent,
                isPending = isPending,
                color = getStageColor(stage)
            )
            
            // 连接线（最后一个不显示）
            if (index < stages.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(
                            if (isCompleted) getStageColor(stage).copy(alpha = 0.8f)
                            else Color.White.copy(alpha = 0.2f)
                        )
                )
            }
        }
    }
}

@Composable
private fun StepDot(
    isCompleted: Boolean,
    isCurrent: Boolean,
    isPending: Boolean,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stepDot")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCurrent) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size((8 * scale).dp)
            .background(
                color = when {
                    isCompleted -> color
                    isCurrent -> color.copy(alpha = 0.8f)
                    else -> Color.White.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(50)
            )
    )
}

/**
 * 获取阶段图标
 */
private fun getStageIcon(stage: RecognitionStage): String {
    return when (stage) {
        RecognitionStage.PREPARING -> "📷"
        RecognitionStage.OFFLINE_RECOGNITION -> "🔍"
        RecognitionStage.API_RECOGNITION -> "☁️"
        RecognitionStage.AI_RECOGNITION -> "🤖"
        RecognitionStage.KNOWLEDGE_ENHANCEMENT -> "📚"
        RecognitionStage.COMPLETED -> "✅"
        RecognitionStage.FAILED -> "❌"
    }
}

/**
 * 获取阶段颜色
 */
private fun getStageColor(stage: RecognitionStage): Color {
    return when (stage) {
        RecognitionStage.PREPARING -> Color(0xFF2196F3)
        RecognitionStage.OFFLINE_RECOGNITION -> Color(0xFF4CAF50)
        RecognitionStage.API_RECOGNITION -> Color(0xFF9C27B0)
        RecognitionStage.AI_RECOGNITION -> Color(0xFFFF9800)
        RecognitionStage.KNOWLEDGE_ENHANCEMENT -> Color(0xFF00BCD4)
        RecognitionStage.COMPLETED -> Color(0xFF4CAF50)
        RecognitionStage.FAILED -> Color(0xFFF44336)
    }
}