package com.wjp.jellyfinduanju.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wjp.jellyfinduanju.data.repository.AuthRepository

/**
 * LoginViewModelFactory - ViewModel 工厂类
 * 
 * 为什么需要 Factory？
 * - 默认情况下，ViewModel 只能有无参构造函数
 * - 如果 ViewModel 需要依赖（如 Repository），就需要 Factory
 * 
 * 使用方式：
 * ```
 * val viewModel: LoginViewModel = viewModel(
 *     factory = LoginViewModelFactory(authRepository)
 * )
 * ```
 * 
 * 注意：使用 Hilt 可以自动处理 ViewModel 注入，无需手动创建 Factory
 */
class LoginViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // 检查请求的 ViewModel 类型是否是 LoginViewModel
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

