package com.ruolijianzhen.app.ui.history

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruolijianzhen.app.domain.history.HistoryItem
import java.text.SimpleDateFormat
import java.util.*

/**
 * 历史记录页面 - 简洁现代设计
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val categories by viewModel.categories.collectAsState()
    
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showSearchBar by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            if (showSearchBar) {
                SearchTopBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onClose = {
                        showSearchBar = false
                        viewModel.setSearchQuery("")
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "历史记录",
                            fontWeight = FontWeight.SemiBold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Default.Search, "搜索")
                        }
                        if (uiState is HistoryUiState.Success) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(Icons.Default.DeleteSweep, "清空")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 筛选标签
            FilterSection(
                selectedFilter = selectedFilter,
                categories = categories,
                onFilterSelected = { viewModel.setFilter(it) }
            )
            
            // 内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is HistoryUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    
                    is HistoryUiState.Empty -> {
                        EmptyHistoryView(
                            modifier = Modifier.align(Alignment.Center),
                            isFiltered = selectedFilter != HistoryFilter.All || searchQuery.isNotEmpty()
                        )
                    }
                    
                    is HistoryUiState.Success -> {
                        HistoryList(
                            groupedHistory = state.groupedHistory,
                            onItemClick = { viewModel.selectItem(it) },
                            onDeleteClick = { showDeleteDialog = it.id },
                            onFavoriteClick = { viewModel.toggleFavorite(it) }
                        )
                    }
                    
                    is HistoryUiState.Error -> {
                        ErrorView(
                            message = state.message,
                            onRetry = { viewModel.refresh() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
    
    // 详情弹窗
    selectedItem?.let { item ->
        HistoryDetailSheet(
            item = item,
            onDismiss = { viewModel.clearSelection() },
            onDelete = {
                showDeleteDialog = item.id
                viewModel.clearSelection()
            },
            onFavorite = { viewModel.toggleFavorite(item) }
        )
    }
    
    // 清空确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空历史记录") },
            text = { Text("确定要清空所有历史记录吗？收藏的记录将被保留。") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearAll()
                    showClearDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
    
    // 删除确认对话框
    showDeleteDialog?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除记录") },
            text = { Text("确定要删除这条记录吗？") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteItem(id)
                    showDeleteDialog = null
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }
}

/**
 * 搜索顶栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("搜索物品名称或分类", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, "关闭搜索")
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "清除")
                }
            }
        }
    )
}

/**
 * 筛选区域
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(
    selectedFilter: HistoryFilter,
    categories: List<String>,
    onFilterSelected: (HistoryFilter) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedFilter == HistoryFilter.All,
                onClick = { onFilterSelected(HistoryFilter.All) },
                label = { Text("全部") }
            )
        }
        
        item {
            FilterChip(
                selected = selectedFilter == HistoryFilter.Favorites,
                onClick = { onFilterSelected(HistoryFilter.Favorites) },
                label = { Text("收藏") }
            )
        }
        
        items(categories) { category ->
            FilterChip(
                selected = selectedFilter is HistoryFilter.Category && selectedFilter.name == category,
                onClick = { onFilterSelected(HistoryFilter.Category(category)) },
                label = { Text(category) }
            )
        }
    }
}

/**
 * 历史记录列表
 */
@Composable
fun HistoryList(
    groupedHistory: Map<String, List<HistoryItem>>,
    onItemClick: (HistoryItem) -> Unit,
    onDeleteClick: (HistoryItem) -> Unit,
    onFavoriteClick: (HistoryItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groupedHistory.forEach { (dateGroup, items) ->
            item {
                Text(
                    text = dateGroup,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }
            
            items(items, key = { it.id }) { item ->
                HistoryItemCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onDeleteClick = { onDeleteClick(item) },
                    onFavoriteClick = { onFavoriteClick(item) }
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 历史记录项卡片 - 简洁设计
 */
@Composable
fun HistoryItemCard(
    item: HistoryItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧内容
            Column(modifier = Modifier.weight(1f)) {
                // 名称和收藏标记
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.isFavorite) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "❤️",
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // 分类和时间
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 分类标签
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Text(
                        text = timeFormat.format(Date(item.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // 置信度
                    Text(
                        text = "${(item.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 右侧操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 收藏按钮
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (item.isFavorite) "取消收藏" else "收藏",
                        tint = if (item.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 删除按钮
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 空历史视图
 */
@Composable
fun EmptyHistoryView(
    modifier: Modifier = Modifier,
    isFiltered: Boolean = false
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isFiltered) "🔍" else "📭",
            fontSize = 56.sp
        )
        
        Text(
            text = if (isFiltered) "没有找到匹配的记录" else "暂无历史记录",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = if (isFiltered) "试试其他搜索词或筛选条件" else "识别物品后会自动保存到这里",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * 错误视图
 */
@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("😕", fontSize = 56.sp)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        FilledTonalButton(onClick = onRetry) { 
            Text("重试") 
        }
    }
}

/**
 * 历史详情底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailSheet(
    item: HistoryItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = onFavorite) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (item.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 详情内容
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (item.aliases.isNotEmpty()) {
                    DetailCard("别名", item.aliases.joinToString("、"))
                }
                DetailCard("来历", item.origin)
                DetailCard("用途", item.usage)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailCard(
                        label = "分类",
                        value = item.category,
                        modifier = Modifier.weight(1f)
                    )
                    DetailCard(
                        label = "置信度",
                        value = "${(item.confidence * 100).toInt()}%",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 底部按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

/**
 * 详情卡片
 */
@Composable
fun DetailCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val displayValue = if (value.isBlank() || value.lowercase() == "null") "暂无" else value
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 筛选类型
 */
sealed class HistoryFilter {
    data object All : HistoryFilter()
    data object Favorites : HistoryFilter()
    data class Category(val name: String) : HistoryFilter()
}
