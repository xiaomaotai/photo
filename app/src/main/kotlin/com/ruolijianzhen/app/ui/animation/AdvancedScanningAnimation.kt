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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ruolijianzhen.app.util.DevicePerformanceUtils
import kotlin.math.*
import kotlin.random.Random

/**
 * 高级扫描动画组件
 * 提供粒子效果、光波扩散、AI风格动画等炫酷效果
 *
 * 优化：
 * - 更流畅的动画过渡
 * - 更炫酷的视觉效果
 * - 性能优化（低端机简化效果）
 */
@Composable
fun AdvancedScanningAnimation(
    state: ScanningAnimationState,
    modifier: Modifier = Modifier,
    reduceAnimations: Boolean = false
) {
    // 如果没有传入，自动检测设备性能
    val context = LocalContext.current
    val shouldReduce = remember(reduceAnimations) {
        reduceAnimations || DevicePerformanceUtils.shouldReduceAnimations(context)
    }

    when (state) {
        ScanningAnimationState.Idle -> { /* 空闲不显示 */ }
        ScanningAnimationState.Scanning -> ParticleGatherAnimation(modifier = modifier, reduceAnimations = shouldReduce)
        ScanningAnimationState.Processing -> AIProcessingAnimation(modifier = modifier, reduceAnimations = shouldReduce)
        ScanningAnimationState.Success -> SuccessExplosionAnimation(modifier = modifier, reduceAnimations = shouldReduce)
        ScanningAnimationState.Error -> ErrorRippleAnimation(modifier = modifier, reduceAnimations = shouldReduce)
    }
}

private data class Particle(
    val id: Int,
    val angle: Float,
    val distance: Float,
    val size: Float,
    val speed: Float,
    val color: Color
)

/**
 * 粒子聚集动画 - 扫描状态
 * @param reduceAnimations 低端机模式：减少粒子数量
 */
@Composable
private fun ParticleGatherAnimation(
    modifier: Modifier = Modifier,
    reduceAnimations: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gather")

    val gatherProgress by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "gatherProgress"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // 低端机减少粒子数量：40→12
    val particleCount = if (reduceAnimations) 12 else 40

    val particles = remember(particleCount) {
        List(particleCount) { i ->
            val angleStep = 360f / particleCount
            Particle(i, (i * angleStep) + Random.nextFloat() * 10f, 0.5f + Random.nextFloat() * 0.5f,
                3f + Random.nextFloat() * 5f, 0.8f + Random.nextFloat() * 0.4f,
                when (i % 3) { 0 -> primaryColor; 1 -> secondaryColor; else -> tertiaryColor })
        }
    }
    
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2
            
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(primaryColor.copy(alpha = 0.4f * (1 - gatherProgress)),
                        primaryColor.copy(alpha = 0.1f * (1 - gatherProgress)), Color.Transparent),
                    center = center, radius = maxRadius * 0.4f * pulse
                ), radius = maxRadius * 0.4f * pulse, center = center
            )
            
            particles.forEach { particle ->
                val adjustedProgress = (gatherProgress * particle.speed).coerceIn(0f, 1f)
                val currentDistance = maxRadius * particle.distance * adjustedProgress
                val angleRad = Math.toRadians((particle.angle + rotation).toDouble())
                val x = center.x + cos(angleRad).toFloat() * currentDistance
                val y = center.y + sin(angleRad).toFloat() * currentDistance
                val alpha = (1f - adjustedProgress * 0.5f).coerceIn(0.3f, 1f)
                
                drawCircle(particle.color.copy(alpha = alpha * 0.3f), particle.size * 2, Offset(x, y))
                drawCircle(particle.color.copy(alpha = alpha), particle.size, Offset(x, y))
            }
            
            val centerGlow = (1 - gatherProgress).coerceIn(0f, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = centerGlow * 0.8f),
                        primaryColor.copy(alpha = centerGlow * 0.5f), Color.Transparent),
                    center = center, radius = 30.dp.toPx() * pulse
                ), radius = 30.dp.toPx() * pulse, center = center
            )
        }
    }
}

