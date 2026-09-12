package io.github.mauricem0ss.fastdroid

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mauricem0ss.fastdroid.ui.FastDroidRoot
import io.github.mauricem0ss.fastdroid.ui.MainViewModel
import io.github.mauricem0ss.fastdroid.ui.theme.FastDroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val container = container
        setContent {
            val vm = viewModel { MainViewModel(container) }
            val theme by vm.theme.collectAsStateWithLifecycle()
            FastDroidTheme(theme) {
                FastDroidRoot(vm)
            }
        }
    }
}
