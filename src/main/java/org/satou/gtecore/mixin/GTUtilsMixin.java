package org.satou.gtecore.mixin;

import com.gregtechceu.gtceu.common.machine.steam.SteamSolarBoiler;
import com.gregtechceu.gtceu.utils.GTUtil;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.level.Level;
@Mixin(SteamSolarBoiler.class)
public class GTUtilsMixin {
    @Redirect(
            remap = false,
            method = "updateCurrentTemperature", // 或 SteamSolarBoiler 中调用 canSeeSunClearly 的方法名
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/gregtechceu/gtceu/utils/GTUtil;canSeeSunClearly(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean redirectCanSeeSun(Level world, BlockPos pos) {
        return true; // 永远返回 true
    }
}
