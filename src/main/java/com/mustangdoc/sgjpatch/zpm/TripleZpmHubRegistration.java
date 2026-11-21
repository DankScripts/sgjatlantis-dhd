package com.mustangdoc.sgjpatch.zpm;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(
   modid = "sgjatlantis_dhd",
   bus = Bus.MOD
)
/** Ported from SG Mobs with permission from Mike; retains SGJourney ZPM behavior. */
public final class TripleZpmHubRegistration {
   public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("sgjatlantis_dhd", "triple_zpm_hub");
   public static TripleZpmHubBlock BLOCK;
   public static BlockEntityType<TripleZpmHubBlockEntity> BLOCK_ENTITY;
   public static MenuType<TripleZpmHubMenu> MENU;

   private TripleZpmHubRegistration() {
   }

   @SubscribeEvent
   public static void register(RegisterEvent event) {
      event.register(Registries.BLOCK, h -> {
         BLOCK = new TripleZpmHubBlock(Properties.of().strength(4.0F).requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.BLOCK));
         h.register(ID, BLOCK);
      });
      event.register(Registries.ITEM, h -> h.register(ID, new BlockItem(BLOCK, new net.minecraft.world.item.Item.Properties())));
      event.register(Registries.BLOCK_ENTITY_TYPE, h -> {
         BLOCK_ENTITY = Builder.of(TripleZpmHubBlockEntity::new, new Block[]{BLOCK}).build(null);
         h.register(ID, BLOCK_ENTITY);
      });
      event.register(Registries.MENU, h -> {
         MENU = IMenuTypeExtension.create(TripleZpmHubMenu::new);
         h.register(ID, MENU);
      });
   }

   @SubscribeEvent
   public static void capabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, BLOCK_ENTITY, (be, side) -> be.getItemHandler(side));
      event.registerBlockEntity(EnergyStorage.BLOCK, BLOCK_ENTITY, (be, side) -> be.getEnergyHandler(side));
   }
}
