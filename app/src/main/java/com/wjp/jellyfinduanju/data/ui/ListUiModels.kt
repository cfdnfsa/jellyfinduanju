package com.wjp.jellyfinduanju.data.ui

import com.wjp.jellyfinduanju.data.model.SeriesLocalData

/**
 * ListUiModels.kt - 列表页面的 UI 层模型
 * 
 * 包含列表页面相关的枚举、数据类等 UI 状态定义
 */

/**
 * 视图模式
 */
enum class ViewMode {
    CLOUD,  // 云端剧集
    LOCAL   // 本地导入
}

/**
 * 对话框类型
 */
enum class DialogType {
    CACHE_ALL,           // 缓存全部
    DELETE_ALL_CACHE,    // 删除全部缓存
    SET_TAG,             // 设置标签
    DELETE_SERIES        // 删除剧集（包括列表、详情和所有缓存）
}

/**
 * 排序类型
 */
enum class SortType {
    NAME,           // 按名称
    UPDATE_TIME,    // 按更新时间
    PLAY_TIME       // 按播放时间
}

/**
 * ListUiState - 剧集列表页面的 UI 状态
 */
data class ListUiState(
    /** 是否正在加载数据（显示加载指示器） */
    val isLoading: Boolean = false,
    /** 剧集列表数据（经过筛选和排序后的列表） */
    val seriesList: List<SeriesLocalData> = emptyList(),
    /** 列表是否为空（用于显示空状态界面） */
    val isEmpty: Boolean = true,
    /** 错误信息（如果有错误则显示，通过 Snackbar 提示） */
    val error: String? = null,
    /** 搜索关键词（用户输入的搜索文本） */
    val searchQuery: String = "",
    /** 当前选中的剧集（用于显示菜单） */
    val selectedSeries: SeriesLocalData? = null,
    /** 当前显示的对话框类型 */
    val dialogType: DialogType? = null,
    /** 当前选中的筛选标签 */
    val selectedFilterTag: String = "全部",
    /** 排序类型 */
    val sortType: SortType = SortType.UPDATE_TIME,
    /** 排序方向：true = 升序, false = 降序 */
    val sortAscending: Boolean = false,
    /** 视图模式：云端 / 本地 */
    val viewMode: ViewMode = ViewMode.CLOUD
)

