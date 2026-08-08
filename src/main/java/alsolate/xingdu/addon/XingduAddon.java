package alsolate.xingdu.addon;

import alsolate.xingdu.addon.modules.Automend;
import alsolate.xingdu.addon.modules.BetterPlayerAlarms;
import alsolate.xingdu.addon.modules.MeteorTextFix;
import alsolate.xingdu.addon.modules.PacketEat;
import alsolate.xingdu.addon.modules.XinQueue;
import alsolate.xingdu.addon.modules.autologin.AutoLogin;
import alsolate.xingdu.addon.modules.basefinder.BaseFinder;
import alsolate.xingdu.addon.modules.betterelytrafly.BetterElytraFly;
import alsolate.xingdu.addon.modules.sniffernametags.SnifferNametags;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class XingduAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Xingdu");
    public static final HudGroup HUD_GROUP = new HudGroup("Xingdu HUD");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Xingdu Addon");

        Modules modules = Modules.get();
        // 原有模块
        modules.add(new Automend());
        modules.add(new MeteorTextFix());
        modules.add(new XinQueue());
        modules.add(new PacketEat());
        modules.add(new BetterPlayerAlarms());

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
        return "alsolate.xingdu.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("AlsoLate", "XingduAddon");
    }
}
