package com.mustangdoc.sgjpatch.transporter;
import com.mustangdoc.sgjpatch.city.AtlantisFinish;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

@Mod.EventBusSubscriber(modid=TransporterRegistry.NS, bus=Mod.EventBusSubscriber.Bus.MOD)
public final class TransporterRegistry {
    public static final String NS="sgjadditions_capacity_patch";
    public static TransporterBlock BLOCK;
    public static TransporterBlock.Controller PANEL;
    public static TransporterRoomItem ITEM;
    public static final java.util.Map<AtlantisFinish,TransporterRoomItem> FINISH_KITS=new java.util.EnumMap<>(AtlantisFinish.class);
    public static Item CONTROL_ITEM,MAP_ITEM;
    public static BlockEntityType<TransporterEntity> ENTITY;
    public static MenuType<TransporterMenu> MENU;
    public static ResourceLocation id(String name) { return new ResourceLocation(NS,name); }
    @SubscribeEvent public static void common(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent e) {
        TransporterPackets.init();
    }
    @SubscribeEvent public static void register(RegisterEvent e) {
        e.register(ForgeRegistries.Keys.BLOCKS,h->{ BLOCK=new TransporterBlock();h.register(id("atlantis_transporter"),BLOCK);
            PANEL=new TransporterBlock.Controller();h.register(id("atlantis_transporter_panel"),PANEL); });
        e.register(ForgeRegistries.Keys.ITEMS,h->{ ITEM=new TransporterRoomItem();h.register(id("atlantis_transporter_room"),ITEM);for(AtlantisFinish finish:AtlantisFinish.values())if(finish!=AtlantisFinish.LEGACY){
 TransporterRoomItem kit=new TransporterRoomItem(finish);FINISH_KITS.put(finish,kit);h.register(id("atlantis_"+finish.getSerializedName()+"_transporter_room"),kit);
}
            CONTROL_ITEM=new TransporterComponentItem(TransporterBlock.Part.SWITCH);h.register(id("atlantis_transporter_control"),CONTROL_ITEM);
            MAP_ITEM=new TransporterComponentItem(TransporterBlock.Part.MAP);h.register(id("atlantis_transporter_map"),MAP_ITEM);
        });
        e.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES,h->{
            ENTITY=BlockEntityType.Builder.of(TransporterEntity::new,PANEL).build(null);
            h.register(id("atlantis_transporter"),ENTITY);
        });
        e.register(ForgeRegistries.Keys.MENU_TYPES,h->{ MENU=IForgeMenuType.create(TransporterMenu::new);h.register(id("atlantis_transporter"),MENU); });
    }
    @Mod.EventBusSubscriber(modid=NS,bus=Mod.EventBusSubscriber.Bus.MOD,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent e) {
            e.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(MENU,TransporterScreen::new));
        }
    }
}

