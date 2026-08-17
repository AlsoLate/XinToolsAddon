package alsolate.xintools.addon.mixin;

import alsolate.xintools.addon.XinToolsAddon;
import alsolate.xintools.addon.i18n.MeteorTranslator;
import alsolate.xintools.addon.i18n.WaveXinI18n;
import alsolate.xintools.addon.modules.I18nModule;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(value = Setting.class, remap = false)
public abstract class MeteorSettingMixin {

    @Shadow @Final @Mutable public String title;
    @Shadow @Final @Mutable public String description;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void xintools$translateSetting(
        String name,
        String description,
        Object defaultValue,
        Consumer<?> onChanged,
        Consumer<?> onModuleActivated,
        IVisible visible,
        CallbackInfo ci
    ) {
        MeteorTranslator translator = XinToolsAddon.METEOR_TRANSLATOR;
        if (translator == null) return;

        Setting<?> self = (Setting<?>) (Object) this;

        // ALWAYS snapshot first — including XinTools settings.
        translator.rememberSetting(self, this.title, this.description);

        if (!I18nModule.isActiveStatic()) return;

        boolean isXinTools = self.module != null
            && self.module.getClass().getName().startsWith(WaveXinI18n.PACKAGE_PREFIX);

        if (isXinTools) {
            String st = WaveXinI18n.tr(WaveXinI18n.settingKey(self.module, self, "title"), this.title);
            String sd = WaveXinI18n.tr(WaveXinI18n.settingKey(self.module, self, "description"), this.description);
            this.title = st;
            this.description = sd;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc != null) translator.reload(mc.getResourceManager());

        this.title = translator.translate("Setting.Meteor." + name, name);
        this.description = translator.translate("Setting.Meteor." + name + ".Description", description);
    }
}
