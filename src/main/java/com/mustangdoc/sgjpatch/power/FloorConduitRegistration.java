package com.mustangdoc.sgjpatch.power;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent; import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="sgjadditions_capacity_patch", bus=Mod.EventBusSubscriber.Bus.MOD)
public final class FloorConduitRegistration {
 public static final java.util.Map<String,Block> DECORATIVE_FLOORS=new java.util.LinkedHashMap<>();
 public static FloorBlock BLOCK;
 public static final java.util.Map<String,FloorBlock> VARIANTS=new java.util.LinkedHashMap<>();
 public static BlockEntityType<FloorConduitBlockEntity> TYPE;
 @SubscribeEvent public static void register(RegisterEvent event) {
  ResourceLocation id=new ResourceLocation("sgjadditions_capacity_patch","atlantis_floor_conduit");
  event.register(Registries.BLOCK,h->{BLOCK=new FloorBlock(BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL));h.register(id,BLOCK); for(String finish:new String[]{"silver","bronze","copper","blue_grey","teal","charcoal","stair_grey"}) {FloorBlock block=new FloorBlock(BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL)); VARIANTS.put(finish,block);h.register(id.withPath("atlantis_"+finish+"_floor_conduit"),block);} });
  event.register(Registries.ITEM,h->{h.register(id,new BlockItem(BLOCK,new Item.Properties()));VARIANTS.forEach((finish,block)->h.register(id.withPath("atlantis_"+finish+"_floor_conduit"),new BlockItem(block,new Item.Properties())));});
  event.register(Registries.BLOCK,h->{for(String finish:new String[]{"","silver","bronze","copper","blue_grey","teal","charcoal","stair_grey"}) {String name="atlantis_"+(finish.isEmpty()?"":finish+"_")+"floor";Block block=new Block(BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL));DECORATIVE_FLOORS.put(name,block);h.register(id.withPath(name),block);}});
  event.register(Registries.ITEM,h->DECORATIVE_FLOORS.forEach((name,block)->h.register(id.withPath(name),new BlockItem(block,new Item.Properties()))));
  event.register(Registries.BLOCK_ENTITY_TYPE,h->{java.util.List<Block> blocks=new java.util.ArrayList<>(VARIANTS.values());blocks.add(BLOCK);TYPE=BlockEntityType.Builder.of(FloorConduitBlockEntity::new,blocks.toArray(Block[]::new)).build(null);h.register(id,TYPE);});
 }
 
 public static final class FloorBlock extends Block implements EntityBlock {
  public FloorBlock(BlockBehaviour.Properties properties){super(properties);}
  public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new FloorConduitBlockEntity(pos,state);}
 }
}
