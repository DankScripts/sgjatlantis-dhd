package com.mustangdoc.sgjpatch.client.render;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

public final class PegasusShieldTransitionHelper {
   private static final float MAX_ALPHA = 0.9F;
   private static final long ENGAGE_DURATION_NS = 1776000000L;
   private static final long DISENGAGE_DURATION_NS = 1829000000L;
   private static final float SHIELD_RADIUS = 4.02F;
   private static final Map<Object, PegasusShieldTransitionHelper.Transition> STATES = new WeakHashMap<>();
   private static float currentReveal;
   private static boolean currentTargetClosed;
   private static float currentTransitionPhase;
   private static float currentTimeSeconds;

   private PegasusShieldTransitionHelper() {
   }

   public static float alpha(Object var0, float var1) {
      boolean var2 = readClosed(var0, var1);
      long var3 = System.nanoTime();
      synchronized (STATES) {
         PegasusShieldTransitionHelper.Transition var6 = STATES.get(var0);
         if (var6 == null) {
            float var7 = var2 ? 1.0F : 0.0F;
            var6 = new PegasusShieldTransitionHelper.Transition(var2, var7, var7, var3);
            STATES.put(var0, var6);
         } else {
            float var10 = interpolate(var6, var3);
            if (var2 != var6.targetClosed) {
               if (var2) {
                  // Begin a visible energy flash on the first closing frame.
                  var10 = Math.max(var10, 0.08F);
               }
               var6.startReveal = var10;
               var6.currentReveal = var10;
               var6.targetClosed = var2;
               var6.startedAt = var3;
            }
         }

         float var11 = interpolate(var6, var3);
         var6.currentReveal = var11;
         currentReveal = var11;
         currentTargetClosed = var6.targetClosed;
         currentTransitionPhase = transitionPhase(var6, var3);
         currentTimeSeconds = (float)((double)var3 * 1.0E-9);
         return !var6.targetClosed && var11 <= 0.001F ? 0.0F : 0.9F;
      }
   }

   public static float vertexAlpha(float[] var0, float var1) {
      if (var0 != null && var0.length >= 2 && !(var1 <= 0.0F)) {
         float var2 = currentReveal;
         if (var2 <= 0.0F) {
            return 0.0F;
         } else if (var2 >= 0.9995F && currentTargetClosed) {
            return var1;
         } else {
            float var3 = var0[0];
            float var4 = var0[1];
            float var5 = var0.length > 2 ? var0[2] : 0.0F;
            float var6 = (float)Math.sqrt((double)(var3 * var3 + var4 * var4)) / 4.02F;
            var6 = clamp01(var6);
            float var7 = (float)Math.atan2((double)var4, (double)var3);
            float var8 = noise01(var3, var4, var5);
            float var9 = 0.5F + 0.5F * (float)Math.sin((double)(currentTimeSeconds * 13.0F + var7 * 7.0F + var6 * 10.0F));
            float var10 = 0.5F + 0.5F * (float)Math.sin((double)(currentTimeSeconds * 21.0F - var7 * 4.0F + var3 * 2.7F - var4 * 3.1F));
            float var11 = 0.72F + 0.18F * var9 + 0.1F * var10;
            float var12;
            if (currentTargetClosed) {
               float var13 = smoothstep(var2);
               float var14 = var8 * 0.82F + var6 * 0.07F + 0.05F * (0.5F + 0.5F * (float)Math.sin((double)(var7 * 5.0F)));
               float var15 = var13 * 1.1F;
               float var16 = smoothstep((var15 - var14 + 0.1F) / 0.2F);
               float var17 = Math.abs(var15 - var14);
               float var18 = 1.0F - clamp01(var17 / 0.16F);
               float var19 = currentTransitionPhase;
               float var20 = 0.0F;
               if (var19 < 0.16F) {
                  float var21 = 1.0F - var19 / 0.16F;
                  var20 = var21 * (0.12F + 0.13F * var10);
               }

               var12 = Math.max(var20, var16 * var11 + var18 * 0.22F);
            } else {
               float var23 = clamp01(var2);
               float var24 = var8 * 0.9F + 0.06F * var9 + 0.04F * (0.5F + 0.5F * (float)Math.sin((double)(var7 * 9.0F)));
               float var25 = smoothstep((var23 - var24 + 0.14F) / 0.18F);
               float var26 = 1.0F - var23;
               float var27 = 1.0F - var26 * (0.18F + 0.34F * var10);
               float var28 = 1.0F - clamp01(Math.abs(var23 - var24) / 0.13F);
               float var29 = var28 * (0.16F + 0.18F * var9);
               var12 = var25 * var27 + var29;
            }

            return var1 * clamp01(var12);
         }
      } else {
         return 0.0F;
      }
   }

