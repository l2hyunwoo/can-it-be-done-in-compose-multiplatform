package io.github.l2hyunwoo.compose.rainy

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import com.skydoves.cloudy.ExperimentalMirage
import com.skydoves.cloudy.MirageClock
import com.skydoves.cloudy.mirage
import org.jetbrains.compose.resources.painterResource
import rainy_window.composeapp.generated.resources.Res
import rainy_window.composeapp.generated.resources.portrait

/** Full-screen rainy window: fogs in on launch, drops run and refract, drag to wipe the fog clear. */
@OptIn(ExperimentalMirage::class)
@Composable
fun App() {
  val wipeState = remember { WipeState() }
  val intro = remember { Animatable(0f) }

  LaunchedEffect(Unit) { intro.animateTo(1f, tween(1400)) }

  LaunchedEffect(Unit) {
    while (true) {
      withFrameNanos { }
      wipeState.decayStep()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black),
  ) {
    Image(
      painter = painterResource(Res.drawable.portrait),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .fillMaxSize()
        .onSizeChanged { wipeState.ensureSize(it) }
        .mirage(clock = MirageClock.Auto) {
          filter(RainyWindowOptic.RainyWindow) {
            @Suppress("UNUSED_EXPRESSION")
            wipeState.generation // ties the draw to mask mutations
            wipeMask(wipeState.bitmap)
            maskSize(WipeState.MASK_SIZE.toFloat())
            introProgress(intro.value)
            rainAmount(0.6f)
            blurRadius(14f)
            hazeStrength(0.45f)
          }
        }
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { wipeState.begin(it) },
            onDrag = { change, _ -> wipeState.extend(change.position) },
          )
        },
    )
  }
}
