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
    val treeNodes = remember {
        mutableStateListOf(
            TreeNode("1", "com.example", NodeType.PACKAGE, listOf(
                TreeNode("1-1", "MainActivity", NodeType.CLASS, listOf(
                    TreeNode("1-1-1", "onCreate", NodeType.METHOD),
                    TreeNode("1-1-2", "onDestroy", NodeType.METHOD),
                    TreeNode("1-1-3", "mData", NodeType.FIELD)
                )),
                TreeNode("1-2", "UserModel", NodeType.CLASS, listOf(
                    TreeNode("1-2-1", "getName", NodeType.METHOD),
                    TreeNode("1-2-2", "setName", NodeType.METHOD)
                ))
            )),
            TreeNode("2", "res", NodeType.DIRECTORY, listOf(
                TreeNode("2-1", "layout", NodeType.DIRECTORY, listOf(
                    TreeNode("2-1-1", "activity_main.xml", NodeType.FILE)
                )),
                TreeNode("2-2", "values", NodeType.DIRECTORY, listOf(
                    TreeNode("2-2-1", "strings.xml", NodeType.FILE)
                ))
            ))
        )
    }
    
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