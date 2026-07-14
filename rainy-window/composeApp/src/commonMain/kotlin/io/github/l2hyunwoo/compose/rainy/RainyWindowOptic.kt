/*
 * Drop field ported from "Heartfelt" by Martijn Steinrucken (BigWings),
 * https://www.shadertoy.com/view/ltffzl — CC BY-NC-SA 3.0 (non-commercial, share-alike). See README.
 */
@file:OptIn(com.skydoves.cloudy.ExperimentalMirage::class)

package io.github.l2hyunwoo.compose.rainy

import androidx.compose.ui.graphics.TileMode
import com.skydoves.cloudy.CompositeShader
import com.skydoves.cloudy.MirageParams
import com.skydoves.cloudy.MirageShader
import com.skydoves.cloudy.UFloat
import com.skydoves.cloudy.UTexture
import com.skydoves.cloudy.internal.edsl.Float1Type
import com.skydoves.cloudy.internal.edsl.Float2Type
import com.skydoves.cloudy.internal.edsl.Float3Type
import com.skydoves.cloudy.internal.edsl.If
import com.skydoves.cloudy.internal.edsl.a
import com.skydoves.cloudy.internal.edsl.abs
import com.skydoves.cloudy.internal.edsl.clamp
import com.skydoves.cloudy.internal.edsl.div
import com.skydoves.cloudy.internal.edsl.dot
import com.skydoves.cloudy.internal.edsl.eval
import com.skydoves.cloudy.internal.edsl.float1
import com.skydoves.cloudy.internal.edsl.float2
import com.skydoves.cloudy.internal.edsl.float3
import com.skydoves.cloudy.internal.edsl.floor
import com.skydoves.cloudy.internal.edsl.fract
import com.skydoves.cloudy.internal.edsl.greaterThan
import com.skydoves.cloudy.internal.edsl.half
import com.skydoves.cloudy.internal.edsl.half3
import com.skydoves.cloudy.internal.edsl.half4
import com.skydoves.cloudy.internal.edsl.length
import com.skydoves.cloudy.internal.edsl.local
import com.skydoves.cloudy.internal.edsl.max
import com.skydoves.cloudy.internal.edsl.min
import com.skydoves.cloudy.internal.edsl.minus
import com.skydoves.cloudy.internal.edsl.mirageResolution
import com.skydoves.cloudy.internal.edsl.mirageTime
import com.skydoves.cloudy.internal.edsl.mix
import com.skydoves.cloudy.internal.edsl.mod
import com.skydoves.cloudy.internal.edsl.plus
import com.skydoves.cloudy.internal.edsl.r
import com.skydoves.cloudy.internal.edsl.rgb
import com.skydoves.cloudy.internal.edsl.sampleContent
import com.skydoves.cloudy.internal.edsl.shaderFunction
import com.skydoves.cloudy.internal.edsl.sin
import com.skydoves.cloudy.internal.edsl.smoothstep
import com.skydoves.cloudy.internal.edsl.sqrt
import com.skydoves.cloudy.internal.edsl.times
import com.skydoves.cloudy.internal.edsl.unaryMinus
import com.skydoves.cloudy.internal.edsl.x
import com.skydoves.cloudy.internal.edsl.xy
import com.skydoves.cloudy.internal.edsl.y
import com.skydoves.cloudy.internal.edsl.yx
import com.skydoves.cloudy.internal.edsl.yzx
import com.skydoves.cloudy.internal.edsl.z

/**
 * Rain on steamed glass: running refracting raindrops + condensation you wipe clear with a finger.
 * The drop field is the "Heartfelt" shader by Martijn Steinrucken (BigWings), written directly in the
 * mirage eDSL: the six "Heartfelt" helpers are declared as [shaderFunction] locals (1–5 params) and the
 * main body composes them, so the whole shader is Kotlin — no hand-written AGSL/SKSL string.
 */
public object RainyWindowOptic {

