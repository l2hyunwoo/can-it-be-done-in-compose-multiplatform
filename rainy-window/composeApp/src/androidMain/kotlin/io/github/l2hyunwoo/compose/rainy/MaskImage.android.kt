package io.github.l2hyunwoo.compose.rainy

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun argbToImageBitmap(width: Int, height: Int, pixels: IntArray): ImageBitmap {
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  bitmap.setHasAlpha(true)
  bitmap.isPremultiplied = false
  bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
  return bitmap.asImageBitmap()
}
