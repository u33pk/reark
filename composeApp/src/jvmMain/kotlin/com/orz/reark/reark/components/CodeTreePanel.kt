package com.orz.reark.reark.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import me.yricky.oh.abcd.AbcBuf
import me.yricky.oh.abcd.cfm.AbcClass
import me.yricky.oh.abcd.cfm.ClassItem
import me.yricky.oh.common.wrapAsLEByteBuf
import java.io.File
import java.nio.ByteOrder
import java.nio.channels.FileChannel


data class TreeNode(
    val id: String,
    val name: String,
    val type: NodeType,
    val children: List<TreeNode> = emptyList(),
    var isExpanded: Boolean = false
)

enum class NodeType {
    FILE, CLASS, METHOD, FIELD, PACKAGE, DIRECTORY
}

@Composable
fun CodeTreePanel(modifier: Modifier = Modifier) {
    /************************/
    // TODO: 这一段为测试数据用，后续需要转换为真实参数: classes: Map<Int, ClassItem>
    val file = File("/home/orz/data/unitTest/out/ets/widgets.abc")
    val mmap = FileChannel.open(file.toPath()).map(FileChannel.MapMode.READ_ONLY,0,file.length())
    val abc = AbcBuf("", wrapAsLEByteBuf(mmap.order(ByteOrder.LITTLE_ENDIAN)))
    val classes = abc.classes;
    /**********************/
    val treeNodes = buildClassTree(classes)
    
    var searchText by remember { mutableStateOf("") }
    
    Column(modifier = modifier) {
        // 搜索栏
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("搜索类/方法") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
        
        // 树状结构
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            items(treeNodes) { node ->
                TreeNodeItem(node = node, depth = 0)
            }
        }
    }
}

@Composable
fun TreeNodeItem(node: TreeNode, depth: Int) {
    var isExpanded by remember { mutableStateOf(node.isExpanded) }
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    isExpanded = !isExpanded
                    node.isExpanded = isExpanded
                }
                .padding(start = (depth * 16).dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 展开/折叠图标
            Icon(
                imageVector = if (node.children.isNotEmpty()) {
                    if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight
                } else {
                    Icons.Default.FiberManualRecord
                },
                contentDescription = if (node.children.isNotEmpty()) "展开/折叠" else "节点",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 节点类型图标
            Icon(
                imageVector = when (node.type) {
                    NodeType.FILE -> Icons.Default.InsertDriveFile
                    NodeType.CLASS -> Icons.Default.Class
                    NodeType.METHOD -> Icons.Default.Functions
                    NodeType.FIELD -> Icons.Default.Memory
                    NodeType.PACKAGE -> Icons.Default.Folder
                    NodeType.DIRECTORY -> Icons.Default.Folder
                },
                contentDescription = "类型图标",
                modifier = Modifier.size(16.dp),
                tint = when (node.type) {
                    NodeType.FILE -> Color(0xFF2196F3)
                    NodeType.CLASS -> Color(0xFF4CAF50)
                    NodeType.METHOD -> Color(0xFF9C27B0)
                    NodeType.FIELD -> Color(0xFFFF9800)
                    NodeType.PACKAGE -> Color(0xFF795548)
                    NodeType.DIRECTORY -> Color(0xFF607D8B)
                }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 节点名称
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // 子节点
        if (isExpanded && node.children.isNotEmpty()) {
            node.children.forEach { child ->
                TreeNodeItem(node = child, depth = depth + 1)
            }
        }
    }
}

fun buildClassTree(classes: Map<Int, ClassItem>): List<TreeNode> {
    class MutableTreeNode(
        val name: String,
        val type: NodeType,
        val id: String,
        val children: MutableMap<String, MutableTreeNode> = mutableMapOf()
    )

    val root = MutableTreeNode("root", NodeType.DIRECTORY, "root")

    // 第一步：构建目录结构 + 类节点（使用短名称）
    for ((off, item) in classes) {
        if(item !is AbcClass)
            continue
        val pathParts = item.name.split("/")
        var current = root

        // 遍历所有路径段
        for (i in pathParts.indices) {
            val part = pathParts[i]
            val isLast = (i == pathParts.lastIndex)

            if (isLast) {
                // 最后一段：类节点，name = 最后一段，id = off
                val className = part
                val classNode = MutableTreeNode(
                    name = className,
                    type = NodeType.CLASS,
                    id = off.toString()
                )
                current.children[part] = classNode
            } else {
                // 中间段：PACKAGE 节点
                current = current.children.getOrPut(part) {
                    MutableTreeNode(
                        name = part,
                        type = NodeType.PACKAGE,
                        id = "pkg_${current.id}_$part" // 非关键，仅保证唯一
                    )
                }
            }
        }
    }

    // 第二步：为每个 CLASS 节点挂载 methods 和 fields
    fun findAndAttachMembers(node: MutableTreeNode, classesByPath: Map<String, ClassItem>) {
        if (node.type == NodeType.CLASS) {
            // 根据完整路径反查 ClassItem
            // 我们需要知道这个类的完整路径 → 但当前没有存储
            // 所以换种方式：遍历 classes，匹配 id
            // 更简单：在第一步就记录 classNode -> ClassItem 映射
        }
    }

    // 更直接的方式：重新遍历 classes，在树中定位类节点并附加成员
    for ((off, item) in classes) {
        if(item !is AbcClass)
            continue
        val pathParts = item.name.split("/")
        var current: MutableTreeNode? = root

        // 导航到类节点
        for (part in pathParts) {
            current = current?.children?.get(part)
            if (current == null) break
        }

        if (current != null && current.type == NodeType.CLASS && current.id == off.toString()) {
            // 添加 methods
            item.methods.forEach { method ->
                // 避免重复 key，用 method 内容作 key（假设唯一）
                current.children[method.name] = MutableTreeNode(
                    name = method.name,
                    type = NodeType.METHOD,
                    id = "${current.id}_m_${method.hashCode()}"
                )
            }

            // 添加 fields
            item.fields.forEach { field ->
                current.children[field.name] = MutableTreeNode(
                    name = field.name,
                    type = NodeType.FIELD,
                    id = "${current.id}_f_${field.hashCode()}"
                )
            }
        }
    }

    // 转为不可变 TreeNode
    fun convert(node: MutableTreeNode): TreeNode {
        return TreeNode(
            id = node.id,
            name = node.name,
            type = node.type,
            children = node.children.values.sortedBy { it.name }.map { convert(it) }
        )
    }

    return root.children.values.map { convert(it) }
}