package com.mustangdoc.sgjpatch.mixin;

import java.util.function.Predicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NbtUtils.class)
public abstract class AtlantisStructureBlockMigrationMixin {
    @Inject(method = "readBlockState", at = @At("HEAD"), cancellable = true)
    private static void sgjpatch$resolveLegacyBlock(HolderGetter<Block> lookup, CompoundTag tag,
            CallbackInfoReturnable<BlockState> callback) {
        CompoundTag migrated = sgjpatch$migrateBlock(tag, ForgeRegistries.BLOCKS::containsKey);
        if (migrated != null) callback.setReturnValue(NbtUtils.readBlockState(lookup, migrated));
    }

    @Unique
    private static CompoundTag sgjpatch$migrateBlock(CompoundTag tag, Predicate<ResourceLocation> exists) {
        ResourceLocation old = ResourceLocation.tryParse(tag.getString("Name"));
        if (old == null || !old.getNamespace().equals("sgjadditions_capacity_patch")) return null;
        ResourceLocation target = new ResourceLocation("stargate_atlantis", old.getPath());
        if (!exists.test(target)) return null;
        CompoundTag migrated = tag.copy();
        migrated.putString("Name", target.toString());
        return migrated;
    }
}
