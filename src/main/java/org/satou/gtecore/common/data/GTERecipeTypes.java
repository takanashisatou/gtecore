package org.satou.gtecore.common.data;

import org.satou.gtecore.GTECore;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeType;

import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.MULTIBLOCK;
import static com.lowdragmc.lowdraglib.gui.texture.ProgressTexture.FillDirection.LEFT_TO_RIGHT;

public class GTERecipeTypes {



    public static GTRecipeType register(String name, String group, RecipeType<?>... proxyRecipes) {
        var recipeType = new GTRecipeType(GTECore.id(name), group, proxyRecipes);
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, recipeType.registryName, recipeType);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, recipeType.registryName, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(recipeType.registryName, recipeType);
        return recipeType;
    }

    public final static GTRecipeType EASY_BOX = register("easy_box", MULTIBLOCK)
            .setMaxIOSize(1, 80, 0, 0).setEUIO(IO.IN)
            // .setSlotOverlay(false, false,GuiTextures.SLOT)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT)
            .setSteamProgressBar(GuiTextures.PROGRESS_BAR_ARROW_STEAM, LEFT_TO_RIGHT);
    public final static GTRecipeType Component_Factory = register("component_factory", MULTIBLOCK)
            .setMaxIOSize(2, 16, 0, 0).setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
    public final static GTRecipeType Circuit_Factory = register("circuit_factory", MULTIBLOCK)
            .setMaxIOSize(16, 6, 0, 0).setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
    public final static GTRecipeType Ecological_Simulator = register("ecological_simulator", MULTIBLOCK)
            .setMaxIOSize(2, 16, 0, 0)
            .setEUIO(IO.IN);
    public final static GTRecipeType General_Fuel_Generator = register("general_fuel_generator", MULTIBLOCK)
            .setMaxIOSize(0, 0, 1, 1)
            .setEUIO(IO.OUT);
    public final static GTRecipeType Desulfurization_Recipe = register("desulfurization_recipe", MULTIBLOCK)
            .setMaxIOSize(1, 1, 8, 8)
            .setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
    public final static GTRecipeType SteamOp_Recipe = register("steam_op_recipe", MULTIBLOCK)
            .setMaxIOSize(2, 16, 0, 0)
            .setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
    public final static GTRecipeType Chemistry_Terminator_Recipe = register("chemistry_terminator_recipe", MULTIBLOCK)
            .setMaxIOSize(9, 9, 9, 9)
            .setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
    public final static GTRecipeType SUPER_FUSION_REACTOR_RECIPE = register("super_fusion_reactor_recipe", MULTIBLOCK)
            .setMaxIOSize(9, 9, 9, 9)
            .setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
    public final static GTRecipeType SUPER_STRING_MIXING = register("super_string_mixing", MULTIBLOCK)
            .setMaxIOSize(9, 9, 9, 9)
            .setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
    public final static GTRecipeType STRING_OF_CREATION = register("string_of_creation", MULTIBLOCK)
            .setMaxIOSize(9,1,6,0)
            .setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
    public final static GTRecipeType SUPER_STRING_OSCILLATOR_ARRAY = register("super_string_oscillator_array",MULTIBLOCK)
            .setMaxIOSize(9,9,9,9)
            .setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW,LEFT_TO_RIGHT);
    public final static GTRecipeType CHORD_OF_ALL_THINGS = register("chord_of_all_things", MULTIBLOCK)
            .setMaxIOSize(6,12,6,12)
            .setEUIO(IO.IN);
    public final static GTRecipeType EASY_FLUID = register("easy_fluid", MULTIBLOCK)
            .setMaxIOSize(1,0,0,12)
            .setEUIO(IO.IN)
            .setSteamProgressBar(GuiTextures.PROGRESS_BAR_ARROW_STEAM, LEFT_TO_RIGHT);
    public static final GTRecipeType MOLECULAR_SEPARATORS = register("molecular_separators", MULTIBLOCK)
            .setMaxIOSize(9,9,3,3)
            .setEUIO(IO.IN);
    public static final GTRecipeType INTEGRATED_PETROCHEMICAL_PLANT = register("integrated_petrochemical_plant",MULTIBLOCK)
            .setMaxIOSize(9,9,9,12)
            .setEUIO(IO.IN)
            .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, LEFT_TO_RIGHT);
    public static final GTRecipeType ORE_PROCESS_CENTER = register("ore_process_center", MULTIBLOCK)
            .setMaxIOSize(9,9,3,3)
            .setEUIO(IO.IN);
    public static final GTRecipeType WIREMILL_FACTORY = register("wiremill_factory", MULTIBLOCK)
            .setMaxIOSize(1,9,0,0)
            .setEUIO(IO.IN);
    public static final GTRecipeType QUANTUM_CABLE_ASSEMBLER = register("quantum_cable_assembler",MULTIBLOCK)
            .setMaxIOSize(1,16,1,0)
            .setEUIO(IO.IN);
    public static final GTRecipeType CRYSTAL_CENTER = register("crystal_center",MULTIBLOCK)
            .setMaxIOSize(9,9,9,9)
            .setEUIO(IO.IN);
    public static final GTRecipeType STARBLADE_ETCHING = register("starblade_etching",MULTIBLOCK)
            .setMaxIOSize(9,9,9,9)
            .setEUIO(IO.IN);
    public static final GTRecipeType ANTIMATTER_TRANSFORMATION = register("antimatter_transformation",MULTIBLOCK)
            .setMaxIOSize(9,9,9,9)
            .setEUIO(IO.IN);
    public static final GTRecipeType YIN_YANG_EIGHT_TRIGMAS_BLAST = register("yin_yang_eight_trigmas_blast",MULTIBLOCK)
            .setMaxIOSize(9,9,9,9)
            .setEUIO(IO.IN);
    public static final GTRecipeType TAICHI_FIVE_ELEMENTS_SEPARATING = register("taichi_five_elements_separating",MULTIBLOCK)
            .setMaxIOSize(9,9,9,9)
            .setEUIO(IO.IN);
    public static final GTRecipeType KUN_GEN_STAR_HUB = register("kun_gen_star_hub",MULTIBLOCK)
            .setMaxIOSize(9,9,9,9)
            .setEUIO(IO.IN);
    public static final GTRecipeType QIAN_QIONG_ENGINE = register("qian_qiong_engine",MULTIBLOCK)
            .setMaxIOSize(0,0,5,5)
            .setEUIO(IO.OUT);
    public static final GTRecipeType RED_SUN_STAR_CORE = register("red_sun_tao_core",MULTIBLOCK)
            .setMaxIOSize(9,9,9,9)
            .setEUIO(IO.IN);
    public static final GTRecipeType ASHING_STAR_FUSION_ARRAY = register("ashing_star_fusion_array",MULTIBLOCK)
            .setMaxIOSize(9,9,9,9)
            .setEUIO(IO.IN);
    public static final GTRecipeType RARE_EARTH_PROCESSING = register("rare_earth_processing",MULTIBLOCK)
            .setMaxIOSize(1,9,0,0)
            .setEUIO(IO.IN);
    public static void init() {//Lazy init, one class must be used so that the class static field will be init)}
    }
}
