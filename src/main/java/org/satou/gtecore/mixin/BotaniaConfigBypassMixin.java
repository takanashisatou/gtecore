package org.satou.gtecore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "vazkii.botania.forge.ForgeBotaniaConfig$Client", remap = false)
public class BotaniaConfigBypassMixin {

    /**
     * @author takanashisatou
     * @reason Bypass dev-only early config check during splash reload
     */
    @Overwrite
    public boolean splashesEnabled() {
        return true;
    }
}
