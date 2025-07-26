#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2 u_lightPosition;
uniform float u_lightRadius;
uniform float u_ambientLight;
uniform vec2 u_resolution;

void main() {
    vec4 texColor = texture2D(u_texture, v_texCoords) * v_color;

    // Convert fragment position to world coordinates
    vec2 fragPos = gl_FragCoord.xy / u_resolution;

    // Calculate distance from light source
    float distance = length(fragPos - u_lightPosition);

    // Calculate light intensity with smooth falloff
    float lightIntensity = 1.0 - smoothstep(0.0, u_lightRadius, distance);

    // Combine ambient and dynamic light
    float totalLight = clamp(u_ambientLight + lightIntensity, u_ambientLight, 1.0);

    // Apply lighting to the texture
    gl_FragColor = vec4(texColor.rgb * totalLight, texColor.a);
}