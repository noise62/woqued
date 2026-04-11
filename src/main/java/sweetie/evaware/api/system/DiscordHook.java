package sweetie.evaware.api.system;

import eu.donyka.discord.RPCHandler;
import eu.donyka.discord.discord.RichPresence;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import sweetie.evaware.api.system.backend.ClientInfo;
import sweetie.evaware.api.system.interfaces.QuickImports;

@UtilityClass
public class DiscordHook implements QuickImports {
    @SneakyThrows
    public void startRPC() {
        RPCHandler.setOnReady(user -> {
            RichPresence presence = RichPresence.builder()
                    .details("Version: " + ClientInfo.VERSION)
                    .largeImageKey("ava")
                    .largeImageText(user.getUsername())
                    .build();

            RPCHandler.updatePresence(presence);
        });

        RPCHandler.setOnDisconnected(error -> {
            System.out.println("RPC Disconnected: " + error);
        });

        RPCHandler.setOnErrored(error -> {
            System.out.println("RPC Errored: " + error);
        });

        RPCHandler.startup("1378057680316268685", false);
    }

    public void stopRPC() {
        RPCHandler.shutdown();
    }
}
