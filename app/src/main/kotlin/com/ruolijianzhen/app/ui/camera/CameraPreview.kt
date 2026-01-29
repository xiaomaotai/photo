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

/**
 * CameraX预览Compose组件
 * 支持点击对焦、双指缩放、双击快速缩放
 * 优化：直接应用缩放，减少延迟感
 */
@Composable
fun CameraPreview(
    cameraManager: CameraManager,
    modifier: Modifier = Modifier,
    onTapToFocus: ((Float, Float) -> Unit)? = null,
    onZoomChanged: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current

    // 当前缩放比例（直接应用，减少延迟）
    var currentZoom by remember { mutableFloatStateOf(1f) }

    // 对焦指示器状态
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusIndicator by remember { mutableStateOf(false) }

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
    
    // 当缩放值变化时，直接应用到相机
    LaunchedEffect(currentZoom) {
        cameraManager.setZoomRatio(currentZoom)
        onZoomChanged?.invoke(currentZoom)
    }

    // 启动摄像头预览
    LaunchedEffect(previewView) {
        try {
            cameraManager.startPreview(previewView)
            currentZoom = cameraManager.getCurrentZoomRatio()
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
                // 双指缩放手势 - 直接应用版本
                .pointerInput(cameraManager) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) {
                            currentZoom = (currentZoom * zoom).coerceIn(minZoom, maxZoom)
                        }
                    }
                }
                // 点击和双击手势
                .pointerInput(cameraManager) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            // 双击快速缩放
                            currentZoom = if (currentZoom > minZoom + 0.5f) {
                                minZoom
                            } else {
                                minOf(2.5f, maxZoom)
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
