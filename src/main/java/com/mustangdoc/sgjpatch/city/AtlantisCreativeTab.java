package com.mustangdoc.sgjpatch.city;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** One home for every Atlantis building and device item in this mod. */
@EventBusSubscriber(modid = "sgjatlantis_dhd", bus = EventBusSubscriber.Bus.MOD)
public final class AtlantisCreativeTab {
    private AtlantisCreativeTab() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(
                ResourceLocation.fromNamespaceAndPath("sgjatlantis_dhd", "sgj_atlantis"),
                CreativeModeTab.builder()
                        .title(Component.literal("SGJ Atlantis"))
                        .icon(() -> new ItemStack(BuiltInRegistries.ITEM.get(
                                ResourceLocation.fromNamespaceAndPath("sgjatlantis_dhd", "atlantis_dhd"))))
                        .displayItems((parameters, output) -> BuiltInRegistries.ITEM.entrySet().stream()
                                .filter(entry -> {
                                    String namespace = entry.getKey().location().getNamespace();
                                    if (namespace.equals("stargate_atlantis")
                                            && entry.getKey().location().getPath().equals("triple_zpm_hub")
                                            && net.neoforged.fml.ModList.get().isLoaded("sgjourney_mobs"))
                                        return false;
                                    return namespace.equals("stargate_atlantis") || namespace.equals("sgjatlantis_dhd")
                                            || namespace.equals("sgjadditions_capacity_patch");
                                })
                                .map(java.util.Map.Entry::getValue)
                                .filter(item -> item != net.minecraft.world.item.Items.AIR)
                                .forEach(output::accept))
                        .build()));
    }
}
