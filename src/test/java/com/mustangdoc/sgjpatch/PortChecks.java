package com.mustangdoc.sgjpatch;

import com.mustangdoc.sgjpatch.compat.LanteaJumperNetwork;
import com.mustangdoc.sgjpatch.client.util.AtlantisReferencePlate;
import com.mustangdoc.sgjpatch.power.MixedPowerTransfer;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy;

/** Release checks using real NeoForge/Minecraft classes, not mocked networking. */
public final class PortChecks {
    private static com.google.gson.JsonObject recipe(String name) throws java.io.IOException {
        String path = "data/sgjatlantis_dhd/recipe/" + name + ".json";
        try (var stream = PortChecks.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new AssertionError("Missing recipe: " + path);
            try (var reader = new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {
                return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    private static void checkRestoredRecipes() throws java.io.IOException {
        for (String name : List.of("atlantis_dhd", "atlantis_angular_door", "atlantis_hall_door",
                "atlantis_quarters_door", "atlantis_wide_glass_door", "atlantis_door_control",
                "atlantis_door_control_crystal", "atlantis_door_control_damaged",
                "atlantis_transporter_control", "atlantis_transporter_map", "atlantis_transporter_room",
                "jumper_bay_door_table", "jumper_bay_hatch_panel", "jumper_bay_hatch_redstone_port")) {
            var result = recipe(name).getAsJsonObject("result");
            if (!result.has("id") || !result.get("id").getAsString().equals("stargate_atlantis:" + name))
                throw new AssertionError("Incorrect NeoForge recipe result: " + name);
        }
        var dhd = recipe("atlantis_dhd");
        if (!dhd.getAsJsonArray("pattern").toString().equals("[\"TCT\",\"EDE\",\"TXT\"]")
                || !dhd.getAsJsonObject("key").getAsJsonObject("D").get("item").getAsString().equals("sgjourney:pegasus_dhd"))
            throw new AssertionError("Atlantis DHD recipe does not match the Forge recipe");
        var room = recipe("atlantis_transporter_room").getAsJsonObject("key");
        if (!room.getAsJsonObject("C").get("item").getAsString().equals("stargate_atlantis:atlantis_transporter_control")
                || !room.getAsJsonObject("M").get("item").getAsString().equals("stargate_atlantis:atlantis_transporter_map"))
            throw new AssertionError("Transporter recipe must use the registered component items");
        if (recipe("jumper_bay_hatch_panel").getAsJsonObject("result").get("count").getAsInt() != 8)
            throw new AssertionError("Hatch panel recipe must produce eight panels");
        var control = recipe("atlantis_door_control");
        if (!control.get("type").getAsString().equals("minecraft:crafting_shapeless")
                || control.getAsJsonArray("ingredients").size() != 2)
            throw new AssertionError("Door control repair must be shapeless");
        System.out.println("PASS: missing NeoForge recipes and key crafting details are present.");
    }

    private static void checkNamespaceMigration() throws ReflectiveOperationException {
        var registryKey = net.minecraft.resources.ResourceKey.<String>createRegistryKey(
                ResourceLocation.fromNamespaceAndPath("test", "migration"));
        var registry = new net.minecraft.core.MappedRegistry<String>(
                registryKey, com.mojang.serialization.Lifecycle.stable());
        var target = ResourceLocation.fromNamespaceAndPath("stargate_atlantis", "wallpanel");
        var existing = ResourceLocation.fromNamespaceAndPath("sgjatlantis_dhd", "existing");
        net.minecraft.core.Registry.register(registry, existing, "existing");
        net.minecraft.core.Registry.register(registry,
                ResourceLocation.fromNamespaceAndPath("stargate_atlantis", "existing"), "replacement");
        var bus = net.neoforged.bus.api.BusBuilder.builder().build();
        bus.register(AtlantisNamespace.LegacyAliases.class);
        bus.addListener(net.neoforged.bus.api.EventPriority.NORMAL,
                net.neoforged.neoforge.registries.RegisterEvent.class,
                event -> net.minecraft.core.Registry.register(registry, target, "new block"));
        var constructor = net.neoforged.neoforge.registries.RegisterEvent.class.getDeclaredConstructor(
                net.minecraft.resources.ResourceKey.class, net.minecraft.core.Registry.class);
        constructor.setAccessible(true);
        bus.post(constructor.newInstance(net.minecraft.core.registries.Registries.BLOCK, registry));
        for (String legacy : List.of("sgjadditions_capacity_patch", "sgjatlantis_dhd")) {
            var old = ResourceLocation.fromNamespaceAndPath(legacy, "wallpanel");
            if (!"new block".equals(registry.get(old)) || !"new block".equals(registry.getHolder(
                    net.minecraft.resources.ResourceKey.create(registryKey, old)).orElseThrow().value()))
                throw new AssertionError("Legacy registry lookup failed: " + old);
        }
        if (!"existing".equals(registry.get(existing)))
            throw new AssertionError("Migration overwrote an existing registry entry");
        System.out.println("PASS: registry event installs legacy aliases after normal registration.");
    }

    private static Class<?> type(String name) throws Exception {
        return Class.forName(name, false, PortChecks.class.getClassLoader());
    }
    private static void method(String name, String method, String... parameters) throws Exception {
        Class<?>[] types=new Class<?>[parameters.length];
        for(int i=0;i<types.length;i++) types[i]=type(parameters[i]);
        type(name).getDeclaredMethod(method,types);
    }
    private static void checkEventSubscribers() throws Exception {
        var classes=java.nio.file.Path.of(LanteaJumperNetwork.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        int[] checked={0};
        try(var files=java.nio.file.Files.walk(classes)) {
            for(var file:files.filter(p -> p.toString().endsWith(".class")).toList()) {
                boolean[] subscriber={false};
                int[] handlers={0};
                new org.objectweb.asm.ClassReader(java.nio.file.Files.readAllBytes(file)).accept(
                    new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                        @Override public org.objectweb.asm.AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                            if(desc.equals("Lnet/neoforged/fml/common/EventBusSubscriber;")) subscriber[0]=true;
                            return null;
                        }
                        @Override public org.objectweb.asm.MethodVisitor visitMethod(int access,String name,String desc,String signature,String[] exceptions) {
                            return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9) {
                                @Override public org.objectweb.asm.AnnotationVisitor visitAnnotation(String annotation,boolean visible) {
                                    if(annotation.equals("Lnet/neoforged/bus/api/SubscribeEvent;")) {
                                        if((access & org.objectweb.asm.Opcodes.ACC_STATIC)==0) throw new AssertionError("Non-static subscriber: "+file);
                                        handlers[0]++;
                                    }
                                    return null;
                                }
                            };
                        }
                    },org.objectweb.asm.ClassReader.SKIP_CODE);
                if(subscriber[0]) {
                    if(handlers[0]==0) throw new AssertionError("Event subscriber has no handlers: "+file);
                    checked[0]++;
                }
            }
        }
        if(checked[0]<5) throw new AssertionError("Expected five subscriber classes; found "+checked[0]);
        System.out.println("PASS: all "+checked[0]+" compiled event subscribers have static event handlers.");
    }
    private static void checkMemory() throws Exception {
        String root="io.github.aqueoxx.lantea.core.";
        Class<?> address=type(root+"id.GateAddress"), codec=type(root+"id.AddressCodec");
        Class<?> logType=type(root+"dial.ConnectionLog"), entryType=type(root+"dial.ConnectionLog$Entry");
        var parse=codec.getMethod("parse",String.class);
        var constructor=entryType.getConstructor(address,boolean.class,long.class);
        Object log=logType.getMethod("empty").invoke(null), last=null;
        for(int i=1;i<=10;i++) {
            last=parse.invoke(null,String.format("mw:%02d-34-10-38-09-13-31",i+19));
            log=logType.getMethod("record",entryType).invoke(log,constructor.newInstance(last,true,(long)i));
        }
        List<?> entries=(List<?>)logType.getMethod("entries").invoke(log);
        if(entries.size()!=8)throw new AssertionError("Lantea memory capacity");
        Object first=entryType.getMethod("address").invoke(entries.get(0));
        if(!last.equals(first))throw new AssertionError("Memory must be newest first");
        log=logType.getMethod("record",entryType).invoke(log,constructor.newInstance(last,true,20L));
        entries=(List<?>)logType.getMethod("entries").invoke(log);
        if(entries.size()!=8 || (long)entryType.getMethod("gameTime").invoke(entries.get(0))!=20L)
            throw new AssertionError("Repeat address must update, not duplicate");
        var read=LanteaJumperNetwork.class.getDeclaredMethod("readMemory",RegistryFriendlyByteBuf.class);
        read.setAccessible(true);
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        buf.writeVarInt(1); buf.writeUtf((String)codec.getMethod("format",address).invoke(null,last),80);
        String[] restored=(String[])read.invoke(null,buf);
        if(restored.length!=1 || !last.equals(parse.invoke(null,restored[0])))throw new AssertionError("Memory wire format");
        buf.clear(); buf.writeVarInt(9);
        try { read.invoke(null,buf); throw new AssertionError("Oversized memory accepted"); }
        catch(java.lang.reflect.InvocationTargetException expected) {
            if(!(expected.getCause() instanceof IllegalArgumentException))throw expected;
        }
        buf.release();
        System.out.println("PASS: native Lantea memory ordering, capacity, duplicate handling and bounded memory decoding.");
    }
    private static void checkTransporterInterior() {
        var anchor = new BlockPos(40, 80, -30);
        for (var facing : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            var area = com.mustangdoc.sgjpatch.transporter.TransporterRoom.interior(anchor, facing);
            for (int depth=1; depth<=2; depth++) for (int side=-1; side<=1; side++) {
                var pos = com.mustangdoc.sgjpatch.transporter.TransporterRoom.at(anchor,facing,side,0,depth);
                if (!area.contains(net.minecraft.world.phys.Vec3.atCenterOf(pos)))
                    throw new AssertionError("Passenger space omitted next to back screen: " + facing);
            }
            if (area.contains(net.minecraft.world.phys.Vec3.atCenterOf(anchor)))
                throw new AssertionError("Back wall included in passenger space");
        }
        System.out.println("PASS: transporter passenger space includes the exposed screen in all four orientations.");
    }
    private static final class PowerBranch {
        private PowerBranch first;
        private PowerBranch second;

        private long simulate() {
            return MixedPowerTransfer.run(this, 1, true, false, () -> {
                if (first == null) return 0;
                long sent = first.simulate();
                return sent != 0 ? sent : second.simulate();
            });
        }
    }

    private static void checkMixedPower() {
        Object sourceNetwork = new Object();
        Object outputNetwork = new Object();
        java.util.function.LongUnaryOperator splitBetweenTwoOutputs = offer -> (offer / 2) * 2;
        long accepted = MixedPowerTransfer.run(sourceNetwork, 3, false, true, () -> {
            long first = MixedPowerTransfer.run(outputNetwork, 1, false, true,
                    () -> splitBetweenTwoOutputs.applyAsLong(1));
            long second = MixedPowerTransfer.run(outputNetwork, 2, false, true,
                    () -> splitBetweenTwoOutputs.applyAsLong(2));
            return first + second;
        });
        if (accepted != 2)
            throw new AssertionError("A failed small offer must not block a later larger offer to the same network");
        Object root = new Object();
        Object branchA = new Object();
        Object branchB = new Object();
        Object junction = new Object();
        Object nestedJunction = new Object();
        Object primer = new Object();
        long simulated = MixedPowerTransfer.run(root, 10, true, false, () -> {
            long first = MixedPowerTransfer.run(branchA, 10, true, false, () -> {
                MixedPowerTransfer.run(primer, 10, true, false,
                        () -> MixedPowerTransfer.run(nestedJunction, 10, true, false,
                                () -> MixedPowerTransfer.run(branchA, 10, true, false, () -> 10)));
                return MixedPowerTransfer.run(junction, 10, true, false,
                            () -> MixedPowerTransfer.run(nestedJunction, 10, true, false,
                                    () -> MixedPowerTransfer.run(branchA, 10, true, false, () -> 10)));
            });
            long second = MixedPowerTransfer.run(branchB, 10, true, false,
                    () -> MixedPowerTransfer.run(junction, 10, true, false,
                            () -> MixedPowerTransfer.run(nestedJunction, 10, true, false,
                                    () -> MixedPowerTransfer.run(branchA, 10, true, false, () -> 10))));
            return first + second;
        });
        if (simulated != 10)
            throw new AssertionError("A blocked path must not suppress a valid path through the same junction");

        Object output = new Object();
        long[] remaining = {10};
        MixedPowerTransfer.run(root, 10, false, false, () -> {
            long firstProbe = MixedPowerTransfer.run(output, 10, true, false, () -> remaining[0]);
            long committed = MixedPowerTransfer.run(output, 10, false, false, () -> {
                long sent = remaining[0];
                remaining[0] = 0;
                return sent;
            });
            long secondProbe = MixedPowerTransfer.run(output, 10, true, false, () -> remaining[0]);
            if (firstProbe != 10 || committed != 10 || secondProbe != 0)
                throw new AssertionError("Simulation must reflect energy committed earlier in the same transfer");
            return committed;
        });
        PowerBranch[][] branches = new PowerBranch[24][2];
        for (PowerBranch[] layer : branches)
            for (int i = 0; i < layer.length; i++) layer[i] = new PowerBranch();
        for (int i = 0; i < branches.length - 1; i++)
            for (PowerBranch branch : branches[i]) {
                branch.first = branches[i + 1][0];
                branch.second = branches[i + 1][1];
            }
        Object reachableOutput = new Object();
        long reachable = MixedPowerTransfer.run(root, 1, true, false, () -> {
            long first = branches[0][0].simulate();
            return first != 0 ? first
                    : MixedPowerTransfer.run(reachableOutput, 1, true, false, () -> 1);
        });
        if (reachable != 1)
            throw new AssertionError("Many converging dead-end routes must not hide a reachable output");
        for(boolean firstCable:new boolean[]{false,true}) {
            long[] stored={0};
            SGJourneyEnergy target=new SGJourneyEnergy(600,600,0) {
                @Override public long receiveLongEnergy(long amount,boolean simulate){return 0;}
                @Override public long receiveZeroPointEnergy(long amount,boolean simulate){
                    long accepted=Math.min(amount,600-stored[0]);
                    if(!simulate)stored[0]+=accepted;
                    return accepted;
                }
                @Override public void onEnergyChanged(long difference,boolean simulate){}
            };
            SGJourneyEnergy[] route=new SGJourneyEnergy[5];
            route[4]=target;
            for(int i=3;i>=0;i--){
                int index=i;
                boolean cable=((i&1)==0)==firstCable;
                route[i]=new SGJourneyEnergy(Long.MAX_VALUE,Long.MAX_VALUE,0) {
                    private long forward(long amount,boolean simulate,boolean zeroPoint){
                        return MixedPowerTransfer.run(this,amount,simulate,zeroPoint,()->{
                            SGJourneyEnergy next=route[index+1];
                            if(index!=0)route[index-1].receiveZeroPointEnergy(amount,simulate);
                            if(cable && MixedPowerTransfer.probeCableOutput(next,Integer.MAX_VALUE,true)==0)return 0;
                            return zeroPoint?next.receiveZeroPointEnergy(amount,simulate):next.receiveLongEnergy(amount,simulate);
                        });
                    }
                    @Override public int receiveEnergy(int amount,boolean simulate){return (int)receiveLongEnergy(amount,simulate);}
                    @Override public long receiveLongEnergy(long amount,boolean simulate){return forward(amount,simulate,false);}
                    @Override public long receiveZeroPointEnergy(long amount,boolean simulate){return forward(amount,simulate,true);}
                    @Override public void onEnergyChanged(long difference,boolean simulate){}
                };
            }
            if(route[0].receiveLongEnergy(1000,false)!=0 || stored[0]!=0
                    || route[0].receiveZeroPointEnergy(1000,true)!=600 || stored[0]!=0
                    || route[0].receiveZeroPointEnergy(1000,false)!=600 || stored[0]!=600
                    || route[0].receiveZeroPointEnergy(1000,false)!=0)
                throw new AssertionError("Mixed power failed with "+(firstCable?"cable":"floor")+" first");
        }
        System.out.println("PASS: alternating floor/cable power in both orders, cycle guard, simulation and transfer accounting.");
    }

    public static void main(String[] args) throws Exception {
        checkRestoredRecipes();
        checkNamespaceMigration();
        method("com.mustangdoc.sgjpatch.doors.DoorControlBlock", "useItemOn",
                "net.minecraft.world.item.ItemStack", "net.minecraft.world.level.block.state.BlockState",
                "net.minecraft.world.level.Level", "net.minecraft.core.BlockPos",
                "net.minecraft.world.entity.player.Player", "net.minecraft.world.InteractionHand",
                "net.minecraft.world.phys.BlockHitResult");
        method("com.mustangdoc.sgjpatch.doors.DoorControlBlock", "useWithoutItem",
                "net.minecraft.world.level.block.state.BlockState", "net.minecraft.world.level.Level",
                "net.minecraft.core.BlockPos", "net.minecraft.world.entity.player.Player",
                "net.minecraft.world.phys.BlockHitResult");
        System.out.println("PASS: NeoForge door control handles item and empty-hand interactions.");
        checkTransporterInterior();
        if (!com.mustangdoc.sgjpatch.transporter.TransporterLevels.isLower(-3885,255,5885,"Renamed")
                || !com.mustangdoc.sgjpatch.transporter.TransporterLevels.isLower(-3937,263,5973,"Renamed")
                || !com.mustangdoc.sgjpatch.transporter.TransporterLevels.isLower(0,0,0,"GENETICS LAB")
                || !com.mustangdoc.sgjpatch.transporter.TransporterLevels.isLower(0,0,0,"Lower Spire")
                || !com.mustangdoc.sgjpatch.transporter.TransporterLevels.isLower(-3897,256,5876,"Renamed")
                || !com.mustangdoc.sgjpatch.transporter.TransporterLevels.isLower(0,0,0,"Underwater Jumper Bay")
                || com.mustangdoc.sgjpatch.transporter.TransporterLevels.isLower(-4268,326,5886,"West City")
                || com.mustangdoc.sgjpatch.transporter.TransporterLevels.isLower(1,200,1,"Custom Station"))
            throw new AssertionError("Transporter lower-city classification");
        System.out.println("PASS: lower-city transporter destinations and existing station behavior.");
        var bundledChunksReady=type("com.mustangdoc.sgjpatch.transporter.TransporterCityUpgrade")
                .getDeclaredMethod("bundledChunksReady",java.util.function.Predicate.class);
        bundledChunksReady.setAccessible(true);
        java.util.function.Predicate<BlockPos> allComplete=pos->true;
        java.util.function.Predicate<BlockPos> underwaterBayIncomplete=
                pos->(pos.getX()>>4)!=(-4090>>4) || (pos.getZ()>>4)!=(5546>>4);
        if(!bundledChunksReady.invoke(null,allComplete).equals(true)
                || !bundledChunksReady.invoke(null,underwaterBayIncomplete).equals(false))
            throw new AssertionError("City readiness must check every bundled transporter, including the underwater bay.");
        System.out.println("PASS: city transporter readiness checks all bundled stations without overrunning the facing list.");
        checkEventSubscribers();
        checkMixedPower();
        com.mustangdoc.sgjpatch.power.CableNetworkRefreshChecks.run();
        com.mustangdoc.sgjpatch.doors.ClientDoorAnimationChecks.run();
        var centerAction=type("com.mustangdoc.sgjpatch.compat.LanteaJumperScreen")
                .getDeclaredMethod("nativeCenterAction",String.class,boolean.class);
        centerAction.setAccessible(true);
        if(!centerAction.invoke(null,"Entering address",true).equals(2)
                || !centerAction.invoke(null,"Awaiting activation",true).equals(1)
                || !centerAction.invoke(null,"Ready",true).equals(0)
                || !centerAction.invoke(null,"Connected",true).equals(2)
                || !centerAction.invoke(null,"Entering address",false).equals(0))
            throw new AssertionError("Native Atlantis centre button must clear partial entry without changing valid dialing.");
        System.out.println("PASS: native Atlantis table centre button clears partial input and preserves activation.");
        checkMemory();
        com.mustangdoc.sgjpatch.standalone.RecentAddressHistoryChecks.run();
        for(int i=-1;i<=38;i++) {
            var key=AtlantisReferencePlate.keyShape(i);
            if(Math.abs(key.height()/key.width()-Math.sqrt(3)/2)>1e-9) throw new AssertionError("Triangle "+i);
        }
        String root="io.github.aqueoxx.lantea.";
        String services=root+"server.LanteaServerServices", dial=root+"jumper.JumperDial", jumper=root+"jumper.PuddleJumper";
        method(services,"get","net.minecraft.server.MinecraftServer");
        method(dial,"open","net.minecraft.server.level.ServerPlayer",jumper);
        method(dial,"mayUse","net.minecraft.server.level.ServerPlayer",jumper);
        method(dial,"nearest",services,"net.minecraft.server.level.ServerLevel","net.minecraft.world.phys.Vec3");
        method(dial,"guide",services,dial+"$Near","java.lang.String");
        method(dial,"dial","net.minecraft.server.level.ServerPlayer",jumper,"java.lang.String","java.util.List");
        type(root+"core.state.GateEvent$SymbolEncoded").getConstructor(int.class,long.class);
        type(root+"core.state.GateEvent$Engaged").getConstructor(long.class);
        type(root+"core.dial.DhdBuffer").getConstructor(type(root+"core.id.GlyphSet"),List.class,boolean.class);
        method(root+"core.id.AddressCodec","parse","java.lang.String");
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        UUID token=UUID.randomUUID();
        var command=new LanteaJumperNetwork.Command(token,5,"34");
        LanteaJumperNetwork.Command.CODEC.encode(buf,command);
        if(!command.equals(LanteaJumperNetwork.Command.CODEC.decode(buf)))throw new AssertionError("Command round trip");
        buf.clear();
        var guide=new LanteaJumperNetwork.DialGuide(token,"mw:02-34-10-38-09-13-29",new int[]{2,34,10,38,9,13,29,0},"");
        LanteaJumperNetwork.DialGuide.CODEC.encode(buf,guide);
        var decoded=LanteaJumperNetwork.DialGuide.CODEC.decode(buf);
        if(!Arrays.equals(guide.keys(),decoded.keys()) || !guide.address().equals(decoded.address()))throw new AssertionError("Guide round trip");
        buf.clear();
        // State's rich Component codec requires a fully loaded NeoForge registry context.
        // Exercise that packet in game; the other three codecs do not require game startup.
        var lights=new LanteaJumperNetwork.TableLights(ResourceLocation.parse("minecraft:overworld"),new BlockPos(1,65,2),"mw",new int[]{2,34},true);
        LanteaJumperNetwork.TableLights.CODEC.encode(buf,lights);
        var received=LanteaJumperNetwork.TableLights.CODEC.decode(buf);
        if(!Arrays.equals(lights.symbols(),received.symbols()) || !received.pos().equals(lights.pos()) || !received.connected())throw new AssertionError("Table lights round trip");
        buf.release();
        System.out.println("PASS: production geometry, installed Lantea API contracts, command, guide and table-light packet round trips.");
    }
}


