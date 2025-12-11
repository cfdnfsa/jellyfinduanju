package com.wjp.jellyfinduanju.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wjp.jellyfinduanju.data.repository.AuthRepository

/**
 * SettingsViewModelFactory - SettingsViewModel 的工厂类
 * 
 * 用于创建带有 AuthRepository 依赖的 SettingsViewModel
 */
class SettingsViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

