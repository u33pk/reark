package com.orz.reark.reark.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ConsoleTab(
    val id: String,
    val title: String,
    val type: ConsoleType,
    val messages: MutableList<ConsoleMessage> = mutableListOf()
)

data class ConsoleMessage(
    val id: String,
    val content: String,
    val level: LogLevel,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ConsoleType {
    BUILD, DEBUG, TERMINAL, ANALYSIS
}

enum class LogLevel {
    INFO, WARNING, ERROR, SUCCESS, DEBUG
}

@Composable
fun ConsoleTabs(modifier: Modifier = Modifier) {
    val tabs = remember {
        mutableStateListOf(
            ConsoleTab("1", "构建输出", ConsoleType.BUILD).apply {
                messages.addAll(listOf(
                    ConsoleMessage("1-1", "开始构建项目...", LogLevel.INFO),
                    ConsoleMessage("1-2", "编译成功: 23个文件", LogLevel.SUCCESS),
                    ConsoleMessage("1-3", "生成APK: app-debug.apk", LogLevel.INFO),
                    ConsoleMessage("1-4", "构建完成，耗时 2.3秒", LogLevel.SUCCESS)
                ))
            },
            ConsoleTab("2", "调试日志", ConsoleType.DEBUG).apply {
                messages.addAll(listOf(
                    ConsoleMessage("2-1", "应用已启动", LogLevel.INFO),
                    ConsoleMessage("2-2", "MainActivity.onCreate() 调用", LogLevel.DEBUG),
                    ConsoleMessage("2-3", "加载布局: activity_main", LogLevel.DEBUG),
                    ConsoleMessage("2-4", "初始化数据完成", LogLevel.INFO)
                ))
            },
            ConsoleTab("3", "终端", ConsoleType.TERMINAL).apply {
                messages.addAll(listOf(
                    ConsoleMessage("3-1", "$ adb devices", LogLevel.INFO),
                    ConsoleMessage("3-2", "List of devices attached", LogLevel.INFO),
                    ConsoleMessage("3-3", "emulator-5554 device", LogLevel.SUCCESS),
                    ConsoleMessage("3-4", "$ adb logcat -s MainActivity", LogLevel.INFO)
                ))
            },
            ConsoleTab("4", "分析结果", ConsoleType.ANALYSIS).apply {
                messages.addAll(listOf(
                    ConsoleMessage("4-1", "开始分析代码...", LogLevel.INFO),
                    ConsoleMessage("4-2", "发现 5个类，12个方法", LogLevel.INFO),
                    ConsoleMessage("4-3", "检测到潜在安全问题: 3处", LogLevel.WARNING),
                    ConsoleMessage("4-4", "分析完成，生成报告", LogLevel.SUCCESS)
                ))
            }
        )
    }
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    var isAutoScroll by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    
    Column(modifier = modifier) {
        // 控制台标题栏
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "控制台",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "控制台",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 控制按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            // 清空当前标签
                            if (tabs.isNotEmpty()) {
                                tabs[selectedTabIndex].messages.clear()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "清空",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { isAutoScroll = !isAutoScroll },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isAutoScroll) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isAutoScroll) "暂停自动滚动" else "启用自动滚动",
                            modifier = Modifier.size(18.dp),
                            tint = if (isAutoScroll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            // 添加测试消息
                            coroutineScope.launch {
                                if (tabs.isNotEmpty()) {
                                    val tab = tabs[selectedTabIndex]
                                    val newId = "${tab.id}-${tab.messages.size + 1}"
                                    val level = when ((1..5).random()) {
                                        1 -> LogLevel.INFO
                                        2 -> LogLevel.WARNING
                                        3 -> LogLevel.ERROR
                                        4 -> LogLevel.SUCCESS
                                        else -> LogLevel.DEBUG
                                    }
                                    
                                    val message = when (tab.type) {
                                        ConsoleType.BUILD -> ConsoleMessage(newId, "构建步骤完成", level)
                                        ConsoleType.DEBUG -> ConsoleMessage(newId, "调试信息: ${System.currentTimeMillis()}", level)
                                        ConsoleType.TERMINAL -> ConsoleMessage(newId, "$ echo 'test'", level)
                                        ConsoleType.ANALYSIS -> ConsoleMessage(newId, "分析进度更新", level)
                                    }
                                    
                                    tab.messages.add(message)
                                }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加测试消息",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        
        // Tab栏 - 使用改进的Tab布局
        ImprovedTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            tabs = tabs.mapIndexed { index, tab ->
                {
                    ImprovedTab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        onClose = {
                            if (tabs.size > 1) {
                                tabs.removeAt(index)
                                if (selectedTabIndex >= tabs.size) {
                                    selectedTabIndex = tabs.size - 1
                                }
                            }
                        },
                        icon = when (tab.type) {
                            ConsoleType.BUILD -> Icons.Default.Build
                            ConsoleType.DEBUG -> Icons.Default.BugReport
                            ConsoleType.TERMINAL -> Icons.Default.Terminal
                            ConsoleType.ANALYSIS -> Icons.Default.Analytics
                        },
                        title = tab.title,
                        badgeCount = tab.messages.size,
                        showCloseButton = true
                    )
                }
            },
            onAddTab = {
                val newId = (tabs.size + 1).toString()
                val newTab = ConsoleTab(newId, "新控制台", ConsoleType.TERMINAL)
                newTab.messages.add(
                    ConsoleMessage("$newId-1", "新控制台已创建", LogLevel.INFO)
                )
                tabs.add(newTab)
                selectedTabIndex = tabs.size - 1
            }
        )
        
        // 控制台消息区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (tabs.isNotEmpty()) {
                val selectedTab = tabs[selectedTabIndex]
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    reverseLayout = true
                ) {
                    items(selectedTab.messages.reversed()) { message ->
                        ConsoleMessageItem(message = message)
                    }
                }
                
                // 空状态
                if (selectedTab.messages.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (selectedTab.type) {
                                ConsoleType.BUILD -> Icons.Default.Build
                                ConsoleType.DEBUG -> Icons.Default.BugReport
                                ConsoleType.TERMINAL -> Icons.Default.Terminal
                                ConsoleType.ANALYSIS -> Icons.Default.Analytics
                            },
                            contentDescription = "空状态",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "没有消息",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "控制台输出将显示在这里",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "无控制台",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "没有控制台标签",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点击 + 按钮添加新控制台",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun ConsoleMessageItem(message: ConsoleMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 时间戳
        Text(
            text = String.format("%tT", message.timestamp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(70.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 日志级别指示器
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    when (message.level) {
                        LogLevel.INFO -> Color(0xFF2196F3)
                        LogLevel.WARNING -> Color(0xFFFF9800)
                        LogLevel.ERROR -> Color(0xFFF44336)
                        LogLevel.SUCCESS -> Color(0xFF4CAF50)
                        LogLevel.DEBUG -> Color(0xFF9C27B0)
                    }
                )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // 消息内容
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            ),
            color = when (message.level) {
                LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
                LogLevel.WARNING -> Color(0xFFFF9800)
                LogLevel.ERROR -> Color(0xFFF44336)
                LogLevel.SUCCESS -> Color(0xFF4CAF50)
                LogLevel.DEBUG -> Color(0xFF9C27B0)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}