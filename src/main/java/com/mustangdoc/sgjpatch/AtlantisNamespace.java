package com.mustangdoc.sgjpatch;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(AtlantisNamespace.ID)
public final class AtlantisNamespace {
    public static final String ID = "stargate_atlantis";

    @EventBusSubscriber(modid = SGJAdditionsCapacityPatch.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static final class LegacyAliases {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void register(RegisterEvent event) {
            if (!event.getRegistryKey().equals(Registries.BLOCK)
                    && !event.getRegistryKey().equals(Registries.ITEM)
                    && !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
            addAliases(event.getRegistry());
        }
    }

    static void addAliases(Registry<?> registry) {
        for (ResourceLocation target : registry.keySet()) {
            if (!target.getNamespace().equals(ID)) continue;
            for (String legacy : new String[]{"sgjatlantis_dhd", "sgjadditions_capacity_patch"}) {
                ResourceLocation source = ResourceLocation.fromNamespaceAndPath(legacy, target.getPath());
                if (!registry.keySet().contains(source)) registry.addAlias(source, target);
            }
        }
    }
}
