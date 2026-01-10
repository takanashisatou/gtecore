package org.satou.gtecore.common.data.condition;

import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.mojang.serialization.Codec;
import org.satou.gtecore.api.registry.GTECoreRegistration;

import static org.satou.gtecore.api.registry.GTECoreRegistration.GTECore_REGISTRATE;

public class GTERecipeConditions {
    public static final RecipeConditionType<QING_LONG_CONDITION> QING_LONG = register("qing_long",QING_LONG_CONDITION::new, QING_LONG_CONDITION.CODEC);
    public static final RecipeConditionType<BAI_HU_CONDITION> BAI_HU = register("bai_hu",BAI_HU_CONDITION::new,BAI_HU_CONDITION.CODEC);
    public static final RecipeConditionType<ZHU_QUE_CONDITION> ZHU_QUE = register("zhu_que",ZHU_QUE_CONDITION::new,ZHU_QUE_CONDITION.CODEC);
    public static final RecipeConditionType<XUAN_WU_CONDITION> XUAN_WU = register("xuan_wu",XUAN_WU_CONDITION::new,XUAN_WU_CONDITION.CODEC);



    private GTERecipeConditions() {}
    public static void init(){

    }
    private static <T extends RecipeCondition> RecipeConditionType<T> register(String name,
                                                                               RecipeConditionType.ConditionFactory<T> factory,
                                                                               Codec<T> codec) {
        return GTRegistries.RECIPE_CONDITIONS.register(name, new RecipeConditionType<>(factory, codec));
    }
}
