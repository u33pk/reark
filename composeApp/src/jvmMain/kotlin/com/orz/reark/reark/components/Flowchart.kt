package com.orz.reark.reark.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

data class FlowchartNode(
    val id: String,
    val address: String,
    val instructions: List<String>,
    val nextBlock: String?,
    val jumpTarget: String?
)

data class FlowchartConnection(
    val fromId: String,
    val toId: String,
    val isJump: Boolean
)

@Composable
fun Flowchart(
    modifier: Modifier = Modifier,
    nodes: List<FlowchartNode>,
    connections: List<FlowchartConnection>
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    
    // 计算节点位置
    val nodePositions = remember(nodes, connections) {
        calculateNodePositions(nodes, connections)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // 限制缩放范围
                    val newScale = (scale * zoom).coerceIn(0.5f, 3f)
                    scale = newScale
                    
                    // 简化拖动逻辑，不限制范围，让用户自由拖动
                    offsetX = offsetX + pan.x
                    offsetY = offsetY + pan.y
                }
            }
    ) {
        // 使用Column来包含可缩放的流程图内容
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        ) {
            // 绘制连接线（使用Canvas）
            FlowchartConnections(
                connections = connections,
                nodePositions = nodePositions,
                nodes = nodes
            )

            // 绘制节点
            nodes.forEach { node ->
                val position = nodePositions[node.id]
                if (position != null) {
                    FlowchartNodeComponent(
                        node = node,
                        position = position,
                        isHighlighted = false
                    )
                }
            }
        }

        // 控制面板
        FlowchartControls(
            scale = scale,
            onZoomIn = { scale = (scale * 1.2f).coerceAtMost(5f) },
            onZoomOut = { scale = (scale / 1.2f).coerceAtLeast(0.1f) },
            onReset = {
                scale = 1f
                offsetX = 0f
                offsetY = 0f
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )

    }
}

