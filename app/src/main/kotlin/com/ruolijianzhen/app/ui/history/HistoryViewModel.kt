package com.ruolijianzhen.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruolijianzhen.app.data.history.HistoryRepositoryImpl
import com.ruolijianzhen.app.domain.history.HistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * 历史记录页面ViewModel - 支持搜索、筛选和批量操作
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepositoryImpl
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    private val _selectedItem = MutableStateFlow<HistoryItem?>(null)
    val selectedItem: StateFlow<HistoryItem?> = _selectedItem.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow<HistoryFilter>(HistoryFilter.All)
    val selectedFilter: StateFlow<HistoryFilter> = _selectedFilter.asStateFlow()
    
    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()
    
    // 批量选择模式
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    
    // 已选择的项目ID
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()
    
    // 当前所有项目（用于全选）
    private var allItems: List<HistoryItem> = emptyList()
    
    private var searchJob: Job? = null
    
    init {
        loadCategories()
        loadHistory()
        
        // 监听搜索和筛选变化
        viewModelScope.launch {
            combine(searchQuery, selectedFilter) { query, filter ->
                query to filter
            }.debounce(300).collect { (query, filter) ->
                applyFilters(query, filter)
            }
        }
    }
    
    /**
     * 加载分类列表
     */
    private fun loadCategories() {
        viewModelScope.launch {
            historyRepository.getAllCategories().collect { cats ->
                _categories.value = cats.filter { it.isNotBlank() }
            }
        }
    }
    
    /**
     * 加载历史记录
     */
    private fun loadHistory() {
        viewModelScope.launch {
            try {
                val groupedHistory = historyRepository.getGroupedByDateSync()
                allItems = groupedHistory.values.flatten()
                if (groupedHistory.isEmpty()) {
                    _uiState.value = HistoryUiState.Empty
                } else {
                    _uiState.value = HistoryUiState.Success(groupedHistory)
                }
            } catch (e: Exception) {
                _uiState.value = HistoryUiState.Error("加载历史记录失败: ${e.message}")
            }
        }
    }
    
    /**
     * 应用搜索和筛选
     */
    private fun applyFilters(query: String, filter: HistoryFilter) {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading
            
            try {
                val flow: Flow<List<HistoryItem>> = when {
                    query.isNotBlank() -> historyRepository.search(query)
                    filter == HistoryFilter.Favorites -> historyRepository.getFavorites()
                    filter is HistoryFilter.Category -> historyRepository.getByCategory(filter.name)
                    else -> historyRepository.getAllFlow()
                }
                
                flow.collect { items ->
                    allItems = items
                    if (items.isEmpty()) {
                        _uiState.value = HistoryUiState.Empty
                    } else {
                        val grouped = groupByDate(items)
                        _uiState.value = HistoryUiState.Success(grouped)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = HistoryUiState.Error("加载失败: ${e.message}")
            }
        }
    }
    
    /**
     * 按日期分组
     */
    private fun groupByDate(items: List<HistoryItem>): Map<String, List<HistoryItem>> {
        val calendar = Calendar.getInstance()
        val todayStart = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val yesterdayStart = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val result = linkedMapOf<String, MutableList<HistoryItem>>()
        
        items.forEach { item ->
            val group = when {
                item.timestamp >= todayStart -> "今天"
                item.timestamp >= yesterdayStart -> "昨天"
                else -> "更早"
            }
            result.getOrPut(group) { mutableListOf() }.add(item)
        }
        
        return result
    }
    
    /**
     * 设置搜索关键词
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * 设置筛选条件
     */
    fun setFilter(filter: HistoryFilter) {
        _selectedFilter.value = filter
    }
    
    /**
     * 刷新历史记录
     */
    fun refresh() {
        _uiState.value = HistoryUiState.Loading
        applyFilters(_searchQuery.value, _selectedFilter.value)
    }
    
    /**
     * 选择历史记录项
     */
    fun selectItem(item: HistoryItem) {
        if (_isSelectionMode.value) {
            toggleItemSelection(item.id)
        } else {
            _selectedItem.value = item
        }
    }
    
    /**
     * 清除选中项
     */
    fun clearSelection() {
        _selectedItem.value = null
    }
    
    // ========== 批量操作功能 ==========
    
    /**
     * 进入批量选择模式
     */
    fun enterSelectionMode() {
        _isSelectionMode.value = true
        _selectedIds.value = emptySet()
    }
    
    /**
     * 退出批量选择模式
     */
    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedIds.value = emptySet()
    }
    
    /**
     * 切换单个项目的选择状态
     */
    fun toggleItemSelection(id: String) {
        val currentSelection = _selectedIds.value.toMutableSet()
        if (currentSelection.contains(id)) {
            currentSelection.remove(id)
        } else {
            currentSelection.add(id)
        }
        _selectedIds.value = currentSelection
        
        // 如果没有选中任何项目，退出选择模式
        if (currentSelection.isEmpty()) {
            _isSelectionMode.value = false
        }
    }
    
    /**
     * 全选
     */
    fun selectAll() {
        _selectedIds.value = allItems.map { it.id }.toSet()
    }
    
    /**
     * 取消全选
     */
    fun deselectAll() {
        _selectedIds.value = emptySet()
    }
    
    /**
     * 批量删除选中项
     */
    fun deleteSelected() {
        viewModelScope.launch {
            try {
                val idsToDelete = _selectedIds.value.toList()
                idsToDelete.forEach { id ->
                    historyRepository.delete(id)
                }
                exitSelectionMode()
                refresh()
            } catch (e: Exception) {
                _uiState.value = HistoryUiState.Error("批量删除失败: ${e.message}")
            }
        }
    }
    
    /**
     * 批量收藏选中项
     */
    fun favoriteSelected() {
        viewModelScope.launch {
            try {
                val idsToFavorite = _selectedIds.value.toList()
                idsToFavorite.forEach { id ->
                    historyRepository.updateFavorite(id, true)
                }
                exitSelectionMode()
                refresh()
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }
    
    /**
     * 批量取消收藏选中项
     */
    fun unfavoriteSelected() {
        viewModelScope.launch {
            try {
                val idsToUnfavorite = _selectedIds.value.toList()
                idsToUnfavorite.forEach { id ->
                    historyRepository.updateFavorite(id, false)
                }
                exitSelectionMode()
                refresh()
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }
    
    /**
     * 获取选中项数量
     */
    fun getSelectedCount(): Int = _selectedIds.value.size
    
    /**
     * 检查是否全选
     */
    fun isAllSelected(): Boolean = _selectedIds.value.size == allItems.size && allItems.isNotEmpty()
    
    // ========== 原有功能 ==========
    
    /**
     * 删除历史记录
     */
    fun deleteItem(id: String) {
        viewModelScope.launch {
            try {
                historyRepository.delete(id)
                _selectedItem.value = null
                refresh()
            } catch (e: Exception) {
                _uiState.value = HistoryUiState.Error("删除失败: ${e.message}")
            }
        }
    }
    
    /**
     * 切换收藏状态
     */
    fun toggleFavorite(item: HistoryItem) {
        viewModelScope.launch {
            try {
                historyRepository.updateFavorite(item.id, !item.isFavorite)
                // 更新选中项
                if (_selectedItem.value?.id == item.id) {
                    _selectedItem.value = item.copy(isFavorite = !item.isFavorite)
                }
                refresh()
            } catch (e: Exception) {
                // 忽略错误
            }
        }
    }
    
    /**
     * 清空所有历史记录（保留收藏）
     */
    fun clearAll() {
        viewModelScope.launch {
            try {
                // 只清理非收藏的记录
                historyRepository.cleanOldData(0)
                refresh()
            } catch (e: Exception) {
                _uiState.value = HistoryUiState.Error("清空失败: ${e.message}")
            }
        }
    }
}

/**
 * 历史记录页面UI状态
 */
sealed class HistoryUiState {
    data object Loading : HistoryUiState()
    data object Empty : HistoryUiState()
    data class Success(val groupedHistory: Map<String, List<HistoryItem>>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}
