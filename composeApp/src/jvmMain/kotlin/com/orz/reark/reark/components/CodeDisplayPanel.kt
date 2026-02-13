package com.orz.reark.reark.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CodeTab(
    val id: String,
    val title: String,
    val language: String,
    val content: String
)

@Composable
fun CodeDisplayPanel(modifier: Modifier = Modifier) {
    val tabs = remember {
        mutableStateListOf(
            CodeTab("1", "MainActivity.java", "java", """
package com.example;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    private String mData = "Hello World";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化数据
        initData();
        
        // 设置UI
        setupUI();
    }
    
    private void initData() {
        mData = "Initialized Data";
    }
    
    private void setupUI() {
        // UI设置代码
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
    }
    
    private void cleanup() {
        mData = null;
    }
}
            """.trimIndent()),
            CodeTab("2", "UserModel.java", "java", """
package com.example;

public class UserModel {
    private String name;
    private int age;
    
    public UserModel(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
    
    @Override
    public String toString() {
        return "UserModel{name='" + name + "', age=" + age + "}";
    }
}
            """.trimIndent()),
            CodeTab("3", "strings.xml", "xml", """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">My Application</string>
    <string name="hello_world">Hello World!</string>
    <string name="action_settings">Settings</string>
    <string name="title_activity_main">MainActivity</string>
    <string name="welcome_message">Welcome to our app!</string>
    
    <!-- 用户界面文本 -->
    <string name="login_button">Login</string>
    <string name="register_button">Register</string>
    <string name="forgot_password">Forgot Password?</string>
    
    <!-- 错误消息 -->
    <string name="error_invalid_email">Invalid email address</string>
    <string name="error_password_too_short">Password must be at least 8 characters</string>
</resources>
            """.trimIndent())
        )
    }
    
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    Column(modifier = modifier) {
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
                        icon = when (tab.language) {
                            "java" -> Icons.Default.Code
                            "xml" -> Icons.Default.DataObject
                            "kotlin" -> Icons.Default.Functions
                            else -> Icons.Default.InsertDriveFile
                        },
                        title = tab.title,
                        badgeCount = null,
                        showCloseButton = true
                    )
                }
            },
            onAddTab = {
                val newId = (tabs.size + 1).toString()
                tabs.add(
                    CodeTab(
                        newId,
                        "NewFile.kt",
                        "kotlin",
                        "// 新文件内容\nfun main() {\n    println(\"Hello World\")\n}"
                    )
                )
                selectedTabIndex = tabs.size - 1
            }
        )
        
        // 代码显示区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (tabs.isNotEmpty()) {
                val selectedTab = tabs[selectedTabIndex]
                
                SelectionContainer {
                    ScrollableColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(
                                    color = Color(0xFF569CD6),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp
                                )) {
                                    append(selectedTab.content)
                                }
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
                
                // 代码信息栏
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "${selectedTab.language.uppercase()} • ${selectedTab.content.lines().size} 行",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "无代码",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "没有打开的代码文件",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击 + 按钮添加新文件或从左侧树中选择文件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier.verticalScroll(scrollState),
        content = content
    )
}