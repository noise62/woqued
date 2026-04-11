package sweetie.evaware.api.utils.combat;

import sweetie.evaware.api.system.interfaces.QuickImports;

public class ClickScheduler implements QuickImports {
    private long delay = 0;
    private long lastClickTime = System.currentTimeMillis();

    public boolean isCooldownComplete() {
        float currentCooldown = mc.player != null ? mc.player.getAttackCooldownProgress(0.5f) : 1.0f;
        long currentTime = System.currentTimeMillis();
        long timeSinceLastClick = currentTime - lastClickTime;

        boolean wasComplete = timeSinceLastClick >= delay && currentCooldown > 0.9f;
        return wasComplete;
    }

    public boolean hasTicksElapsedSinceLastClick(int ticks) {
        return lastClickPassed() >= (ticks * 50L);
    }

    public long lastClickPassed() {
        return System.currentTimeMillis() - lastClickTime;
    }

    public void recalculate(long delay) {
        lastClickTime = System.currentTimeMillis();
        this.delay = delay;
    }

    public boolean isOneTickBeforeAttack() {
        return willClickAt(3);
    }

    public boolean willClickAt(int tick) {
        long timeSinceLastClick = lastClickPassed();
        long time = tick * 50L;
        return timeSinceLastClick >= (delay - time) && timeSinceLastClick < delay + time;
    }

    public long toNext() {
        return delay - lastClickPassed();
    }

    public float cooldownProgress(int baseTime) {
        return (mc.player.lastAttackedTicks + baseTime) / mc.player.getAttackCooldownProgressPerTick();
    }
}
