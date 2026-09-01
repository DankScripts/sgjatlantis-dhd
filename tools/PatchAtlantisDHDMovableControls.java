import java.nio.file.Files;
import java.nio.file.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Adds the expanded GUI canvas and the F8-editable table-control overlay. */
public final class PatchAtlantisDHDMovableControls {
    private static final String SCREEN =
            "com/mustangdoc/sgjpatch/client/screens/AtlantisDHDScreenFixed";
    private static final String EDITOR =
            "com/mustangdoc/sgjpatch/client/util/AtlantisTableControlEditor";

    private PatchAtlantisDHDMovableControls() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: <input.class> <output.class>");
        }

        ClassNode node = new ClassNode();
        new ClassReader(Files.readAllBytes(Path.of(args[0]))).accept(node, 0);

        MethodNode init = method(node, "m_7856_");
        replaceConstant(init, 640, 720, 1);
        replaceConstant(init, 320, 360, 1);
        injectInitialize(init);

        MethodNode layout = method(node, "sgjpatch$addAtlantisTableLayout");
        replaceAddedConstant(layout, 1, 270, 310, 4, 1);
        replaceAddedConstant(layout, 2, 56, 76, 5, 1);
        replaceAddedConstant(layout, 1, 74, 114, 15, 1);
        replaceAddedConstant(layout, 2, 71, 91, 16, 1);

        MethodNode render = method(node, "sgjpatch$renderNormal");
        replaceConstant(render, 238, 278, 1);
        injectRender(render);
        injectMouse(method(node, "sgjpatch$mouseClickedNormal"));
        injectKey(method(node, "m_7933_"));

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        Path output = Path.of(args[1]);
        Files.createDirectories(output.getParent());
        Files.write(output, writer.toByteArray());
        System.out.println("Patched Atlantis GUI to 720x360 with eight F8-editable table controls.");
    }

    private static void injectInitialize(MethodNode method) {
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction.getOpcode() == Opcodes.RETURN) {
                InsnList patch = new InsnList();
                patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
                patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC, EDITOR, "initialize",
                        "(Ljava/lang/Object;)V", false));
                method.instructions.insertBefore(instruction, patch);
            }
        }
    }

    private static void injectMouse(MethodNode method) {
        InsnList patch = new InsnList();
        patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
        patch.add(new VarInsnNode(Opcodes.DLOAD, 1));
        patch.add(new VarInsnNode(Opcodes.DLOAD, 3));
        patch.add(new VarInsnNode(Opcodes.ILOAD, 5));
        patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
        patch.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD, SCREEN,
                "f_97735_", "I"));
        patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
        patch.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD, SCREEN,
                "f_97736_", "I"));
        patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC, EDITOR, "mouseClicked",
                "(Ljava/lang/Object;DDIII)V", false));
        method.instructions.insert(patch);
    }

    private static void injectKey(MethodNode method) {
        InsnList patch = new InsnList();
        patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
        patch.add(new VarInsnNode(Opcodes.ILOAD, 1));
        patch.add(new VarInsnNode(Opcodes.ILOAD, 3));
        patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC, EDITOR, "keyPressed",
                "(Ljava/lang/Object;II)V", false));
        method.instructions.insert(patch);
    }

    private static void injectRender(MethodNode method) {
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null;
             instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == Opcodes.INVOKESPECIAL
                    && call.name.equals("m_88315_")) {
                InsnList patch = new InsnList();
                patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
                patch.add(new VarInsnNode(Opcodes.ALOAD, 1));
                patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
                patch.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD, SCREEN,
                        "f_97735_", "I"));
                patch.add(new VarInsnNode(Opcodes.ALOAD, 0));
                patch.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD, SCREEN,
                        "f_97736_", "I"));
                patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC, EDITOR, "render",
                        "(Ljava/lang/Object;Lnet/minecraft/client/gui/GuiGraphics;II)V", false));
                method.instructions.insert(instruction, patch);
                return;
            }
        }
        throw new IllegalStateException("Missing AbstractDHDScreen render call");
    }

    private static MethodNode method(ClassNode node, String name) {
        return node.methods.stream().filter(value -> value.name.equals(name)).findFirst()
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
            if (constantInstruction instanceof IntInsnNode constant
                    && constant.operand == oldValue
                    && addInstruction != null && addInstruction.getOpcode() == Opcodes.IADD
                    && storeInstruction instanceof VarInsnNode store
                    && store.getOpcode() == Opcodes.ISTORE && store.var == storeVariable) {
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
