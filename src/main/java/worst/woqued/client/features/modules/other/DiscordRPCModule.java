package worst.woqued.client.features.modules.other;

import eu.donyka.discord.RPCHandler;
import eu.donyka.discord.discord.RichPresence;
import lombok.Getter;
import worst.woqued.api.module.Category;
import worst.woqued.api.module.Module;
import worst.woqued.api.module.ModuleRegister;
import worst.woqued.api.system.backend.ClientInfo;

@ModuleRegister(name = "Discord RPC", category = Category.OTHER)
public class DiscordRPCModule extends Module {
    @Getter private static final DiscordRPCModule instance = new DiscordRPCModule();

    private boolean rpcStarted = false;

    public DiscordRPCModule() {
    }

    @Override
    public void onEnable() {
        if (rpcStarted) return;
        startRPC();
        rpcStarted = true;
    }

    @Override
    public void onDisable() {
        stopRPC();
        rpcStarted = false;
    }

    private void startRPC() {
        RPCHandler.setOnReady(user -> updatePresence());

        RPCHandler.setOnDisconnected(error -> {
            System.out.println("RPC Disconnected: " + error);
        });

        RPCHandler.setOnErrored(error -> {
            System.out.println("RPC Errored: " + error);
        });

        RPCHandler.startup("1498001191349518538", false);
    }

    private void stopRPC() {
        RPCHandler.shutdown();
    }

    public void updatePresence() {
        String serverText = getServerText();

        RichPresence presence = RichPresence.builder()
                .details("Version: " + ClientInfo.VERSION)
                .state("github.com/noise62/woqued")
                .largeImageKey("ava")
                .largeImageText("Woqued")
                .build();

        RPCHandler.updatePresence(presence);
    }

    public String getServerText() {
        if (mc.getCurrentServerEntry() != null) {
            return mc.getCurrentServerEntry().address;
        }
        return "singleplayer";
    }

    @Override
    public void onEvent() {
        if (rpcStarted) {
            updatePresence();
        }
    }
}