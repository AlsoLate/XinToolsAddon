package alsolate.xingdu.addon.core;

import alsolate.xingdu.addon.XingduAddon;
import alsolate.xingdu.addon.i18n.WaveXinI18n;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.Arrays;

public abstract class WaveXinModule extends Module {
    protected WaveXinModule(Category category, String name, String description, String... aliases) {
        super(category, name, description, aliases);
    }

    @Override
    public void sendToggledMsg() {
        if (Config.get().chatFeedback.get() && chatFeedback) {
            ChatUtils.forceNextPrefixClass(getClass());
            String state = isActive()
                ? ChatFormatting.GREEN + WaveXinI18n.tr("status.wavexin.module.on", "on")
                : ChatFormatting.RED + WaveXinI18n.tr("status.wavexin.module.off", "off");

            ChatUtils.sendMsg(
                hashCode(),
                ChatFormatting.GRAY,
                WaveXinI18n.tr(
                    "message.wavexin.module.toggled",
                    "Toggled (highlight)%s(default) %s(default).",
                    WaveXinI18n.moduleTitle(this),
                    state
                )
            );
        }
    }

    @Override
    public void info(Component message) {
        ChatUtils.sendMsg(message);
    }

    @Override
    public void info(String message, Object... args) {
        ChatUtils.info(message, args);
    }

    @Override
    public void warning(String message, Object... args) {
        XingduAddon.LOG.warn("[WaveXinDebug] module={} message={}", getClass().getSimpleName(), formatLogMessage(message, args));
        ChatUtils.warning(message, args);
    }

    @Override
    public void error(String message, Object... args) {
        XingduAddon.LOG.error("[WaveXinDebug] module={} message={}", getClass().getSimpleName(), formatLogMessage(message, args));
        ChatUtils.error(message, args);
    }

    protected final void infoKey(String key, String fallback, Object... args) {
        info(WaveXinI18n.tr(key, fallback, args));
    }

    protected final void warningKey(String key, String fallback, Object... args) {
        warning(WaveXinI18n.tr(key, fallback, args));
    }

    protected final void errorKey(String key, String fallback, Object... args) {
        error(WaveXinI18n.tr(key, fallback, args));
    }

    private static String formatLogMessage(String message, Object... args) {
        if (args == null || args.length == 0) return message;

        try {
            return message.formatted(args);
        } catch (IllegalArgumentException ignored) {
            return message + " " + Arrays.toString(args);
        }
    }
}
