package com.mustangdoc.sgjpatch.client.screens;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mustangdoc.sgjpatch.client.render.NativePegasusTextureHelper;
import com.mustangdoc.sgjpatch.client.util.AtlantisDebugConfig;
import com.mustangdoc.sgjpatch.client.util.AtlantisEditorHelper;
import com.mustangdoc.sgjpatch.client.util.AtlantisHoverNumberOverlay;
import com.mustangdoc.sgjpatch.client.util.LinkedMiniGate;
import com.mustangdoc.sgjpatch.client.util.AtlantisHoverPriorityHelper;
import com.mustangdoc.sgjpatch.client.util.AtlantisKeypadSurface;
import com.mustangdoc.sgjpatch.client.util.AtlantisLayoutDefaults;
import com.mustangdoc.sgjpatch.client.util.AtlantisTableControlEditor;
import com.mustangdoc.sgjpatch.client.util.AtlantisVisualHelper;
import com.mustangdoc.sgjpatch.client.util.ButtonScaleHelper;
import com.mustangdoc.sgjpatch.client.util.EditModeTextHelper;
import com.mustangdoc.sgjpatch.compat.AtlantisButtonHitHelper;
import com.mustangdoc.sgjpatch.compat.AtlantisEngageCloseHelper;
import com.mustangdoc.sgjpatch.mixin.AbstractDHDMenuAccessor;
import com.mustangdoc.sgjpatch.network.SGJPatchNetwork;
import com.mustangdoc.sgjpatch.network.ServerboundToggleIrisPacket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fml.loading.FMLPaths;
import net.povstalec.sgjourney.client.resourcepack.symbols.ClientSymbols;
import net.povstalec.sgjourney.client.screens.dhd.AbstractDHDScreen;
import net.povstalec.sgjourney.client.widgets.dhd.PegasusDHDBigButton;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.config.ClientDHDConfig;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;
import org.joml.Matrix4f;

public class AtlantisDHDScreenFixed extends AbstractDHDScreen<AbstractDHDMenu<?>> {
   private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("sgjpatch", "textures/gui/atlantis_dhd_clean.png");
   private static final ResourceLocation CRYSTAL_UP = ResourceLocation.fromNamespaceAndPath("sgjpatch", "textures/gui/atlantis_dhd_crystal_up.png");
   private static final ResourceLocation CRYSTAL_DOWN = ResourceLocation.fromNamespaceAndPath("sgjpatch", "textures/gui/atlantis_dhd_crystal_down.png");
   private static final ResourceLocation CRYSTAL_CENTER = ResourceLocation.fromNamespaceAndPath("sgjpatch", "textures/gui/atlantis_dhd_crystal_center.png");
   private static final int CIRCLE_EDIT_INDEX = 1000;
   private static final int DIALER_PANEL_X_OFFSET = 238;
   private static final int DIALER_PANEL_Y_OFFSET = 40;
   private static final int TRI_SIZE = 20;
   private static final int CENTER_TRI_SIZE = 10;
   private static final int CENTER_TRI_X_STEP = 5;
   private static final int CENTER_TRI_Y_STEP = 5;
   private static final int DIAMOND_X_STEP = 16;
   private static final int DIAMOND_Y_STEP = 14;
   private static final int PANEL_W = 168;
   private static final int PANEL_H = 160;
   private static final int EDIT_TOGGLE_KEY = 297;
   private static final int EDIT_FLIP_KEY = 84;
   private static final String OFFSETS_FILE_NAME = "sgjpatch-atlantis-dialer-offsets.json";
   private static final int[] ATLANTIS_SYMBOL_ORDER = new int[]{
      1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38
   };
   private boolean editMode = false;
   private int selectedIndex = -1;
   private final List<AtlantisDHDScreenFixed.AtlantisTriangleSymbolButton> dialerButtons = new ArrayList<>();
   private AtlantisDHDScreenFixed.DialerOffsets dialerOffsets;
   private final boolean[] engagedLatch = new boolean[39];
   private final LinkedMiniGate miniGate = new LinkedMiniGate();
   private AbstractButton circleButton;
   private AtlantisDHDScreenFixed.AtlantisTriangleCenterButton centerButton;
   private int circleBaseX;
   private int circleBaseY;
   private int centerGlowTicks = 0;

   public AtlantisDHDScreenFixed(AbstractDHDMenu<?> menu, Inventory inventory, Component title) {
      super(menu, inventory, title, BACKGROUND);
   }

   protected void init() {
      this.imageWidth = 720;
      this.imageHeight = 360;
      super.init();
      this.dialerButtons.clear();
      this.selectedIndex = -1;
      this.editMode = false;
      Arrays.fill(this.engagedLatch, false);
      this.circleButton = null;
      this.centerButton = null;
      this.dialerOffsets = AtlantisDHDScreenFixed.DialerOffsets.load();
      this.sgjpatch$addAtlantisTableLayout();
      this.sgjpatch$applyStateToButtons();
      AtlantisTableControlEditor.initialize(this);
   }

