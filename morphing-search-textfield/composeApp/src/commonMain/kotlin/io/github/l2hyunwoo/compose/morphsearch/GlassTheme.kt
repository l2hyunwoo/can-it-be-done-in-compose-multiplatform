package io.github.l2hyunwoo.compose.morphsearch

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** [tint] feeds the theming uniform on [MorphSearchParams]: rgb = glass body colour, a = mix amount. */
@Immutable
data class GlassTheme(
  val background: Color,
  val tint: Color,
  val specStrength: Float,
  val onGlass: Color,
) {
  val onGlassMuted: Color get() = onGlass.copy(alpha = onGlass.alpha * 0.7f)

  companion object {
    val Dark = GlassTheme(
      background = Color(0xFF2E4B63),
      tint = Color(0.03f, 0.06f, 0.10f, 0.55f),
      specStrength = 0.75f,
      onGlass = Color(0xFFF2F5F8),
    )

    val Light = GlassTheme(
      background = Color(0xFFDCE3EA),
      tint = Color(0.90f, 0.94f, 0.98f, 0.32f),
      specStrength = 0.55f,
      onGlass = Color(0xFF1B2733),
    )
  }
}
