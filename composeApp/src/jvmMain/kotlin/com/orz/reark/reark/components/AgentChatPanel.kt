package com.orz.reark.reark.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val content: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis()
)

enum class Sender {
    USER, AGENT, SYSTEM
}

@Composable
fun AgentChatPanel(modifier: Modifier = Modifier) {
    val messages = remember {
        mutableStateListOf(
            ChatMessage("1", "欢迎使用ReArk反编译分析助手！我可以帮助您分析反编译的代码，提供代码解释、安全建议和优化方案。", Sender.AGENT),
            ChatMessage("2", "请选择左侧的代码文件，我可以为您分析其中的类、方法和逻辑。", Sender.AGENT),
            ChatMessage("3", "好的，请帮我分析MainActivity.java文件", Sender.USER),
            ChatMessage("4", "正在分析MainActivity.java...\n\n分析结果：\n1. 这是一个Android Activity类\n2. 包含onCreate和onDestroy生命周期方法\n3. 使用了setContentView设置布局\n4. 有私有字段mData\n5. 建议：考虑使用ViewModel来管理UI相关数据", Sender.AGENT)
        )
    }
    
    var inputText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Column(modifier = modifier) {
        // 标题栏
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "AI助手",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "AI代码分析助手",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 状态指示器
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF4CAF50))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "在线",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
        
        // 聊天消息区域
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                ChatMessageItem(message = message)
            }
        }
        
        // 输入区域
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 快捷操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        text = "分析当前代码",
                        icon = Icons.Default.Analytics,
                        onClick = {
                            coroutineScope.launch {
                                val userMessage = ChatMessage(
                                    id = (messages.size + 1).toString(),
                                    content = "请分析当前打开的代码文件",
                                    sender = Sender.USER
                                )
                                messages.add(userMessage)
                                
                                delay(1000)
                                
                                val agentMessage = ChatMessage(
                                    id = (messages.size + 1).toString(),
                                    content = "正在分析当前代码...\n\n分析完成！\n• 代码结构清晰\n• 遵循了Android开发规范\n• 建议添加更多注释\n• 考虑使用Kotlin重写以获得更好的空安全",
                                    sender = Sender.AGENT
                                )
                                messages.add(agentMessage)
                            }
                        }
                    )
                    
                    QuickActionButton(
                        text = "安全检查",
                        icon = Icons.Default.Security,
                        onClick = {
                            coroutineScope.launch {
                                val userMessage = ChatMessage(
                                    id = (messages.size + 1).toString(),
                                    content = "请进行安全检查",
                                    sender = Sender.USER
                                )
                                messages.add(userMessage)
                                
                                delay(1200)
                                
                                val agentMessage = ChatMessage(
                                    id = (messages.size + 1).toString(),
                                    content = "安全检查完成！\n\n发现的问题：\n1. 没有数据验证\n2. 缺少权限检查\n3. 建议添加输入验证\n4. 考虑使用ProGuard混淆\n\n安全建议：\n• 添加输入验证\n• 实现权限检查\n• 使用HTTPS通信\n• 定期更新依赖",
                                    sender = Sender.AGENT
                                )
                                messages.add(agentMessage)
                            }
                        }
                    )
                    
                    QuickActionButton(
                        text = "优化建议",
                        icon = Icons.Default.AutoAwesome,
                        onClick = {
                            coroutineScope.launch {
                                val userMessage = ChatMessage(
                                    id = (messages.size + 1).toString(),
                                    content = "请提供优化建议",
                                    sender = Sender.USER
                                )
                                messages.add(userMessage)
                                
                                delay(800)
                                
                                val agentMessage = ChatMessage(
                                    id = (messages.size + 1).toString(),
                                    content = "优化建议：\n\n1. 性能优化：\n   • 使用异步任务处理耗时操作\n   • 实现缓存机制\n   • 减少内存泄漏\n\n2. 代码质量：\n   • 提取重复代码为方法\n   • 使用设计模式\n   • 添加单元测试\n\n3. 架构改进：\n   • 采用MVVM架构\n   • 使用依赖注入\n   • 模块化设计",
                                    sender = Sender.AGENT
                                )
                                messages.add(agentMessage)
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 输入框
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("输入您的问题...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        trailingIcon = {
                            if (inputText.isNotEmpty()) {
                                IconButton(
                                    onClick = { inputText = "" }
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "清空")
                                }
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val userMessage = ChatMessage(
                                    id = (messages.size + 1).toString(),
                                    content = inputText,
                                    sender = Sender.USER
                                )
                                messages.add(userMessage)
                                
                                // 模拟AI回复
                                coroutineScope.launch {
                                    delay(1500)
                                    
                                    val agentMessage = ChatMessage(
                                        id = (messages.size + 1).toString(),
                                        content = "我已经收到您的问题：\"$inputText\"\n\n让我分析一下...\n\n根据我的分析，这是一个关于代码理解的问题。建议您：\n1. 查看相关文档\n2. 检查代码逻辑\n3. 考虑使用调试工具\n4. 参考最佳实践",
                                        sender = Sender.AGENT
                                    )
                                    messages.add(agentMessage)
                                }
                                
                                inputText = ""
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "发送",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.sender == Sender.USER
    val isSystem = message.sender == Sender.SYSTEM
    
    if (isSystem) {
        // 系统消息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    } else {
        // 用户或AI消息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                // AI头像
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AI助手",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Column(
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                // 消息内容
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp
                        ),
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                
                // 时间戳
                Text(
                    text = if (isUser) "您" else "AI助手",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp).padding(horizontal = 4.dp)
                )
            }
            
            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                // 用户头像
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "用户",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall
        )
    }
}