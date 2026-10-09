var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Handle = Java.type('org.objectweb.asm.Handle');
var Type = Java.type('org.objectweb.asm.Type');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var FrameNode = Java.type('org.objectweb.asm.tree.FrameNode');
var InvokeDynamicInsnNode = Java.type('org.objectweb.asm.tree.InvokeDynamicInsnNode');
var LdcInsnNode = Java.type('org.objectweb.asm.tree.LdcInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var MethodNode = Java.type('org.objectweb.asm.tree.MethodNode');
var MultiANewArrayInsnNode = Java.type('org.objectweb.asm.tree.MultiANewArrayInsnNode');
var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode');

var LEGACY_MENU = 'com/struxnet/sgjadditions/common/menu/AtlantisDHDMenu';
var COMPAT_MENU = 'com/mustangdoc/sgjpatch/menu/AtlantisDHDCompatMenu';
var LEGACY_MENU_DESCRIPTOR = 'L' + LEGACY_MENU + ';';
var COMPAT_MENU_DESCRIPTOR = 'L' + COMPAT_MENU + ';';
var ACCEPTED_MENU = 'net/povstalec/sgjourney/common/menu/AbstractDHDMenu';
var CURRENT_MENU = 'net/povstalec/sgjourney/common/menu/dhd/AbstractDHDMenu';
var ACCEPTED_BIG_BUTTON = 'net/povstalec/sgjourney/client/widgets/dhd/DHDBigButton$Pegasus';
var COMPAT_BIG_BUTTON = 'com/mustangdoc/sgjpatch/client/widgets/AtlantisPegasusBigButton';

function remapDescriptor(descriptor) {
    return descriptor.split(LEGACY_MENU_DESCRIPTOR).join(COMPAT_MENU_DESCRIPTOR);
}

function remapAcceptedInternalName(internalName) {
    if (internalName === ACCEPTED_MENU) {
        return CURRENT_MENU;
    }
    if (internalName === ACCEPTED_BIG_BUTTON) {
        return COMPAT_BIG_BUTTON;
    }
    return internalName;
}

function remapAcceptedDescriptor(descriptor) {
    if (descriptor === null) {
        return null;
    }
    return descriptor
        .split('L' + ACCEPTED_MENU + ';').join('L' + CURRENT_MENU + ';')
        .split('L' + ACCEPTED_BIG_BUTTON + ';').join('L' + COMPAT_BIG_BUTTON + ';');
}

function remapAcceptedLayoutField(fieldName, descriptor) {
    if (descriptor !== 'I') {
        return fieldName;
    }
    if (fieldName === 'f_97726_') {
        return 'imageWidth';
    }
    if (fieldName === 'f_97727_') {
        return 'imageHeight';
    }
    if (fieldName === 'f_97735_') {
        return 'leftPos';
    }
    if (fieldName === 'f_97736_') {
        return 'topPos';
    }
    return fieldName;
}

function remapAcceptedHandle(handle) {
    return new Handle(
        handle.getTag(),
        remapAcceptedInternalName(handle.getOwner()),
        handle.getName(),
        remapAcceptedDescriptor(handle.getDesc()),
        handle.isInterface());
}

function remapFrameEntries(entries) {
    if (entries === null) {
        return;
    }
    for (var entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
        var entry = entries.get(entryIndex);
        if (entry === ACCEPTED_MENU || entry === ACCEPTED_BIG_BUTTON) {
            entries.set(entryIndex, remapAcceptedInternalName(String(entry)));
        }
    }
}

function remapAcceptedScreenClass(classNode) {
    classNode.signature = remapAcceptedDescriptor(classNode.signature);

    for (var innerIndex = classNode.innerClasses.size() - 1; innerIndex >= 0; innerIndex--) {
        if (classNode.innerClasses.get(innerIndex).name === ACCEPTED_BIG_BUTTON) {
            classNode.innerClasses.remove(innerIndex);
        }
    }

    for (var fieldIndex = 0; fieldIndex < classNode.fields.size(); fieldIndex++) {
        var field = classNode.fields.get(fieldIndex);
        field.desc = remapAcceptedDescriptor(field.desc);
        field.signature = remapAcceptedDescriptor(field.signature);
    }

    var replacements = 0;
    var backgroundReplacements = 0;
    for (var methodIndex = 0; methodIndex < classNode.methods.size(); methodIndex++) {
        var method = classNode.methods.get(methodIndex);
        var remappedMethodDescriptor = remapAcceptedDescriptor(method.desc);
        if (remappedMethodDescriptor !== method.desc) {
            method.desc = remappedMethodDescriptor;
            replacements++;
        }
        method.signature = remapAcceptedDescriptor(method.signature);

        if (method.localVariables !== null) {
            for (var localIndex = 0; localIndex < method.localVariables.size(); localIndex++) {
                var local = method.localVariables.get(localIndex);
                local.desc = remapAcceptedDescriptor(local.desc);
                local.signature = remapAcceptedDescriptor(local.signature);
            }
        }

        var iterator = method.instructions.iterator();
        while (iterator.hasNext()) {
            var instruction = iterator.next();
            if (String(method.name) === 'm_7379_'
                    && instruction instanceof MethodInsnNode
                    && String(instruction.owner) === 'net/povstalec/sgjourney/client/screens/dhd/AbstractDHDScreen'
                    && String(instruction.name) === 'm_7379_') {
                instruction.setOpcode(Opcodes.INVOKEVIRTUAL);
                instruction.name = 'sgjpatch$closeMenu';
                replacements++;
            }
                if (String(method.name) === 'sgjpatch$renderNormal'
                    && instruction instanceof MethodInsnNode
                    && String(instruction.owner) === 'net/povstalec/sgjourney/client/screens/dhd/AbstractDHDScreen'
                    && String(instruction.name) === 'm_88315_'
                    && String(instruction.desc) === '(Lnet/minecraft/client/gui/GuiGraphics;IIF)V') {
                var background = new InsnList();
                background.add(new VarInsnNode(Opcodes.ALOAD, 0));
                background.add(new VarInsnNode(Opcodes.ALOAD, 1));
                background.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                    classNode.name, 'm_280273_', '(Lnet/minecraft/client/gui/GuiGraphics;)V', false));
                background.add(new VarInsnNode(Opcodes.ALOAD, 0));
                background.add(new VarInsnNode(Opcodes.ALOAD, 1));
                background.add(new VarInsnNode(Opcodes.FLOAD, 4));
                background.add(new VarInsnNode(Opcodes.ILOAD, 2));
                background.add(new VarInsnNode(Opcodes.ILOAD, 3));
                background.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                    classNode.name, 'm_7286_', '(Lnet/minecraft/client/gui/GuiGraphics;FII)V', false));
                method.instructions.insertBefore(method.instructions.getFirst(), background);
                instruction.setOpcode(Opcodes.INVOKEVIRTUAL);
                instruction.name = 'sgjpatch$renderWidgets';
                method.maxStack = Math.max(method.maxStack, 5);
                replacements++;
                backgroundReplacements++;
            }
            if (instruction instanceof FieldInsnNode || instruction instanceof MethodInsnNode) {
                var remappedOwner = remapAcceptedInternalName(instruction.owner);
                var remappedInstructionDescriptor = remapAcceptedDescriptor(instruction.desc);
                var remappedFieldName = instruction instanceof FieldInsnNode
                    ? remapAcceptedLayoutField(instruction.name, instruction.desc)
                    : instruction.name;
                if (remappedOwner !== instruction.owner
                        || remappedInstructionDescriptor !== instruction.desc
                        || remappedFieldName !== instruction.name) {
                    instruction.owner = remappedOwner;
                    instruction.desc = remappedInstructionDescriptor;
                    instruction.name = remappedFieldName;
                    replacements++;
                }
            } else if (instruction instanceof TypeInsnNode) {
                var remappedType = remapAcceptedInternalName(instruction.desc);
                if (remappedType !== instruction.desc) {
                    instruction.desc = remappedType;
                    replacements++;
                }
            } else if (instruction instanceof InvokeDynamicInsnNode) {
                instruction.desc = remapAcceptedDescriptor(instruction.desc);
                for (var argumentIndex = 0; argumentIndex < instruction.bsmArgs.length; argumentIndex++) {
                    var argument = instruction.bsmArgs[argumentIndex];
                    if (argument instanceof Handle) {
                        instruction.bsmArgs[argumentIndex] = remapAcceptedHandle(argument);
                    } else if (argument instanceof Type) {
                        instruction.bsmArgs[argumentIndex] = Type.getType(
                            remapAcceptedDescriptor(argument.getDescriptor()));
                    }
                }
            } else if (instruction instanceof LdcInsnNode && instruction.cst instanceof Type) {
                instruction.cst = Type.getType(remapAcceptedDescriptor(instruction.cst.getDescriptor()));
            } else if (instruction instanceof MultiANewArrayInsnNode) {
                instruction.desc = remapAcceptedDescriptor(instruction.desc);
            } else if (instruction instanceof FrameNode) {
                remapFrameEntries(instruction.local);
                remapFrameEntries(instruction.stack);
            }
        }
    }

    if (String(classNode.name) === 'com/mustangdoc/sgjpatch/client/screens/AtlantisDHDScreenFixed') {
        var guardedClose = new MethodNode(Opcodes.ACC_PUBLIC,
            'sgjpatch$closeAfterEngage', '()V', null, null);
        var closeAllowed = new LabelNode();
        guardedClose.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        guardedClose.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name,
            'm_6262_', '()Lnet/minecraft/world/inventory/AbstractContainerMenu;', false));
        guardedClose.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, CURRENT_MENU));
        guardedClose.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CURRENT_MENU,
            'getDHD', '()Lnet/povstalec/sgjourney/common/block_entities/dhd/AbstractDHDEntity;', false));
        guardedClose.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
            'net/povstalec/sgjourney/common/block_entities/dhd/AbstractDHDEntity',
            'getAddress', '()Lnet/povstalec/sgjourney/common/sgjourney/Address;', false));
        guardedClose.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
            'net/povstalec/sgjourney/common/sgjourney/Address', 'regularSymbolCount', '()I', false));
        guardedClose.instructions.add(new LdcInsnNode(Java.to([6], 'int[]')[0]));
        guardedClose.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPGE, closeAllowed));
        guardedClose.instructions.add(new InsnNode(Opcodes.RETURN));
        guardedClose.instructions.add(closeAllowed);
        guardedClose.instructions.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        guardedClose.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        guardedClose.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name,
            'm_7379_', '()V', false));
        guardedClose.instructions.add(new InsnNode(Opcodes.RETURN));
        guardedClose.maxStack = 2;
        guardedClose.maxLocals = 1;
        classNode.methods.add(guardedClose);
    }
    if (String(classNode.name) === 'com/mustangdoc/sgjpatch/client/screens/AtlantisDHDScreenFixed'
            && backgroundReplacements !== 1) {
        throw new Error('Expected one accepted Atlantis background render bridge; found ' + backgroundReplacements);
    }
    if (replacements < 1) {
        throw new Error('No SGJourney 0.6.50 references were remapped in ' + classNode.name);
    }
    ASMAPI.log('INFO', '[SGJPATCH] Remapped accepted Atlantis DHD UI bytecode for SGJourney 0.6.50: ' + classNode.name);
    return classNode;
}

