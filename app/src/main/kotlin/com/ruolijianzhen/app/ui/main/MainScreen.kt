package com.ruolijianzhen.app.ui.main

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruolijianzhen.app.ui.animation.ScanningAnimation
import com.ruolijianzhen.app.ui.animation.ScanningAnimationState
import com.ruolijianzhen.app.ui.camera.CameraPreview
import com.ruolijianzhen.app.ui.util.CameraPermissionRequest
import com.ruolijianzhen.app.ui.util.PermissionUtils

/**
 * 主界面（相机识别页面）
 * 全屏沉浸式设计，顶部悬浮工具栏
 */
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val frozenBitmap by viewModel.frozenBitmap.collectAsState()
    
    var hasPermission by remember { mutableStateOf(PermissionUtils.hasCameraPermission(context)) }
    var showPermissionDenied by remember { mutableStateOf(false) }
    
    // 闪光灯状态
    var isFlashOn by remember { mutableStateOf(false) }
    
    // 网格显示状态
    var showGrid by remember { mutableStateOf(false) }
    
    // 缩放比例状态
    var currentZoom by remember { mutableFloatStateOf(1f) }
    
    // 是否前置摄像头
    var isFrontCamera by remember { mutableStateOf(false) }
    
    // 是否正在处理中
    val isProcessing = uiState is MainUiState.Processing
    
    // 进入页面时，只有当前状态是ShowResult或Error时才重置
    // 避免重置Idle状态导致识别引擎异常
    LaunchedEffect(Unit) {
        if (uiState is MainUiState.ShowResult || uiState is MainUiState.Error) {
            viewModel.dismissResult()
        }
    }
    
    // 根据UI状态计算动画状态
    val animationState by remember {
        derivedStateOf {
            when (uiState) {
                is MainUiState.Idle -> ScanningAnimationState.Idle
                is MainUiState.Processing -> ScanningAnimationState.Processing
                is MainUiState.ShowResult -> ScanningAnimationState.Success
                is MainUiState.Error -> ScanningAnimationState.Error
            }
        }
    }
    
    // 识别中文字动画
    val infiniteTransition = rememberInfiniteTransition(label = "processing")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    
    // 权限请求
    if (!hasPermission && !showPermissionDenied) {
        CameraPermissionRequest(
            onPermissionGranted = { hasPermission = true },
            onPermissionDenied = { showPermissionDenied = true }
        )
    }
    
    // 全屏沉浸式布局
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            // 摄像头预览或冻结帧 - 全屏显示
            Box(modifier = Modifier.fillMaxSize()) {
                if (frozenBitmap != null) {
                    FrozenFrameOverlay(
                        bitmap = frozenBitmap!!,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CameraPreview(
                        cameraManager = viewModel.cameraManager,
                        modifier = Modifier.fillMaxSize(),
                        showGrid = showGrid,
                        onZoomChanged = { zoom -> currentZoom = zoom }
                    )
                }
            }
            
            // 顶部渐变遮罩 + 悬浮工具栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回按钮
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                    
                    // 右侧工具按钮组
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 网格按钮
                        IconButton(
                            onClick = { showGrid = !showGrid },
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (showGrid) Color.White.copy(alpha = 0.3f) 
                                    else Color.Black.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        ) {
                            Icon(Icons.Default.GridOn, "网格", tint = Color.White)
                        }
                        
                        // 闪光灯按钮
                        if (!isFrontCamera) {
                            IconButton(
                                onClick = { 
                                    isFlashOn = !isFlashOn
                                    viewModel.setFlashEnabled(isFlashOn)
                                },
                                enabled = !isProcessing,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (isFlashOn) Color(0xFFFFD700).copy(alpha = 0.5f)
                                        else Color.Black.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    "闪光灯",
                                    tint = if (isProcessing) Color.White.copy(alpha = 0.5f) else Color.White
                                )
                            }
                        }
                        
                        // 切换摄像头
                        IconButton(
                            onClick = { 
                                viewModel.switchCamera()
                                isFrontCamera = !isFrontCamera
                                if (isFrontCamera && isFlashOn) {
                                    isFlashOn = false
                                    viewModel.setFlashEnabled(false)
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(Icons.Default.Cameraswitch, "切换", tint = Color.White)
                        }
                        
                        // 历史记录
                        IconButton(
                            onClick = onNavigateToHistory,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(Icons.Default.History, "历史", tint = Color.White)
                        }
                        
                        // 设置
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(Icons.Default.Settings, "设置", tint = Color.White)
                        }
                    }
                }
            }
            
            // 缩放倍数指示器
            if (currentZoom > 1.01f && frozenBitmap == null) {
                ZoomIndicator(
                    zoomRatio = currentZoom,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 60.dp)
                )
            }
            
            // 处理中遮罩
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
            
            // 扫描动画
            ScanningAnimation(state = animationState, modifier = Modifier.fillMaxSize())
            
            // 底部渐变遮罩 + 识别按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp, top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { if (!isProcessing) viewModel.captureAndRecognize() },
                    modifier = Modifier.height(60.dp).widthIn(min = 160.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isProcessing) MaterialTheme.colorScheme.secondary 
                            else MaterialTheme.colorScheme.primary
                    ),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onSecondary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("识别中", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("...", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.alpha(dotAlpha))
                    } else {
                        Icon(Icons.Default.CameraAlt, null, Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("识别", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }
        } else if (showPermissionDenied) {
            PermissionDeniedView(
                onOpenSettings = { PermissionUtils.openAppSettings(context) },
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // 结果弹窗
        if (uiState is MainUiState.ShowResult) {
            ResultBottomSheet(
                objectInfo = (uiState as MainUiState.ShowResult).objectInfo,
                capturedBitmap = frozenBitmap,
                onDismiss = { viewModel.dismissResult() },
                onRecognizeAgain = { viewModel.dismissResult() }
            )
        }
        
        // 错误提示
        if (uiState is MainUiState.Error) {
            ErrorSnackbar(
                message = (uiState as MainUiState.Error).message,
                onRetry = { viewModel.retry() },
                onDismiss = { viewModel.dismissResult() }
            )
        }
    }
}

@Composable
fun PermissionDeniedView(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Default.CameraAlt, null, Modifier.size(64.dp), tint = Color.White.copy(alpha = 0.7f))
        Text("需要摄像头权限", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text("若里见真需要使用摄像头来识别物品", color = Color.White.copy(alpha = 0.7f))
        Button(onClick = onOpenSettings) { Text("去设置") }
    }
}

@Composable
fun BoxScope.ErrorSnackbar(message: String, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Snackbar(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 100.dp),
        action = { TextButton(onClick = onRetry) { Text("重试") } },
        dismissAction = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭") } }
    ) { Text(message) }
}

@Composable
fun FrozenFrameOverlay(bitmap: Bitmap, modifier: Modifier = Modifier) {
    Image(bitmap.asImageBitmap(), "捕获的画面", modifier, contentScale = ContentScale.Crop)
}

@Composable
fun ZoomIndicator(zoomRatio: Float, modifier: Modifier = Modifier) {
    Surface(modifier, RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = 0.6f)) {
        Text("%.1fx".format(zoomRatio), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), Color.White, 16.sp, fontWeight = FontWeight.Medium)
    }
}
