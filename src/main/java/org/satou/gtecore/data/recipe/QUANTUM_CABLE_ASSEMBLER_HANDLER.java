package org.satou.gtecore.data.recipe;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import net.minecraft.data.recipes.FinishedRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.wireFine;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Rubber;
import static org.satou.gtecore.common.data.GTERecipeTypes.QUANTUM_CABLE_ASSEMBLER;

public class QUANTUM_CABLE_ASSEMBLER_HANDLER {
    public static void init(@NotNull Consumer<FinishedRecipe> provider){
        for(Material material: GTCEuAPI.materialManager.getRegisteredMaterials()){
            if(material.hasFlag(MaterialFlags.NO_UNIFICATION)) continue;
            boolean flag = false;
            TagPrefix prefix = material.hasProperty(PropertyKey.INGOT) ? ingot :
                    material.hasProperty(PropertyKey.GEM) ? gem : dust;
            int cnt = 0;
            var builder = QUANTUM_CABLE_ASSEMBLER.recipeBuilder("WIREMILL_factory_" + material.getName())
                    .duration(20 * 10)
                    .EUt(28);
            //.inputItems(prefix,material,16);
            if(material.shouldGenerateRecipesFor(TagPrefix.wireGtSingle)){
                flag = true;
                builder = builder.outputItems(wireGtSingle,material,16);
                cnt += 8;
            }
            if(material.shouldGenerateRecipesFor(TagPrefix.wireGtDouble)){
                flag = true;
                builder = builder.outputItems(wireGtDouble,material,8);
                cnt += 8;
            }
            if(material.shouldGenerateRecipesFor(wireGtQuadruple)){
                flag = true;
                builder = builder.outputItems(wireGtQuadruple,material,4);
                cnt += 8;
            }
            if(material.shouldGenerateRecipesFor(wireGtOctal)){
                flag = true;
                builder = builder.outputItems(wireGtOctal,material,2);
                cnt += 8;
            }
            if(material.shouldGenerateRecipesFor(wireGtHex)){
                flag = true;
                builder = builder.outputItems(wireGtHex,material,1);
                cnt += 8;
            }
            if(material.shouldGenerateRecipesFor(wireFine)){
                flag = true;
                builder = builder.outputItems(wireFine,material,64);
                cnt += 8;
            }
            int cnt2 = 0;
            if(material.shouldGenerateRecipesFor(cableGtSingle)){
                flag = true;
                builder = builder.outputItems(cableGtSingle,material,16);
                cnt += 8;
                cnt2 += 16;
            }
            if(material.shouldGenerateRecipesFor(cableGtDouble)){
                flag = true;
                builder = builder.outputItems(cableGtDouble,material,8);
                cnt += 8;
                cnt2 += 8;
            }
            if(material.shouldGenerateRecipesFor(cableGtQuadruple)){
                flag = true;
                builder = builder.outputItems(cableGtQuadruple,material,4);
                cnt += 8;
                cnt2 += 4;
            }
            if(material.shouldGenerateRecipesFor(cableGtOctal)){
                flag = true;
                builder = builder.outputItems(cableGtOctal,material,2);
                cnt += 8;
                cnt2 += 2;
            }
            if(material.shouldGenerateRecipesFor(cableGtHex)){
                flag = true;
                builder = builder.outputItems(cableGtHex,material,1);
                cnt += 8;
                cnt2 += 1;
            }
            if (cnt2 > 0){
                builder = builder.inputFluids(Rubber,288 * cnt2);
            }
            if(flag){
                builder.inputItems(prefix,material,cnt)
                        .save(provider);
            }
        }
    }
}
