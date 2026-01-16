package com.ruolijianzhen.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruolijianzhen.app.data.ai.UserAiServiceImpl
import com.ruolijianzhen.app.data.preferences.ThemeMode
import com.ruolijianzhen.app.data.preferences.ThemePreferences
import com.ruolijianzhen.app.domain.history.HistoryRepository
import com.ruolijianzhen.app.domain.learning.LearningRepository
import com.ruolijianzhen.app.domain.model.AiApiType
import com.ruolijianzhen.app.domain.model.PriorityConfig
import com.ruolijianzhen.app.domain.model.UserAiConfig
import com.ruolijianzhen.app.domain.model.UserAiConfigList
import com.ruolijianzhen.app.domain.priority.PriorityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页面ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userAiService: UserAiServiceImpl,
    private val learningRepository: LearningRepository,
    private val priorityManager: PriorityManager,
    private val themePreferences: ThemePreferences,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private val _aiConfig = MutableStateFlow<UserAiConfig?>(null)
    val aiConfig: StateFlow<UserAiConfig?> = _aiConfig.asStateFlow()
    
    private val _isValidating = MutableStateFlow(false)
    val isValidating: StateFlow<Boolean> = _isValidating.asStateFlow()
    
    private val _learningDataCount = MutableStateFlow(0)
    val learningDataCount: StateFlow<Int> = _learningDataCount.asStateFlow()
    
    // 多AI配置列表
    val aiConfigList: StateFlow<UserAiConfigList> = userAiService.getConfigListFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserAiConfigList()
        )
    
    // 优先级配置
    val priorityConfig: StateFlow<PriorityConfig> = priorityManager.getConfigFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PriorityConfig.DEFAULT
        )
    
    // 主题模式
    val themeMode: StateFlow<ThemeMode> = themePreferences.getThemeMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )
    
    // 动态颜色
    val dynamicColor: StateFlow<Boolean> = themePreferences.getDynamicColor()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    init {
        loadConfig()
        loadLearningDataCount()
    }
    
    /**
     * 加载当前配置
     */
    private fun loadConfig() {
        viewModelScope.launch {
            try {
                val config = userAiService.getConfig()
                _aiConfig.value = config
                _uiState.value = SettingsUiState.Ready(config != null)
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("加载配置失败: ${e.message}")
            }
        }
    }
    
    /**
     * 加载学习数据数量
     */
    private fun loadLearningDataCount() {
        viewModelScope.launch {
            try {
                _learningDataCount.value = learningRepository.getCount()
            } catch (e: Exception) {
                _learningDataCount.value = 0
            }
        }
    }
    
    /**
     * 清除学习数据
     */
    fun clearLearningData() {
        viewModelScope.launch {
            try {
                learningRepository.clearAll()
                _learningDataCount.value = 0
                _uiState.value = SettingsUiState.Ready(_aiConfig.value != null)
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("清除学习数据失败: ${e.message}")
            }
        }
    }
    
    /**
     * 保存AI配置（兼容旧接口）
     */
    fun saveAiConfig(
        apiType: AiApiType,
        apiUrl: String,
        apiKey: String,
        modelName: String
    ) {
        viewModelScope.launch {
            try {
                _isValidating.value = true
                
                val config = UserAiConfig(
                    name = apiType.displayName,
                    apiType = apiType,
                    apiUrl = apiUrl.trim(),
                    apiKey = apiKey.trim(),
                    modelName = modelName.trim()
                )
                
                // 验证配置
                val isValid = userAiService.validateConfig(config)
                if (!isValid) {
                    _uiState.value = SettingsUiState.Error("配置验证失败，请检查API地址和密钥")
                    return@launch
                }
                
                // 保存配置
                val saved = userAiService.saveConfig(config)
                if (saved) {
                    _aiConfig.value = config
                    _uiState.value = SettingsUiState.SaveSuccess
                } else {
                    _uiState.value = SettingsUiState.Error("保存配置失败")
                }
                
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("保存失败: ${e.message}")
            } finally {
                _isValidating.value = false
            }
        }
    }
    
    /**
     * 添加AI配置
     */
    fun addAiConfig(config: UserAiConfig) {
        viewModelScope.launch {
            try {
                _isValidating.value = true
                
                // 验证配置
                val isValid = userAiService.validateConfig(config)
                if (!isValid) {
                    _uiState.value = SettingsUiState.Error("配置验证失败，请检查API地址和密钥")
                    return@launch
                }
                
                // 添加配置
                val saved = userAiService.addConfig(config)
                if (saved) {
                    _uiState.value = SettingsUiState.SaveSuccess
                    loadConfig()
                } else {
                    _uiState.value = SettingsUiState.Error("添加配置失败")
                }
                
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("添加失败: ${e.message}")
            } finally {
                _isValidating.value = false
            }
        }
    }
    
    /**
     * 更新AI配置
     */
    fun updateAiConfig(config: UserAiConfig) {
        viewModelScope.launch {
            try {
                _isValidating.value = true
                
                // 验证配置
                val isValid = userAiService.validateConfig(config)
                if (!isValid) {
                    _uiState.value = SettingsUiState.Error("配置验证失败，请检查API地址和密钥")
                    return@launch
                }
                
                // 更新配置
                val saved = userAiService.updateConfig(config)
                if (saved) {
                    _uiState.value = SettingsUiState.SaveSuccess
                    loadConfig()
                } else {
                    _uiState.value = SettingsUiState.Error("更新配置失败")
                }
                
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("更新失败: ${e.message}")
            } finally {
                _isValidating.value = false
            }
        }
    }
    
    /**
     * 删除AI配置
     */
    fun deleteAiConfig(configId: String) {
        viewModelScope.launch {
            try {
                val deleted = userAiService.deleteConfig(configId)
                if (deleted) {
                    loadConfig()
                } else {
                    _uiState.value = SettingsUiState.Error("删除配置失败")
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("删除失败: ${e.message}")
            }
        }
    }
    
    /**
     * 设置激活的AI配置
     */
    fun setActiveAiConfig(configId: String) {
        viewModelScope.launch {
            try {
                val success = userAiService.setActiveConfig(configId)
                if (success) {
                    loadConfig()
                } else {
                    _uiState.value = SettingsUiState.Error("设置激活配置失败")
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("设置失败: ${e.message}")
            }
        }
    }
    
    /**
     * 清除AI配置
     */
    fun clearAiConfig() {
        viewModelScope.launch {
            try {
                userAiService.clearConfig()
                _aiConfig.value = null
                _uiState.value = SettingsUiState.Ready(false)
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("清除配置失败: ${e.message}")
            }
        }
    }
    
    /**
     * 保存优先级配置
     */
    fun savePriorityConfig(config: PriorityConfig) {
        viewModelScope.launch {
            try {
                priorityManager.saveConfig(config)
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("保存优先级配置失败: ${e.message}")
            }
        }
    }
    
    /**
     * 设置主题模式
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }
    
    /**
     * 设置动态颜色
     */
    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setDynamicColor(enabled)
        }
    }
    
    /**
     * 清理旧数据
     */
    fun cleanOldData(daysToKeep: Int = 30) {
        viewModelScope.launch {
            try {
                val deleted = historyRepository.cleanOldData(daysToKeep)
                if (deleted > 0) {
                    _uiState.value = SettingsUiState.SaveSuccess
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error("清理数据失败: ${e.message}")
            }
        }
    }
    
    /**
     * 重置状态
     */
    fun resetState() {
        viewModelScope.launch {
            val hasConfig = userAiService.isConfigured()
            _uiState.value = SettingsUiState.Ready(hasConfig)
        }
    }
}

/**
 * 设置页面UI状态
 */
sealed class SettingsUiState {
    data object Loading : SettingsUiState()
    data class Ready(val hasAiConfig: Boolean) : SettingsUiState()
    data object SaveSuccess : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}
