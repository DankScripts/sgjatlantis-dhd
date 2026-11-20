package com.mustangdoc.sgjpatch.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mustangdoc.sgjpatch.client.render.NativePegasusTextureHelper;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Inverted Atlantis button arrangement, with upright labels and gate art. */
@OnlyIn(Dist.CLIENT)
public final class LanteaJumperScreen extends Screen {
    private static final String GUI = "textures/gui/", GATE = GUI + "mini_gate/early_port/";
    private static final String TABLE_BACKGROUND = GUI + "atlantis_dhd_clean.png";
    private static final String TABLE_CRYSTAL = GUI + "atlantis_dhd_control_crystal.png";
    private static final String TABLE_BAR = GUI + "atlantis_dhd_control_bar.png";
    private static final int W = 370, H = 265;
    private static final int TABLE_W = 720, TABLE_H = 360;
    // Keep the table's miniature gate at its original upper-right position.
    private static final int TABLE_GATE_X = 505, TABLE_GATE_Y = 38;
    private static final int TABLE_BAR_Y = 252;
    private static final int TABLE_DIALER_X_OFFSET = -46;
    private static final int TABLE_CONTROLS_X_OFFSET = -24;
    // Cockpit rows, top to bottom: preserve the inverted physical arrangement.
    private static final int[][] KEYS = makeKeys();
    private static int[][] makeKeys() {
        int[][] rows = {{18,25,31,24,36,7,15}, {35,16,30,37,34,29,17,19,5},
                {23,6,38,28,8,32,27,13,22}, {14,12,21,26,20,11,2}, {4,10,9,3,1}};
        List<int[]> keys = new ArrayList<>();
        for (int r = 0; r < rows.length; r++) {
            int start = (9 - rows[r].length) * 12;
            for (int c = 0; c < rows[r].length; c++) {
                if (rows[r][c] == 8) continue;
                keys.add(new int[]{rows[r][c], start + c * 24, 15 + r * 46, 44,
                        (c + (r < 2 ? 0 : 1)) % 2});
            }
        }
        // Four separate hit regions inside the original inverted centre triangle.
        keys.add(new int[]{0,97,108,20,1});
        keys.add(new int[]{33,119,108,20,1});
        keys.add(new int[]{8,108,130,20,1});
        keys.add(new int[]{-1,108,108,20,0});
        return keys.toArray(int[][]::new);
    }
    private final List<Integer> chosen = new ArrayList<>();
    private List<ResourceLocation> symbolTextures;
    private LanteaJumperNetwork.State state;
    private Component message;
    private float scale;
    private int left, top, ticks;
    private boolean pending;
    private String pastedAddress;
    private int[] guideKeys;

