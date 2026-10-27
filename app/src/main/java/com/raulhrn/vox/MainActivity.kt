package com.raulhrn.vox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.raulhrn.vox.data.Prefs
import com.raulhrn.vox.data.SongStore
import com.raulhrn.vox.ui.VoxApp
import com.raulhrn.vox.ui.VoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(applicationContext)
        SongStore.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            VoxTheme { VoxApp() }
        }
    }
}
