package alsolate.xintools.addon.i18n;

import alsolate.xintools.addon.XinToolsAddon;
import alsolate.xintools.addon.modules.I18nModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.gui.screens.ModuleScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.macros.Macro;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.profiles.Profile;
import meteordevelopment.meteorclient.systems.profiles.Profiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Global translator with irreversible original-string snapshots + live GUI reload.
 */
public final class MeteorTranslator {
    private static final boolean DEVELOPMENT = Boolean.getBoolean("fabric.development");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final VarHandle MODULE_TITLE;
    private static final VarHandle MODULE_DESCRIPTION;
    private static final VarHandle SETTING_TITLE;
    private static final VarHandle SETTING_DESCRIPTION;
    private static final VarHandle SETTING_GROUP_NAME;

    static {
        try {
            MethodHandles.Lookup modLookup = MethodHandles.privateLookupIn(Module.class, MethodHandles.lookup());
            MODULE_TITLE = modLookup.findVarHandle(Module.class, "title", String.class);
            MODULE_DESCRIPTION = modLookup.findVarHandle(Module.class, "description", String.class);

            MethodHandles.Lookup setLookup = MethodHandles.privateLookupIn(Setting.class, MethodHandles.lookup());
            SETTING_TITLE = setLookup.findVarHandle(Setting.class, "title", String.class);
            SETTING_DESCRIPTION = setLookup.findVarHandle(Setting.class, "description", String.class);

            MethodHandles.Lookup groupLookup = MethodHandles.privateLookupIn(SettingGroup.class, MethodHandles.lookup());
            SETTING_GROUP_NAME = groupLookup.findVarHandle(SettingGroup.class, "name", String.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final JsonObject missingKeys = new JsonObject();
    private volatile Map<String, String> strings = Map.of();
    private volatile String loadedLang = "";

    private final Map<Module, String[]> moduleOriginals = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<Setting<?>, String[]> settingOriginals = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<SettingGroup, String> groupOriginals = Collections.synchronizedMap(new IdentityHashMap<>());

    public String translate(String key, String fallback) {
        String value = strings.get(key);
        if (value != null) return value;
        if (DEVELOPMENT && fallback != null) recordMissing(key, fallback);
        return fallback != null ? fallback : key;
    }

    public void reload(ResourceManager manager) {
        if (manager == null) return;
        String langCode = currentLangCode();
        if (langCode.equals(loadedLang) && !strings.isEmpty()) return;

        HashMap<String, String> map = new HashMap<>();
        loadFile(manager, "en_us", map);
        if (!"en_us".equals(langCode)) loadFile(manager, langCode, map);

        this.strings = Collections.unmodifiableMap(map);
        this.loadedLang = langCode;
        XinToolsAddon.LOG.info("MeteorTranslator loaded {} strings for '{}'", map.size(), langCode);
    }

    public void invalidate() {
        this.loadedLang = "";
    }

    public void rememberModule(Module module, String originalTitle, String originalDescription) {
        if (module == null) return;
        moduleOriginals.putIfAbsent(module, new String[]{
            safe(originalTitle, module.name),
            safe(originalDescription, module.name)
        });
    }

    public void rememberSetting(Setting<?> setting, String originalTitle, String originalDescription) {
        if (setting == null) return;
        settingOriginals.putIfAbsent(setting, new String[]{
            safe(originalTitle, setting.name),
            safe(originalDescription, setting.name)
        });
    }

    public void applyToAllModules(boolean enable) {
        Minecraft mc = Minecraft.getInstance();
        if (enable && mc != null) {
            invalidate();
            reload(mc.getResourceManager());
        }

        int modulesTouched = 0;
        int settingsTouched = 0;

        Modules modules = Modules.get();
        if (modules != null) {
            for (Module module : modules.getAll()) {
                modulesTouched += applyModule(module, enable) ? 1 : 0;
                settingsTouched += applySettings(module.settings, enable, module);
            }
        }

        // Config / HUD / Macros / Profiles — not under Modules
        try {
            Config config = Config.get();
            if (config != null) settingsTouched += applySettings(config.settings, enable, null);
        } catch (Throwable t) {
            XinToolsAddon.LOG.debug("Config i18n skip: {}", t.toString());
        }

        try {
            GuiTheme theme = GuiThemes.get();
            if (theme != null) settingsTouched += applySettings(theme.settings, enable, null);
        } catch (Throwable t) {
            XinToolsAddon.LOG.debug("GUI theme i18n skip: {}", t.toString());
        }

        try {
            Hud hud = Hud.get();
            if (hud != null) {
                settingsTouched += applySettings(hud.settings, enable, null);
                for (HudElement element : hud) {
                    try {
                        // HudElement.settings if present
                        var field = HudElement.class.getField("settings");
                        Object s = field.get(element);
                        if (s instanceof Settings hs) settingsTouched += applySettings(hs, enable, null);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable t) {
            XinToolsAddon.LOG.debug("Hud i18n skip: {}", t.toString());
        }

        try {
            Macros macros = Macros.get();
            if (macros != null) {
                for (Macro macro : macros) {
                    settingsTouched += applySettings(macro.settings, enable, null);
                }
            }
        } catch (Throwable t) {
            XinToolsAddon.LOG.debug("Macros i18n skip: {}", t.toString());
        }

        try {
            Profiles profiles = Profiles.get();
            if (profiles != null) {
                for (Profile profile : profiles) {
                    settingsTouched += applySettings(profile.settings, enable, null);
                }
            }
        } catch (Throwable t) {
            XinToolsAddon.LOG.debug("Profiles i18n skip: {}", t.toString());
        }

        // Baritone / PathManager tab settings
        try {
            var pm = PathManagers.get();
            if (pm != null) {
                var iset = pm.getSettings();
                if (iset != null && iset.get() != null) {
                    settingsTouched += applySettings(iset.get(), enable, null);
                }
            }
        } catch (Throwable t) {
            XinToolsAddon.LOG.debug("PathManager i18n skip: {}", t.toString());
        }

        XinToolsAddon.LOG.info(
            "MeteorTranslator apply enable={} modules={} settings={}",
            enable, modulesTouched, settingsTouched
        );

        // Live GUI refresh — no manual reopen needed
        refreshOpenGui();
    }

    private boolean applyModule(Module module, boolean enable) {
        String[] orig = moduleOriginals.get(module);
        if (orig == null) {
            if (enable) {
                orig = new String[]{safe(module.title, module.name), safe(module.description, module.name)};
            } else {
                orig = new String[]{module.name, safe(module.description, module.name)};
            }
            moduleOriginals.put(module, orig);
        }

        boolean isXinTools = module.getClass().getName().startsWith(WaveXinI18n.PACKAGE_PREFIX);

        if (enable) {
            if (isXinTools) {
                writeModule(module,
                    WaveXinI18n.tr(WaveXinI18n.moduleKey(module, "title"), orig[0]),
                    WaveXinI18n.tr(WaveXinI18n.moduleKey(module, "description"), orig[1])
                );
            } else {
                // Try both exact name and common alias forms (hyphen stripped / lowercased)
                String t = translateModuleTitle(module.name, orig[0]);
                String d = translateModuleDescription(module.name, orig[1]);
                writeModule(module, t, d);
            }
        } else {
            writeModule(module, orig[0], orig[1]);
        }
        return true;
    }

    /** Try Module.Meteor.{name} and alias without hyphens (rejects uses "fullflight"). */
    private String translateModuleTitle(String name, String fallback) {
        String v = strings.get("Module.Meteor." + name);
        if (v != null) return v;
        String compact = name.replace("-", "").replace(" ", "");
        if (!compact.equals(name)) {
            v = strings.get("Module.Meteor." + compact);
            if (v != null) return v;
        }
        return translate("Module.Meteor." + name, fallback);
    }

    private String translateModuleDescription(String name, String fallback) {
        String v = strings.get("Module.Meteor." + name + ".Description");
        if (v != null) return v;
        String compact = name.replace("-", "").replace(" ", "");
        if (!compact.equals(name)) {
            v = strings.get("Module.Meteor." + compact + ".Description");
            if (v != null) return v;
        }
        return translate("Module.Meteor." + name + ".Description", fallback);
    }

    private int applySettings(Settings settings, boolean enable, Module owner) {
        if (settings == null) return 0;
        int n = 0;
        for (SettingGroup group : settings) {
            applyGroupName(group, enable, owner);
            for (Setting<?> setting : group) {
                applyOneSetting(setting, enable, owner);
                n++;
            }
        }
        return n;
    }

    private void applyGroupName(SettingGroup group, boolean enable, Module owner) {
        if (group == null) return;
        String orig = groupOriginals.get(group);
        if (orig == null) {
            // Capture BEFORE any translation (must stay English for stable keys)
            orig = group.name == null || group.name.isBlank() ? "General" : group.name;
            groupOriginals.put(group, orig);
        }
        try {
            if (enable) {
                String translated;
                String generic = genericGroupTranslation(orig);
                if (generic != null) {
                    SETTING_GROUP_NAME.set(group, generic);
                    return;
                }
                if (owner != null && owner.getClass().getName().startsWith(WaveXinI18n.PACKAGE_PREFIX)) {
                    String key = "group.wavexin." + WaveXinI18n.keySegment(owner.name) + "." + WaveXinI18n.keySegment(orig);
                    translated = WaveXinI18n.tr(key, orig);
                } else {
                    translated = translate("SettingGroup.Meteor." + orig, orig);
                }
                SETTING_GROUP_NAME.set(group, safe(translated, orig));
            } else {
                SETTING_GROUP_NAME.set(group, orig);
            }
        } catch (Throwable ex) {
            XinToolsAddon.LOG.debug("Failed to write group name {}: {}", orig, ex.toString());
        }
    }

    private static String genericGroupTranslation(String original) {
        if (!I18nModule.isActiveStatic() || original == null) return null;
        return switch (original.trim().toLowerCase(Locale.ROOT)) {
            case "general", "default" -> "基础设置";
            case "control", "controls" -> "控制";
            case "bind", "binding" -> "绑定";
            case "render", "renderer" -> "渲染";
            case "visual", "visuals" -> "视觉设置";
            case "statistics", "stats" -> "统计信息";
            case "colors", "color" -> "颜色";
            case "text" -> "文字";
            case "background" -> "背景";
            case "outline" -> "轮廓";
            case "separator" -> "分隔线";
            case "scrollbar" -> "滚动条";
            case "slider" -> "滑块";
            case "stanscript" -> "Stanscript";
            default -> null;
        };
    }

    private void applyOneSetting(Setting<?> setting, boolean enable, Module owner) {
        String[] sOrig = settingOriginals.get(setting);
        if (sOrig == null) {
            if (enable) {
                sOrig = new String[]{safe(setting.title, setting.name), safe(setting.description, setting.name)};
            } else {
                sOrig = new String[]{setting.name, safe(setting.description, setting.name)};
            }
            settingOriginals.put(setting, sOrig);
        }

        if (enable) {
            if (owner != null && owner.getClass().getName().startsWith(WaveXinI18n.PACKAGE_PREFIX)) {
                // Build keys from original group name so Chinese group titles do not break lookups
                String gName = "general";
                try {
                    for (SettingGroup g : owner.settings) {
                        if (g != null) {
                            for (Setting<?> s : g) {
                                if (s == setting) {
                                    gName = groupOriginals.getOrDefault(g, g.name == null ? "General" : g.name);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {}
                String base = "setting.wavexin." + WaveXinI18n.keySegment(owner.name) + "."
                    + WaveXinI18n.keySegment(gName) + "." + WaveXinI18n.keySegment(setting.name);
                writeSetting(setting,
                    WaveXinI18n.tr(base + ".title", sOrig[0]),
                    WaveXinI18n.tr(base + ".description", sOrig[1])
                );
            } else {
                writeSetting(setting,
                    translateGuiSetting(setting.name, translate("Setting.Meteor." + setting.name, sOrig[0])),
                    translate("Setting.Meteor." + setting.name + ".Description", sOrig[1])
                );
            }
        } else {
            writeSetting(setting, sOrig[0], sOrig[1]);
        }
    }

    private static String translateGuiSetting(String name, String fallback) {
        if (!I18nModule.isActiveStatic() || name == null) return fallback;
        return switch (name) {
            case "Theme" -> "主题";
            case "Scale" -> "缩放";
            case "Module Alignment" -> "模块对齐";
            case "Category Icons" -> "分类图标";
            case "Hide HUD" -> "隐藏 HUD";
            case "Accent Color" -> "强调色";
            case "Checkbox Color" -> "复选框颜色";
            case "Plus Color" -> "加号颜色";
            case "Minus Color" -> "减号颜色";
            case "Favorite Color" -> "收藏颜色";
            case "Text Color" -> "文字颜色";
            case "Text Secondary Text Color" -> "次要文字颜色";
            case "Text Highlight Color" -> "文字高亮颜色";
            case "Title Text Color" -> "标题文字颜色";
            case "Logged In Text Color" -> "登录文字颜色";
            case "Placeholder Color" -> "占位文字颜色";
            case "Background Color" -> "背景颜色";
            case "Hovered Background Color" -> "悬停背景颜色";
            case "Pressed Background Color" -> "按下背景颜色";
            case "Module Background Color" -> "模块背景颜色";
            case "Outline Color" -> "轮廓颜色";
            case "Hovered Outline Color" -> "悬停轮廓颜色";
            case "Pressed Outline Color" -> "按下轮廓颜色";
            case "Separator Text Color" -> "分隔线文字颜色";
            case "Separator Center Color" -> "分隔线中心颜色";
            case "Separator Edges Color" -> "分隔线边缘颜色";
            case "Scrollbar Color" -> "滚动条颜色";
            case "Hovered Scrollbar Color" -> "悬停滚动条颜色";
            case "Pressed Scrollbar Color" -> "按下滚动条颜色";
            case "Slider Handle Color" -> "滑块手柄颜色";
            case "Hovered Slider Handle Color" -> "悬停滑块手柄颜色";
            case "Pressed Slider Handle Color" -> "按下滑块手柄颜色";
            case "Slider Left Color" -> "滑块左侧颜色";
            case "Slider Right Color" -> "滑块右侧颜色";
            default -> fallback;
        };
    }

    /**
     * Force Meteor GUI to rebuild so new titles appear immediately.
     * Prefer close+reopen of the current Tab; fall back to WidgetScreen.reload().
     */
    public static void refreshOpenGui() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            Screen screen = mc.screen;
            if (screen == null) return;

            // 1) Tab screens (Modules / Config / HUD / Macros / Profiles / Baritone)
            if (screen instanceof TabScreen tabScreen) {
                final Tab tab = tabScreen.tab;
                // Double-defer: first escapes the click handler, second runs after screen settles.
                mc.execute(() -> mc.execute(() -> {
                    try {
                        if (tab != null) {
                            tab.openScreen(GuiThemes.get());
                        } else if (!Tabs.get().isEmpty()) {
                            Tabs.get().getFirst().openScreen(GuiThemes.get());
                        }
                    } catch (Throwable t) {
                        XinToolsAddon.LOG.warn("Tab GUI reopen failed: {}", t.toString());
                    }
                }));
                return;
            }

            // 2) Single-module settings screen
            if (screen instanceof ModuleScreen moduleScreen) {
                try {
                    var f = ModuleScreen.class.getDeclaredField("module");
                    f.setAccessible(true);
                    Module mod = (Module) f.get(moduleScreen);
                    mc.execute(() -> {
                        try {
                            if (mod != null) {
                                mc.setScreen(GuiThemes.get().moduleScreen(mod));
                            } else {
                                moduleScreen.reload();
                            }
                        } catch (Throwable t) {
                            XinToolsAddon.LOG.warn("ModuleScreen reopen failed: {}", t.toString());
                        }
                    });
                    return;
                } catch (Throwable t) {
                    XinToolsAddon.LOG.debug("ModuleScreen reflect fail: {}", t.toString());
                }
            }

            // 3) Generic WidgetScreen
            if (screen instanceof WidgetScreen widgetScreen) {
                mc.execute(() -> {
                    try {
                        widgetScreen.reload();
                    } catch (Throwable t) {
                        XinToolsAddon.LOG.debug("WidgetScreen.reload fail: {}", t.toString());
                    }
                });
            }
        } catch (Throwable t) {
            XinToolsAddon.LOG.warn("GUI refresh fail: {}", t.toString());
        }
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void writeModule(Module module, String title, String description) {
        try {
            MODULE_TITLE.set(module, safe(title, module.name));
            MODULE_DESCRIPTION.set(module, safe(description, module.name));
        } catch (Throwable t) {
            XinToolsAddon.LOG.warn("Failed to write module strings for {}: {}", module.name, t.toString());
        }
    }

    private static void writeSetting(Setting<?> setting, String title, String description) {
        try {
            SETTING_TITLE.set(setting, safe(title, setting.name));
            SETTING_DESCRIPTION.set(setting, safe(description, setting.name));
        } catch (Throwable t) {
            XinToolsAddon.LOG.warn("Failed to write setting strings for {}: {}", setting.name, t.toString());
        }
    }

    private void loadFile(ResourceManager manager, String langCode, Map<String, String> out) {
        String[] candidates = {
            "lang/meteor_" + langCode + ".json",
            "lang/" + langCode + ".json"
        };
        for (String path : candidates) {
            Identifier id = Identifier.fromNamespaceAndPath("xintools", path);
            try {
                List<Resource> resources = manager.getResourceStack(id);
                if (resources.isEmpty()) continue;
                for (Resource resource : resources) {
                    try (InputStream stream = resource.open()) {
                        JsonObject obj = GSON.fromJson(
                            new java.io.InputStreamReader(stream, StandardCharsets.UTF_8),
                            JsonObject.class
                        );
                        if (obj == null) continue;
                        for (var entry : obj.entrySet()) {
                            if (entry.getValue().isJsonPrimitive()) {
                                out.put(entry.getKey(), entry.getValue().getAsString());
                            }
                        }
                    } catch (IOException e) {
                        XinToolsAddon.LOG.warn("MeteorTranslator read fail {} / {}", path, resource.sourcePackId(), e);
                    }
                }
                return;
            } catch (Exception e) {
                XinToolsAddon.LOG.debug("MeteorTranslator skip {}: {}", path, e.toString());
            }
        }
    }

    private static String currentLangCode() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getLanguageManager() != null) {
                String code = mc.getLanguageManager().getSelected();
                if (code != null && !code.isBlank()) return code.toLowerCase(Locale.ROOT);
            }
        } catch (Exception ignored) {
        }
        return "en_us";
    }

    private void recordMissing(String key, String fallback) {
        synchronized (missingKeys) {
            if (missingKeys.has(key)) return;
            missingKeys.addProperty(key, fallback);
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("meteor_missing_lang.json"))) {
                GSON.toJson(missingKeys, writer);
            } catch (IOException e) {
                XinToolsAddon.LOG.debug("missing lang write fail", e);
            }
        }
    }
}
