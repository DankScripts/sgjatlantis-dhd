package com.mustangdoc.sgjpatch.compat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Calls the optional mod's own native-gate operations without linking any Lantea classes. */
public final class LanteaNativeJumperBackend {
    record Gate(Object controller, Object id, String nativeGlyphSet, String glyphSet, int symbols) {}
    record Snapshot(String status, int[] encodedSymbols, int[] typedSymbols) {}
    private static final Map<Object, int[]> LAST_ADDRESS = new WeakHashMap<>();
    private static final ThreadLocal<Feedback> FEEDBACK = new ThreadLocal<>();
    private static Api api;

    private static final class Feedback {
        final ServerPlayer player;
        Component message;
        Feedback(ServerPlayer player) { this.player = player; }
    }

    private record Api(Method services, Method nearest, Method guide, Method controller, Method gateId,
                       Method phase, Method state, Method host, Method kind, Method glyphSet,
                       Method glyphSetId, Method glyphSetSize, Method dial, Method close) {}

    private LanteaNativeJumperBackend() {}

    private static Method method(Class<?> type, String name, Class<?>... parameters)
            throws ReflectiveOperationException {
        Method result = type.getDeclaredMethod(name, parameters);
        result.setAccessible(true);
        return result;
    }

    private static Api api() throws ReflectiveOperationException {
        if (api != null) return api;
        Class<?> services = Class.forName("io.github.aqueoxx.lantea.server.LanteaServerServices");
        Class<?> dial = Class.forName("io.github.aqueoxx.lantea.jumper.JumperDial");
        Class<?> jumper = Class.forName("io.github.aqueoxx.lantea.jumper.PuddleJumper");
        Class<?> near = Class.forName("io.github.aqueoxx.lantea.jumper.JumperDial$Near");
        Class<?> controller = Class.forName("io.github.aqueoxx.lantea.server.gate.GateController");
        Class<?> host = Class.forName("io.github.aqueoxx.lantea.server.gate.GateHost");
        Class<?> kind = Class.forName("io.github.aqueoxx.lantea.core.structure.GateKind");
        Class<?> glyphSet = Class.forName("io.github.aqueoxx.lantea.core.id.GlyphSet");
        api = new Api(method(services, "get", MinecraftServer.class),
                method(dial, "nearest", services, ServerLevel.class, Vec3.class),
                method(dial, "guide", services, near, String.class),
                method(near, "controller"), method(controller, "gateId"),
                method(controller, "phase"), method(controller, "state"), method(controller, "host"),
                method(host, "kind"), method(kind, "glyphSet"), method(glyphSet, "id"),
                method(glyphSet, "size"), method(dial, "dial", ServerPlayer.class, jumper, String.class, List.class),
                method(dial, "close", ServerPlayer.class, jumper));
        return api;
    }

    static Optional<Gate> nearest(ServerPlayer player, Entity jumper) throws ReflectiveOperationException {
        Api a = api();
        Optional<?> services = (Optional<?>) a.services.invoke(null, player.getServer());
        if (services.isEmpty()) return Optional.empty();
        Optional<?> near = (Optional<?>) a.nearest.invoke(null, services.get(), player.serverLevel(), jumper.position());
        if (near.isEmpty()) return Optional.empty();
        Object controller = a.controller.invoke(near.get());
        Object glyphSet = a.glyphSet.invoke(a.kind.invoke(a.host.invoke(controller)));
        String id = (String) a.glyphSetId.invoke(glyphSet);
        String displaySet = switch (id) { case "pg" -> "pegasus"; case "mw" -> "milky_way"; default -> id; };
        return Optional.of(new Gate(controller, a.gateId.invoke(controller), id, displaySet,
                ((Number) a.glyphSetSize.invoke(glyphSet)).intValue()));
    }

    static Gate describe(Object controller) throws ReflectiveOperationException {
        Api a = api();
        Object glyphs = a.glyphSet.invoke(a.kind.invoke(a.host.invoke(controller)));
        String id = (String) a.glyphSetId.invoke(glyphs);
        return new Gate(controller, a.gateId.invoke(controller), id,
                id.equals("pg") ? "pegasus" : "milky_way", ((Number) a.glyphSetSize.invoke(glyphs)).intValue());
    }

    static boolean valid(ServerPlayer player, Entity jumper, Gate target) throws ReflectiveOperationException {
        Optional<Gate> current = nearest(player, jumper);
        return current.isPresent() && current.get().id.equals(target.id)
                && current.get().controller == target.controller;
    }

