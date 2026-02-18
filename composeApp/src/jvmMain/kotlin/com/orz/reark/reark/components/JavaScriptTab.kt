package com.orz.reark.reark.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JavaScriptTab(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "JavaScript Content",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = """
                // Sample JavaScript code
                function greet(name) {
                    console.log(\`Hello, \$\{name\}!\`);
                }
                
                class Person {
                    constructor(name, age) {
                        this.name = name;
                        this.age = age;
                    }
                    
                    introduce() {
                        console.log(\`My name is \$\{this.name\} and I am \$\{this.age\} years old.\`);
                    }
                }
                
                // Usage
                const person = new Person("Alice", 30);
                person.introduce();
                greet("Bob");
            """.trimIndent(),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}