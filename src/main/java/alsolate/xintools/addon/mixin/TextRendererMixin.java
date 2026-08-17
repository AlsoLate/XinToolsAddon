package alsolate.xintools.addon.mixin;

import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Always use vanilla text renderer so CJK glyphs never become invisible
 * when Meteor's custom font atlas lacks those characters.
 * Independent of the i18n module toggle.
 */
@Mixin(value = TextRenderer.class, remap = false)
public interface TextRendererMixin {

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private static void onGet(CallbackInfoReturnable<TextRenderer> cir) {
        cir.setReturnValue(VanillaTextRenderer.INSTANCE);
    }
}
