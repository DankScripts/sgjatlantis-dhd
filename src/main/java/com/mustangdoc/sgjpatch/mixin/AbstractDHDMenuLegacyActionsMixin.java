package com.mustangdoc.sgjpatch.mixin;

import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.init.PacketHandlerInit;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;
import net.povstalec.sgjourney.common.packets.ServerboundDHDUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AbstractDHDMenu.class, remap = false)
public abstract class AbstractDHDMenuLegacyActionsMixin {
	@Unique
	public void engageStargate() {
		sgjpatch$sendDhdUpdate(-1);
	}

	@Unique
	public void encodeSymbol(int symbol) {
		sgjpatch$sendDhdUpdate(symbol);
	}

	@Unique
	private void sgjpatch$sendDhdUpdate(int symbol) {
		if (((AbstractDHDMenuAccessor) this).sgjpatch$getBlockEntity() instanceof AbstractDHDEntity dhd) {
			PacketHandlerInit.INSTANCE.sendToServer(new ServerboundDHDUpdatePacket(dhd.getBlockPos(), symbol));
		}
	}
}