@Composable
private fun FlowchartConnections(
    connections: List<FlowchartConnection>,
    nodePositions: Map<String, Offset>,
    nodes: List<FlowchartNode>
) {
    // 使用Canvas绘制连接线
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        connections.forEach { connection ->
            val fromPos = nodePositions[connection.fromId]
            val toPos = nodePositions[connection.toId]

            if (fromPos != null && toPos != null) {
                // 计算连接点（从节点下面到节点上面）
                val fromNode = nodes.find { it.id == connection.fromId }
                val toNode = nodes.find { it.id == connection.toId }

                // 获取节点高度
                val fromNodeHeight = fromNode?.let { 30f + it.instructions.size * 15f } ?: 80f
                val toNodeHeight = toNode?.let { 30f + it.instructions.size * 15f } ?: 80f
                
                val startX = fromPos.x + 100f // 节点中心
                val startY = fromPos.y + fromNodeHeight // 节点底部
                val endX = toPos.x + 100f // 节点中心
                val endY = toPos.y // 节点顶部

                drawConnectionLine(
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    isJump = connection.isJump
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConnectionLine(
    start: Offset,
    end: Offset,
    isJump: Boolean
) {
    val color = if (isJump) Color.Red else Color.Blue
    val strokeWidth = 2f
    val curveOffset = 30f // 减小这个值使曲线变短

    // 绘制贝塞尔曲线
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(start.x, start.y)

        val controlX1 = start.x + curveOffset
        val controlY1 = start.y
        val controlX2 = end.x - curveOffset
        val controlY2 = end.y

        cubicTo(controlX1, controlY1, controlX2, controlY2, end.x, end.y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth)
    )

    // 计算贝塞尔曲线上的中间点（t = 0.5）
    // 贝塞尔曲线公式：B(t) = (1-t)³P0 + 3(1-t)²tP1 + 3(1-t)t²P2 + t³P3
    // 其中 P0=start, P1=control1, P2=control2, P3=end
    val t = 0.5f
    val oneMinusT = 1f - t
    val oneMinusT2 = oneMinusT * oneMinusT
    val oneMinusT3 = oneMinusT2 * oneMinusT
    val t2 = t * t
    val t3 = t2 * t
    
    val controlX1 = start.x + curveOffset
    val controlY1 = start.y
    val controlX2 = end.x - curveOffset
    val controlY2 = end.y
    
    val midX = oneMinusT3 * start.x + 
               3 * oneMinusT2 * t * controlX1 + 
               3 * oneMinusT * t2 * controlX2 + 
               t3 * end.x
    val midY = oneMinusT3 * start.y + 
               3 * oneMinusT2 * t * controlY1 + 
               3 * oneMinusT * t2 * controlY2 + 
               t3 * end.y
    
    // 计算贝塞尔曲线在中间点的切线方向（导数）
    // B'(t) = 3(1-t)²(P1-P0) + 6(1-t)t(P2-P1) + 3t²(P3-P2)
    val dx = 3 * oneMinusT2 * (controlX1 - start.x) + 
             6 * oneMinusT * t * (controlX2 - controlX1) + 
             3 * t2 * (end.x - controlX2)
    val dy = 3 * oneMinusT2 * (controlY1 - start.y) + 
             6 * oneMinusT * t * (controlY2 - controlY1) + 
             3 * t2 * (end.y - controlY2)
    
    val angle = kotlin.math.atan2(dy, dx)

    // 绘制箭头在中间点
    val arrowSize = 8f
    val arrowPath = androidx.compose.ui.graphics.Path().apply {
        moveTo(midX, midY)
        lineTo(
            (midX - arrowSize * kotlin.math.cos(angle - kotlin.math.PI / 6)).toFloat(),
            (midY - arrowSize * kotlin.math.sin(angle - kotlin.math.PI / 6)).toFloat()
        )
        lineTo(
            (midX - arrowSize * kotlin.math.cos(angle + kotlin.math.PI / 6)).toFloat(),
            (midY - arrowSize * kotlin.math.sin(angle + kotlin.math.PI / 6)).toFloat()
        )
        close()
    }

    drawPath(
        path = arrowPath,
        color = color
    )
}

@Composable
private fun FlowchartNodeComponent(
    node: FlowchartNode,
    position: Offset,
    isHighlighted: Boolean
) {
    val nodeHeight = 30f + node.instructions.size * 15f
    val nodeWidth = 200f
    
    Box(
        modifier = Modifier
            .offset(
                x = position.x.dp,
                y = position.y.dp
            )
            .width(nodeWidth.dp)
            .height(nodeHeight.dp)
            .background(
                color = if (isHighlighted) Color.LightGray else Color.White,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = if (isHighlighted) Color.DarkGray else Color.Gray,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 地址和汇编代码
            Text(
                text = node.address,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                lineHeight=12.sp
            )
            
            // 汇编指令
            node.instructions.forEach { instruction ->
                Text(
                    text = instruction,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.DarkGray,
                    lineHeight=12.sp
                )
            }
        }
    }
}

fun calculateNodePositions(
    nodes: List<FlowchartNode>,
    connections: List<FlowchartConnection>
): Map<String, Offset> {
    val positions = mutableMapOf<String, Offset>()
    val nodeLevels = calculateNodeLevels(nodes, connections)
    
    // 按层级分组
    val levelGroups = nodeLevels.entries.groupBy({ it.value }, { it.key })
    
    val horizontalSpacing = 250f
    val verticalSpacing = 150f
    var currentY = 50f
    
    // 按层级处理
    val maxLevel = levelGroups.keys.maxOrNull() ?: 0
    for (level in 0..maxLevel) {
        val levelNodes = levelGroups[level] ?: emptyList()
        val maxNodeHeight = levelNodes.maxOfOrNull {
            nodes.find { n -> n.id == it }?.instructions?.size ?: 0
        }?.let { 60f + it * 18f }
        
        // 计算该层级的起始X位置
        val totalWidth = levelNodes.size * horizontalSpacing
        var currentX = 50f + (1000f - totalWidth) / 2
        
        levelNodes.forEach { nodeId ->
            val node = nodes.find { it.id == nodeId }
            if (node != null) {
                val nodeHeight = 60f + node.instructions.size * 18f
                positions[nodeId] = Offset(currentX, currentY)
                currentX += horizontalSpacing
            }
        }

        if (maxNodeHeight != null) {
            currentY += maxNodeHeight + verticalSpacing
        }
    }
    
    return positions
}

data class CanvasBounds(
    val width: Float,
    val height: Float,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
)

fun calculateCanvasBounds(
    nodePositions: Map<String, Offset>
): CanvasBounds {
    if (nodePositions.isEmpty()) {
        return CanvasBounds(1000f, 1000f, 0f, 0f, 1000f, 1000f)
    }

    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE

    nodePositions.values.forEach { position ->
        minX = min(minX, position.x)
        minY = min(minY, position.y)
        maxX = max(maxX, position.x + 200f) // 估计节点宽度
        maxY = max(maxY, position.y + 100f) // 估计节点高度
    }

    val margin = 100f
    return CanvasBounds(
        width = maxX - minX + 2 * margin,
        height = maxY - minY + 2 * margin,
        minX = minX - margin,
        minY = minY - margin,
        maxX = maxX + margin,
        maxY = maxY + margin
    )
}

fun calculateNodeLevels(
    nodes: List<FlowchartNode>,
    connections: List<FlowchartConnection>
): Map<String, Int> {
    val nodeLevels = mutableMapOf<String, Int>()
    val processedNodes = mutableSetOf<String>()

    // 找到入口节点（没有被任何连接指向的节点）
    val entryNodes = nodes.filter { node ->
        !connections.any { it.toId == node.id }
    }.ifEmpty { nodes.take(1) }

    // 按层级布局
    for (entry in entryNodes) {
        layoutNodesByLevel(nodes, connections, entry.id, 0, nodeLevels, processedNodes)
    }

    // 确保所有节点都有层级
    nodes.forEach { node ->
        if (!nodeLevels.containsKey(node.id)) {
            nodeLevels[node.id] = 0
        }
    }
    
    // 直接返回 nodeLevels，因为所有节点都已经确保有层级值
    return nodeLevels
}

fun layoutNodesByLevel(
    nodes: List<FlowchartNode>,
    connections: List<FlowchartConnection>,
    nodeId: String,
    currentLevel: Int,
    nodeLevels: MutableMap<String, Int>,
    processedNodes: MutableSet<String>
) {
    if (processedNodes.contains(nodeId)) return
    processedNodes.add(nodeId)

    val existingLevel = nodeLevels[nodeId]
    if (existingLevel == null || currentLevel > existingLevel) {
        nodeLevels[nodeId] = currentLevel
    }

    connections
        .filter { it.fromId == nodeId }
        .forEach { connection ->
            layoutNodesByLevel(nodes, connections, connection.toId, currentLevel + 1, nodeLevels, processedNodes)
        }
}

@Composable
fun FlowchartControls(
    scale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Button(
            onClick = onZoomIn,
            modifier = Modifier.size(40.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        androidx.compose.material3.Button(
            onClick = onZoomOut,
            modifier = Modifier.size(40.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        androidx.compose.material3.Button(
            onClick = onReset,
            modifier = Modifier.height(40.dp)
        ) {
            Text("重置", fontSize = 12.sp)
        }

        Text(
            text = "${(scale * 100).toInt()}%",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}