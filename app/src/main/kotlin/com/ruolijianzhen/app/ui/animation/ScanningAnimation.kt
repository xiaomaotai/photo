package com.ruolijianzhen.app.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * 扫描动画状态
 */
sealed class ScanningAnimationState {
    object Idle : ScanningAnimationState()
    object Scanning : ScanningAnimationState()
    object Processing : ScanningAnimationState()
    object Success : ScanningAnimationState()
    object Error : ScanningAnimationState()
}

/**
 * 扫描动画组件
 * 提供脉冲圆环、扫描线、旋转边框等动画效果
 */
@Composable
fun ScanningAnimation(
    state: ScanningAnimationState,
    modifier: Modifier = Modifier
) {
    when (state) {
        ScanningAnimationState.Idle -> {
            // 空闲状态不显示任何动画
        }
        ScanningAnimationState.Scanning -> {
            PulseRingAnimation(modifier = modifier)
        }
        ScanningAnimationState.Processing -> {
            ProcessingAnimation(modifier = modifier)
        }
        ScanningAnimationState.Success -> {
            SuccessAnimation(modifier = modifier)
        }
        ScanningAnimationState.Error -> {
            ErrorAnimation(modifier = modifier)
        }
    }
}

/**
 * 脉冲圆环动画（Scanning状态）
 * 从中心向外扩散的圆环，透明度渐变消失
 */
@Composable
private fun PulseRingAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // 多个圆环，错开时间
    val rings = listOf(0f, 0.33f, 0.66f)
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        rings.forEach { delay ->
            val progress by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 2000,
                        easing = LinearEasing,
                        delayMillis = (delay * 2000).toInt()
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ringProgress$delay"
            )
            
            val primaryColor = MaterialTheme.colorScheme.primary
            
            Canvas(modifier = Modifier.size(200.dp)) {
                val radius = size.minDimension / 2 * progress
                val alpha = (1f - progress).coerceIn(0f, 1f)
                
                drawCircle(
                    color = primaryColor.copy(alpha = alpha * 0.6f),
                    radius = radius,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }
    }
}

/**
 * 处理中动画（Processing状态）
 * 扫描线 + 旋转边框
 */
@Composable
private fun ProcessingAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "processing")
    
    // 扫描线位置
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )
    
    // 边框旋转角度
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val frameSize = size.minDimension * 0.8f
            val cornerLength = frameSize * 0.15f
            val strokeWidth = 4.dp.toPx()
            
            // 旋转的渐变边框（四个角）
            rotate(rotation, pivot = Offset(centerX, centerY)) {
                val gradientBrush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor,
                        tertiaryColor,
                        primaryColor.copy(alpha = 0.3f),
                        tertiaryColor.copy(alpha = 0.3f),
                        primaryColor
                    ),
                    center = Offset(centerX, centerY)
                )
                
                // 左上角
                drawLine(
                    brush = gradientBrush,
                    start = Offset(centerX - frameSize / 2, centerY - frameSize / 2 + cornerLength),
                    end = Offset(centerX - frameSize / 2, centerY - frameSize / 2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = gradientBrush,
                    start = Offset(centerX - frameSize / 2, centerY - frameSize / 2),
                    end = Offset(centerX - frameSize / 2 + cornerLength, centerY - frameSize / 2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                
                // 右上角
                drawLine(
                    brush = gradientBrush,
                    start = Offset(centerX + frameSize / 2 - cornerLength, centerY - frameSize / 2),
                    end = Offset(centerX + frameSize / 2, centerY - frameSize / 2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = gradientBrush,
                    start = Offset(centerX + frameSize / 2, centerY - frameSize / 2),
                    end = Offset(centerX + frameSize / 2, centerY - frameSize / 2 + cornerLength),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                
                // 右下角
                drawLine(
                    brush = gradientBrush,
                    start = Offset(centerX + frameSize / 2, centerY + frameSize / 2 - cornerLength),
                    end = Offset(centerX + frameSize / 2, centerY + frameSize / 2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = gradientBrush,
                    start = Offset(centerX + frameSize / 2, centerY + frameSize / 2),
                    end = Offset(centerX + frameSize / 2 - cornerLength, centerY + frameSize / 2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                
                // 左下角
                drawLine(
                    brush = gradientBrush,
                    start = Offset(centerX - frameSize / 2 + cornerLength, centerY + frameSize / 2),
                    end = Offset(centerX - frameSize / 2, centerY + frameSize / 2),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = gradientBrush,
                    start = Offset(centerX - frameSize / 2, centerY + frameSize / 2),
                    end = Offset(centerX - frameSize / 2, centerY + frameSize / 2 - cornerLength),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            
            // 扫描线
            val scanY = centerY - frameSize / 2 + frameSize * scanLineProgress
            val scanLineGradient = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    primaryColor.copy(alpha = 0.8f),
                    primaryColor,
                    primaryColor.copy(alpha = 0.8f),
                    Color.Transparent
                ),
                startX = centerX - frameSize / 2,
                endX = centerX + frameSize / 2
            )
            
            drawLine(
                brush = scanLineGradient,
                start = Offset(centerX - frameSize / 2, scanY),
                end = Offset(centerX + frameSize / 2, scanY),
                strokeWidth = 2.dp.toPx()
            )
            
            // 扫描线光晕
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.1f),
                        primaryColor.copy(alpha = 0.2f),
                        primaryColor.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    startY = scanY - 30.dp.toPx(),
                    endY = scanY + 10.dp.toPx()
                ),
                topLeft = Offset(centerX - frameSize / 2, scanY - 30.dp.toPx()),
                size = Size(frameSize, 40.dp.toPx())
            )
        }
    }
}

/**
 * 成功动画（Success状态）
 * 绿色对勾 + 缩放弹出 + 淡出
 */
@Composable
private fun SuccessAnimation(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(true) }
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "successScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "successAlpha"
    )
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        visible = false
    }
    
    if (scale > 0.01f) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(100.dp)
            ) {
                drawCircle(
                    color = Color(0xFF4CAF50).copy(alpha = alpha * 0.2f),
                    radius = size.minDimension / 2 * scale
                )
                drawCircle(
                    color = Color(0xFF4CAF50).copy(alpha = alpha),
                    radius = size.minDimension / 2 * scale,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
            
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "成功",
                modifier = Modifier.size((48 * scale).dp),
                tint = Color(0xFF4CAF50).copy(alpha = alpha)
            )
        }
    }
}

/**
 * 错误动画（Error状态）
 * 红色脉冲 + 轻微震动 + 淡出
 */
@Composable
private fun ErrorAnimation(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(true) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "errorPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(50),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "errorAlpha"
    )
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000)
        visible = false
    }
    
    if (alpha > 0.01f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .offset(x = offsetX.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size((100 * pulse).dp)
            ) {
                drawCircle(
                    color = Color(0xFFF44336).copy(alpha = alpha * 0.2f),
                    radius = size.minDimension / 2
                )
                drawCircle(
                    color = Color(0xFFF44336).copy(alpha = alpha),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
            
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "失败",
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFF44336).copy(alpha = alpha)
            )
        }
    }
}