/**
 * AI处理动画 - 识别中状态
 * 炫酷的科技感动画效果
 * @param reduceAnimations 低端机模式：简化部分效果
 */
@Composable
private fun AIProcessingAnimation(
    modifier: Modifier = Modifier,
    reduceAnimations: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai")
    
    // 扫描线位置
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "scanProgress"
    )
    
    // 外圈旋转
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "outerRotation"
    )
    
    // 内圈反向旋转
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "innerRotation"
    )
    
    // 数据流动画
    val dataFlow by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Restart),
        label = "dataFlow"
    )
    
    // 脉冲效果
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    
    // 光环扩散
    val haloExpand by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "haloExpand"
    )
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(320.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val frameSize = size.minDimension * 0.75f

            // 基础效果（所有设备）
            // 绘制扩散光环
            drawExpandingHalo(center, frameSize, haloExpand, primaryColor)

            // 绘制科技感边框
            drawTechFrame(center, frameSize, outerRotation, primaryColor, secondaryColor)

            // 绘制扫描线
            drawScanLine(center, frameSize, scanProgress, primaryColor)

            // 绘制AI核心
            drawAICore(center, frameSize * 0.15f * pulse, primaryColor, tertiaryColor)

            // 高端机额外效果
            if (!reduceAnimations) {
                // 绘制内圈
                drawInnerRings(center, frameSize * 0.6f, innerRotation, primaryColor, tertiaryColor)

                // 绘制数据流点
                drawDataFlowPoints(center, frameSize * 0.4f, dataFlow, primaryColor, secondaryColor)

                // 绘制神经网络连接
                drawNeuralConnections(center, frameSize * 0.35f, dataFlow, primaryColor)

                // 绘制能量粒子
                drawEnergyParticles(center, frameSize * 0.5f, dataFlow, primaryColor, secondaryColor)
            }
        }
    }
}

/**
 * 绘制扩散光环
 */
private fun DrawScope.drawExpandingHalo(center: Offset, frameSize: Float, progress: Float, color: Color) {
    val alpha = (1f - progress) * 0.3f
    val radius = frameSize * 0.4f + frameSize * 0.3f * progress
    
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
    
    // 第二层光环（延迟）
    val progress2 = (progress + 0.5f) % 1f
    val alpha2 = (1f - progress2) * 0.2f
    val radius2 = frameSize * 0.4f + frameSize * 0.3f * progress2
    
    drawCircle(
        color = color.copy(alpha = alpha2),
        radius = radius2,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )
}

/**
 * 绘制科技感边框
 */
private fun DrawScope.drawTechFrame(center: Offset, frameSize: Float, rotation: Float, primaryColor: Color, secondaryColor: Color) {
    val cornerLength = frameSize * 0.2f
    val strokeWidth = 3.dp.toPx()
    val halfFrame = frameSize / 2
    
    rotate(rotation, pivot = center) {
        val gradientBrush = Brush.sweepGradient(
            listOf(primaryColor, secondaryColor, primaryColor.copy(alpha = 0.3f), secondaryColor.copy(alpha = 0.3f), primaryColor),
            center = center
        )
        
        // 四个角的线条
        listOf(
            Pair(Offset(center.x - halfFrame, center.y - halfFrame + cornerLength), Offset(center.x - halfFrame, center.y - halfFrame)),
            Pair(Offset(center.x - halfFrame, center.y - halfFrame), Offset(center.x - halfFrame + cornerLength, center.y - halfFrame)),
            Pair(Offset(center.x + halfFrame - cornerLength, center.y - halfFrame), Offset(center.x + halfFrame, center.y - halfFrame)),
            Pair(Offset(center.x + halfFrame, center.y - halfFrame), Offset(center.x + halfFrame, center.y - halfFrame + cornerLength)),
            Pair(Offset(center.x + halfFrame, center.y + halfFrame - cornerLength), Offset(center.x + halfFrame, center.y + halfFrame)),
            Pair(Offset(center.x + halfFrame, center.y + halfFrame), Offset(center.x + halfFrame - cornerLength, center.y + halfFrame)),
            Pair(Offset(center.x - halfFrame + cornerLength, center.y + halfFrame), Offset(center.x - halfFrame, center.y + halfFrame)),
            Pair(Offset(center.x - halfFrame, center.y + halfFrame), Offset(center.x - halfFrame, center.y + halfFrame - cornerLength))
        ).forEach { (start, end) ->
            drawLine(gradientBrush, start, end, strokeWidth, StrokeCap.Round)
        }
        
        // 四个方向的装饰点
        listOf(
            Offset(center.x, center.y - halfFrame), Offset(center.x + halfFrame, center.y),
            Offset(center.x, center.y + halfFrame), Offset(center.x - halfFrame, center.y)
        ).forEach { 
            drawCircle(primaryColor, 4.dp.toPx(), it) 
            drawCircle(primaryColor.copy(alpha = 0.3f), 8.dp.toPx(), it)
        }
    }
}

