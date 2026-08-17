package alsolate.xintools.addon.mixin;

import alsolate.xintools.addon.modules.IMGTips;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(remap = false, value = Screen.class)
public class MixinScreenOverlay {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics context, int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
        IMGTips.renderOnScreen(context);
    }
}
