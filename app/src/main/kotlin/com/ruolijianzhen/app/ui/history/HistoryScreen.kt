package com.ruolijianzhen.app.ui.history

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruolijianzhen.app.domain.history.HistoryItem
import com.ruolijianzhen.app.domain.model.RecognitionQuality
import com.ruolijianzhen.app.ui.components.CompactQualityIndicator
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            when {
                isSelectionMode -> SelectionModeTopBar(
                    selectedCount = viewModel.getSelectedCount(),
                    isAllSelected = viewModel.isAllSelected(),
                    onSelectAll = { if (viewModel.isAllSelected()) viewModel.deselectAll() else viewModel.selectAll() },
                    onClose = { viewModel.exitSelectionMode() }
                )
                showSearchBar -> SearchTopBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onClose = { showSearchBar = false; viewModel.setSearchQuery("") }
                )
                else -> CenterAlignedTopAppBar(
                    title = { Text("历史记录", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "返回") } },
                    actions = {
                        IconButton(onClick = { showSearchBar = true }) { Icon(Icons.Default.Search, "搜索") }
                        if (uiState is HistoryUiState.Success) {
                            IconButton(onClick = { viewModel.enterSelectionMode() }) { Icon(Icons.Default.Checklist, "批量操作") }
                            IconButton(onClick = { showClearDialog = true }) { Icon(Icons.Default.DeleteSweep, "清空") }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode && selectedIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BatchActionBar(
                    selectedCount = viewModel.getSelectedCount(),
                    onDelete = { showBatchDeleteDialog = true },
                    onFavorite = { viewModel.favoriteSelected() },
                    onUnfavorite = { viewModel.unfavoriteSelected() }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (!isSelectionMode) {
                FilterSection(selectedFilter = selectedFilter, categories = categories, onFilterSelected = { viewModel.setFilter(it) })
            }
            
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is HistoryUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    is HistoryUiState.Empty -> EmptyHistoryView(modifier = Modifier.align(Alignment.Center), isFiltered = selectedFilter != HistoryFilter.All || searchQuery.isNotEmpty())
                    is HistoryUiState.Success -> HistoryList(
                        groupedHistory = state.groupedHistory,
                        isSelectionMode = isSelectionMode,
                        selectedIds = selectedIds,
                        onItemClick = { viewModel.selectItem(it) },
                        onItemLongClick = { if (!isSelectionMode) { viewModel.enterSelectionMode(); viewModel.toggleItemSelection(it.id) } },
                        onDeleteClick = { showDeleteDialog = it.id },
                        onFavoriteClick = { viewModel.toggleFavorite(it) }
                    )
                    is HistoryUiState.Error -> ErrorView(message = state.message, onRetry = { viewModel.refresh() }, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
    
    selectedItem?.let { item ->
        HistoryDetailSheet(item = item, onDismiss = { viewModel.clearSelection() }, onDelete = { showDeleteDialog = item.id; viewModel.clearSelection() }, onFavorite = { viewModel.toggleFavorite(item) })
    }
    
    if (showClearDialog) {
        AlertDialog(onDismissRequest = { showClearDialog = false }, title = { Text("清空历史记录") }, text = { Text("确定要清空所有历史记录吗？收藏的记录将被保留。") },
            confirmButton = { Button(onClick = { viewModel.clearAll(); showClearDialog = false }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } })
    }
    
    showDeleteDialog?.let { id ->
        AlertDialog(onDismissRequest = { showDeleteDialog = null }, title = { Text("删除记录") }, text = { Text("确定要删除这条记录吗？") },
            confirmButton = { Button(onClick = { viewModel.deleteItem(id); showDeleteDialog = null }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("取消") } })
    }
    
    if (showBatchDeleteDialog) {
        AlertDialog(onDismissRequest = { showBatchDeleteDialog = false }, title = { Text("批量删除") }, text = { Text("确定要删除选中的 ${viewModel.getSelectedCount()} 条记录吗？") },
            confirmButton = { Button(onClick = { viewModel.deleteSelected(); showBatchDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("删除") } },
            dismissButton = { TextButton(onClick = { showBatchDeleteDialog = false }) { Text("取消") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionModeTopBar(selectedCount: Int, isAllSelected: Boolean, onSelectAll: () -> Unit, onClose: () -> Unit) {
    TopAppBar(
        title = { Text("已选择 $selectedCount 项") },
        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "取消选择") } },
        actions = { TextButton(onClick = onSelectAll) { Text(if (isAllSelected) "取消全选" else "全选") } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    )
}

@Composable
private fun BatchActionBar(selectedCount: Int, onDelete: () -> Unit, onFavorite: () -> Unit, onUnfavorite: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onFavorite)) {
                Icon(Icons.Default.Favorite, "收藏", tint = Color(0xFFE91E63), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text("收藏", style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onUnfavorite)) {
                Icon(Icons.Default.FavoriteBorder, "取消收藏", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text("取消收藏", style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onDelete)) {
                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text("删除", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    TopAppBar(
        title = {
            OutlinedTextField(value = query, onValueChange = onQueryChange, placeholder = { Text("搜索物品名称或分类") }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent), modifier = Modifier.fillMaxWidth())
        },
        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, "关闭搜索") } },
        actions = { if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Clear, "清除") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(selectedFilter: HistoryFilter, categories: List<String>, onFilterSelected: (HistoryFilter) -> Unit) {
    LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { FilterChip(selected = selectedFilter == HistoryFilter.All, onClick = { onFilterSelected(HistoryFilter.All) }, label = { Text("全部") }) }
        item { FilterChip(selected = selectedFilter == HistoryFilter.Favorites, onClick = { onFilterSelected(HistoryFilter.Favorites) }, label = { Text("收藏") }) }
        items(categories) { category -> FilterChip(selected = selectedFilter is HistoryFilter.Category && selectedFilter.name == category, onClick = { onFilterSelected(HistoryFilter.Category(category)) }, label = { Text(category) }) }
    }
}

@Composable
fun HistoryList(groupedHistory: Map<String, List<HistoryItem>>, isSelectionMode: Boolean, selectedIds: Set<String>, onItemClick: (HistoryItem) -> Unit, onItemLongClick: (HistoryItem) -> Unit, onDeleteClick: (HistoryItem) -> Unit, onFavoriteClick: (HistoryItem) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        groupedHistory.forEach { (dateGroup, items) ->
            item { Text(dateGroup, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) }
            items(items, key = { it.id }) { item ->
                HistoryItemCard(item = item, isSelectionMode = isSelectionMode, isSelected = selectedIds.contains(item.id), onClick = { onItemClick(item) }, onLongClick = { onItemLongClick(item) }, onDeleteClick = { onDeleteClick(item) }, onFavoriteClick = { onFavoriteClick(item) })
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItemCard(item: HistoryItem, isSelectionMode: Boolean, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit, onDeleteClick: () -> Unit, onFavoriteClick: () -> Unit) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelectionMode) Checkbox(checked = isSelected, onCheckedChange = { onClick() }, modifier = Modifier.padding(end = 8.dp))
            
            ThumbnailImage(thumbnailPath = item.thumbnailPath, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (item.isFavorite) { Spacer(modifier = Modifier.width(6.dp)); Text("❤️", fontSize = 12.sp) }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                        Text(item.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Text(timeFormat.format(Date(item.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CompactQualityIndicator(confidence = item.confidence)
                }
            }
            
            if (!isSelectionMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onFavoriteClick, modifier = Modifier.size(40.dp)) {
                        Icon(if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (item.isFavorite) "取消收藏" else "收藏", tint = if (item.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ThumbnailImage(thumbnailPath: String?, modifier: Modifier = Modifier) {
    if (thumbnailPath != null && File(thumbnailPath).exists()) {
        val bitmap = remember(thumbnailPath) { try { BitmapFactory.decodeFile(thumbnailPath) } catch (e: Exception) { null } }
        if (bitmap != null) Image(bitmap.asImageBitmap(), "缩略图", modifier, contentScale = ContentScale.Crop)
        else PlaceholderThumbnail(modifier)
    } else PlaceholderThumbnail(modifier)
}

@Composable
private fun PlaceholderThumbnail(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
    }
}

@Composable
fun EmptyHistoryView(modifier: Modifier = Modifier, isFiltered: Boolean = false) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (isFiltered) "🔍" else "📭", fontSize = 56.sp)
        Text(if (isFiltered) "没有找到匹配的记录" else "暂无历史记录", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (isFiltered) "试试其他搜索词或筛选条件" else "识别物品后会自动保存到这里", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("😕", fontSize = 56.sp)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        FilledTonalButton(onClick = onRetry) { Text("重试") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailSheet(item: HistoryItem, onDismiss: () -> Unit, onDelete: () -> Unit, onFavorite: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            if (item.thumbnailPath != null) {
                ThumbnailImage(thumbnailPath = item.thumbnailPath, modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)))
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(item.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onFavorite) {
                    Icon(if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (item.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (item.aliases.isNotEmpty()) DetailCard("别名", item.aliases.joinToString("、"))
                DetailCard("来历", item.origin)
                DetailCard("用途", item.usage)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailCard("分类", item.category, Modifier.weight(1f))
                    DetailCard("置信度", "${(item.confidence * 100).toInt()}%", Modifier.weight(1f))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除") }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("关闭") }
            }
        }
    }
}

@Composable
fun DetailCard(label: String, value: String, modifier: Modifier = Modifier) {
    val displayValue = if (value.isBlank() || value.lowercase() == "null") "暂无" else value
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(displayValue, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

sealed class HistoryFilter {
    data object All : HistoryFilter()
    data object Favorites : HistoryFilter()
    data class Category(val name: String) : HistoryFilter()
}