/**
 * 绘制内圈
 */
private fun DrawScope.drawInnerRings(center: Offset, radius: Float, rotation: Float, primaryColor: Color, tertiaryColor: Color) {
    rotate(rotation, pivot = center) {
        // 分段圆弧
        for (i in 0 until 24) {
            if (i % 2 == 0) {
                val color = if (i % 4 == 0) primaryColor else tertiaryColor
                drawArc(color.copy(alpha = 0.6f), i * 15f, 8f, false,
                    Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2),
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
    
    // 内圈虚线
    drawCircle(primaryColor.copy(alpha = 0.3f), radius * 0.7f, center, style = Stroke(1.dp.toPx()))
    drawCircle(tertiaryColor.copy(alpha = 0.2f), radius * 0.5f, center, style = Stroke(1.dp.toPx()))
}

/**
 * 绘制扫描线
 */
private fun DrawScope.drawScanLine(center: Offset, frameSize: Float, progress: Float, color: Color) {
    val halfFrame = frameSize / 2
    val scanY = center.y - halfFrame + frameSize * progress
    
    // 主扫描线
    drawLine(
        Brush.horizontalGradient(
            listOf(Color.Transparent, color.copy(alpha = 0.8f), color, color.copy(alpha = 0.8f), Color.Transparent),
            center.x - halfFrame, center.x + halfFrame
        ),
        Offset(center.x - halfFrame * 0.8f, scanY), Offset(center.x + halfFrame * 0.8f, scanY), 3.dp.toPx()
    )
    
    // 扫描线光晕
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, color.copy(alpha = 0.1f), color.copy(alpha = 0.2f), color.copy(alpha = 0.1f), Color.Transparent),
            scanY - 50.dp.toPx(), scanY + 20.dp.toPx()
        ),
        Offset(center.x - halfFrame * 0.8f, scanY - 50.dp.toPx()), Size(halfFrame * 1.6f, 70.dp.toPx())
    )
    
    // 扫描线两端的发光点
    drawCircle(color.copy(alpha = 0.8f), 4.dp.toPx(), Offset(center.x - halfFrame * 0.8f, scanY))
    drawCircle(color.copy(alpha = 0.8f), 4.dp.toPx(), Offset(center.x + halfFrame * 0.8f, scanY))
}

/**
 * 绘制数据流点
 */
private fun DrawScope.drawDataFlowPoints(center: Offset, radius: Float, progress: Float, primaryColor: Color, secondaryColor: Color) {
    for (i in 0 until 8) {
        val adjustedProgress = (progress + i * 0.1f) % 1f
        val currentRadius = radius * (0.5f + adjustedProgress * 0.5f)
        val angleRad = Math.toRadians((i * 45.0))
        val x = center.x + cos(angleRad).toFloat() * currentRadius
        val y = center.y + sin(angleRad).toFloat() * currentRadius
        val alpha = (1f - adjustedProgress).coerceIn(0.2f, 1f)
        val pointColor = if (i % 2 == 0) primaryColor else secondaryColor
        
        drawCircle(pointColor.copy(alpha = alpha * 0.3f), 10.dp.toPx(), Offset(x, y))
        drawCircle(pointColor.copy(alpha = alpha), 5.dp.toPx(), Offset(x, y))
    }
}

