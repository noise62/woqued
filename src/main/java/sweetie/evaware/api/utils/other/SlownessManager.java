package sweetie.evaware.api.utils.other;

import lombok.experimental.UtilityClass;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import sweetie.evaware.api.system.interfaces.QuickImports;
import sweetie.evaware.api.utils.player.MoveUtil;
import sweetie.evaware.client.features.modules.movement.InventoryMoveModule;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class SlownessManager implements QuickImports {
    private final List<Entry> entries = new ArrayList<>(4);

    public boolean slowed = false;

    public boolean isEnabled() {
        return InventoryMoveModule.getInstance().isLegit();
    }

    public void applySlowness(long totalMs, long delayMs, Runnable action) {
        long now = System.nanoTime();
        long execNs = now + delayMs * 1_000_000L;
        long endNs  = now + totalMs * 1_000_000L;

        entries.add(new Entry(execNs, endNs, action));

        if (!slowed) {
            for (KeyBinding key : MoveUtil.getMovementKeys()) {
                key.setPressed(false);
            }
            slowed = true;
        }
    }

    public void applySlowness(long totalMs, Runnable action) {
        applySlowness(totalMs, 1L, action);
    }

    public void tick() {
        if (!slowed && entries.isEmpty()) return;

        long now = System.nanoTime();

        List<Entry> toExecute = null;
        for (Entry e : entries) {
            if (!e.executed && now >= e.execNs) {
                if (toExecute == null) toExecute = new ArrayList<>(4);
                toExecute.add(e);
            }
        }

        if (toExecute != null) {
            for (Entry e : toExecute) {
                try {
                    if (e.action != null) e.action.run();
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    e.executed = true;
                }
            }
        }

        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry e = entries.get(i);
            if (e.executed && now >= e.endNs) {
                entries.remove(i);
            }
        }

        if (entries.isEmpty() && slowed) {
            resetKeys();
        }
    }

    private void resetKeys() {
        if (mc.player == null || mc.world == null) {
            entries.clear();
            slowed = false;
            return;
        }

        long handle = mc.getWindow().getHandle();
        for (KeyBinding key : MoveUtil.getMovementKeys()) {
            key.setPressed(InputUtil.isKeyPressed(handle, key.getDefaultKey().getCode()));
        }

        slowed = false;
    }

    private static final class Entry {
        final long execNs;
        final long endNs;
        final Runnable action;
        boolean executed = false;

        Entry(long execNs, long endNs, Runnable action) {
            this.execNs = execNs;
            this.endNs = endNs;
            this.action = action;
        }
    }
}
