package com.mustangdoc.sgjpatch.compat.moregates;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.nio.file.Path;

/** Registers the generated bridge as a required, top-priority built-in pack. */
@Mod.EventBusSubscriber(
        modid = "sgjadditions_capacity_patch",
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class MoreGates432PackEvents {
    private MoreGates432PackEvents() {
    }

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        if (!MoreGates432Compatibility.isEnabledAndApplicable()) {
            return;
        }
        if (event.getPackType() != PackType.CLIENT_RESOURCES
                && event.getPackType() != PackType.SERVER_DATA) {
            return;
        }

        Path moreGatesJar = MoreGates432Compatibility.exactJar().orElse(null);
        if (moreGatesJar == null) {
            return;
        }

        final Path packRoot;
        try {
            packRoot = MoreGates432PackGenerator.ensureReady(moreGatesJar);
        } catch (IOException exception) {
            System.out.println("[SGJPATCH] Failed to prepare the More Gates 4.3.2 compatibility pack: "
                    + exception.getMessage());
            return;
        }

        event.addRepositorySource(consumer -> {
            Pack.ResourcesSupplier resources = id ->
                    (PackResources) new PathPackResources(id, packRoot, true);
            Pack pack = Pack.readMetaAndCreate(
                    MoreGates432PackGenerator.PACK_ID,
                    Component.literal("Atlantis DHD: More Gates 4.3.2 Compatibility"),
                    true,
                    resources,
                    event.getPackType(),
                    Pack.Position.TOP,
                    PackSource.BUILT_IN
            );
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }
}