    private LanteaJumperScreen(LanteaJumperNetwork.State state) {
        super(Component.literal("lantea_dhd".equals(state.backend()) ? "Atlantis DHD" : "Puddle jumper dialer"));
        this.state = state;
        this.message = state.message();
        for (int symbol : nativeGate()?state.typedSymbols():state.encodedSymbols()) chosen.add(symbol);
    }
    public static void receive(LanteaJumperNetwork.State state) {
        // Server progress also updates the cockpit after the screen is dismissed.
        String symbols = java.util.Arrays.stream(state.encodedSymbols()).mapToObj(Integer::toString)
                .collect(java.util.stream.Collectors.joining("-"));
        if ("lantea_dhd".equals(state.backend())) { /* Stationary console, not a jumper light cache. */ }
        else if ("lantea".equals(state.backend())) LanteaJumperGlyphLights.updateNative(state.jumper(), symbols, state.symbols());
        else if ("Ready".equals(state.status()) || !state.valid()) LanteaJumperGlyphLights.clear(state.jumper());
        else LanteaJumperGlyphLights.update(state.jumper(), symbols, state.symbols());
        Minecraft mc = Minecraft.getInstance();
        if (state.open()) mc.setScreen(new LanteaJumperScreen(state));
        else if (mc.screen instanceof LanteaJumperScreen screen && screen.state.token().equals(state.token())) {
            screen.state = state;
            screen.chosen.clear();
            for (int symbol : screen.nativeGate()?state.typedSymbols():state.encodedSymbols()) screen.chosen.add(symbol);
            if(screen.nativeGate() && screen.guideKeys!=null && screen.acceptsSymbols()) screen.guideMessage();
            if (!state.message().getString().isEmpty()) screen.message = state.message();
            screen.pending = false;
        }
    }
    public static void receiveGuide(LanteaJumperNetwork.DialGuide guide) {
        if (!(Minecraft.getInstance().screen instanceof LanteaJumperScreen screen)
                || !screen.nativeGate() || !screen.state.token().equals(guide.token())
                || !guide.address().equals(screen.pastedAddress)) return;
        screen.pending = false;
        if (guide.keys().length == 0) {
            screen.pastedAddress = null;
            screen.guideKeys = null;
            screen.message = guide.error().startsWith("lantea.")
                    ? Component.translatable(guide.error())
                    : Component.literal(guide.error().isBlank() ? "Cannot guide that address." : guide.error());
        } else {
            screen.guideKeys = guide.keys();
            screen.message = Component.literal("Press the glowing symbol, then the next. Right-click to clear the sequence.");
        }
    }
    @Override protected void init() {
        int canvasW = canvasWidth(), canvasH = canvasHeight();
        scale = Math.max(.25f, Math.min(1.5f, Math.min((width - 24f) / canvasW, (height - 56f) / canvasH)));
        left = Math.round((width - canvasW * scale) / 2f);
        top = Math.round((height - canvasH * scale) / 2f) - 9;
    }
    private boolean stationary() { return "lantea_dhd".equals(state.backend()); }
    private int canvasWidth() { return stationary() ? TABLE_W : W; }
    private int canvasHeight() { return stationary() ? TABLE_H : H; }
    private boolean nativeGate() { return stationary() || "lantea".equals(state.backend()); }
    private boolean acceptsSymbols() { return "Ready".equals(state.status()) || "Entering address".equals(state.status()); }
    private int maxDestinations() { return stationary() ? 7 : nativeGate() ? 6 : 8; }
    /** Advance only through symbols acknowledged by the native gate. */
    private int guideNext() {
        if (guideKeys == null) return -3;
        if (chosen.size() > guideKeys.length) return -2;
        for (int i = 0; i < chosen.size(); i++) if (chosen.get(i) != guideKeys[i]) return -2;
        return chosen.size() == guideKeys.length ? -1 : guideKeys[chosen.size()];
    }
    private void guideMessage() {
        int next=guideNext();
        if (next == -2) message=Component.literal("Wrong sequence. Right-click to clear and start again.");
        else if (next == -1) message=Component.literal("Address entered. Press the glowing centre diamond to dial.");
        else if (next >= 0) message=Component.literal("Symbol " + (chosen.size()+1) + "/" + guideKeys.length
                + ": press the glowing button.");
    }
    private void press(int symbol) {
        if (pending || !state.valid()) return;
        if (!nativeGate()) {
            pending = true;
            LanteaJumperNetwork.command(state.token(), symbol == -1 ? 1 : 4,
                    symbol == -1 ? "" : Integer.toString(symbol));
            return;
        }
        if (symbol == -1) {
            if ("Awaiting activation".equals(state.status())) {
                pending = true;
                LanteaJumperNetwork.command(state.token(), 1, "");
            } else if (!acceptsSymbols()) {
                pending = true;
                LanteaJumperNetwork.command(state.token(), 2, "");
            } else message = Component.literal("Enter the destination symbols and origin 0 first.");
            return;
        }
        if (!acceptsSymbols()) return;
        if (pastedAddress != null) {
            if (guideKeys == null) { message=Component.literal("Checking the pasted address…"); return; }
            if (symbol != guideNext()) { message=Component.literal("Press the glowing symbol next."); return; }
        }
        // Only the server's accepted native buffer advances the guide and button lights.
        pending=true;
        LanteaJumperNetwork.command(state.token(),5,Integer.toString(symbol));
    }
    private double localX(double mouseX) { return (mouseX - left) / scale; }
    private double localY(double mouseY) { return (mouseY - top) / scale; }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = localX(mouseX), y = localY(mouseY);
        if (button == 1) {
            if (nativeGate() && !pending && ("Entering address".equals(state.status()) || "Awaiting activation".equals(state.status()))) {
                pending=true;
                LanteaJumperNetwork.command(state.token(),2,"");
            }
            return true;
        }
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if(clickMemory(x,y))return true;
        // The center of the upright miniature gate doubles as a clear Dial control.
        int gateX = stationary() ? TABLE_GATE_X : 246;
        int gateY = stationary() ? TABLE_GATE_Y : 1;
        if (x >= gateX + 26 && x <= gateX + 92 && y >= gateY + 26 && y <= gateY + 92) { if (stationary()) LanteaJumperNetwork.command(state.token(),7,""); else press(-1); return true; }
        for (int[] key : KEYS) {
            if (contains(key, x, y)) { press(key[0]); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    private static final int MEMORY_X=498, MEMORY_Y=162, MEMORY_W=128;
    private static final int MEMORY_REDIAL_X=94;
    private void renderMemory(GuiGraphics g,double mx,double my) {
        if(!stationary())return;
        g.fill(MEMORY_X,MEMORY_Y,MEMORY_X+MEMORY_W,MEMORY_Y+145,0xFF704836);
        g.fill(MEMORY_X+1,MEMORY_Y+1,MEMORY_X+MEMORY_W-1,MEMORY_Y+144,0xFF231A19);
        smallText(g,"RECENT ADDRESSES",MEMORY_X+5,MEMORY_Y+5,0xFFE4C798);
        if(state.memory().length==0)smallText(g,"No connections saved yet",MEMORY_X+5,MEMORY_Y+21,0xFFB8C1C0);
        for(int i=0;i<state.memory().length;i++) {
            int y=MEMORY_Y+19+i*15;
            boolean hover=my>=y && my<y+13 && mx>=MEMORY_X+3 && mx<MEMORY_X+MEMORY_W-3;
            g.fill(MEMORY_X+3,y,MEMORY_X+MEMORY_W-3,y+13,hover?0xFF51382D:0xFF342723);
            smallText(g,state.memory()[i],MEMORY_X+5,y+4,0xFFBFEAE7);
            g.fill(MEMORY_X+MEMORY_REDIAL_X,y+1,MEMORY_X+MEMORY_W-4,y+12,0xFF866849);
            smallText(g,"Redial",MEMORY_X+MEMORY_REDIAL_X+3,y+4,0xFFFFFFFF);
        }
    }
    private void smallText(GuiGraphics g,String text,int x,int y,int color) {
        g.pose().pushPose(); g.pose().translate(x,y,0); g.pose().scale(.6F,.6F,1);
        g.drawString(font,text,0,0,color,false); g.pose().popPose();
    }
    private boolean clickMemory(double x,double y) {
        if(!stationary() || x<MEMORY_X || x>=MEMORY_X+MEMORY_W
                || y<MEMORY_Y+19 || y>=MEMORY_Y+19+state.memory().length*15)return false;
        if(pending || !state.valid())return true;
        if(!"Ready".equals(state.status())) {
            message=Component.literal("Clear the current sequence before selecting a saved address."); return true;
        }
        int index=(int)(y-MEMORY_Y-19)/15;
        chosen.clear(); guideKeys=null; pastedAddress=null; pending=true;
        if(x>=MEMORY_X+MEMORY_REDIAL_X) {
            message=Component.literal("Redialing saved address…");
            LanteaJumperNetwork.command(state.token(),6,Integer.toString(index));
        } else {
            pastedAddress=state.memory()[index];
            message=Component.literal("Checking saved address…");
            LanteaJumperNetwork.command(state.token(),4,pastedAddress);
        }
        return true;
    }
    @Override public void tick() {
        if (++ticks % 1 == 0 && state.valid()) LanteaJumperNetwork.command(state.token(), 0, "");
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nativeGate() && Screen.isPaste(keyCode)) {
            if (!"Ready".equals(state.status()) || pending) {
                message=Component.literal("Clear the current sequence before pasting another address.");
                return true;
            }
            String clipboard = minecraft.keyboardHandler.getClipboard();
            var match = java.util.regex.Pattern.compile("(?i)\\b(?:mw|pg):\\d{1,2}(?:-\\d{1,2}){5,7}\\b")
                    .matcher(clipboard == null ? "" : clipboard);
            if (match.find()) {
                pastedAddress = match.group().toLowerCase(java.util.Locale.ROOT);
                guideKeys = null;
                chosen.clear();
                pending = true;
                message = Component.literal("Checking " + pastedAddress + " against Lantea's gate registry…");
                LanteaJumperNetwork.command(state.token(), 4, pastedAddress);
            } else message = Component.literal("Clipboard has no Lantea address.");
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public void removed() {
        if (!stationary() && !pending && "Ready".equals(state.status())) LanteaJumperGlyphLights.clear(state.jumper());
        LanteaJumperNetwork.command(state.token(), 3, "");
    }
    @Override public boolean isPauseScreen() { return false; }
    private static ResourceLocation texture(String name) { return new ResourceLocation("sgjpatch", name); }
    private static void blit(GuiGraphics g, String name, int x, int y, int w, int h) {
        g.blit(texture(name), x, y, 0, 0, w, h, w, h);
    }
    private static void blitBar(GuiGraphics g, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(68F / 52F, 26F / 18F, 1F);
        blit(g, TABLE_BAR, 0, 0, 52, 18);
        g.pose().popPose();
    }
    private List<ResourceLocation> symbolTextures() {
        if (symbolTextures != null) return symbolTextures;
        String set = switch (state.glyphSet()) {
            case "pegasus" -> "lantea";
            case "milky_way" -> "galaxy_milky_way";
            case "universe" -> "universal";
            default -> state.glyphSet();
        };
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath("sgjourney", "sgjourney/symbols/" + set + ".json");
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(file);
            if (resource.isPresent()) try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray entries = root.getAsJsonArray("textures");
                List<ResourceLocation> paths = new ArrayList<>();
                if (entries != null) for (var entry : entries) {
                    ResourceLocation path = ResourceLocation.tryParse(entry.getAsString());
                    if (path != null) paths.add(ResourceLocation.fromNamespaceAndPath(path.getNamespace(),
                            "textures/" + path.getPath() + ".png"));
                }
                return symbolTextures = List.copyOf(paths);
            }
        } catch (Exception ignored) { }
        return symbolTextures = List.of();
    }
    private static void goldTriangle(GuiGraphics g, int x, int y, int size, boolean up) {
        for (int row = 1; row < size - 1; row++) {
            double progress = up ? (row + 1d) / size : (size - row) / (double) size;
            int halfWidth = Math.max(1, (int) (progress * (size - 2) / 2));
            g.fill(x + size / 2 - halfWidth, y + row, x + size / 2 + halfWidth + 1, y + row + 1, 0xB3FFAE32);
        }
    }
    private void renderGate(GuiGraphics g) {
        int x = stationary() ? TABLE_GATE_X : 246, y = stationary() ? TABLE_GATE_Y : 1;
        blit(g, GATE + "gate_base.png", x, y, 118, 118);
        if (stationary()) {
            var client = Minecraft.getInstance();
            var gate = client.level == null || client.player == null ? null
                    : LanteaIrisBridge.nearest(client.level, client.player.blockPosition(), false);
            boolean closed = gate instanceof LanteaEnergyIrisState iris && iris.sgjpatch$irisClosed();
            blit(g, GUI + (closed ? "atlantis_shield_button_on_large.png" : "atlantis_shield_button_off_large.png"), x + 22, y + 22, 74, 74);
        }
        int[] encoded = state.encodedSymbols();
        blit(g, GATE + "idle_symbols.png", x, y, 118, 118);
        int[] positions = state.lockedPositions();
        for (int i = 0; i < positions.length && i < encoded.length; i++) {
            int symbol = encoded[i], position = positions[i];
            if (symbol >= 0 && symbol <= 38) {
                blit(g, GATE + String.format("s%02d_p%02d.png", symbol, position), x, y, 118, 118);
                blit(g, GATE + "chevron_slot_" + (position / 4) + ".png", x, y, 118, 118);
            }
        }
        int moving = state.movingPosition();
        if (moving >= 0 && positions.length < encoded.length) {
            int symbol = encoded[positions.length];
            boolean occupied = false;
            for (int locked : positions) if (locked == moving) occupied = true;
            if (!occupied && symbol >= 0 && symbol <= 38)
                blit(g, GATE + String.format("s%02d_p%02d.png", symbol, moving), x, y, 118, 118);
        }
        if ("Connected".equals(state.status())) {
            for (int slot = 0; slot < 9; slot++)
                blit(g, GATE + "chevron_slot_" + slot + ".png", x, y, 118, 118);
        }
    }
    public record KeyShape(double x, double y, double width, double height, boolean up) {
        // Shrink about the centroid: every edge stays parallel and equally inset.
        KeyShape inset(double margin) {
            double w = width - margin * 2, h = height * w / width;
            return new KeyShape(x + margin, y + (height - h) * (up ? 2d/3 : 1d/3), w, h, up);
        }
        public boolean contains(double mx, double my) {
            double t = (my-y)/height;
            return t >= 0 && t <= 1 && Math.abs(mx-x-width/2) <= (up?t:1-t)*width/2;
        }
    }
    private KeyShape keyShape(int[] key) {
        if (!stationary()) return new KeyShape(key[1], key[2], key[3], key[3], key[4] == 0);
        KeyShape shape=com.mustangdoc.sgjpatch.client.util.AtlantisReferencePlate.keyShape(key[0]);
        return new KeyShape(shape.x()+TABLE_DIALER_X_OFFSET,shape.y(),shape.width(),shape.height(),shape.up());
    }
    /** Shared geometry for the stationary Lantea and SGJourney screens. */
    public static KeyShape tableKeyShape(int symbol) { return com.mustangdoc.sgjpatch.client.util.AtlantisReferencePlate.keyShape(symbol); }
    private boolean contains(int[] key, double mx, double my) {
        return keyShape(key).contains(mx, my);
    }
    private static void triangle(GuiGraphics g, KeyShape shape, int color) {
        for (int row = (int)Math.ceil(shape.y); row < shape.y + shape.height; row++) {
            double t = Math.min(1, (row + .5 - shape.y) / shape.height);
            double half = (shape.up ? t : 1-t) * shape.width / 2;
            double cx = shape.x + shape.width / 2;
            g.fill((int)Math.ceil(cx-half), row, (int)Math.ceil(cx+half), row+1, color);
        }
    }
    private void renderKey(GuiGraphics g, int[] key, double mx, double my) {
        KeyShape shape = keyShape(key);
        int symbol=key[0], width=(int)Math.round(shape.width), cx=(int)Math.round(shape.x+shape.width/2);
        boolean selected=chosen.contains(symbol), hover=shape.contains(mx,my);
        for (int encoded:state.encodedSymbols()) if(encoded==symbol) selected=true;
        triangle(g,shape,0xFF21150E); int inset=1;
        int idle = stationary() ? 0xFF159C89 : 0xFF653C25;
        int hot = stationary() ? 0xFF50D8BF : 0xFF99603A;
        boolean guided = (acceptsSymbols() || "Awaiting activation".equals(state.status())) && symbol == guideNext();
        int guideGlow = (int)(210 + 45 * Math.sin(System.currentTimeMillis() / 240.0));
        int guideColor = 0xFF000000 | (guideGlow << 16) | (0xF5 << 8) | 0xFF;
        triangle(g,shape.inset(inset),
                guided?guideColor:selected?0xFFFFC453:hover?hot:idle);
        int cy=(int)Math.round(shape.y+shape.height*(shape.up?2d/3:1d/3));
        if(symbol == -1) {
            g.drawCenteredString(font,"◆",cx,cy-4,guided?0xFFFFFFFF:0xFFFFD578);
        } else if(Screen.hasShiftDown() || symbol==0) {
            g.drawCenteredString(font,Integer.toString(symbol),cx,cy-4,selected?0x382109:0xFFE7CD9C);
        } else {
            int artSymbol=nativeGate() && symbol>=20?symbol+1:symbol;
            String path=NativePegasusTextureHelper.texturePath(artSymbol,true);
            ResourceLocation art=path!=null && "pegasus".equals(state.glyphSet())?texture(path):
                    symbol>0 && symbol<=symbolTextures().size()?symbolTextures().get(symbol-1):null;
            if(art!=null) {
                // Draw the complete source UV range into a smaller, triangle-safe box.
                g.flush();
                RenderSystem.setShaderColor(.42f,1f,.76f,1f);
                int gw=width<30?8:18, gh=width<30?7:16;
                g.blit(art,cx-gw/2,cy-gh/2,gw,gh,0,0,28,28,28,28);
                g.flush();
                RenderSystem.setShaderColor(1,1,1,1);
            }
        }
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        double mx = localX(mouseX), my = localY(mouseY);
        g.pose().pushPose();
        g.pose().translate(left, top, 0);
        g.pose().scale(scale, scale, 1);
        if (stationary()) {
            blit(g, TABLE_BACKGROUND, 0, 0, TABLE_W, TABLE_H);
            g.blit(texture(TABLE_BACKGROUND),631,0,611,0,20,TABLE_H,TABLE_W,TABLE_H);
            g.blit(texture(TABLE_BACKGROUND),651,0,631,0,31,TABLE_H,TABLE_W,TABLE_H);
            g.blit(texture(TABLE_BACKGROUND),83,198,283,198,170,69,TABLE_W,TABLE_H);
            RenderSystem.setShaderColor(.42f,.90f,.68f,1f);
            for (int row = 0; row < 2; row++) for (int col = 0; col < 3; col++)
                blit(g, TABLE_CRYSTAL, 70 + col * 53, 70 + row * 45, 44, 34);
            RenderSystem.setShaderColor(1f,1f,1f,1f);
            blitBar(g, 60, TABLE_BAR_Y);
            blitBar(g, 142, TABLE_BAR_Y);
        } else {
            g.fill(-10, -12, W + 10, H + 9, 0xF01B2528);
            g.fill(-8, -10, W + 8, H + 7, 0xFF755C45);
            g.fill(-5, -7, W + 5, H + 4, 0xFF35251A);
        }
        if (stationary()) com.mustangdoc.sgjpatch.client.util.AtlantisKeypadSurface.backplate(g, TABLE_DIALER_X_OFFSET, 0);
        renderGate(g);
        for (int[] key : KEYS) renderKey(g, key, mx, my);
        renderMemory(g,mx,my);
        g.pose().popPose();
        int textY = Math.min(height - 28, top + Math.round(canvasHeight() * scale) + 5);
        String status = acceptsSymbols() ? (nativeGate() && pastedAddress != null
                ? "Pasted " + pastedAddress + " • follow the glowing buttons • centre diamond to dial"
                : "Select 6–" + maxDestinations() + " glyphs • centre diamond to dial • "
                + (nativeGate() ? "Ctrl+V to paste a Lantea address" : "hold Shift for numbers"))
                : "Gate: " + state.status() + " • centre diamond to engage / disconnect";
        g.drawCenteredString(font, status, width / 2, textY, 0xBFEAE7);
        if (!message.getString().isBlank()) g.drawCenteredString(font, message, width / 2, textY + 11, 0xE4D9B0);
        super.render(g, mouseX, mouseY, partial);
    }
}







