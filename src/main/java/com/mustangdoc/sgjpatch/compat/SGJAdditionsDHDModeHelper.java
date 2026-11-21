package com.mustangdoc.sgjpatch.compat;

import com.mustangdoc.sgjpatch.config.SGJCompatibilityConfig;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

public final class SGJAdditionsDHDModeHelper {
   private SGJAdditionsDHDModeHelper() {
   }

   public static InteractionResult use(BlockState var0, Level var1, BlockPos var2, Player var3, InteractionHand var4, BlockHitResult var5) {
      if (SGJCompatibilityConfig.useOriginalSGJAdditionsDHD()) {
         return useSgJourneyPegasusDialer(var0, var1, var2, var3, var4, var5);
      } else if (var1 != null && !var1.isClientSide()) {
         try {
            BlockState var6 = replacementState(var0);
            preserveAndReplace(var1, var2, var6);
            Block var7 = upgradedBlock();
            InteractionResult var8 = invokeUse(var7, var6, var1, var2, var3, var4, var5);
            return var8 == null ? InteractionResult.SUCCESS : var8;
         } catch (Throwable var9) {
            System.err.println("[SGJPATCH] Failed to replace SGJ Additions Atlantis DHD with upgraded DHD at " + var2 + ": " + var9);
            var9.printStackTrace();
            return InteractionResult.FAIL;
         }
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   public static void convertLoadedLegacy(BlockEntity var0) {
      if (var0 != null && !SGJCompatibilityConfig.useOriginalSGJAdditionsDHD()) {
         try {
            Level var1 = var0.getLevel();
            if (var1 == null || var1.isClientSide()) {
               return;
            }

            BlockPos var2 = var0.getBlockPos();
            BlockState var3 = var1.getBlockState(var2);
            preserveAndReplace(var1, var2, replacementState(var3));
            System.out.println("[SGJPATCH] Replaced legacy SGJ Additions Atlantis DHD with upgraded Atlantis DHD at " + var2);
         } catch (Throwable var4) {
            System.err.println("[SGJPATCH] Deferred legacy Atlantis DHD replacement at " + safePos(var0) + ": " + var4);
         }
      }
   }

   private static String safePos(BlockEntity var0) {
      try {
         return String.valueOf(var0.getBlockPos());
      } catch (Throwable var2) {
         return "<unknown>";
      }
   }

   private static Block upgradedBlock() throws Exception {
      Class var0 = Class.forName("com.mustangdoc.sgjpatch.init.SGJPatchBlocks");
      Field var1 = var0.getField("ATLANTIS_DHD");
      Object var2 = var1.get(null);
      Object var3 = var2.getClass().getMethod("get").invoke(var2);
      if (!(var3 instanceof Block)) {
         throw new IllegalStateException("ATLANTIS_DHD registry object did not contain a Block");
      } else {
         return (Block)var3;
      }
   }

   private static BlockState replacementState(BlockState var0) throws Exception {
      BlockState var1 = upgradedBlock().defaultBlockState();
      if (var0 == null) {
         return var1;
      } else {
         try {
            Direction var2 = var0.getValue(HorizontalDirectionalBlock.FACING);
            var1 = var1.setValue(HorizontalDirectionalBlock.FACING, var2);
         } catch (Throwable var4) {
         }

         try {
            Boolean var5 = var0.getValue(BlockStateProperties.WATERLOGGED);
            var1 = var1.setValue(BlockStateProperties.WATERLOGGED, var5);
         } catch (Throwable var3) {
         }

         return var1;
      }
   }

   private static void preserveAndReplace(Level var0, BlockPos var1, BlockState var2) throws Exception {
      BlockEntity var3 = var0.getBlockEntity(var1);
      CompoundTag var4 = trySave(var3);
      if (!var0.setBlock(var1, var2, 3)) {
         throw new IllegalStateException("Level#setBlock returned false");
      } else {
         if (var4 != null) {
            BlockEntity var5 = var0.getBlockEntity(var1);
            if (var5 != null) {
               try {
                  var5.load(var4);
                  tryInvokeNoArgs(var5, "m_6596_", "setChanged");
               } catch (Throwable var7) {
                  System.err.println("[SGJPATCH] Atlantis DHD block converted but legacy NBT could not be restored: " + var7);
               }
            }
         }
      }
   }

   private static CompoundTag trySave(BlockEntity var0) {
      if (var0 == null) {
         return null;
      } else {
         String[] var1 = new String[]{"m_187482_", "m_187480_", "saveWithFullMetadata", "saveWithoutMetadata"};

         for (String var5 : var1) {
            try {
               Method var6 = var0.getClass().getMethod(var5);
               Object var7 = var6.invoke(var0);
               if (var7 instanceof CompoundTag) {
                  return (CompoundTag)var7;
               }
            } catch (Throwable var8) {
            }
         }

         return null;
      }
   }

   private static void tryInvokeNoArgs(Object var0, String... var1) {
      for (String var5 : var1) {
         try {
            var0.getClass().getMethod(var5).invoke(var0);
            return;
         } catch (Throwable var7) {
         }
      }
   }

   private static InteractionResult invokeUse(Block var0, BlockState var1, Level var2, BlockPos var3, Player var4, InteractionHand var5, BlockHitResult var6) throws Exception {
      for (String var10 : new String[]{"m_6227_", "use"}) {
         try {
            Method var11 = var0.getClass()
               .getMethod(var10, BlockState.class, Level.class, BlockPos.class, Player.class, InteractionHand.class, BlockHitResult.class);
            Object var12 = var11.invoke(var0, var1, var2, var3, var4, var5, var6);
            if (var12 instanceof InteractionResult) {
               return (InteractionResult)var12;
            }
         } catch (NoSuchMethodException var13) {
         }
      }

      return null;
   }

   private static InteractionResult useSgJourneyPegasusDialer(
      BlockState var0, Level var1, BlockPos var2, Player var3, InteractionHand var4, BlockHitResult var5
   ) {
      try {
         Block var6 = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("sgjourney", "pegasus_dhd"));
         if (var6 == null) {
            for (ResourceLocation var8 : ForgeRegistries.BLOCKS.getKeys()) {
               String var9 = var8.getPath().toLowerCase(Locale.ROOT);
               if ("sgjourney".equals(var8.getNamespace()) && var9.contains("pegasus") && var9.contains("dhd")) {
                  var6 = ForgeRegistries.BLOCKS.getValue(var8);
                  if (var6 != null) {
                     break;
                  }
               }
            }
         }

         if (var6 == null) {
            throw new IllegalStateException("Could not locate SGJourney Pegasus DHD block");
         } else {
            InteractionResult var11 = invokeUse(var6, var0, var1, var2, var3, var4, var5);
            if (var11 == null) {
               throw new NoSuchMethodException("Pegasus DHD use method not found on " + var6.getClass().getName());
            } else {
               return var11;
            }
         }
      } catch (Throwable var10) {
         System.err.println("[SGJPATCH] Failed to route SGJ Additions Atlantis DHD to SGJourney Pegasus dialer: " + var10);
         var10.printStackTrace();
         return InteractionResult.FAIL;
      }
   }
}
