package worst.woqued.client.features.modules.render.chinahat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class ChinaHatTargetFilter {
    private static final double MAX_RENDER_DISTANCE_SQ = 64d * 64d;

    private final MinecraftClient client;
    private final ChinaHatSettings settings;
    private final FriendService friendService;
    private final CameraAdapter cameraAdapter;
    private final MovementStateAdapter movementStateAdapter;

    public ChinaHatTargetFilter(MinecraftClient client,
                                ChinaHatSettings settings,
                                FriendService friendService,
                                CameraAdapter cameraAdapter,
                                MovementStateAdapter movementStateAdapter) {
        this.client = client;
        this.settings = settings;
        this.friendService = friendService;
        this.cameraAdapter = cameraAdapter;
        this.movementStateAdapter = movementStateAdapter;
    }

    public List<PlayerEntity> collectTargets() {
        List<PlayerEntity> targets = new ArrayList<>();
        if (client.world == null || client.player == null) {
            return targets;
        }

        Vec3d cameraPos = cameraAdapter.getCameraPos(client);
        boolean firstPerson = cameraAdapter.isFirstPerson(client);
        float tickDelta = client.getRenderTickCounter().getTickDelta(false);

        for (PlayerEntity player : client.world.getPlayers()) {
            if (!isBaseRenderable(player)) {
                continue;
            }
            if (movementStateAdapter.shouldBlockHat(player)) {
                continue;
            }
            if (player == client.player && firstPerson && !settings.showOnFirstPerson()) {
                continue;
            }
            if (!matchesTargetSettings(player)) {
                continue;
            }

            Vec3d playerPos = interpolatedPos(player, tickDelta);
            if (cameraPos.squaredDistanceTo(playerPos) > MAX_RENDER_DISTANCE_SQ) {
                continue;
            }

            targets.add(player);
        }

        return targets;
    }

    private boolean isBaseRenderable(PlayerEntity player) {
        if (player == null || player.isRemoved() || !player.isAlive()) {
            return false;
        }
        if (player.isInvisible() || client.player != null && player.isInvisibleTo(client.player)) {
            return false;
        }
        return !player.isGliding();
    }

    private boolean matchesTargetSettings(PlayerEntity player) {
        if (player == client.player) {
            return settings.onSelf();
        }
        if (friendService.isFriend(player)) {
            return settings.onFriends();
        }
        return settings.onPlayers();
    }

    private Vec3d interpolatedPos(PlayerEntity player, float tickDelta) {
        return new Vec3d(
                MathHelper.lerp(tickDelta, player.prevX, player.getX()),
                MathHelper.lerp(tickDelta, player.prevY, player.getY()),
                MathHelper.lerp(tickDelta, player.prevZ, player.getZ())
        );
    }
}
