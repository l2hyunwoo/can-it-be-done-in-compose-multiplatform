@file:OptIn(ExperimentalMirage::class)

package io.github.l2hyunwoo.compose.morphsearch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.skydoves.cloudy.CompositeShader
import com.skydoves.cloudy.ExperimentalMirage
import androidx.compose.ui.graphics.Color
import com.skydoves.cloudy.MirageParams
import com.skydoves.cloudy.MirageShader
import com.skydoves.cloudy.UColor
import com.skydoves.cloudy.UFloat
import com.skydoves.cloudy.UOffset
import com.skydoves.cloudy.USize
import com.skydoves.cloudy.edsl.Float1
import com.skydoves.cloudy.edsl.Float1Type
import com.skydoves.cloudy.edsl.Float2
import com.skydoves.cloudy.edsl.Float2Type
import com.skydoves.cloudy.edsl.Half4
import com.skydoves.cloudy.edsl.If
import com.skydoves.cloudy.edsl.a
import com.skydoves.cloudy.edsl.and
import com.skydoves.cloudy.edsl.boxRoundedSDF
import com.skydoves.cloudy.edsl.clamp
import com.skydoves.cloudy.edsl.div
import com.skydoves.cloudy.edsl.dot
import com.skydoves.cloudy.edsl.float1
import com.skydoves.cloudy.edsl.float2
import com.skydoves.cloudy.edsl.float3
import com.skydoves.cloudy.edsl.float4
import com.skydoves.cloudy.edsl.greaterThan
import com.skydoves.cloudy.edsl.guard
import com.skydoves.cloudy.edsl.half3
import com.skydoves.cloudy.edsl.half4
import com.skydoves.cloudy.edsl.length
import com.skydoves.cloudy.edsl.lessThanEqual
import com.skydoves.cloudy.edsl.local
import com.skydoves.cloudy.edsl.max
import com.skydoves.cloudy.edsl.min
import com.skydoves.cloudy.edsl.minus
import com.skydoves.cloudy.edsl.mix
import com.skydoves.cloudy.edsl.normalize
import com.skydoves.cloudy.edsl.plus
import com.skydoves.cloudy.edsl.pow
import com.skydoves.cloudy.edsl.processColor
import com.skydoves.cloudy.edsl.rgb
import com.skydoves.cloudy.edsl.sampleContent
import com.skydoves.cloudy.edsl.shaderFunction
import com.skydoves.cloudy.edsl.smoothstep
import com.skydoves.cloudy.edsl.sqrt
import com.skydoves.cloudy.edsl.times
import com.skydoves.cloudy.edsl.unaryMinus
import com.skydoves.cloudy.edsl.x
import com.skydoves.cloudy.edsl.y

// Blob geometry is individual UOffset/UFloat handles, not a float[N] array: cloudy's UFloatArray
// uniform is not readable inside a traced eDSL body.
@ExperimentalMirage
class MorphSearchParams : MirageParams() {
  val iLight: UOffset by uniform(Offset(-1f, -1f))
  val pillCenter: UOffset by uniform(Offset.Zero)
  val pillHalf: USize by uniform(Size(1f, 1f))

  val blob0: UOffset by uniform(Offset.Zero)
  val blob1: UOffset by uniform(Offset.Zero)
  val blob2: UOffset by uniform(Offset.Zero)
  val blob3: UOffset by uniform(Offset.Zero)
  val r0: UFloat by uniform(0f)
  val r1: UFloat by uniform(0f)
  val r2: UFloat by uniform(0f)
  val r3: UFloat by uniform(0f)

  /** High -> goo neck, 0 -> plain min (independent lenses). */
  val smin: UFloat by uniform(0f)

  /** Global glass opacity; dismissal animates this with the geometry frozen. */
  val fade: UFloat by uniform(1f)

  /** rgb = tint colour, a = mix amount. */
  val tint: UColor by uniformColor(Color(0f, 0f, 0f, 0f))

  val specStrength: UFloat by uniform(0.7f)
}

@ExperimentalMirage
fun MorphSearchParams.apply(
  layout: MorphLayout,
  light: Offset,
  theme: GlassTheme = GlassTheme.Dark,
  visibility: Float = 1f,
) {
  iLight(light)
  pillCenter(layout.pillCenter)
  pillHalf(layout.pillHalfSize)
  blob0(layout.blobs[0].center); r0(layout.blobs[0].radius)
  blob1(layout.blobs[1].center); r1(layout.blobs[1].radius)
  blob2(layout.blobs[2].center); r2(layout.blobs[2].radius)
  blob3(layout.blobs[3].center); r3(layout.blobs[3].radius)
  smin(layout.sminK)
  fade(visibility)
  tint(theme.tint)
  specStrength(theme.specStrength)
}

