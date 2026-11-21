package com.mustangdoc.sgjpatch.doors;
import com.mustangdoc.sgjpatch.city.AtlantisFinish;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.RegisterEvent;
@EventBusSubscriber(modid = "sgjatlantis_dhd",bus=EventBusSubscriber.Bus.MOD)
public final class AtlantisDoorRegistry {
 public static final String NS="sgjadditions_capacity_patch";
 public static AtlantisDoorBlock BLOCK;
 public static final java.util.Map<AtlantisFinish,Item[]> FINISH_KITS=new java.util.EnumMap<>(AtlantisFinish.class);
 public static Item kit(boolean solid,int style,AtlantisFinish finish){if(finish==AtlantisFinish.LEGACY)return style==2?WIDE:style==1?ANGULAR:solid?QUARTERS:HALL;return FINISH_KITS.get(finish)[style==2?3:style==1?2:solid?1:0];}
 public static Item HALL,QUARTERS,ANGULAR,WIDE,CONTROL,DAMAGED,CRYSTAL;
 public static DoorControlBlock PANEL;
 public static BlockEntityType<AtlantisDoorEntity> ENTITY;
 private static ResourceLocation id(String n){return ResourceLocation.fromNamespaceAndPath(NS,n);}
 @SubscribeEvent public static void register(RegisterEvent e){
  e.register(Registries.BLOCK,h->{BLOCK=new AtlantisDoorBlock();h.register(id("atlantis_sliding_door"),BLOCK);PANEL=new DoorControlBlock();h.register(id("atlantis_door_control"),PANEL);});
  e.register(Registries.ITEM,h->{HALL=new AtlantisDoorItem(false);QUARTERS=new AtlantisDoorItem(true);h.register(id("atlantis_hall_door"),HALL);h.register(id("atlantis_quarters_door"),QUARTERS);ANGULAR=new AtlantisDoorItem(true,1);WIDE=new AtlantisDoorItem(false,2);h.register(id("atlantis_angular_door"),ANGULAR);h.register(id("atlantis_wide_glass_door"),WIDE);for(AtlantisFinish finish:AtlantisFinish.values())if(finish!=AtlantisFinish.LEGACY){
 Item[] kits={new AtlantisDoorItem(false,0,finish),new AtlantisDoorItem(true,0,finish),new AtlantisDoorItem(true,1,finish),new AtlantisDoorItem(false,2,finish)};
 FINISH_KITS.put(finish,kits);String[] names={"hall_door","quarters_door","angular_door","wide_glass_door"};
 for(int i=0;i<kits.length;i++)h.register(id("atlantis_"+finish.getSerializedName()+"_"+names[i]),kits[i]);
}CONTROL=new DoorControlItem(true);DAMAGED=new DoorControlItem(false);CRYSTAL=new Item(new Item.Properties());h.register(id("atlantis_door_control"),CONTROL);h.register(id("atlantis_door_control_damaged"),DAMAGED);h.register(id("atlantis_door_control_crystal"),CRYSTAL);});
  e.register(Registries.BLOCK_ENTITY_TYPE,h->{ENTITY=BlockEntityType.Builder.of(AtlantisDoorEntity::new,BLOCK).build(null);h.register(id("atlantis_sliding_door"),ENTITY);});
 }
}


