package com.mustangdoc.sgjpatch.mixin.client;

import com.mustangdoc.sgjpatch.client.screens.AtlantisDHDScreenFixed;
import com.mustangdoc.sgjpatch.menu.AtlantisDHDCompatMenu;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerOpenScreenMixin {
    private static final Logger LOGGER = LogManager.getLogger("SGJPATCH");
    private static final ResourceLocation ATLANTIS_DHD_MENU = ResourceLocation.fromNamespaceAndPath("sgjadditions", "atlantis_dhd");
    private static final AtomicInteger OPEN_SCREEN_LOG_BUDGET = new AtomicInteger(40);

    @Inject(method = "handleOpenScreen(Lnet/minecraft/network/protocol/game/ClientboundOpenScreenPacket;)V", at = @At("HEAD"))
    private void sgjpatch$logOpenScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        sgjpatch$logOpenScreenCommon(packet);
    }

    // Optional production fallback in case remapping/refmap fails for some reason.
    @Inject(
            method = "m_5980_(Lnet/minecraft/network/protocol/game/ClientboundOpenScreenPacket;)V",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private void sgjpatch$logOpenScreen$srg(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        sgjpatch$logOpenScreenCommon(packet);
    }

    private void sgjpatch$logOpenScreenCommon(ClientboundOpenScreenPacket packet) {
        MenuType<?> menuType = packet.getType();
        ResourceLocation key = BuiltInRegistries.MENU.getKey(menuType);

        int remaining = OPEN_SCREEN_LOG_BUDGET.get();
        if (remaining > 0 && OPEN_SCREEN_LOG_BUDGET.compareAndSet(remaining, remaining - 1)) {
            LOGGER.info(
                "[SGJPATCH] OpenScreen(HEAD): menuType={} containerId={} title={} (remainingBudget={})",
                (key == null ? "<null>" : key),
                packet.getContainerId(),
                packet.getTitle(),
                remaining - 1);
        }

        boolean looksDhdLike = false;
        if (key != null) {
            String namespace = key.getNamespace();
            String path = key.getPath().toLowerCase();
            looksDhdLike = ATLANTIS_DHD_MENU.equals(key)
                    || path.contains("dhd")
                    || "sgjadditions".equals(namespace)
                    || "sgjourney".equals(namespace);
        }

        if (!looksDhdLike) {
            return;
        }

        Component title = packet.getTitle();
        LOGGER.info(
            "[SGJPATCH] Client received OpenScreen: menuType={} containerId={} title={}",
            (key == null ? "<null>" : key),
                packet.getContainerId(),
                title);
    }

    @Inject(method = "handleOpenScreen(Lnet/minecraft/network/protocol/game/ClientboundOpenScreenPacket;)V", at = @At("TAIL"))
    private void sgjpatch$forceAtlantisDhdScreen(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        sgjpatch$forceAtlantisDhdScreenCommon(packet);
    }

    // Optional production fallback in case remapping/refmap fails for some reason.
    @Inject(
            method = "m_5980_(Lnet/minecraft/network/protocol/game/ClientboundOpenScreenPacket;)V",
            at = @At("TAIL"),
            remap = false,
            require = 0
    )
    private void sgjpatch$forceAtlantisDhdScreen$srg(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        sgjpatch$forceAtlantisDhdScreenCommon(packet);
    }

    private void sgjpatch$forceAtlantisDhdScreenCommon(ClientboundOpenScreenPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        try {
            MenuType<?> menuType = packet.getType();
            ResourceLocation key = BuiltInRegistries.MENU.getKey(menuType);
            if (key != null && (ATLANTIS_DHD_MENU.equals(key)
                    || key.getPath().toLowerCase().contains("dhd")
                    || "sgjadditions".equals(key.getNamespace())
                    || "sgjourney".equals(key.getNamespace()))) {
                LOGGER.info(
                    "[SGJPATCH] OpenScreen(TAIL): menuType={} player.containerMenu={} minecraft.screen={}",
                    key,
                    (minecraft.player.containerMenu == null ? "<null>" : minecraft.player.containerMenu.getClass().getName()),
                    (minecraft.screen == null ? "<null>" : minecraft.screen.getClass().getName()));
            }
        } catch (Throwable ignored) {
        }

        if (minecraft.screen instanceof AtlantisDHDScreenFixed) {
            return;
        }

        if (!(minecraft.player.containerMenu instanceof AtlantisDHDCompatMenu)) {
            return;
        }

        if (!(minecraft.player.containerMenu instanceof AbstractDHDMenu menu)) {
            return;
        }

        // If Atlantis DHD menu was opened but the screen factory didn't fire (or was blocked), force open the screen.
        minecraft.execute(() -> {
            if (minecraft.player == null) {
                return;
            }
            if (!(minecraft.player.containerMenu instanceof AtlantisDHDCompatMenu)) {
                return;
            }
            if (minecraft.screen instanceof AtlantisDHDScreenFixed) {
                return;
            }
            try {
                minecraft.setScreen(AtlantisDHDScreenFixed.class
                        .getConstructor(AbstractDHDMenu.class, Inventory.class, Component.class)
                        .newInstance(menu, minecraft.player.getInventory(), Component.translatable("screen.sgjourney.dhd")));
            } catch (ReflectiveOperationException exception) {
                LOGGER.error("[SGJPATCH] Failed to construct the migrated Atlantis DHD screen", exception);
            }
        });
        LOGGER.warn("[SGJPATCH] OpenScreen(TAIL): AtlantisDHDMenu detected but no AtlantisDHDScreen; forcing screen open");
    }
}
