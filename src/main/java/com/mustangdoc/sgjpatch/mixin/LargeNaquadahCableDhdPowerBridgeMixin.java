package com.mustangdoc.sgjpatch.mixin;
import com.mustangdoc.sgjpatch.power.ConcealedFloorDhdPowerBridge;
import com.mustangdoc.sgjpatch.power.MixedPowerTransfer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.povstalec.sgjourney.common.block_entities.tech.CableBlockEntity;
import net.povstalec.sgjourney.common.data.ConduitNetworks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=CableBlockEntity.class,remap=false)
public abstract class LargeNaquadahCableDhdPowerBridgeMixin {
 @Redirect(method="transferEnergy",at=@At(value="INVOKE",target="Lnet/povstalec/sgjourney/common/data/ConduitNetworks$ConduitNetwork;transferEnergy(Lnet/minecraft/world/level/Level;JZZ)J"))
 private long sgjpatch$guardMixedTransfer(ConduitNetworks.ConduitNetwork network,Level level,long amount,boolean simulate,boolean zeroPoint){
  return MixedPowerTransfer.run(network,amount,simulate,zeroPoint,()->network.transferEnergy(level,amount,simulate,zeroPoint));
 }
 @Redirect(method="validOutputs",at=@At(value="INVOKE",target="Lnet/neoforged/neoforge/energy/IEnergyStorage;receiveEnergy(IZ)I"))
 private int sgjpatch$probeMatchingEnergy(IEnergyStorage output,int amount,boolean simulate){
  return MixedPowerTransfer.probeCableOutput(output,amount,simulate);
 }
 @Inject(method="update",at=@At("HEAD"))
 private void sgjpatch$invalidateRoutes(CallbackInfo ci){ConcealedFloorDhdPowerBridge.invalidate((BlockEntity)(Object)this);}
 @Inject(method="getConnectedSides",at=@At("RETURN"),cancellable=true)
 private void sgjpatch$addVirtualDhdOutput(CallbackInfoReturnable<List<Direction>> ci){
  if(!((Object)this instanceof CableBlockEntity.LargeNaquadahCable)||ci.getReturnValue()==null)return;
  BlockEntity cable=(BlockEntity)(Object)this;Level level=cable.getLevel();if(level==null)return;
  List<Direction> sides=new ArrayList<>(ci.getReturnValue());
  for(Direction side:ConcealedFloorDhdPowerBridge.outputRoutes(level,cable.getBlockPos()).keySet())if(!sides.contains(side))sides.add(side);
  ci.setReturnValue(sides);
 }
}
