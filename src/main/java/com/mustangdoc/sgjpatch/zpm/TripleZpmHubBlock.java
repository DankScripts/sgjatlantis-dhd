package com.mustangdoc.sgjpatch.zpm;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.povstalec.sgjourney.common.block_entities.ProtectedBlockEntity;
import net.povstalec.sgjourney.common.blocks.ProtectedBlock;

/** Ported from SG Mobs with permission from Mike; retains SGJourney ZPM behavior. */
public final class TripleZpmHubBlock extends BaseEntityBlock implements ProtectedBlock {
   public static final MapCodec<TripleZpmHubBlock> CODEC = simpleCodec(TripleZpmHubBlock::new);
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

   public TripleZpmHubBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   protected MapCodec<TripleZpmHubBlock> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      b.add(new Property[]{FACING});
   }

   public static BlockPos base(BlockPos pos, BlockState state) {
      return pos;
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new TripleZpmHubBlockEntity(pos, state);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide() ? null : createTickerHelper(type, TripleZpmHubRegistration.BLOCK_ENTITY, TripleZpmHubBlockEntity::tick);
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      BlockPos origin = base(pos, state);
      if (level.getBlockEntity(origin) instanceof TripleZpmHubBlockEntity hub) {
         if (!hub.hasPermissions(player, true)) {
            return InteractionResult.FAIL;
         }

         if (player instanceof ServerPlayer sp) {
            sp.openMenu(
               new SimpleMenuProvider(
                  (id, inventory, p) -> new TripleZpmHubMenu(id, inventory, hub), Component.translatable("block.sgjatlantis_dhd.triple_zpm_hub")
               ),
               buf -> buf.writeBlockPos(origin)
            );
         }
      }

      return InteractionResult.sidedSuccess(level.isClientSide());
   }

   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
      if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof TripleZpmHubBlockEntity hub) {
         hub.drops();
      }

      super.onRemove(state, level, pos, next, moving);
   }

   public ProtectedBlockEntity getProtectedBlockEntity(BlockGetter level, BlockPos pos, BlockState state) {
      return level.getBlockEntity(base(pos, state)) instanceof TripleZpmHubBlockEntity hub ? hub : null;
   }

   public boolean hasPermissions(BlockGetter level, BlockPos pos, BlockState state, Player player, boolean sendMessage) {
      if (level.getBlockEntity(base(pos, state)) instanceof TripleZpmHubBlockEntity hub && !hub.hasPermissions(player, sendMessage)) {
         return false;
      }

      return true;
   }
}
