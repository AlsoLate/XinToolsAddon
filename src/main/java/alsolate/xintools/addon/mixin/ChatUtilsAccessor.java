package alsolate.xintools.addon.mixin;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(remap = false, value = ChatUtils.class)
public interface ChatUtilsAccessor {
    @Accessor(value = "PREFIX", remap = true)
    static void setPrefix(Component text) {
    }
}
