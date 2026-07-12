package io.github.l2hyunwoo.compose.rainy

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

internal actual fun argbToImageBitmap(width: Int, height: Int, pixels: IntArray): ImageBitmap {
  val rgba = ByteArray(width * height * 4)
  var o = 0
  for (p in pixels) {
    rgba[o] = ((p ushr 16) and 0xFF).toByte() // R
    rgba[o + 1] = ((p ushr 8) and 0xFF).toByte() // G
    rgba[o + 2] = (p and 0xFF).toByte() // B
    rgba[o + 3] = ((p ushr 24) and 0xFF).toByte() // A
    o += 4
  }
  val info = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
  val bitmap = Bitmap()
  bitmap.allocPixels(info)
  bitmap.installPixels(info, rgba, width * 4)
  bitmap.setImmutable()
  return bitmap.asComposeImageBitmap()
}