// The rim/specular is rebuilt from the union-gradient normal rather than the rounded-rect's L-inf
// specDir, so at settle (smin -> 0) the look approximates liquidGlass/Specular but is not bit-exact.
@ExperimentalMirage
val MorphSearchShader: CompositeShader<MorphSearchParams> =
  MirageShader.composite("morphSearch", ::MorphSearchParams) { xy ->
    val smoothEdgePx = 1.5f

    // A shaderFunction helper so the 3 finite-difference taps below emit calls, not inlined copies.
    val unionSDF by shaderFunction(Float2Type, Float1Type) { p ->
      var acc by local(boxRoundedSDF(p - pillCenter, pillHalf, min(pillHalf.x, pillHalf.y)))
      acc = smoothMin(acc, length(p - blob0) - r0, smin)
      acc = smoothMin(acc, length(p - blob1) - r1, smin)
      acc = smoothMin(acc, length(p - blob2) - r2, smin)
      acc = smoothMin(acc, length(p - blob3) - r3, smin)
      acc
    }

    val sdf = unionSDF(xy)
    guard(sdf greaterThan smoothEdgePx) { sampleContent(xy) }

    val eps = 1.5f
    val dx = unionSDF(xy + float2(eps, 0f)) - unionSDF(xy - float2(eps, 0f))
    val dy = unionSDF(xy + float2(0f, eps)) - unionSDF(xy - float2(0f, eps))
    val normal = normalize(float2(dx, dy) + float2(1.0e-4f, 1.0e-4f)) // guards the flat-interior case

    // LiquidGlass's depth -> curvature -> bend -> offset, with minFeature standing in for its
    // single-lens minDim so a small blob bends as hard as the big pill. refraction/curve match
    // LiquidGlassDefaults; not exposed as uniforms since no theme overrides them.
    val refraction = 0.25f
    val curve = 0.25f
    val minFeature = clamp(-sdf * 4f + 24f, 24f, 220f)
    val depth = clamp(-sdf / (minFeature * refraction), 0f, 1f)
    val curvature = 1f - depth
    val bend = 1f - sqrt(max(1f - curvature * curvature, 0f))
    val sampleXY = xy - normal * (bend * curve * minFeature)

    var pixel by local(sampleContent(sampleXY))
    If(pixel.a lessThanEqual 0f) { pixel = sampleContent(xy) }
    pixel = half4(processColor(pixel.rgb, 1f, 1f, float4(0f, 0f, 0f, 0f)), pixel.a)
    pixel = half4(mix(pixel.rgb, tint.rgb, tint.a), pixel.a)

    If(specStrength greaterThan 0f) {
      val specPower = 10.0f
      val specWidthPx = 14.0f
      val specLightZ = 0.6f
      val lightVec = normalize(iLight)
      val t = clamp(-sdf / specWidthPx, 0f, 1f)
      val nz = sqrt(max(1f - (1f - t) * (1f - t), 0f)) + 1.0e-3f
      val nn = normalize(float3(normal * (1f - t), nz))

      val ll = normalize(float3(lightVec, specLightZ))
      val vv = float3(0f, 0f, 1f)
      val hh = normalize(ll + vv)

      val rimBand = smoothstep(-specWidthPx, 0f, sdf)
      val glint = pow(max(dot(nn, hh), 0f), specPower) * specStrength
      val ndl = max(dot(nn, ll), 0f)
      val bodySheen = pow(ndl, 2.5f) * specStrength * 0.5f
      val highlight = clamp(glint * rimBand + bodySheen * rimBand, 0f, 1f)

      pixel = half4(pixel.rgb + (half3(1f) - pixel.rgb) * highlight, pixel.a)
    }

    val alpha = (1f - smoothstep(-smoothEdgePx * 0.5f, smoothEdgePx * 0.5f, sdf)) * fade
    val bg = sampleContent(xy)
    mix(bg, pixel, alpha)
  }

/** Polynomial smooth-min (Inigo Quilez); k -> 0 reduces to plain min. */
@ExperimentalMirage
private fun smoothMin(a: Float1, b: Float1, k: Float1): Float1 {
  val kk = max(k, float1(1.0e-3f))
  val h = clamp(0.5f + 0.5f * (b - a) / kk, 0f, 1f)
  return mix(b, a, h) - kk * h * (1f - h)
}
