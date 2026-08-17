package alsolate.xintools.addon.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

public class AlienInventoryUtil {
   private static final Minecraft mc = Minecraft.getInstance();

   public static int getPotionCount(MobEffect targetEffect) {
      int count = 0;
      for (int i = 35; i >= 0; i--) {
         ItemStack itemStack = mc.player.getInventory().getItem(i);
         if (itemStack.getItem() == Items.SPLASH_POTION) {
            PotionContents potionContents = itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            for (MobEffectInstance effect : potionContents.getAllEffects()) {
               if (effect.getEffect().value() == targetEffect) {
                  count += itemStack.getCount();
               }
            }
         }
      }
      return count;
   }
}
