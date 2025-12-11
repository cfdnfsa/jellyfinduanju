package com.wjp.jellyfinduanju.ui.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wjp.jellyfinduanju.ui.theme.JellyfinDuanjuTheme

/**
 * LoginScreen.kt - 登录页面 UI
 * 
 * 使用 Jetpack Compose 构建的登录界面
 * 
 * 页面流程：
 * 1. 输入服务器地址 -> 验证服务器
 * 2. 输入用户名密码 -> 登录
 * 3. 登录成功 -> 导航到主页
 * 
 * 组件说明：
 * - OutlinedTextField：Material 3 的输入框
 * - Button：主要操作按钮
 * - Card：卡片容器
 * - AnimatedVisibility：带动画的显示/隐藏
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,       // ViewModel：管理页面状态和业务逻辑
    onLoginSuccess: () -> Unit       // 回调函数：登录成功后通知外部（用于页面跳转）
) {
    // ==================== 状态订阅 ====================
    // 
    // collectAsState() 做了两件事：
    // 1. 订阅 ViewModel 中的 StateFlow，持续监听状态变化
    // 2. 当状态变化时，自动触发 UI 重组（重新执行这个函数）
    // 
    // 使用 by 委托语法，可以直接用 uiState 而不是 uiState.value
    val uiState by viewModel.uiState.collectAsState()
    
    // ==================== 记住的对象 ====================
    // 
    // remember { } 的作用：
    // - 首次执行时创建对象并缓存
    // - 后续重组时直接返回缓存的对象，不会重新创建
    // 
    // Snackbar 状态：控制底部提示条的显示
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 焦点管理器：用于控制输入框焦点、隐藏键盘等
    // LocalFocusManager.current 是 Compose 提供的"环境变量"
    val focusManager = LocalFocusManager.current
    
    // ==================== 副作用：监听状态变化执行操作 ====================
    // 
    // LaunchedEffect(key) { } 的工作方式：
    // - 当 key 的值发生变化时，执行 { } 中的代码
    // - 内部是协程环境，可以调用 suspend 函数
    // - 当 key 没变化时，重组不会重新执行里面的代码
    //
    // 监听登录成功状态
    // key = uiState.isLoginSuccess，当它从 false 变为 true 时触发
    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            onLoginSuccess()  // 调用回调，通知外部登录成功（跳转到主页）
        }
    }
    
    // 监听错误信息
    // key = uiState.error，当它从 null 变为有值时触发
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)  // 显示错误提示（这是 suspend 函数）
            viewModel.clearError()                  // 清除错误，避免重复显示
        }
    }
    
    // ==================== UI 布局 ====================
    // 
    // Box：层叠布局容器，子元素可以堆叠在一起
    // 这里用 Box 是为了让 Snackbar 可以悬浮在底部
    Box(
        modifier = Modifier
            .fillMaxSize()    // 填满整个屏幕
            .imePadding()     // 键盘弹出时自动调整布局，避免遮挡输入框
    ) {
        // Column：垂直排列的布局容器
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())  // 支持垂直滚动（内容超出屏幕时）
                .padding(24.dp),                        // 内边距 24dp
            horizontalAlignment = Alignment.CenterHorizontally,  // 子元素水平居中
            verticalArrangement = Arrangement.Center             // 子元素垂直居中
        ) {
            // ==================== Logo 区域 ====================
            
            // 云朵图标
            Icon(
                imageVector = Icons.Default.Cloud,      // Material 图标
                contentDescription = null,              // 无障碍描述（装饰性图标可为 null）
                modifier = Modifier.size(80.dp),        // 图标大小
                tint = MaterialTheme.colorScheme.primary // 使用主题主色
            )
            
            // Spacer：空白间距
            Spacer(modifier = Modifier.height(16.dp))
            
            // 应用标题
            Text(
                text = "Jellyfin 短剧",
                style = MaterialTheme.typography.headlineMedium,  // 使用主题中定义的标题样式
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 副标题
            Text(
                text = "连接你的 Jellyfin 服务器",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant  // 次要文字颜色
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // ==================== 输入区域：两个互斥的卡片 ====================
            // 
            // AnimatedVisibility 根据 visible 参数控制子组件的显示/隐藏
            // - visible = true  → 显示（带进入动画）
            // - visible = false → 隐藏（带退出动画）
            //
            // 这里用两个 AnimatedVisibility 实现"服务器输入"和"登录输入"的切换
            
            // 卡片 1：服务器地址输入（未验证服务器时显示）
            AnimatedVisibility(
                visible = !uiState.isServerValidated,   // 服务器未验证时显示
                enter = fadeIn() + slideInVertically(), // 进入动画：淡入 + 从上滑入
                exit = fadeOut() + slideOutVertically() // 退出动画：淡出 + 向上滑出
            ) {
                // ServerInputCard 组件的参数说明：
                ServerInputCard(
                    // 【数据参数】从 ViewModel 获取，用于显示
                    serverUrl = uiState.serverUrl,      // 输入框显示的服务器地址
                    isLoading = uiState.isLoading,      // 是否正在加载（显示加载圈）
                    
                    // 【回调参数】用户操作时调用，通知 ViewModel
                    onServerUrlChange = viewModel::updateServerUrl,  // 用户输入时更新地址
                    // viewModel::updateServerUrl 等价于 { text -> viewModel.updateServerUrl(text) }
                    
                    onValidate = {
                        // 用户点击"连接服务器"按钮时执行
                        focusManager.clearFocus()       // 1. 隐藏键盘
                        viewModel.validateServer()      // 2. 验证服务器
                    }
                )
            }
            
            // 卡片 2：用户名密码输入（已验证服务器时显示）
            AnimatedVisibility(
                visible = uiState.isServerValidated,    // 服务器已验证时显示
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                // CredentialsInputCard 组件的参数说明：
                CredentialsInputCard(
                    // 【数据参数】
                    serverName = uiState.serverConfig?.serverName ?: "",  // 服务器名称
                    username = uiState.username,        // 用户名输入框内容
                    password = uiState.password,        // 密码输入框内容
                    isLoading = uiState.isLoading,      // 是否正在加载
                    
                    // 【回调参数】
                    onUsernameChange = viewModel::updateUsername,  // 用户名变化时
                    onPasswordChange = viewModel::updatePassword,  // 密码变化时
                    onLogin = {
                        // 用户点击"登录"按钮时执行
                        focusManager.clearFocus()       // 1. 隐藏键盘
                        viewModel.login()               // 2. 执行登录
                    },
                    onBack = viewModel::backToServerInput  // 用户点击"返回"时，回到服务器输入
                )
            }
        }
        
        // ==================== Snackbar：底部提示条 ====================
        // 
        // SnackbarHost 是 Snackbar 的容器
        // 当调用 snackbarHostState.showSnackbar() 时，会在这里显示提示
        SnackbarHost(
            hostState = snackbarHostState,                    // 关联状态
            modifier = Modifier.align(Alignment.BottomCenter) // 底部居中
        ) { snackbarData ->
            // 自定义 Snackbar 样式（错误提示用红色背景）
            Snackbar(
                snackbarData = snackbarData,
                containerColor = MaterialTheme.colorScheme.errorContainer,   // 错误背景色
                contentColor = MaterialTheme.colorScheme.onErrorContainer    // 错误文字色
            )
        }
    }
}

/**
 * ServerInputCard - 服务器地址输入卡片组件
 * 
 * 这是一个"无状态组件"（Stateless Component）：
 * - 自己不管理任何状态
 * - 所有数据通过参数传入
 * - 所有用户操作通过回调传出
 * 
 * 这种设计的好处：
 * 1. 组件可复用、可测试
 * 2. 状态集中在 ViewModel 管理
 * 3. 数据流向清晰（单向数据流）
 * 
 * @param serverUrl       【数据↓】输入框显示的服务器地址
 * @param isLoading       【数据↓】是否正在加载（true 时显示加载圈，禁用输入）
 * @param onServerUrlChange 【事件↑】用户输入时回调，参数是新的输入内容
 * @param onValidate      【事件↑】用户点击"连接服务器"按钮时回调
 */
