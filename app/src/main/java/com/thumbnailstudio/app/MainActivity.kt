package com.thumbnailstudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.thumbnailstudio.app.data.ProjectStore
import com.thumbnailstudio.app.data.SettingsStore
import com.thumbnailstudio.app.ui.ThumbnailStudioRoot
import com.thumbnailstudio.app.ui.theme.ThumbnailStudioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settings = remember { SettingsStore(context.applicationContext) }
            val store = remember { ProjectStore(context.applicationContext) }
            val dark = settings.darkTheme(isSystemInDarkTheme())
            ThumbnailStudioTheme(darkTheme = dark) {
                ThumbnailStudioRoot(store = store, settings = settings)
            }
        }
    }
}
