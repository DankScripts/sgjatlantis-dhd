package com.mustangdoc.sgjpatch.mixin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.povstalec.sgjourney.common.sgjourney.StargateConnection;
import net.povstalec.sgjourney.api.stargate.Stargate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve optional Lantea seat identities before SGJourney transfers passengers first. */
@Mixin(targets = "net.povstalec.sgjourney.common.sgjourney.Wormhole", remap = false)
public abstract class JumperWormholeSeatsMixin {
    private static final String SEATS = "sgjatlantis:transit_seats";
    private static boolean isJumper(Entity entity) {
        return entity != null && entity.getClass().getName().equals("io.github.aqueoxx.lantea.jumper.PuddleJumper");
    }

    @Inject(method = "recursivePassengerTeleport", at = @At("HEAD"), require = 1)
    private void capture(StargateConnection connection, ServerLevel destination, Stargate source,
                         Stargate target, Entity entity, Vec3 position, Vec3 momentum, Vec3 rotation,
                         CallbackInfoReturnable<Entity> callback) {
        if (!isJumper(entity) || entity.level() == destination) return;
        CompoundTag seats = new CompoundTag();
        try {
            Method seatOf = entity.getClass().getMethod("seatOf", Entity.class);
            for (Entity passenger : entity.getPassengers()) {
                int seat = (Integer) seatOf.invoke(entity, passenger);
                if (seat >= 0) seats.putInt(passenger.getUUID().toString(), seat);
            }
            // Forge persistent data survives copying the now-empty vehicle into the destination.
            entity.getPersistentData().put(SEATS, seats);
        } catch (ReflectiveOperationException exception) {
            System.err.println("[SGJPATCH] Could not capture jumper seats: " + exception);
        }
    }

    @Redirect(method = "recursivePassengerTeleport", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;startRiding(Lnet/minecraft/world/entity/Entity;Z)Z",
            remap = true), require = 1)
    @SuppressWarnings("unchecked")
    private boolean restore(Entity passenger, Entity vehicle, boolean force) {
        if (isJumper(vehicle)) {
            CompoundTag seats = vehicle.getPersistentData().getCompound(SEATS);
            String id = passenger.getUUID().toString();
            if (seats.contains(id)) {
                try {
                    Field field = vehicle.getClass().getDeclaredField("savedSeats");
                    field.setAccessible(true);
                    ((Map<UUID, Integer>) field.get(vehicle)).put(passenger.getUUID(), seats.getInt(id));
                } catch (ReflectiveOperationException exception) {
                    System.err.println("[SGJPATCH] Could not restore jumper seat: " + exception);
                }
            }
        }
        return passenger.startRiding(vehicle, force);
    }

    @Inject(method = "recursivePassengerTeleport", at = @At("RETURN"), require = 1)
    private void clear(StargateConnection connection, ServerLevel destination, Stargate source,
                       Stargate target, Entity entity, Vec3 position, Vec3 momentum, Vec3 rotation,
                       CallbackInfoReturnable<Entity> callback) {
        if (isJumper(entity)) entity.getPersistentData().remove(SEATS);
        Entity result = callback.getReturnValue();
        if (isJumper(result)) result.getPersistentData().remove(SEATS);
    }
}