   public boolean sgjpatch$mouseClickedNormal(double mouseX, double mouseY, int button) {
      AtlantisTableControlEditor.mouseClicked(this, mouseX, mouseY, button, this.leftPos, this.topPos);
      if (AtlantisEditorHelper.selectConstellation(this, mouseX, mouseY, button)) {
         return true;
      } else if (this.editMode && this.circleButton != null && this.circleButton.isMouseOver(mouseX, mouseY)) {
         this.selectedIndex = 1000;
         return true;
      } else {
         if (!this.editMode && button == 0) {
            AbstractButton best = null;
            double bestDist = Double.MAX_VALUE;

            for (AtlantisDHDScreenFixed.AtlantisTriangleSymbolButton triangle : this.dialerButtons) {
               if (1 > 0 && triangle.isMouseOver(mouseX, mouseY)) {
                  double centerX = (double)triangle.getX() + (double)triangle.size / 2.0;
                  double centerY = (double)triangle.getY() + (double)triangle.size / 2.0;
                  double dx = mouseX - centerX;
                  double dy = mouseY - centerY;
                  double dist = dx * dx + dy * dy;
                  if (dist < bestDist) {
                     bestDist = dist;
                     best = triangle;
                  }
               }
            }

            if (this.centerButton != null && this.centerButton.isMouseOver(mouseX, mouseY)) {
               double centerX = (double)this.centerButton.getX() + 10.0;
               double centerY = (double)this.centerButton.getY() + 10.0;
               double dx = mouseX - centerX;
               double dy = mouseY - centerY;
               double dist = dx * dx + dy * dy;
               if (dist < bestDist) {
                  best = this.centerButton;
               }
            }

            if (best != null) {
               return best.mouseClicked(mouseX, mouseY, button);
            }
         }

         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public AbstractDHDMenu<?> getMenu() {
      return this.menu;
   }

   @Override
   public void tick() {
      super.tick();
      {
         AbstractDHDMenu<?> menu = this.getMenu();
         if (this.centerGlowTicks > 0) {
            this.centerGlowTicks--;
         }

         boolean anyServerEngaged = false;

         for (int i = 1; i <= 38; i++) {
            if (menu.isSymbolEngaged(i)) {
               this.engagedLatch[i] = true;
               anyServerEngaged = true;
            }
         }

         if (!anyServerEngaged && this.sgjpatch$hasAnyLatchedEngagement()) {
            this.sgjpatch$clearEngagedLatch();
         }
      }
   }

   public void onClose() {
      if (this.dialerOffsets != null) {
         this.dialerOffsets.save();
      }

      this.sgjpatch$clearEngagedLatch();
      this.centerGlowTicks = 0;
      if (Minecraft.getInstance().player != null) {
         Minecraft.getInstance().player.closeContainer();
      } else {
         super.onClose();
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      AtlantisTableControlEditor.keyPressed(this, keyCode, modifiers);
      if (keyCode == 297) {
         if (!AtlantisDebugConfig.editorEnabled()) {
            this.editMode = false;
            this.selectedIndex = -1;
            AtlantisEditorHelper.clear(this);
            return false;
         }

         AtlantisEditorHelper.clear(this);
      } else if (!AtlantisDebugConfig.editorEnabled()) {
         this.editMode = false;
         this.selectedIndex = -1;
         AtlantisEditorHelper.clear(this);
      } else if (this.editMode && AtlantisEditorHelper.handleConstellationKey(this, keyCode, modifiers)) {
         return true;
      }

      if (keyCode == 297) {
         this.editMode = !this.editMode;
         this.selectedIndex = -1;
         return true;
      } else {
         if (this.editMode && (this.selectedIndex == 1000 || this.selectedIndex >= 0 && this.selectedIndex < this.dialerButtons.size())) {
            if (keyCode == 84) {
               if (this.selectedIndex == 1000) {
                  return true;
               }

               AtlantisDHDScreenFixed.DialerOffsets.Offset offset = this.dialerOffsets.get(this.selectedIndex);
               offset.flip = !offset.flip;
               this.sgjpatch$applyStateToButtons();
               this.dialerOffsets.save();
               return true;
            }

            int step = 1;
            if ((modifiers & 1) != 0) {
               step = 5;
            } else if ((modifiers & 2) != 0) {
               step = 10;
            }

            int dx = 0;
            int dy = 0;
            switch (keyCode) {
               case 82:
                  this.dialerOffsets.set(this.selectedIndex, 0, 0);
                  if (this.selectedIndex != 1000) {
                     this.dialerOffsets.get(this.selectedIndex).flip = false;
                  }

                  this.sgjpatch$applyStateToButtons();
                  this.dialerOffsets.save();
                  return true;
               case 262:
                  dx = step;
                  break;
               case 263:
                  dx = -step;
                  break;
               case 264:
                  dy = step;
                  break;
               case 265:
                  dy = -step;
            }

            if (dx != 0 || dy != 0) {
               AtlantisDHDScreenFixed.DialerOffsets.Offset offset = this.dialerOffsets.get(this.selectedIndex);
               this.dialerOffsets.set(this.selectedIndex, offset.dx + dx, offset.dy + dy);
               this.sgjpatch$applyStateToButtons();
               this.dialerOffsets.save();
               return true;
            }
         }

         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public void sgjpatch$renderNormal(GuiGraphics var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      this.renderBg(var1, var4, var2, var3);
      RenderSystem.disableDepthTest();
      for (Renderable renderable : this.renderables) {
         renderable.render(var1, var2, var3, var4);
      }
      var1.drawCenteredString(this.font, ClientDHDConfig.dhd_symbols_numbers.get() ? SYMBOLS_TO_NUMBERS : NUMBERS_TO_SYMBOLS,
         this.leftPos + this.imageWidth / 2, this.topPos + this.imageHeight + 1, 0xFFFFFF);
      RenderSystem.enableDepthTest();
      AtlantisTableControlEditor.render(this, var1, this.leftPos, this.topPos);
      if (this.editMode && AtlantisDebugConfig.editorEnabled()) {
         int var5 = this.leftPos + 278;
         int var6 = this.topPos + this.imageHeight - 34;
         var1.drawString(Minecraft.getInstance().font, "DEBUG EDIT (F8): L-click button | R-click constellation", var5, var6, 16777215, false);
         var1.drawString(Minecraft.getInstance().font, "Arrows move | Shift=5 Ctrl=10 | R reset | T flips button", var5, var6 + 10, 16777215, false);
         String var7 = AtlantisEditorHelper.selectionStatus(this);
         if (var7 != null) {
            var1.drawString(Minecraft.getInstance().font, var7, var5, var6 + 20, 16777215, false);
         } else if (this.selectedIndex >= 0 && this.dialerOffsets != null) {
            AtlantisDHDScreenFixed.DialerOffsets.Offset var8 = this.dialerOffsets.get(this.selectedIndex);
            String var9 = EditModeTextHelper.selectedLabel(this.selectedIndex);
            var7 = EditModeTextHelper.selectedDetails(var9, var8.dx, var8.dy, var8.flip);
            var1.drawString(Minecraft.getInstance().font, var7, var5, var6 + 20, 16777215, false);
         }
      }

      AtlantisHoverNumberOverlay.renderHoveredNumber(this, var1, var2, var3);
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
   }

   private boolean sgjpatch$hasAnyLatchedEngagement() {
      for (int i = 0; i <= 38; i++) {
         if (this.engagedLatch[i]) {
            return true;
         }
      }

      return false;
   }

   private void sgjpatch$clearEngagedLatch() {
      Arrays.fill(this.engagedLatch, false);
   }

   private boolean sgjpatch$showLegacyNumbers() {
      return hasShiftDown();
   }

   private void sgjpatch$addAtlantisTableLayout() {
      int x = this.leftPos;
      int y = this.topPos;
      {
         AbstractDHDMenu<?> menu = this.getMenu();
         int var39 = x + 310;
         int panelY = y + 76;
         byte circlePadding = 6;
         byte circleDiameter = 54;
         int circleX = var39 + 300 - circleDiameter - circlePadding;
         int circleY = panelY + circlePadding;
         this.circleBaseX = circleX;
         this.circleBaseY = circleY;
         AtlantisDHDScreenFixed.DialerOffsets.Offset circleOffset = this.dialerOffsets != null
            ? this.dialerOffsets.get(1000)
            : new AtlantisDHDScreenFixed.DialerOffsets.Offset();
         this.circleButton = this.addRenderableWidget(new PegasusDHDBigButton<AbstractDHDMenu<?>>(circleX + circleOffset.dx, circleY + circleOffset.dy, menu, button ->
            SGJPatchNetwork.CHANNEL.sendToServer(new ServerboundToggleIrisPacket(menu.getDHD().getBlockPos()))) {});
         this.circleButton.setTooltip(Tooltip.create(Component.literal("Toggle Shield")));
         int tileW = 40;
         int tileH = 30;
         int gapX = 13;
         int gapY = 15;
         int tileX0 = x + 114;
         int tileY0 = y + 91;

         for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
               int tileX = tileX0 + col * (tileW + gapX);
               int tileY = tileY0 + row * (tileH + gapY);
               this.addRenderableWidget(new AtlantisDHDScreenFixed.AtlantisInvisibleTileButton(tileX, tileY, tileW, tileH, () -> {
               }));
            }
         }

         ArrayList<AtlantisDHDScreenFixed.DialerTriangle> triangles = new ArrayList<>();
         int[] rowCounts = new int[]{4, 5, 6, 7, 6, 5, 5};
         int maxRow = 7;

         for (int row = 0; row < rowCounts.length; row++) {
            int count = rowCounts[row];
            int baseX = (maxRow - count) * 26 / 2;
            int baseY = row * 22;

            for (int col = 0; col < count; col++) {
               boolean up = (row + col) % 2 == 0;
               int cellX = baseX + col * 26;
               triangles.add(new AtlantisDHDScreenFixed.DialerTriangle(cellX, baseY, 32, up));
            }
         }

         int minX = Integer.MAX_VALUE;
         int minY = Integer.MAX_VALUE;
         int maxX = Integer.MIN_VALUE;
         int maxY = Integer.MIN_VALUE;

         for (AtlantisDHDScreenFixed.DialerTriangle triangle : triangles) {
            minX = Math.min(minX, triangle.x);
            minY = Math.min(minY, triangle.y);
            maxX = Math.max(maxX, triangle.x + triangle.size);
            maxY = Math.max(maxY, triangle.y + triangle.size);
         }

         int boundsW = Math.max(1, maxX - minX);
         int boundsH = Math.max(1, maxY - minY);
         int usableW = 166;
         int offsetX = var39 + (usableW - boundsW) / 2 - minX;
         int offsetY = panelY + (168 - boundsH) / 2 - minY;

         for (int i = 0; i < triangles.size(); i++) {
            AtlantisDHDScreenFixed.DialerTriangle triangle = triangles.get(i);
            triangles.set(i, new AtlantisDHDScreenFixed.DialerTriangle(triangle.x + offsetX, triangle.y + offsetY, triangle.size, triangle.up));
         }

         double centerX = triangles.stream().mapToDouble(trianglex -> trianglex.centerX).average().orElse(0.0);
         double centerY = triangles.stream().mapToDouble(trianglex -> trianglex.centerY).average().orElse(0.0);
         triangles.sort(
            Comparator.<AtlantisDHDScreenFixed.DialerTriangle>comparingDouble(trianglex -> -trianglex.radiusTo(centerX, centerY))
               .thenComparingDouble(trianglex -> trianglex.angleClockwiseFromTop(centerX, centerY))
         );

         for (int i = 0; i < triangles.size() && i < ATLANTIS_SYMBOL_ORDER.length; i++) {
            AtlantisDHDScreenFixed.DialerTriangle triangle = triangles.get(i);
            AtlantisDHDScreenFixed.DialerOffsets.Offset offset = this.dialerOffsets != null
               ? this.dialerOffsets.get(i)
               : new AtlantisDHDScreenFixed.DialerOffsets.Offset();
            int symbol = ATLANTIS_SYMBOL_ORDER[i];
            if (symbol != 8 && symbol != 33) {
               AtlantisDHDScreenFixed.AtlantisTriangleSymbolButton button = new AtlantisDHDScreenFixed.AtlantisTriangleSymbolButton(
                  triangle.x, triangle.y, triangle.size, triangle.size, menu, symbol, symbol, i, triangle.up, this
               );
               this.dialerButtons.add(button);
               button.applyFlip(offset.flip);
               int symbolYOffset = symbol != 4 && symbol != 9 ? 0 : -1;
               button.setPosition(triangle.x + offset.dx, triangle.y + offset.dy + symbolYOffset);
               this.addRenderableWidget(button);
            }
         }

         AtlantisDHDScreenFixed.DialerTriangle oldSymbol33 = triangles.get(32);
         if (this.dialerOffsets != null) {
            this.dialerOffsets.get(32);
         } else {
            new AtlantisDHDScreenFixed.DialerOffsets.Offset();
         }

         int clusterCenterX = oldSymbol33.x + 0 + 10;
         int clusterCenterY = oldSymbol33.y + 0 + 10;
         int clusterX = clusterCenterX - 10;
         int clusterY = clusterCenterY - -8;
         this.sgjpatch$addCenterTriangle(menu, clusterX + 8, clusterY - 2, 8, 7, true, true);
         this.sgjpatch$addCenterTriangle(menu, clusterX - 5 + 5, clusterY + 8 + 6, 0, -1, true, false);
         this.sgjpatch$addCenterTriangle(menu, clusterX + 16 + 5 - 5, clusterY + 8 + 6, 33, 32, true, true);
         this.sgjpatch$addCenterEngageTriangle(menu, clusterX + 8, clusterY + 8 + 5);
      }
   }

   private void sgjpatch$addCenterTriangle(AbstractDHDMenu var1, int var2, int var3, int var4, int var5, boolean var6, boolean var7) {
      int var8;
      if (var5 < 0) {
         var8 = 38;
      } else {
         var8 = var5;
      }

      AtlantisDHDScreenFixed.AtlantisTriangleSymbolButton var9 = new AtlantisDHDScreenFixed.AtlantisTriangleSymbolButton(
         var2, var3, 16, 16, var1, var4, var7 ? var4 : 0, var8, var6, this
      );
      this.dialerButtons.add(var9);
      this.addRenderableWidget(var9);
   }

   private void sgjpatch$addCenterEngageTriangle(AbstractDHDMenu var1, int var2, int var3) {
      AtlantisDHDScreenFixed.AtlantisTriangleSymbolButton var4 = new AtlantisDHDScreenFixed.AtlantisTriangleSymbolButton(
         var2, var3, 16, 16, var1, -1, -1, 39, false, this
      );
      this.dialerButtons.add(var4);
      var4.setTooltip(Tooltip.create(Component.literal("Engage Stargate")));
      this.addRenderableWidget(var4);
   }

   private static TextureAtlasSprite sgjpatch$getSymbolSprite(AbstractDHDMenu menu, int symbol) {
      if (symbol >= 0 && symbol <= 38) {
         try {
            if (menu instanceof AbstractDHDMenuAccessor accessor) {
               if (accessor.sgjpatch$getBlockEntity() instanceof AbstractDHDEntity dhd) {
                  ClientSymbols symbols = ClientSymbols.getSymbols(dhd.symbolInfo().symbols());
                  return symbols == null ? null : ClientSymbols.getSprite(symbols, symbol);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } catch (Throwable var5) {
            return null;
         }
      } else {
         return null;
      }
   }

   private void sgjpatch$applyStateToButtons() {
      if (this.dialerOffsets != null) {
         if (this.circleButton != null) {
            AtlantisDHDScreenFixed.DialerOffsets.Offset var1 = this.dialerOffsets.get(1000);
            this.circleButton.setPosition(this.circleBaseX + var1.dx, this.circleBaseY + var1.dy);
         }

         for (AtlantisDHDScreenFixed.AtlantisTriangleSymbolButton var2 : this.dialerButtons) {
            AtlantisDHDScreenFixed.DialerOffsets.Offset var3 = this.dialerOffsets.get(var2.index);
            var2.applyFlip(var3.flip);
            byte var4 = 0;
            if (var2.actionSymbol == 4 || var2.actionSymbol == 9) {
               var4 = -1;
            }

            var2.setPosition(var2.baseX + var3.dx, var2.baseY + var3.dy + var4);
         }
      }
   }

   private static boolean pointInUpTriangle(double x, double y, int size) {
      x--;
      y--;
      size -= 2;
      if (size <= 2) {
         return false;
      } else {
         double maxY = (double)(size - 1);
         if (!(y < 0.0) && !(y > maxY)) {
            double centerX = (double)(size - 1) / 2.0;
            double halfWidth = y / maxY * centerX;
            return Math.abs(x - centerX) <= halfWidth;
         } else {
            return false;
         }
      }
   }

   private static boolean pointInDownTriangle(double x, double y, int size) {
      x--;
      y--;
      size -= 2;
      if (size <= 2) {
         return false;
      } else {
         double maxY = (double)(size - 1);
         if (!(y < 0.0) && !(y > maxY)) {
            double centerX = (double)(size - 1) / 2.0;
            double halfWidth = (1.0 - y / maxY) * centerX;
            return Math.abs(x - centerX) <= halfWidth;
         } else {
            return false;
         }
      }
   }

   public boolean mouseClicked(double var1, double var3, int var5) {
      if (var5 == 0 && !this.editMode && this.circleButton != null
            && this.circleButton.isMouseOver(var1, var3)) {
         this.circleButton.onPress();
         return true;
      }
      return this.sgjpatch$mouseClickedNormal(var1, var3, var5);
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.sgjpatch$renderNormal(var1, var2, var3, var4);
      this.miniGate.render(var1, this.circleButton, this.getMenu().getDHD());
   }

   private static final class AtlantisInvisibleTileButton extends AbstractButton {
      private final Runnable onPress;

      private AtlantisInvisibleTileButton(int x, int y, int width, int height, Runnable onPress) {
         super(x, y, width, height, Component.empty());
         this.onPress = onPress;
      }

      public void onPress() {
         this.onPress.run();
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      }

      protected void updateWidgetNarration(NarrationElementOutput output) {
      }
   }

   private static final class AtlantisTriangleCenterButton extends AbstractButton {
      private final AbstractDHDMenu menu;
      private final Runnable onCancel;
      private final AtlantisDHDScreenFixed screen;
      private final int size;
      private boolean up;

      private AtlantisTriangleCenterButton(int x, int y, int width, int height, AbstractDHDMenu menu, Runnable onCancel, AtlantisDHDScreenFixed screen) {
         super(x, y, width, height, Component.empty());
         this.menu = menu;
         this.onCancel = onCancel;
         this.screen = screen;
         this.size = width;
         this.up = true;
      }

      private void applyFlip(boolean flip, boolean triangleBaseUp) {
         this.up = triangleBaseUp ^ flip;
      }

      public void onPress() {
         if (this.screen != null) {
            this.screen.centerGlowTicks = 40;
         }

         this.screen.engageStargate();
         this.onCancel.run();
      }

      public boolean isMouseOver(double mouseX, double mouseY) {
         double px = mouseX - (double)this.getX();
         double py = mouseY - (double)this.getY();
         if (!(px < 0.0) && !(py < 0.0) && !(px >= (double)this.size) && !(py >= (double)this.size)) {
            return this.up ? AtlantisDHDScreenFixed.pointInUpTriangle(px, py, this.size) : AtlantisDHDScreenFixed.pointInDownTriangle(px, py, this.size);
         } else {
            return false;
         }
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         boolean active = this.menu != null && this.menu.isCenterButtonEngaged() || this.screen != null && this.screen.centerGlowTicks > 0;
         int frame = active ? 2 : (this.isHoveredOrFocused() ? 1 : 0);
         int v = frame * this.size;
         graphics.blit(
            AtlantisDHDScreenFixed.CRYSTAL_CENTER, this.getX(), this.getY(), 0.0F, (float)v, this.size, this.size, this.size, this.size * 3
         );
      }

      protected void updateWidgetNarration(NarrationElementOutput output) {
      }
   }

   private static final class AtlantisTriangleSymbolButton extends AbstractButton {
      private final AbstractDHDMenu menu;
      private final int displaySymbol;
      private final int actionSymbol;
      private final int index;
      private final boolean baseUp;
      private boolean up;
      private final int size;
      private final AtlantisDHDScreenFixed screen;
      private final int baseX;
      private final int baseY;

      private AtlantisTriangleSymbolButton(
         int var1, int var2, int var3, int var4, AbstractDHDMenu var5, int var6, int var7, int var8, boolean var9, AtlantisDHDScreenFixed var10
      ) {
         super(var1, var2, ButtonScaleHelper.remapSize(var3), ButtonScaleHelper.remapSize(var3),
            var6 >= 0 ? Component.literal(Integer.toString(var6)) : Component.empty());
         this.menu = var5;
         this.displaySymbol = var6;
         this.actionSymbol = var7;
         this.index = var8;
         this.baseUp = var9;
         this.up = var9;
         this.size = ButtonScaleHelper.remapSize(var3);
         this.screen = var10;
         this.baseX = var1;
         this.baseY = var2;
      }

      private void applyFlip(boolean flip) {
         this.up = this.baseUp ^ flip;
      }

      public void sgjpatch$pressNormal() {
         if (this.actionSymbol >= 0 && this.actionSymbol <= 38
               && this.menu != null && this.menu.isCenterButtonEngaged()) {
            return;
         }
         if (this.screen.editMode) {
            this.screen.selectedIndex = this.index;
         } else {
            if (this.actionSymbol == -1) {
               this.screen.engageStargate();
               this.screen.sgjpatch$clearEngagedLatch();
            } else if (this.actionSymbol >= 0 && this.actionSymbol <= 38) {
               this.screen.engagedLatch[this.actionSymbol] = true;
               this.screen.encodeSymbol(this.actionSymbol);
            }
         }
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         if (this.screen.editMode) {
            if (button != 0) {
               return false;
            }

            AtlantisEditorHelper.clear(this.screen);
         }

         if (this.screen.editMode && this.isMouseOver(mouseX, mouseY)) {
            this.screen.selectedIndex = this.index;
            return true;
         } else {
            return super.mouseClicked(mouseX, mouseY, button);
         }
      }

      public boolean isMouseOver(double var1, double var3) {
         return AtlantisButtonHitHelper.contains(this, var1, var3);
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         AtlantisHoverPriorityHelper.setMouse(mouseX, mouseY);
         boolean showLegacyNumbers = this.screen != null && this.screen.sgjpatch$showLegacyNumbers();
         if (showLegacyNumbers || !this.sgjpatch$renderSymbolTexture(graphics)) {
            ResourceLocation texture;
            if (this.size >= 30) {
               texture = ResourceLocation.fromNamespaceAndPath("sgjpatch", AtlantisVisualHelper.outerBackground(this.up));
            } else {
               texture = this.up ? AtlantisDHDScreenFixed.CRYSTAL_UP : AtlantisDHDScreenFixed.CRYSTAL_DOWN;
            }

            boolean engaged = this.actionSymbol >= 0
               && this.actionSymbol <= 38
               && (this.screen.engagedLatch[this.actionSymbol] || this.menu.isSymbolEngaged(this.actionSymbol));
            int frame = AtlantisHoverPriorityHelper.isExclusiveCurrent(this) ? 1 : 0;
            this.sgjpatch$renderTriangleBackground(graphics, texture, frame);
            if (engaged) {
               graphics.flush();
               renderGoldOverlay(graphics, this.getX(), this.getY(), this.size, this.up);
            }

            String text = this.displaySymbol >= 0 ? Integer.toString(this.displaySymbol) : "";
            float scale = 0.75F * (float)this.size / 20.0F;
            int textWidth = Minecraft.getInstance().font.width(text);
            int centerX = this.getX() + this.size / 2;
            int centerY = this.getY() + this.size / 2;
            int textX = (int)((float)(centerX + 1) / scale - (float)textWidth / 2.0F);
            int textY = (int)((float)centerY / scale - 9.0F / 2.0F);
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 1.0F);
            if (!text.isEmpty()) {
               graphics.drawString(Minecraft.getInstance().font, text, textX, textY, -16119286, false);
            }

            graphics.pose().popPose();
         }
      }

      private boolean sgjpatch$renderSymbolTexture(GuiGraphics var1) {
         if (this.displaySymbol < 1) {
            return false;
         } else if (this.displaySymbol > 38) {
            return false;
         } else {
            boolean var5 = this.size >= 30;
            ResourceLocation var2;
            if (var5) {
               var2 = ResourceLocation.fromNamespaceAndPath("sgjpatch", AtlantisVisualHelper.outerBackground(this.up));
            } else {
               var2 = this.up ? AtlantisDHDScreenFixed.CRYSTAL_UP : AtlantisDHDScreenFixed.CRYSTAL_DOWN;
            }

            boolean var3 = false;
            if (this.actionSymbol >= 0
               && this.actionSymbol <= 38
               && (this.screen.engagedLatch[this.actionSymbol] || this.menu.isSymbolEngaged(this.actionSymbol))) {
               var3 = true;
            }

            int var4 = AtlantisHoverPriorityHelper.isExclusiveCurrent(this) ? 1 : 0;
            this.sgjpatch$renderTriangleBackground(var1, var2, var4);
            if (var3) {
               var1.flush();
               renderGoldOverlay(var1, this.getX(), this.getY(), this.size, this.up);
            }

            String var6 = NativePegasusTextureHelper.texturePath(this.displaySymbol, var5);
            if (var6 != null) {
               NativePegasusTextureHelper.applyTint();
               ResourceLocation gateTexture = com.mustangdoc.sgjpatch.client.render.AtlantisDHDButtonStateRenderer
                     .symbolTexture(this.menu.getDHD(), this.actionSymbol);
               if (gateTexture != null) {
                  int glyphSize = (int)Math.floor(this.size * .50);
                  int glyphX = this.getX() + (this.size - glyphSize) / 2;
                  int glyphY = this.getY() + (int)Math.round(this.size * (this.up ? 2d / 3 : 1d / 3) - glyphSize / 2d);
                  RenderSystem.setShaderColor(0.42F, 1.0F, 0.76F, 1.0F);
                  var1.blit(gateTexture, glyphX, glyphY, glyphSize, glyphSize, 0, 0, 1, 1, 1, 1);
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               }
               NativePegasusTextureHelper.resetTint();
            }

            return true;
         }
      }

      private void sgjpatch$renderTriangleBackground(GuiGraphics var1, ResourceLocation var2, int var3) {
         AtlantisKeypadSurface.crystal(var1, this.getX(), this.getY(), this.size, this.up, var3);
      }

      protected void updateWidgetNarration(NarrationElementOutput output) {
      }

      private static void renderGoldOverlay(GuiGraphics graphics, int x, int y, int size, boolean up) {
         Integer gold = ChatFormatting.GOLD.getColor();
         int rgb = gold != null ? gold : 16755200;
         float r = (float)(rgb >> 16 & 0xFF) / 255.0F;
         float g = (float)(rgb >> 8 & 0xFF) / 255.0F;
         float b = (float)(rgb & 0xFF) / 255.0F;
         float a = 0.55F;
         float inset = 1.0F;
         float x0 = (float)x + inset;
         float y0 = (float)y + inset;
         float x1 = (float)(x + size) - inset;
         float y1 = (float)(y + size) - inset;
         float centerX = (x0 + x1) * 0.5F;
         Matrix4f matrix = graphics.pose().last().pose();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         BufferBuilder buffer = Tesselator.getInstance().getBuilder();
         buffer.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
         if (up) {
            buffer.vertex(matrix, centerX, y0, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x0, y1, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x1, y1, 0.0F).color(r, g, b, a).endVertex();
         } else {
            buffer.vertex(matrix, x0, y0, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x1, y0, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, centerX, y1, 0.0F).color(r, g, b, a).endVertex();
         }

         BufferUploader.drawWithShader(Objects.requireNonNull(buffer.end()));
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }

      public void onPress() {
         AtlantisEngageCloseHelper.press(this);
      }
   }

   private static final class DialerOffsets {
      private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
      private final Map<Integer, AtlantisDHDScreenFixed.DialerOffsets.Offset> offsets = new HashMap<>();
      private final Path path;

      private DialerOffsets(Path path) {
         this.path = path;
      }

      static AtlantisDHDScreenFixed.DialerOffsets load() {
         Path path = FMLPaths.CONFIGDIR.get().resolve("sgjpatch-atlantis-dialer-scaled2.json");
         AtlantisDHDScreenFixed.DialerOffsets store = new AtlantisDHDScreenFixed.DialerOffsets(path);
         if (false) {
            return store;
         } else if (!Files.exists(path)) {
            return store;
         } else {
            try {
               String json = Files.readString(path, StandardCharsets.UTF_8);
               Map<String, Object> raw = (Map<String, Object>)GSON.fromJson(json, Map.class);
               if (raw == null) {
                  return store;
               } else {
                  for (Entry<String, Object> entry : raw.entrySet()) {
                     try {
                        int index = Integer.parseInt(entry.getKey());
                        if (entry.getValue() instanceof Map<?, ?> valueMap) {
                           AtlantisDHDScreenFixed.DialerOffsets.Offset offset = new AtlantisDHDScreenFixed.DialerOffsets.Offset();
                           Object dx = valueMap.get("dx");
                           Object dy = valueMap.get("dy");
                           Object flip = valueMap.get("flip");
                           if (dx instanceof Number number) {
                              offset.dx = number.intValue();
                           }

                           if (dy instanceof Number number) {
                              offset.dy = number.intValue();
                           }

                           if (flip instanceof Boolean bool) {
                              offset.flip = bool;
                           } else if (flip instanceof String stringValue) {
                              offset.flip = Boolean.parseBoolean(stringValue);
                           }

                           store.offsets.put(index, offset);
                        }
                     } catch (NumberFormatException var14) {
                     }
                  }

                  return store;
               }
            } catch (Exception var15) {
               return store;
            }
         }
      }

      AtlantisDHDScreenFixed.DialerOffsets.Offset get(int index) {
         return this.offsets.computeIfAbsent(index, var0 -> {
            AtlantisDHDScreenFixed.DialerOffsets.Offset var1x = new AtlantisDHDScreenFixed.DialerOffsets.Offset();
            int var2 = var0;
            var1x.dx = AtlantisLayoutDefaults.dx(var2);
            var1x.dy = AtlantisLayoutDefaults.dy(var2);
            var1x.flip = AtlantisLayoutDefaults.flip(var2);
            return var1x;
         });
      }

      void set(int index, int dx, int dy) {
         AtlantisDHDScreenFixed.DialerOffsets.Offset offset = this.get(index);
         offset.dx = dx;
         offset.dy = dy;
      }

      void save() {
         try {
            Files.createDirectories(this.path.getParent());
            Files.writeString(this.path, GSON.toJson(this.offsets), StandardCharsets.UTF_8);
         } catch (Exception var2) {
         }
      }

      private static final class Offset {
         int dx = 0;
         int dy = 0;
         boolean flip = false;
      }
   }

   private static final class DialerTriangle {
      private final int x;
      private final int y;
      private final int size;
      private final boolean up;
      private final double centerX;
      private final double centerY;

      private DialerTriangle(int x, int y, int size, boolean up) {
         this.x = x;
         this.y = y;
         this.size = size;
         this.up = up;
         this.centerX = (double)x + (double)size / 2.0;
         this.centerY = (double)y + (double)size / 2.0;
      }

      private double radiusTo(double cx, double cy) {
         double dx = this.centerX - cx;
         double dy = this.centerY - cy;
         return Math.sqrt(dx * dx + dy * dy);
      }

      private double angleClockwiseFromTop(double cx, double cy) {
         double dx = this.centerX - cx;
         double dy = this.centerY - cy;
         double angle = Math.toDegrees(Math.atan2(dx, -dy));
         if (angle < 0.0) {
            angle += 360.0;
         }

         return angle;
      }
   }
}
