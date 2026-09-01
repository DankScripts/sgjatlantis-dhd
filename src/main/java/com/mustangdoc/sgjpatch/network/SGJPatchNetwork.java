package com.mustangdoc.sgjpatch.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Network registration with exact release-version enforcement.
 *
 * Keeping the channel protocol equal to the public mod version prevents a
 * client or server running another Atlantis DHD build from silently joining
 * with incompatible packets or synchronized state.
 */
public final class SGJPatchNetwork {
    private static final String PROTOCOL_VERSION = "2.0.2-beta.2-hotfix2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("sgjadditions_capacity_patch", "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int nextPacketId = 0;

    private SGJPatchNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(ServerboundToggleIrisPacket.class, nextPacketId++)
                .encoder(ServerboundToggleIrisPacket::encode)
                .decoder(ServerboundToggleIrisPacket::decode)
                .consumerMainThread(ServerboundToggleIrisPacket::handle)
                .add();
    }
}
