package com.mustangdoc.sgjpatch.transporter;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-dimension directory. Unloading a cabin must never remove its destination. */
public final class TransporterNetwork extends SavedData {
    public record Station(BlockPos pos, Direction facing, String name) {}
    private final Map<BlockPos,Station> stations=new HashMap<>();
    public static TransporterNetwork get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(TransporterNetwork::new,TransporterNetwork::load),"sgjatlantis_transporters");
    }
    public List<Station> stations() {
        return stations.values().stream().sorted(Comparator.comparing(Station::name).thenComparingLong(s->s.pos().asLong())).toList();
    }
    /** Check saved destinations when opening the directory, not on every tick.
     * Load the controller chunk before testing so unloaded cabins remain valid. */
    public List<Station> validatedStations(ServerLevel level) {
        for (Station station : stations()) {
            BlockPos panel = station.pos().above();
            level.getChunk(panel.getX() >> 4, panel.getZ() >> 4);
            if (!(level.getBlockEntity(panel) instanceof TransporterEntity controller)
                    || !controller.anchor().equals(station.pos())) {
                remove(station.pos());
            } else {
                register(new Station(controller.anchor(), controller.facing(), controller.stationName()));
            }
        }
        return stations();
    }
    public void register(Station s) { if(!s.equals(stations.put(s.pos(),s))) setDirty(); }
    public void remove(BlockPos p) { if(stations.remove(p)!=null) setDirty(); }
    public Station find(BlockPos p) { return stations.get(p); }
    private static TransporterNetwork load(CompoundTag tag, HolderLookup.Provider registries) {
        TransporterNetwork n=new TransporterNetwork();
        for(Tag t:tag.getList("Stations",10)) {
            CompoundTag s=(CompoundTag)t;
            Direction d=Direction.from2DDataValue(s.getInt("Facing"));
            BlockPos p=BlockPos.of(s.getLong("Pos"));
            n.stations.put(p,new Station(p,d,s.getString("Name")));
        }
        return n;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list=new ListTag();
        for(Station s:stations()) { CompoundTag t=new CompoundTag();t.putLong("Pos",s.pos().asLong());t.putInt("Facing",s.facing().get2DDataValue());t.putString("Name",s.name());list.add(t); }
        tag.put("Stations",list);return tag;
    }
}