/**
 * 绘制AI核心
 */
private fun DrawScope.drawAICore(center: Offset, radius: Float, primaryColor: Color, tertiaryColor: Color) {
    // 外层光晕
    drawCircle(
        Brush.radialGradient(listOf(primaryColor.copy(alpha = 0.5f), primaryColor.copy(alpha = 0.2f), Color.Transparent), center, radius * 2.5f),
        radius * 2.5f, center
    )
    
    // 中层渐变
    drawCircle(
        Brush.radialGradient(listOf(tertiaryColor.copy(alpha = 0.6f), primaryColor.copy(alpha = 0.4f), Color.Transparent), center, radius * 1.5f),
        radius * 1.5f, center
    )
    
    // 核心
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.95f), primaryColor.copy(alpha = 0.8f), primaryColor.copy(alpha = 0.5f)), center, radius),
        radius, center
    )
    
    // 核心边框
    drawCircle(Color.White.copy(alpha = 0.9f), radius, center, style = Stroke(2.dp.toPx()))
}

/**
 * 绘制神经网络连接
 */
private fun DrawScope.drawNeuralConnections(center: Offset, radius: Float, progress: Float, color: Color) {
    val nodes = List(6) { i ->
        val angle = Math.toRadians((i * 60.0) + 30)
        Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
    }
    
    nodes.forEachIndexed { i, node ->
        val lineAlpha = (sin(((progress * 2 + i * 0.15f) % 1f) * PI).toFloat() * 0.6f).coerceIn(0.1f, 0.6f)
        
        // 连接到中心
        drawLine(color.copy(alpha = lineAlpha), center, node, 1.5.dp.toPx())
        
        // 连接到相邻节点
        drawLine(color.copy(alpha = lineAlpha * 0.5f), node, nodes[(i + 1) % 6], 1.dp.toPx())
        
        // 节点
        drawCircle(color.copy(alpha = 0.3f), 8.dp.toPx(), node)
        drawCircle(color.copy(alpha = 0.7f + lineAlpha * 0.3f), 4.dp.toPx(), node)
    }
}

/**
 * 绘制能量粒子
 */
private fun DrawScope.drawEnergyParticles(center: Offset, radius: Float, progress: Float, primaryColor: Color, secondaryColor: Color) {
    for (i in 0 until 12) {
        val particleProgress = (progress + i * 0.08f) % 1f
        val angle = Math.toRadians((i * 30.0) + progress * 360)
        val distance = radius * (0.3f + particleProgress * 0.7f)
        val x = center.x + cos(angle).toFloat() * distance
        val y = center.y + sin(angle).toFloat() * distance
        val alpha = (1f - particleProgress).coerceIn(0f, 0.8f)
        val particleSize = (3f - particleProgress * 2f).coerceIn(1f, 3f)
        val color = if (i % 2 == 0) primaryColor else secondaryColor
        
        drawCircle(color.copy(alpha = alpha), particleSize.dp.toPx(), Offset(x, y))
    }
}

/**
 * 成功爆炸动画
 * @param reduceAnimations 低端机模式：减少粒子数量
 */
