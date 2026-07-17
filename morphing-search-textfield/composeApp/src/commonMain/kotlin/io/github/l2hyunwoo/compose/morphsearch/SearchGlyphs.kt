package io.github.l2hyunwoo.compose.morphsearch

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Icon glyphs drawn directly with [Path]/[DrawScope] rather than pulled from material-icons: the four
 * button glyphs (app grid / folder / action gear / clipboard) are not in the bundled `material-icons-core`
 * set, and drawing them keeps the dependency surface unchanged and works identically on every target.
 */

/** Magnifying glass for the search field: a ring plus a handle. */
fun DrawScope.drawSearchGlyph(center: Offset, size: Float, color: Color, strokeWidth: Float) {
  val r = size * 0.32f
  val ringCenter = Offset(center.x - size * 0.06f, center.y - size * 0.06f)
  drawCircle(color = color, radius = r, center = ringCenter, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
  val handleStart = Offset(ringCenter.x + r * 0.72f, ringCenter.y + r * 0.72f)
  val handleEnd = Offset(handleStart.x + size * 0.22f, handleStart.y + size * 0.22f)
  drawLine(color, handleStart, handleEnd, strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

/** Index into the four button glyphs: app grid / folder / action / clipboard, matching the reference. */
fun DrawScope.drawButtonGlyph(index: Int, center: Offset, size: Float, color: Color, alpha: Float) {
  if (alpha <= 0f) return
  val c = color.copy(alpha = color.alpha * alpha)
  val sw = size * 0.09f
  when (index) {
    0 -> drawAppGridGlyph(center, size, c)
    1 -> drawFolderGlyph(center, size, c, sw)
    2 -> drawActionGlyph(center, size, c, sw)
    else -> drawClipboardGlyph(center, size, c, sw)
  }
}

/** App icon: 2x2 grid of rounded squares (SF Symbols "square.grid.2x2" silhouette), filled. */
private fun DrawScope.drawAppGridGlyph(center: Offset, size: Float, color: Color) {
  val cell = size * 0.34f
  val gap = size * 0.10f
  val corner = CornerRadius(cell * 0.28f)
  val offset = cell * 0.5f + gap * 0.5f
  for (dx in intArrayOf(-1, 1)) {
    for (dy in intArrayOf(-1, 1)) {
      val topLeft = Offset(center.x + dx * offset - cell / 2f, center.y + dy * offset - cell / 2f)
      drawRoundRect(color, topLeft, Size(cell, cell), corner)
    }
  }
}

/** Folder: a tab-topped rounded rectangle outline (SF Symbols "folder"). */
private fun DrawScope.drawFolderGlyph(center: Offset, size: Float, color: Color, sw: Float) {
  val w = size * 0.62f
  val hgt = size * 0.46f
  val left = center.x - w * 0.5f
  val top = center.y - hgt * 0.5f
  val tab = w * 0.4f
  val path = Path().apply {
    moveTo(left, top + hgt * 0.22f)
    lineTo(left, top)
    lineTo(left + tab, top)
    lineTo(left + tab + hgt * 0.18f, top + hgt * 0.22f)
    lineTo(left + w, top + hgt * 0.22f)
    lineTo(left + w, top + hgt)
    lineTo(left, top + hgt)
    close()
  }
  drawPath(path, color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Action: a gear (SF Symbols "gearshape") — a ring with radial teeth around a hollow center. */
private fun DrawScope.drawActionGlyph(center: Offset, size: Float, color: Color, sw: Float) {
  val outerR = size * 0.30f
  val toothLen = size * 0.11f
  repeat(8) { i ->
    val a = i * (kotlin.math.PI.toFloat() / 4f)
    val dir = Offset(kotlin.math.cos(a), kotlin.math.sin(a))
    drawLine(color, center + dir * outerR, center + dir * (outerR + toothLen), strokeWidth = size * 0.09f)
  }
  drawCircle(color, radius = outerR, center = center, style = Stroke(width = sw))
  drawCircle(color, radius = size * 0.17f, center = center, style = Stroke(width = sw))
}

/** Clipboard: a rounded-rect board with a small tab notch (SF Symbols "clipboard"). */
private fun DrawScope.drawClipboardGlyph(center: Offset, size: Float, color: Color, sw: Float) {
  val w = size * 0.5f
  val hgt = size * 0.62f
  val left = center.x - w * 0.5f
  val top = center.y - hgt * 0.5f
  drawRoundRect(
    color = color,
    topLeft = Offset(left, top),
    size = Size(w, hgt),
    cornerRadius = CornerRadius(w * 0.14f),
    style = Stroke(width = sw, join = StrokeJoin.Round),
  )
  val tabW = w * 0.42f
  val tabH = hgt * 0.12f
  drawRoundRect(
    color = color,
    topLeft = Offset(center.x - tabW * 0.5f, top - tabH * 0.5f),
    size = Size(tabW, tabH),
    cornerRadius = CornerRadius(tabH * 0.4f),
  )
}
