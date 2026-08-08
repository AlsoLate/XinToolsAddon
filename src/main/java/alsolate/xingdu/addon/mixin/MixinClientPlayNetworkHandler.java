package alsolate.xingdu.addon.mixin;

import alsolate.xingdu.addon.modules.autologin.AutoLoginTextEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "setTitleText", at = @At("HEAD"))
    private void onTitle(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(new AutoLoginTextEvent(packet.text().getString(), AutoLoginTextEvent.Source.Title));
    }

    @Inject(method = "setSubtitleText", at = @At("HEAD"))
    private void onSubtitle(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(new AutoLoginTextEvent(packet.text().getString(), AutoLoginTextEvent.Source.Subtitle));
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void onGameMessage(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(new AutoLoginTextEvent(packet.content().getString(), AutoLoginTextEvent.Source.Chat));
    }
}
