package com.wjp.jellyfinduanju.ui.components.player

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * PlayerGestureCallbacks - 播放器手势回调封装
 * 
 * 将播放器相关的手势回调封装到一个数据类中，
 * 简化手势处理的参数传递
 * 
 * @param onTap 单击回调（横屏：切换控制栏，竖屏：播放/暂停）
 * @param onLongPressStart 长按开始回调（开始 3x 倍速）
 * @param onLongPressEnd 长按结束回调（恢复正常倍速）
 * @param onSeekStart 拖动开始回调（暂停播放）
 * @param onSeekEnd 拖动结束回调（跳转并恢复播放）
 */
data class PlayerGestureCallbacks(
    val onTap: () -> Unit = {},
    val onLongPressStart: () -> Unit = {},
    val onLongPressEnd: () -> Unit = {},
    val onSeekStart: () -> Unit = {},
    val onSeekEnd: (Long?) -> Unit = {}
)

/**
 * PlayerGestureState - 播放器手势状态
 * 
 * 封装手势处理过程中的状态数据
 * 
 * @property isDragging 是否正在拖动
 * @property dragTargetPosition 拖动的目标位置
 */
data class PlayerGestureState(
    val isDragging: Boolean,
    val dragTargetPosition: Long
)

/**
 * playerGestures - 播放器手势处理 Modifier
 * 
 * 统一封装播放器的手势处理逻辑，包括：
 * - 单击：切换控制栏或播放/暂停
 * - 长按：3x 倍速播放
 * - 水平滑动：调节播放进度
 * 
 * 使用场景：
 * - 横屏播放器
 * - 竖屏播放器
 * - 任何需要视频播放手势的地方
 * 
 * @param currentPosition 当前播放位置（毫秒）
 * @param duration 视频总时长（毫秒）
 * @param callbacks 手势回调封装
 * @param onGestureStateChanged 手势状态变化回调（用于更新 UI，如显示拖动进度）
 * 
 * 使用示例：
 * ```
 * var gestureState by remember { mutableStateOf(PlayerGestureState(false, 0L)) }
 * 
 * Box(
 *     modifier = Modifier.playerGestures(
 *         currentPosition = uiState.currentPosition,
 *         duration = uiState.duration,
 *         callbacks = PlayerGestureCallbacks(
 *             onTap = { ... },
 *             onLongPressStart = { ... },
 *             onLongPressEnd = { ... },
 *             onSeekStart = { ... },
 *             onSeekEnd = { ... }
 *         ),
 *         onGestureStateChanged = { gestureState = it }
 *     )
 * ) {
 *     // 视频内容
 * }
 * 
 * // 显示拖动进度
 * DragProgressIndicator(
 *     targetPosition = gestureState.dragTargetPosition,
 *     duration = duration,
 *     visible = gestureState.isDragging
 * )
 * ```
 */
@Composable
fun Modifier.playerGestures(
    currentPosition: Long,
    duration: Long,
    callbacks: PlayerGestureCallbacks,
    onGestureStateChanged: (PlayerGestureState) -> Unit
): Modifier {
    // 长按状态（用于检测长按结束）
    var isLongPressing by remember { mutableStateOf(false) }
    
    // 拖动状态
    var isDragging by remember { mutableStateOf(false) }
    var dragTargetPosition by remember { mutableStateOf(0L) }
    
    // 更新手势状态到外部
    val updateGestureState = {
        onGestureStateChanged(
            PlayerGestureState(
                isDragging = isDragging,
                dragTargetPosition = dragTargetPosition
            )
        )
    }
    
    return this
        // 手势1：点击和长按
        .pointerInput(Unit) {
            detectTapGestures(
                // 单击：切换控制栏或播放/暂停
                onTap = { callbacks.onTap() },
                // 长按：开始 3x 倍速
                onLongPress = {
                    isLongPressing = true
                    callbacks.onLongPressStart()
                },
                // 按压：用于检测长按结束
                onPress = {
                    tryAwaitRelease()  // 等待手指抬起
                    if (isLongPressing) {
                        isLongPressing = false
                        callbacks.onLongPressEnd()
                    }
                }
            )
        }
        // 手势2：水平滑动调节进度
        .pointerInput(duration, currentPosition) {
            detectHorizontalDragGestures(
                // 开始拖动
                onDragStart = {
                    isDragging = true
                    dragTargetPosition = currentPosition
                    updateGestureState()
                    callbacks.onSeekStart()  // 通知暂停播放
                },
                // 结束拖动
                onDragEnd = {
                    if (isDragging) {
                        val targetPos = dragTargetPosition
                        isDragging = false
                        updateGestureState()
                        callbacks.onSeekEnd(targetPos)  // 跳转并恢复播放
                    }
                },
                // 取消拖动
                onDragCancel = {
                    if (isDragging) {
                        isDragging = false
                        updateGestureState()
                        callbacks.onSeekEnd(null)  // 不跳转，只恢复播放
                    }
                },
                // 拖动中
                onHorizontalDrag = { _, dragAmount ->
                    if (duration > 0) {
                        // 每滑动 1px 调整 100ms 进度
                        val seekAmount = (dragAmount * 100).toLong()
                        dragTargetPosition = (dragTargetPosition + seekAmount)
                            .coerceIn(0, duration)
                        updateGestureState()
                    }
                }
            )
        }
}

