package alsolate.xintools.addon.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface AccessorMinecraftClient {
    /**
     * Mojang mappings name for the right-click / item-use repeat delay timer
     * (Yarn: itemUseCooldown).
     */
    @Accessor(value = "rightClickDelay", remap = true)
    void setRightClickDelay(int rightClickDelay);
}
