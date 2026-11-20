package com.mustangdoc.sgjpatch.compat;

/** Optional state carried by Lantea's gate block entity. */
public interface LanteaEnergyIrisState {
    boolean sgjpatch$irisInstalled();
    boolean sgjpatch$irisClosed();
    void sgjpatch$setIris(boolean installed, boolean closed);
}
