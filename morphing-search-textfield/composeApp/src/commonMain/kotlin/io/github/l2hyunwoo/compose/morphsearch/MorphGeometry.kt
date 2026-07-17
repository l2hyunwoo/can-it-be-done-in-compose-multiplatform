package io.github.l2hyunwoo.compose.morphsearch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Pure function of (progress, container size, layout constants), read identically by the shader
 *  uniforms and the overlay composable. Progress 0..[MATERIALIZE_END] grows the pill from zero to
 *  full capsule; MATERIALIZE_END..1 retracts the pill while the buttons pinch off. */

/** Compile-time fixed: the kernel unrolls this and the program cache keys on the kernel source. */
const val BLOB_COUNT = 4

/** Progress at which the pill has finished materialising and the buttons begin to pinch off. */
const val MATERIALIZE_END = 0.35f

data class Blob(val center: Offset, val radius: Float)

data class MorphLayout(
  val pillCenter: Offset,
  val pillHalfSize: Size,
  val blobs: List<Blob>,
  val sminK: Float,
)

data class MorphConstants(
  val pillHeightFraction: Float = 0.16f,
  val pillHeightMaxPx: Float = 118f,
  val pillWidthFractionOfUsable: Float = 0.52f,
  val pillWidthMaxPx: Float = 760f,
  val horizontalMarginFraction: Float = 0.055f,
  val pillRightFractionEnd: Float = 0.86f,
  val detachGapFraction: Float = 0.85f,
  val buttonGapFraction: Float = 0.34f,
  val sminPeakPx: Float = 64f,
)

/** The settled assembly (retracted pill + gap + button row) is centered horizontally; button diameter
 *  equals pill height, shrunk only when a narrow container can't fit the row inside the margins. */
fun morphLayout(
  progress: Float,
  containerSize: Size,
  constants: MorphConstants = MorphConstants(),
): MorphLayout {
  val t = progress.coerceIn(0f, 1f)
  val w = containerSize.width
  val h = containerSize.height

  val materializeT = smoothstep(0f, MATERIALIZE_END, t)
  val buttonT = smoothstep(MATERIALIZE_END, 1f, t)

  val centerY = h * 0.5f
  val marginX = w * constants.horizontalMarginFraction
  val usable = w - 2f * marginX

  val pillHeightFull = min(h * constants.pillHeightFraction, constants.pillHeightMaxPx)
  val pillHalfHFull = pillHeightFull * 0.5f
  val pillWidthFull = min(usable * constants.pillWidthFractionOfUsable, constants.pillWidthMaxPx)

  val retractedWidthFinal = pillWidthFull * constants.pillRightFractionEnd
  val detachGap = pillHalfHFull * constants.detachGapFraction
  val rowUnits = BLOB_COUNT + (BLOB_COUNT - 1) * constants.buttonGapFraction
  val fitDiameter = (usable - retractedWidthFinal - detachGap) / rowUnits
  val buttonDiameter = min(pillHeightFull, fitDiameter)
  val buttonRadiusFull = buttonDiameter * 0.5f
  val step = buttonDiameter + buttonDiameter * constants.buttonGapFraction
  val buttonRadius = buttonRadiusFull * smoothstep(0.08f, 0.85f, buttonT)

  val assemblyWidth = retractedWidthFinal + detachGap + buttonDiameter * rowUnits
  val pillLeft = (w - assemblyWidth) * 0.5f

  val rightFraction = lerp(1f, constants.pillRightFractionEnd, buttonT)
  val pillWidthRetracted = maxOf(pillWidthFull * rightFraction, pillHeightFull)

  val pillHalfH = pillHalfHFull * materializeT
  val pillHalfW = pillWidthRetracted * 0.5f * smoothstep(0f, 1f, materializeT)
  val pillCenterX = pillLeft + pillHalfW

  val retractedRightFinal = pillLeft + retractedWidthFinal
  val firstCenterXFinal = retractedRightFinal + detachGap + buttonRadiusFull
  val capX = pillLeft + pillWidthFull - pillHalfHFull

  val blobs = ArrayList<Blob>(BLOB_COUNT)
  for (i in 0 until BLOB_COUNT) {
    val finalX = firstCenterXFinal + step * i
    val emerge = smoothstep(0f, 1f, ((buttonT - i * 0.06f) / (1f - (BLOB_COUNT - 1) * 0.06f)).coerceIn(0f, 1f))
    val cx = lerp(capX, finalX, emerge)
    blobs += Blob(Offset(cx, centerY), buttonRadius)
  }

  // Single hump over the detach segment: 0 at both ends, peak mid pinch-off.
  val neck = sin(buttonT * PI.toFloat()) * constants.sminPeakPx

  return MorphLayout(
    pillCenter = Offset(pillCenterX, centerY),
    pillHalfSize = Size(pillHalfW, pillHalfH),
    blobs = blobs,
    sminK = neck,
  )
}

private const val HIT_SLOP_PX = 6f

fun MorphLayout.hitsGlass(point: Offset): Boolean {
  val dxPill = kotlin.math.abs(point.x - pillCenter.x) - pillHalfSize.width
  val dyPill = kotlin.math.abs(point.y - pillCenter.y) - pillHalfSize.height
  if (dxPill <= HIT_SLOP_PX && dyPill <= HIT_SLOP_PX) return true
  for (b in blobs) {
    if (b.radius <= 0f) continue
    val dx = point.x - b.center.x
    val dy = point.y - b.center.y
    if (dx * dx + dy * dy <= (b.radius + HIT_SLOP_PX) * (b.radius + HIT_SLOP_PX)) return true
  }
  return false
}

data class MorphFades(val field: Float, val buttons: Float)

fun morphFades(progress: Float): MorphFades {
  val t = progress.coerceIn(0f, 1f)
  val field = smoothstep(MATERIALIZE_END * 0.35f, MATERIALIZE_END, t)
  val buttons = smoothstep(MATERIALIZE_END + 0.12f, 0.9f, t)
  return MorphFades(field, buttons)
}

const val COMPACT_MAX_WIDTH_PX = 640f

fun layoutSpecFor(containerWidthPx: Float): MorphConstants =
  if (containerWidthPx < COMPACT_MAX_WIDTH_PX) {
    MorphConstants(
      pillHeightFraction = 0.15f,
      pillHeightMaxPx = 72f,
      pillWidthFractionOfUsable = 0.46f,
      pillWidthMaxPx = 520f,
      horizontalMarginFraction = 0.05f,
      pillRightFractionEnd = 0.82f,
      detachGapFraction = 0.6f,
      buttonGapFraction = 0.24f,
      sminPeakPx = 46f,
    )
  } else {
    MorphConstants()
  }

/** Hermite smoothstep, matching the GLSL builtin so shader-side and layout-side easing agree. */
private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
  if (edge0 == edge1) return if (x < edge0) 0f else 1f
  val u = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
  return u * u * (3f - 2f * u)
}
