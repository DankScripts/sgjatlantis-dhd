package com.mustangdoc.sgjpatch.mixin;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public final class DevelopmentMappingsCheck {
    public static void main(String[] args) throws Exception {
        for (boolean development : new boolean[]{false, true}) {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
            if (engine == null) throw new AssertionError("Forge's Nashorn script engine is unavailable");
            engine.put("development", development);
            engine.eval("""
                    var originalJava = Java;
                    var methodNames = {
                        m_88315_: 'render', m_91087_: 'getInstance', m_6915_: 'closeContainer'
                    };
                    Java = { type: function(name) {
                        if (name === 'net.minecraftforge.coremod.api.ASMAPI')
                            return {
                                mapMethod: function(name) { return development ? methodNames[name] : name; },
                                mapField: function(name) { return development ? 'player' : name; },
                                log: function() {}
                            };
                        if (name.indexOf('org.objectweb.asm.') !== 0)
                            throw new Error('Unsupported coremod Java.type access: ' + name);
                        return originalJava.type(name);
                    }};
                    """);
            engine.eval(Files.readString(Path.of(args[0])));
            engine.eval("""
                    var transforms = initializeCoreMod();
                    if (transforms.redirect_atlantis_menu_factory.target.name !==
                            'com.struxnet.sgjadditions.common.init.MenuInit' ||
                            transforms.disable_legacy_atlantis_screen_registration.target.name !==
                            'com.struxnet.sgjadditions.SGJAdditions$ClientModEvents')
                        throw new Error('Additions patches must target only optional Additions classes');
                    var node = new (Java.type('org.objectweb.asm.tree.ClassNode'))();
                    transforms.bridge_atlantis_widget_render.transformer(node);
                    var names = {};
                    for (var i = 0; i < node.methods.size(); i++) {
                        var instructions = node.methods.get(i).instructions.iterator();
                        while (instructions.hasNext()) {
                            var instruction = instructions.next();
                            if (instruction.name !== undefined) {
                                if (development && /^[mf]_[0-9]+_$/.test(String(instruction.name)))
                                    throw new Error('Unmapped Minecraft reference: ' + instruction.name);
                                names[String(instruction.name)] = true;
                            }
                        }
                    }
                    var expected = development ? ['render', 'getInstance', 'player', 'closeContainer'] :
                            ['m_88315_', 'm_91087_', 'f_91074_', 'm_6915_'];
                    if (expected.some(function(name) { return !names[name]; }))
                        throw new Error('Widgets/close bridge lost required mapped instructions');
                    """);
        }
        System.out.println("PASS: optional Additions targets and development/production bridges.");
    }
}
