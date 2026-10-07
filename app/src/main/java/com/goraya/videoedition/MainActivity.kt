package com.goraya.videoedition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.goraya.videoedition.ui.EditorScreen
import com.goraya.videoedition.ui.GorayaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GorayaTheme { EditorScreen() } }
    }
}
