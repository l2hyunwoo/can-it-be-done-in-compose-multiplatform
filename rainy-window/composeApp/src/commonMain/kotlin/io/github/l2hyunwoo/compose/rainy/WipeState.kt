package io.github.l2hyunwoo.compose.rainy

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.sqrt

/** Finger-wipe fog mask: a [MASK_SIZE]² field of wiped values 0 (foggy)..255 (clear), decaying back to 0. */
class WipeState(
  private val wipeStrength: Int = 255,
  private val decayRatePerFrame: Float = 0.4f, // ~10s to fully re-fog at 60fps
  private val brushRadius: Float = 14f,
) {

  // Carries the fractional part of decayRatePerFrame so re-fog can be slower than 1 unit/frame.
  private var decayCarry: Float = 0f

  private val values = IntArray(MASK_SIZE * MASK_SIZE)
  private var paneSize: IntSize = IntSize.Zero
  private var lastX = 0f
  private var lastY = 0f
  private var hasLast = false

  private val _bitmap = mutableStateOf(buildBitmap())
  val bitmap: ImageBitmap get() = _bitmap.value

  private val _generation = mutableIntStateOf(0)

  // Read this inside the mirage params block — required so the node rebinds the mutated mask each draw.
  val generation: Int get() = _generation.intValue

  fun ensureSize(size: IntSize) {
    paneSize = size
  }

  fun begin(pos: Offset) {
    val (mx, my) = toMask(pos)
    lastX = mx
    lastY = my
    hasLast = true
    stampDisc(mx, my)
    publish()
  }

  fun extend(pos: Offset) {
    val (mx, my) = toMask(pos)
    if (hasLast) stampSegment(lastX, lastY, mx, my) else stampDisc(mx, my)
    lastX = mx
    lastY = my
    hasLast = true
    publish()
  }

  fun decayStep() {
    if (decayRatePerFrame <= 0f) return
    decayCarry += decayRatePerFrame
    val step = decayCarry.toInt()
    if (step <= 0) return
    decayCarry -= step
    var changed = false
    for (i in values.indices) {
      val v = values[i]
      if (v > 0) {
        values[i] = max(0, v - step)
        changed = true
      }
    }
    if (changed) publish()
  }

  private fun toMask(pos: Offset): Pair<Float, Float> {
    val w = max(1, paneSize.width)
    val h = max(1, paneSize.height)
    return (pos.x / w) * MASK_SIZE to (pos.y / h) * MASK_SIZE
  }

  private fun stampSegment(x0: Float, y0: Float, x1: Float, y1: Float) {
    val dx = x1 - x0
    val dy = y1 - y0
    val dist = sqrt(dx * dx + dy * dy)
    val step = max(1f, brushRadius * 0.5f)
    val steps = max(1, (dist / step).toInt())
    for (s in 0..steps) {
      val t = s.toFloat() / steps
      stampDisc(x0 + dx * t, y0 + dy * t)
    }
  }

  private fun stampDisc(cx: Float, cy: Float) {
    val r = brushRadius
    val minX = max(0, (cx - r).toInt())
    val maxX = kotlin.math.min(MASK_SIZE - 1, (cx + r).toInt())
    val minY = max(0, (cy - r).toInt())
    val maxY = kotlin.math.min(MASK_SIZE - 1, (cy + r).toInt())
    val invR = 1f / r
    for (py in minY..maxY) {
      for (px in minX..maxX) {
        val ddx = px + 0.5f - cx
        val ddy = py + 0.5f - cy
        val u = sqrt(ddx * ddx + ddy * ddy) * invR
        if (u >= 1f) continue
        val soft = 1f - u * u * (3f - 2f * u)
        val add = (wipeStrength * soft).toInt()
        val idx = py * MASK_SIZE + px
        val nv = values[idx] + add
        values[idx] = if (nv > 255) 255 else nv
      }
    }
  }

  private fun publish() {
    _bitmap.value = buildBitmap()
    _generation.intValue++
  }

  private fun buildBitmap(): ImageBitmap {
    val px = IntArray(values.size)
    for (i in values.indices) {
      val v = values[i]
      px[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    return argbToImageBitmap(MASK_SIZE, MASK_SIZE, px)
  }

  companion object {
    const val MASK_SIZE: Int = 256
  }
}

internal expect fun argbToImageBitmap(width: Int, height: Int, pixels: IntArray): ImageBitmap
