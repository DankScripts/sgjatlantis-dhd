package com.mustangdoc.sgjpatch.item;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.povstalec.sgjourney.common.config.CommonIrisConfig;
import net.povstalec.sgjourney.common.items.StargateIrisItem;

public final class PegasusUpgradedShieldItem extends StargateIrisItem {
   private static final int DEFAULT_DURABILITY = 1500;
   public static final ResourceLocation SHIELD_TEXTURE = ResourceLocation.fromNamespaceAndPath(
      "sgjadditions_capacity_patch", "textures/entity/stargate/shield/pegasus_upgraded_shield.jpg"
   );

   public PegasusUpgradedShieldItem(Properties properties) {
      super(properties, SHIELD_TEXTURE, PegasusUpgradedShieldItem::resolveDurability);
   }

   private static int resolveDurability() {
      for (String fieldName : new String[]{"naquadah_alloy_iris_durability", "naquadah_iris_durability", "iris_durability"}) {
         try {
            Field field = CommonIrisConfig.class.getField(fieldName);
            Object value = field.get(null);
            if (value != null && value.getClass().getMethod("get").invoke(value) instanceof Number number) {
               return number.intValue();
            }
         } catch (ReflectiveOperationException var8) {
         }
      }

      return 1500;
   }

   public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, level, tooltip, flag);
      tooltip.add(Component.translatable("item.sgjadditions_capacity_patch.pegasus_upgraded_shield.tooltip.search_hint"));
   }
}
