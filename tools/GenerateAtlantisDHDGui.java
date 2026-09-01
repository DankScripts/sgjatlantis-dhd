import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Generates the expanded, physical-table-inspired Atlantis DHD GUI texture. */
public final class GenerateAtlantisDHDGui {
    private static final int WIDTH = 720;
    private static final int HEIGHT = 360;
    // Keep the 720-pixel canvas/anchors, but end the visible tabletop directly
    // after the final 16-pixel checker panel at x=630.
    private static final int TABLE_WIDTH = 662;
    private static final int SHIFT_X = (WIDTH - 640) / 2;
    private static final int SHIFT_Y = (HEIGHT - 320) / 2;

    private static final Color OUTER = color(37, 42, 50);
    private static final Color SHADOW = color(25, 21, 25);
    private static final Color SHELL = color(67, 36, 33);
    private static final Color SHELL_LIGHT = color(94, 49, 41);
    private static final Color COPPER = color(127, 67, 50);
    private static final Color COPPER_LIGHT = color(167, 86, 59);
    private static final Color FASCIA = color(112, 57, 47);
    private static final Color DECK = color(43, 29, 31);
    private static final Color DECK_ALT = color(49, 31, 33);
    private static final Color DECK_ACCENT = color(56, 34, 35);
    private static final Color INLAY = color(97, 54, 44);

    private GenerateAtlantisDHDGui() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: GenerateAtlantisDHDGui <old.png> <new.png> <crystal.png> <bar.png>");
        }

        BufferedImage old = ImageIO.read(Path.of(args[0]).toFile());
        if (old == null || old.getWidth() != 576 || old.getHeight() != 288) {
            throw new IllegalStateException("Expected the accepted 576x288 GUI texture");
        }

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Charcoal screen surround and a stepped Lantean copper console frame.
        // Leave the unused right side transparent so the fixed 720-pixel GUI
        // canvas does not paint a charcoal tail beyond the shortened table.
        fill(graphics, OUTER, 0, 0, TABLE_WIDTH, HEIGHT);
        fill(graphics, SHADOW, 18, 6, TABLE_WIDTH - 36, HEIGHT - 12);
        fill(graphics, SHELL, 21, 9, TABLE_WIDTH - 42, HEIGHT - 18);
        fill(graphics, COPPER, 24, 12, TABLE_WIDTH - 48, HEIGHT - 24);
        fill(graphics, SHADOW, 28, 16, TABLE_WIDTH - 56, HEIGHT - 32);
        fill(graphics, DECK, 31, 19, TABLE_WIDTH - 62, HEIGHT - 38);

        // Subtle block-panel language copied from the physical prop textures.
        for (int row = 0; row < 18; row++) {
            for (int col = 0; col < 37; col++) {
                int x = 39 + col * 16;
                int y = 27 + row * 16;
                Color panel = ((row * 7 + col * 11) % 5 == 0) ? DECK_ACCENT
                        : ((row + col) % 3 == 0 ? DECK_ALT : DECK);
                fill(graphics, panel, x, y, 16, 16);
            }
        }

        // Dark control-field insets and restrained copper circuit paths.
        // The crystal frame follows the newly accepted F8 baseline: the six
        // crystals span x=70..219 and y=70..150 on the 720x360 canvas.
        fill(graphics, SHADOW, 58, 52, 175, 116);
        fill(graphics, SHELL, 62, 56, 167, 108);
        fill(graphics, DECK, 66, 60, 159, 100);

        fill(graphics, INLAY, 44 + SHIFT_X, 179 + SHIFT_Y, 168, 3);
        fill(graphics, COPPER, 44 + SHIFT_X, 182 + SHIFT_Y, 3, 64);
        fill(graphics, INLAY, 44 + SHIFT_X, 243 + SHIFT_Y, 168, 3);
        fill(graphics, COPPER, 209 + SHIFT_X, 182 + SHIFT_Y, 3, 64);

        // Segmented fascia bands echo the front of both physical tables.
        drawFasciaBand(graphics, 31, 19, TABLE_WIDTH - 62, true);
        drawFasciaBand(graphics, 31, HEIGHT - 37, TABLE_WIDTH - 62, false);

        graphics.dispose();
        Path output = Path.of(args[1]);
        Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());

        // The eight F8-editable controls must be separate sprites. Baking them
        // into the background would leave a visible duplicate behind when an
        // editor offset moves the live control.
        BufferedImage crystal = new BufferedImage(44, 34, BufferedImage.TYPE_INT_ARGB);
        Graphics2D crystalGraphics = crystal.createGraphics();
        crystalGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        crystalGraphics.drawImage(old.getSubimage(54, 60, 36, 28), 0, 0, 44, 34, null);
        crystalGraphics.dispose();
        Path crystalOutput = Path.of(args[2]);
        Files.createDirectories(crystalOutput.getParent());
        ImageIO.write(crystal, "png", crystalOutput.toFile());

        BufferedImage bar = old.getSubimage(66, 156, 52, 18);
        Path barOutput = Path.of(args[3]);
        Files.createDirectories(barOutput.getParent());
        ImageIO.write(bar, "png", barOutput.toFile());
        System.out.println("Generated " + output + " (" + WIDTH + "x" + HEIGHT + ")");
    }

    private static void drawFasciaBand(Graphics2D graphics, int x, int y, int width,
                                       boolean highlightBelow) {
        fill(graphics, SHELL_LIGHT, x, y, width, 5);
        fill(graphics, COPPER, x, highlightBelow ? y + 5 : y - 3, width, 3);
        for (int panelX = x + 12; panelX < x + width - 20; panelX += 68) {
            fill(graphics, FASCIA, panelX, highlightBelow ? y + 1 : y, 45, 3);
            fill(graphics, COPPER_LIGHT, panelX + 4, highlightBelow ? y + 1 : y, 22, 1);
        }
    }

    private static void fill(Graphics2D graphics, Color color,
                             int x, int y, int width, int height) {
        graphics.setColor(color);
        graphics.fillRect(x, y, width, height);
    }

    private static Color color(int red, int green, int blue) {
        return new Color(red, green, blue, 255);
    }
}
