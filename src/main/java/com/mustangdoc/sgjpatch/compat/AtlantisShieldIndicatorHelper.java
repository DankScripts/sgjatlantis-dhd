package com.mustangdoc.sgjpatch.compat;

import com.mustangdoc.sgjpatch.client.screens.AtlantisDHDScreenFixed;
import com.mustangdoc.sgjpatch.client.util.AtlantisMiniGateOverlay;
import com.mustangdoc.sgjpatch.mixin.AbstractDHDMenuAccessor;
import com.mustangdoc.sgjpatch.util.DhdLookup;
import java.lang.reflect.Field;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.resources.ResourceLocation;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo.Interface;

public final class AtlantisShieldIndicatorHelper {
   private static final ResourceLocation OFF_OVERLAY = ResourceLocation.fromNamespaceAndPath("sgjpatch", "textures/gui/atlantis_shield_button_off_overlay.png");
   private static final ResourceLocation ON_OVERLAY = ResourceLocation.fromNamespaceAndPath("sgjpatch", "textures/gui/atlantis_shield_button_on_overlay.png");

   private AtlantisShieldIndicatorHelper() {
   }

   public static void render(AtlantisDHDScreenFixed var0, GuiGraphics var1) {
      AtlantisMiniGateOverlay.render(var0, var1);
      AbstractButton var2 = circleButton(var0);
      if (var2 != null) {
         boolean var3 = isShieldActive(var0);
         int var4 = var2.getX();
         int var5 = var2.getY();
         var1.blit(var3 ? ON_OVERLAY : OFF_OVERLAY, var4, var5, 0.0F, 0.0F, 54, 54, 54, 54);
      }
   }

   private static AbstractButton circleButton(AtlantisDHDScreenFixed var0) {
      try {
         Field var1 = AtlantisDHDScreenFixed.class.getDeclaredField("circleButton");
         var1.setAccessible(true);
         return (AbstractButton)var1.get(var0);
      } catch (ReflectiveOperationException var2) {
         return null;
      }
   }

   private static boolean isShieldActive(AtlantisDHDScreenFixed var0) {
      try {
         if (var0.m_6262_() instanceof AbstractDHDMenuAccessor var2) {
            if (var2.sgjpatch$getBlockEntity() instanceof AbstractDHDEntity var4) {
               if (DhdLookup.resolveStargate(var4) instanceof Interface var6) {
                  IrisInfo var7 = var6.irisInfo();
                  return var7 != null && var7.hasIris() && var7.isIrisClosed();
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } catch (Throwable var8) {
         return false;
      }
   }
}