@Composable
private fun ServerInputCard(
    serverUrl: String,                    // 数据：服务器地址
    onServerUrlChange: (String) -> Unit,  // 回调：输入变化时调用
    onValidate: () -> Unit,               // 回调：点击连接按钮时调用
    isLoading: Boolean                    // 数据：是否加载中
) {
    // ==================== Card 容器 ====================
    // Card 是 Material Design 的卡片组件，带有圆角和阴影效果
    Card(
        modifier = Modifier.fillMaxWidth(),  // 宽度填满父容器
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface  // 使用主题的表面色
        )
    ) {
        // Column：垂直排列子元素
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)  // 内边距 20dp
        ) {
            // ==================== 标题区域 ====================
            
            // 主标题
            Text(
                text = "服务器地址",
                style = MaterialTheme.typography.titleMedium,  // 中等标题样式
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))  // 4dp 间距
            
            // 副标题/说明文字
            Text(
                text = "输入 Jellyfin 服务器的地址",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant  // 次要文字颜色
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ==================== 输入框 ====================
            // 
            // OutlinedTextField 是 Material 3 的带边框输入框
            // 
            // 关键参数：
            // - value：显示的内容（从外部传入）
            // - onValueChange：内容变化时的回调（通知外部）
            // 
            // 这就是 Compose 的"受控组件"模式：
            // 输入框不自己管理内容，而是由外部控制
            //
            OutlinedTextField(
                value = serverUrl,                    // 显示的内容（来自 ViewModel）
                onValueChange = onServerUrlChange,    // 用户输入时，调用回调通知外部
                modifier = Modifier.fillMaxWidth(),
                label = { Text("服务器地址") },       // 浮动标签
                placeholder = { Text("") },  // 占位提示
                leadingIcon = {                       // 左侧图标
                    Icon(Icons.Default.Cloud, contentDescription = null)
                },
                singleLine = true,                    // 单行输入
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,  // 键盘类型：URL
                    imeAction = ImeAction.Done        // 键盘右下角按钮：完成
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onValidate() }         // 点击键盘"完成"时，执行验证
                ),
                enabled = !isLoading                  // 加载时禁用输入
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // ==================== 连接按钮 ====================
            //
            // Button 的 enabled 参数控制按钮是否可点击
            // 这里要求：地址不为空 且 没在加载中
            //
            Button(
                onClick = onValidate,                 // 点击时调用验证回调
                modifier = Modifier.fillMaxWidth(),   // 宽度填满
                enabled = serverUrl.isNotEmpty() && !isLoading  // 启用条件
            ) {
                // 按钮内容：根据加载状态显示不同内容
                if (isLoading) {
                    // 加载中：显示转圈圈
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,  // 白色
                        strokeWidth = 2.dp
                    )
                } else {
                    // 正常状态：显示文字
                    Text("连接服务器")
                }
            }
        }
    }
}

