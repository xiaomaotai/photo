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
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlin.random.Random

/**
 * 高级扫描动画组件
 * 提供粒子效果、光波扩散、AI风格动画等炫酷效果
 */
@Composable
fun AdvancedScanningAnimation(
    state: ScanningAnimationState,
    modifier: Modifier = Modifier
) {
    when (state) {
        ScanningAnimationState.Idle -> { /* 空闲不显示 */ }
        ScanningAnimationState.Scanning -> ParticleGatherAnimation(modifier = modifier)
        ScanningAnimationState.Processing -> AIProcessingAnimation(modifier = modifier)
        ScanningAnimationState.Success -> SuccessExplosionAnimation(modifier = modifier)
        ScanningAnimationState.Error -> ErrorRippleAnimation(modifier = modifier)
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

@Composable
private fun ParticleGatherAnimation(modifier: Modifier = Modifier) {
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
    
    val particles = remember {
        List(40) { i ->
            Particle(i, (i * 9f) + Random.nextFloat() * 10f, 0.5f + Random.nextFloat() * 0.5f,
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

@Composable
private fun AIProcessingAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai")
    
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "scanProgress"
    )
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "outerRotation"
    )
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "innerRotation"
    )
    val dataFlow by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
        label = "dataFlow"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(320.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val frameSize = size.minDimension * 0.75f
            
            drawTechFrame(center, frameSize, outerRotation, primaryColor, secondaryColor)
            drawInnerRings(center, frameSize * 0.6f, innerRotation, primaryColor)
            drawScanLine(center, frameSize, scanProgress, primaryColor)
            drawDataFlowPoints(center, frameSize * 0.4f, dataFlow, primaryColor, secondaryColor)
            drawAICore(center, frameSize * 0.15f * pulse, primaryColor)
            drawNeuralConnections(center, frameSize * 0.35f, dataFlow, primaryColor)
        }
    }
}

private fun DrawScope.drawTechFrame(center: Offset, frameSize: Float, rotation: Float, primaryColor: Color, secondaryColor: Color) {
    val cornerLength = frameSize * 0.2f
    val strokeWidth = 3.dp.toPx()
    val halfFrame = frameSize / 2
    
    rotate(rotation, pivot = center) {
        val gradientBrush = Brush.sweepGradient(
            listOf(primaryColor, secondaryColor, primaryColor.copy(alpha = 0.3f), secondaryColor.copy(alpha = 0.3f), primaryColor),
            center = center
        )
        
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
        
        listOf(
            Offset(center.x, center.y - halfFrame), Offset(center.x + halfFrame, center.y),
            Offset(center.x, center.y + halfFrame), Offset(center.x - halfFrame, center.y)
        ).forEach { drawCircle(primaryColor, 4.dp.toPx(), it) }
    }
}

private fun DrawScope.drawInnerRings(center: Offset, radius: Float, rotation: Float, color: Color) {
    rotate(rotation, pivot = center) {
        for (i in 0 until 24) {
            if (i % 2 == 0) {
                drawArc(color.copy(alpha = 0.6f), i * 15f, 8f, false,
                    Offset(center.x - radius, center.y - radius), Size(radius * 2, radius * 2),
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }
    drawCircle(color.copy(alpha = 0.3f), radius * 0.7f, center, style = Stroke(1.dp.toPx()))
}

private fun DrawScope.drawScanLine(center: Offset, frameSize: Float, progress: Float, color: Color) {
    val halfFrame = frameSize / 2
    val scanY = center.y - halfFrame + frameSize * progress
    
    drawLine(
        Brush.horizontalGradient(
            listOf(Color.Transparent, color.copy(alpha = 0.8f), color, color.copy(alpha = 0.8f), Color.Transparent),
            center.x - halfFrame, center.x + halfFrame
        ),
        Offset(center.x - halfFrame * 0.8f, scanY), Offset(center.x + halfFrame * 0.8f, scanY), 2.dp.toPx()
    )
    
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, color.copy(alpha = 0.15f), color.copy(alpha = 0.25f), color.copy(alpha = 0.15f), Color.Transparent),
            scanY - 40.dp.toPx(), scanY + 15.dp.toPx()
        ),
        Offset(center.x - halfFrame * 0.8f, scanY - 40.dp.toPx()), Size(halfFrame * 1.6f, 55.dp.toPx())
    )
}

private fun DrawScope.drawDataFlowPoints(center: Offset, radius: Float, progress: Float, primaryColor: Color, secondaryColor: Color) {
    for (i in 0 until 8) {
        val adjustedProgress = (progress + i * 0.1f) % 1f
        val currentRadius = radius * (0.5f + adjustedProgress * 0.5f)
        val angleRad = Math.toRadians((i * 45.0))
        val x = center.x + cos(angleRad).toFloat() * currentRadius
        val y = center.y + sin(angleRad).toFloat() * currentRadius
        val alpha = (1f - adjustedProgress).coerceIn(0.2f, 1f)
        val pointColor = if (i % 2 == 0) primaryColor else secondaryColor
        
        drawCircle(pointColor.copy(alpha = alpha * 0.3f), 8.dp.toPx(), Offset(x, y))
        drawCircle(pointColor.copy(alpha = alpha), 4.dp.toPx(), Offset(x, y))
    }
}

private fun DrawScope.drawAICore(center: Offset, radius: Float, color: Color) {
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = 0.6f), color.copy(alpha = 0.2f), Color.Transparent), center, radius * 2),
        radius * 2, center
    )
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), color.copy(alpha = 0.8f), color.copy(alpha = 0.4f)), center, radius),
        radius, center
    )
    drawCircle(Color.White.copy(alpha = 0.8f), radius, center, style = Stroke(2.dp.toPx()))
}

