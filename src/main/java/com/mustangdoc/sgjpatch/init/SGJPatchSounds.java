package com.mustangdoc.sgjpatch.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SGJPatchSounds {
   private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, "sgjatlantis_dhd");
   public static final DeferredHolder<SoundEvent, SoundEvent> OPENING = register("pegasus_shield_opening");
   public static final DeferredHolder<SoundEvent, SoundEvent> CLOSING = register("pegasus_shield_closing");
   public static final DeferredHolder<SoundEvent, SoundEvent> HIT = register("pegasus_shield_hit");
   public static final DeferredHolder<SoundEvent, SoundEvent> HUMMING = register("pegasus_shield_humming");

   private SGJPatchSounds() {
   }

   private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
      return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("sgjatlantis_dhd", name)));
   }

   public static void register(IEventBus bus) {
      SOUNDS.register(bus);
   }
}
