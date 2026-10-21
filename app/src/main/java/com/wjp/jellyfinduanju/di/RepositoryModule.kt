package com.wjp.jellyfinduanju.di

import android.content.Context
import com.wjp.jellyfinduanju.data.local.AppDataStore
import com.wjp.jellyfinduanju.data.local.EpisodeDataStore
import com.wjp.jellyfinduanju.data.local.SeriesDataStore
import com.wjp.jellyfinduanju.data.repository.AuthRepository
import com.wjp.jellyfinduanju.data.repository.DetailRepository
import com.wjp.jellyfinduanju.data.repository.SeriesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * RepositoryModule - Hilt 模块：提供 Repository 层依赖
 * 
 * 职责：创建业务逻辑层的 Repository 对象
 * Repository 负责协调多个数据源（网络 API + 本地缓存）
 * 
 * ========== 注解说明 ==========
 * 
 * @Module + @InstallIn + @Provides + @Singleton
 * - 作用与 DataStoreModule 相同
 * - 区别在于：这里提供的是业务层对象（Repository）
 * - DataStoreModule 提供的是数据层对象（DataStore）
 * 
 * ========== 依赖注入链 ==========
 * 
 * Repository 通常依赖 DataStore：
 * 
 * 1. Hilt 先创建 DataStore（底层）
 *    └─ 通过 DataStoreModule 提供
 * 
 * 2. Hilt 再创建 Repository（业务层）
 *    └─ 自动注入 DataStore 作为构造参数
 *    └─ 通过 RepositoryModule 提供
 * 
 * 3. ViewModel 使用 Repository（表现层）
 *    └─ 自动注入 Repository
 * 
 * 依赖链：DataStore → Repository → ViewModel
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    /**
     * 提供 AuthRepository
     * 
     * 依赖注入演示：
     * - 方法参数 dataStore: AppDataStore 会被 Hilt 自动注入
     * - Hilt 查找如何创建 AppDataStore → 找到 DataStoreModule.provideAppDataStore()
     * - Hilt 先创建 AppDataStore，再传入这个方法
     * - 最后创建并返回 AuthRepository
     * 
     * 依赖链：AppDataStore → AuthRepository
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        dataStore: AppDataStore  // ← Hilt 自动注入（由 DataStoreModule 提供）
    ): AuthRepository {
        return AuthRepository(dataStore)
    }
    
    /**
     * 提供 SeriesRepository
     * 
     * 多依赖注入示例：
     * - 这个方法有 4 个参数，Hilt 会自动注入所有参数
     * - @ApplicationContext：Hilt 提供 Application Context
     * - seriesDataStore：DataStoreModule.provideSeriesDataStore() 提供
     * - episodeDataStore：DataStoreModule.provideEpisodeDataStore() 提供
     * - authRepository：本模块的 provideAuthRepository() 提供
     * 
     * Hilt 自动解决依赖顺序：
     * 1. 创建 AppDataStore
     * 2. 创建 AuthRepository（依赖 AppDataStore）
     * 3. 创建 SeriesDataStore 和 EpisodeDataStore
     * 4. 创建 SeriesRepository（依赖以上所有）
     */
    @Provides
    @Singleton
    fun provideSeriesRepository(
        @ApplicationContext context: Context,      // Hilt 自动提供
        seriesDataStore: SeriesDataStore,          // DataStoreModule 提供
        episodeDataStore: EpisodeDataStore,        // DataStoreModule 提供
        authRepository: AuthRepository             // 本模块提供
    ): SeriesRepository {
        return SeriesRepository(context, seriesDataStore, episodeDataStore, authRepository)
    }
    
    /**
     * 提供 DetailRepository
     * 依赖：AuthRepository, SeriesDataStore, EpisodeDataStore
     */
    @Provides
    @Singleton
    fun provideDetailRepository(
        authRepository: AuthRepository,      // 本模块提供
        seriesDataStore: SeriesDataStore,    // DataStoreModule 提供
        episodeDataStore: EpisodeDataStore   // DataStoreModule 提供
    ): DetailRepository {
        return DetailRepository(authRepository, seriesDataStore, episodeDataStore)
    }
}

