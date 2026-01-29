package com.ruolijianzhen.app.ui.camera

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 取景框样式
 */
enum class ViewfinderStyle {
    /**
     * 四角标记
     */
    CORNERS,
    
    /**
     * 完整边框
     */
    FULL_BORDER,
    
    /**
     * 圆形
     */
    CIRCLE,
    
    /**
     * 无边框
     */
    NONE
}

/**
 * 拍照引导提示类型
 */
enum class CameraGuideType {
    /**
     * 正常状态
     */
    NORMAL,
    
    /**
     * 光线不足
     */
    LOW_LIGHT,
    
    /**
     * 距离太远
     */
    TOO_FAR,
    
    /**
     * 距离太近
     */
    TOO_CLOSE,
    
    /**
     * 画面模糊
     */
    BLURRY,
    
    /**
     * 物品不在中央
     */
    OFF_CENTER
}

/**
 * 取景框组件
 * 提供拍照引导和视觉辅助
 */
@Composable
fun CameraViewfinder(
    modifier: Modifier = Modifier,
    style: ViewfinderStyle = ViewfinderStyle.CORNERS,
    guideType: CameraGuideType = CameraGuideType.NORMAL,
    isProcessing: Boolean = false,
    showGuideText: Boolean = true,
    frameColor: Color = Color.White,
    frameAlpha: Float = 0.8f
) {
    // 动画
    val infiniteTransition = rememberInfiniteTransition(label = "viewfinder")
    
    // 边框呼吸动画
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = frameAlpha * 0.6f,
        targetValue = frameAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )
    
    // 扫描线动画（处理中显示）
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )
    
    Box(modifier = modifier) {
        // 取景框
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameWidth = size.width * 0.75f
            val frameHeight = size.height * 0.5f
            val left = (size.width - frameWidth) / 2
            val top = (size.height - frameHeight) / 2
            
            val frameRect = Rect(
                left = left,
                top = top,
                right = left + frameWidth,
                bottom = top + frameHeight
            )
            
            // 绘制半透明遮罩（取景框外部）
            drawMask(frameRect)
            
            // 根据样式绘制取景框
            when (style) {
                ViewfinderStyle.CORNERS -> {
                    drawCornerMarkers(
                        rect = frameRect,
                        color = frameColor.copy(alpha = borderAlpha),
                        cornerLength = 40f,
                        strokeWidth = 4f
                    )
                }
                ViewfinderStyle.FULL_BORDER -> {
                    drawRoundRect(
                        color = frameColor.copy(alpha = borderAlpha),
                        topLeft = Offset(frameRect.left, frameRect.top),
                        size = Size(frameRect.width, frameRect.height),
                        cornerRadius = CornerRadius(16f, 16f),
                        style = Stroke(width = 3f)
                    )
                }
                ViewfinderStyle.CIRCLE -> {
                    val radius = minOf(frameRect.width, frameRect.height) / 2
                    drawCircle(
                        color = frameColor.copy(alpha = borderAlpha),
                        radius = radius,
                        center = frameRect.center,
                        style = Stroke(width = 3f)
                    )
                }
                ViewfinderStyle.NONE -> {
                    // 不绘制边框
                }
            }
            
            // 处理中显示扫描线
            if (isProcessing) {
                val scanY = frameRect.top + frameRect.height * scanLineProgress
                drawLine(
                    color = Color(0xFF4CAF50).copy(alpha = 0.7f),
                    start = Offset(frameRect.left + 10, scanY),
                    end = Offset(frameRect.right - 10, scanY),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }
        
        // 引导文字
        if (showGuideText && !isProcessing) {
            GuideText(
                guideType = guideType,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 180.dp)
            )
        }
        
        // 中心对焦点
        if (!isProcessing && guideType == CameraGuideType.NORMAL) {
            CenterFocusIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = frameColor.copy(alpha = borderAlpha * 0.5f)
            )
        }
    }
}

/**
 * 绘制遮罩
 */
private fun DrawScope.drawMask(frameRect: Rect) {
    val maskColor = Color.Black.copy(alpha = 0.4f)
    
    // 上方遮罩
    drawRect(
        color = maskColor,
        topLeft = Offset.Zero,
        size = Size(size.width, frameRect.top)
    )
    
    // 下方遮罩
    drawRect(
        color = maskColor,
        topLeft = Offset(0f, frameRect.bottom),
        size = Size(size.width, size.height - frameRect.bottom)
    )
    
    // 左侧遮罩
    drawRect(
        color = maskColor,
        topLeft = Offset(0f, frameRect.top),
        size = Size(frameRect.left, frameRect.height)
    )
    
    // 右侧遮罩
    drawRect(
        color = maskColor,
        topLeft = Offset(frameRect.right, frameRect.top),
        size = Size(size.width - frameRect.right, frameRect.height)
    )
}

/**
 * 绘制四角标记
 */
