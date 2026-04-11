package sweetie.evaware.api.system.backend;

import sweetie.evaware.api.module.setting.ModeSetting;
import sweetie.evaware.client.features.modules.render.AmbienceModule;

import java.util.Arrays;

public abstract class Choice extends Configurable implements ModeSetting.NamedChoice {
    @Override abstract public String getName();

    public static String[] getValues(Choice... choices) {
        return Arrays.stream(choices)
                .map(Choice::getName)
                .toArray(String[]::new);
    }

    public static Choice getChoiceByName(String value, Choice... choices) {
        if (choices != null) {
            if (value == null) {
                if (choices.length == 0) return null;
                else return choices[0];
            }
        } else {
            return null;
        }

        for (Choice choice : choices) {
            if (choice != null && value.equals(choice.getName())) {
                return choice;
            }
        }
        return null;
    }

    @SafeVarargs
    public static <T extends ModeSetting.NamedChoice> T getChoiceByName(String value, T... choices) {
        if (choices == null || choices.length == 0) return null;

        T first = choices[0];

        return Arrays.stream(choices)
                .filter(wt -> wt.getName().equals(value))
                .findFirst()
                .orElse(first);
    }
}
