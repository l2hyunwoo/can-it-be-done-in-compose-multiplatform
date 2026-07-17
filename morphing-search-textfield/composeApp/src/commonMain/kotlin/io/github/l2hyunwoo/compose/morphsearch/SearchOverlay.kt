package io.github.l2hyunwoo.compose.morphsearch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SearchOverlay(
  layout: MorphLayout,
  fades: MorphFades,
  query: String,
  onQueryChange: (String) -> Unit,
  theme: GlassTheme,
  focusRequester: FocusRequester,
  interactive: Boolean,
  onSubmit: () -> Unit,
) {
  val density = LocalDensity.current
  val pill = layout.pillHalfSize

  Box(Modifier.fillMaxSize()) {
    if (fades.buttons > 0f) {
      Canvas(Modifier.fillMaxSize()) {
        layout.blobs.forEachIndexed { i, blob ->
          drawButtonGlyph(
            index = i,
            center = blob.center,
            size = blob.radius * 1.05f,
            color = theme.onGlass,
            alpha = fades.buttons,
          )
        }
      }
    }

    if (pill.width > 1f && pill.height > 1f && fades.field > 0f) {
      val fieldWidthPx = (pill.width * 2f).roundToInt()
      val fieldHeightPx = (pill.height * 2f).roundToInt()
      val fieldLeftPx = (layout.pillCenter.x - pill.width).roundToInt()
      val fieldTopPx = (layout.pillCenter.y - pill.height).roundToInt()
      val iconInsetPx = pill.height * 1.9f // clears the magnifier ink, not just its center
      val fontSizeSp = with(density) { (pill.height * 2f * 0.34f).toSp() }

      Box(
        Modifier
          .placeAt(fieldLeftPx, fieldTopPx, fieldWidthPx, fieldHeightPx)
          .graphicsLayer { alpha = fades.field },
        contentAlignment = Alignment.CenterStart,
      ) {
        Canvas(Modifier.fillMaxSize()) {
          val glyphSize = size.height * 0.42f
          drawSearchGlyph(
            center = Offset(size.height * 0.55f, size.height * 0.5f),
            size = glyphSize,
            color = theme.onGlassMuted,
            strokeWidth = glyphSize * 0.11f,
          )
        }

        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          enabled = interactive,
          singleLine = true,
          textStyle = TextStyle(color = theme.onGlass, fontSize = fontSizeSp),
          cursorBrush = SolidColor(theme.onGlass),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
          modifier = Modifier
            .fillMaxSize()
            .padding(start = with(density) { iconInsetPx.toDp() }, end = 14.dp)
            .focusRequester(focusRequester),
          decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
              if (query.isEmpty()) {
                BasicText(
                  text = "Spotlight Search",
                  style = TextStyle(color = theme.onGlassMuted, fontSize = fontSizeSp),
                  maxLines = 1,
                  overflow = TextOverflow.Clip,
                )
              }
              inner()
            }
          },
        )
      }
    }
  }
}

private fun Modifier.placeAt(xPx: Int, yPx: Int, widthPx: Int, heightPx: Int): Modifier =
  this.layout { measurable, _ ->
    val placeable = measurable.measure(
      androidx.compose.ui.unit.Constraints.fixed(widthPx.coerceAtLeast(0), heightPx.coerceAtLeast(0)),
    )
    layout(placeable.width, placeable.height) { placeable.place(IntOffset(xPx, yPx)) }
  }
