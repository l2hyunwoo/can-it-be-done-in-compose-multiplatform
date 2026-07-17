package io.github.l2hyunwoo.compose.morphsearch

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "Morphing Search",
    state = rememberWindowState(width = 1000.dp, height = 700.dp),
  ) {
    App()
  }
}
