package worst.woqued.client.features.modules.render.chinahat;

import worst.woqued.api.system.configs.FriendManager;
import worst.woqued.api.system.files.FileUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

interface FriendService {
    boolean isFriend(PlayerEntity player);
}

final class DefaultFriendService implements FriendService {
    @Override
    public boolean isFriend(PlayerEntity player) {
        return player != null && FriendManager.getInstance().contains(player.getName().getString());
    }
}

interface CameraAdapter {
    Vec3d getCameraPos(MinecraftClient client);

    boolean isFirstPerson(MinecraftClient client);
}

final class DefaultCameraAdapter implements CameraAdapter {
    @Override
    public Vec3d getCameraPos(MinecraftClient client) {
        return client.gameRenderer != null && client.gameRenderer.getCamera() != null
                ? client.gameRenderer.getCamera().getPos()
                : Vec3d.ZERO;
    }

    @Override
    public boolean isFirstPerson(MinecraftClient client) {
        return client.options.getPerspective().isFirstPerson();
    }
}

interface MovementStateAdapter {
    boolean shouldBlockHat(PlayerEntity player);
}

final class DefaultMovementStateAdapter implements MovementStateAdapter {
    @Override
    public boolean shouldBlockHat(PlayerEntity player) {
        if (player == null) {
            return true;
        }

        return player.isSleeping()
                || player.isGliding()
                || player.isUsingRiptide()
                || player.getPose() == EntityPose.SWIMMING;
    }
}

final class ChinaHatMaterialResolver {
    public Identifier resolve(ChinaHatSettings.MaterialMode mode) {
        return FileUtil.getImage("modules/chinahat/" + mode.textureName());
    }
}
