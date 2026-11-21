package com.mustangdoc.sgjpatch.client.render;

import java.util.Map;
import java.util.WeakHashMap;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo.Interface;

public final class PegasusShieldTransitionHelper {
   private static final Map<Interface, PegasusShieldTransitionHelper.Transition> STATES = new WeakHashMap<>();

   private PegasusShieldTransitionHelper() {
   }

   public static PegasusShieldTransitionHelper.Transition update(Interface gate) {
      boolean closed = gate.irisInfo().getIrisMotion().isClosing()
         || (!gate.irisInfo().getIrisMotion().isOpening() && gate.irisInfo().isIrisClosed());
      long now = System.nanoTime();
      PegasusShieldTransitionHelper.Transition transition = STATES.computeIfAbsent(gate, ignored -> new PegasusShieldTransitionHelper.Transition(closed, now));
      transition.advance(closed, now);
      return transition;
   }

   private static float smooth(float value) {
      float clamped = clamp(value);
      return clamped * clamped * (3.0F - 2.0F * clamped);
   }

   private static float clamp(float value) {
      return Math.max(0.0F, Math.min(value, 1.0F));
   }

   public static final class Transition {
      private boolean closed;
      private float startReveal;
      private float reveal;
      private float phase;
      private float timeSeconds;
      private long startedAt;

      private Transition(boolean closed, long now) {
         this.closed = closed;
         this.startReveal = this.reveal = closed ? 1.0F : 0.0F;
         this.startedAt = now;
      }

      private void advance(boolean targetClosed, long now) {
         this.interpolate(now);
         if (this.closed != targetClosed) {
            // Begin a visible energy flash on the first closing frame.
            this.startReveal = targetClosed ? Math.max(this.reveal, 0.08F) : this.reveal;
            this.reveal = this.startReveal;
            this.closed = targetClosed;
            this.startedAt = now;
            this.interpolate(now);
         }

         this.timeSeconds = (float)((double)now * 1.0E-9);
      }

      private void interpolate(long now) {
         float target = this.closed ? 1.0F : 0.0F;
         float distance = Math.abs(target - this.startReveal);
         long duration = Math.max(120000000L, (long)((float)(this.closed ? 1776000000L : 1829000000L) * distance));
         this.phase = distance <= 1.0E-4F ? 1.0F : PegasusShieldTransitionHelper.clamp((float)(now - this.startedAt) / (float)duration);
         float eased = this.phase * this.phase * this.phase * (this.phase * (this.phase * 6.0F - 15.0F) + 10.0F);
         this.reveal = this.startReveal + (target - this.startReveal) * eased;
      }

      public float reveal() {
         return this.reveal;
      }

      public float vertexAlpha(float[] position) {
         if (this.reveal <= 0.0F) {
            return 0.0F;
         } else if (this.reveal >= 0.9995F && this.closed) {
            return 0.9F;
         } else {
            float horizontal = position[0];
            float vertical = position[1];
            float depth = position[2];
            float radius = PegasusShieldTransitionHelper.clamp((float)Math.hypot((double)horizontal, (double)vertical) / 4.02F);
            float angle = (float)Math.atan2((double)vertical, (double)horizontal);
            double noiseValue = Math.sin((double)horizontal * 12.9898 + (double)vertical * 78.233 + (double)depth * 37.719) * 43758.5453;
            float noise = (float)(noiseValue - Math.floor(noiseValue));
            float wave = 0.5F + 0.5F * (float)Math.sin((double)(this.timeSeconds * 13.0F + angle * 7.0F + radius * 10.0F));
            float flicker = 0.5F + 0.5F * (float)Math.sin((double)(this.timeSeconds * 21.0F - angle * 4.0F + horizontal * 2.7F - vertical * 3.1F));
            float opacity;
            if (this.closed) {
               float threshold = noise * 0.82F + radius * 0.07F + 0.05F * (0.5F + 0.5F * (float)Math.sin((double)(angle * 5.0F)));
               float front = PegasusShieldTransitionHelper.smooth(this.reveal) * 1.1F;
               float fill = PegasusShieldTransitionHelper.smooth((front - threshold + 0.1F) / 0.2F);
               float edge = 1.0F - PegasusShieldTransitionHelper.clamp(Math.abs(front - threshold) / 0.16F);
               float flash = this.phase < 0.16F ? (1.0F - this.phase / 0.16F) * (0.12F + 0.13F * flicker) : 0.0F;
               opacity = Math.max(flash, fill * (0.72F + 0.18F * wave + 0.1F * flicker) + edge * 0.22F);
            } else {
               float threshold = noise * 0.9F + 0.06F * wave + 0.04F * (0.5F + 0.5F * (float)Math.sin((double)(angle * 9.0F)));
               float fill = PegasusShieldTransitionHelper.smooth((this.reveal - threshold + 0.14F) / 0.18F);
               float fade = 1.0F - (1.0F - this.reveal) * (0.18F + 0.34F * flicker);
               float edge = 1.0F - PegasusShieldTransitionHelper.clamp(Math.abs(this.reveal - threshold) / 0.13F);
               opacity = fill * fade + edge * (0.16F + 0.18F * wave);
            }

            return 0.9F * PegasusShieldTransitionHelper.clamp(opacity);
         }
      }
   }
}
