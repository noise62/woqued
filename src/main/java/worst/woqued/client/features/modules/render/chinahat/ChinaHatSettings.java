package worst.woqued.client.features.modules.render.chinahat;

import worst.woqued.api.module.setting.BooleanSetting;
import worst.woqued.api.module.setting.ModeSetting;
import worst.woqued.api.module.setting.Setting;

import java.util.List;

public final class ChinaHatSettings {
    public enum MaterialMode implements ModeSetting.NamedChoice {
        BAMBOO("Bamboo", "hat01"),
        CARPET("Carpet", "hat02"),
        METAL("Metal", "hat03"),
        TURBINE("Turbine", "hat04"),
        ILLUSION("Illusion", "hat05"),
        MAGMA("Magma", "hat06"),
        DIRTY("Dirty", "hat07"),
        FOIL("Foil", "hat08");

        private final String name;
        private final String textureName;

        MaterialMode(String name, String textureName) {
            this.name = name;
            this.textureName = textureName;
        }

        @Override
        public String getName() {
            return name;
        }

        public String textureName() {
            return textureName;
        }

        public static MaterialMode byName(String name) {
            for (MaterialMode mode : values()) {
                if (mode.name.equalsIgnoreCase(name)) {
                    return mode;
                }
            }
            return BAMBOO;
        }
    }

    private final ModeSetting material = new ModeSetting("Material")
            .values(MaterialMode.values())
            .value(MaterialMode.BAMBOO);
    private final BooleanSetting highPolygonal = new BooleanSetting("HighPolygonal").value(false);
    private final BooleanSetting antialiasing = new BooleanSetting("Antialiasing").value(true);
    private final BooleanSetting onPlayers = new BooleanSetting("OnPlayers").value(true);
    private final BooleanSetting onFriends = new BooleanSetting("OnFriends").value(true);
    private final BooleanSetting onSelf = new BooleanSetting("OnSelf").value(true);
    private final BooleanSetting showOnFirstPerson = new BooleanSetting("ShowOnFirstPerson")
            .value(false)
            .setVisible(onSelf::getValue);

    public List<Setting<?>> asSettings() {
        return List.of(material, highPolygonal, antialiasing, onPlayers, onFriends, onSelf, showOnFirstPerson);
    }

    public MaterialMode materialMode() {
        return MaterialMode.byName(material.getValue());
    }

    public boolean highPolygonal() {
        return highPolygonal.getValue();
    }

    public boolean antialiasing() {
        return antialiasing.getValue();
    }

    public boolean onPlayers() {
        return onPlayers.getValue();
    }

    public boolean onFriends() {
        return onFriends.getValue();
    }

    public boolean onSelf() {
        return onSelf.getValue();
    }

    public boolean showOnFirstPerson() {
        return showOnFirstPerson.getValue();
    }

    public boolean hasAnyEnabledTarget() {
        return onPlayers() || onFriends() || onSelf();
    }
}
