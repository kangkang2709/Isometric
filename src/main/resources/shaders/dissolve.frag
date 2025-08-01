#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform sampler2D u_dissolveTexture;
uniform sampler2D u_burnTexture;
uniform float u_dissolveAmount;
uniform float u_dissolveEdgeWidth;
uniform vec3 u_dissolveEdgeColor;
uniform float u_time;
uniform float u_intensity;
uniform float u_edgeSharpness;
uniform vec2 u_dissolveDirection;
uniform int u_useBurnTexture;

void main() {
    vec4 texColor = texture2D(u_texture, v_texCoords);

    // Hiệu ứng nhẹ khi đang dissolve (ripple mờ)
    float distortion = sin(v_texCoords.y * 15.0 + u_time * 3.0) * 0.004 * (1.0 - u_dissolveAmount);
    vec2 distortedUV = v_texCoords + vec2(distortion, distortion * 0.5);

    // Dùng noise UV nhẹ
    vec2 noiseUV = distortedUV + vec2(
        sin(u_time * 2.0 + v_texCoords.y * 8.0) * 0.005,
        cos(u_time * 1.5 + v_texCoords.x * 6.0) * 0.005
    ) * (1.0 - u_dissolveAmount);

    float dissolveNoise = texture2D(u_dissolveTexture, noiseUV).r;
    float threshold = u_dissolveAmount;

    float edgeStart = threshold - u_dissolveEdgeWidth * 0.5;
    float edgeEnd = threshold + u_dissolveEdgeWidth * 0.5;

    float edgeFactor = 1.0 - smoothstep(
        edgeStart - u_edgeSharpness * dissolveNoise * 0.1,
        edgeEnd + u_edgeSharpness * dissolveNoise * 0.1,
        dissolveNoise
    );

    float visibilityMask = step(dissolveNoise, threshold);

    // Màu viền cháy đơn giản
    vec3 fireColor = u_dissolveEdgeColor * u_intensity * edgeFactor;

    // Optional burn texture
    if (u_useBurnTexture == 1) {
        vec2 burnUV = v_texCoords + vec2(sin(u_time * 2.0), cos(u_time)) * 0.005 * (1.0 - visibilityMask);
        vec4 burnColor = texture2D(u_burnTexture, burnUV);
        fireColor = mix(fireColor, burnColor.rgb * 1.2, 0.4);
    }

    vec3 finalColor = mix(fireColor, texColor.rgb, visibilityMask);
    float finalAlpha = texColor.a * (visibilityMask + (1.0 - visibilityMask) * edgeFactor * 0.9) * v_color.a;

    gl_FragColor = vec4(finalColor, finalAlpha);
}
