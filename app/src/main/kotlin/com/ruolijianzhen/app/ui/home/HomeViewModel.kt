package com.ruolijianzhen.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruolijianzhen.app.domain.history.HistoryItem
import com.ruolijianzhen.app.domain.stats.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * 首页ViewModel
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {
    
    // 今日识别次数
    val todayCount: StateFlow<Int> = statsRepository.getTodayCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    // 总识别次数
    val totalCount: StateFlow<Int> = statsRepository.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    // 最近识别的物品
    val recentItems: StateFlow<List<HistoryItem>> = statsRepository.getRecentItems(3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 收藏数量
    val favoriteCount: StateFlow<Int> = statsRepository.getFavoriteCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
