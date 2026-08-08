package alsolate.xintools.addon.mixin;

import alsolate.xintools.addon.events.Event;
import alsolate.xintools.addon.events.TravelEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class MixinPlayerEntity {
    // 注意：不能在 Mixin 类中持有 static Minecraft 字段——它会被注入到目标类 Player，
    // 在 Player 类加载时执行 Minecraft.getInstance() 会返回 null（时序问题），
    // 导致后续访问 mc.player 时空指针崩溃。必须改为在方法体内获取实例。

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void handleWaveTravelPre(Vec3 movementInput, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || player != mc.player) return;

        TravelEvent event = new TravelEvent(Event.Stage.Pre, player);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
            event = new TravelEvent(Event.Stage.Post, player);
            MeteorClient.EVENT_BUS.post(event);
        }
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void handleWaveTravelPost(Vec3 movementInput, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || player != mc.player) return;

        TravelEvent event = new TravelEvent(Event.Stage.Post, player);
        MeteorClient.EVENT_BUS.post(event);
    }
}
