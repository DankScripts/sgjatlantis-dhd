package com.mustangdoc.sgjpatch.mixin;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class SGJPatchMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        System.out.println("[SGJPATCH] Mixin plugin loaded for " + mixinPackage);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (targetClassName.startsWith("com.struxnet.sgjadditions.")) {
            return FMLLoader.getLoadingModList().getModFileById("sgjadditions") != null;
        }
        if (targetClassName.startsWith("io.github.aqueoxx.lantea.")) {
            return FMLLoader.getLoadingModList().getModFileById("lantea") != null;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
            String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
            String mixinClassName, IMixinInfo mixinInfo) {
    }
}
