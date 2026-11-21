package com.mustangdoc.sgjpatch.transporter;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import com.mustangdoc.sgjpatch.city.AtlantisCityEvents;
import com.mustangdoc.sgjpatch.city.AtlantisCitySavedData;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;


/** Only the two endpoints of the user's original City Tower1 route are converted.
 * The unpaired, back-to-back central-tower booth is deliberately outside both boxes. */
@EventBusSubscriber(modid = "sgjatlantis_dhd")
public final class TransporterCityUpgrade {
    private record BundledStation(BlockPos panel, Direction facing) {}

    private static final List<BundledStation> BUNDLED_STATIONS = List.of(
        new BundledStation(new BlockPos(-3896, 414, 5871), Direction.EAST), // Control Room
        new BundledStation(new BlockPos(-4268, 327, 5886), Direction.WEST), // West City
        new BundledStation(new BlockPos(-3704, 326, 5562), Direction.NORTH), // North Tower
        new BundledStation(new BlockPos(-3897, 257, 5876), Direction.SOUTH), // Legacy lower bay
        new BundledStation(new BlockPos(-3830, 299, 5756), Direction.EAST),
        new BundledStation(new BlockPos(-3885, 256, 5885), Direction.WEST), // Lower Spire
        new BundledStation(new BlockPos(-3937, 264, 5973), Direction.NORTH), // Genetics Lab
        new BundledStation(new BlockPos(-4090, 252, 5546), Direction.NORTH)  // Underwater Jumper Bay
    );
    private static final Set<ServerLevel> DIRECTORY_SEEDED = Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent public static void tick(LevelTickEvent.Post e){
        if(!(e.getLevel() instanceof ServerLevel l)||l.getGameTime()%100!=0
            ||!l.dimension().location().toString().equals("sgjourney:lantea"))return;
        if(!DIRECTORY_SEEDED.contains(l) && !AtlantisCityEvents.cityInstallationRunning(l) && bundledChunksReady(l))
            refreshBundledStations(l);
        upgrade(l,new BlockPos(-3896,412,5871),new BlockPos(-3896,413,5871),Direction.EAST,"Central Tower");
        upgrade(l,new BlockPos(-4268,325,5886),new BlockPos(-4268,326,5886),Direction.WEST,"West City");
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload e){
        if(e.getLevel() instanceof ServerLevel l) DIRECTORY_SEEDED.remove(l);
    }
    private static boolean bundledChunksReady(ServerLevel l){
        var data=AtlantisCitySavedData.get(l);
        return bundledChunksReady(pos -> data.complete(pos.getX()>>4,pos.getZ()>>4));
    }
    static boolean bundledChunksReady(Predicate<BlockPos> isComplete){
        for(var station:BUNDLED_STATIONS)
            // The floor spans the full footprint; the volume checks the same chunks as the shell.
            for(BlockPos pos:TransporterRoom.volume(station.panel().below(),station.facing()))
                if(!isComplete.test(pos))return false;
        return true;
    }
    public static void refreshBundledStations(ServerLevel l){
        var network=TransporterNetwork.get(l);
        int registered=0;
        boolean migrationPending=false;
        for(var station:BUNDLED_STATIONS){
            BlockPos panel=station.panel();
            Direction facing=station.facing();
            l.getChunk(panel.getX()>>4,panel.getZ()>>4);
            BlockPos oldPanel=panel.relative(facing.getOpposite());
            l.getChunk(oldPanel.getX()>>4,oldPanel.getZ()>>4);
            if(!(l.getBlockEntity(panel) instanceof TransporterEntity)
                && l.getBlockEntity(oldPanel) instanceof TransporterEntity old){
                for(BlockPos pos:TransporterRoom.volume(panel.below(),facing))
                    l.getChunk(pos.getX()>>4,pos.getZ()>>4);
                for(BlockPos pos:TransporterRoom.volume(oldPanel.below(),facing))
                    l.getChunk(pos.getX()>>4,pos.getZ()>>4);
                if(!TransporterRoom.shorten(l,old))migrationPending=true;
            }
            // Check the complete room before making an unloaded destination selectable.
            if(l.getBlockEntity(panel) instanceof TransporterEntity be){
                for(BlockPos pos:TransporterRoom.shell(be.anchor(),be.facing()).keySet())
                    l.getChunk(pos.getX()>>4,pos.getZ()>>4);
                if(TransporterRoom.complete(l,be.anchor(),be.facing())){
                    network.register(new TransporterNetwork.Station(be.anchor(),be.facing(),be.stationName()));
                    registered++;
                    continue;
                }
            }
            if(!migrationPending)network.remove(panel.below());
        }
        if(!migrationPending)DIRECTORY_SEEDED.add(l);
        LogUtils.getLogger().info("Registered {} of {} bundled Atlantis transporters",registered,BUNDLED_STATIONS.size());
    }
    private static void upgrade(ServerLevel l,BlockPos old,BlockPos anchor,Direction f,String name){
        if(!l.hasChunkAt(old))return;
        if(!String.valueOf(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(l.getBlockState(old).getBlock())).equals("mekanism:teleporter"))return;
        for(BlockPos p:TransporterRoom.volume(anchor,f))if(!l.hasChunkAt(p))return;
        // No edit while anyone is inside the replacement volume.
        if(!l.getEntities(null,TransporterRoom.interior(anchor,f).inflate(1)).isEmpty())return;
        var positions=TransporterRoom.volume(anchor,f);
        int minX=positions.stream().mapToInt(BlockPos::getX).min().orElseThrow(),minZ=positions.stream().mapToInt(BlockPos::getZ).min().orElseThrow();
        var id=TransporterRegistry.id("transporter_backups/"+old.getX()+"_"+old.getY()+"_"+old.getZ()+"_"+l.getGameTime());
        try {
            var manager=l.getStructureManager();StructureTemplate backup=manager.getOrCreate(id);
            backup.fillFromWorld(l,new BlockPos(minX,anchor.getY()-1,minZ),new Vec3i(5,5,5),false,null);
            if(!manager.save(id)){LogUtils.getLogger().error("Cannot save transporter backup {}; leaving booth unchanged",id);return;}
            // Remove the old door controller first, preventing its multiblock cleanup from touching the new shell.
            for(BlockPos p:positions)if(l.getBlockEntity(p)!=null)l.removeBlock(p,false);
            TransporterRoom.build(l,anchor,f,name);
            LogUtils.getLogger().info("Upgraded Atlantis transporter {} at {}; original room saved as {}",name,anchor,id);
        }catch(Exception ex){LogUtils.getLogger().error("Transporter upgrade at {} failed; backup {}",anchor,id,ex);}
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("sgjatlantis").requires(s->s.hasPermission(2))
            .then(Commands.literal("transporter")
                .then(Commands.literal("give").executes(c->{var p=c.getSource().getPlayerOrException();p.getInventory().placeItemBackInInventory(new net.minecraft.world.item.ItemStack(TransporterRegistry.ITEM));return 1;}))
                .then(Commands.literal("rename").then(Commands.argument("name",StringArgumentType.greedyString()).executes(c->{
                    ServerPlayer p=c.getSource().getPlayerOrException();TransporterEntity be=findNear(p);
                    if(be==null){c.getSource().sendFailure(Component.literal("Stand inside or beside a transporter room."));return 0;}
                    be.rename(StringArgumentType.getString(c,"name"));c.getSource().sendSuccess(()->Component.literal("Transporter named "+be.stationName()),true);return 1;
                })))
                .then(Commands.literal("list").executes(c->{
                    var stations=TransporterNetwork.get(c.getSource().getLevel()).stations();
                    for(var s:stations)c.getSource().sendSuccess(()->Component.literal(s.name()+" — "+s.pos().toShortString()),false);
                    return stations.size();
                }))));
    }
    private static TransporterEntity findNear(ServerPlayer p){
        for(var s:TransporterNetwork.get(p.serverLevel()).stations())if(s.pos().distSqr(p.blockPosition())<64
            &&p.serverLevel().getBlockEntity(s.pos().above()) instanceof TransporterEntity be)return be;
        return null;
    }
}
