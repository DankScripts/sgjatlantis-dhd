package com.mustangdoc.sgjpatch.init;

import com.mustangdoc.sgjpatch.block.AtlantisDHDBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SGJPatchBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "stargate_atlantis");
    public static final RegistryObject<AtlantisDHDBlock> ATLANTIS_DHD =
            BLOCKS.register("atlantis_dhd",
                    () -> new AtlantisDHDBlock(BlockBehaviour.Properties.of().strength(3.5F).noOcclusion()));

    private SGJPatchBlocks() {}

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
