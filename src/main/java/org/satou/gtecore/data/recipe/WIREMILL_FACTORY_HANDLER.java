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
import static org.satou.gtecore.common.data.GTERecipeTypes.WIREMILL_FACTORY;

public class WIREMILL_FACTORY_HANDLER {
    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        for(Material material: GTCEuAPI.materialManager.getRegisteredMaterials()){
            if(material.hasFlag(MaterialFlags.NO_UNIFICATION)) continue;
            boolean flag = false;
            TagPrefix prefix = material.hasProperty(PropertyKey.INGOT) ? ingot :
                    material.hasProperty(PropertyKey.GEM) ? gem : dust;
            int cnt = 0;
            var builder = WIREMILL_FACTORY.recipeBuilder("WIREMILL_factory_" + material.getName())
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
            if(flag){
                builder.inputItems(prefix,material,cnt).save(provider);
            }
        }
    }
}
