package com.mustangdoc.sgjpatch.client.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;

/**
 * Draws and edits the six flat control crystals and two lower control bars.
 *
 * <p>These elements deliberately live outside the static GUI background so an
 * F8 offset moves the visible control and its edit hitbox together.</p>
 */
public final class AtlantisTableControlEditor {
    private static final int F8 = 297;
    private static final int RESET = 82;
    private static final int LEFT = 263;
    private static final int RIGHT = 262;
    private static final int UP = 265;
    private static final int DOWN = 264;

    private static final int CRYSTAL_WIDTH = 44;
    private static final int CRYSTAL_HEIGHT = 34;
    private static final int BAR_WIDTH = 52;
    private static final int BAR_HEIGHT = 18;

    private static final int[] BASE_X = {112, 165, 218, 112, 165, 218, 138, 202};
    private static final int[] BASE_Y = {89, 89, 89, 134, 134, 134, 192, 192};
    private static final int[] DEFAULT_DX = {-42, -42, -42, -42, -42, -42, -24, -22};
    private static final int[] DEFAULT_DY = {-19, -19, -19, -17, -17, -17, 32, 32};
    private static final int[] WIDTH = {
            CRYSTAL_WIDTH, CRYSTAL_WIDTH, CRYSTAL_WIDTH,
            CRYSTAL_WIDTH, CRYSTAL_WIDTH, CRYSTAL_WIDTH,
            BAR_WIDTH, BAR_WIDTH
    };
    private static final int[] HEIGHT = {
            CRYSTAL_HEIGHT, CRYSTAL_HEIGHT, CRYSTAL_HEIGHT,
            CRYSTAL_HEIGHT, CRYSTAL_HEIGHT, CRYSTAL_HEIGHT,
            BAR_HEIGHT, BAR_HEIGHT
    };

