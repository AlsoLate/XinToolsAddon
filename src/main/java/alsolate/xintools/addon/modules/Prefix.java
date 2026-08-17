package alsolate.xintools.addon.modules;

import alsolate.xintools.addon.XinToolsAddon;
import alsolate.xintools.addon.mixin.ChatUtilsAccessor;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class Prefix extends Module {
    private final SettingGroup sgPrefix = settings.getDefaultGroup();
    private final SettingGroup sgColor = settings.getDefaultGroup();

    private final Setting<String> seperatorLeft = sgPrefix.add(new StringSetting.Builder()
        .name("seperator-left")
        .description("What the prefix it should be")
        .defaultValue("[")
        .build()
    );

    private final Setting<String> prefix = sgPrefix.add(new StringSetting.Builder()
        .name("prefix")
        .description("What the prefix it should be")
        .defaultValue("MeteorExtras")
        .build()
    );

    private final Setting<String> seperatorRight = sgPrefix.add(new StringSetting.Builder()
        .name("seperator-right")
        .description("What the prefix it should be")
        .defaultValue("]")
        .build()
    );
    private final Setting<SettingColor> color = sgColor.add(new ColorSetting.Builder()
        .name("color")
        .description("The color of the prefix")
        .defaultValue(new SettingColor(255, 0, 0))
        .build()
    );

    private final Setting<SettingColor> seperatorColor = sgColor.add(new ColorSetting.Builder()
        .name("seperator-color")
        .description("The color of the prefix")
        .defaultValue(new SettingColor(ChatFormatting.GRAY))
        .build()
    );

    public Prefix() {
        super(XinToolsAddon.CATEGORY, "Prefix", "Change the Meteor chat prefix.");
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (this.isActive()) {
            ChatUtilsAccessor.setPrefix(Component.literal(seperatorLeft.get())
                .withColor(seperatorColor.get().getPacked()).append(
                    Component.literal(prefix.get()).withColor(color.get().getPacked()).append(
                        Component.literal(seperatorRight.get() + " ").withColor(seperatorColor.get().getPacked()))));
        }
    }
}
