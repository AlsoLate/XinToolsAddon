package liuliuliu0127.donkeyspawner.addon.mixin;

import liuliuliu0127.donkeyspawner.addon.modules.BetterEntityControl;
import liuliuliu0127.donkeyspawner.addon.utils.MountTeleportUtil;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Strider.class)
public abstract class StriderMixin {
    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
    private void overrideGetControllingPassenger(CallbackInfoReturnable<LivingEntity> cir) {
        BetterEntityControl bec = Modules.get().get(BetterEntityControl.class);
        Strider strider = (Strider) (Object) this;

        if (MountTeleportUtil.shouldSpoofSaddle(strider) || (bec != null && bec.isActive() && bec.spoofSaddle())) {
            if (strider.getFirstPassenger() instanceof Player player) {
                cir.setReturnValue(player);
            }
        }
    }
}
