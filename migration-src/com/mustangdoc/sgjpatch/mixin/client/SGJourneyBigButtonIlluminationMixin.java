package com.mustangdoc.sgjpatch.mixin.client;

import com.mustangdoc.sgjpatch.mixin.AbstractDHDEntityAccessor;
import com.mustangdoc.sgjpatch.mixin.AbstractDHDMenuAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.povstalec.sgjourney.client.widgets.dhd.DHDBigButton;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.misc.CoordinateHelper;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;
import net.povstalec.sgjourney.common.menu.dhd.IDHDMenu;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DHDBigButton.class, remap = false)
public abstract class SGJourneyBigButtonIlluminationMixin {
	private static final String ATLANTIS_MENU_CLASS = "com.mustangdoc.sgjpatch.menu.AtlantisDHDCompatMenu";

	@Shadow
	public IDHDMenu menu;

	@Inject(method = "getYImage", at = @At("HEAD"), cancellable = true, remap = false)
	private void sgjpatch$useIrisIlluminationForAtlantis(boolean hovered, CallbackInfoReturnable<Integer> cir) {
		if (menu == null) {
			return;
		}
		if (!ATLANTIS_MENU_CLASS.equals(menu.getClass().getName())) {
			return;
		}

		// Atlantis: outer ring is always illuminated.
		// Full illumination only when the linked stargate has an iris and it's closed.
		boolean irisClosed = false;
		try {
			if (menu instanceof AbstractDHDMenuAccessor accessor) {
				if (accessor.sgjpatch$getBlockEntity() instanceof AbstractDHDEntity dhd) {
					Level level = dhd.getLevel();
					if (level != null) {
						BlockPos dhdPos = dhd.getBlockPos();
						Vec3i rel = ((AbstractDHDEntityAccessor) dhd).sgjpatch$getStargateRelativePos();
						if (rel != null) {
							BlockPos gatePos = CoordinateHelper.Relative.getOffsetPos(dhd.getDirection(), dhdPos, rel);
							if (gatePos != null) {
								BlockEntity gateBe = level.getBlockEntity(gatePos);
								if (gateBe instanceof AbstractStargateEntity gate && gate instanceof IrisInfo.Interface irisHolder) {
									IrisInfo iris = irisHolder.irisInfo();
									irisClosed = iris != null && iris.hasIris() && iris.isIrisClosed();
								}
							}
						}
					}
				}
			}
		} catch (Throwable ignored) {
		}

		if (irisClosed) {
			cir.setReturnValue(hovered ? 3 : 2);
			return;
		}

		cir.setReturnValue(1);
	}
}
