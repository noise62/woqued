package worst.woqued.client.features.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import worst.woqued.api.command.Command;
import worst.woqued.api.command.CommandRegister;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@CommandRegister(name = "fakeplayer")
public class CommandFakePlayer extends Command {
    private static AbstractClientPlayerEntity fakePlayer = null;

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add").executes(context -> {
            if (fakePlayer != null && !fakePlayer.isRemoved()) {
                print("Фейк плеер уже существует. Используй .fakeplayer off чтобы убрать.");
            } else {
                spawn();
                print("Фейк плеер добавлен.");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("off").executes(context -> {
            if (fakePlayer == null || fakePlayer.isRemoved()) {
                print("Фейк плеер не активен.");
            } else {
                remove();
                print("Фейк плеер убран.");
            }
            return SINGLE_SUCCESS;
        }));
    }

    private void spawn() {
        if (mc.player == null || mc.world == null) return;

        remove();

        UUID uuid = UUID.randomUUID();
        GameProfile profile = new GameProfile(uuid, "FakePlayer");
        fakePlayer = new AbstractClientPlayerEntity(mc.world, profile) {};
        fakePlayer.copyPositionAndRotation(mc.player);
        mc.world.addEntity(fakePlayer);
    }

    private void remove() {
        if (fakePlayer != null && !fakePlayer.isRemoved()) {
            fakePlayer.discard();
        }
        fakePlayer = null;
    }
}