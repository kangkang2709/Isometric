package ctu.game.isometric.view.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class OctopathCombatEnvironment3D {
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private BattleTerrainGenerator terrainGenerator;

    // Octopath-style effects
    private FrameBuffer sceneBuffer;
    private FrameBuffer blurBuffer;
    private ShaderProgram blurShader;
    private ShaderProgram dofShader;
    private ShaderProgram lightRayShader;

    // Camera animation
    private Vector3 baseCameraPosition = new Vector3(0f, 12f, 15f);
    private Vector3 targetCameraPosition = new Vector3();
    private float cameraTransitionSpeed = 2.0f;
    private boolean isTransitioning = false;

    // Depth of field parameters
    private float focusDistance = 10f;
    private float focusRange = 5f;
    private float blurStrength = 0.8f;

    public OctopathCombatEnvironment3D() {
        setupCamera();
        setupEnvironment();
        setupShaders();
        setupFrameBuffers();

        terrainGenerator = new BattleTerrainGenerator();

        targetCameraPosition.set(baseCameraPosition);
    }

    private void setupCamera() {
        camera = new PerspectiveCamera(45f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(baseCameraPosition);
        camera.lookAt(0f, 2f, 0f);
        camera.near = 0.1f;
        camera.far = 100f;
        camera.update();
    }

    private void setupEnvironment() {
        environment = new Environment();

        // Ambient light với màu sắc ấm áp giống Octopath
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.7f, 0.8f, 1f));

        // Main directional light (ánh sáng mặt trời)
        DirectionalLight sunLight = new DirectionalLight();
        sunLight.set(0.9f, 0.95f, 0.8f, -0.5f, -0.8f, -0.3f);
        environment.add(sunLight);

        // Rim light để tạo hiệu ứng viền giống Octopath
        DirectionalLight rimLight = new DirectionalLight();
        rimLight.set(0.3f, 0.4f, 0.6f, 0.8f, 0.2f, 0.5f);
        environment.add(rimLight);

        modelBatch = new ModelBatch();
    }

    private void setupFrameBuffers() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();

        sceneBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, true);
        blurBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, width/2, height/2, false);
    }

    private void setupShaders() {
        // Blur shader cho background blur effect
        String blurVertexShader = """
            attribute vec4 a_position;
            attribute vec2 a_texCoord0;
            varying vec2 v_texCoords;
            
            void main() {
                v_texCoords = a_texCoord0;
                gl_Position = a_position;
            }
        """;

        String blurFragmentShader = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform vec2 u_resolution;
            uniform float u_blurRadius;
            
            void main() {
                vec4 color = vec4(0.0);
                vec2 off1 = vec2(1.3846153846) * u_blurRadius / u_resolution;
                vec2 off2 = vec2(3.2307692308) * u_blurRadius / u_resolution;
                
                color += texture2D(u_texture, v_texCoords) * 0.2270270270;
                color += texture2D(u_texture, v_texCoords + off1) * 0.3162162162;
                color += texture2D(u_texture, v_texCoords - off1) * 0.3162162162;
                color += texture2D(u_texture, v_texCoords + off2) * 0.0702702703;
                color += texture2D(u_texture, v_texCoords - off2) * 0.0702702703;
                
                gl_FragColor = color;
            }
        """;

        blurShader = new ShaderProgram(blurVertexShader, blurFragmentShader);
        if (!blurShader.isCompiled()) {
            Gdx.app.error("BlurShader", blurShader.getLog());
        }

        // Depth of Field shader
        String dofFragmentShader = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform sampler2D u_depthTexture;
            uniform float u_focusDistance;
            uniform float u_focusRange;
            uniform float u_blurStrength;
            
            void main() {
                vec4 color = texture2D(u_texture, v_texCoords);
                float depth = texture2D(u_depthTexture, v_texCoords).r;
                
                float blur = abs(depth - u_focusDistance) / u_focusRange;
                blur = clamp(blur * u_blurStrength, 0.0, 1.0);
                
                // Tạo hiệu ứng bokeh đơn giản
                vec4 blurredColor = color * 0.8;
                gl_FragColor = mix(color, blurredColor, blur);
            }
        """;

        dofShader = new ShaderProgram(blurVertexShader, dofFragmentShader);

        // Light ray shader cho hiệu ứng ánh sáng
        String lightRayFragmentShader = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            
            varying vec2 v_texCoords;
            uniform sampler2D u_texture;
            uniform vec2 u_lightPosition;
            uniform float u_intensity;
            uniform float u_time;
            
            void main() {
                vec4 color = texture2D(u_texture, v_texCoords);
                
                vec2 direction = v_texCoords - u_lightPosition;
                float distance = length(direction);
                
                // Tạo tia sáng với animation
                float rayEffect = sin(distance * 10.0 - u_time * 3.0) * 0.5 + 0.5;
                rayEffect *= exp(-distance * 2.0) * u_intensity;
                
                color.rgb += vec3(rayEffect * 0.3, rayEffect * 0.25, rayEffect * 0.1);
                gl_FragColor = color;
            }
        """;

        lightRayShader = new ShaderProgram(blurVertexShader, lightRayFragmentShader);
    }

    public void render() {
        updateCamera();

        // Render scene to framebuffer
        sceneBuffer.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        renderScene();
        sceneBuffer.end();

        // Apply post-processing effects
        applyOctopathEffects();
    }

    private void renderScene() {
        modelBatch.begin(camera);

        // Render terrain
        modelBatch.render(terrainGenerator.getTerrainInstance(), environment);

        modelBatch.end();
    }

    private void applyOctopathEffects() {
        // Disable depth test for post-processing
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);

        // Apply background blur effect during combat transitions
        if (isTransitioning) {
            applyBackgroundBlur();
        }

        // Apply depth of field
        applyDepthOfField();

        // Apply light rays
        applyLightRays();

        // Re-enable depth test
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    private void applyBackgroundBlur() {
        // Implementation của background blur
        // Render blurred background khi có combat transition
    }

    private void applyDepthOfField() {
        // Implementation của depth of field effect
        // Tạo hiệu ứng focus vào character trong combat
    }

    private void applyLightRays() {
        // Implementation của light ray effects
        // Tạo tia sáng từ các skill effects
    }

    private void updateCamera() {
        if (isTransitioning) {
            camera.position.lerp(targetCameraPosition, cameraTransitionSpeed * Gdx.graphics.getDeltaTime());

            if (camera.position.dst(targetCameraPosition) < 0.1f) {
                isTransitioning = false;
            }

            camera.update();
        }
    }

    // Camera transitions giống Octopath Traveler
    public void focusOnCharacter(Vector3 characterPosition, float zoomLevel) {
        Vector3 newPosition = characterPosition.cpy().add(0, 8f * zoomLevel, 12f * zoomLevel);
        transitionCameraTo(newPosition);
    }

    public void resetCameraToDefault() {
        transitionCameraTo(baseCameraPosition);
    }

    private void transitionCameraTo(Vector3 position) {
        targetCameraPosition.set(position);
        isTransitioning = true;
    }

    // Battle-specific camera movements
    public void triggerSkillCameraMovement(String skillType) {
        switch (skillType) {
            case "ATTACK":
                focusOnCharacter(new Vector3(0, 1, 0), 0.7f);
                break;
            case "MAGIC":
                // Camera cao hơn cho magic effects
                transitionCameraTo(new Vector3(0, 15f, 10f));
                break;
            case "SPECIAL":
                // Dramatic angle cho special attacks
                transitionCameraTo(new Vector3(-5f, 10f, 8f));
                break;
        }
    }

    public PerspectiveCamera getCamera() {
        return camera;
    }

    public void dispose() {
        modelBatch.dispose();
        terrainGenerator.dispose();
        sceneBuffer.dispose();
        blurBuffer.dispose();
        blurShader.dispose();
        dofShader.dispose();
        lightRayShader.dispose();
    }
}