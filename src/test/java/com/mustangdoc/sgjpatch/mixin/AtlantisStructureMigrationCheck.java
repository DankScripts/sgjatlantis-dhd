package com.mustangdoc.sgjpatch.mixin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public final class AtlantisStructureMigrationCheck {
    public static void main(String[] args) throws ReflectiveOperationException {
        Method migrate = AtlantisStructureBlockMigrationMixin.class.getDeclaredMethod(
                "sgjpatch$migrateBlock", CompoundTag.class, Predicate.class);
        if (!Modifier.isPrivate(migrate.getModifiers()) || !Modifier.isStatic(migrate.getModifiers()))
            throw new AssertionError("Mixin helper must be private and static");
        migrate.setAccessible(true);
        CompoundTag original = new CompoundTag();
        original.putString("Name", "sgjadditions_capacity_patch:atlantis_floor_conduit");
        CompoundTag properties = new CompoundTag();
        properties.putString("facing", "east");
        properties.putString("waterlogged", "true");
        original.put("Properties", properties);
        ResourceLocation target = new ResourceLocation("stargate_atlantis", "atlantis_floor_conduit");
        CompoundTag migrated = (CompoundTag) migrate.invoke(null, original, (Predicate<ResourceLocation>) target::equals);
        if (migrated == null || !migrated.getString("Name").equals(target.toString())
                || !migrated.getCompound("Properties").equals(properties)
                || !original.getString("Name").equals("sgjadditions_capacity_patch:atlantis_floor_conduit"))
            throw new AssertionError("Structure migration must preserve state and the original tag");
        if (migrate.invoke(null, original, (Predicate<ResourceLocation>) id -> false) != null)
            throw new AssertionError("Unknown legacy blocks must not be changed");
        original.putString("Name", "sgjourney:atlantis_floor_conduit");
        if (migrate.invoke(null, original, (Predicate<ResourceLocation>) id -> true) != null)
            throw new AssertionError("Other mods' blocks must not be changed");
        System.out.println("PASS: structure migration preserves block states and excludes unknown blocks.");
    }
}
