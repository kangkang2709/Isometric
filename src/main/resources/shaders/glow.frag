// glow.frag
#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform float u_intensity;
varying vec2 v_texCoord;

void main() {
    vec4 texColor = texture2D(u_texture, v_texCoord);

    // Add glow effect by blending towards bright blue for lighter pixels
    vec3 glowColor = vec3(0.2, 0.7, 1.0);
    float intensity = texColor.a * u_intensity;

    texColor.rgb = mix(texColor.rgb, glowColor, intensity * 0.5);
    texColor.a *= 0.8;

    gl_FragColor = texColor;
}