  /** The rainy-window composite shader. Full-bleed by design. */
  public val RainyWindow: CompositeShader<RainyWindowParams> =
    MirageShader.composite("rainyWindow", ::RainyWindowParams) { xy ->
      // The six "Heartfelt" helpers as shaderFunction locals. Declaration order is leaf-first so each
      // callee is in scope for its caller; first-call order stays dependency-first, so the emitter
      // declares every helper before it is used (N13/N/Saw ahead of their callers).

      /** `float3 N13(float p)` — a 3-lane hash. */
      val n13 by shaderFunction(Float1Type, Float3Type) { p ->
        val p3a = fract(float3(p, p, p) * float3(0.1031f, 0.11369f, 0.13787f))
        val p3 = p3a + dot(p3a, p3a.yzx + 19.19f)
        fract(
          float3(
            (p3.x + p3.y) * p3.z,
            (p3.x + p3.z) * p3.y,
            (p3.y + p3.z) * p3.x,
          ),
        )
      }

      /** `float N(float t)`. */
      val nHash by shaderFunction(Float1Type, Float1Type) { t ->
        fract(sin(t * 12345.564f) * 7658.76f)
      }

      /** `float Saw(float b, float t)`. */
      val saw by shaderFunction(Float1Type, Float1Type, Float1Type) { b, t ->
        smoothstep(float1(0f), b, t) * smoothstep(float1(1f), b, t)
      }

      /** `float2 DropLayer2(float2 uv, float t)` — the running-drop layer. */
      val dropLayer2 by shaderFunction(Float2Type, Float1Type, Float2Type) { uvArg, tArg ->
        val bigUV = uvArg // float2 UV = uv;
        var uv by local(uvArg)
        uv = float2(uv.x, uv.y + tArg * 0.75f) // uv.y += t * 0.75

        val a = float2(6f, 1f)
        val grid = a * 2f
        var id by local(floor(uv * grid))
        val colShift = nHash(id.x)
        uv = float2(uv.x, uv.y + colShift) // uv.y += colShift
        id = floor(uv * grid)
        val n = n13(id.x * 35.2f + id.y * 2376.1f)
        val stv = fract(uv * grid) - float2(0.5f, 0f)
        var x by local(n.x - 0.5f)
        var y by local(bigUV.y * 20f)
        val wiggle = sin(y + sin(y))
        x = x + wiggle * (0.5f - abs(x)) * (n.z - 0.5f)
        x = x * 0.7f
        val ti = fract(tArg + n.z)
        y = (saw(float1(0.85f), ti) - 0.5f) * 0.9f + 0.5f
        // Freeze every intermediate that reads the mutable `x`/`y`: a plain `val` captures VarRef(y),
        // which would re-read `y`'s *current* value at each inlined use — but `y` is reassigned below,
        // so these must snapshot now, exactly as the original's `float`/`float2` locals do.
        val p by local(float2(x, y))
        val d by local(length((stv - p) * a.yx))
        val mainDrop by local(smoothstep(0.4f, 0f, d))
        val r by local(sqrt(smoothstep(float1(1f), y, stv.y)))
        val cd by local(abs(stv.x - x))
        var trail by local(smoothstep(0.23f * r, 0.15f * r * r, cd))
        val trailFront by local(smoothstep(-0.02f, 0.02f, stv.y - y))
        trail = trail * trailFront * r * r
        y = bigUV.y
        val trail2 = smoothstep(0.2f * r, float1(0f), cd)
        var droplets by local(
          max(float1(0f), (sin(y * (1f - y) * 120f) - stv.y)) * trail2 * trailFront * n.z,
        )
        y = fract(y * 10f) + (stv.y - 0.5f)
        val dd = length(stv - float2(x, y))
        droplets = smoothstep(0.3f, 0f, dd)
        val m = mainDrop + droplets * r * trailFront
        float2(m, trail)
      }

      /** `float StaticDrops(float2 uv, float t)` — the sparse static-drop layer. */
      val staticDrops by shaderFunction(Float2Type, Float1Type, Float1Type) { uvArg, tArg ->
        var uv by local(uvArg * 40f)
        // Freeze `id` before `uv` is reassigned below (a plain val would re-read the mutated `uv`).
        val id by local(floor(uv))
        uv = fract(uv) - 0.5f
        val n = n13(id.x * 107.45f + id.y * 3543.654f)
        val p = (n.xy - 0.5f) * 0.7f
        val d = length(uv - p)
        val fade = saw(float1(0.025f), fract(tArg + n.z))
        smoothstep(0.3f, 0f, d) * fract(n.z * 10f) * fade
      }

      /** `float2 Drops(float2 uv, float t, float l0, float l1, float l2)`. */
      val drops by shaderFunction(
        Float2Type,
        Float1Type,
        Float1Type,
        Float1Type,
        Float1Type,
        Float2Type,
      ) { uvArg, tArg, l0, l1, l2 ->
        val s = staticDrops(uvArg, tArg) * l0
        val m1 = dropLayer2(uvArg, tArg) * l1
        val m2 = dropLayer2(uvArg * 1.85f, tArg) * l2
        var c by local(s + m1.x + m2.x)
        c = smoothstep(0.3f, 1f, c)
        float2(c, max(m1.y * l0, m2.y * l1))
      }

      // --- main ---
      val res = mirageResolution
      val amount = clamp(rainAmount, 0f, 1f)

      val uv0 = (xy - 0.5f * res) / max(res.y, 1f)
      // uv.y = -uv.y  (F4 write-swizzle workaround: rebuild flipped)
      val uv1 = float2(uv0.x, -uv0.y)
      val scale = max(dropScale, 0.2f)
      val uv = uv1 * scale

      val timeWrapped = mod(mirageTime, 120f)
      val t = timeWrapped * 0.2f

      val uvNorm = xy / max(res, float2(1f, 1f))

      // Bilinear wipe-mask read: four clamped taps (kept un-merged by SampleTexture position-dependence).
      val maskUV = clamp(xy / max(res, float2(1f, 1f)), 0f, 1f) * maskSize
      val mlo = float2(0.5f, 0.5f)
      val mhi = float2(maskSize - 0.5f, maskSize - 0.5f)
      val base = floor(maskUV - 0.5f) + 0.5f
      val f = fract(maskUV - 0.5f)
      val w00 = clamp(wipeMask.eval(clamp(base, mlo, mhi)).r, 0f, 1f)
      val w10 = clamp(wipeMask.eval(clamp(base + float2(1f, 0f), mlo, mhi)).r, 0f, 1f)
      val w01 = clamp(wipeMask.eval(clamp(base + float2(0f, 1f), mlo, mhi)).r, 0f, 1f)
      val w11 = clamp(wipeMask.eval(clamp(base + float2(1f, 1f), mlo, mhi)).r, 0f, 1f)
      val wiped = mix(mix(w00, w10, f.x), mix(w01, w11, f.x), f.y)

      val maxBlur = mix(3f, 6f, amount)
      val minBlur = float1(2f)
      val staticLevel = smoothstep(-0.5f, 1f, amount) * 2f
      val layer1 = smoothstep(0.25f, 0.75f, amount)
      val layer2 = smoothstep(0f, 0.5f, amount)

      val c = drops(uv, t, staticLevel, layer1, layer2)

      val e = float2(0.001f, 0f)
      val cx = drops(uv + e, t, staticLevel, layer1, layer2).x
      val cy = drops(uv + e.yx, t, staticLevel, layer1, layer2).x
      val n0 = float2(cx - c.x, cy - c.x)
      // n.y = -n.y
      val n = float2(n0.x, -n0.y)

      val focus0 = mix(maxBlur - c.y, minBlur, smoothstep(0.1f, 0.2f, c.x))
      val focus = mix(focus0, minBlur, clamp(wiped, 0f, 1f))
      var foggy by local(clamp((focus - minBlur) / max(maxBlur - minBlur, 1e-3f), 0f, 1f))
      foggy = foggy * clamp(fogAmount, 0f, 1f)

      val nPx0 = n * res
      val nLen = length(nPx0)
      val nPx = nPx0 * (min(nLen, 24f) / max(nLen, 1e-4f))
      val tap = clamp((uvNorm * res) + nPx, float2(0.5f, 0.5f), res - 0.5f)
      val sharpBg = sampleContent(tap)

      val st = max(blurRadius, 0f) * foggy
      val st2 = st * 2f

      var fogged by local(sharpBg)
      If(foggy greaterThan 0.01f) {
        val blur0 = sharpBg +
          sampleContent(clamp(tap + float2(-st2, -st2), float2(0.5f, 0.5f), res - 0.5f)) +
          sampleContent(clamp(tap + float2(st2, -st2), float2(0.5f, 0.5f), res - 0.5f)) +
          sampleContent(clamp(tap + float2(-st2, st2), float2(0.5f, 0.5f), res - 0.5f)) +
          sampleContent(clamp(tap + float2(st2, st2), float2(0.5f, 0.5f), res - 0.5f))
        val blur = blur0 * half(1f / 5f)
        fogged = mix(sharpBg, blur, half(foggy))
      }
      val haze = half3(0.86f, 0.90f, 0.94f)
      // fogged.rgb = mix(fogged.rgb, haze, foggy * hazeStrength)
      val foggedRgb = mix(fogged.rgb, haze, half(foggy) * half(clamp(hazeStrength, 0f, 1f)))
      val foggedFinal = half4(foggedRgb, fogged.a)

      val photo = sampleContent(clamp(xy, float2(0.5f, 0.5f), res - 0.5f))

      mix(photo, foggedFinal, half(clamp(introProgress, 0f, 1f)))
    }
}

/** Shader uniforms (property name == uniform id). [wipeMask] is the finger-wipe fog mask from `WipeState`. */
public class RainyWindowParams : MirageParams() {
  public val wipeMask: UTexture by texture(default = null, tileMode = TileMode.Clamp)
  public val maskSize: UFloat by uniform(256f)
  public val introProgress: UFloat by uniform(1f)
  public val rainAmount: UFloat by uniform(0.6f)
  public val blurRadius: UFloat by uniform(9f)
  public val fogAmount: UFloat by uniform(1f)
  public val hazeStrength: UFloat by uniform(0.55f)
  public val dropScale: UFloat by uniform(1f)
}
