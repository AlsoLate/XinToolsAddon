package alsolate.xintools.addon.mixin;

import alsolate.xintools.addon.modules.CustomFov;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(remap = false, value = GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, remap = false)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> info) {
        CustomFov module = Modules.get().get(CustomFov.class);

        if (module != null && module.isActive()) {
            // changingFov 是关键：
            // true  -> 正在为“世界/地形”计算 FOV
            // false -> 正在为“手持物品/手臂”计算 FOV

            if (!changingFov) {
                info.setReturnValue(module.itemFov.get().floatValue());
            } else {
                float fov = module.fov.get().floatValue();

                Zoom zoom = Modules.get().get(Zoom.class);
                if (zoom != null) {
                    double scaling = zoom.getScaling();
                    if (scaling > 1.0) {
                        fov /= (float) scaling;
                    }
                }

                info.setReturnValue(fov);
            }
        }
    }
}
