package com.wjp.jellyfinduanju.navigation.routes

/**
 * AppRoutes - 应用路由定义
 * 
 * 集中管理所有非底部导航的路由路径
 * 底部导航的路由在 BottomNavItem 中定义
 */
object AppRoutes {
    // ==================== 设置子页面路由 ====================
    
    /**
     * 缓存设置页面
     */
    const val CACHE_SETTINGS = "cache_settings"
    
    /**
     * 标签管理页面
     */
    const val TAG_MANAGE = "tag_manage"
    
    /**
     * 本地文件夹管理页面
     */
    const val LOCAL_FOLDER_MANAGE = "local_folder_manage"
    
    // ==================== 带参数的路由（模板格式） ====================
    
    /**
     * 剧集详情页路由模板
     * 参数：{seriesId} - 剧集ID
     */
    const val SERIES_DETAIL = "series_detail/{seriesId}"
    
    /**
     * 播放器页面路由模板
     * 参数：{seriesId} - 剧集ID，{episodeId} - 集数ID
     */
    const val PLAYER = "player/{seriesId}/{episodeId}"
    
    // ==================== 路由构建辅助函数 ====================
    
    /**
     * 构建剧集详情页路由（填充参数）
     * @param seriesId 剧集ID
     * @return 完整的路由路径
     */
    fun seriesDetail(seriesId: String) = "series_detail/$seriesId"
    
    /**
     * 构建播放器页面路由（填充参数）
     * @param seriesId 剧集ID
     * @param episodeId 集数ID
     * @return 完整的路由路径
     */
    fun player(seriesId: String, episodeId: String) = "player/$seriesId/$episodeId"
}
