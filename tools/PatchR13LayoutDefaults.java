import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Bakes the user-approved R13 F8 positions into the already-tested R12 runtime classes. */
public final class PatchR13LayoutDefaults {
    private PatchR13LayoutDefaults() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: <layout-in> <layout-out> <editor-in> <editor-out>");
        }

        patchLayout(Path.of(args[0]), Path.of(args[1]));
        patchEditor(Path.of(args[2]), Path.of(args[3]));
        System.out.println("Baked R13 crystal/bar and minigate defaults into R12 runtime classes.");
    }

    private static void patchLayout(Path input, Path output) throws Exception {
        ClassNode node = read(input);
        replaceSwitchValue(method(node, "dx"), 1000, -28, -29);
        replaceSwitchValue(method(node, "dy"), 1000, 24, -16);
        write(node, output);
    }

    private static void patchEditor(Path input, Path output) throws Exception {
        ClassNode node = read(input);
        MethodNode initializer = method(node, "<clinit>");
        replaceArray(initializer, "DEFAULT_DX",
                new int[] {-42, -42, -42, -42, -42, -42, -24, -22});
        replaceArray(initializer, "DEFAULT_DY",
                new int[] {-19, -19, -19, -17, -17, -17, 32, 32});
        write(node, output);
    }

    private static void replaceSwitchValue(MethodNode method, int key,
                                           int oldValue, int newValue) {
        LookupSwitchInsnNode lookup = null;
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof LookupSwitchInsnNode candidate
                    && candidate.keys.contains(key)) {
                lookup = candidate;
                break;
            }
        }
        if (lookup == null) {
            throw new IllegalStateException(method.name + ": missing switch key " + key);
        }

        int index = lookup.keys.indexOf(key);
        LabelNode target = lookup.labels.get(index);
        for (AbstractInsnNode instruction = nextReal(target); instruction != null;
             instruction = nextReal(instruction)) {
            int value = integer(instruction);
            if (value != Integer.MIN_VALUE) {
                if (value != oldValue) {
                    throw new IllegalStateException(method.name + "(" + key + "): expected "
                            + oldValue + ", found " + value);
                }
                setInteger(method, instruction, newValue);
                return;
            }
            if (instruction.getOpcode() == Opcodes.IRETURN) {
                break;
            }
        }
        throw new IllegalStateException(method.name + "(" + key + "): missing return value");
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
            replacement = new InsnNode(Opcodes.ICONST_0 + value);
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
}
