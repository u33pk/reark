package com.orz.reark.reark.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MenuBar() {
    var selectedMenu by remember { mutableStateOf("文件") }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .height(48.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 文件菜单
        DropdownMenuButton(
            text = "文件",
            isSelected = selectedMenu == "文件",
            onClick = { selectedMenu = "文件" },
            items = listOf(
                MenuItem("新建项目", Icons.Default.Create, "Ctrl+N"),
                MenuItem("打开项目", Icons.Default.FolderOpen, "Ctrl+O"),
                MenuItem("保存", Icons.Default.Save, "Ctrl+S"),
                MenuItem("另存为", Icons.Default.SaveAs, "Ctrl+Shift+S"),
                MenuItem("-", null, null),
                MenuItem("导入APK", Icons.Default.Upload, "Ctrl+I"),
                MenuItem("导出报告", Icons.Default.Download, "Ctrl+E"),
                MenuItem("-", null, null),
                MenuItem("退出", Icons.Default.ExitToApp, "Alt+F4")
            )
        )
        
        // 编辑菜单
        DropdownMenuButton(
            text = "编辑",
            isSelected = selectedMenu == "编辑",
            onClick = { selectedMenu = "编辑" },
            items = listOf(
                MenuItem("撤销", Icons.Default.Undo, "Ctrl+Z"),
                MenuItem("重做", Icons.Default.Redo, "Ctrl+Y"),
                MenuItem("-", null, null),
                MenuItem("复制", Icons.Default.ContentCopy, "Ctrl+C"),
                MenuItem("剪切", Icons.Default.ContentCut, "Ctrl+X"),
                MenuItem("粘贴", Icons.Default.ContentPaste, "Ctrl+V"),
                MenuItem("-", null, null),
                MenuItem("查找", Icons.Default.Search, "Ctrl+F"),
                MenuItem("替换", Icons.Default.FindReplace, "Ctrl+H")
            )
        )
        
        // 视图菜单
        DropdownMenuButton(
            text = "视图",
            isSelected = selectedMenu == "视图",
            onClick = { selectedMenu = "视图" },
            items = listOf(
                MenuItem("放大", Icons.Default.ZoomIn, "Ctrl++"),
                MenuItem("缩小", Icons.Default.ZoomOut, "Ctrl+-"),
                MenuItem("重置缩放", Icons.Default.ZoomOutMap, "Ctrl+0"),
                MenuItem("-", null, null),
                MenuItem("显示/隐藏代码树", Icons.Default.AccountTree, "Ctrl+1"),
                MenuItem("显示/隐藏控制台", Icons.Default.Terminal, "Ctrl+2"),
                MenuItem("显示/隐藏AI助手", Icons.Default.SmartToy, "Ctrl+3"),
                MenuItem("-", null, null),
                MenuItem("主题设置", Icons.Default.Palette, null)
            )
        )
        
        // 分析菜单
        DropdownMenuButton(
            text = "分析",
            isSelected = selectedMenu == "分析",
            onClick = { selectedMenu = "分析" },
            items = listOf(
                MenuItem("开始分析", Icons.Default.PlayArrow, "F5"),
                MenuItem("停止分析", Icons.Default.Stop, "Shift+F5"),
                MenuItem("-", null, null),
                MenuItem("代码安全检查", Icons.Default.Security, "F6"),
                MenuItem("性能分析", Icons.Default.Speed, "F7"),
                MenuItem("依赖分析", Icons.Default.Analytics, "F8"),
                MenuItem("-", null, null),
                MenuItem("生成报告", Icons.Default.Assessment, "F9")
            )
        )
        
        // 工具菜单
        DropdownMenuButton(
            text = "工具",
            isSelected = selectedMenu == "工具",
            onClick = { selectedMenu = "工具" },
            items = listOf(
                MenuItem("反编译设置", Icons.Default.Settings, null),
                MenuItem("插件管理", Icons.Default.Extension, null),
                MenuItem("-", null, null),
                MenuItem("ADB工具", Icons.Default.Android, null),
                MenuItem("签名工具", Icons.Default.Fingerprint, null),
                MenuItem("资源提取", Icons.Default.Folder, null)
            )
        )
        
        // 帮助菜单
        DropdownMenuButton(
            text = "帮助",
            isSelected = selectedMenu == "帮助",
            onClick = { selectedMenu = "帮助" },
            items = listOf(
                MenuItem("用户手册", Icons.Default.Help, "F1"),
                MenuItem("快捷键参考", Icons.Default.Keyboard, null),
                MenuItem("-", null, null),
                MenuItem("检查更新", Icons.Default.Update, null),
                MenuItem("关于", Icons.Default.Info, null)
            )
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 快速操作按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(end = 16.dp)
        ) {
            IconButton(
                onClick = { /* 运行分析 */ },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "运行",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            IconButton(
                onClick = { /* 停止分析 */ },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "停止",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            IconButton(
                onClick = { /* 设置 */ },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class MenuItem(
    val text: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?,
    val shortcut: String?
)

@Composable
fun DropdownMenuButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    items: List<MenuItem>
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        TextButton(
            onClick = {
                onClick()
                expanded = true
            },
            colors = ButtonDefaults.textButtonColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.height(48.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
        
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(200.dp)
        ) {
            items.forEach { item ->
                if (item.text == "-") {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                } else {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { 
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    item.icon?.let { icon ->
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = item.text,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                
                                item.shortcut?.let { shortcut ->
                                    Text(
                                        text = shortcut,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        },
                        onClick = {
                            expanded = false
                            // 处理菜单项点击
                            println("点击了: ${item.text}")
                        }
                    )
                }
            }
        }
    }
}
