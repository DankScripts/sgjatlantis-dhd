package com.mustangdoc.sgjpatch.compat;

import com.mojang.logging.LogUtils;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.povstalec.sgjourney.api.stargate.BlockEntityStargate;
import net.povstalec.sgjourney.api.stargate.DHDControlledStargate;
import org.slf4j.Logger;

/** Optional stationary console adapter. Lantea retains ownership of the actual connection and transit. */
@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch")
public final class AtlantisNativeDhd {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ROOT = "io.github.aqueoxx.lantea.";
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();
    private static final Map<AtlantisDHDEntity, LanteaNativeJumperBackend.Gate> ACTIVE_TABLES = new IdentityHashMap<>();
    private record Target(LanteaNativeJumperBackend.Gate gate, double distance) {}
    private static final class Session {
        final UUID token = UUID.randomUUID();
        final AtlantisDHDEntity dhd;
        final LanteaNativeJumperBackend.Gate gate;
        long expires, poll, action;
        Session(AtlantisDHDEntity dhd, LanteaNativeJumperBackend.Gate gate, long now) {
            this.dhd=dhd; this.gate=gate; expires=now+6000;
        }
    }
    private AtlantisNativeDhd() {}
    static Object call(Object target, String name, Object... args) throws ReflectiveOperationException {
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount()!=args.length) continue;
            Class<?>[] types=method.getParameterTypes(); boolean matches=true;
            for(int i=0;i<types.length;i++) {
                Class<?> t=types[i];
                if(t==long.class)t=Long.class; else if(t==int.class)t=Integer.class;
                else if(t==boolean.class)t=Boolean.class;
                if(args[i]!=null&&!t.isInstance(args[i]))matches=false;
            }
            if(matches)return method.invoke(target,args);
        }
        throw new NoSuchMethodException(target.getClass().getName()+"."+name);
    }
    static Object services(ServerPlayer p) throws ReflectiveOperationException {
        Class<?> type=Class.forName(ROOT+"server.LanteaServerServices");
        return ((Optional<?>)type.getMethod("get",net.minecraft.server.MinecraftServer.class)
                .invoke(null,p.getServer())).orElseThrow();
    }
    private static Object event(String name, Class<?>[] types, Object... args) throws ReflectiveOperationException {
        return Class.forName(ROOT+"core.state.GateEvent$"+name).getConstructor(types).newInstance(args);
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    private static Object constant(String type,String name) throws ReflectiveOperationException {
        return Enum.valueOf((Class)Class.forName(ROOT+type),name);
    }
    private static Target nearest(ServerPlayer p, AtlantisDHDEntity dhd, boolean logMiss) throws ReflectiveOperationException {
        Object services=services(p), registry=call(services,"registry"); Target best=null;
        double range=dhd.getMaxConnectionDistance();
        int inRange=0, assembled=0;
        for(Object record:(Collection<?>)call(registry,"all")) {
            Object location=call(record,"location");
            if(!p.level().dimension().location().toString().equals(call(location,"dimension")))continue;
            Vec3 pos=new Vec3(((Number)call(location,"x")).doubleValue()+.5,
                    ((Number)call(location,"y")).doubleValue()+.5,((Number)call(location,"z")).doubleValue()+.5);
            double distance=pos.distanceToSqr(Vec3.atCenterOf(dhd.getBlockPos()));
            if(distance>range*range)continue;
            inRange++;
            if(!Boolean.TRUE.equals(call(record,"assembled")))continue;
            assembled++;
            if(best!=null&&distance>=best.distance)continue;
            Optional<?> controller=(Optional<?>)call(services,"controller",call(record,"id"));
            if(controller.isEmpty() || !Boolean.TRUE.equals(call(controller.get(),"structureAssembled")))continue;
            var gate=LanteaNativeJumperBackend.describe(controller.get());
            best=new Target(gate,distance);
        }
        if(best==null&&logMiss) {
            LOGGER.info("Atlantis table at {} in {} opened the SGJourney dialer: no live assembled Lantea gate within {} blocks ({} registered in range, {} marked assembled)",
                    dhd.getBlockPos(), p.level().dimension().location(), range, inRange, assembled);
        }
        return best;
    }
    private static boolean journeyLinked(ServerPlayer p,AtlantisDHDEntity dhd) {
        var table=dhd.getDHD();
        if(table==null)return false;
        var linked=table.receiverCache().getCached();
        if(!(linked instanceof BlockEntityStargate<?> gate)
                || linked.controllerCache().getCached()!=table
                || !p.level().dimension().equals(gate.getDimension())
                || gate.getBlockPos().distSqr(dhd.getBlockPos())>dhd.getMaxConnectionDistanceSqr()
                || !p.serverLevel().hasChunkAt(gate.getBlockPos()))return false;
        var block=gate.getStargateBlockEntity();
        return block!=null && !block.isRemoved() && p.level().getBlockEntity(gate.getBlockPos())==block
                && block.getCapability(DHDControlledStargate.DHD_CONTROLLED_STARGATE_CAPABILITY).orElse(null)==linked;
    }
    public static boolean open(ServerPlayer p, BlockPos pos) {
        if(!ModList.get().isLoaded("lantea") || !(p.level().getBlockEntity(pos) instanceof AtlantisDHDEntity dhd)
                || journeyLinked(p,dhd))return false;
        try {
            Target target=nearest(p,dhd,true); if(target==null)return false;
            if(p.isSpectator() || p.distanceToSqr(Vec3.atCenterOf(pos))>64)return true;
            Session session=new Session(dhd,target.gate,p.serverLevel().getGameTime());
            SESSIONS.put(p,session); send(p,session,true,Component.literal("Select symbols or paste a Lantea address with Ctrl+V."));
            return true;
        } catch(ReflectiveOperationException|RuntimeException ex) {
            LOGGER.error("Cannot open native Atlantis DHD",ex);
            p.displayClientMessage(Component.literal("Lantea DHD compatibility failed; see the game log."),true);
            return true;
        }
    }
    public static boolean command(ServerPlayer p,UUID token,int action,String address) {
        Session s=SESSIONS.get(p); if(s==null || !s.token.equals(token))return false;
        if(action==3){SESSIONS.remove(p);return true;}
        long now=p.serverLevel().getGameTime();
        try {
            if(s.dhd.isRemoved() || s.dhd.getLevel()!=p.level() || !p.isAlive() || p.isSpectator()
                    || now>s.expires || p.distanceToSqr(Vec3.atCenterOf(s.dhd.getBlockPos()))>64
                    || p.level().getBlockEntity(s.dhd.getBlockPos())!=s.dhd)throw new IllegalStateException("Console is out of reach.");
            if(action==0&&now<s.poll)return true;
            if(action!=0&&action!=4&&now<s.action){send(p,s,false,Component.literal("Please retry that button."));return true;}
            s.poll=now+5; if(action!=0&&action!=4)s.action=now+1;
            Target target=nearest(p,s.dhd,false);
            if(target==null || target.gate.controller()!=s.gate.controller() || journeyLinked(p,s.dhd))throw new IllegalStateException("Gate link changed; reopen the DHD.");
            if ((action==1 || action==5 || action==6)
                    && !AtlantisDhdCrystalRequirements.canDial(s.dhd)) {
                send(p,s,false,Component.literal("Atlantis DHD needs a large control crystal, two advanced energy crystals, and an advanced transfer crystal."));
                return true;
            }
            s.expires=now+6000;
            Component feedback=Component.empty();
            if(action==7) {
                if (call(s.gate.controller(),"host") instanceof LanteaEnergyIrisState iris && iris.sgjpatch$irisInstalled()) {
                    boolean closed = !iris.sgjpatch$irisClosed();
                    iris.sgjpatch$setIris(true, closed);
                    feedback = Component.literal(closed ? "Energy iris closed." : "Energy iris opened.");
                } else feedback = Component.literal("No energy iris installed.");
            } else if(action==1)feedback=activate(p,s.gate);
            else if(action==2)feedback=close(p,s);
            else if(action==5) {
                feedback=pressSymbol(p,s.gate,Integer.parseInt(address),7);
                ACTIVE_TABLES.put(s.dhd,s.gate);
            }
            else if(action==6) {
                if (!call(s.gate.controller(),"state").toString().equals("IDLE"))
                    throw new IllegalArgumentException("Clear the current sequence before redialing.");
                String[] memory=AtlantisLanteaMemory.addresses(s.dhd);
                int index=Integer.parseInt(address);
                if(index<0 || index>=memory.length)throw new IllegalArgumentException("That memory entry is no longer available.");
                int[] keys=guidedKeys(p,s,memory[index]);
                for(int key:keys) {
                    feedback=pressSymbol(p,s.gate,key,7);
                    if(!feedback.getString().isEmpty() && key!=0)break;
                }
                if(call(s.gate.controller(),"phase").getClass().getSimpleName().equals("Encoding")
                        && ((Optional<?>)call(call(s.gate.controller(),"phase"),"plan")).isPresent())
                    feedback=activate(p,s.gate);
                ACTIVE_TABLES.put(s.dhd,s.gate);
            }
            else if(action==4) {
                int[] keys;
                String error="";
                try { keys=guidedKeys(p,s,address); }
                catch(IllegalArgumentException ex) { keys=new int[0]; error=ex.getMessage(); }
                send(p,s,false,Component.empty());
                LanteaJumperNetwork.sendGuide(p,s.token,address,keys,error);
                return true;
            }
            else if(action!=0)return true;
            send(p,s,false,feedback);
        } catch(IllegalArgumentException ex) {
            try { send(p,s,false,Component.literal(ex.getMessage())); } catch(Exception ignored){SESSIONS.remove(p);}
        } catch(ReflectiveOperationException|RuntimeException ex) {
            SESSIONS.remove(p);
            com.mojang.logging.LogUtils.getLogger().warn("Native Atlantis DHD session ended",ex);
            LanteaJumperNetwork.send(p,new LanteaJumperNetwork.State(s.token,s.token,false,false,36,
                    Component.literal("Gate link unavailable; reopen the DHD."),"Unavailable","lantea_dhd","pegasus",new int[0],new int[0],-1));
        }
        return true;
    }
    /** Same buffer validation and events as Lantea's DhdService.press. Never batch-replay the address. */
    static Component pressSymbol(ServerPlayer p,LanteaNativeJumperBackend.Gate gate,int symbol,int longest)
            throws ReflectiveOperationException {
        Object controller=gate.controller(), phase=call(controller,"phase");
        boolean encoding=phase.getClass().getSimpleName().equals("Encoding");
        if(encoding ? ((Optional<?>)call(phase,"plan")).isPresent() : !call(controller,"state").toString().equals("IDLE"))
            return Component.literal("Gate is busy.");
        Object svc=services(p), glyphs=call(call(call(controller,"host"),"kind"),"glyphSet");
        if(((Number)call(call(svc,"power"),"available",gate.id())).longValue()<=0)
            return Component.translatable("lantea.dial.failure.insufficient_power");
        Object buffer=Class.forName(ROOT+"core.dial.DhdBuffer")
                .getConstructor(glyphs.getClass(),List.class,boolean.class)
                .newInstance(glyphs,encoding?call(phase,"symbols"):List.of(),false);
        Object entry=call(buffer,"press",symbol,longest);
        if(entry.getClass().getSimpleName().equals("Refused"))
            return Component.translatable((String)call(call(entry,"reason"),"translationKey"));
        buffer=call(entry,"buffer");
        long tick=((Number)call(svc,"tickCount")).longValue();
        if(Boolean.TRUE.equals(call(buffer,"origin"))) {
            Object address=((Optional<?>)call(buffer,"address")).orElseThrow();
            Object result=call(call(svc,"dialing"),"dial",controller,address,
                    constant("core.dial.DialInitiator","DHD"),tick);
            if(result.getClass().getSimpleName().equals("Rejected"))
                return Component.translatable((String)call(call(result,"failure"),"translationKey"));
            return Component.literal("Address entered. Press the centre diamond to connect.");
        }
        call(controller,"apply",event("SymbolEncoded",new Class<?>[]{int.class,long.class},symbol,tick));
        return Component.empty();
    }
    static Component activate(ServerPlayer p,LanteaNativeJumperBackend.Gate gate) throws ReflectiveOperationException {
        Object phase=call(gate.controller(),"phase");
        if(!phase.getClass().getSimpleName().equals("Encoding") || ((Optional<?>)call(phase,"plan")).isEmpty())
            return Component.literal("Enter the address and origin 0 first.");
        if(Boolean.TRUE.equals(call(phase,"engaged")))return Component.literal("Connection is already starting.");
        call(gate.controller(),"apply",event("Engaged",new Class<?>[]{long.class},
                ((Number)call(services(p),"tickCount")).longValue()));
        return Component.literal("Opening Lantea connection.");
    }
    /** Resolve exactly as the actual dial does, so the guide cannot show a different gate. */
    private static Object translatedAddress(Session s,String text,Object svc,Object dialing,Object glyphs)
            throws ReflectiveOperationException {
        Object pasted;
        try {
            pasted=Class.forName(ROOT+"core.id.AddressCodec").getMethod("parse",String.class)
                    .invoke(null,text.toLowerCase(Locale.ROOT));
        } catch(InvocationTargetException ex) {
            throw new IllegalArgumentException("Invalid Lantea address: " + ex.getCause().getMessage());
        }
        Object registry=call(svc,"registry");
        Optional<?> record=(Optional<?>)call(registry,"byAddress",call(pasted,"place"));
        if(record.isEmpty())throw new IllegalArgumentException("That address is not in Lantea's gate registry.");
        if(((List<?>)call(pasted,"symbols")).size()>6
                && !call(dialing,"written",record.get(),call(pasted,"glyphSet")).equals(pasted))
            throw new IllegalArgumentException("The address has the wrong galaxy symbol.");
        Object translated=call(dialing,"written",record.get(),glyphs);
        return call(dialing,"dialedFrom",s.gate.controller(),translated);
    }
    private static int[] guidedKeys(ServerPlayer p,Session s,String text) throws ReflectiveOperationException {
        if(text==null || !text.matches("(?i)(mw|pg):.*"))
            throw new IllegalArgumentException("Clipboard has no Lantea address.");
        Object svc=services(p), dialing=call(svc,"dialing");
        Object glyphs=call(call(call(s.gate.controller(),"host"),"kind"),"glyphSet");
        Object address=translatedAddress(s,text,svc,dialing,glyphs);
        Object registry=call(svc,"registry");
        Optional<?> source=(Optional<?>)call(registry,"byId",call(s.gate.controller(),"gateId"));
        if(source.isPresent()) {
            int galaxy=((Number)call(dialing,"galaxySymbol",source.get(),glyphs)).intValue();
            address=call(address,"within",galaxy);
        }
        List<?> symbols=(List<?>)call(address,"symbols");
        int[] keys=new int[symbols.size()+1];
        for(int i=0;i<symbols.size();i++)keys[i]=((Number)symbols.get(i)).intValue();
        keys[symbols.size()]=((Number)call(glyphs,"pointOfOrigin")).intValue();
        if(keys.length>9)throw new IllegalArgumentException("Address is too long for the dialer.");
        return keys;
    }
    private static Component close(ServerPlayer p,Session s) throws ReflectiveOperationException {
        return close(p,s.gate);
    }
    static Component close(ServerPlayer p,LanteaNativeJumperBackend.Gate gate) throws ReflectiveOperationException {
        Object phase=call(gate.controller(),"phase"); long tick=((Number)call(services(p),"tickCount")).longValue();
        if(phase.getClass().getSimpleName().equals("Incoming"))return Component.literal("An incoming Lantea connection cannot be cancelled from this end.");
        if(phase.getClass().getSimpleName().equals("Connected")) {
            if(!call(phase,"role").toString().equals("OUTGOING"))return Component.literal("An incoming Lantea wormhole cannot be closed from this end.");
            Object reason=constant("core.connection.CloseReason","REQUESTED");
            call(gate.controller(),"apply",event("CloseRequested",new Class<?>[]{reason.getClass(),long.class},reason,tick));
        } else call(gate.controller(),"apply",event("AbortRequested",new Class<?>[]{long.class},tick));
        return Component.literal("Close/reset requested.");
    }
    private static void send(ServerPlayer p,Session s,boolean open,Component message) throws ReflectiveOperationException {
        var snapshot=LanteaNativeJumperBackend.snapshot(s.gate);
        var host=call(s.gate.controller(),"host");
        var gatePos=host instanceof net.minecraft.world.level.block.entity.BlockEntity block ? block.getBlockPos() : null;
        LanteaJumperNetwork.send(p,new LanteaJumperNetwork.State(s.token,s.token,open,true,s.gate.symbols(),message,
                snapshot.status(),"lantea_dhd",s.gate.glyphSet(),snapshot.encodedSymbols(),new int[0],-1,snapshot.typedSymbols(),
                AtlantisLanteaMemory.addresses(s.dhd),gatePos));
    }

    /** Keep the physical table in sync even after the dialer screen closes. */
    @SubscribeEvent
    public static void syncTableLights(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || ACTIVE_TABLES.isEmpty())return;
        var iterator=ACTIVE_TABLES.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();
            AtlantisDHDEntity dhd=entry.getKey();
            if(dhd.isRemoved() || !(dhd.getLevel() instanceof ServerLevel level)
                    || !level.hasChunkAt(dhd.getBlockPos())
                    || level.getBlockEntity(dhd.getBlockPos())!=dhd) {
                iterator.remove();
                continue;
            }
            if(level.getGameTime()%5!=0)continue;
            try {
                var snapshot=LanteaNativeJumperBackend.snapshot(entry.getValue());
                boolean connected=snapshot.status().equals("Connected");
                if(connected) AtlantisLanteaMemory.record(dhd,entry.getValue(),level);
                boolean idle=snapshot.status().equals("Ready") || snapshot.status().equals("Unavailable")
                        || snapshot.status().equals("Resetting") || snapshot.status().equals("Closing");
                int[] symbols=idle?new int[0]:snapshot.encodedSymbols();
                Vec3 center=Vec3.atCenterOf(dhd.getBlockPos());
                for(ServerPlayer player:level.players())
                    if(player.distanceToSqr(center)<64*64)
                        LanteaJumperNetwork.sendTableLights(player,level.dimension().location(),dhd.getBlockPos(),
                                entry.getValue().nativeGlyphSet(),symbols,connected);
                if(idle)iterator.remove();
            } catch(ReflectiveOperationException|RuntimeException ex) {
                com.mojang.logging.LogUtils.getLogger().warn("Cannot sync Lantea DHD table lights",ex);
                iterator.remove();
            }
        }
    }
}
