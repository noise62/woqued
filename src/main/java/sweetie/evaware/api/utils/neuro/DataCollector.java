package sweetie.evaware.api.utils.neuro;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import sweetie.evaware.api.event.events.client.PacketEvent;
import sweetie.evaware.api.event.events.player.world.AttackEvent;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.api.utils.rotation.RotationUtil;
import sweetie.evaware.api.utils.rotation.manager.Rotation;
import sweetie.evaware.api.utils.rotation.manager.RotationManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class DataCollector implements QuickImports {
    @Getter private static final DataCollector instance = new DataCollector();

    @Getter private int dataSize = 0;

    @Getter private final List<FeaturesData> dataBuffer = new ObjectArrayList<>(20);
    private LivingEntity target;
    private final TimerUtil timer = new TimerUtil();
    private final TimerUtil attackTimer = new TimerUtil();

    public void startCollecting() {
        dataBuffer.clear();
        dataSize = 0;
        print("Начата сборка датасета");
    }

    public void stopCollecting() {
        dataSize = 0;
        saveDataset();
        dataBuffer.clear();
        print("Прекращена сборка датасета");
    }

    public void onAttack(AttackEvent.AttackEventData event){
        if (event.entity() instanceof LivingEntity livingEntity) {
            target = livingEntity;
            timer.reset();
            attackTimer.reset();
        }
    }


    public void onPacket(PacketEvent.PacketEventData event) {
        if (event.isReceive() && event.packet() instanceof PlaySoundS2CPacket play && (play.getSound().value() == SoundEvents.ENTITY_PLAYER_ATTACK_CRIT || play.getSound().value() == SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP)) {
            new Thread(() -> {
                if (!attackTimer.finished(200)) {
                    saveDataset();
                }

                dataSize += dataBuffer.size();
                dataBuffer.clear();
            }).start();
        }
    }

    public void onUpdate() {
        parse(mc.player);
    }

    public void saveDataset() {
        if (dataBuffer.isEmpty()) return;

        File file = new File("rotation_data.csv");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("yaw_delta,pitch_delta,target_yaw,target_pitch,since_attack,distance,on_ground,mini_hitbox," +
                    "player_yaw,player_pitch," +
                    "player_pos_x,player_pos_y,player_pos_z," +
                    "player_vel_x,player_vel_y,player_vel_z," +
                    "target_pos_x,target_pos_y,target_pos_z," +
                    "target_vel_x,target_vel_y,target_vel_z," +
                    "delta_x,delta_y,delta_z\n");

            for (FeaturesData d : dataBuffer) {
                writer.write(String.format(Locale.US,
                        "%f,%f,%f,%f,%f,%f,%d,%d," +
                                "%f,%f,%f,%f,%f,%f,%f," +
                                "%f,%f,%f,%f,%f,%f," +
                                "%f,%f,%f\n",
                        d.yawDelta(), d.pitchDelta(), d.targetYaw(), d.targetPitch(),
                        d.sinceAttack(), d.distance(), d.onGround(), d.miniHitbox(),
                        d.playerYaw(), d.playerPitch(),
                        d.playerPosX(), d.playerPosY(), d.playerPosZ(),
                        d.playerVelX(), d.playerVelY(), d.playerVelZ(),
                        d.targetPosX(), d.targetPosY(), d.targetPosZ(),
                        d.targetVelX(), d.targetVelY(), d.targetVelZ(),
                        d.deltaX(), d.deltaY(), d.deltaZ()
                ));
            }

            dataSize += dataBuffer.size();
            //print("Сохранено " + dataBuffer.size() + " сэмплов");
            dataBuffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void parse(LivingEntity player) {
        if (timer.finished(2000) || player.isDead() || !mc.world.getPlayers().contains(player)) return;
        if (target == null || target.isDead()) return;

        Rotation atTarget = RotationUtil.rotationAt(target.getPos());

        float currentYaw = player.getYaw();
        float currentPitch = player.getPitch();
        float prevYaw = player.prevYaw;
        float prevPitch = player.prevPitch;

        RotationManager rotationManager = RotationManager.getInstance();
        if (rotationManager.getCurrentRotationPlan() != null) {
            Rotation current = rotationManager.getRotation();
            Rotation prev = rotationManager.getPreviousRotation();

            currentYaw = current.getYaw();
            currentPitch = current.getPitch();

            prevYaw = prev.getYaw();
            prevPitch = prev.getPitch();
        }

        float yawDiff = MathHelper.wrapDegrees(atTarget.getYaw());
        float pitchDiff = MathHelper.wrapDegrees(atTarget.getPitch());

        // Скорости
        var pv = player.getVelocity();
        var tv = target.getVelocity();

        // Позиции
        var pp = player.getPos();
        var tp = target.getPos();
        float deltaX = (float) (target.getX() - target.prevX);
        float deltaY = (float) (target.getY() - target.prevY);
        float deltaZ = (float) (target.getZ() - target.prevZ);

        dataBuffer.add(new FeaturesData(
                Math.abs(currentYaw - prevYaw),
                Math.abs(currentPitch - prevPitch),
                yawDiff,
                pitchDiff,
                timer.getElapsedTime(),
                (float) pp.distanceTo(tp),
                player.isOnGround() ? 1 : 0,
                player.isGliding() || player.isSwimming() ? 1 : 0,

                currentYaw, currentPitch,
                (float) pp.x, (float) pp.y, (float) pp.z,
                (float) pv.x, (float) pv.y, (float) pv.z,

                (float) tp.x, (float) tp.y, (float) tp.z,
                (float) tv.x, (float) tv.y, (float) tv.z,

                deltaX, deltaY, deltaZ
        ));
    }

    public float normalizeTo360(float angle) {
        angle %= 360;
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    public float normalizeTo180(float angle) {
        angle = normalizeTo360(angle);
        if (angle > 180) {
            angle -= 360;
        }
        return angle;
    }

    public float getAngleDifference(float angle1, float angle2) {
        float normalizedAngle1 = normalizeTo360(angle1);
        float normalizedAngle2 = normalizeTo360(angle2);

        float difference = normalizedAngle1 - normalizedAngle2;

        return normalizeTo180(difference);
    }
}