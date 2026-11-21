package com.mustangdoc.sgjpatch.client.sound;

import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;

/** Sound follows the requested motion instead of the closed end stop. */
public final class IrisSoundState {
    private IrisSoundState() { }
    public static boolean active(IrisInfo iris) {
        return iris.getIrisMotion().isClosing()
                || (!iris.getIrisMotion().isOpening() && iris.isIrisClosed());
    }
}
