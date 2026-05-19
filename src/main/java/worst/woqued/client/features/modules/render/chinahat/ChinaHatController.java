package worst.woqued.client.features.modules.render.chinahat;

import worst.woqued.api.system.interfaces.QuickImports;
import worst.woqued.client.features.modules.render.ChinaHatModule;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class ChinaHatController implements QuickImports {
    private final ChinaHatModule module;
    private final ChinaHatSettings settings;
    private final ChinaHatTargetFilter targetFilter;
    private final List<PlayerEntity> targets = new ArrayList<>();

    private boolean warnedMissingTargets;

    public ChinaHatController(ChinaHatModule module, ChinaHatSettings settings) {
        this.module = module;
        this.settings = settings;
        this.targetFilter = new ChinaHatTargetFilter(
                mc,
                settings,
                new DefaultFriendService(),
                new DefaultCameraAdapter(),
                new DefaultMovementStateAdapter()
        );
    }

    public void updateTargets() {
        if (!settings.hasAnyEnabledTarget()) {
            targets.clear();
            if (!warnedMissingTargets) {
                warnedMissingTargets = true;
                module.setEnabled(false);
            }
            return;
        }

        warnedMissingTargets = false;
        targets.clear();
        targets.addAll(targetFilter.collectTargets());
    }

    public List<PlayerEntity> getTargets() {
        return targets;
    }

    public void clear() {
        targets.clear();
        warnedMissingTargets = false;
    }
}
