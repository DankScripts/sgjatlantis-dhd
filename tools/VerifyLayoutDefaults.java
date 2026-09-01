import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public final class VerifyLayoutDefaults {
    private static final int[] INDICES = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
            30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
            1000
    };

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected R11 and R12 class directories");
        }

        try (URLClassLoader r11 = loader(args[0]); URLClassLoader r12 = loader(args[1])) {
            Class<?> oldDefaults = r11.loadClass("com.mustangdoc.sgjpatch.client.util.AtlantisLayoutDefaults");
            Class<?> newDefaults = r12.loadClass("com.mustangdoc.sgjpatch.client.util.AtlantisLayoutDefaults");
            int differences = 0;
            for (int index : INDICES) {
                for (String method : new String[] {"dx", "dy"}) {
                    Object oldValue = oldDefaults.getMethod(method, int.class).invoke(null, index);
                    Object newValue = newDefaults.getMethod(method, int.class).invoke(null, index);
                    if (!oldValue.equals(newValue)) {
                        System.out.printf("%s(%d): %s -> %s%n", method, index, oldValue, newValue);
                        differences++;
                    }
                }
                Object oldFlip = oldDefaults.getMethod("flip", int.class).invoke(null, index);
                Object newFlip = newDefaults.getMethod("flip", int.class).invoke(null, index);
                if (!oldFlip.equals(newFlip)) {
                    System.out.printf("flip(%d): %s -> %s%n", index, oldFlip, newFlip);
                    differences++;
                }
            }
            if (differences != 2) {
                throw new AssertionError("Expected exactly two minigate coordinate changes, found " + differences);
            }
        }
    }

    private static URLClassLoader loader(String directory) throws Exception {
        return new URLClassLoader(new URL[] {Path.of(directory).toUri().toURL()}, null);
    }
}
