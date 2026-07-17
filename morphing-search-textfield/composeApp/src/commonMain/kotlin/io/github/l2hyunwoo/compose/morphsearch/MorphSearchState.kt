package io.github.l2hyunwoo.compose.morphsearch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// progress drives appear -> pinch-off; visibility drives the in-place fade-out on dismissal (settled
// glass fades where it sits, not a reverse morph): collapse animates visibility to 0 with progress
// held at 1, then snaps both home.
@Stable
class MorphSearchState {
  val progress: Animatable<Float, *> = Animatable(0f)
  val visibility: Animatable<Float, *> = Animatable(1f)

  var isExpanded: Boolean by mutableStateOf(false)
    private set

  suspend fun expand() {
    isExpanded = true
    progress.animateTo(1f, tween(EXPAND_MS))
  }

  suspend fun collapse() {
    isExpanded = false
    visibility.animateTo(0f, tween(DISMISS_FADE_MS))
    progress.snapTo(0f)
    visibility.snapTo(1f)
  }

  companion object {
    const val EXPAND_MS = 520
    const val DISMISS_FADE_MS = 160
  }
}

@Composable
fun rememberMorphSearchState(): MorphSearchState = remember { MorphSearchState() }
