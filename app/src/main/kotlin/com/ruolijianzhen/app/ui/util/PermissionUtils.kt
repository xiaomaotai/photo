package com.ruolijianzhen.app.ui.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * 权限管理工具
 * 处理各品牌权限差异（华为、小米、OPPO、vivo、Samsung等）
 */
object PermissionUtils {
    
    /**
     * 摄像头权限
     */
    val CAMERA_PERMISSION = Manifest.permission.CAMERA
    
    /**
     * 存储权限（根据Android版本不同）
     */
    val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    
    /**
     * 检查摄像头权限
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            CAMERA_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 检查存储权限
     */
    fun hasStoragePermission(context: Context): Boolean {
        return STORAGE_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 打开应用设置页面
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * 获取设备品牌
     */
    fun getDeviceBrand(): String {
        return Build.MANUFACTURER.lowercase()
    }
    
    /**
     * 是否为华为设备
     */
    fun isHuaweiDevice(): Boolean {
        val brand = getDeviceBrand()
        return brand.contains("huawei") || brand.contains("honor")
    }
    
    /**
     * 是否为小米设备
     */
    fun isXiaomiDevice(): Boolean {
        val brand = getDeviceBrand()
        return brand.contains("xiaomi") || brand.contains("redmi")
    }
    
    /**
     * 是否为OPPO设备
     */
    fun isOppoDevice(): Boolean {
        val brand = getDeviceBrand()
        return brand.contains("oppo") || brand.contains("realme") || brand.contains("oneplus")
    }
    
    /**
     * 是否为vivo设备
     */
    fun isVivoDevice(): Boolean {
        return getDeviceBrand().contains("vivo")
    }
}

/**
 * 摄像头权限请求Composable
 */
@Composable
fun CameraPermissionRequest(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            showSettingsDialog = true
        }
    }
    
    LaunchedEffect(Unit) {
        if (PermissionUtils.hasCameraPermission(context)) {
            onPermissionGranted()
        } else {
            permissionLauncher.launch(PermissionUtils.CAMERA_PERMISSION)
        }
    }
    
    // 权限被拒绝后的引导对话框
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSettingsDialog = false
                onPermissionDenied()
            },
            title = { Text("需要摄像头权限") },
            text = { 
                Text("若里见真需要使用摄像头来识别物品。请在设置中允许摄像头权限。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettingsDialog = false
                        PermissionUtils.openAppSettings(context)
                    }
                ) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSettingsDialog = false
                        onPermissionDenied()
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 权限状态
 */
sealed class PermissionState {
    data object Granted : PermissionState()
    data object Denied : PermissionState()
    data object NeverAskAgain : PermissionState()
}