/**
 * CredentialsInputCard - 用户名密码输入卡片组件
 * 
 * 这个组件比 ServerInputCard 复杂一些，因为：
 * 1. 有自己的内部状态（密码可见性）
 * 2. 需要管理焦点（自动聚焦、焦点切换）
 * 
 * 参数设计原则：
 * - 数据参数（↓）：从父组件传入，用于显示
 * - 回调参数（↑）：用户操作时调用，通知父组件
 * 
 * @param serverName      【数据↓】已连接的服务器名称
 * @param username        【数据↓】用户名输入框内容
 * @param password        【数据↓】密码输入框内容
 * @param isLoading       【数据↓】是否正在加载
 * @param onUsernameChange【事件↑】用户名变化时回调
 * @param onPasswordChange【事件↑】密码变化时回调
 * @param onLogin         【事件↑】点击登录按钮时回调
 * @param onBack          【事件↑】点击返回按钮时回调
 */
@Composable
private fun CredentialsInputCard(
    serverName: String,                   // 服务器名称（显示用）
    username: String,                     // 用户名（双向绑定）
    password: String,                     // 密码（双向绑定）
    onUsernameChange: (String) -> Unit,   // 用户名变化回调
    onPasswordChange: (String) -> Unit,   // 密码变化回调
    onLogin: () -> Unit,                  // 登录按钮回调
    onBack: () -> Unit,                   // 返回按钮回调
    isLoading: Boolean                    // 加载状态
) {
    // ==================== 组件内部状态 ====================
    //
    // 这些状态只在这个组件内部使用，不需要暴露给外部
    // 所以用 remember + mutableStateOf 在本地管理
    //
    
    // 密码是否可见（点击眼睛图标切换）
    // 使用 by 委托，可以直接读写 passwordVisible 而不是 passwordVisible.value
    var passwordVisible by remember { mutableStateOf(false) }
    
    // ==================== 焦点管理 ====================
    //
    // FocusRequester 用于程序化地控制焦点
    // 例如：自动聚焦到某个输入框、点击"下一步"时跳到下一个输入框
    //
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    
    // 组件显示时自动聚焦用户名输入框
    // LaunchedEffect(Unit) 表示只在首次组合时执行一次
    LaunchedEffect(Unit) {
        usernameFocusRequester.requestFocus()  // 请求焦点，键盘会自动弹出
    }
    
    // ==================== UI 布局 ====================
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // ==================== 返回按钮 ====================
            //
            // TextButton 是文字按钮，没有背景色
            // 用于次要操作
            //
            TextButton(
                onClick = onBack,           // 点击时调用返回回调
                enabled = !isLoading        // 加载时禁用
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,  // 返回箭头图标
                    contentDescription = "返回",          // 无障碍描述
                    modifier = Modifier.size(18.dp)
                )
                Text("更换服务器")
            }
            
            // ==================== 服务器连接成功提示 ====================
            //
            // 嵌套的 Card，用不同的背景色表示"成功"状态
            //
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer  // 主色容器色
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Row：水平排列子元素
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically  // 垂直居中对齐
                ) {
                    // 成功图标（打勾）
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    // 服务器信息
                    Column {
                        Text(
                            text = "已连接到",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = serverName,  // 显示服务器名称
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 登录标题
            Text(
                text = "登录账号",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ==================== 用户名输入框 ====================
            OutlinedTextField(
                value = username,                     // 显示内容（来自外部）
                onValueChange = onUsernameChange,     // 变化时通知外部
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(usernameFocusRequester),  // 关联焦点请求器
                label = { Text("用户名") },
                leadingIcon = {                       // 左侧图标：人物
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next        // 键盘右下角按钮：下一步
                ),
                keyboardActions = KeyboardActions(
                    onNext = { 
                        // 点击"下一步"时，焦点跳转到密码输入框
                        passwordFocusRequester.requestFocus() 
                    }
                ),
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // ==================== 密码输入框 ====================
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocusRequester),
                label = { Text("密码") },
                leadingIcon = {                       // 左侧图标：锁
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {                      // 右侧图标：密码可见性切换
                    // IconButton 是图标按钮，点击区域更大
                    IconButton(onClick = { 
                        passwordVisible = !passwordVisible  // 切换可见性
                    }) {
                        Icon(
                            // 根据可见性显示不同图标
                            imageVector = if (passwordVisible) {
                                Icons.Default.VisibilityOff  // 眼睛划掉 = 隐藏密码
                            } else {
                                Icons.Default.Visibility     // 眼睛 = 显示密码
                            },
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                        )
                    }
                },
                singleLine = true,
                // visualTransformation 控制文字的显示方式
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None           // 正常显示
                } else {
                    PasswordVisualTransformation()      // 显示为圆点 ••••••
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done          // 键盘右下角按钮：完成
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onLogin() }              // 点击"完成"时执行登录
                ),
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // ==================== 登录按钮 ====================
            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth(),
                // 启用条件：用户名不为空 且 密码不为空 且 没在加载中
                enabled = username.isNotEmpty() && password.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    // 加载中：显示转圈圈
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("登录")
                }
            }
        }
    }
}

