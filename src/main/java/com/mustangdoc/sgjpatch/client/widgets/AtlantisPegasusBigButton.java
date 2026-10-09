package com.mustangdoc.sgjpatch.client.widgets;

import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceLocation;
import net.povstalec.sgjourney.client.widgets.dhd.DHDBigButton;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;

public final class AtlantisPegasusBigButton extends DHDBigButton<AbstractDHDMenu<?>> {
	private static final ResourceLocation WIDGETS =
			ResourceLocation.fromNamespaceAndPath("sgjourney", "textures/gui/pegasus_dhd_widgets.png");

	public AtlantisPegasusBigButton(int x, int y, AbstractDHDMenu<?> menu, Button.OnPress onPress) {
		super(x, y, menu, onPress, WIDGETS);
	}
}