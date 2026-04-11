package sweetie.evaware.client.features.modules.player;

import lombok.Getter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import sweetie.evaware.api.event.EventListener;
import sweetie.evaware.api.event.Listener;
import sweetie.evaware.api.event.events.client.TickEvent;
import sweetie.evaware.api.module.Category;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleRegister;

import static java.lang.Math.abs;

@ModuleRegister(name = "Hitbox Desync", category = Category.PLAYER)
public class HitboxDesyncModule extends Module {
    @Getter private static final HitboxDesyncModule instance = new HitboxDesyncModule();

    private static final double DONYKA_SEX = .200009968835369999878673424677777777777761;

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            Direction f = mc.player.getHorizontalFacing();
            Box bb = mc.player.getBoundingBox();
            Vec3d center = bb.getCenter();
            Vec3d offset = new Vec3d(f.getUnitVector());

            Vec3d fin = merge(Vec3d.of(BlockPos.ofFloored(center)).add(.5, 0, .5).add(offset.multiply(DONYKA_SEX)), f);
            mc.player.setPosition(fin.x == 0 ? mc.player.getX() : fin.x,
                    mc.player.getY(),
                    fin.z == 0 ? mc.player.getZ() : fin.z);
            toggle();
        }));

        addEvents(tickEvent);
    }

    private Vec3d merge(Vec3d a, Direction facing) {
        return new Vec3d(a.x * abs(facing.getUnitVector().x()), a.y * abs(facing.getUnitVector().y()), a.z * abs(facing.getUnitVector().z()));
    }
}
