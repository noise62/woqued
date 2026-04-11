package sweetie.evaware.client.features.modules.render.motionblur;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.render.Render3DEvent;
import sweetie.evaware.api.system.backend.ClientInfo;
import sweetie.evaware.api.utils.other.TextUtil;

public class ShaderMotionBlur {
    private final MotionBlurModule config;
    private final ManagedShaderEffect motionBlurShader;

    public ShaderMotionBlur(MotionBlurModule config) {
        this.config = config;
        motionBlurShader = ShaderEffectManager.getInstance().manage(
                Identifier.of("evaware", "motion_blur"),
                shader -> shader.setUniformValue("BlendFactor", config.strength.getValue())
        );
    }

    private long lastNano;
    private float currentBlur = 0.0f;
    private float currentFPS = 0.0f;

    public void registerShaderCallbacks() {
        Render3DEvent.getInstance().subscribe(new Listener<>(-1, event -> {
            long now = System.nanoTime();
            float deltaTime = (now - lastNano) / 1_000_000_000.0f;
            float deltaTick = deltaTime * 20.0f;
            lastNano = now;

            if (deltaTime > 0 && deltaTime < 1.0f) {
                currentFPS = 1.0f / deltaTime;
            } else {
                currentFPS = 0.0f;
            }

            if (shouldRenderMotionBlur()) {
                applyMotionBlur(deltaTick);
            }
        }));
    }

    private boolean shouldRenderMotionBlur() {
        if (config.strength.getValue() == 0 || !config.isEnabled()) {
            return false;
        }
        if (FabricLoader.getInstance().isModLoaded("iris")) {
            TextUtil.sendMessage("Не могу нах включить, потому что Iris стоит!");
            config.setEnabled(false);
            return false;
        }

        return true;
    }

    private void applyMotionBlur(float deltaTick) {
        MinecraftClient client = MinecraftClient.getInstance();

        MonitorInfoProvider.updateDisplayInfo();
        int displayRefreshRate = MonitorInfoProvider.getRefreshRate();

        float baseStrength = config.strength.getValue();
        float scaledStrength = baseStrength;
        if (config.useRRC.getValue()) {
            float fpsOverRefresh = (displayRefreshRate > 0) ? currentFPS / displayRefreshRate : 1.0f;
            if (fpsOverRefresh < 1.0f) fpsOverRefresh = 1.0f;
            scaledStrength = baseStrength * fpsOverRefresh;
        }

        if (currentBlur != scaledStrength) {
            motionBlurShader.setUniformValue("BlendFactor", scaledStrength);
            currentBlur = scaledStrength;
        }

        int sampleAmount = getSampleAmountForFPS(currentFPS);
        int halfSampleAmount = sampleAmount / 2;
        float invSamples = 1.0f / sampleAmount;

        motionBlurShader.setUniformValue("view_res", (float) client.getFramebuffer().viewportWidth, (float) client.getFramebuffer().viewportHeight);
        motionBlurShader.setUniformValue("view_pixel_size", 1.0f / client.getFramebuffer().viewportWidth, 1.0f / client.getFramebuffer().viewportHeight);
        motionBlurShader.setUniformValue("motionBlurSamples", sampleAmount);
        motionBlurShader.setUniformValue("halfSamples", halfSampleAmount);
        motionBlurShader.setUniformValue("inverseSamples", invSamples);
        motionBlurShader.setUniformValue("blurAlgorithm", MotionBlurModule.BlurAlgorithm.BACKWARDS.ordinal());

        motionBlurShader.render(deltaTick);
    }

    private int getSampleAmountForFPS(float fps) {
        if (fps > 360) return 8;
        else if (fps > 120) return 10;
        else if (fps > 60) return 12;
        else return 20;
    }

    private final Matrix4f tempPrevModelView = new Matrix4f();
    private final Matrix4f tempPrevProjection = new Matrix4f();
    private final Matrix4f tempProjInverse = new Matrix4f();
    private final Matrix4f tempMvInverse = new Matrix4f();

    public void setFrameMotionBlur(Matrix4f modelView, Matrix4f prevModelView,
                                   Matrix4f projection, Matrix4f prevProjection,
                                   Vector3f cameraPos, Vector3f prevCameraPos) {
        motionBlurShader.setUniformValue("mvInverse", tempMvInverse.set(modelView).invert());
        motionBlurShader.setUniformValue("projInverse", tempProjInverse.set(projection).invert());
        motionBlurShader.setUniformValue("prevModelView", tempPrevModelView.set(prevModelView));
        motionBlurShader.setUniformValue("prevProjection", tempPrevProjection.set(prevProjection));
        motionBlurShader.setUniformValue("cameraPos", cameraPos.x, cameraPos.y, cameraPos.z);
        motionBlurShader.setUniformValue("prevCameraPos", prevCameraPos.x, prevCameraPos.y, prevCameraPos.z);
    }


    public void updateBlurStrength(float strength) {
        motionBlurShader.setUniformValue("BlendFactor", strength);
        currentBlur = strength;
    }
}