// ==================== 预览 ====================

/**
 * 登录页面内容预览（用于 Android Studio 预览）
 * 
 * 由于 LoginScreen 需要 ViewModel，无法直接预览
 * 这里创建一个不需要 ViewModel 的预览版本
 */
@Composable
private fun LoginScreenContent(
    uiState: LoginUiState,
    onServerUrlChange: (String) -> Unit = {},
    onUsernameChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onValidateServer: () -> Unit = {},
    onLogin: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Jellyfin 短剧",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "连接你的 Jellyfin 服务器",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 根据状态显示不同的卡片
            if (!uiState.isServerValidated) {
                ServerInputCard(
                    serverUrl = uiState.serverUrl,
                    onServerUrlChange = onServerUrlChange,
                    onValidate = onValidateServer,
                    isLoading = uiState.isLoading
                )
            } else {
                CredentialsInputCard(
                    serverName = uiState.serverConfig?.serverName ?: "Jellyfin Server",
                    username = uiState.username,
                    password = uiState.password,
                    onUsernameChange = onUsernameChange,
                    onPasswordChange = onPasswordChange,
                    onLogin = onLogin,
                    onBack = onBack,
                    isLoading = uiState.isLoading
                )
            }
        }
    }
}

/**
 * 预览：服务器输入步骤（浅色主题）
 */
