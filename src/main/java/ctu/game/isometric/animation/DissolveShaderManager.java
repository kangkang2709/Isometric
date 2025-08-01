package ctu.game.isometric.animation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import java.util.Random;

public class DissolveShaderManager {
    private static ShaderProgram dissolveShader;
    private static Texture noiseTexture;
    private static float shaderTime = 0f;

    public static void initialize() {
        // Enable detailed shader logging
        ShaderProgram.pedantic = false;

        // Read shader files
        String vertexShader;
        String fragmentShader;

        try {
            vertexShader = Gdx.files.internal("shaders/dissolve.vert").readString();
            fragmentShader = Gdx.files.internal("shaders/dissolve.frag").readString();
        } catch (Exception e) {
            Gdx.app.error("DissolveShader", "Không thể đọc shader files: " + e.getMessage());
            // Fallback to embedded shaders
            vertexShader = getDefaultVertexShader();
            fragmentShader = getDefaultFragmentShader();
        }

        dissolveShader = new ShaderProgram(vertexShader, fragmentShader);
        if (!dissolveShader.isCompiled()) {
            Gdx.app.error("DissolveShader", "Lỗi biên dịch shader: " + dissolveShader.getLog());
        } else {
            Gdx.app.log("DissolveShader", "Shader khởi tạo thành công!");
        }

    }

    private static String getDefaultVertexShader() {
        return "attribute vec4 a_position;\n" +
                "attribute vec4 a_color;\n" +
                "attribute vec2 a_texCoord0;\n" +
                "uniform mat4 u_projTrans;\n" +
                "varying vec4 v_color;\n" +
                "varying vec2 v_texCoords;\n" +
                "void main() {\n" +
                "    v_color = a_color;\n" +
                "    v_texCoords = a_texCoord0;\n" +
                "    gl_Position = u_projTrans * a_position;\n" +
                "}";
    }

    private static String getDefaultFragmentShader() {
        return "#ifdef GL_ES\n" +
                "    precision mediump float;\n" +
                "#endif\n" +
                "varying vec4 v_color;\n" +
                "varying vec2 v_texCoords;\n" +
                "uniform sampler2D u_texture;\n" +
                "uniform sampler2D u_dissolveTexture;\n" +
                "uniform float u_dissolveAmount;\n" +
                "uniform float u_dissolveEdgeWidth;\n" +
                "uniform vec3 u_dissolveEdgeColor;\n" +
                "uniform float u_time;\n" +
                "void main() {\n" +
                "    vec4 texColor = texture2D(u_texture, v_texCoords);\n" +
                "    vec2 animatedUV = v_texCoords + vec2(u_time * 0.1, u_time * 0.05);\n" +
                "    float dissolveValue = texture2D(u_dissolveTexture, animatedUV).r;\n" +
                "    float threshold = 1.0 - u_dissolveAmount;\n" +
                "    float edgeStart = threshold - u_dissolveEdgeWidth;\n" +
                "    float edgeEnd = threshold;\n" +
                "    float edgeIntensity = smoothstep(edgeStart, edgeEnd, dissolveValue);\n" +
                "    float dissolveStep = step(threshold, dissolveValue);\n" +
                "    vec3 fireColor1 = vec3(1.0, 0.3, 0.0);\n" +
                "    vec3 fireColor2 = vec3(1.0, 0.8, 0.2);\n" +
                "    vec3 fireColor3 = vec3(0.8, 0.1, 0.1);\n" +
                "    vec3 edgeColor = mix(fireColor3, fireColor1, edgeIntensity);\n" +
                "    edgeColor = mix(edgeColor, fireColor2, pow(edgeIntensity, 2.0));\n" +
                "    vec3 finalColor = mix(edgeColor, texColor.rgb, dissolveStep);\n" +
                "    float finalAlpha = texColor.a * (dissolveStep + edgeIntensity * 0.8) * v_color.a;\n" +
                "    gl_FragColor = vec4(finalColor, finalAlpha);\n" +
                "}";
    }