   private static float interpolate(PegasusShieldTransitionHelper.Transition var0, long var1) {
      float var3 = var0.targetClosed ? 1.0F : 0.0F;
      float var4 = Math.abs(var3 - var0.startReveal);
      if (var4 <= 1.0E-4F) {
         return var3;
      } else {
         long var5 = var0.targetClosed ? 1776000000L : 1829000000L;
         long var7 = Math.max(120000000L, (long)((float)var5 * var4));
         long var9 = var1 - var0.startedAt;
         if (var9 <= 0L) {
            return var0.startReveal;
         } else if (var9 >= var7) {
            return var3;
         } else {
            float var11 = (float)var9 / (float)var7;
            float var12 = smootherstep(var11);
            return var0.startReveal + (var3 - var0.startReveal) * var12;
         }
      }
   }

   private static float transitionPhase(PegasusShieldTransitionHelper.Transition var0, long var1) {
      float var3 = var0.targetClosed ? 1.0F : 0.0F;
      float var4 = Math.abs(var3 - var0.startReveal);
      if (var4 <= 1.0E-4F) {
         return 1.0F;
      } else {
         long var5 = var0.targetClosed ? 1776000000L : 1829000000L;
         long var7 = Math.max(120000000L, (long)((float)var5 * var4));
         return clamp01((float)(var1 - var0.startedAt) / (float)var7);
      }
   }

   private static float noise01(float var0, float var1, float var2) {
      double var3 = Math.sin((double)var0 * 12.9898 + (double)var1 * 78.233 + (double)var2 * 37.719) * 43758.5453;
      return (float)(var3 - Math.floor(var3));
   }

   private static boolean readClosed(Object var0, float var1) {
      if (var0 != null) {
         try {
            Method var2 = var0.getClass().getMethod("irisInfo");
            Object var3 = var2.invoke(var0);
            if (var3 != null) {
               Object motion = var3.getClass().getMethod("getIrisMotion").invoke(var3);
               if (Boolean.TRUE.equals(motion.getClass().getMethod("isClosing").invoke(motion))) return true;
               if (Boolean.TRUE.equals(motion.getClass().getMethod("isOpening").invoke(motion))) return false;
               Method var4 = var3.getClass().getMethod("isIrisClosed");
               Object var5 = var4.invoke(var3);
               if (var5 instanceof Boolean) {
                  return (Boolean)var5;
               }
            }
         } catch (Throwable var6) {
         }
      }

      return var1 >= 57.5F;
   }

   private static float smoothstep(float var0) {
      float var1 = clamp01(var0);
      return var1 * var1 * (3.0F - 2.0F * var1);
   }

   private static float smootherstep(float var0) {
      float var1 = clamp01(var0);
      return var1 * var1 * var1 * (var1 * (var1 * 6.0F - 15.0F) + 10.0F);
   }

   private static float clamp01(float var0) {
      return var0 < 0.0F ? 0.0F : Math.min(var0, 1.0F);
   }

   private static final class Transition {
      private boolean targetClosed;
      private float startReveal;
      private float currentReveal;
      private long startedAt;

      private Transition(boolean var1, float var2, float var3, long var4) {
         this.targetClosed = var1;
         this.startReveal = var2;
         this.currentReveal = var3;
         this.startedAt = var4;
      }
   }
}
