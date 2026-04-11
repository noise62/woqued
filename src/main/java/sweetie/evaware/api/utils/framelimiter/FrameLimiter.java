package sweetie.evaware.api.utils.framelimiter;

import net.minecraft.client.MinecraftClient;

public class FrameLimiter {
    private long lastHookTime;
    private int accumulatedCalls;
    private final boolean useMCFrameRate;
    private int currentFps = 0;
    private long hookIntervalNS = 0;

    public FrameLimiter(boolean useMCFrameRate) {
        this.lastHookTime = System.nanoTime();
        this.useMCFrameRate = useMCFrameRate;
        this.accumulatedCalls = 0;
    }

    public void execute(int fps, IFrameCall... calls) {
        if (currentFps != fps) {
            hookIntervalNS = 1_000_000_000L / fps;
            currentFps = fps;
        }

        long nanoTime = System.nanoTime();
        long elapsed = nanoTime - lastHookTime;

        accumulatedCalls += (int) (elapsed / hookIntervalNS);
        lastHookTime += (accumulatedCalls * hookIntervalNS);

        accumulatedCalls = Math.min(accumulatedCalls, useMCFrameRate ? Math.min(currentFps, MinecraftClient.getInstance().getCurrentFps()) : currentFps);

        while (accumulatedCalls > 0) {
            for (IFrameCall call : calls) {
                call.execute();
            }
            accumulatedCalls--;
        }
    }

}
