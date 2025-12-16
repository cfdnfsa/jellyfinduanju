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
 * Repository 负责协调数据源（网络 API + 本地缓存）
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    /**
     * 提供 AuthRepository
     * 依赖：AppDataStore
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        dataStore: AppDataStore
    ): AuthRepository {
        return AuthRepository(dataStore)
    }
    
    /**
     * 提供 SeriesRepository
     * 依赖：Context, SeriesDataStore, EpisodeDataStore, AuthRepository
     */
    @Provides
    @Singleton
    fun provideSeriesRepository(
        @ApplicationContext context: Context,
        seriesDataStore: SeriesDataStore,
        episodeDataStore: EpisodeDataStore,
        authRepository: AuthRepository
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
        authRepository: AuthRepository,
        seriesDataStore: SeriesDataStore,
        episodeDataStore: EpisodeDataStore
    ): DetailRepository {
        return DetailRepository(authRepository, seriesDataStore, episodeDataStore)
    }
}

