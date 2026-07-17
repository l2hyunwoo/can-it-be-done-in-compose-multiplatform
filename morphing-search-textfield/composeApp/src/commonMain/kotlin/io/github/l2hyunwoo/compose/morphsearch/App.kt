@file:OptIn(ExperimentalMirage::class, ExperimentalComposeUiApi::class)

package io.github.l2hyunwoo.compose.morphsearch

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.skydoves.cloudy.ExperimentalMirage
import com.skydoves.cloudy.MirageClock
import com.skydoves.cloudy.mirage
import kotlinx.coroutines.launch

@Composable
fun App() {
  val state = rememberMorphSearchState()
  val scope = rememberCoroutineScope()
  val theme = if (isSystemInDarkTheme()) GlassTheme.Dark else GlassTheme.Light
  val focusManager = LocalFocusManager.current
  val focusRequester = remember { FocusRequester() }

  var query by remember { mutableStateOf("") }

  fun expand() = scope.launch {
    state.expand()
    focusRequester.requestFocus()
  }

  fun collapse() = scope.launch {
    focusManager.clearFocus()
    query = ""
    state.collapse()
  }

  BackHandler(enabled = state.isExpanded) { collapse() }

  BoxWithConstraints(Modifier.fillMaxSize().background(theme.background)) {
    val density = LocalDensity.current
    val widthPx = with(density) { maxWidth.toPx() }
    val constants = layoutSpecFor(widthPx)

    var containerSize by remember { mutableStateOf(Size(1f, 1f)) }

    val progress = state.progress.value
    val visibility = state.visibility.value
    val layout = morphLayout(progress, containerSize, constants)
    val fades = morphFades(progress).let {
      MorphFades(field = it.field * visibility, buttons = it.buttons * visibility)
    }

    Box(
      Modifier
        .fillMaxSize()
        .onSizeChanged { containerSize = it.toSize() }
        .onPreviewKeyEvent { e ->
          if (state.isExpanded && e.type == KeyEventType.KeyDown && e.key == Key.Escape) {
            collapse(); true
          } else {
            false
          }
        }
        .pointerInput(state.isExpanded, layout) {
          if (state.isExpanded) {
            detectTapGestures { pos -> if (!layout.hitsGlass(pos)) collapse() }
          }
        },
    ) {
      if (progress > 0.001f) {
        Box(
          Modifier
            .fillMaxSize()
            .mirage(clock = MirageClock.Paused) {
              filter(MorphSearchShader) {
                apply(
                  layout = layout,
                  light = Offset(-1f, -1f),
                  theme = theme,
                  visibility = visibility,
                )
              }
            },
        ) {
          // Background must be a child of the mirage node, not a sibling modifier on it: mirage
          // captures this node's content, and Android's RenderEffect path only sees paints inside it.
          Box(Modifier.fillMaxSize().background(theme.background))
        }

        SearchOverlay(
          layout = layout,
          fades = fades,
          query = query,
          onQueryChange = { query = it },
          theme = theme,
          focusRequester = focusRequester,
          interactive = state.isExpanded,
          onSubmit = { collapse() },
        )
      }

      if (!state.isExpanded && progress <= 0.001f) {
        SearchTrigger(
          theme = theme,
          onClick = { expand() },
          modifier = Modifier.align(Alignment.Center),
        )
      }
    }
  }
}

@Composable
private fun SearchTrigger(theme: GlassTheme, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Canvas(
    modifier
      .size(64.dp)
      .background(theme.onGlass.copy(alpha = 0.10f), CircleShape)
      .border(1.dp, theme.onGlass.copy(alpha = 0.25f), CircleShape)
      .clickable(onClick = onClick)
      .padding(20.dp),
  ) {
    drawSearchGlyph(
      center = Offset(size.width * 0.5f, size.height * 0.5f),
      size = size.minDimension,
      color = theme.onGlass,
      strokeWidth = size.minDimension * 0.10f,
    )
  }
}

private fun IntSize.toSize(): Size = Size(width.toFloat(), height.toFloat())
