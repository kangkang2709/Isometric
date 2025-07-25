// blur.frag
#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform vec2 resolution;
uniform float radius;
varying vec2 v_texCoord;

void main() {
    vec4 sum = vec4(0.0);
    vec2 tc = v_texCoord;
    float blur = radius/resolution.x;

    // Gaussian blur with 9 samples
    sum += texture2D(u_texture, vec2(tc.x - 4.0*blur, tc.y)) * 0.05;
    sum += texture2D(u_texture, vec2(tc.x - 3.0*blur, tc.y)) * 0.09;
    sum += texture2D(u_texture, vec2(tc.x - 2.0*blur, tc.y)) * 0.12;
    sum += texture2D(u_texture, vec2(tc.x - blur, tc.y)) * 0.15;
    sum += texture2D(u_texture, vec2(tc.x, tc.y)) * 0.18;
    sum += texture2D(u_texture, vec2(tc.x + blur, tc.y)) * 0.15;
    sum += texture2D(u_texture, vec2(tc.x + 2.0*blur, tc.y)) * 0.12;
    sum += texture2D(u_texture, vec2(tc.x + 3.0*blur, tc.y)) * 0.09;
    sum += texture2D(u_texture, vec2(tc.x + 4.0*blur, tc.y)) * 0.05;

    gl_FragColor = sum;
}