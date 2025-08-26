package org.satou.gtecore.mixin;

import com.gregtechceu.gtceu.common.machine.multiblock.part.SteamHatchPartMachine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SteamHatchPartMachine.class)
public class steam_hatch_mixin {
    @ModifyConstant(
            method = "<init>",
            constant = @Constant(intValue = 64000), // 替换原本用到的 42
            remap = false
    )
    private static int modifyConstant(int original) {
        return 64000000;
    }

}
