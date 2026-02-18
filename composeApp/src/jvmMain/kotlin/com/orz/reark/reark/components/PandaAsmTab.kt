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
fun PandaAsmTab(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "PandaASM Content",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = """
                .class public Lcom/example/MainActivity;
                .super Landroid/app/Activity;
                .source "MainActivity.java"
                
                # direct methods
                .method public constructor <init>()V
                    .registers 1
                
                    invoke-direct {p0}, Landroid/app/Activity;-><init>()V
                    return-void
                .end method
                
                # virtual methods
                .method public onCreate(Landroid/os/Bundle;)V
                    .registers 2
                    .param p1, "savedInstanceState", Landroid/os/Bundle;
                
                    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V
                    const/high16 v0, 0x7f000000
                    invoke-virtual {p0, v0}, Lcom/example/MainActivity;->setContentView(I)V
                    return-void
                .end method
            """.trimIndent(),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}