@Composable
private fun SuccessExplosionAnimation(
    modifier: Modifier = Modifier,
    reduceAnimations: Boolean = false
) {
    var visible by remember { mutableStateOf(true) }

    val explosionProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing), label = "explosion"
    )
    val scaleProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "scale"
    )

    LaunchedEffect(Unit) { kotlinx.coroutines.delay(2000); visible = false }

    val successColor = Color(0xFF4CAF50)

    // 低端机减少粒子数量：24→8
    val particleCount = if (reduceAnimations) 8 else 24

    val particles = remember(particleCount) {
        val angleStep = 360f / particleCount
        List(particleCount) { i -> Particle(i, i * angleStep + Random.nextFloat() * 10f, 0.5f + Random.nextFloat() * 0.5f,
            3f + Random.nextFloat() * 5f, 0.7f + Random.nextFloat() * 0.5f, successColor) }
    }
    
    if (scaleProgress > 0.01f) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(220.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.minDimension / 2
                
                // 粒子爆炸
                particles.forEach { particle ->
                    val currentDistance = maxRadius * particle.distance * explosionProgress * particle.speed
                    val angleRad = Math.toRadians(particle.angle.toDouble())
                    val x = center.x + cos(angleRad).toFloat() * currentDistance
                    val y = center.y + sin(angleRad).toFloat() * currentDistance
                    val alpha = (1f - explosionProgress * 0.7f).coerceIn(0f, 1f)
                    
                    drawCircle(particle.color.copy(alpha = alpha * 0.4f), particle.size * 2.5f * scaleProgress, Offset(x, y))
                    drawCircle(particle.color.copy(alpha = alpha), particle.size * scaleProgress, Offset(x, y))
                }
                
                // 中心光晕
                drawCircle(
                    Brush.radialGradient(listOf(successColor.copy(alpha = 0.4f * scaleProgress),
                        successColor.copy(alpha = 0.15f * scaleProgress), Color.Transparent), center, maxRadius * 0.6f * scaleProgress),
                    maxRadius * 0.6f * scaleProgress, center
                )
                
                // 成功圆环
                drawCircle(successColor.copy(alpha = scaleProgress), maxRadius * 0.35f * scaleProgress, center, style = Stroke(4.dp.toPx()))
            }
            
            Icon(Icons.Default.Check, "成功", Modifier.size((60 * scaleProgress).dp), successColor.copy(alpha = scaleProgress))
        }
    }
}

/**
 * 错误波纹动画
 * @param reduceAnimations 低端机模式：减少波纹数量
 */
@Composable
private fun ErrorRippleAnimation(
    modifier: Modifier = Modifier,
    reduceAnimations: Boolean = false
) {
    var visible by remember { mutableStateOf(true) }
    val infiniteTransition = rememberInfiniteTransition(label = "errorRipple")

    // 低端机减少波纹数量：3→1
    val rippleDelays = if (reduceAnimations) listOf(0) else listOf(0, 250, 500)

    val ripples = rippleDelays.map { delay ->
        infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing, delayMillis = delay), RepeatMode.Restart),
            label = "ripple$delay"
        )
    }
    
    val shake by infiniteTransition.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(60), RepeatMode.Reverse), label = "shake"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f, animationSpec = tween(300), label = "alpha"
    )
    
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(1500); visible = false }
    
    val errorColor = Color(0xFFF44336)
    
    if (alpha > 0.01f) {
        Box(modifier = modifier.fillMaxSize().offset(x = shake.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.minDimension / 2
                
                ripples.forEach { ripple ->
                    val progress = ripple.value
                    val rippleAlpha = (1f - progress) * alpha
                    drawCircle(errorColor.copy(alpha = rippleAlpha * 0.25f), maxRadius * progress, center)
                    drawCircle(errorColor.copy(alpha = rippleAlpha), maxRadius * progress, center, style = Stroke(2.5.dp.toPx()))
                }
                
                drawCircle(
                    Brush.radialGradient(listOf(errorColor.copy(alpha = 0.5f * alpha), errorColor.copy(alpha = 0.15f * alpha), Color.Transparent), center, maxRadius * 0.4f),
                    maxRadius * 0.4f, center
                )
                drawCircle(errorColor.copy(alpha = alpha), maxRadius * 0.3f, center, style = Stroke(3.dp.toPx()))
            }
            
            Icon(Icons.Default.Close, "失败", Modifier.size(52.dp), errorColor.copy(alpha = alpha))
        }
    }
}