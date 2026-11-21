package com.mustangdoc.sgjpatch;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegisterEvent;

@Mod(AtlantisNamespace.ID)
public final class AtlantisNamespace {
    public static final String ID = "stargate_atlantis";
    private static final String LEGACY = "sgjadditions_capacity_patch";

    @Mod.EventBusSubscriber(modid = LEGACY, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class RegistryAliases {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void register(RegisterEvent event) {
            if (!event.getRegistryKey().equals(Registries.BLOCK)
                    && !event.getRegistryKey().equals(Registries.ITEM)
                    && !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
            if (!(event.getForgeRegistry() instanceof ForgeRegistry<?> registry))
                throw new IllegalStateException("Expected a Forge registry for " + event.getRegistryKey());
            for (ResourceLocation target : registry.getKeys()) {
                if (!target.getNamespace().equals(ID)) continue;
                ResourceLocation source = new ResourceLocation(LEGACY, target.getPath());
                if (!registry.getKeys().contains(source)) registry.addAlias(source, target);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = LEGACY, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class SavedMappings {
        @SubscribeEvent
        public static void remap(MissingMappingsEvent event) {
            remap(event, ForgeRegistries.Keys.BLOCKS, ForgeRegistries.BLOCKS);
            remap(event, ForgeRegistries.Keys.ITEMS, ForgeRegistries.ITEMS);
            remap(event, ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, ForgeRegistries.BLOCK_ENTITY_TYPES);
        }

        private static <T> void remap(MissingMappingsEvent event, ResourceKey<? extends Registry<T>> key,
                IForgeRegistry<T> registry) {
            for (MissingMappingsEvent.Mapping<T> mapping : event.getMappings(key, LEGACY)) {
                ResourceLocation target = new ResourceLocation(ID, mapping.getKey().getPath());
                if (registry.containsKey(target)) mapping.remap(registry.getValue(target));
            }
        }
    }
}
