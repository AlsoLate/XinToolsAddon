package alsolate.xintools.addon.mixin;

import alsolate.xintools.addon.XinToolsAddon;
import alsolate.xintools.addon.i18n.MeteorTranslator;
import alsolate.xintools.addon.i18n.WaveXinI18n;
import alsolate.xintools.addon.modules.I18nModule;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Module.class, remap = false, priority = 999)
public abstract class MeteorModuleMixin {

    @Shadow @Final public String name;
    @Shadow @Final @Mutable public String title;
    @Shadow @Final @Mutable public String description;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void xintools$translateModule(CallbackInfo ci) {
        Module self = (Module) (Object) this;
        MeteorTranslator translator = XinToolsAddon.METEOR_TRANSLATOR;
        if (translator == null) return;

        // ALWAYS snapshot constructor originals first (English / code defaults).
        translator.rememberModule(self, this.title, this.description);

        if (!I18nModule.isActiveStatic()) return;

        if (self.getClass().getName().startsWith(WaveXinI18n.PACKAGE_PREFIX)) {
            this.title = WaveXinI18n.tr(WaveXinI18n.moduleKey(self, "title"), this.title);
            this.description = WaveXinI18n.tr(WaveXinI18n.moduleKey(self, "description"), this.description);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc != null) translator.reload(mc.getResourceManager());

        this.title = translator.translate("Module.Meteor." + this.name, this.title);
        this.description = translator.translate("Module.Meteor." + this.name + ".Description", this.description);
    }
}
