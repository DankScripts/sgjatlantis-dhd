package com.mustangdoc.sgjpatch.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.povstalec.sgjourney.common.init.PacketHandlerInit;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;
import net.povstalec.sgjourney.common.packets.ServerboundDHDUpdatePacket;

/** Keeps the legacy table controls on SGJourney's current client packet API. */
public final class AtlantisEngageCloseHelper {
    private AtlantisEngageCloseHelper() {}

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    public static void press(Object button) {
        try {
            Object screen = field(button, "screen");
            if ((boolean) field(screen, "editMode")) {
                Method normal = button.getClass().getDeclaredMethod("sgjpatch$pressNormal");
                normal.setAccessible(true);
                normal.invoke(button);
                return;
            }
            int symbol = (int) field(button, "actionSymbol");
            if (symbol < -1 || symbol > 38) return;
            AbstractDHDMenu<?> menu = (AbstractDHDMenu<?>) field(button, "menu");
            PacketHandlerInit.INSTANCE.sendToServer(new ServerboundDHDUpdatePacket(
                    ((BlockEntity) menu.blockEntity).getBlockPos(), symbol));
            if (symbol >= 0) {
                ((boolean[]) field(screen, "engagedLatch"))[symbol] = true;
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to process Atlantis DHD button press", e);
        }
    }
}
