package com.mustangdoc.sgjpatch.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SGJPatchSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "sgjadditions_capacity_patch");
    public static final RegistryObject<SoundEvent> PEGASUS_SHIELD_OPENING = register("pegasus_shield_opening");
    public static final RegistryObject<SoundEvent> PEGASUS_SHIELD_CLOSING = register("pegasus_shield_closing");
    public static final RegistryObject<SoundEvent> PEGASUS_SHIELD_HIT = register("pegasus_shield_hit");
    public static final RegistryObject<SoundEvent> PEGASUS_SHIELD_HUMMING = register("pegasus_shield_humming");

    private SGJPatchSounds() {}

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("sgjadditions_capacity_patch", name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
