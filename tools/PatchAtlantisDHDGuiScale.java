import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Expands the Atlantis DHD GUI canvas and the six left crystal hitboxes while
 * preserving every dialer-triangle coordinate, size, and spacing constant.
 */
public final class PatchAtlantisDHDGuiScale {
    private PatchAtlantisDHDGuiScale() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: PatchAtlantisDHDGuiScale <input.class> <output.class>");
        }

        byte[] input = Files.readAllBytes(Path.of(args[0]));
        ClassNode classNode = new ClassNode();
        new ClassReader(input).accept(classNode, 0);

        MethodNode init = method(classNode, "m_7856_");
        replaceConstant(init, 576, 640, 1);
        replaceConstant(init, 288, 320, 1);

        MethodNode layout = method(classNode, "sgjpatch$addAtlantisTableLayout");

        // Moving the canvas origin by -32/-16 would move the dialer on screen.
        // Compensate only its panel anchor so all dialer geometry stays fixed.
        replaceAddedConstant(layout, 1, 238, 270, 4, 1);
        replaceAddedConstant(layout, 2, 40, 56, 5, 1);

        // Enlarge and re-center only the six left crystal interaction tiles.
        replaceStoredConstant(layout, 32, 40, 11, 1);
        replaceStoredConstant(layout, 24, 30, 12, 1);
        replaceStoredConstant(layout, 10, 13, 13, 1);
        replaceStoredConstant(layout, 12, 15, 14, 1);
        replaceAddedConstant(layout, 1, 56, 74, 15, 1);
        replaceAddedConstant(layout, 2, 62, 71, 16, 1);

        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        Path output = Path.of(args[1]);
        Files.createDirectories(output.getParent());
        Files.write(output, writer.toByteArray());
        System.out.println("Patched Atlantis DHD GUI: 640x320 canvas; dialer layout unchanged; six tiles enlarged.");
    }

    private static MethodNode method(ClassNode classNode, String name) {
        return classNode.methods.stream()
                .filter(method -> method.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing method " + name));
    }

    private static void replaceConstant(MethodNode method, int oldValue, int newValue,
                                        int expected) {
        int replacements = 0;
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof IntInsnNode constant && constant.operand == oldValue) {
                constant.operand = newValue;
                replacements++;
            }
        }
        requireCount(method.name, oldValue, newValue, replacements, expected);
    }

    private static void replaceStoredConstant(MethodNode method, int oldValue, int newValue,
                                              int storeVariable, int expected) {
        int replacements = 0;
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (!(instruction instanceof IntInsnNode constant) || constant.operand != oldValue) {
                continue;
            }
            AbstractInsnNode next = nextReal(instruction);
            if (next instanceof VarInsnNode store
                    && store.getOpcode() == Opcodes.ISTORE
                    && store.var == storeVariable) {
                constant.operand = newValue;
                replacements++;
            }
        }
        requireCount(method.name, oldValue, newValue, replacements, expected);
    }

    private static void replaceAddedConstant(MethodNode method, int loadVariable,
                                             int oldValue, int newValue,
                                             int storeVariable, int expected) {
        int replacements = 0;
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (!(instruction instanceof VarInsnNode load)
                    || load.getOpcode() != Opcodes.ILOAD
                    || load.var != loadVariable) {
                continue;
            }
            AbstractInsnNode constantInstruction = nextReal(instruction);
            AbstractInsnNode addInstruction = nextReal(constantInstruction);
            AbstractInsnNode storeInstruction = nextReal(addInstruction);
            if (constantInstruction instanceof IntInsnNode constant
                    && constant.operand == oldValue
                    && addInstruction != null
                    && addInstruction.getOpcode() == Opcodes.IADD
                    && storeInstruction instanceof VarInsnNode store
                    && store.getOpcode() == Opcodes.ISTORE
                    && store.var == storeVariable) {
                constant.operand = newValue;
                replacements++;
            }
        }
        requireCount(method.name, oldValue, newValue, replacements, expected);
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction == null ? null : instruction.getNext();
        while (next != null && next.getOpcode() < 0) {
            next = next.getNext();
        }
        return next;
    }

    private static void requireCount(String method, int oldValue, int newValue,
                                     int actual, int expected) {
        if (actual != expected) {
            throw new IllegalStateException(method + ": expected " + expected
                    + " replacement(s) of " + oldValue + " -> " + newValue
                    + ", found " + actual);
        }
    }
}
