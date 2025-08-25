package org.satou.gtecore.mixin;

import org.satou.gtecore.config.GTEConfig;

import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.nbt.CompoundTag;

import com.google.gson.JsonObject;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GTRecipeBuilder.class)
@Accessors(chain = true, fluent = true)
public class GTRecipeBuilderMixin {

    @Shadow(remap = false)
    public GTRecipeType recipeType;

    @Shadow(remap = false)
    public int duration;

    @Shadow(remap = false)
    public @NotNull CompoundTag data = new CompoundTag();

    @Shadow(remap = false)
    public GTRecipeBuilder duration(int duration) {
        return null;
    }

    @Unique
    private long gTECore$eut = 0;

    @Inject(method = "EUt(J)Lcom/gregtechceu/gtceu/data/recipe/builder/GTRecipeBuilder;", at = @At("HEAD"), remap = false)
    private void eu(long eu, CallbackInfoReturnable<GTRecipeBuilder> cir) {
        gTECore$eut = eu;
        this.data.putInt("euTier", GTUtil.getTierByVoltage(eu > 0 ? eu : -eu));
    }

    @Unique
    private int gTECore$getDuration() {
        if (GTEConfig.INSTANCE.durationMultiplier == 1 || gTECore$eut < 0) {
            return Math.abs(duration);
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, Math.abs(duration * GTEConfig.INSTANCE.durationMultiplier)));
    }

    @Inject(method = "toJson", at = @At("TAIL"), remap = false)
    public void toJson(JsonObject json, CallbackInfo ci) {
        json.addProperty("duration", gTECore$getDuration());
    }
}
