package com.mustangdoc.sgjpatch.compat;

import com.mustangdoc.sgjpatch.block.AtlantisDHDBlock;
import com.mustangdoc.sgjpatch.menu.AtlantisDHDCrystalMenu;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/** Routes normal right-click to the dialer and sneak-right-click to Crystal Controls. */
public final class AtlantisDHDUseHelper {
    private AtlantisDHDUseHelper() {}

    public static InteractionResult use(AtlantisDHDBlock block, BlockState state,
            Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof AtlantisDHDEntity) {
                    SimpleMenuProvider provider = new SimpleMenuProvider(
                            (containerId, inventory, menuPlayer) ->
                                    new AtlantisDHDCrystalMenu(containerId, inventory, blockEntity),
                            Component.literal("Atlantis DHD Crystal / Power Unit"));
                    NetworkHooks.openScreen(serverPlayer, provider, pos);
                }
            }
            return InteractionResult.CONSUME;
        }

        return block.sgjpatch$useNormal(state, level, pos, player, hand, hit);
    }
}
