package sweetie.evaware.api.system.configs;

import lombok.Getter;
import sweetie.evaware.api.system.backend.ClientInfo;
import sweetie.evaware.client.ui.theme.Theme;
import sweetie.evaware.client.ui.theme.ThemeEditor;
import sweetie.evaware.client.ui.theme.ThemeSelectable;
import sweetie.evaware.client.ui.theme.basic.*;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Getter
public class ThemeManager {
    @Getter private static final ThemeManager instance = new ThemeManager();

    private final String path;
    private final File dir;
    private final File lastFile;
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    public ThemeManager(String path) {
        this.path = path;
        this.dir = new File(path);
        this.lastFile = new File(dir, "last_selected");
        ensureDir();
    }

    public ThemeManager() {
        this(ClientInfo.CONFIG_PATH_THEMES);
    }

    public void save(Theme theme) {
        if (theme == null) return;
        ensureDir();
        Path out = dir.toPath().resolve(safeFileName(theme.getName()) + ".theme");
        List<String> lines = new ArrayList<>();
        lines.add("name=" + theme.getName());
        for (Theme.ElementColor ec : theme.getElementColors()) {
            lines.add(ec.getName() + "=" + ec.getColor().getRGB());
        }
        try {
            Files.write(out, lines, UTF8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAll() {
        for (ThemeSelectable ts : ThemeEditor.getInstance().getThemeSelectables()) {
            if (ts != null && ts.getTheme() != null) save(ts.getTheme());
        }
    }

    public void saveLastSelected(Theme theme) {
        if (theme == null) return;
        ensureDir();
        try {
            Files.writeString(lastFile.toPath(), safeFileName(theme.getName()), UTF8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean remove(String themeName) {
        if (themeName == null || themeName.isEmpty()) return false;
        ensureDir();
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".theme"));
        if (files == null) return false;

        for (File file : files) {
            try {
                String first = Files.lines(file.toPath(), UTF8).findFirst().orElse("");
                if (first.startsWith("name=") &&
                        themeName.equals(first.substring(5).trim())) {
                    return file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public Theme load(String name) {
        if (name == null || name.isEmpty()) return null;
        File f = new File(dir, safeFileName(name) + ".theme");
        return f.exists() ? load(f) : null;
    }

    public Theme load(File file) {
        if (file == null || !file.exists()) return null;
        Map<String, String> map = readKeyValueFile(file.toPath());
        String name = map.getOrDefault("name", stripExtension(file.getName()));
        Theme theme = new Theme(name);

        for (Theme.ElementColor ec : theme.getElementColors()) {
            String key = ec.getName();
            if (map.containsKey(key)) {
                try {
                    int rgb = Integer.parseInt(map.get(key));
                    ec.setColor(new Color(rgb, true));
                } catch (NumberFormatException ignored) { }
            }
        }
        return theme;
    }

    public void refresh() {
        List<ThemeSelectable> selectables = ThemeEditor.getInstance().getThemeSelectables();
        selectables.clear();
        ensureDir();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".theme"));
        if (files == null || files.length == 0) {
            loadDefaultThemes();
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File f : files) {
            Theme loaded = load(f);
            if (loaded != null) selectables.add(new ThemeSelectable(loaded));
        }
    }

    public Theme loadLastSelected() {
        if (!lastFile.exists()) return null;
        try {
            String safeName = Files.readString(lastFile.toPath(), UTF8).trim();
            if (safeName.isEmpty()) return null;
            File f = new File(dir, safeName + ".theme");
            return f.exists() ? load(f) : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void ensureDir() {
        if (!dir.exists()) {
            dir.mkdirs();

            loadDefaultThemes();
        }
    }

    private void loadDefaultThemes() {
        Theme[] themes = new Theme[]{
                new Theme("EvaWare"),
                new BlueTheme().update(), new CandyLoveTheme().update(), new CrimsonTheme().update()
        };
        List<ThemeSelectable> themeSelectables = new ArrayList<>();
        for (Theme theme : themes) {
            themeSelectables.add(new ThemeSelectable(theme));
        }
        ThemeEditor.getInstance().getThemeSelectables().addAll(themeSelectables);
    }

    private Map<String, String> readKeyValueFile(Path p) {
        Map<String, String> map = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(p, UTF8);
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int idx = line.indexOf('=');
                if (idx <= 0) continue;
                String key = line.substring(0, idx);
                String value = line.substring(idx + 1);
                map.put(key, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public String safeFileName(String name) {
        return (name == null) ? "theme" : name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String stripExtension(String name) {
        int idx = name.lastIndexOf('.');
        return (idx <= 0) ? name : name.substring(0, idx);
    }
}