@Preview(
    name = "服务器输入 - 浅色",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun PreviewServerInputLight() {
    JellyfinDuanjuTheme(darkTheme = false) {
        LoginScreenContent(
            uiState = LoginUiState(
                serverUrl = "http://192.168.1.100:8096",
                isServerValidated = false
            )
        )
    }
}

/**
 * 预览：服务器输入步骤（深色主题）
 */
@Preview(
    name = "服务器输入 - 深色",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun PreviewServerInputDark() {
    JellyfinDuanjuTheme(darkTheme = true) {
        LoginScreenContent(
            uiState = LoginUiState(
                serverUrl = "",
                isServerValidated = false
            )
        )
    }
}

/**
 * 预览：登录输入步骤
 */
@Preview(
    name = "登录输入",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun PreviewCredentialsInput() {
    JellyfinDuanjuTheme(darkTheme = true) {
        LoginScreenContent(
            uiState = LoginUiState(
                serverUrl = "http://192.168.1.100:8096",
                isServerValidated = true,
                serverConfig = com.wjp.jellyfinduanju.data.model.ServerConfig(
                    serverUrl = "http://192.168.1.100:8096",
                    serverName = "我的 Jellyfin 服务器",
                    serverId = "abc123"
                ),
                username = "admin",
                password = ""
            )
        )
    }
}

/**
 * 预览：加载状态
 */
@Preview(
    name = "加载中",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun PreviewLoading() {
    JellyfinDuanjuTheme(darkTheme = true) {
        LoginScreenContent(
            uiState = LoginUiState(
                serverUrl = "http://192.168.1.100:8096",
                isServerValidated = false,
                isLoading = true
            )
        )
    }
}

/**
 * 预览：服务器输入卡片组件
 */
@Preview(
    name = "服务器输入卡片",
    showBackground = true,
    widthDp = 360
)
@Composable
private fun PreviewServerInputCard() {
    JellyfinDuanjuTheme {
        ServerInputCard(
            serverUrl = "http://192.168.1.100:8096",
            onServerUrlChange = {},
            onValidate = {},
            isLoading = false
        )
    }
}

/**
 * 预览：登录卡片组件
 */
@Preview(
    name = "登录卡片",
    showBackground = true,
    widthDp = 360
)
@Composable
private fun PreviewCredentialsInputCard() {
    JellyfinDuanjuTheme {
        CredentialsInputCard(
            serverName = "Jellyfin Server",
            username = "admin",
            password = "password",
            onUsernameChange = {},
            onPasswordChange = {},
            onLogin = {},
            onBack = {},
            isLoading = false
        )
    }
}
