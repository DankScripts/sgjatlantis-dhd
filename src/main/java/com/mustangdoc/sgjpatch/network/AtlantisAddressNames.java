package com.mustangdoc.sgjpatch.network;

import java.util.Locale;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.data.Universe;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.AddressRegion;

/** Resolves a displayed name with the same SGJourney address tables used when dialing. */
public final class AtlantisAddressNames {
    private AtlantisAddressNames() {}

    public static String resolve(Level level, String text) {
        if (level.isClientSide || text.length() > 80) return "Unknown destination";
        try {
            Address address = new Address.Immutable(text);
            if (address.getLength() == 9) {
                var gate = StargateNetwork.get(level).getStargate(address);
                if (gate == null) return mobsDestination(level, address);
                AddressRegion region = Universe.get(level).getAddressRegionFromDimension(gate.getDimension());
                return region == null ? dimension(gate.getDimension().location().toString()) : regionName(region);
            }
            Universe universe = Universe.get(level);
            AddressRegion region = address.getLength() == 8
                    ? universe.getAddressRegionFromExtragalacticAddress(address)
                    : universe.getSameGalaxyAddressRegion(universe.getAddressRegionFromDimension(level.dimension()), address);
            if (region == null) return mobsDestination(level, address);
            return regionName(region);
        } catch (RuntimeException exception) {
            return "Unknown destination";
        }
    }

    private static String mobsDestination(Level level, Address address) {
        if (!(level instanceof ServerLevel serverLevel)) return "Unknown destination";
        var gate = com.mustangdoc.sgjpatch.compat.SgmobsGateBridge.destination(serverLevel, address.toArray());
        return gate == null || gate.getLevel() == null ? "Unknown destination"
                : dimension(gate.getLevel().dimension().location().toString());
    }

    private static String regionName(AddressRegion region) {
        return region.getName().isBlank() ? region.getSpaceLocations().isEmpty() ? "Unknown destination"
                : dimension(region.getSpaceLocations().get(0).getDimension().location().toString())
                : region.isGenerated ? region.getName() : "@" + region.getName();
    }

    private static String dimension(String id) {
        if (id.equals("minecraft:overworld")) return "Earth / Overworld";
        if (id.equals("minecraft:the_nether")) return "Nether";
        if (id.equals("minecraft:the_end")) return "The End";
        String path = id.substring(id.indexOf(':') + 1).replace('_', ' ').replace('/', ' ');
        String[] words = path.split(" +");
        StringBuilder result = new StringBuilder();
        for (String word : words) if (!word.isEmpty()) {
            if (!result.isEmpty()) result.append(' ');
            result.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return result.toString();
    }
}
