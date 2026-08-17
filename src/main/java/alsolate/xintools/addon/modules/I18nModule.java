package alsolate.xintools.addon.modules;

import alsolate.xintools.addon.XinToolsAddon;
import alsolate.xintools.addon.i18n.MeteorTranslator;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;

/**
 * Master switch for UI string localisation only.
 * Text-renderer CJK fix is always on (see TextRendererMixin) so turning this off
 * never leaves invisible glyphs.
 */
public class I18nModule extends Module {

    public I18nModule() {
        super(
            XinToolsAddon.CATEGORY,
            "i18n",
            "Toggle Chinese UI for Meteor and XinTools. Turn off to restore original English strings."
        );
    }

    public static boolean isActiveStatic() {
        try {
            Modules modules = Modules.get();
            if (modules == null) return false;
            I18nModule mod = modules.get(I18nModule.class);
            return mod != null && mod.isActive();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onActivate() {
        apply(true);
    }

    @Override
    public void onDeactivate() {
        apply(false);
    }

    private void apply(boolean enable) {
        MeteorTranslator translator = XinToolsAddon.METEOR_TRANSLATOR;
        if (translator == null) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (enable && mc != null) {
                translator.invalidate();
                translator.reload(mc.getResourceManager());
            }
            translator.applyToAllModules(enable);
        } catch (Exception e) {
            XinToolsAddon.LOG.error("i18n apply({}) failed", enable, e);
        }
    }
}
