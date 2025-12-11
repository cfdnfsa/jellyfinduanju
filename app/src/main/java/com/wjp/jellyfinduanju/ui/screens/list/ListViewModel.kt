package com.wjp.jellyfinduanju.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wjp.jellyfinduanju.data.model.SeriesLocalData
import com.wjp.jellyfinduanju.data.repository.SeriesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ListViewModel - 剧集列表页面的 ViewModel
 * 
 * 职责：
 * 1. 管理剧集列表的 UI 状态
 * 2. 处理刷新操作
 * 3. 处理搜索筛选
 * 4. 提供图片 URL
 * 
 * @property seriesRepository 剧集数据仓库
 */
class ListViewModel(
    private val seriesRepository: SeriesRepository
) : ViewModel() {
    
    // ==================== UI 状态 ====================
    
    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()
    
    // 完整的剧集列表（用于搜索筛选）
    private var allSeriesList: List<SeriesLocalData> = emptyList()
    
    // ==================== 初始化 ====================
    
    init {
        // 加载本地缓存的剧集列表
        loadLocalSeries()
        
        // 订阅剧集列表变化
        viewModelScope.launch {
            seriesRepository.seriesListFlow.collect { list ->
                allSeriesList = list
                // 应用当前搜索条件
                applySearchFilter()
            }
        }
    }
    
    /**
     * 加载本地缓存的剧集列表
     */
    private fun loadLocalSeries() {
        allSeriesList = seriesRepository.getLocalSeriesList()
        applySearchFilter()
    }
    
    // ==================== 搜索功能 ====================
    
    /**
     * 更新搜索关键词
     * 
     * @param query 搜索关键词
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applySearchFilter()
    }
    
    /**
     * 清除搜索
     */
    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "") }
        applySearchFilter()
    }
    
    /**
     * 应用搜索筛选
     * 
     * 根据搜索关键词筛选剧集列表
     * 支持模糊搜索：匹配剧集名称
     */
    private fun applySearchFilter() {
        val query = _uiState.value.searchQuery.trim()
        
        val filteredList = if (query.isEmpty()) {
            allSeriesList
        } else {
            allSeriesList.filter { series ->
                // 模糊匹配剧集名称（不区分大小写）
                series.name.contains(query, ignoreCase = true)
            }
        }
        
        _uiState.update { 
            it.copy(
                seriesList = filteredList,
                isEmpty = filteredList.isEmpty() && !it.isLoading
            ) 
        }
    }
    
    // ==================== 刷新操作 ====================
    
    /**
     * 刷新剧集列表
     * 
     * 从服务器获取最新的剧集列表
     * 用户点击刷新按钮时调用
     */
    fun refreshSeriesList() {
        // 避免重复刷新
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = seriesRepository.refreshSeriesList()
            
            result.fold(
                onSuccess = { list ->
                    allSeriesList = list
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    // 应用搜索筛选
                    applySearchFilter()
                },
                onFailure = { exception ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "刷新失败"
                        ) 
                    }
                }
            )
        }
    }
    
    // ==================== 图片 URL ====================
    
    /**
     * 获取剧集图片 URL
     * 
     * 优先使用本地缓存，没有则返回远程 URL
     * 
     * @param seriesId 剧集 ID
     * @param callback 回调，返回图片 URL
     */
    fun getSeriesImageUrl(seriesId: String, callback: (String?) -> Unit) {
        viewModelScope.launch {
            val url = seriesRepository.getSeriesImageUrl(seriesId)
            callback(url)
        }
    }
    
    // ==================== 错误处理 ====================
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * ListUiState - 剧集列表页面的 UI 状态
 * 
 * @property isLoading 是否正在加载
 * @property seriesList 剧集列表（已筛选）
 * @property isEmpty 列表是否为空
 * @property error 错误信息
 * @property searchQuery 搜索关键词
 */
data class ListUiState(
    val isLoading: Boolean = false,
    val seriesList: List<SeriesLocalData> = emptyList(),
    val isEmpty: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)

/**
 * ListViewModelFactory - ListViewModel 的工厂类
 * 
 * 用于创建带参数的 ViewModel
 */
class ListViewModelFactory(
    private val seriesRepository: SeriesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListViewModel::class.java)) {
            return ListViewModel(seriesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
