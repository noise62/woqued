package sweetie.evaware.api.utils.player;

import lombok.experimental.UtilityClass;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import sweetie.evaware.api.system.interfaces.QuickImports;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@UtilityClass
public class PlayerUtil implements QuickImports {
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");

    public boolean isEating() {
        return mc.player.isUsingItem() && mc.player.getActiveItem().getComponents().contains(DataComponentTypes.FOOD);
    }

    public boolean canSee(Vec3d to) {
        HitResult hitResult = mc.world.raycast(new RaycastContext(mc.getCameraEntity().getEyePos(), to, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.getCameraEntity()));
        return hitResult == null || hitResult.getType() == HitResult.Type.MISS;
    }

    public boolean isAboveWater() {
        if (mc.player == null) return false;
        if (mc.world == null) return false;

        return mc.player.isSubmergedInWater() || mc.world.getBlockState(mc.player.getBlockPos().add(0, (int) (-0.5), 0)).getBlock() == Blocks.WATER;
    }

    public boolean isInWeb() {
        if (mc.player == null) return false;
        Box playerBox = mc.player.getBoundingBox();
        BlockPos playerPosition = mc.player.getBlockPos();

        return getNearbyBlockPositions(playerPosition).stream().anyMatch(pos -> isBlockCobweb(playerBox, pos));
    }

    private boolean isBlockCobweb(Box playerBox, BlockPos blockPos) {
        return playerBox.intersects(new Box(blockPos)) && mc.world != null && mc.world.getBlockState(blockPos).getBlock() == Blocks.COBWEB;
    }

    public List<BlockPos> getNearbyBlockPositions(BlockPos center) {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = (center.getX() - 2); x <= (center.getX() + 2); x++) {
            for (int y = (center.getY() - 1); y <= (center.getY() + 4); y++) {
                for (int z = (center.getZ() - 2); z <= (center.getZ() + 2); z++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        return positions;
    }

    public Block getBlock(float x, float y, float z) {
        Vec3d pos = mc.player.getPos();
        return mc.world.getBlockState(new BlockPos(new Vec3i((int) (pos.x + x), (int) (pos.y + y), (int) (pos.z + z)))).getBlock();
    }

    public boolean hasCollisionWith(Entity entity) {
        return hasCollisionWith(entity, 0f);
    }

    public boolean hasCollisionWith(Entity entity, float expand) {
        Box box = mc.player.getBoundingBox();
        Box targetbox = entity.getBoundingBox().expand(expand, 0, expand);

        return box.maxX > targetbox.minX
                && box.maxY > targetbox.minY
                && box.maxZ > targetbox.minZ
                && box.minX < targetbox.maxX
                && box.minY < targetbox.maxY
                && box.minZ < targetbox.maxZ;
    }

    public boolean isValidName(String name) {
        return namePattern.matcher(name).matches();
    }
}
