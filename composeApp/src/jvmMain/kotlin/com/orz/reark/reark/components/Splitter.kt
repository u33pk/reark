package com.orz.reark.reark.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 可拖动的分隔线组件
 * @param isVertical 是否为垂直分隔线（true: 垂直分隔左右面板，false: 水平分隔上下面板）
 * @param thickness 分隔线厚度
 * @param onDrag 拖动回调，返回新的位置偏移量
 */
@Composable
fun Splitter(
    isVertical: Boolean = true,
    thickness: Dp = 4.dp,
    onDrag: (Float) -> Unit = {}
) {
    val backgroundColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val hoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    
    var isHovered by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .run {
                if (isVertical) {
                    this.width(thickness).fillMaxHeight()
                } else {
                    this.height(thickness).fillMaxWidth()
                }
            }
            .background(if (isHovered) hoverColor else backgroundColor)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isHovered = true },
                    onDragEnd = { isHovered = false },
                    onDragCancel = { isHovered = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (isVertical) {
                            onDrag(dragAmount.x)
                        } else {
                            onDrag(dragAmount.y)
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                // 鼠标悬停效果
                // 注意：在Compose Desktop中可能需要使用不同的方式检测悬停
                // 这里使用pointerInput模拟
            }
    )
}

/**
 * 带边框的面板容器
 * @param showBorder 是否显示边框
 * @param borderColor 边框颜色
 * @param borderWidth 边框宽度
 */
@Composable
fun BorderedPanel(
    modifier: Modifier = Modifier,
    showBorder: Boolean = true,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = if (showBorder) {
            modifier.background(borderColor, androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
                .padding(borderWidth)
                .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
        } else {
            modifier
        }
    ) {
        content()
    }
}
