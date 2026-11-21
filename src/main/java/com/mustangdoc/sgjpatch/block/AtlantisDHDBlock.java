package com.mustangdoc.sgjpatch.block;

import com.mustangdoc.sgjpatch.compat.AtlantisDHDShapeHelper;
import com.mustangdoc.sgjpatch.compat.AtlantisDHDUseHelper;
import com.mustangdoc.sgjpatch.init.SGJPatchBlockEntities;
import com.mustangdoc.sgjpatch.menu.AtlantisDHDCompatMenu;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.blocks.dhd.AbstractDHDBlock;

public class AtlantisDHDBlock extends AbstractDHDBlock {
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

   public AtlantisDHDBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, Boolean.FALSE));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(WATERLOGGED);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = super.getStateForPlacement(context);
      return state == null
         ? null
         : state.setValue(WATERLOGGED, Boolean.valueOf(context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER));
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new AtlantisDHDEntity(pos, state);
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   public InteractionResult sgjpatch$useNormal(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (player instanceof ServerPlayer serverPlayer) {
         if (level.getBlockEntity(pos) instanceof AtlantisDHDEntity dhdEntity) {
            SimpleMenuProvider provider = new SimpleMenuProvider(
               (containerId, inventory, p) -> new AtlantisDHDCompatMenu(containerId, inventory, dhdEntity), Component.literal("Atlantis DHD")
            );
            NetworkHooks.openScreen(serverPlayer, provider, pos);
            return InteractionResult.CONSUME;
         } else {
            throw new IllegalStateException("Our named container provider is missing!");
         }
      } else {
         return InteractionResult.CONSUME;
      }
   }

   public Block getDHD() {
      return this;
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, SGJPatchBlockEntities.ATLANTIS_DHD.get(), AbstractDHDEntity::tick);
   }

   public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, level, tooltip, flag);
      tooltip.add(Component.translatable("block.sgjadditions_capacity_patch.atlantis_dhd"));
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return AtlantisDHDShapeHelper.shape(state);
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      return AtlantisDHDUseHelper.use(this, state, level, pos, player, hand, hit);
   }
}