    private static void createNoiseTexture() {
        int size = 512;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // Sử dụng nhiều octave noise để tạo cạnh tự nhiên hơn
        float[][] noise = new float[size][size];

        // Khởi tạo noise với nhiều tần số
        SimplexNoise.reseed(System.currentTimeMillis());

        // Tạo base noise
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // Kết hợp nhiều lớp noise với tần số khác nhau
                float nx = x / (float) size;
                float ny = y / (float) size;

                // Lớp noise chính
                float n1 = SimplexNoise.noise(nx * 3, ny * 3);
                // Lớp chi tiết thứ 1
                float n2 = SimplexNoise.noise(nx * 7, ny * 7) * 0.5f;
                // Lớp chi tiết thứ 2
                float n3 = SimplexNoise.noise(nx * 15, ny * 15) * 0.25f;
                // Lớp nhiễu nhỏ
                float n4 = SimplexNoise.noise(nx * 30, ny * 30) * 0.125f;

                // Tạo cạnh không đều
                float edgeNoise = Math.max(0, SimplexNoise.noise(nx * 2, ny * 5) * 0.8f + 0.2f);

                // Tạo hoa văn giống tro than
                float cellNoise = cellularNoise(nx * 10, ny * 10) * 0.4f;

                // Kết hợp các loại noise
                float finalNoise = (n1 + n2 + n3 + n4) * 0.4f + edgeNoise * 0.4f + cellNoise * 0.2f;
                noise[x][y] = Math.max(0.0f, Math.min(1.0f, finalNoise * 0.5f + 0.5f));
            }
        }

        // Làm mịn và tạo mẫu giống lửa cháy
        smoothFirePattern(noise, size);

        // Vẽ noise vào pixmap
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float val = noise[x][y];
                pixmap.setColor(val, val, val, 1);
                pixmap.drawPixel(x, y);
            }
        }

        if (noiseTexture != null) noiseTexture.dispose();
        noiseTexture = new Texture(pixmap);
        noiseTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        pixmap.dispose();
    }

    // Hàm helper tạo cellular noise (Worley noise) cho hiệu ứng giống than cháy
    private static float cellularNoise(float x, float y) {
        int pointCount = 20;
        float minDist = 1.0f;

        for (int i = 0; i < pointCount; i++) {
            // Tạo điểm ngẫu nhiên ổn định
            float px = (float) Math.sin(i * 367.2 + 4.7) * 10000;
            float py = (float) Math.cos(i * 389.2 + 7.1) * 10000;
            px = px - (float) Math.floor(px);
            py = py - (float) Math.floor(py);

            float dx = x - px;
            float dy = y - py;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            minDist = Math.min(minDist, dist);
        }

        return minDist;
    }

    // Làm mịn và tạo hiệu ứng giống lửa cháy
    private static void smoothFirePattern(float[][] noise, int size) {
        float[][] temp = new float[size][size];

        // Copy
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                temp[x][y] = noise[x][y];
            }
        }

        // Làm mịn theo hướng từ dưới lên để tạo hiệu ứng lửa cháy
        for (int y = 1; y < size - 1; y++) {
            for (int x = 1; x < size - 1; x++) {
                // Làm mịn dựa trên hàng xóm
                float sum = 0;
                sum += temp[x - 1][y] * 0.15f;
                sum += temp[x + 1][y] * 0.15f;
                sum += temp[x][y - 1] * 0.20f; // Lấy nhiều hơn từ phía dưới
                sum += temp[x][y + 1] * 0.10f; // Lấy ít hơn từ phía trên
                sum += temp[x][y] * 0.4f;    // Giữ lại 40% giá trị gốc

                // Thêm hiệu ứng "lửa bốc lên"
                float yFactor = (float) y / size;
                sum = sum * (0.8f + yFactor * 0.2f);

                noise[x][y] = sum;
            }
        }
    }

    static Texture burnTexture;

    public static Texture getBurnTexture() {
        if (burnTexture == null) {
            burnTexture = new Texture(Gdx.files.internal("textures/burn_nosise.png"));
            burnTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        }

        return burnTexture;
    }

    public static Texture getNoiseTexture() {
        if (noiseTexture == null) {
            createNoiseTexture();
        }
        return noiseTexture;
    }

    private static Texture createBurnTexture() {
        int size = 256;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // Tạo texture giống lửa thật
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float nx = x / (float) size;
                float ny = y / (float) size;

                // Tạo mẫu lửa với nhiều layer
                float layer1 = SimplexNoise.noise(nx * 5, ny * 8 + 30) * 0.5f + 0.5f;
                float layer2 = SimplexNoise.noise(nx * 10, ny * 16 + 10) * 0.25f + 0.75f;
                float layer3 = SimplexNoise.noise(nx * 20, ny * 32 + 20) * 0.125f + 0.875f;

                // Tạo gradient từ dưới lên cho hiệu ứng lửa bốc
                float yGradient = (1.0f - ny) * (1.0f - ny); // Mạnh hơn ở dưới

                // Tính màu lửa
                float r = Math.min(1.0f, layer1 * 1.5f) * yGradient;
                float g = Math.min(1.0f, layer2 * 0.8f) * yGradient;
                float b = Math.min(1.0f, layer3 * 0.3f) * yGradient;
                float a = yGradient * 0.9f;

                // Set màu
                pixmap.setColor(r, g, b, a);
                pixmap.drawPixel(x, y);
            }
        }

        Texture tex = new Texture(pixmap);
        tex.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        pixmap.dispose();

        return tex;
    }


    public static void update(float delta) {
        shaderTime += delta;
    }

    public static ShaderProgram getDissolveShader() {
        return dissolveShader;
    }


    public static float getShaderTime() {
        return shaderTime;
    }

    public static void dispose() {
        if (dissolveShader != null) {
            dissolveShader.dispose();
            Gdx.app.log("DissolveShader", "Shader đã được dispose");
        }
        if (noiseTexture != null) {
            noiseTexture.dispose();
            Gdx.app.log("DissolveShader", "Noise texture đã được dispose");
        }
    }
}