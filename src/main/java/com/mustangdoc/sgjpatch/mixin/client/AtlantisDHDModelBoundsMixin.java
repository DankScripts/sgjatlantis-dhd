package com.mustangdoc.sgjpatch.mixin.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Permits only the three approved Atlantis DHD element endpoints that extend a
 * fraction beyond Minecraft's normal +32 model-coordinate limit.
 *
 * <p>The injection is intentionally exact rather than widening model limits for
 * every resource pack. This keeps the accepted physical table geometry intact
 * and avoids relying on renderer changes supplied by a particular modpack.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.block.model.BlockElement$Deserializer", remap = false)
public class AtlantisDHDModelBoundsMixin {
    @Inject(method = "m_111352_", at = @At("HEAD"), cancellable = true, remap = false)
    private void sgjpatch$allowApprovedAtlantisDhdBounds(
            JsonObject json,
            CallbackInfoReturnable<Vector3f> cir) {
        if (json == null) {
            return;
        }

        JsonElement element = json.get("to");
        if (element == null || !element.isJsonArray()) {
            return;
        }

        JsonArray values = element.getAsJsonArray();
        if (values.size() != 3) {
            return;
        }

        float x;
        float y;
        float z;
        try {
            x = values.get(0).getAsFloat();
            y = values.get(1).getAsFloat();
            z = values.get(2).getAsFloat();
        } catch (RuntimeException ignored) {
            return;
        }

        if (sgjpatch$boundsIsApprovedEndpoint(x, y, z)) {
            cir.setReturnValue(new Vector3f(x, y, z));
        }
    }

    private static boolean sgjpatch$boundsIsApprovedEndpoint(float x, float y, float z) {
        return sgjpatch$boundsMatches(x, y, z, 32.725F, 2.0F, 16.0F)
                || sgjpatch$boundsMatches(x, y, z, 32.56F, 1.4F, 22.4F)
                || sgjpatch$boundsMatches(x, y, z, 32.5F, 19.0F, 16.25F);
    }

    private static boolean sgjpatch$boundsMatches(
            float x,
            float y,
            float z,
            float expectedX,
            float expectedY,
            float expectedZ) {
        return sgjpatch$boundsNear(x, expectedX)
                && sgjpatch$boundsNear(y, expectedY)
                && sgjpatch$boundsNear(z, expectedZ);
    }

    private static boolean sgjpatch$boundsNear(float value, float expected) {
        return Math.abs(value - expected) < 0.0002F;
    }
}
