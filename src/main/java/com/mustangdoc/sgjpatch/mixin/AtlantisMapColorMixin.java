package com.mustangdoc.sgjpatch.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies vanilla-map/minimap colors for Atlantis blocks that otherwise report MapColor.NONE. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AtlantisMapColorMixin {
    @Inject(method = "getMapColor", at = @At("RETURN"), cancellable = true)
    private void sgjpatch$atlantisMapColor(BlockGetter level, BlockPos pos,
            CallbackInfoReturnable<MapColor> callback) {
        if (callback.getReturnValue() != MapColor.NONE) return;
        BlockState state = (BlockState) (Object) this;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String namespace = id.getNamespace();
        if (!namespace.equals("sgjadditions_capacity_patch")
                && !namespace.equals("sgjatlantis_dhd")
                && !namespace.equals("sgjatlantis_cityshield")) return;
        callback.setReturnValue(colorFor(propertyValue(state, "finish") + "_" + id.getPath()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyValue(BlockState state, String name) {
        for (Property property : state.getProperties()) {
            if (property.getName().equals(name)) return property.getName(state.getValue(property));
        }
        return "";
    }

    private static MapColor colorFor(String name) {
        if (name.contains("charcoal")) return MapColor.COLOR_GRAY;
        if (name.contains("blue_grey")) return MapColor.TERRACOTTA_LIGHT_BLUE;
        if (name.contains("stair_grey") || name.contains("silver")) return MapColor.COLOR_LIGHT_GRAY;
        if (name.contains("bronze") || name.contains("copper")) return MapColor.TERRACOTTA_ORANGE;
        if (name.contains("lightblue") || name.contains("light_blue")) return MapColor.COLOR_LIGHT_BLUE;
        if (name.contains("teal") || name.contains("cyan")) return MapColor.COLOR_CYAN;
        if (name.contains("brown")) return MapColor.COLOR_BROWN;
        if (name.contains("green")) return MapColor.COLOR_GREEN;
        if (name.contains("red")) return MapColor.COLOR_RED;
        if (name.contains("blue")) return MapColor.COLOR_BLUE;
        return MapColor.METAL;
    }
}
