package com.mustangdoc.sgjpatch.standalone;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.povstalec.sgjourney.api.stargate.BlockEntityStargate;
import net.povstalec.sgjourney.api.stargate.DHDControlledStargate;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;

public final class AtlantisDHDApi {
    private AtlantisDHDApi() {}

    public static DHDControlledStargate connectedStargate(AbstractDHDEntity dhd) {
        var adapter = dhd.getDHD();
        if (adapter == null) {
            return null;
        }
        return adapter.getStargate();
    }

    public static AbstractStargateEntity<?> connectedStargateEntity(AbstractDHDEntity dhd) {
        DHDControlledStargate stargate = connectedStargate(dhd);
        if (stargate instanceof BlockEntityStargate<?> blockEntityStargate) {
            BlockEntity blockEntity = blockEntityStargate.getStargateBlockEntity();
            if (blockEntity instanceof AbstractStargateEntity<?> entity) {
                return entity;
            }
        }
        return null;
    }
}
