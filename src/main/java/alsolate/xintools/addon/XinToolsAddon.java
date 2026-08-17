package alsolate.xintools.addon;

import alsolate.xintools.addon.i18n.MeteorTranslator;
import alsolate.xintools.addon.modules.ChatHighlight;
import alsolate.xintools.addon.modules.CustomFov;
import alsolate.xintools.addon.modules.FastXP;
import alsolate.xintools.addon.modules.I18nModule;
import alsolate.xintools.addon.modules.IMGTips;
import alsolate.xintools.addon.modules.IMGWorldStats;
import alsolate.xintools.addon.modules.PacketEat;
import alsolate.xintools.addon.modules.PlayerAlarms;
import alsolate.xintools.addon.modules.PortalESP;
import alsolate.xintools.addon.modules.Prefix;
import alsolate.xintools.addon.modules.XinQueue;
import alsolate.xintools.addon.modules.autologin.AutoLogin;
import alsolate.xintools.addon.modules.basefinder.BaseFinder;
import alsolate.xintools.addon.modules.elytrafly.ElytraFlyPlus;
import alsolate.xintools.addon.modules.sniffernametags.SnifferNametags;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

public class XinToolsAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("XinTools");
    public static final HudGroup HUD_GROUP = new HudGroup("XinTools HUD");

    public static final MeteorTranslator METEOR_TRANSLATOR = new MeteorTranslator();

    @Override
    public void onInitialize() {
        LOG.info("Initializing XinTools Addon");

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                METEOR_TRANSLATOR.reload(mc.getResourceManager());
            }
        } catch (Exception e) {
            LOG.debug("MeteorTranslator early preload skipped: {}", e.toString());
        }

        Modules modules = Modules.get();

        // Master i18n switch (register early)
        modules.add(new I18nModule());

        modules.add(new FastXP());
        modules.add(new XinQueue());
        modules.add(new PacketEat());
        modules.add(new PlayerAlarms());

        modules.add(new SnifferNametags());
        modules.add(new AutoLogin());
        modules.add(new BaseFinder());

        modules.add(new PortalESP());
        modules.add(new ChatHighlight());
        modules.add(new CustomFov());
        modules.add(new IMGWorldStats());
        modules.add(new IMGTips());
        modules.add(new Prefix());
        modules.add(new ElytraFlyPlus());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "alsolate.xintools.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("AlsoLate", "XinToolsAddon");
    }
}