private fun DrawScope.drawCornerMarkers(
    rect: Rect,
    color: Color,
    cornerLength: Float,
    strokeWidth: Float
) {
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    
    // 左上角
    drawLine(color, Offset(rect.left, rect.top + cornerLength), Offset(rect.left, rect.top), strokeWidth)
    drawLine(color, Offset(rect.left, rect.top), Offset(rect.left + cornerLength, rect.top), strokeWidth)
    
    // 右上角
    drawLine(color, Offset(rect.right - cornerLength, rect.top), Offset(rect.right, rect.top), strokeWidth)
    drawLine(color, Offset(rect.right, rect.top), Offset(rect.right, rect.top + cornerLength), strokeWidth)
    
    // 左下角
    drawLine(color, Offset(rect.left, rect.bottom - cornerLength), Offset(rect.left, rect.bottom), strokeWidth)
    drawLine(color, Offset(rect.left, rect.bottom), Offset(rect.left + cornerLength, rect.bottom), strokeWidth)
    
    // 右下角
    drawLine(color, Offset(rect.right - cornerLength, rect.bottom), Offset(rect.right, rect.bottom), strokeWidth)
    drawLine(color, Offset(rect.right, rect.bottom), Offset(rect.right, rect.bottom - cornerLength), strokeWidth)
}

/**
 * 引导文字
 */
@Composable
private fun GuideText(
    guideType: CameraGuideType,
    modifier: Modifier = Modifier
) {
    data class GuideInfo(val icon: ImageVector?, val text: String, val color: Color)

    val guideInfo = when (guideType) {
        CameraGuideType.NORMAL -> GuideInfo(null, "将物品放入取景框内", Color.White)
        CameraGuideType.LOW_LIGHT -> GuideInfo(Icons.Default.Lightbulb, "光线不足，请移到明亮处", Color(0xFFFFC107))
        CameraGuideType.TOO_FAR -> GuideInfo(Icons.Default.Straighten, "距离太远，请靠近物品", Color(0xFFFFC107))
        CameraGuideType.TOO_CLOSE -> GuideInfo(Icons.Default.Straighten, "距离太近，请稍微后退", Color(0xFFFFC107))
        CameraGuideType.BLURRY -> GuideInfo(Icons.Default.CameraAlt, "画面模糊，请保持稳定", Color(0xFFFFC107))
        CameraGuideType.OFF_CENTER -> GuideInfo(Icons.Default.GpsFixed, "请将物品移到画面中央", Color(0xFFFFC107))
    }

    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            guideInfo.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = guideInfo.color
                )
            }
            Text(
                text = guideInfo.text,
                color = guideInfo.color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 中心对焦指示器
 */
@Composable
private fun CenterFocusIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Float = 24f
) {
    Canvas(modifier = modifier.size(size.dp)) {
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val lineLength = this.size.width / 3
        
        // 十字线
        drawLine(
            color = color,
            start = Offset(center.x - lineLength, center.y),
            end = Offset(center.x + lineLength, center.y),
            strokeWidth = 1.5f
        )
        drawLine(
            color = color,
            start = Offset(center.x, center.y - lineLength),
            end = Offset(center.x, center.y + lineLength),
            strokeWidth = 1.5f
        )
        
        // 中心点
        drawCircle(
            color = color,
            radius = 3f,
            center = center
        )
    }
}

/**
 * 对焦动画指示器
 * 点击对焦时显示
 */
@Composable
fun FocusAnimationIndicator(
    position: Offset,
    modifier: Modifier = Modifier,
    onAnimationEnd: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(true) }
    
    val animatedSize by animateFloatAsState(
        targetValue = if (isVisible) 60f else 40f,
        animationSpec = tween(300, easing = EaseOutBack),
        label = "focusSize"
    )
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300),
        finishedListener = { onAnimationEnd() },
        label = "focusAlpha"
    )
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(800)
        isVisible = false
    }
    
    Canvas(
        modifier = modifier
            .offset(
                x = (position.x - animatedSize / 2).dp,
                y = (position.y - animatedSize / 2).dp
            )
            .size(animatedSize.dp)
    ) {
        // 外圈
        drawCircle(
            color = Color.White.copy(alpha = animatedAlpha * 0.8f),
            radius = size.width / 2,
            style = Stroke(width = 2f)
        )
        
        // 内圈
        drawCircle(
            color = Color.White.copy(alpha = animatedAlpha * 0.5f),
            radius = size.width / 4,
            style = Stroke(width = 1f)
        )
    }
}

/**
 * 网格线组件
 */
@Composable
fun GridOverlay(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.3f),
    lineWidth: Float = 1f
) {
    Canvas(modifier = modifier) {
        val thirdWidth = size.width / 3
        val thirdHeight = size.height / 3
        
        // 垂直线
        for (i in 1..2) {
            drawLine(
                color = color,
                start = Offset(thirdWidth * i, 0f),
                end = Offset(thirdWidth * i, size.height),
                strokeWidth = lineWidth
            )
        }
        
        // 水平线
        for (i in 1..2) {
            drawLine(
                color = color,
                start = Offset(0f, thirdHeight * i),
                end = Offset(size.width, thirdHeight * i),
                strokeWidth = lineWidth
            )
        }
    }
}