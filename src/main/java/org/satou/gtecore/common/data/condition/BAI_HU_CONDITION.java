package org.satou.gtecore.common.data.condition;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.common.data.GTRecipeConditions;
import com.gregtechceu.gtceu.common.recipe.condition.DaytimeCondition;

import lombok.Getter;
import net.minecraft.network.chat.Component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.satou.gtecore.common.data.machines.YIN_YANG_EIGHT_TRIGMAS_BLAST_FURNACE;

@Getter
@NoArgsConstructor
public class BAI_HU_CONDITION extends RecipeCondition {
    public static final Codec<BAI_HU_CONDITION> CODEC = RecordCodecBuilder
            .create(instance -> RecipeCondition.isReverse(instance)
                    .apply(instance, BAI_HU_CONDITION::new));

    public BAI_HU_CONDITION(boolean isReverse) {
        super(isReverse);
    }


    @Override
    public RecipeConditionType<?> getType() {
        return GTERecipeConditions.BAI_HU;
    }

    @Override
    public Component getTooltips() {
        return Component.translatable("recipe.condition.bai_hu.tooltip");
    }

    @Override
    public boolean testCondition(@NotNull GTRecipe recipe, @NotNull RecipeLogic recipeLogic) {
        MetaMachine machine = recipeLogic.getMachine();
        if(machine instanceof YIN_YANG_EIGHT_TRIGMAS_BLAST_FURNACE yinYangEightTrigmasBlastFurnace){
            return yinYangEightTrigmasBlastFurnace.BAI_HU_MODULE_ENABLED;
        }else {
            return false;
        }
    }

    @Override
    public RecipeCondition createTemplate() {
        return new BAI_HU_CONDITION();
    }
}
