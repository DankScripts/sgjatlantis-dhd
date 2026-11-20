package com.mustangdoc.sgjpatch.city;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

/** One home for every Atlantis building and device item in this mod. */
@Mod.EventBusSubscriber(modid = AtlantisBuildingBlocks.NS, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AtlantisCreativeTab {
    private AtlantisCreativeTab() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(
                new ResourceLocation(AtlantisBuildingBlocks.NS, "sgj_atlantis"),
                CreativeModeTab.builder()
                        .title(Component.literal("SGJ Atlantis"))
                        .icon(() -> new ItemStack(BuiltInRegistries.ITEM.get(
                                new ResourceLocation(AtlantisBuildingBlocks.NS, "atlantis_dhd"))))
                        .displayItems((parameters, output) -> BuiltInRegistries.ITEM.entrySet().stream()
                                .filter(entry -> entry.getKey().location().getNamespace()
                                        .equals(AtlantisBuildingBlocks.NS))
                                .map(java.util.Map.Entry::getValue)
                                .filter(item -> item != net.minecraft.world.item.Items.AIR)
                                .forEach(output::accept))
                        .build()));
    }
}
