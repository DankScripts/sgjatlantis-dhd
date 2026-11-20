package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.standalone.AtlantisBlockEntityDHD;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import net.povstalec.sgjourney.common.sgjourney.dhd.SGJourneyBlockEntityDHD;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AtlantisDHDEntity.class, remap = false)
public abstract class AtlantisDHDEntityDhdApiMixin {
    @Unique
    protected SGJourneyBlockEntityDHD fetchDHD() {
        return new AtlantisBlockEntityDHD((AtlantisDHDEntity) (Object) this);
    }
}
