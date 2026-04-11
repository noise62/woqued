package sweetie.evaware.client.features.modules.movement.spider.modes;

import net.minecraft.block.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import sweetie.evaware.api.event.events.player.move.MotionEvent;
import sweetie.evaware.api.utils.math.TimerUtil;
import sweetie.evaware.client.features.modules.movement.spider.SpiderMode;

public class SpiderFunTime extends SpiderMode {
    @Override
    public String getName() {
        return "Fun Time";
    }

    private final TimerUtil timerUtil = new TimerUtil();

    @Override
    public void onMotion(MotionEvent.MotionEventData event) {
        Direction facing = mc.player.getHorizontalFacing();

        BlockPos posInFront = BlockPos.ofFloored(mc.player.getPos()).offset(facing);
        BlockState stateInFront = mc.world.getBlockState(posInFront);
        Block blockInFront = stateInFront.getBlock();

        BlockPos posBelow = BlockPos.ofFloored(mc.player.getPos());
        BlockState stateBelow = mc.world.getBlockState(posBelow);
        Block blockBelow = stateBelow.getBlock();

        boolean penisF = blockInFront instanceof TrapdoorBlock
                && Boolean.TRUE.equals(stateInFront.get(Properties.OPEN))
                && stateInFront.contains(Properties.HORIZONTAL_FACING);

        boolean penisB = blockBelow instanceof TrapdoorBlock
                && Boolean.TRUE.equals(stateBelow.get(Properties.OPEN))
                && stateBelow.contains(Properties.HORIZONTAL_FACING);

        boolean xuiF = blockInFront instanceof FenceBlock
                || stateInFront.isIn(BlockTags.WALLS)
                || blockInFront instanceof FenceGateBlock
                || blockInFront instanceof LanternBlock
                || penisF;

        boolean glubgseB = blockBelow instanceof FenceBlock
                || stateBelow.isIn(BlockTags.WALLS)
                || blockBelow instanceof FenceGateBlock
                || blockBelow instanceof LanternBlock
                || penisB;

        // 230
        if (timerUtil.finished(240) && hozColl() && (xuiF || glubgseB)) {
            event.ground(true);
            mc.player.setOnGround(true);
            mc.player.jump();
            mc.player.fallDistance = 0;
            timerUtil.reset();
        }
    }
}
