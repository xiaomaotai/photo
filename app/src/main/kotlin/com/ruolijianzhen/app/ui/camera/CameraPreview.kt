package com.ruolijianzhen.app.ui.camera

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ruolijianzhen.app.domain.camera.CameraManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * CameraX预览Compose组件
 * 支持点击对焦、双指缩放、双击快速缩放、网格辅助线
 * 优化：使用transformable手势实现丝滑缩放
 */
@Composable
fun CameraPreview(
    cameraManager: CameraManager,
    modifier: Modifier = Modifier,
    showGrid: Boolean = false,
    onTapToFocus: ((Float, Float) -> Unit)? = null,
    onZoomChanged: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 目标缩放比例（用于动画）
    var targetZoom by remember { mutableFloatStateOf(1f) }
    
    // 当前显示的缩放比例（带动画）
    val animatedZoom by animateFloatAsState(
        targetValue = targetZoom,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "zoomAnimation"
    )
    
    // 实际应用到相机的缩放比例
    var appliedZoom by remember { mutableFloatStateOf(1f) }
    
    // 对焦指示器状态
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusIndicator by remember { mutableStateOf(false) }
    
    // 双击检测
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }
    
    // 缩放范围
    var minZoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(10f) }
    
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // 使用PERFORMANCE模式提升流畅度
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }
    
    // 当动画缩放值变化时，应用到相机
    LaunchedEffect(animatedZoom) {
        if (kotlin.math.abs(animatedZoom - appliedZoom) > 0.005f) {
            appliedZoom = animatedZoom
            cameraManager.setZoomRatio(animatedZoom)
            onZoomChanged?.invoke(animatedZoom)
        }
    }
    
    // 启动摄像头预览
    LaunchedEffect(previewView) {
        try {
            cameraManager.startPreview(previewView)
            targetZoom = cameraManager.getCurrentZoomRatio()
            appliedZoom = targetZoom
            minZoom = cameraManager.getMinZoomRatio()
            maxZoom = cameraManager.getMaxZoomRatio()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            cameraManager.stopPreview()
        }
    }
    
    // 对焦指示器自动隐藏
    LaunchedEffect(showFocusIndicator) {
        if (showFocusIndicator) {
            delay(1500)
            showFocusIndicator = false
        }
    }
    
    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                // 双指缩放手势 - 丝滑版本
                .pointerInput(cameraManager) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) {
                            val newZoom = (targetZoom * zoom).coerceIn(minZoom, maxZoom)
                            targetZoom = newZoom
                        }
                    }
                }
                // 点击和双击手势
                .pointerInput(cameraManager) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            // 双击快速缩放
                            scope.launch {
                                targetZoom = if (targetZoom > minZoom + 0.5f) {
                                    minZoom
                                } else {
                                    minOf(2.5f, maxZoom)
                                }
                            }
                        },
                        onTap = { offset ->
                            // 单击对焦
                            focusPoint = offset
                            showFocusIndicator = true
                            onTapToFocus?.invoke(offset.x, offset.y)
                                ?: cameraManager.setTapToFocus(offset.x, offset.y)
                        }
                    )
                }
        )
        
        // 网格辅助线
        if (showGrid) {
            GridOverlay(modifier = Modifier.fillMaxSize())
        }
        
        // 对焦指示器
        if (showFocusIndicator && focusPoint != null) {
            FocusIndicator(
                position = focusPoint!!,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 九宫格辅助线
 */
@Composable
fun GridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = 1.dp.toPx()
        val color = Color.White.copy(alpha = 0.4f)
        
        // 垂直线
        drawLine(
            color = color,
            start = Offset(width / 3, 0f),
            end = Offset(width / 3, height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = color,
            start = Offset(width * 2 / 3, 0f),
            end = Offset(width * 2 / 3, height),
            strokeWidth = strokeWidth
        )
        
        // 水平线
        drawLine(
            color = color,
            start = Offset(0f, height / 3),
            end = Offset(width, height / 3),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = color,
            start = Offset(0f, height * 2 / 3),
            end = Offset(width, height * 2 / 3),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * 对焦指示器
 */
@Composable
fun FocusIndicator(
    position: Offset,
    modifier: Modifier = Modifier
) {
    val indicatorSize = 70.dp
    val density = LocalDensity.current
    val sizePx = with(density) { indicatorSize.toPx() }
    
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "focusScale"
    )
    
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(200),
        label = "focusAlpha"
    )
    
    Canvas(modifier = modifier) {
        val centerX = position.x
        val centerY = position.y
        val radius = (sizePx / 2) * animatedScale
        
        // 外圈
        drawCircle(
            color = Color.White.copy(alpha = 0.8f * animatedAlpha),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // 内圈
        drawCircle(
            color = Color.White.copy(alpha = 0.5f * animatedAlpha),
            radius = radius * 0.3f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.dp.toPx())
        )
        
        // 十字线
        val lineLength = radius * 0.4f
        val lineColor = Color.White.copy(alpha = 0.6f * animatedAlpha)
        val lineWidth = 1.5.dp.toPx()
        
        drawLine(lineColor, Offset(centerX, centerY - radius + 8.dp.toPx()), Offset(centerX, centerY - radius + 8.dp.toPx() + lineLength), lineWidth)
        drawLine(lineColor, Offset(centerX, centerY + radius - 8.dp.toPx()), Offset(centerX, centerY + radius - 8.dp.toPx() - lineLength), lineWidth)
        drawLine(lineColor, Offset(centerX - radius + 8.dp.toPx(), centerY), Offset(centerX - radius + 8.dp.toPx() + lineLength, centerY), lineWidth)
        drawLine(lineColor, Offset(centerX + radius - 8.dp.toPx(), centerY), Offset(centerX + radius - 8.dp.toPx() - lineLength, centerY), lineWidth)
    }
}

private fun calculateDistance(p1: Offset, p2: Offset): Float {
    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    return sqrt(dx * dx + dy * dy)
}
