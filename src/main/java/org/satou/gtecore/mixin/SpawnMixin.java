package org.satou.gtecore.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import org.satou.gtecore.config.GTEConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(MinecraftServer.class)
public class SpawnMixin {


    @Inject(remap = true,method = "isSpawningMonsters",cancellable = true,at = @At("HEAD"))
    public void isSpawningMonsters(CallbackInfoReturnable<Boolean> cir) {
        if(GTEConfig.INSTANCE.superPeace) {
            cir.setReturnValue(false);
        }else{
            cir.setReturnValue(((MinecraftServer) ((Object) this)).getWorldData().getDifficulty() != Difficulty.PEACEFUL);
        }
        cir.cancel();
    }

    @Inject(remap = true,method = "isSpawningAnimals",cancellable = true,at = @At("HEAD"))
    public void isSpawningAnimals(CallbackInfoReturnable<Boolean> cir) {
        if(GTEConfig.INSTANCE.superPeace) {cir.setReturnValue(false);cir.cancel();return;}
        cir.setReturnValue(true);
        cir.cancel();
    }
}