function initializeCoreMod() {
    return {
        'guard_atlantis_engage_close': {
            'target': {
                'type': 'CLASS',
                'name': 'com.mustangdoc.sgjpatch.compat.AtlantisEngageCloseHelper'
            },
            'transformer': function(classNode) {
                var replacements = 0;
                for (var methodIndex = 0; methodIndex < classNode.methods.size(); methodIndex++) {
                    var method = classNode.methods.get(methodIndex);
                    if (String(method.name) !== 'press') {
                        continue;
                    }
                    var iterator = method.instructions.iterator();
                    while (iterator.hasNext()) {
                        var instruction = iterator.next();
                        if (instruction instanceof LdcInsnNode && String(instruction.cst) === 'm_7379_') {
                            instruction.cst = 'sgjpatch$closeAfterEngage';
                            replacements++;
                        }
                    }
                }
                if (replacements !== 1) {
                    throw new Error('Expected one Atlantis engage close callback; found ' + replacements);
                }
                return classNode;
            }
        },
        'bridge_atlantis_widget_render': {
            'target': {
                'type': 'CLASS',
                'name': 'net.povstalec.sgjourney.client.screens.dhd.AbstractDHDScreen'
            },
            'transformer': function(classNode) {
                var bridge = new MethodNode(Opcodes.ACC_PUBLIC,
                    'sgjpatch$renderWidgets', '(Lnet/minecraft/client/gui/GuiGraphics;IIF)V', null, null);
                bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                bridge.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
                bridge.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
                bridge.instructions.add(new VarInsnNode(Opcodes.FLOAD, 4));
                bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                    'net/povstalec/sgjourney/client/screens/SGJourneyMenuScreen',
                    'm_88315_', '(Lnet/minecraft/client/gui/GuiGraphics;IIF)V', false));
                bridge.instructions.add(new InsnNode(Opcodes.RETURN));
                bridge.maxStack = 5;
                bridge.maxLocals = 5;
                classNode.methods.add(bridge);
                var close = new MethodNode(Opcodes.ACC_PUBLIC,
                    'sgjpatch$closeMenu', '()V', null, null);
                close.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    'net/minecraft/client/Minecraft', 'm_91087_', '()Lnet/minecraft/client/Minecraft;', false));
                close.instructions.add(new FieldInsnNode(Opcodes.GETFIELD,
                    'net/minecraft/client/Minecraft', 'f_91074_', 'Lnet/minecraft/client/player/LocalPlayer;'));
                close.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                    'net/minecraft/client/player/LocalPlayer', 'm_6915_', '()V', false));
                close.instructions.add(new InsnNode(Opcodes.RETURN));
                close.maxStack = 1;
                close.maxLocals = 1;
                classNode.methods.add(close);
                ASMAPI.log('INFO', '[SGJPATCH] Added Atlantis widgets-only render bridge');
                return classNode;
            }
        },
        'redirect_atlantis_menu_factory': {
            'target': {
                'type': 'CLASS',
                'name': 'com.struxnet.sgjadditions.common.init.MenuInit'
            },
            'transformer': function(classNode) {
                var handleReplacements = 0;
                var typeReplacements = 0;
                for (var methodIndex = 0; methodIndex < classNode.methods.size(); methodIndex++) {
                    var method = classNode.methods.get(methodIndex);
                    var iterator = method.instructions.iterator();
                    while (iterator.hasNext()) {
                        var instruction = iterator.next();
                        if (!(instruction instanceof InvokeDynamicInsnNode)) {
                            continue;
                        }

                        instruction.desc = remapDescriptor(instruction.desc);
                        for (var argumentIndex = 0; argumentIndex < instruction.bsmArgs.length; argumentIndex++) {
                            var argument = instruction.bsmArgs[argumentIndex];
                            if (argument instanceof Handle) {
                                var remappedOwner = argument.getOwner() === LEGACY_MENU
                                    ? COMPAT_MENU
                                    : argument.getOwner();
                                var remappedHandleDescriptor = remapDescriptor(argument.getDesc());
                                if (remappedOwner !== argument.getOwner()
                                        || remappedHandleDescriptor !== argument.getDesc()) {
                                    instruction.bsmArgs[argumentIndex] = new Handle(
                                        argument.getTag(),
                                        remappedOwner,
                                        argument.getName(),
                                        remappedHandleDescriptor,
                                        argument.isInterface());
                                    handleReplacements++;
                                }
                            } else if (argument instanceof Type) {
                                var remappedTypeDescriptor = remapDescriptor(argument.getDescriptor());
                                if (remappedTypeDescriptor !== argument.getDescriptor()) {
                                    instruction.bsmArgs[argumentIndex] = Type.getType(remappedTypeDescriptor);
                                    typeReplacements++;
                                }
                            }
                        }
                    }
                }
                if (handleReplacements !== 1 || typeReplacements < 1) {
                    throw new Error('Expected one Atlantis menu handle and at least one method type; found '
                        + handleReplacements + ' handle(s) and ' + typeReplacements + ' type(s)');
                }
                ASMAPI.log('INFO', '[SGJPATCH] Redirected SGJourney Additions Atlantis menu factory and lambda types to the 0.6.50 compatibility menu');
                return classNode;
            }
        },
        'disable_legacy_atlantis_screen_registration': {
            'target': {
                'type': 'CLASS',
                'name': 'com.struxnet.sgjadditions.SGJAdditions$ClientModEvents'
            },
            'transformer': function(classNode) {
                for (var methodIndex = 0; methodIndex < classNode.methods.size(); methodIndex++) {
                    var method = classNode.methods.get(methodIndex);
                    if (method.name === 'onClientSetup') {
                        method.instructions.clear();
                        method.tryCatchBlocks.clear();
                        if (method.localVariables !== null) {
                            method.localVariables.clear();
                        }
                        method.instructions.add(new InsnNode(Opcodes.RETURN));
                        method.maxStack = 0;
                        ASMAPI.log('INFO', '[SGJPATCH] Disabled obsolete SGJourney Additions Atlantis screen registration');
                        return classNode;
                    }
                }
                throw new Error('SGJourney Additions ClientModEvents.onClientSetup was not found');
            }
        },
        'remap_accepted_atlantis_screen': {
            'target': {
                'type': 'CLASS',
                'name': 'com.mustangdoc.sgjpatch.client.screens.AtlantisDHDScreenFixed'
            },
            'transformer': remapAcceptedScreenClass
        },
        'remap_accepted_atlantis_symbol_button': {
            'target': {
                'type': 'CLASS',
                'name': 'com.mustangdoc.sgjpatch.client.screens.AtlantisDHDScreenFixed$AtlantisTriangleSymbolButton'
            },
            'transformer': remapAcceptedScreenClass
        },
        'remap_accepted_atlantis_center_button': {
            'target': {
                'type': 'CLASS',
                'name': 'com.mustangdoc.sgjpatch.client.screens.AtlantisDHDScreenFixed$AtlantisTriangleCenterButton'
            },
            'transformer': remapAcceptedScreenClass
        },
        'synchronize_kubejs_generated_data_reads': {
            'target': {
                'type': 'CLASS',
                'name': 'dev.latvian.mods.kubejs.script.data.GeneratedData'
            },
            'transformer': function(classNode) {
                var replacements = 0;
                for (var methodIndex = 0; methodIndex < classNode.methods.size(); methodIndex++) {
                    var method = classNode.methods.get(methodIndex);
                    if (method.name === 'get' && method.desc === '()Ljava/io/InputStream;') {
                        method.access |= Opcodes.ACC_SYNCHRONIZED;
                        replacements++;
                    }
                }
                if (replacements !== 1) {
                    throw new Error('Expected one KubeJS GeneratedData.get method; found ' + replacements);
                }
                ASMAPI.log('INFO', '[SGJPATCH] Synchronized KubeJS generated data reads to prevent parallel reload cache races');
                return classNode;
            }
        }
    };
}