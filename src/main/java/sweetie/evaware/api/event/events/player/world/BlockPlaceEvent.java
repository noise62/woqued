package sweetie.evaware.api.event.events.player.world;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import sweetie.evaware.api.event.events.Event;

public class BlockPlaceEvent extends Event<BlockPlaceEvent.BlockPlaceEventData> {
    @Getter private static final BlockPlaceEvent instance = new BlockPlaceEvent();

    public record BlockPlaceEventData(Block block, BlockState state, BlockPos pos, LivingEntity placer) {}
}
