package com.mustangdoc.sgjpatch.transporter;
import com.mustangdoc.sgjpatch.city.AtlantisFinish;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = "sgjatlantis_dhd", bus=EventBusSubscriber.Bus.MOD)
public final class TransporterRegistry {
    public static final String NS="sgjadditions_capacity_patch";
    public static TransporterBlock BLOCK;
    public static TransporterBlock.Controller PANEL;
    public static TransporterRoomItem ITEM;
    public static final java.util.Map<AtlantisFinish,TransporterRoomItem> FINISH_KITS=new java.util.EnumMap<>(AtlantisFinish.class);
    public static Item CONTROL_ITEM,MAP_ITEM;
    public static BlockEntityType<TransporterEntity> ENTITY;
    public static MenuType<TransporterMenu> MENU;
    public static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath(NS,name); }
    @SubscribeEvent public static void common(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e) {
        TransporterPackets.init(e);
    }
    @SubscribeEvent public static void register(RegisterEvent e) {
        e.register(Registries.BLOCK,h->{ BLOCK=new TransporterBlock();h.register(id("atlantis_transporter"),BLOCK);
            PANEL=new TransporterBlock.Controller();h.register(id("atlantis_transporter_panel"),PANEL); });
        e.register(Registries.ITEM,h->{ ITEM=new TransporterRoomItem();h.register(id("atlantis_transporter_room"),ITEM);for(AtlantisFinish finish:AtlantisFinish.values())if(finish!=AtlantisFinish.LEGACY){
 TransporterRoomItem kit=new TransporterRoomItem(finish);FINISH_KITS.put(finish,kit);h.register(id("atlantis_"+finish.getSerializedName()+"_transporter_room"),kit);
}
            CONTROL_ITEM=new TransporterComponentItem(TransporterBlock.Part.SWITCH);h.register(id("atlantis_transporter_control"),CONTROL_ITEM);
            MAP_ITEM=new TransporterComponentItem(TransporterBlock.Part.MAP);h.register(id("atlantis_transporter_map"),MAP_ITEM);
        });
        e.register(Registries.BLOCK_ENTITY_TYPE,h->{ 
            ENTITY=BlockEntityType.Builder.of(TransporterEntity::new,PANEL).build(null);
            h.register(id("atlantis_transporter"),ENTITY);
        });
        e.register(Registries.MENU,h->{ MENU=IMenuTypeExtension.create(TransporterMenu::new);h.register(id("atlantis_transporter"),MENU); });
    }
    @EventBusSubscriber(modid = "sgjatlantis_dhd",bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent public static void setup(RegisterMenuScreensEvent e) {
            e.register(MENU,TransporterScreen::new);
        }
    }
}

