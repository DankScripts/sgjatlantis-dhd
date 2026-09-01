import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Applies the approved 1.25x X/Z dialer scale directly to the last known-good
 * reobfuscated renderer. This deliberately leaves Y, symbol ordering, and GUI
 * layout untouched.
 */
public final class PatchAtlantisDHDRendererScale {
    private static final double CENTER_X = 8.8D;
    private static final double CENTER_Z = 6.7D;
    private static final double SCALE = 1.25D;

    private PatchAtlantisDHDRendererScale() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: <input class> <output class>");
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(input)).accept(node, 0);

        MethodNode button = find(node, "button", "(DDZI)");
        MethodNode centerButton = find(node, "centerButton", "(DDZ)");
        injectCoordinateScale(button);
        injectCoordinateScale(centerButton);

        Map<Double, Double> gold = new LinkedHashMap<>();
        gold.put(0.09816D, 0.12270D);
        gold.put(0.19334D, 0.241675D);
        gold.put(0.24500D, 0.30625D);
        gold.put(0.20336D, 0.25420D);
        gold.put(0.16172D, 0.20215D);
        gold.put(0.12008D, 0.15010D);
        gold.put(0.07844D, 0.09805D);
        gold.put(0.03680D, 0.04600D);
        gold.put(0.58000D, 0.72500D);
        gold.put(0.47934D, 0.599175D);
        gold.put(0.37868D, 0.47335D);
        gold.put(0.27802D, 0.347525D);
        gold.put(0.17736D, 0.22170D);
        gold.put(0.07670D, 0.095875D);
        int goldChanges = replaceConstants(find(node, "drawGoldCrystal", null), gold, false);

        Map<Double, Double> glyph = new LinkedHashMap<>();
        glyph.put(0.410D, 0.5125D);
        MethodNode drawConstellation = find(node, "drawConstellation", null);
        int glyphChanges = replaceConstants(drawConstellation, glyph, true)
                + replaceFirstOpcode(drawConstellation, Opcodes.DCONST_1, 1.250D);

        Map<Double, Double> scoring = new LinkedHashMap<>();
        scoring.put(0.052D, 0.065D);
        scoring.put(0.030D, 0.0375D);
        int scoringChanges = replaceConstants(find(node, "scoreRotation", null), scoring, false);

        int revisionChanges = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof LdcInsnNode ldc
                        && ldc.cst instanceof String text
                        && text.startsWith("gui-slot-map")) {
                    ldc.cst = "gui-slot-map-r4-center-mirror-scale125";
                    revisionChanges++;
                }
            }
        }

        if (goldChanges != 14 || glyphChanges != 2 || scoringChanges != 5) {
            throw new IllegalStateException("Unexpected patch counts: gold=" + goldChanges
                    + ", glyph=" + glyphChanges + ", scoring=" + scoringChanges);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        Files.createDirectories(output.getParent());
        Files.write(output, writer.toByteArray());
        System.out.println("Patched renderer: coordinate helpers=2, gold=" + goldChanges
                + ", glyph=" + glyphChanges + ", scoring=" + scoringChanges
                + ", revision=" + revisionChanges);
    }

    private static MethodNode find(ClassNode node, String name, String descriptorPrefix) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name)
                    && (descriptorPrefix == null || method.desc.startsWith(descriptorPrefix))) {
                return method;
            }
        }
        throw new IllegalStateException("Missing method " + name + " " + descriptorPrefix);
    }

    private static void injectCoordinateScale(MethodNode method) {
        InsnList patch = new InsnList();
        scaleLocal(patch, 0, CENTER_X);
        scaleLocal(patch, 2, CENTER_Z);
        method.instructions.insert(patch);
    }

    private static void scaleLocal(InsnList patch, int local, double center) {
        patch.add(new VarInsnNode(Opcodes.DLOAD, local));
        patch.add(new LdcInsnNode(center));
        patch.add(new InsnNode(Opcodes.DSUB));
        patch.add(new LdcInsnNode(SCALE));
        patch.add(new InsnNode(Opcodes.DMUL));
        patch.add(new LdcInsnNode(center));
        patch.add(new InsnNode(Opcodes.DADD));
        patch.add(new VarInsnNode(Opcodes.DSTORE, local));
    }

    private static int replaceConstants(MethodNode method, Map<Double, Double> replacements,
                                        boolean onlyFirstPerValue) {
        int changes = 0;
        Map<Double, Boolean> replaced = new LinkedHashMap<>();
        for (Double value : replacements.keySet()) {
            replaced.put(value, false);
        }
        for (AbstractInsnNode insn : method.instructions) {
            if (!(insn instanceof LdcInsnNode ldc) || !(ldc.cst instanceof Double value)) {
                continue;
            }
            for (Map.Entry<Double, Double> entry : replacements.entrySet()) {
                if (Double.compare(value, entry.getKey()) == 0
                        && (!onlyFirstPerValue || !replaced.get(entry.getKey()))) {
                    ldc.cst = entry.getValue();
                    replaced.put(entry.getKey(), true);
                    changes++;
                    break;
                }
            }
        }
        for (Map.Entry<Double, Boolean> entry : replaced.entrySet()) {
            if (!entry.getValue()) {
                throw new IllegalStateException("Missing constant " + entry.getKey()
                        + " in " + method.name);
            }
        }
        return changes;
    }

    private static int replaceFirstOpcode(MethodNode method, int opcode, double replacement) {
        for (AbstractInsnNode insn : method.instructions) {
            if (insn.getOpcode() == opcode) {
                method.instructions.set(insn, new LdcInsnNode(replacement));
                return 1;
            }
        }
        throw new IllegalStateException("Missing opcode " + opcode + " in " + method.name);
    }
}
