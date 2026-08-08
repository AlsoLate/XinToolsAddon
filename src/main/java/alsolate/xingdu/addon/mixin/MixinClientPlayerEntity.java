package alsolate.xingdu.addon.mixin;

import alsolate.xingdu.addon.events.MoveEvent;
import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayer {
    public MixinClientPlayerEntity(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    public void handleWaveMoveHook(MoverType movementType, Vec3 movement, CallbackInfo ci) {
        MoveEvent event = new MoveEvent(movement.x, movement.y, movement.z);
        MeteorClient.EVENT_BUS.post(event);

        boolean changed = event.getX() != movement.x || event.getY() != movement.y || event.getZ() != movement.z;
        if (event.isCancelled()) {
            ci.cancel();
        } else if (changed) {
            ci.cancel();
            super.move(movementType, new Vec3(event.getX(), event.getY(), event.getZ()));
        }
    }
}
