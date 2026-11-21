package com.mustangdoc.sgjpatch.compat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.sgjourney.Dialing;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import net.povstalec.sgjourney.api.stargate.BlockEntityStargate;

/** Server-authoritative bridge for the optional Lantea 0.7.2 jumper DHD. */
@EventBusSubscriber(modid = "sgjatlantis_dhd")
public final class LanteaJumperBridge {
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();
    private static Method mayUse;
    private static boolean reflectionFailed;
    private static final double RANGE_SQUARED = 64.0 * 64.0;


    private static final class Session {
        final UUID token = UUID.randomUUID();
        final UUID jumper;
        final AbstractStargateEntity<?> gate;
        final LanteaNativeJumperBackend.Gate nativeGate;
        long expires, nextPoll, nextAction;

        boolean screenOpen = true;
        boolean wasConnected;
        Session(Entity jumper, AbstractStargateEntity<?> gate, long now) {
            this.jumper = jumper.getUUID(); this.gate = gate; this.nativeGate = null; expires = now + 6000;
        }
        Session(Entity jumper, LanteaNativeJumperBackend.Gate gate, long now) {
            this.jumper = jumper.getUUID(); this.gate = null; this.nativeGate = gate; expires = now + 6000;
        }
    }
    private LanteaJumperBridge() {}

    private static boolean authorized(ServerPlayer player, Entity jumper) {
        if (reflectionFailed || player.isSpectator() || !player.isAlive() || jumper == null
                || !jumper.isAlive() || jumper.level() != player.level()
                || !jumper.getClass().getName().equals("io.github.aqueoxx.lantea.jumper.PuddleJumper")) return false;
        try {
            if (mayUse == null) {
                Class<?> dial = Class.forName("io.github.aqueoxx.lantea.jumper.JumperDial");
                mayUse = dial.getDeclaredMethod("mayUse", ServerPlayer.class, jumper.getClass());
                mayUse.setAccessible(true);
            }
            // Lantea checks occupancy, Ancient gene, and installed DHD crystal.
            return Boolean.TRUE.equals(mayUse.invoke(null, player, jumper));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            reflectionFailed = true;
            System.err.println("[Atlantis DHD] Lantea jumper compatibility disabled: " + ex);
            return false;
        }
    }