    private static final ResourceLocation CRYSTAL =
            new ResourceLocation("sgjpatch", "textures/gui/atlantis_dhd_control_crystal.png");
    private static final ResourceLocation BAR =
            new ResourceLocation("sgjpatch", "textures/gui/atlantis_dhd_control_bar.png");
    private static final Path OFFSETS_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("sgjpatch-atlantis-table-control-offsets.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Object, State> STATES = new WeakHashMap<>();

    private AtlantisTableControlEditor() {
    }

    public static void initialize(Object screen) {
        synchronized (STATES) {
            STATES.put(screen, State.load());
        }
    }

    public static void keyPressed(Object screen, int keyCode, int modifiers) {
        State state = state(screen);
        if (keyCode == F8) {
            if (AtlantisDebugConfig.editorEnabled()) {
                state.editMode = !state.editMode;
                state.selected = -1;
            } else {
                state.editMode = false;
                state.selected = -1;
            }
            return;
        }

        if (!AtlantisDebugConfig.editorEnabled()) {
            state.editMode = false;
            state.selected = -1;
            return;
        }
        if (!state.editMode || state.selected < 0 || state.selected >= BASE_X.length) {
            return;
        }

        if (keyCode == RESET) {
            state.offsets.put(key(state.selected), defaultOffset(state.selected));
            state.save();
            return;
        }

        int step = (modifiers & 1) != 0 ? 5 : ((modifiers & 2) != 0 ? 10 : 1);
        int dx = 0;
        int dy = 0;
        if (keyCode == LEFT) {
            dx = -step;
        } else if (keyCode == RIGHT) {
            dx = step;
        } else if (keyCode == UP) {
            dy = -step;
        } else if (keyCode == DOWN) {
            dy = step;
        }
        if (dx != 0 || dy != 0) {
            Offset offset = state.offset(state.selected);
            offset.dx += dx;
            offset.dy += dy;
            state.save();
        }
    }

    public static void mouseClicked(Object screen, double mouseX, double mouseY, int button,
                                    int leftPos, int topPos) {
        State state = state(screen);
        if (!state.editMode || !AtlantisDebugConfig.editorEnabled() || button != 0) {
            return;
        }

        // Walk backwards so the visually topmost element wins if the user
        // intentionally overlaps controls while editing.
        for (int index = BASE_X.length - 1; index >= 0; index--) {
            Offset offset = state.offset(index);
            int x = leftPos + BASE_X[index] + offset.dx;
            int y = topPos + BASE_Y[index] + offset.dy;
            if (mouseX >= x && mouseX < x + WIDTH[index]
                    && mouseY >= y && mouseY < y + HEIGHT[index]) {
                state.selected = index;
                return;
            }
        }
        state.selected = -1;
    }

    public static void render(Object screen, GuiGraphics graphics, int leftPos, int topPos) {
        if (!isPowered(screen)) {
            return;
        }

        State state = state(screen);
        for (int index = 0; index < BASE_X.length; index++) {
            Offset offset = state.offset(index);
            int x = leftPos + BASE_X[index] + offset.dx;
            int y = topPos + BASE_Y[index] + offset.dy;
            ResourceLocation texture = index < 6 ? CRYSTAL : BAR;
            int width = WIDTH[index];
            int height = HEIGHT[index];
            graphics.blit(texture, x, y, 0.0F, 0.0F, width, height, width, height);

            if (state.editMode && AtlantisDebugConfig.editorEnabled() && state.selected == index) {
                outline(graphics, x - 2, y - 2, width + 4, height + 4, 0xFFFFFFFF);
                outline(graphics, x - 1, y - 1, width + 2, height + 2, 0xFF00D7E8);
            }
        }
    }

    private static boolean isPowered(Object screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)
                || !(containerScreen.getMenu() instanceof AbstractDHDMenu<?> menu)) {
            return true;
        }
        return menu.blockEntity.energyStorage.hasEnergy(1L);
    }

    private static void outline(GuiGraphics graphics, int x, int y, int width, int height,
                                int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static State state(Object screen) {
        synchronized (STATES) {
            return STATES.computeIfAbsent(screen, ignored -> State.load());
        }
    }

    private static String key(int index) {
        return index < 6 ? "crystal_" + (index + 1) : "bar_" + (index - 5);
    }

    private static Offset defaultOffset(int index) {
        Offset offset = new Offset();
        if (index >= 0 && index < DEFAULT_DX.length) {
            offset.dx = DEFAULT_DX[index];
            offset.dy = DEFAULT_DY[index];
        }
        return offset;
    }

    private static final class State {
        private final Map<String, Offset> offsets = new LinkedHashMap<>();
        private boolean editMode;
        private int selected = -1;

        private static State load() {
            State state = new State();
            try {
                if (Files.exists(OFFSETS_PATH)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> loaded = GSON.fromJson(
                            Files.readString(OFFSETS_PATH, StandardCharsets.UTF_8), Map.class);
                    if (loaded != null) {
                        // Gson returns nested maps for this erased generic type;
                        // normalize their numeric members explicitly.
                        for (Map.Entry<String, Object> entry : loaded.entrySet()) {
                            Object raw = entry.getValue();
                            if (raw instanceof Map<?, ?> map) {
                                Offset offset = new Offset();
                                Object dx = map.get("dx");
                                Object dy = map.get("dy");
                                if (dx instanceof Number number) {
                                    offset.dx = number.intValue();
                                }
                                if (dy instanceof Number number) {
                                    offset.dy = number.intValue();
                                }
                                state.offsets.put(entry.getKey(), offset);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                state.offsets.clear();
            }
            return state;
        }

        private Offset offset(int index) {
            return offsets.computeIfAbsent(key(index), ignored -> defaultOffset(index));
        }

        private void save() {
            try {
                Files.createDirectories(OFFSETS_PATH.getParent());
                Files.writeString(OFFSETS_PATH, GSON.toJson(offsets), StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }
    }

    private static final class Offset {
        private int dx;
        private int dy;
    }
}
