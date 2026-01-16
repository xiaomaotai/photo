package com.ruolijianzhen.app.domain.camera

import android.graphics.Bitmap
import androidx.camera.view.PreviewView

/**
 * 摄像头管理器接口
 */
interface CameraManager {
    /**
     * 启动摄像头预览
     * @param previewView 预览视图
     */
    suspend fun startPreview(previewView: PreviewView)
    
    /**
     * 停止摄像头预览
     */
    fun stopPreview()
    
    /**
     * 捕获当前帧
     * @return 捕获的图片
     */
    suspend fun captureImage(): Bitmap?
    
    /**
     * 切换前后摄像头
     */
    suspend fun switchCamera()
    
    /**
     * 设置点击对焦
     * @param x 触摸点X坐标
     * @param y 触摸点Y坐标
     */
    fun setTapToFocus(x: Float, y: Float)
    
    /**
     * 获取当前是否使用前置摄像头
     */
    fun isFrontCamera(): Boolean
    
    /**
     * 设置缩放比例
     * @param ratio 缩放比例 (1.0 = 无缩放)
     */
    fun setZoomRatio(ratio: Float)
    
    /**
     * 获取最小缩放比例
     */
    fun getMinZoomRatio(): Float
    
    /**
     * 获取最大缩放比例
     */
    fun getMaxZoomRatio(): Float
    
    /**
     * 获取当前缩放比例
     */
    fun getCurrentZoomRatio(): Float
    
    /**
     * 开启/关闭闪光灯
     * @param enabled 是否开启
     */
    fun setFlashEnabled(enabled: Boolean)
    
    /**
     * 获取闪光灯是否开启
     */
    fun isFlashEnabled(): Boolean
    
    /**
     * 释放资源
     */
    fun release()
}
