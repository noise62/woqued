package sweetie.evaware.api.utils.neuro;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class FeaturesData {
    // Состояния (булевы признаки)
    private final int onGround, miniHitbox;

    // Ротации
    private final float yawDelta, pitchDelta;
    private final float targetYaw, targetPitch;
    private final float sinceAttack, distance;

    // Игрок
    private final float playerYaw, playerPitch;
    private final float playerPosX, playerPosY, playerPosZ;
    private final float playerVelX, playerVelY, playerVelZ;

    // Цель
    private final float targetPosX, targetPosY, targetPosZ;
    private final float targetVelX, targetVelY, targetVelZ;

    private final float deltaX, deltaY, deltaZ;

    public FeaturesData(
            float yawDelta,
            float pitchDelta,
            float targetYaw,
            float targetPitch,
            float sinceAttack,
            float distance,
            int onGround,
            int miniHitbox,

            float playerYaw,
            float playerPitch,
            float playerPosX,
            float playerPosY,
            float playerPosZ,
            float playerVelX,
            float playerVelY,
            float playerVelZ,

            float targetPosX,
            float targetPosY,
            float targetPosZ,
            float targetVelX,
            float targetVelY,
            float targetVelZ,
            float deltaX,
            float deltaY,
            float deltaZ
    ) {
        this.yawDelta = yawDelta;
        this.pitchDelta = pitchDelta;
        this.targetYaw = targetYaw;
        this.targetPitch = targetPitch;
        this.sinceAttack = sinceAttack;
        this.distance = distance;
        this.onGround = onGround;
        this.miniHitbox = miniHitbox;

        this.playerYaw = playerYaw;
        this.playerPitch = playerPitch;
        this.playerPosX = playerPosX;
        this.playerPosY = playerPosY;
        this.playerPosZ = playerPosZ;
        this.playerVelX = playerVelX;
        this.playerVelY = playerVelY;
        this.playerVelZ = playerVelZ;

        this.targetPosX = targetPosX;
        this.targetPosY = targetPosY;
        this.targetPosZ = targetPosZ;
        this.targetVelX = targetVelX;
        this.targetVelY = targetVelY;
        this.targetVelZ = targetVelZ;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.deltaZ = deltaZ;
    }
}