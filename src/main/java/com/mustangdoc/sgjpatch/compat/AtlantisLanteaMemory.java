package com.mustangdoc.sgjpatch.compat;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;

/** Persistent table memory using Lantea's own connection-log ordering and deduplication. */
public final class AtlantisLanteaMemory {
    private static final String ROOT = "io.github.aqueoxx.lantea.";
    private static final String TAG = "AtlantisLanteaMemory";
    private AtlantisLanteaMemory() {}
    public static String[] addresses(AtlantisDHDEntity dhd) {
        ListTag entries = dhd.getPersistentData().getList(TAG, Tag.TAG_COMPOUND);
        String[] result = new String[Math.min(8, entries.size())];
        for (int i=0;i<result.length;i++) result[i]=entries.getCompound(i).getString("Address");
        return result;
    }
    public static void record(AtlantisDHDEntity dhd, LanteaNativeJumperBackend.Gate gate, ServerLevel level)
            throws ReflectiveOperationException {
        Object phase=AtlantisNativeDhd.call(gate.controller(),"phase");
        if (!phase.getClass().getSimpleName().equals("Connected")
                || !AtlantisNativeDhd.call(phase,"role").toString().equals("OUTGOING")) return;
        String connection=AtlantisNativeDhd.call(phase,"connection").toString();
        CompoundTag data=dhd.getPersistentData();
        if (connection.equals(data.getString(TAG+"Connection"))) return;
        Object svc=((Optional<?>)Class.forName(ROOT+"server.LanteaServerServices")
                .getMethod("get",net.minecraft.server.MinecraftServer.class).invoke(null,level.getServer())).orElseThrow();
        Object live=((Optional<?>)AtlantisNativeDhd.call(AtlantisNativeDhd.call(svc,"connections"),"involving",gate.id())).orElseThrow();
        Object remote=((Optional<?>)AtlantisNativeDhd.call(AtlantisNativeDhd.call(svc,"registry"),"byId",
                AtlantisNativeDhd.call(live,"destination"))).orElseThrow();
        Object glyphs=AtlantisNativeDhd.call(AtlantisNativeDhd.call(AtlantisNativeDhd.call(gate.controller(),"host"),"kind"),"glyphSet");
        Object address=AtlantisNativeDhd.call(AtlantisNativeDhd.call(svc,"dialing"),"written",remote,glyphs);
        Class<?> codec=Class.forName(ROOT+"core.id.AddressCodec");
        Class<?> addressType=Class.forName(ROOT+"core.id.GateAddress");
        Class<?> entryType=Class.forName(ROOT+"core.dial.ConnectionLog$Entry");
        var constructor=entryType.getConstructor(addressType,boolean.class,long.class);
        List<Object> entries=new ArrayList<>();
        ListTag saved=data.getList(TAG,Tag.TAG_COMPOUND);
        for(int i=0;i<Math.min(8,saved.size());i++) {
            CompoundTag tag=saved.getCompound(i);
            try { entries.add(constructor.newInstance(codec.getMethod("parse",String.class).invoke(null,tag.getString("Address")),true,tag.getLong("Time"))); }
            catch(java.lang.reflect.InvocationTargetException invalid) { /* Skip malformed saved entries. */ }
        }
        Object log=Class.forName(ROOT+"core.dial.ConnectionLog").getConstructor(List.class).newInstance(entries);
        log=AtlantisNativeDhd.call(log,"record",constructor.newInstance(address,true,level.getGameTime()));
        ListTag updated=new ListTag();
        for(Object entry:(List<?>)AtlantisNativeDhd.call(log,"entries")) {
            CompoundTag tag=new CompoundTag();
            tag.putString("Address",(String)codec.getMethod("format",addressType).invoke(null,AtlantisNativeDhd.call(entry,"address")));
            tag.putLong("Time",((Number)AtlantisNativeDhd.call(entry,"gameTime")).longValue());
            updated.add(tag);
        }
        data.put(TAG,updated); data.putString(TAG+"Connection",connection); dhd.setChanged();
    }
}
