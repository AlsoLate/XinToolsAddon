package alsolate.xintools.addon.modules;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import alsolate.xintools.addon.XinToolsAddon;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Locale;

public class XinQueue extends Module {
    private final JsonObject questions;
    private boolean safetyApplied;

    public XinQueue() {
        super(XinToolsAddon.CATEGORY, "AutoQueue", "automatically answer questions when queueing in 2b2t.xin");
        this.questions = loadQuestions();
    }

    @Override
    public void onActivate() {
        safetyApplied = false;
        disableChatRewriters();
    }

    private void disableChatRewriters() {
        if (safetyApplied) return;
        safetyApplied = true;

        int disabled = 0;
        for (Module module : Modules.get().getAll()) {
            if (module == this || !module.isActive() || !isChatRewriter(module)) continue;
            module.toggle();
            disabled++;
        }
        if (disabled > 0) {
            ChatUtils.info("[XinTools] AutoQueue disabled " + disabled
                + " chat prefix/suffix/spam module(s) to keep answers unmodified.");
        }
    }

    private static boolean isChatRewriter(Module module) {
        String id = module.name == null ? "" : module.name.toLowerCase(Locale.ROOT)
            .replace(" ", "").replace("-", "").replace("_", "");
        return id.equals("prefix") || id.contains("spam") || id.contains("suffix")
            || id.contains("chatprefix") || id.contains("chatsuffix");
    }

    private JsonObject loadQuestions() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("questions.json")),
                StandardCharsets.UTF_8))) {
            String content = reader.lines().collect(Collectors.joining("\n"));
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new JsonObject(); // 加载失败时返回空对象，避免崩溃
        }
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (mc.getConnection() == null) return;  // Mojang mapping: getConnection()

        String message = event.getMessage().getString();
        if (!message.contains("丨")) return;

        String[] parts = message.split("丨");
        if (parts.length != 2) return;

        String question = parts[0].replaceAll("<[^>]*>", "").trim();
        String options = parts[1].trim();

        if (!questions.has(question)) return;

        Pattern pattern = Pattern.compile(questions.get(question).getAsString());
        Matcher matcher = pattern.matcher(options);

        if (!matcher.find()) return;

        String answer = matcher.group(1);
        // Mojang mapping: 发送聊天消息使用 mc.player.connection.sendChat()
        mc.player.connection.sendChat(answer);
    }
}
