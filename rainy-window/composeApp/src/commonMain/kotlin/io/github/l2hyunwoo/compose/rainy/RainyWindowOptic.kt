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

/** Rain on steamed glass: running refracting raindrops + condensation you wipe clear with a finger. */
public object RainyWindowOptic {

  /** The rainy-window composite shader. Full-bleed by design. */
  public val RainyWindow: CompositeShader<RainyWindowParams> = MirageShader.composite(
    name = "rainyWindow",
    paramsFactory = ::RainyWindowParams,
    agsl = RAINY_WINDOW_KERNEL,
    sksl = RAINY_WINDOW_KERNEL,
  )
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

private const val RAINY_WINDOW_KERNEL: String = """
// Drop field: "Heartfelt" by Martijn Steinrucken (BigWings), https://www.shadertoy.com/view/ltffzl
// CC BY-NC-SA 3.0 — non-commercial, share-alike. Ported to AGSL/SKSL. See README.

float3 N13(float p) {
    float3 p3 = fract(float3(p, p, p) * float3(0.1031, 0.11369, 0.13787));
    p3 += dot(p3, p3.yzx + 19.19);
    return fract(float3((p3.x + p3.y) * p3.z, (p3.x + p3.z) * p3.y, (p3.y + p3.z) * p3.x));
}
float N(float t) { return fract(sin(t * 12345.564) * 7658.76); }
float Saw(float b, float t) { return smoothstep(0.0, b, t) * smoothstep(1.0, b, t); }

float2 DropLayer2(float2 uv, float t) {
    float2 UV = uv;
    uv.y += t * 0.75;
    float2 a = float2(6.0, 1.0);
    float2 grid = a * 2.0;
    float2 id = floor(uv * grid);
    float colShift = N(id.x);
    uv.y += colShift;
    id = floor(uv * grid);
    float3 n = N13(id.x * 35.2 + id.y * 2376.1);
    float2 st = fract(uv * grid) - float2(0.5, 0.0);
    float x = n.x - 0.5;
    float y = UV.y * 20.0;
    float wiggle = sin(y + sin(y));
    x += wiggle * (0.5 - abs(x)) * (n.z - 0.5);
    x *= 0.7;
    float ti = fract(t + n.z);
    y = (Saw(0.85, ti) - 0.5) * 0.9 + 0.5;
    float2 p = float2(x, y);
    float d = length((st - p) * a.yx);
    float mainDrop = smoothstep(0.4, 0.0, d);
    float r = sqrt(smoothstep(1.0, y, st.y));
    float cd = abs(st.x - x);
    float trail = smoothstep(0.23 * r, 0.15 * r * r, cd);
    float trailFront = smoothstep(-0.02, 0.02, st.y - y);
    trail *= trailFront * r * r;
    y = UV.y;
    float trail2 = smoothstep(0.2 * r, 0.0, cd);
    float droplets = max(0.0, (sin(y * (1.0 - y) * 120.0) - st.y)) * trail2 * trailFront * n.z;
    y = fract(y * 10.0) + (st.y - 0.5);
    float dd = length(st - float2(x, y));
    droplets = smoothstep(0.3, 0.0, dd);
    float m = mainDrop + droplets * r * trailFront;
    return float2(m, trail);
}

float StaticDrops(float2 uv, float t) {
    uv *= 40.0;
    float2 id = floor(uv);
    uv = fract(uv) - 0.5;
    float3 n = N13(id.x * 107.45 + id.y * 3543.654);
    float2 p = (n.xy - 0.5) * 0.7;
    float d = length(uv - p);
    float fade = Saw(0.025, fract(t + n.z));
    float c = smoothstep(0.3, 0.0, d) * fract(n.z * 10.0) * fade;
    return c;
}

float2 Drops(float2 uv, float t, float l0, float l1, float l2) {
    float s = StaticDrops(uv, t) * l0;
    float2 m1 = DropLayer2(uv, t) * l1;
    float2 m2 = DropLayer2(uv * 1.85, t) * l2;
    float c = s + m1.x + m2.x;
    c = smoothstep(0.3, 1.0, c);
    return float2(c, max(m1.y * l0, m2.y * l1));
}

half4 main(float2 xy) {
    float2 res = mirageResolution;
    float amount = clamp(rainAmount, 0.0, 1.0);

    float2 UV = xy / max(res, float2(1.0));
    float2 uv = (xy - 0.5 * res) / max(res.y, 1.0);
    uv.y = -uv.y;
    float scale = max(dropScale, 0.2);
    uv *= scale;

    float T = mod(mirageTime, 120.0);
    float t = T * 0.2;

    float2 maskUV = clamp(xy / max(res, float2(1.0)), 0.0, 1.0) * maskSize;
    float2 mlo = float2(0.5);
    float2 mhi = float2(maskSize - 0.5);
    float2 base = floor(maskUV - 0.5) + 0.5;
    float2 f = fract(maskUV - 0.5);
    float w00 = clamp(float(wipeMask.eval(clamp(base,                   mlo, mhi)).r), 0.0, 1.0);
    float w10 = clamp(float(wipeMask.eval(clamp(base + float2(1.0, 0.0), mlo, mhi)).r), 0.0, 1.0);
    float w01 = clamp(float(wipeMask.eval(clamp(base + float2(0.0, 1.0), mlo, mhi)).r), 0.0, 1.0);
    float w11 = clamp(float(wipeMask.eval(clamp(base + float2(1.0, 1.0), mlo, mhi)).r), 0.0, 1.0);
    float wiped = mix(mix(w00, w10, f.x), mix(w01, w11, f.x), f.y);

    float maxBlur = mix(3.0, 6.0, amount);
    float minBlur = 2.0;
    float staticDrops = smoothstep(-0.5, 1.0, amount) * 2.0;
    float layer1 = smoothstep(0.25, 0.75, amount);
    float layer2 = smoothstep(0.0, 0.5, amount);

    float2 c = Drops(uv, t, staticDrops, layer1, layer2);

    float2 e = float2(0.001, 0.0);
    float cx = Drops(uv + e,    t, staticDrops, layer1, layer2).x;
    float cy = Drops(uv + e.yx, t, staticDrops, layer1, layer2).x;
    float2 n = float2(cx - c.x, cy - c.x);
    n.y = -n.y;

    float focus = mix(maxBlur - c.y, minBlur, smoothstep(0.1, 0.2, c.x));
    focus = mix(focus, minBlur, clamp(wiped, 0.0, 1.0));
    float foggy = clamp((focus - minBlur) / max(maxBlur - minBlur, 1e-3), 0.0, 1.0);
    foggy *= clamp(fogAmount, 0.0, 1.0);

    float2 nPx = n * res;
    float nLen = length(nPx);
    nPx *= min(nLen, 24.0) / max(nLen, 1e-4);
    float2 tap = clamp((UV * res) + nPx, float2(0.5), res - 0.5);
    half4 sharpBg = content.eval(tap);

    float st = max(blurRadius, 0.0) * foggy;
    float st2 = st * 2.0;

    half4 fogged = sharpBg;
    if (foggy > 0.01) {
        half4 blur = sharpBg
                   + content.eval(clamp(tap + float2(-st2, -st2), float2(0.5), res - 0.5))
                   + content.eval(clamp(tap + float2( st2, -st2), float2(0.5), res - 0.5))
                   + content.eval(clamp(tap + float2(-st2,  st2), float2(0.5), res - 0.5))
                   + content.eval(clamp(tap + float2( st2,  st2), float2(0.5), res - 0.5));
        blur *= half(1.0 / 5.0);
        fogged = mix(sharpBg, blur, half(foggy));
    }
    half3 haze = half3(0.86, 0.90, 0.94);
    fogged.rgb = mix(fogged.rgb, haze, half(foggy) * half(clamp(hazeStrength, 0.0, 1.0)));

    half4 photo = content.eval(clamp(xy, float2(0.5), res - 0.5));

    half4 col = mix(photo, fogged, half(clamp(introProgress, 0.0, 1.0)));
    return col;
}
"""
