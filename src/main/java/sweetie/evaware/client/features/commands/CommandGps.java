package sweetie.evaware.client.features.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import org.joml.Vector2i;
import sweetie.evaware.api.command.Command;
import sweetie.evaware.api.command.CommandRegister;
import sweetie.evaware.api.system.client.GpsManager;

@CommandRegister(name = "gps")
public class CommandGps extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("off").executes(context -> {
            if (GpsManager.getInstance().getGpsPosition() == null) {
                print("А чё ты выключить собираешься?");
            } else {
                GpsManager.getInstance().setGpsPosition(null);
                print("Маршрут успешно удален.");
            }

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("add").then(argument("x", IntegerArgumentType.integer()).then(argument("z", IntegerArgumentType.integer()).executes(context -> {
            int x = IntegerArgumentType.getInteger(context, "x");
            int z = IntegerArgumentType.getInteger(context, "z");

            if (x == 0 && z == 0) {
                print("Ты не можешь указать маршрут на нулевые координаты.");
            } else {
                Vector2i newPos = new Vector2i(x, z);

                if (GpsManager.getInstance().getGpsPosition() != null && GpsManager.getInstance().getGpsPosition().equals(newPos)) {
                    print("Такая точка уже есть в маршруте.");
                } else {
                    GpsManager.getInstance().setGpsPosition(newPos);
                    print("Установлен маршрут: " + newPos.x + " " + newPos.y);
                }
            }

            return SINGLE_SUCCESS;
        }))));
    }
}