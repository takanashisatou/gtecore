package org.satou.gtecore.data.recipe;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.OreProperty;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.L;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Lubricant;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Water;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ELECTROMAGNETIC_SEPARATOR_RECIPES;
import static org.satou.gtecore.common.data.GTERecipeTypes.ORE_PROCESS_CENTER;

public class ORE_RECIPE_CENTER_HANDLER {
    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        for(Material material : GTCEuAPI.materialManager.getRegisteredMaterials()){
            if(material.hasFlag(MaterialFlags.NO_UNIFICATION)) continue;
            if(material.hasProperty(PropertyKey.ORE)){
                crushcrushWash(provider,material);
                crushcrushCentrifuge(provider,material);
                crushWashHotCentrifugeCrush(provider,material);
                crushHotCentrifugeCrush(provider,material);
                crushWashCrushWash(provider,material);
                crushWashCrushCentrifugeOrElectric(provider,material);
            }
        }


    }

    private static void crushcrushWash(@NotNull Consumer<FinishedRecipe> provider, Material material) {
        //粉碎 粉碎 洗矿
        OreProperty property = material.getProperty(PropertyKey.ORE);
        Material byproductMaterial = property.getOreByProduct(0, material);
        ItemStack byproductStackFirst = ChemicalHelper.get(gem, byproductMaterial);
        ItemStack dustStack = ChemicalHelper.get(dust, material);
        if (byproductStackFirst.isEmpty()) {
            byproductStackFirst = ChemicalHelper.get(dust, byproductMaterial);

        }
        Material byproductMaterialSecond = property.getOreByProduct(0, material);
        ORE_PROCESS_CENTER.recipeBuilder(material.getName() + "ore_process_center_crush_crush_wash")
                .EUt(20)
                .duration(20)
                .inputFluids(Water.getFluid(1))
                .inputItems(rawOre, material)
                .outputItems(dustStack.copyWithCount(property.getOreMultiplier() * 5))
                .outputItems(byproductStackFirst)
                .outputItems(ChemicalHelper.get(dust, byproductMaterialSecond, property.getByProductMultiplier()))
                .circuitMeta(1)
                .save(provider);
    }
    private static void crushcrushCentrifuge(@NotNull Consumer<FinishedRecipe> provider, Material material){
        //粉碎 粉碎 离心
        OreProperty property = material.getProperty(PropertyKey.ORE);
        Material byproductMaterial = property.getOreByProduct(0, material);
        ItemStack byproductStackFirst = ChemicalHelper.get(gem, byproductMaterial);
        ItemStack dustStack = ChemicalHelper.get(dust, material);
        if (byproductStackFirst.isEmpty()) {
            byproductStackFirst = ChemicalHelper.get(dust, byproductMaterial);
        }
        Material byproductMaterialSecond = property.getOreByProduct(0, material);
        GTRecipeBuilder builder = ORE_PROCESS_CENTER.recipeBuilder(material.getName() + "ore_process_center_crush_crush_centrifuge")
                .EUt(20)
                .duration(20)
                .inputFluids(Lubricant.getFluid(1))
                .inputItems(rawOre, material)
                .outputItems(dustStack.copyWithCount(property.getOreMultiplier() * 5))
                .outputItems(byproductStackFirst)
                .outputItems(ChemicalHelper.get(dust, byproductMaterialSecond, property.getByProductMultiplier()))
                .circuitMeta(2);
        if (byproductMaterial.hasProperty(PropertyKey.DUST)) {
            builder.outputItems(TagPrefix.dust, byproductMaterial);

        } else {
            builder.outputFluids(byproductMaterial.getFluid(L / 9));
        }
        builder.save(provider);
    }
    private static void crushWashHotCentrifugeCrush(@NotNull Consumer<FinishedRecipe> provider, Material material){
        OreProperty property = material.getProperty(PropertyKey.ORE);
        Material byproductMaterial = property.getOreByProduct(0, material);
        Material byproductMaterialSecond = property.getOreByProduct(1, material);
        ItemStack byproductStackFirst = ChemicalHelper.get(gem, byproductMaterial);
        ItemStack byproductStackThird = ChemicalHelper.get(dust, property.getOreByProduct(2, material), 1);
        ItemStack dustStack = ChemicalHelper.get(dust, material);
        if (byproductStackFirst.isEmpty()) {
            byproductStackFirst = ChemicalHelper.get(dust, byproductMaterial);
        }
        GTRecipeBuilder builder = ORE_PROCESS_CENTER.recipeBuilder(material.getName() + "ore_process_center_crush_wash_hot_centrifuge_crush")
                .EUt(20)
                .duration(20)
                .inputFluids(Lubricant.getFluid(1),Water.getFluid(1))
                .inputItems(rawOre, material)
                .outputItems(dustStack.copyWithCount(property.getOreMultiplier() * 5))
                .outputItems(byproductStackFirst)
                .outputItems(TagPrefix.dust, byproductMaterial)
                .outputItems(TagPrefix.dust, byproductMaterialSecond)
                .outputItems(byproductStackThird)
                .circuitMeta(3);
        builder.save(provider);
    }
    private static void crushHotCentrifugeCrush(@NotNull Consumer<FinishedRecipe> provider, Material material) {
        OreProperty property = material.getProperty(PropertyKey.ORE);
        Material byproductMaterial = property.getOreByProduct(0, material);
        Material byproductMaterialSecond = property.getOreByProduct(1, material);
        ItemStack byproductStackFirst = ChemicalHelper.get(gem, byproductMaterial);
        ItemStack byproductStackThird = ChemicalHelper.get(dust, property.getOreByProduct(2, material), 1);
        ItemStack dustStack = ChemicalHelper.get(dust, material);
        if (byproductStackFirst.isEmpty()) {
            byproductStackFirst = ChemicalHelper.get(dust, byproductMaterial);
        }
        GTRecipeBuilder builder = ORE_PROCESS_CENTER.recipeBuilder(material.getName() + "ore_process_center_crush_hot_centrifuge_crush")
                .EUt(20)
                .duration(20)
                .inputFluids(Lubricant.getFluid(1))
                .inputItems(rawOre, material)
                .outputItems(dustStack.copyWithCount(property.getOreMultiplier() * 5))
                .outputItems(byproductStackFirst)
                .outputItems(TagPrefix.dust, byproductMaterialSecond)
                .outputItems(byproductStackThird)
                .circuitMeta(4);
        builder.save(provider);
    }
    private static void crushWashCrushWash(@NotNull Consumer<FinishedRecipe> provider, Material material){
        OreProperty property = material.getProperty(PropertyKey.ORE);
        Material byproductMaterial = property.getOreByProduct(0, material);
        Material byproductMaterialSecond = property.getOreByProduct(1, material);
        ItemStack byproductStackFirst = ChemicalHelper.get(gem, byproductMaterial);
        ItemStack byproductStackThird = ChemicalHelper.get(dust, property.getOreByProduct(2, material), 1);
        ItemStack dustStack = ChemicalHelper.get(dust, material);
        ItemStack byproductStackSecond = ChemicalHelper.get(dust, byproductMaterialSecond);
        if (byproductStackFirst.isEmpty()) {
            byproductStackFirst = ChemicalHelper.get(dust, byproductMaterial);
        }
        GTRecipeBuilder builder = ORE_PROCESS_CENTER.recipeBuilder(material.getName() + "ore_process_center_crush_wash_crush_wash")
                .EUt(20)
                .duration(20)
                .inputFluids(Water.getFluid(2))
                .inputItems(rawOre, material)
                .outputItems(dustStack.copyWithCount(property.getOreMultiplier() * 5))
                .outputItems(byproductStackFirst)
                .outputItems(TagPrefix.dust, byproductMaterial)
                .outputItems(byproductStackSecond)
                .circuitMeta(5);
        builder.save(provider);
    }
    private static void crushWashCrushCentrifugeOrElectric(@NotNull Consumer<FinishedRecipe> provider, Material material){
        OreProperty property = material.getProperty(PropertyKey.ORE);
        Material byproductMaterial = property.getOreByProduct(0, material);
        Material byproductMaterialSecond = property.getOreByProduct(1, material);
        ItemStack byproductStackFirst = ChemicalHelper.get(gem, byproductMaterial);
        ItemStack byproductStackThird = ChemicalHelper.get(dust, property.getOreByProduct(2, material), 1);
        ItemStack dustStack = ChemicalHelper.get(dust, material);
        ItemStack byproductStackSecond = ChemicalHelper.get(dust, byproductMaterialSecond);
        if (byproductStackFirst.isEmpty()) {
            byproductStackFirst = ChemicalHelper.get(dust, byproductMaterial);
        }
        GTRecipeBuilder builder = ORE_PROCESS_CENTER.recipeBuilder(material.getName() + "ore_process_center_crush_wash_crush_centrifuge")
                .EUt(20)
                .duration(20)
                .inputFluids(Water.getFluid(1),Lubricant.getFluid(1))
                .inputItems(rawOre, material)
                .outputItems(dustStack.copyWithCount(property.getOreMultiplier() * 5))
                .outputItems(byproductStackFirst)
                .outputItems(TagPrefix.dust, byproductMaterial)
                .outputItems(byproductStackSecond)
                .outputItems(TagPrefix.dust, byproductMaterialSecond)
                .circuitMeta(6);
        builder.save(provider);
        if (property.getSeparatedInto() != null && !property.getSeparatedInto().isEmpty()) {
            List<Material> separatedMaterial = property.getSeparatedInto();
            TagPrefix prefix = (separatedMaterial.get(separatedMaterial.size() - 1).getBlastTemperature() == 0 &&
                    separatedMaterial.get(separatedMaterial.size() - 1).hasProperty(PropertyKey.INGOT)) ? nugget : dust;

            ItemStack separatedStack2 = ChemicalHelper.get(prefix, separatedMaterial.get(separatedMaterial.size() - 1),
                    prefix == nugget ? 2 : 1);

            ORE_PROCESS_CENTER.recipeBuilder(material.getName() + "ore_process_center_crush_wash_crush_electric")
                    .EUt(20)
                    .duration(20)
                    .inputFluids(Water.getFluid(1),Lubricant.getFluid(1))
                    .inputItems(rawOre, material)
                    .outputItems(dustStack.copyWithCount(property.getOreMultiplier() * 5))
                    .outputItems(byproductStackFirst)
                    .outputItems(TagPrefix.dust, byproductMaterial)
                    .outputItems(byproductStackSecond)
                    .outputItems(TagPrefix.dust, separatedMaterial.get(0))
                    .outputItems(separatedStack2)
                    .circuitMeta(8)
                    .save(provider);
        }
    }
}
