package org.satou.gtecore.mixin;

import guideme.internal.GuideMEClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GuideMEClient.class)
public class GuideMEMixin {

    /**
     * @author Satou
     * @reason To Solve RunClient Bugs
     */
    @Overwrite(remap = false)
    public boolean isIgnoreTranslatedGuides() {
        return true;
    }
}
