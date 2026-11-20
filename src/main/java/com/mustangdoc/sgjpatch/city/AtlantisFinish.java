package com.mustangdoc.sgjpatch.city;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;
/** The legacy finish is retained for existing worlds and original kits. */
public enum AtlantisFinish implements StringRepresentable {
 LEGACY,SILVER,BRONZE,COPPER,BLUE_GREY,TEAL,CHARCOAL;
 public static final EnumProperty<AtlantisFinish> PROPERTY=EnumProperty.create("finish",AtlantisFinish.class);
 public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}
 public String texture(){return this==LEGACY?"brown_terracotta":"atlantis_"+getSerializedName()+"_seamless_alloy";}
}
