package com.mustangdoc.sgjpatch.util;

import net.minecraft.resources.ResourceLocation;

public final class SGJPatchOpenContainerTrace {
   public static final ThreadLocal<SGJPatchOpenContainerTrace.Trace> TRACE = new ThreadLocal<>();

   private SGJPatchOpenContainerTrace() {
   }

   public static final class Trace {
      public final ResourceLocation menuKey;
      public final int windowId;
      public final int additionalDataBytes;

      public Trace(ResourceLocation menuKey, int windowId, int additionalDataBytes) {
         this.menuKey = menuKey;
         this.windowId = windowId;
         this.additionalDataBytes = additionalDataBytes;
      }
   }
}
