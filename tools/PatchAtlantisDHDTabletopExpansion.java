import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Expands the already-tested R10 tabletop without reinjecting its F8 hooks.
 * All content anchors move by half of the added canvas size, which keeps every
 * control at the same absolute screen position when the GUI remains centered.
 */
public final class PatchAtlantisDHDTabletopExpansion {
    private PatchAtlantisDHDTabletopExpansion() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: <screen-in> <screen-out> <editor-in> <editor-out>");
        }

        patchScreen(Path.of(args[0]), Path.of(args[1]));
        patchEditor(Path.of(args[2]), Path.of(args[3]));
        System.out.println("Expanded only the tabletop to 720x360; preserved control sizes and F8 offsets.");
    }

    private static void patchScreen(Path input, Path output) throws Exception {
        ClassNode node = read(input);

        MethodNode init = method(node, "m_7856_");
        replaceConstant(init, 672, 720, 1);
        replaceConstant(init, 336, 360, 1);

        MethodNode layout = method(node, "sgjpatch$addAtlantisTableLayout");
        replaceAddedConstant(layout, 1, 286, 310, 4, 1);
        replaceAddedConstant(layout, 2, 64, 76, 5, 1);
        replaceAddedConstant(layout, 1, 90, 114, 15, 1);
        replaceAddedConstant(layout, 2, 79, 91, 16, 1);

        MethodNode render = method(node, "sgjpatch$renderNormal");
        replaceConstant(render, 254, 278, 1);
        write(node, output);
    }

    private static void patchEditor(Path input, Path output) throws Exception {
        ClassNode node = read(input);
        MethodNode initializer = method(node, "<clinit>");
        replaceArray(initializer, "BASE_X",
                new int[] {112, 165, 218, 112, 165, 218, 138, 202});
        replaceArray(initializer, "BASE_Y",
                new int[] {89, 89, 89, 134, 134, 134, 192, 192});
        write(node, output);
    }

    private static ClassNode read(Path input) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(input)).accept(node, 0);
        return node;
    }

    private static void write(ClassNode node, Path output) throws Exception {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        Files.createDirectories(output.getParent());
        Files.write(output, writer.toByteArray());
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(value -> value.name.equals(name)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing method " + name));
    }

    private static void replaceArray(MethodNode method, String fieldName, int[] values) {
        FieldInsnNode target = null;
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.PUTSTATIC
                    && field.name.equals(fieldName)) {
                target = field;
                break;
            }
        }
        if (target == null) {
            throw new IllegalStateException("Missing array field " + fieldName);
        }

        int index = values.length - 1;
        for (AbstractInsnNode instruction = previousReal(target); instruction != null && index >= 0;
             instruction = previousReal(instruction)) {
            if (instruction.getOpcode() == Opcodes.IASTORE) {
                AbstractInsnNode value = previousReal(instruction);
                setInteger(method, value, values[index--]);
            }
        }
        if (index != -1) {
            throw new IllegalStateException(fieldName + ": found only "
                    + (values.length - index - 1) + " array entries");
        }
    }

    private static void replaceConstant(MethodNode method, int oldValue, int newValue,
                                        int expected) {
        int replacements = 0;
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (integer(instruction) == oldValue) {
                setInteger(method, instruction, newValue);
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
                    || load.getOpcode() != Opcodes.ILOAD || load.var != loadVariable) {
                continue;
            }
            AbstractInsnNode constantInstruction = nextReal(instruction);
            AbstractInsnNode addInstruction = nextReal(constantInstruction);
            AbstractInsnNode storeInstruction = nextReal(addInstruction);
            if (integer(constantInstruction) == oldValue
                    && addInstruction != null && addInstruction.getOpcode() == Opcodes.IADD
                    && storeInstruction instanceof VarInsnNode store
                    && store.getOpcode() == Opcodes.ISTORE && store.var == storeVariable) {
                setInteger(method, constantInstruction, newValue);
                replacements++;
            }
        }
        requireCount(method.name, oldValue, newValue, replacements, expected);
    }

    private static int integer(AbstractInsnNode instruction) {
        if (instruction instanceof IntInsnNode value) {
            return value.operand;
        }
        if (instruction instanceof LdcInsnNode value && value.cst instanceof Integer number) {
            return number;
        }
        int opcode = instruction == null ? -1 : instruction.getOpcode();
        return opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5
                ? opcode - Opcodes.ICONST_0 : Integer.MIN_VALUE;
    }

    private static void setInteger(MethodNode method, AbstractInsnNode instruction, int value) {
        AbstractInsnNode replacement;
        if (value >= -1 && value <= 5) {
            replacement = new org.objectweb.asm.tree.InsnNode(Opcodes.ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            replacement = new IntInsnNode(Opcodes.BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            replacement = new IntInsnNode(Opcodes.SIPUSH, value);
        } else {
            replacement = new LdcInsnNode(value);
        }
        method.instructions.set(instruction, replacement);
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction == null ? null : instruction.getNext();
        while (next != null && next.getOpcode() < 0) {
            next = next.getNext();
        }
        return next;
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction == null ? null : instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) {
            previous = previous.getPrevious();
        }
        return previous;
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
