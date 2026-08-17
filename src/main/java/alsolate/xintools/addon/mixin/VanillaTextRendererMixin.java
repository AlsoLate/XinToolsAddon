package alsolate.xintools.addon.mixin;

import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Always scale glyphs individually for correct CJK width. */
@Mixin(value = VanillaTextRenderer.class, remap = false)
public class VanillaTextRendererMixin {

    @Shadow
    private boolean scaleIndividually;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(String text, double x, double y, Color color, boolean shadow,
                          CallbackInfoReturnable<Double> cir) {
        this.scaleIndividually = true;
    }
}
