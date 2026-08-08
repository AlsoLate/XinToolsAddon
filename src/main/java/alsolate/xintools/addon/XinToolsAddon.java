package alsolate.xintools.addon;

import alsolate.xintools.addon.modules.Automend;
import alsolate.xintools.addon.modules.PacketEat;
import alsolate.xintools.addon.modules.PlayerAlarms;
import alsolate.xintools.addon.modules.XinQueue;
import alsolate.xintools.addon.modules.autologin.AutoLogin;
import alsolate.xintools.addon.modules.basefinder.BaseFinder;
import alsolate.xintools.addon.modules.betterelytrafly.BetterElytraFly;
import alsolate.xintools.addon.modules.sniffernametags.SnifferNametags;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class XinToolsAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("XinTools");
    public static final HudGroup HUD_GROUP = new HudGroup("XinTools HUD");

    @Override
    public void onInitialize() {
        LOG.info("Initializing XinTools Addon");

        Modules modules = Modules.get();
        // 原有模块
        modules.add(new Automend());
        modules.add(new XinQueue());
        modules.add(new PacketEat());
        modules.add(new PlayerAlarms());

        // WaveXinAddon 迁入模块
        modules.add(new BetterElytraFly());
        modules.add(new SnifferNametags());
        modules.add(new AutoLogin());
        modules.add(new BaseFinder());
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
