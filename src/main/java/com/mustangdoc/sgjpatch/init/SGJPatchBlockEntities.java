package com.mustangdoc.sgjpatch.init;

import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SGJPatchBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "stargate_atlantis");
    public static final RegistryObject<BlockEntityType<AtlantisDHDEntity>> ATLANTIS_DHD =
            BLOCK_ENTITIES.register("atlantis_dhd",
                    () -> BlockEntityType.Builder.of(AtlantisDHDEntity::new, SGJPatchBlocks.ATLANTIS_DHD.get()).build(null));

    private SGJPatchBlockEntities() {}

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
