package com.wjp.jellyfinduanju.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * SearchableTopBar - 带搜索功能的通用顶栏
 * 
 * @param searchQuery 当前搜索关键词
 * @param onSearchQueryChange 搜索关键词变化回调
 * @param onClearSearch 清除搜索回调
 * @param placeholder 搜索框占位符文本
 * @param modifier 布局修饰符
 * @param actions 右侧操作按钮插槽，允许调用者自定义按钮
 * 
 * 说明：
 * - 提供统一样式的搜索框
 * - 通过 actions 插槽支持不同页面的自定义按钮
 * - 自动处理焦点管理和键盘操作
 * 
 * 使用示例：
 * ```
 * SearchableTopBar(
 *     searchQuery = uiState.searchQuery,
 *     onSearchQueryChange = { viewModel.updateSearchQuery(it) },
 *     onClearSearch = { viewModel.clearSearch() }
 * ) {
 *     // 自定义右侧按钮
 *     IconButton(onClick = { ... }) {
 *         Icon(Icons.Default.Sort, "排序")
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableTopBar(
    searchQuery: String,  // 当前搜索关键词
    onSearchQueryChange: (String) -> Unit,  // 搜索关键词变化回调
    onClearSearch: () -> Unit,  // 清除搜索回调
    placeholder: String = "搜索",  // 搜索框占位符文本
    modifier: Modifier = Modifier,  // 布局修饰符
    actions: @Composable RowScope.() -> Unit = {}  // 右侧操作按钮插槽
) {
    val focusManager = LocalFocusManager.current  // 获取焦点管理器，用于控制键盘收起
    
    TopAppBar(
        modifier = modifier,  // 应用修饰符
        title = {  // 标题区域（搜索框）
            SearchField(
                searchQuery = searchQuery,  // 当前搜索文本
                onSearchQueryChange = onSearchQueryChange,  // 文本变化回调
                onClearSearch = onClearSearch,  // 清除回调
                placeholder = placeholder,  // 占位符文本
                focusManager = focusManager  // 焦点管理器
            )
        },
        actions = actions  // 右侧操作按钮（由调用者提供）
    )
}

/**
 * SearchField - 搜索输入框
 * 
 * @param searchQuery 当前搜索文本
 * @param onSearchQueryChange 文本变化回调
 * @param onClearSearch 清除按钮回调
 * @param placeholder 占位符文本
 * @param focusManager 焦点管理器
 */
@Composable
private fun SearchField(
    searchQuery: String,  // 当前搜索文本
    onSearchQueryChange: (String) -> Unit,  // 文本变化回调
    onClearSearch: () -> Unit,  // 清除按钮回调
    placeholder: String,  // 占位符文本
    focusManager: FocusManager  // 焦点管理器
) {
    OutlinedTextField(
        value = searchQuery,  // 当前输入框的值
        onValueChange = onSearchQueryChange,  // 当用户输入时触发
        modifier = Modifier
            .fillMaxWidth()  // 填充整个宽度
            .padding(end = 8.dp),  // 右侧留出 8dp 间距（与右侧按钮保持距离）
        placeholder = { SearchPlaceholder(placeholder) },  // 占位符（输入框为空时显示）
        leadingIcon = { SearchLeadingIcon() },  // 左侧图标（搜索图标）
        trailingIcon = { SearchTrailingIcon(searchQuery, onClearSearch) },  // 右侧图标（清除按钮，有内容时显示）
        singleLine = true,  // 单行输入
        shape = RoundedCornerShape(28.dp),  // 圆角矩形（28dp 圆角）
        colors = TextFieldDefaults.colors(  // 颜色配置
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,  // 获得焦点时的背景色
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,  // 失去焦点时的背景色
            focusedIndicatorColor = Color.Transparent,  // 隐藏获得焦点时的下划线
            unfocusedIndicatorColor = Color.Transparent  // 隐藏失去焦点时的下划线
        ),
        textStyle = MaterialTheme.typography.bodyMedium,  // 文本样式
        keyboardOptions = KeyboardOptions(  // 键盘选项配置
            imeAction = ImeAction.Search  // 键盘右下角显示"搜索"按钮
        ),
        keyboardActions = KeyboardActions(  // 键盘操作配置
            onSearch = { focusManager.clearFocus() }  // 点击"搜索"按钮时收起键盘
        )
    )
}

/**
 * SearchPlaceholder - 搜索框占位符
 * 
 * @param text 占位符文本
 */
@Composable
private fun SearchPlaceholder(text: String) {  // 占位符文本
    Text(
        text = text,  // 显示的文本内容
        style = MaterialTheme.typography.bodyMedium  // 使用中等字体样式
    )
}

/**
 * SearchLeadingIcon - 搜索框左侧图标
 */
@Composable
private fun SearchLeadingIcon() {
    Icon(
        imageVector = Icons.Default.Search,  // 搜索图标 🔍
        contentDescription = "搜索",  // 无障碍描述
        tint = MaterialTheme.colorScheme.onSurfaceVariant  // 图标颜色（灰色）
    )
}

/**
 * SearchTrailingIcon - 搜索框右侧清除按钮
 * 
 * @param searchQuery 当前搜索文本
 * @param onClearSearch 清除回调
 */
@Composable
private fun SearchTrailingIcon(
    searchQuery: String,  // 当前搜索文本
    onClearSearch: () -> Unit  // 清除回调
) {
    // 仅当有输入内容时显示清除按钮
    if (searchQuery.isNotEmpty()) {
        IconButton(onClick = onClearSearch) {  // 点击清除按钮
            Icon(
                imageVector = Icons.Default.Clear,  // 清除图标 ✕
                contentDescription = "清除",  // 无障碍描述
                tint = MaterialTheme.colorScheme.onSurfaceVariant  // 图标颜色（灰色）
            )
        }
    }
}
