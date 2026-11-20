package com.mustangdoc.sgjpatch.mixin;
import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(AbstractDHDEntity.class)
public abstract class AtlantisRecentAddressesMixin {
    private static final String KEY="AtlantisRecentAddresses";
    @Inject(method="tick",at=@At("TAIL"),remap=false)
    private static void record(Level level,BlockPos pos,BlockState state,AbstractDHDEntity dhd,CallbackInfo ci) {
        if(level.isClientSide || level.getGameTime()%20!=0 ||
            !dhd.getClass().getName().startsWith("com.mustangdoc.sgjpatch.") ||
            dhd.getStargateOpenTime()<=0) return;
        String address=dhd.getAddress().toString();
        if(address.isBlank() || dhd.getAddress().getLength()<6)return;
        CompoundTag data=dhd.getPersistentData();
        if(address.equals(data.getString(KEY+"Last")))return;
        ListTag old=data.getList(KEY,Tag.TAG_STRING), entries=new ListTag();
        entries.add(StringTag.valueOf(address));
        for(int i=0;i<old.size() && entries.size()<8;i++)
            if(!old.getString(i).equals(address))entries.add(StringTag.valueOf(old.getString(i)));
        data.put(KEY,entries);data.putString(KEY+"Last",address);
        dhd.setChanged();level.sendBlockUpdated(pos,state,state,3);
    }
    @Inject(method="m_5995_",at=@At("RETURN"),remap=false)
    private void sync(CallbackInfoReturnable<CompoundTag> ci){
        ci.getReturnValue().put("AtlantisRecentAddresses",((AbstractDHDEntity)(Object)this).getPersistentData().getList("AtlantisRecentAddresses",Tag.TAG_STRING).copy());
    }
    @Inject(method="handleUpdateTag",at=@At("TAIL"),remap=false)
    private void receive(CompoundTag tag,CallbackInfo ci){
        ((AbstractDHDEntity)(Object)this).getPersistentData().put("AtlantisRecentAddresses",tag.getList("AtlantisRecentAddresses",Tag.TAG_STRING).copy());
    }
}