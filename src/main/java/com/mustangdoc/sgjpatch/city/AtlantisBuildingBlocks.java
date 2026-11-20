package com.mustangdoc.sgjpatch.city;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Atlantis building pieces, including locally ported SGJ Additions model shapes. */
@Mod.EventBusSubscriber(modid = AtlantisBuildingBlocks.NS, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AtlantisBuildingBlocks {
    public static final String NS = "sgjadditions_capacity_patch";
    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    // Forge loads event subscriber classes after the vanilla block registry is frozen.
    // Construct the blocks only when Forge opens the block RegisterEvent.
    private static void createBlocks() {
        if (!BLOCKS.isEmpty()) return;
        for (String color : new String[]{"blue", "brown", "copper", "green", "red", "lightblue"}) {
            add(color + "ancientblock", new Block(stone()));
            add(color + "ancientwall", new FacingBlock(stone()));
        }
        // Additional metallic architecture: preserve all legacy block IDs and artwork.
        for (String finish : new String[]{"silver", "bronze", "copper", "blue_grey", "teal", "charcoal"}) {
            for (String design : new String[]{"alloy_panel", "recessed_panel", "ribbed_panel",
                    "inset_panel", "service_panel", "illuminated_panel", "seamless_alloy"}) {
                BlockBehaviour.Properties properties = metal();
                if (design.equals("illuminated_panel")) properties.lightLevel(state -> 15);
                add("atlantis_" + finish + "_" + design, design.equals("illuminated_panel") ? new SeamPanelBlock(properties) : new Block(properties));
            }
        }
        // Preserve the retired add-on's misspelled brown ID for easy world conversion.
        Block brown = BLOCKS.remove("brownancientblock");
        add("brownancioentblock", brown);
        add("ancientpillar", new AtlantisPillarBlock(stone().noOcclusion().lightLevel(state -> 12)));
        add("ancientstairs", new AtlantisOpenStairs(Blocks.STONE.defaultBlockState(), stone().noOcclusion().lightLevel(
                state -> 11)));
        add("ancientslab", new SlabBlock(stone()));
        add("atlantis_metal_block", new Block(metal()));
        add("atlantis_stone_block", new Block(stone()));
        add("atlantis_metal_slab", new SlabBlock(metal()));
        add("atlantis_stone_slab", new SlabBlock(stone()));
        add("atlantis_red_terracotta_slab", new SlabBlock(stone()));
        add("atlantis_metal_stairs", new AtlantisPlainStairs(Blocks.STONE.defaultBlockState(), metal()));
        add("atlantis_stone_stairs", new AtlantisPlainStairs(Blocks.STONE.defaultBlockState(), stone()));
        add("atlantis_inscribed_stairs", new AtlantisGlowStairs(Blocks.STONE.defaultBlockState(), metal().noOcclusion().lightLevel(
                state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 11 : 0)));
        add("atlantis_inscribed_slab", new AtlantisGlowSlab(metal().noOcclusion().lightLevel(
                state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 11 : 0)));
        add("ancientlight", new AtlantisWallLight(light(), false));
        add("ancientlight_ceiling", new AtlantisCeilingLight(light()));
        add("ancientlight_2", new AtlantisWallLight(light(), true));
        add("wallpanel", new FacingBlock(stone().noOcclusion().lightLevel(state -> 10)));
        for (String name : new String[]{"advancedclamptop", "advancedclampbottom", "basicclamp", "advancedpost"})
            add(name, new FacingBlock(metal().noOcclusion().lightLevel(state -> 10)));
        createPoweredCopies();
    }

    private static void createPoweredCopies() {
        add("powered_ancientpillar", new PoweredLightBlocks.Pillar(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 12 : 0)));
        add("powered_ancientstairs", new PoweredLightBlocks.OpenStairs(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 11 : 0)));
        add("powered_atlantis_inscribed_stairs", new PoweredLightBlocks.Stairs(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 11 : 0)));
        add("powered_atlantis_inscribed_slab", new PoweredLightBlocks.Slab(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 11 : 0)));
        add("powered_ancientlight", new PoweredLightBlocks.Wall(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 15 : 0)));
        add("powered_ancientlight_2", new PoweredLightBlocks.WideWall(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 15 : 0)));
        add("powered_wallpanel", new PoweredLightBlocks.Facing(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 10 : 0)));
        add("powered_advancedclamptop", new PoweredLightBlocks.Facing(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 10 : 0)));
        add("powered_advancedclampbottom", new PoweredLightBlocks.Facing(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 10 : 0)));
        add("powered_basicclamp", new PoweredLightBlocks.Facing(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 10 : 0)));
        add("powered_advancedpost", new PoweredLightBlocks.Facing(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 10 : 0)));
        add("powered_atlantis_silver_illuminated_panel", new PoweredLightBlocks.Panel(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 15 : 0)));
        add("powered_atlantis_bronze_illuminated_panel", new PoweredLightBlocks.Panel(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 15 : 0)));
        add("powered_atlantis_copper_illuminated_panel", new PoweredLightBlocks.Panel(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 15 : 0)));
        add("powered_atlantis_blue_grey_illuminated_panel", new PoweredLightBlocks.Panel(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 15 : 0)));
        add("powered_atlantis_teal_illuminated_panel", new PoweredLightBlocks.Panel(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 15 : 0)));
        add("powered_atlantis_charcoal_illuminated_panel", new PoweredLightBlocks.Panel(metal().noOcclusion().lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 15 : 0)));
    }

    private static void add(String name, Block block) { BLOCKS.put(name, block); }
    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.STONE);
    }
    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL);
    }
    private static BlockBehaviour.Properties light() {
        return BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.GLASS)
                .noOcclusion().lightLevel(state -> 15);
    }

    public static Block stairs() { return BLOCKS.get("ancientstairs"); }
    public static Block get(String name) { return BLOCKS.get(name); }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.BLOCKS, helper -> {
            createBlocks();
            BLOCKS.forEach((name, block) -> helper.register(new ResourceLocation(NS, name), block));
        });
        event.register(ForgeRegistries.Keys.ITEMS, helper -> BLOCKS.forEach((name, block) -> {
            Item item = AtlantisUnreleasedItems.item(name, block);
            ITEMS.put(name, item);
            helper.register(new ResourceLocation(NS, name), item);
        }));
    }

    public static class FacingBlock extends HorizontalDirectionalBlock {
        public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
        public FacingBlock(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
    }
}

