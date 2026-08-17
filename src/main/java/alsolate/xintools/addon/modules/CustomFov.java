package alsolate.xintools.addon.modules;

import alsolate.xintools.addon.XinToolsAddon;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class CustomFov extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Double> fov = sgGeneral.add(new DoubleSetting.Builder()
        .name("fov")
        .description("自定义游戏视角。")
        .defaultValue(110)
        .min(30)
        .max(170)
        .sliderMax(170)
        .build()
    );

    public final Setting<Double> itemFov = sgGeneral.add(new DoubleSetting.Builder()
        .name("item-fov")
        .description("自定义手持物品视角。")
        .defaultValue(70)
        .min(30)
        .max(170)
        .sliderMax(170)
        .build()
    );

    public CustomFov() {
        super(XinToolsAddon.CATEGORY, "CustomFov", "Independently adjust world FOV and held-item FOV.");
    }
}
