package com.mustangdoc.sgjpatch.client.util;

import com.mustangdoc.sgjpatch.compat.LanteaJumperScreen.KeyShape;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;

/** Repositions the legacy SGJourney buttons while retaining their renderer and actions. */
@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch", value = Dist.CLIENT)
public final class AtlantisJourneyKeypadLayout {
    private static final String SCREEN = "com.mustangdoc.sgjpatch.client.screens.AtlantisDHDScreenFixed";

    private AtlantisJourneyKeypadLayout() {}

    private static Field field(Object owner, String name) throws ReflectiveOperationException {
        for (Class<?> type=owner.getClass();type!=null;type=type.getSuperclass()) {
            try {
                Field field=type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }

    private static int origin(Object screen, String mapped, String legacy) throws ReflectiveOperationException {
        try { return field(screen,mapped).getInt(screen); }
        catch (NoSuchFieldException ignored) { return field(screen,legacy).getInt(screen); }
    }

    @SubscribeEvent
    public static void align(ScreenEvent.Init.Post event) {
        if (!event.getScreen().getClass().getName().equals(SCREEN)) return;
        // SGJourney 0.6.52 uses SGJourneyMenuScreen, which is NOT an AbstractContainerScreen.
        Object screen = event.getScreen();
        try {
            int left=origin(screen,"leftPos","f_97735_"), top=origin(screen,"topPos","f_97736_");
            @SuppressWarnings("unchecked")
            List<AbstractButton> oldButtons = (List<AbstractButton>) field(screen, "dialerButtons").get(screen);
            List<AlignedButton> replacements = new ArrayList<>();
            Set<Integer> symbols = new HashSet<>();
            // Validate everything before modifying the screen, including after window resize.
            for (AbstractButton original : oldButtons) {
                int symbol = field(original, "actionSymbol").getInt(original);
                if (!symbols.add(symbol)) throw new IllegalStateException("Duplicate DHD symbol " + symbol);
                KeyShape base = AtlantisReferencePlate.keyShape(symbol); KeyShape shape = new KeyShape(base.x()-46,base.y(),base.width(),base.height(),base.up());
                replacements.add(new AlignedButton(original, oldButtons, shape, symbol,
                        left, top, field(original, "up"),
                        field(original, "size").getInt(original), replacements.isEmpty()));
            }
            if (replacements.size() != 40) throw new IllegalStateException("Expected 40 DHD controls");
            AbstractButton miniGate = (AbstractButton) field(screen, "circleButton").get(screen);
            if (miniGate != null) {
                miniGate.setX(miniGate.getX() + 14);
                miniGate.setY(miniGate.getY() - 18);
                Field baseX = field(screen, "circleBaseX");
                baseX.setInt(screen, baseX.getInt(screen) + 14);
                Field baseY = field(screen, "circleBaseY");
                baseY.setInt(screen, baseY.getInt(screen) - 18);
            }
            for (AlignedButton replacement : replacements) event.removeListener(replacement.original);
            // The original screen's manual hit loop must not catch clicks at the old positions.
            oldButtons.clear();
            field(screen, "centerButton").set(screen, null);
            for (AlignedButton replacement : replacements) event.addListener(replacement);
            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof AbstractDHDMenu<?> menu)
                AtlantisAddressGuide.begin(menu.getDHD());
            System.out.println("[SGJPATCH] SGJourney reference keypad installed: 40 aligned controls and bronze plate.");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to align the SGJourney Atlantis keypad", e);
        }
    }

    @SubscribeEvent
    public static void paste(ScreenEvent.KeyPressed.Pre event) {
        if (!event.getScreen().getClass().getName().equals(SCREEN) || event.getKeyCode()!=86
                || !net.minecraft.client.gui.screens.Screen.hasControlDown()) return;
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof AbstractDHDMenu<?> menu) {
            AtlantisAddressGuide.paste(menu.getDHD());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void recentClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!event.getScreen().getClass().getName().equals(SCREEN) || event.getButton()!=0) return;
        if (Minecraft.getInstance().player == null || !(Minecraft.getInstance().player.containerMenu instanceof AbstractDHDMenu<?> menu)) return;
        try {
            int left=origin(event.getScreen(),"leftPos","f_97735_");
            int top=origin(event.getScreen(),"topPos","f_97736_");
            if (AtlantisRecentAddresses.click(menu.getDHD(),left,top,event.getMouseX(),event.getMouseY())) event.setCanceled(true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to read Atlantis DHD position",e);
        }
    }

    private static final class AlignedButton extends AbstractButton {
        private final AbstractButton original;
        private final List<AbstractButton> legacyHoverButtons;
        private final KeyShape shape;
        private final int left, top, originalSize, symbol;
        private final Field orientation;
        private final boolean drawBackplate;

        private AlignedButton(AbstractButton original, List<AbstractButton> legacyHoverButtons,
                              KeyShape shape, int symbol, int left, int top, Field orientation, int originalSize, boolean drawBackplate) {
            super(left + (int) Math.floor(shape.x()), top + (int) Math.floor(shape.y()),
                    (int) Math.ceil(shape.width()) + 1, (int) Math.ceil(shape.height()) + 1,
                    original.getMessage());
            this.original = original;
            this.legacyHoverButtons = legacyHoverButtons;
            this.shape = shape;
            this.symbol = symbol;
            this.left = left;
            this.top = top;
            this.orientation = orientation;
            this.originalSize = originalSize;
            this.drawBackplate = drawBackplate;
            setTooltip(original.getTooltip());
        }

        @Override public boolean isMouseOver(double x, double y) {
            return visible && shape.contains(x - left, y - top);
        }

        @Override protected boolean clicked(double x, double y) {
            return active && isMouseOver(x, y);
        }

        @Override public void onPress() {
            original.onPress();
        }

        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (drawBackplate) {
                var background = new net.minecraft.resources.ResourceLocation("sgjpatch", "textures/gui/atlantis_dhd_clean.png");
                graphics.blit(background, left+83, top+198, 283, 198, 170, 69, 720, 360);
                var bar = new net.minecraft.resources.ResourceLocation("sgjpatch", "textures/gui/atlantis_dhd_control_bar.png");
                for (int barX : new int[]{66, 148}) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(left+barX, top+252, 0);
                    graphics.pose().scale(68F/52F, 26F/18F, 1F);
                    graphics.blit(bar, 0, 0, 0, 0, 52, 18, 52, 18);
                    graphics.pose().popPose();
                }
                AtlantisKeypadSurface.backplate(graphics, left-46, top);
                var player=net.minecraft.client.Minecraft.getInstance().player;
                if(player!=null && player.containerMenu instanceof net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu<?> menu)
                    AtlantisRecentAddresses.render(graphics,menu.getDHD(),left,top,mouseX,mouseY);
            }
            double x = left + shape.x(), y = top + shape.y();
            double scaleX = shape.width() / originalSize, scaleY = shape.height() / originalSize;
            boolean hover = isMouseOver(mouseX, mouseY);
            var player=Minecraft.getInstance().player;
            if (player!=null && player.containerMenu instanceof AbstractDHDMenu<?> menu)
                hover |= AtlantisAddressGuide.highlights(menu.getDHD(),symbol);
            // The legacy renderer chooses its own textures, glyph tint, and engagement overlay.
            // Only its coordinate system changes; no replacement palette or shader tint is used.
            int localMouseX = hover ? original.getX() + originalSize / 2 : Integer.MIN_VALUE / 2;
            int localMouseY = hover ? original.getY() + originalSize / 2 : Integer.MIN_VALUE / 2;
            graphics.flush();
            graphics.pose().pushPose();
            try {
                orientation.setBoolean(original, shape.up());
                graphics.pose().translate(x, y, 0);
                graphics.pose().scale((float) scaleX, (float) scaleY, 1);
                graphics.pose().translate(-original.getX(), -original.getY(), 0);
                // The legacy hover helper checks this list. Scope it to the button being drawn;
                // leave it empty outside rendering so obsolete click areas cannot be selected.
                legacyHoverButtons.add(original);
                original.render(graphics, localMouseX, localMouseY, partialTick);
                graphics.flush();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to orient an Atlantis key", e);
            } finally {
                legacyHoverButtons.clear();
                graphics.pose().popPose();
            }
        }

        @Override protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