private fun DrawScope.drawNeuralConnections(center: Offset, radius: Float, progress: Float, color: Color) {
    val nodes = List(6) { i ->
        val angle = Math.toRadians((i * 60.0) + 30)
        Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
    }
    
    nodes.forEachIndexed { i, node ->
        val lineAlpha = (sin(((progress * 2 + i * 0.1f) % 1f) * PI).toFloat() * 0.5f).coerceIn(0.1f, 0.5f)
        drawLine(color.copy(alpha = lineAlpha), center, node, 1.dp.toPx())
        drawLine(color.copy(alpha = lineAlpha * 0.5f), node, nodes[(i + 1) % 6], 0.5.dp.toPx())
        drawCircle(color.copy(alpha = 0.6f + lineAlpha), 3.dp.toPx(), node)
    }
}

@Composable
private fun SuccessExplosionAnimation(modifier: Modifier = Modifier) {
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
    val particles = remember {
        List(20) { i -> Particle(i, i * 18f + Random.nextFloat() * 10f, 0.6f + Random.nextFloat() * 0.4f,
            4f + Random.nextFloat() * 4f, 0.8f + Random.nextFloat() * 0.4f, successColor) }
    }
    
    if (scaleProgress > 0.01f) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.minDimension / 2
                
                particles.forEach { particle ->
                    val currentDistance = maxRadius * particle.distance * explosionProgress * particle.speed
                    val angleRad = Math.toRadians(particle.angle.toDouble())
                    val x = center.x + cos(angleRad).toFloat() * currentDistance
                    val y = center.y + sin(angleRad).toFloat() * currentDistance
                    val alpha = (1f - explosionProgress * 0.7f).coerceIn(0f, 1f)
                    
                    drawCircle(particle.color.copy(alpha = alpha * 0.5f), particle.size * 2 * scaleProgress, Offset(x, y))
                    drawCircle(particle.color.copy(alpha = alpha), particle.size * scaleProgress, Offset(x, y))
                }
                
                drawCircle(
                    Brush.radialGradient(listOf(successColor.copy(alpha = 0.3f * scaleProgress),
                        successColor.copy(alpha = 0.1f * scaleProgress), Color.Transparent), center, maxRadius * 0.6f * scaleProgress),
                    maxRadius * 0.6f * scaleProgress, center
                )
                drawCircle(successColor.copy(alpha = scaleProgress), maxRadius * 0.35f * scaleProgress, center, style = Stroke(4.dp.toPx()))
            }
            
            Icon(Icons.Default.Check, "成功", Modifier.size((56 * scaleProgress).dp), successColor.copy(alpha = scaleProgress))
        }
    }
}

@Composable
private fun ErrorRippleAnimation(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(true) }
    val infiniteTransition = rememberInfiniteTransition(label = "errorRipple")
    
    val ripples = listOf(0, 333, 666).map { delay ->
        infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing, delayMillis = delay), RepeatMode.Restart),
            label = "ripple$delay"
        )
    }
    
    val shake by infiniteTransition.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(50), RepeatMode.Reverse), label = "shake"
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
                    drawCircle(errorColor.copy(alpha = rippleAlpha * 0.3f), maxRadius * progress, center)
                    drawCircle(errorColor.copy(alpha = rippleAlpha), maxRadius * progress, center, style = Stroke(2.dp.toPx()))
                }
                
                drawCircle(
                    Brush.radialGradient(listOf(errorColor.copy(alpha = 0.4f * alpha), errorColor.copy(alpha = 0.1f * alpha), Color.Transparent), center, maxRadius * 0.4f),
                    maxRadius * 0.4f, center
                )
                drawCircle(errorColor.copy(alpha = alpha), maxRadius * 0.3f, center, style = Stroke(3.dp.toPx()))
            }
            
            Icon(Icons.Default.Close, "失败", Modifier.size(48.dp), errorColor.copy(alpha = alpha))
        }
    }
}