    public static boolean open(ServerPlayer player, Object candidate) {
        if (!(candidate instanceof Entity jumper) || !authorized(player, jumper)) return false;
        // A native gate keeps native address semantics and has the same priority as Lantea's DHD.
        // Only the presentation changes when the server enables our optional native-gate GUI.
        try {
            var nativeGate = LanteaNativeJumperBackend.nearest(player, jumper);
            if (nativeGate.isPresent()) {
                if (!LanteaJumperConfig.useAtlantisForLantea()) return false;
                var session = new Session(jumper, nativeGate.get(), player.serverLevel().getGameTime());
                SESSIONS.put(player, session);
                send(player, session, true, Component.literal("Select a Lantea address."));
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            disableNativeCompatibility(ex);
            return false;
        }
        AbstractStargateEntity<?> nearest = null;
        double distance = RANGE_SQUARED;
        for (var gate : StargateNetwork.get(player.serverLevel()).getStargatesInDimension(player.level().dimension())) {
            if (!(gate instanceof BlockEntityStargate<?> blockGate)
                    || !player.serverLevel().hasChunkAt(blockGate.getBlockPos())) continue;
            double d = gate.getPosition().distanceToSqr(jumper.position());
            if (d > distance) continue;
            if (blockGate.getStargateBlockEntity() instanceof AbstractStargateEntity<?> be) {
                nearest = be; distance = d;
            }
        }
        if (nearest == null) return false;
        Session existing = SESSIONS.get(player);
        if (existing != null && existing.gate == nearest && existing.jumper.equals(jumper.getUUID())
                ) {
            existing.screenOpen = true;
            existing.expires = player.serverLevel().getGameTime() + 6000;
            send(player, existing, true, Component.literal("Dialing sequence in progress."));
            return true;
        }
        var session = new Session(jumper, nearest, player.serverLevel().getGameTime());
        SESSIONS.put(player, session);
        send(player, session, true, Component.literal("Select an SGJourney address."));
        return true;
    }

    private static void disableNativeCompatibility(Exception ex) {
        if (!reflectionFailed) System.err.println("[Atlantis DHD] Optional Lantea jumper integration unavailable: " + ex);
        reflectionFailed = true;
    }

    public static void command(ServerPlayer player, UUID token, int action, String address) {
        Session session = SESSIONS.get(player);
        if (session == null || !session.token.equals(token)) return;
        if (action == 3) {
            session.screenOpen = false;
            // The physical keypad must keep following the SGJourney gate after
            // the GUI closes, including its eventual return to idle.
            if (session.gate == null) SESSIONS.remove(player);
            return;
        }
        long now = player.serverLevel().getGameTime();
        // Background status polling must never swallow a click on Dial/Disconnect.
        if (action == 0) {
            if (now < session.nextPoll) return;
            session.nextPoll = now + 1;
        } else if (action != 4) {
            if (now < session.nextAction) { send(player, session, false, Component.literal("Please retry that button.")); return; }
            session.nextAction = now + 1;
        }
        Entity jumper = player.serverLevel().getEntity(session.jumper);
        if (now > session.expires || !authorized(player, jumper)) {
            lost(player, session);
            return;
        }
        session.expires = now + 6000;
        if (session.nativeGate != null) {
            try {
                if (!LanteaJumperConfig.useAtlantisForLantea()
                        || !LanteaNativeJumperBackend.valid(player, jumper, session.nativeGate)) {
                    lost(player, session);
                    return;
                }
                if (action == 4) {
                    int[] keys;
                    String error="";
                    try { keys=LanteaNativeJumperBackend.guide(player,jumper,address); }
                    catch(IllegalArgumentException ex) { keys=new int[0]; error=ex.getMessage(); }
                    send(player, session, false, Component.empty());
                    LanteaJumperNetwork.sendGuide(player, session.token, address, keys, error);
                    return;
                }
                if (action != 0 && action != 1 && action != 2 && action != 5) return;
                send(player, session, false,
                        LanteaNativeJumperBackend.command(player, jumper, session.nativeGate, action, address));
            } catch (IllegalArgumentException ex) {
                send(player,session,false,Component.literal(ex.getMessage()));
            } catch (ReflectiveOperationException | RuntimeException ex) {
                disableNativeCompatibility(ex);
                lost(player, session);
            }
            return;
        }
        var gate = session.gate;
        if (!validJourneyGate(player, session, jumper)) {
            lost(player, session);
            return;
        }
        Component message = Component.empty();
        if (action == 4) {
            try {
                int symbol = Integer.parseInt(address);
                if (gate.isSymbolOutOfBounds(symbol)) throw new IllegalArgumentException("Invalid symbol.");
                // Same gate entry point used by AbstractDHDEntity.encodeSymbol.
                message = gate.indirectEngageSymbol(
                        symbol, false, StargateInfo.ChevronSound.ENGAGE).getMessageComponent();
            } catch (IllegalArgumentException ex) { message = Component.literal(ex.getMessage()); }
        } else if (action == 1) {
            if (gate.isConnected()) {
                message = gate.disconnectStargate(StargateInfo.Feedback.CONNECTION_ENDED_BY_DISCONNECT.withInfo()).getMessageComponent();
            } else {
                if (!gate.getEncodedSymbols().containsSymbol(0) && gate.getEncodedSymbols().toArray().length >= 6)
                    gate.indirectEngageSymbol(0, false, StargateInfo.ChevronSound.ENGAGE);
                message = gate.engageStargate(true, Dialing.Action.EXECUTE).getMessageComponent();
            }
        } else if (action == 2) {
            var reason = StargateInfo.Feedback.CONNECTION_ENDED_BY_DISCONNECT.withInfo();
            message = (gate.isConnected() ? gate.disconnectStargate(reason) : gate.resetStargate(reason)).getMessageComponent();
        } else if (action != 0) return;
        send(player, session, false, message);
    }

    @SubscribeEvent
    public static void syncClosedJumperDisplays(ServerTickEvent.Post event) {

        for (var entry : new ArrayList<>(SESSIONS.entrySet())) {
            ServerPlayer player = entry.getKey();
            Session session = entry.getValue();
            if (player == null || session.screenOpen || session.gate == null) continue;
            long now = player.serverLevel().getGameTime();
            if (now < session.nextPoll) continue;
            session.nextPoll = now + 10;
            Entity jumper = player.serverLevel().getEntity(session.jumper);
            if (!validJourneyGate(player, session, jumper)) {
                lost(player, session);
                continue;
            }
            boolean connectionEnded = session.wasConnected
                    && !session.gate.isConnected() && !session.gate.isDialing();
            boolean idle = !session.gate.isConnected() && !session.gate.isDialing()
                    && session.gate.getChevronsEngaged() == 0;
            send(player, session, false, Component.empty());
            if (connectionEnded || idle) SESSIONS.remove(player);
        }
    }

    private static void send(ServerPlayer player, Session session, boolean open, Component message) {
        if (session.nativeGate != null) {
            try {
                var nativeGate = session.nativeGate;
                var state = LanteaNativeJumperBackend.snapshot(nativeGate);
                LanteaJumperNetwork.send(player, new LanteaJumperNetwork.State(session.token, session.jumper, open, true,
                        nativeGate.symbols(), message, state.status(), "lantea", nativeGate.glyphSet(), state.encodedSymbols(), new int[0], -1,state.typedSymbols()));
            } catch (ReflectiveOperationException | RuntimeException ex) {
                disableNativeCompatibility(ex);
                lost(player, session);
            }
            return;
        }
        var gate = session.gate;
        if (gate.isConnected()) session.wasConnected = true;
        boolean finished = session.wasConnected && !gate.isConnected() && !gate.isDialing();
        String status = gate.isConnected() ? "Connected"
                : finished ? "Ready" : gate.isDialing() || gate.getChevronsEngaged() > 0 ? "Dialing" : "Ready";
        LanteaJumperNetwork.send(player, new LanteaJumperNetwork.State(session.token, session.jumper, open, true,
                gate.totalSymbols(), message, status, "sgjourney", journeyGlyphSet(gate), gate.getEncodedSymbols().toArray(), lockedPositions(gate), movingPosition(gate)));
        if (finished) session.wasConnected = false;
    }

    private static int movingPosition(AbstractStargateEntity<?> gate) {
        if (gate instanceof net.povstalec.sgjourney.common.block_entities.stargate.StopMotionStargateEntity<?> animated
                && animated.isSymbolSpinning()) {
            return Math.floorMod(-animated.getCurrentSymbol(), 36);
        }
        return -1;
    }
    private static int[] lockedPositions(AbstractStargateEntity<?> gate) {
        int[] order = gate.getEngagedChevrons();
        int count = Math.min(gate.getAddress().getLength(), order.length);
        int[] positions = new int[count];
        for (int i = 0; i < count; i++) {
            // early_port textures count counterclockwise; SGJourney counts clockwise.
            positions[i] = Math.floorMod(-4 * order[i], 36);
        }
        return positions;
    }
    private static String journeyGlyphSet(AbstractStargateEntity<?> gate) {
        var symbols = gate.symbolInfo().symbols();
        String id = symbols == null ? gate.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT)
                : symbols.location().getPath();
        if (id.contains("pegasus") || id.equals("lantea")) return "pegasus";
        if (id.contains("universe")) return "universe";
        if (id.contains("milky") || id.contains("tollan") || id.contains("classic")) return "milky_way";
        return id;
    }

    private static boolean validJourneyGate(ServerPlayer player, Session session, Entity jumper) {
        var gate = session.gate;
        return jumper != null && gate != null && !gate.isRemoved() && gate.getLevel() == player.level()
                && player.serverLevel().hasChunkAt(gate.getBlockPos())
                && player.serverLevel().getBlockEntity(gate.getBlockPos()) == gate
                && gate.stargateReturn(stargate ->
                        stargate.getPosition().distanceToSqr(jumper.position()) <= RANGE_SQUARED, false);
    }

    private static void lost(ServerPlayer player, Session session) {
        SESSIONS.remove(player);

        LanteaJumperNetwork.send(player, new LanteaJumperNetwork.State(session.token, session.jumper, false, false, 0,
                Component.literal("Connection lost. Return to the jumper near the gate and reopen its DHD."), "Unavailable",
                session.nativeGate == null ? "sgjourney" : "lantea", "pegasus", new int[0], new int[0], -1));
    }

}




