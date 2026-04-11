package sweetie.evaware.api.utils.neuro;

import ai.catboost.CatBoostModel;
import ai.catboost.CatBoostPredictions;
import lombok.SneakyThrows;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.player.world.AttackEvent;
import sweetie.evaware.api.system.backend.ClientInfo;
import sweetie.evaware.api.system.backend.Configurable;
import sweetie.evaware.api.system.files.FileUtil;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.animation.wrap.infinity.RotationAnimation;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.api.utils.player.PlayerUtil;
import sweetie.evaware.api.utils.rotation.RotationUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AIPredictor extends Configurable implements QuickImports {
    private CatBoostModel yawModel;
    private CatBoostModel pitchModel;
    private final TimerUtil timer = new TimerUtil();
    public RotationAnimation interpolation = new RotationAnimation();

    @Override
    public void onEvent() {
        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            timer.reset();
        }));

        addEvents(attackEvent);
    }

    public void loadModel(String model) {
        if (!model.equals("Default")) {
            try {
                String pisun = ClientInfo.CONFIG_PATH_AI_MODELS + "/" + model;
                yawModel = CatBoostModel.loadModel(pisun + "_yaw.model");
                pitchModel = CatBoostModel.loadModel(pisun + "_pitch.model");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                yawModel = loadModelFromResource(model.toLowerCase() + "_yaw.model");
                pitchModel = loadModelFromResource(model.toLowerCase() + "_pitch.model");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private CatBoostModel loadModelFromResource(String resourceName) throws Exception {
        InputStream is = FileUtil.getFromAssets("models/" + resourceName);
        if (is == null) throw new FileNotFoundException(resourceName);

        File temp = File.createTempFile(resourceName, ".cbm");
        temp.deleteOnExit();

        byte[] buffer = is.readAllBytes();
        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write(buffer);
        }
        return CatBoostModel.loadModel(temp.getAbsolutePath());
    }

    public boolean isLoaded() {
        return yawModel != null && pitchModel != null;
    }

    @SneakyThrows
    public void close() {
        if (yawModel != null) yawModel.close();
        if (pitchModel != null) pitchModel.close();
        yawModel = null;
        pitchModel = null;
    }

    @SneakyThrows
    public Rotation predict(Entity target, Rotation current, Rotation prev, Vector2f speed) {
        if (!isLoaded()) return current;

        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = target.getPos();
        Vec3d playerVel = Vec3d.ZERO;
        Vec3d targetVel = Vec3d.ZERO;

        Rotation atTarget = getRotationAt(targetPos);
        float yawDiff = MathHelper.wrapDegrees(atTarget.getYaw());
        float pitchDiff = MathHelper.clamp(atTarget.getPitch(), -90, 90);

        FeaturesData d = new FeaturesData(
                Math.abs(current.getYaw() - prev.getYaw()), Math.abs(current.getPitch() - prev.getPitch()),
                0, pitchDiff, timer.getElapsedTime(),
                (float) mc.player.getPos().distanceTo(targetPos),
                mc.player.isOnGround() ? 1 : 0,
                mc.player.isGliding() || mc.player.isSubmergedInWater() ? 1 : 0,
                MathHelper.wrapDegrees(current.getYaw()), current.getPitch(),
                (float) playerPos.x, (float) playerPos.y, (float) playerPos.z,
                (float) playerVel.x, (float) playerVel.y, (float) playerVel.z,
                (float) targetPos.x, (float) targetPos.y, (float) targetPos.z,
                (float) targetVel.x, (float) targetVel.y, (float) targetVel.z,
                //(float) (targetPos.x - target.prevX), (float) (targetPos.y - target.prevY), (float) (targetPos.z - target.prevZ)
                0f, 0f, 0f
        );

        float[] features = new float[]{
                d.yawDelta(), d.pitchDelta(), d.targetYaw(), d.targetPitch(),
                d.sinceAttack(), d.distance(), d.onGround(), d.miniHitbox(),
                d.playerYaw(), d.playerPitch(),
                d.playerPosX(), d.playerPosY(), d.playerPosZ(),
                d.playerVelX(), d.playerVelY(), d.playerVelZ(),
                d.targetPosX(), d.targetPosY(), d.targetPosZ(),
                d.targetVelX(), d.targetVelY(), d.targetVelZ(),
                d.deltaX(), d.deltaY(), d.deltaZ()
        };

        String[] crendil = new String[]{
                String.valueOf(d.onGround()),
                String.valueOf(d.miniHitbox())
        };
        CatBoostPredictions yawPred = yawModel.predict(features, crendil);
        CatBoostPredictions pitchPred = pitchModel.predict(features, crendil);

        float predictedYaw = (float) yawPred.get(0, 0);
        float predictedPitch = (float) pitchPred.get(0, 0);

        float shortestYawPath = ((((predictedYaw + yawDiff - interpolation.getYaw()) % 360) + 540) % 360) - 180;
        float targetYaw = interpolation.getYaw() + shortestYawPath;
//        float targetPitch = predictedPitch + pitchDiff - interpolation.getPitch();
        float targetPitch = predictedPitch - interpolation.getPitch();

        if (mc.player.getY() > target.getY() + target.getHeight()) {
            targetPitch = getRotationAt(target.getPos().add(0, target.getHeight(), 0)).getPitch();
        }
        if (mc.player.getY() + 1 < target.getY()) {
            targetPitch = getRotationAt(target.getPos().add(0, 0.5f, 0)).getPitch();
        }
        if (mc.player.isSwimming()) {
            targetPitch = getRotationAt(target.getPos().add(0, target.getHeight() / 2F, 0)).getPitch();
            speed = new Vector2f(MathUtil.randomInRange(200, 350), MathUtil.randomInRange(200, 350));
        }
        if (PlayerUtil.hasCollisionWith(target) && (PlayerUtil.getBlock(0, 2, 0) != Blocks.AIR && PlayerUtil.getBlock(0, -1, 0) != Blocks.AIR && PlayerUtil.getBlock(0, 2, 0) != Blocks.WATER && PlayerUtil.getBlock(0, -1, 0) != Blocks.WATER) || PlayerUtil.hasCollisionWith(target, -0.7f)) {
            speed = new Vector2f(speed.getX() * 1.6f, speed.getY() * 1.6f);
        }

        interpolation.easing(Easing.LINEAR).animate(new Rotation(targetYaw, targetPitch), (int) speed.getX(), (int) speed.getY());

        float finalYaw = speed.getX() == 0f ? targetYaw : interpolation.getYaw();
        float finalPitch = speed.getY() == 0f ? targetPitch : interpolation.getPitch();

        return new Rotation(
                finalYaw,
                finalPitch
        );
    }

    private Rotation getRotationAt(Vec3d pos) {
        return RotationUtil.rotationAt(pos);
    }
}