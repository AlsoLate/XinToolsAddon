package alsolate.xintools.addon.modules;

import alsolate.xintools.addon.XinToolsAddon;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class IMGWorldStats extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgStats = settings.createGroup("Statistics");
    private final SettingGroup sgVisual = settings.createGroup("Visual");

    // ================== 基础信息设置 ==================
    private final Setting<Boolean> showTicks = sgGeneral.add(new BoolSetting.Builder().name("day-time").description("Show the current day time in ticks.").defaultValue(true).build());
    private final Setting<Boolean> showDays = sgGeneral.add(new BoolSetting.Builder().name("survival-days").description("Show the number of days survived in this world.").defaultValue(true).build());
    private final Setting<Boolean> showBiome = sgGeneral.add(new BoolSetting.Builder().name("biome").description("Show the current biome.").defaultValue(true).build());
    private final Setting<Boolean> showWeather = sgGeneral.add(new BoolSetting.Builder().name("weather").description("Show the current weather.").defaultValue(true).build());
    private final Setting<Boolean> showCoords = sgGeneral.add(new BoolSetting.Builder().name("coordinates").description("Show overworld and nether coordinates.").defaultValue(true).build());

    // ================== 统计信息设置 ==================
    private final Setting<Boolean> showPlaytime = sgStats.add(new BoolSetting.Builder().name("playtime").description("Show total playtime.").defaultValue(true).build());
    private final Setting<Boolean> showDistance = sgStats.add(new BoolSetting.Builder().name("distance").description("Show total distance traveled.").defaultValue(true).build());
    private final Setting<Boolean> showKills = sgStats.add(new BoolSetting.Builder().name("kills").description("Show player and mob kills.").defaultValue(true).build());
    private final Setting<Boolean> showDeaths = sgStats.add(new BoolSetting.Builder().name("deaths").description("Show the number of deaths.").defaultValue(true).build());
    private final Setting<Boolean> showBreaks = sgStats.add(new BoolSetting.Builder().name("blocks-mined").description("Show the number of blocks mined.").defaultValue(true).build());

    // ================== 视觉客制化 ==================
    public enum Theme { 简约现代风, 简约苹果风, 黑客帝国, 飘雪, 合成器浪潮 }
    private final Setting<Theme> theme = sgVisual.add(new EnumSetting.Builder<Theme>().name("theme").description("Visual theme.").defaultValue(Theme.简约现代风).build());
    // ================== 分离式：文字控制 ==================
    private final Setting<Double> textX = sgVisual.add(new DoubleSetting.Builder().name("text-x").description("Text X position.").defaultValue(15.0).min(0.0).sliderMax(8000.0).build());
    private final Setting<Double> textY = sgVisual.add(new DoubleSetting.Builder().name("text-y").description("Text Y position.").defaultValue(15.0).min(0.0).sliderMax(8000.0).build());
    private final Setting<Double> textScale = sgVisual.add(new DoubleSetting.Builder().name("text-scale").description("Text scale.").defaultValue(1.0).min(0.1).sliderMax(3.0).build());
    private final Setting<SettingColor> textColor = sgVisual.add(new ColorSetting.Builder().name("text-color").description("Text color.").defaultValue(new SettingColor(255, 255, 255)).build());
    private final Setting<Boolean> textShadow = sgVisual.add(new BoolSetting.Builder().name("text-shadow").description("Render text shadows.").defaultValue(true).build());

    // ================== 分离式：背景与动画控制 ==================
    private final Setting<Double> bgX = sgVisual.add(new DoubleSetting.Builder().name("background-x").description("Background X position.").defaultValue(10.0).min(0.0).sliderMax(8000.0).build());
    private final Setting<Double> bgY = sgVisual.add(new DoubleSetting.Builder().name("background-y").description("Background Y position.").defaultValue(10.0).min(0.0).sliderMax(8000.0).build());
    private final Setting<Double> bgWidth = sgVisual.add(new DoubleSetting.Builder().name("background-width").description("Background width.").defaultValue(150.0).min(10.0).sliderMax(1000.0).build());
    private final Setting<Double> bgHeight = sgVisual.add(new DoubleSetting.Builder().name("background-height").description("Background height.").defaultValue(180.0).min(10.0).sliderMax(1000.0).build());

    // ================== 内部状态变量 ==================
    private int syncTimer = 0;
    private int cachedBlocksMined = 0;
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    public IMGWorldStats() {
        super(XinToolsAddon.CATEGORY, "IMGWorldStats", "World info and playtime stats overlay.");
    }

    @Override
    public void onActivate() {
        requestStats();
        particles.clear();
        for (int i = 0; i < 40; i++) particles.add(new Particle(random.nextDouble() * 200.0, random.nextDouble() * 100.0));
    }

    private void requestStats() {
        if (mc.getConnection() != null && mc.player != null) {
            mc.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
            updateCachedBlocksMined();
        }
    }

    private void updateCachedBlocksMined() {
        if (mc.player == null) return;
        StatsCounter stats = mc.player.getStats();
        int total = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            total += stats.getValue(Stats.BLOCK_MINED.get(block));
        }
        cachedBlocksMined = total;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        if (syncTimer++ >= 200) {
            requestStats();
            syncTimer = 0;
        }

        Theme currentTheme = theme.get();
        for (Particle p : particles) {
            p.y += (currentTheme == Theme.黑客帝国 ? 2.5 : 0.8);
            if (p.y > 1000.0) p.y = 0.0;
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.level == null || mc.player == null || mc.options.hideGui) return;

        List<String> lines = buildDisplayLines();
        if (lines.isEmpty()) return;

        // --- 1. 独立渲染背景框与动画 ---
        double bX = bgX.get();
        double bY = bgY.get();
        double bW = bgWidth.get();
        double bH = bgHeight.get();

        Renderer2D.COLOR.begin();
        renderBackground(bX, bY, bW, bH);
        Renderer2D.COLOR.render();

        // --- 2. 独立渲染文字 ---
        TextRenderer text = TextRenderer.get();
        double tX = textX.get();
        double tY = textY.get();
        double tS = textScale.get();
        Color c = getThemeTextColor();

        text.begin(tS);
        for (String line : lines) {
            text.render(line, tX / tS, tY / tS, c, textShadow.get());
            tY += (text.getHeight() + 2.0) * tS;
        }
        text.end();
    }

    private List<String> buildDisplayLines() {
        List<String> lines = new ArrayList<>();
        StatsCounter stats = mc.player.getStats();

        if (showTicks.get()) {
            long dayTime = mc.level.getDayTime() % 24000L;
            lines.add(String.format("当日刻: %d", dayTime));
        }

        if (showDays.get()) {
            long worldDays = mc.level.getDayTime() / 24000L;
            lines.add("生存天数: " + worldDays + " 天");
        }

        // ================== 生物群系中文翻译修复 ==================
        if (showBiome.get()) {
            Identifier biomeId = mc.level.getBiome(mc.player.blockPosition()).unwrapKey().get().identifier();
            String transKey = "biome." + biomeId.toString().replace(":", ".");
            String localBiomeName = Component.translatable(transKey).getString();
            lines.add("生物群系: " + localBiomeName);
        }

        if (showWeather.get()) {
            String w = mc.level.isThundering() ? "雷雨 ⛈" : (mc.level.isRaining() ? "下雨 🌧" : "晴朗 ☀");
            lines.add("天气: " + w);
        }

        if (showCoords.get()) {
            double px = mc.player.getX();
            double py = mc.player.getY();
            double pz = mc.player.getZ();
            boolean inNether = mc.level.dimension().identifier().getPath().contains("nether");
            lines.add(String.format("主界: %.1f, %.1f, %.1f", inNether ? px * 8.0 : px, py, inNether ? pz * 8.0 : pz));
            lines.add(String.format("下界: %.1f, %.1f, %.1f", inNether ? px : px / 8.0, py, inNether ? pz : pz / 8.0));
        }

        if (showPlaytime.get()) {
            int pt = stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            int hrs = pt / 72000;
            int mins = (pt % 72000) / 1200;
            lines.add(String.format("总时长: %dh %dm", hrs, mins));
        }

        if (showDistance.get()) {
            long cm = stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM))
                    + stats.getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM))
                    + stats.getValue(Stats.CUSTOM.get(Stats.FLY_ONE_CM));
            lines.add(String.format("总距离: %.1f km", cm / 100000.0));
        }

        if (showKills.get()) {
            int pk = stats.getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));
            int mk = stats.getValue(Stats.CUSTOM.get(Stats.MOB_KILLS));
            lines.add("总击杀: 玩家[" + pk + "] / 怪物[" + mk + "]");
        }

        if (showDeaths.get()) {
            lines.add("死亡次数: " + stats.getValue(Stats.CUSTOM.get(Stats.DEATHS)));
        }

        // ================== 挖掘总数修复 ==================
        if (showBreaks.get()) {
            lines.add("挖掘总数: " + cachedBlocksMined);
        }

        return lines;
    }

    private void renderBackground(double x, double y, double w, double h) {
        Theme t = theme.get();
        if (t == Theme.简约现代风) Renderer2D.COLOR.quad(x, y, w, h, new Color(20, 20, 20, 160));
        else if (t == Theme.简约苹果风) Renderer2D.COLOR.quad(x, y, w, h, new Color(245, 245, 247, 210));
        else if (t == Theme.黑客帝国) {
            Renderer2D.COLOR.quad(x, y, w, h, new Color(0, 15, 0, 230));
            for (Particle p : particles) {
                double px = p.x % w;
                double py = p.y % h;
                Renderer2D.COLOR.quad(x + px, y + py, 1.5, 5.0, new Color(0, 255, 70, 150));
            }
        } else if (t == Theme.飘雪) {
            Renderer2D.COLOR.quad(x, y, w, h, new Color(25, 30, 45, 180));
            for (Particle p : particles) {
                double px = p.x % w;
                double py = p.y % h;
                Renderer2D.COLOR.quad(x + px, y + py, 2.0, 2.0, new Color(255, 255, 255, 180));
            }
        } else if (t == Theme.合成器浪潮) {
            Renderer2D.COLOR.quad(x, y, w, h, new Color(45, 0, 70, 200), new Color(0, 180, 255, 200), new Color(0, 180, 255, 200), new Color(45, 0, 70, 200));
        }
    }

    private Color getThemeTextColor() {
        Theme t = theme.get();
        if (t == Theme.简约苹果风) return new Color(25, 25, 25);
        if (t == Theme.黑客帝国) return new Color(0, 255, 0);
        if (t == Theme.合成器浪潮) return new Color(255, 50, 200);
        return textColor.get();
    }

    private static class Particle {
        double x, y;
        Particle(double x, double y) { this.x = x; this.y = y; }
    }
}
