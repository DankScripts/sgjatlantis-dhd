import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarFile;

/** Verifies the live gate-variant constellation build without launching Minecraft. */
public final class VerifyConstellationBuild {
    private VerifyConstellationBuild() {
    }

    public static void main(String[] args) throws Exception {
        Class<?> renderer = Class.forName(
                "com.mustangdoc.sgjpatch.client.render.AtlantisDHDButtonStateRenderer",
                true,
                VerifyConstellationBuild.class.getClassLoader());

        boolean hasRuntimeRenderMethod = Arrays.stream(renderer.getMethods())
                .anyMatch(method -> method.getName().equals("m_6922_")
                        && method.getParameterCount() == 6);
        if (!hasRuntimeRenderMethod) {
            throw new IllegalStateException("Renderer was not reobfuscated to Forge runtime names");
        }

        var revisionField = renderer.getDeclaredField("BUTTON_MAP_REVISION");
        revisionField.setAccessible(true);
        String revision = (String)revisionField.get(null);
        if (!"gui-slot-map-r5-direct-resource-json-scale125".equals(revision)) {
            throw new IllegalStateException("Unexpected renderer revision: " + revision);
        }

        var buttonsField = renderer.getDeclaredField("OUTER_BUTTONS");
        buttonsField.setAccessible(true);
        Object[] buttons = (Object[])buttonsField.get(null);
        int[] expectedSymbols = {
                4, 10, 9, 3, 1,
                14, 12, 21, 26, 20, 11, 2,
                23, 6, 38, 28, 32, 27, 13, 22,
                35, 16, 30, 37, 34, 29, 17, 19, 5,
                18, 25, 31, 24, 36, 7, 15
        };
        int[] actualSymbols = new int[buttons.length];
        var symbolField = buttons[0].getClass().getDeclaredField("symbol");
        symbolField.setAccessible(true);
        for (int index = 0; index < buttons.length; index++) {
            actualSymbols[index] = symbolField.getInt(buttons[index]);
        }
        if (!Arrays.equals(expectedSymbols, actualSymbols)) {
            throw new IllegalStateException("Accepted physical symbol mapping changed");
        }

        assertCenter(renderer, "POINT_OF_ORIGIN", 9.2625D, 6.275D, true);
        assertCenter(renderer, "ENGAGE", 8.8D, 6.275D, false);
        assertCenter(renderer, "SYMBOL_33", 8.3375D, 6.275D, true);
        assertCenter(renderer, "SYMBOL_8", 8.8D, 7.125D, true);

        Path rendererJar = Path.of(renderer.getProtectionDomain().getCodeSource().getLocation().toURI());
        try (JarFile jar = new JarFile(rendererJar.toFile())) {
            var entry = jar.getJarEntry(
                    "com/mustangdoc/sgjpatch/client/render/AtlantisDHDButtonStateRenderer.class");
            if (entry == null) {
                throw new IllegalStateException("Renderer class is absent from the test JAR");
            }
            String constants = new String(jar.getInputStream(entry).readAllBytes(),
                    StandardCharsets.ISO_8859_1);
            assertContains(constants, "getSprite");
            assertContains(constants, "getPixelRGBA");
            assertContains(constants, "stargateCache");
            assertContains(constants, "sgjourney/symbols/");
            assertContains(constants, "sgjourney/point_of_origin/");
            assertContains(constants, "NativePegasusTextureHelper");
            assertContains(constants, "loadAcceptedGuiMask");
            assertAbsent(constants, "dilateMask");
            assertAbsent(constants, "loadBundledPegasusMask");
            assertAbsent(constants, "getExtendedSymbolTexture");
            assertAbsent(constants, "getExtendedTexture");
        }

        System.out.println(
                "Accepted R13 mapping, direct gate resources, exact GUI fallback, and runtime linkage OK");
    }

    private static void assertContains(String constants, String expected) {
        if (!constants.contains(expected)) {
            throw new IllegalStateException("Renderer does not reference " + expected);
        }
    }

    private static void assertAbsent(String constants, String forbidden) {
        if (constants.contains(forbidden)) {
            throw new IllegalStateException("Renderer still references forbidden path " + forbidden);
        }
    }

    private static void assertCenter(Class<?> renderer, String fieldName,
                                     double expectedX, double expectedZ, boolean expectedUp)
            throws Exception {
        var field = renderer.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object button = field.get(null);
        var buttonClass = button.getClass();
        var xField = buttonClass.getDeclaredField("x");
        var zField = buttonClass.getDeclaredField("z");
        var upField = buttonClass.getDeclaredField("up");
        xField.setAccessible(true);
        zField.setAccessible(true);
        upField.setAccessible(true);
        double x = xField.getDouble(button);
        double z = zField.getDouble(button);
        boolean up = upField.getBoolean(button);
        if (Math.abs(x - expectedX) > 0.00001D
                || Math.abs(z - expectedZ) > 0.00001D
                || up != expectedUp) {
            throw new IllegalStateException("Accepted center mapping changed for " + fieldName);
        }
    }
}
