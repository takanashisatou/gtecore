package org.satou.gtecore;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static org.satou.gtecore.GTERecipeTypes.Component_Factory;

public class GTERecipe {

    public static void init(@NotNull Consumer<FinishedRecipe> provider) {

        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (material.hasFlag(MaterialFlags.NO_UNIFICATION)) {
                continue;
            }
            if(!material.isSolid()) continue;
            Boolean flag = false;
            Boolean flag2 = false;
            GTRecipeBuilder gtRecipeBuilder = null;

            if(material.hasProperty(PropertyKey.INGOT)) {
                gtRecipeBuilder = Component_Factory.recipeBuilder("_Component_Factory_" + material.getName())
                        .inputItems(ingot, material);
                flag2 = true;
            }
            if(material.hasProperty(PropertyKey.GEM)) {
                gtRecipeBuilder = Component_Factory.recipeBuilder("_Component_Factory_" + material.getName())
                        .inputItems(gem, material);
                flag2 = true;
            }
            if(!flag2) continue;
                gtRecipeBuilder = gtRecipeBuilder.duration(40 * 20)
                        .EUt(GTValues.VA[GTValues.MV]);
                if(material.hasFlag(MaterialFlags.GENERATE_ROD)) {
                    gtRecipeBuilder = gtRecipeBuilder.outputItems(rod,material,8);
                    flag = true;
                }

                if(material.hasFlag(MaterialFlags.GENERATE_PLATE)) {
                    gtRecipeBuilder = gtRecipeBuilder.outputItems(plate,material,4);
                    flag = true;
                }

                if(material.hasFlag(MaterialFlags.GENERATE_BOLT_SCREW)) {

                    gtRecipeBuilder = gtRecipeBuilder.outputItems(bolt,material,32);
                    gtRecipeBuilder = gtRecipeBuilder.outputItems(screw,material,32);
                    flag = true;
                }

                if(material.hasFlag(MaterialFlags.GENERATE_RING)) {
                    gtRecipeBuilder = gtRecipeBuilder.outputItems(ring,material,32);
                    flag = true;
                }

                if(material.hasFlag(MaterialFlags.GENERATE_GEAR)) {
                    gtRecipeBuilder = gtRecipeBuilder.outputItems(gear,material,1);
                    flag = true;
                }

                if(!material.hasFlag(MaterialFlags.GENERATE_SMALL_GEAR)) {
                    gtRecipeBuilder = gtRecipeBuilder.outputItems(gearSmall,material,4);
                    flag = true;
                }

                if(material.hasFlag(MaterialFlags.GENERATE_ROTOR)){
                    gtRecipeBuilder = gtRecipeBuilder.outputItems(rotor,material,1);
                    flag = true;
                }
                if(flag) {
                    gtRecipeBuilder.save(provider);
                }

        }
    }
}
