package com.brianreborn.greenmintz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.brianreborn.greenmintz.ui.MintzScreen
import com.brianreborn.greenmintz.ui.MintzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MintzTheme {
                MintzScreen()
            }
        }
    }
}