    /** Ask Lantea's own jumper guide to translate and validate a pasted address. */
    static int[] guide(ServerPlayer player, Entity jumper, String address) throws ReflectiveOperationException {
        Api a=api();
        Optional<?> services=(Optional<?>)a.services.invoke(null,player.getServer());
        if(services.isEmpty())throw new IllegalArgumentException("Lantea gate service is unavailable.");
        Optional<?> near=(Optional<?>)a.nearest.invoke(null,services.get(),player.serverLevel(),jumper.position());
        if(near.isEmpty())throw new IllegalArgumentException("No Lantea gate is close enough to the jumper.");
        Object result=a.guide.invoke(null,services.get(),near.get(),address);
        List<?> keys=(List<?>)method(result.getClass(),"keys").invoke(result);
        if(keys.size()!=7) {
            String refusal=(String)method(result.getClass(),"refusal").invoke(result);
            throw new IllegalArgumentException(refusal.isBlank() ? "Lantea cannot guide that address." : refusal);
        }
        return keys.stream().mapToInt(key -> ((Number)key).intValue()).toArray();
    }

    static Snapshot snapshot(Gate gate) throws ReflectiveOperationException {
        Api a = api();
        Object phase = a.phase.invoke(gate.controller);
        String state = ((Enum<?>) a.state.invoke(gate.controller)).name();
        String status = switch (state) {
            case "IDLE" -> "Ready";
            case "ENCODING", "DIALING", "LOCKING" -> "Dialing";
            case "ESTABLISHING" -> "Opening";
            case "CONNECTED" -> "Connected";
            case "INCOMING" -> "Incoming";
            case "CLOSING" -> "Closing";
            case "RESETTING" -> "Resetting";
            default -> "Unavailable";
        };
        int[] encoded = new int[0];
        int[] typed = new int[0];
        if (state.equals("ENCODING")) {
            encoded = prefix(call(phase, "symbols"), ((Number) call(phase, "chevronsLocked")).intValue());
            Optional<?> plan = (Optional<?>) call(phase, "plan");
            if (plan.isPresent()) {
                typed = prefix(call(plan.get(), "sequence"), 9);
                LAST_ADDRESS.put(gate.controller, typed);
                encoded = Arrays.copyOf(typed, Math.min(typed.length, ((Number)call(phase,"chevronsLocked")).intValue()));
                if (!Boolean.TRUE.equals(call(phase,"engaged"))) status="Awaiting activation";
            } else {
                typed = prefix(call(phase,"symbols"), 8);
                status="Entering address";
            }
        } else if (state.equals("DIALING") || state.equals("LOCKING")) {
            Object sequence = call(call(phase, "plan"), "sequence");
            LAST_ADDRESS.put(gate.controller, prefix(sequence, 9));
            encoded = prefix(sequence, ((Number) call(phase, "chevronsLocked")).intValue());
        } else if (state.equals("ESTABLISHING")) {
            encoded = prefix(call(call(phase, "plan"), "sequence"), 9);
            LAST_ADDRESS.put(gate.controller, encoded);
        } else if (state.equals("CONNECTED")) {
            // The native Connected phase keeps the chevron count but drops the dial plan.
            encoded = LAST_ADDRESS.getOrDefault(gate.controller, new int[0]);
        }
        if (state.equals("IDLE") || state.equals("RESETTING") || state.equals("CLOSING") || state.equals("FAULT"))
            LAST_ADDRESS.remove(gate.controller);
        return new Snapshot(status, encoded.clone(), typed);
    }

    private static Object call(Object target, String name) throws ReflectiveOperationException {
        return method(target.getClass(), name).invoke(target);
    }

    private static int[] prefix(Object value, int count) {
        List<?> values = (List<?>) value;
        return values.stream().limit(Math.max(0, Math.min(9, count)))
                .mapToInt(v -> ((Number) v).intValue()).toArray();
    }

    static Component command(ServerPlayer player, Entity jumper, Gate gate, int action, String input)
            throws ReflectiveOperationException {
        if (action == 1 || action == 5) {
            // The bridge still checks native occupancy/gene/crystals and the nearest gate
            // on every request. Preserve JumperDial's surge check before input AND activation.
            Class<?> dial=Class.forName("io.github.aqueoxx.lantea.jumper.JumperDial");
            if (Boolean.TRUE.equals(method(dial,"inSurge",gate.controller.getClass(),jumper.getClass())
                    .invoke(null,gate.controller,jumper))) return Component.translatable("lantea.jumper.dial.surge");
            return action==1 ? AtlantisNativeDhd.activate(player,gate)
                    : AtlantisNativeDhd.pressSymbol(player,gate,Integer.parseInt(input),6);
        }
        if(action==2)return AtlantisNativeDhd.close(player,gate);
        return Component.empty();
    }

    /** Called by the optional JumperDial mixin; leaves native chat feedback intact. */
    public static void captureFeedback(ServerPlayer player, String translationKey, Object[] args) {
        Feedback feedback = FEEDBACK.get();
        if (feedback != null && feedback.player == player)
            feedback.message = Component.translatable(translationKey, args);
    }
}
