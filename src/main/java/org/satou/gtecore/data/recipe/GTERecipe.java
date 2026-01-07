package org.satou.gtecore.data.recipe;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.data.recipes.FinishedRecipe;

import org.jetbrains.annotations.NotNull;
import org.satou.gtecore.common.data.GTERecipeTypes;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Americium;
import static org.satou.gtecore.common.data.GTERecipeTypes.*;

public class GTERecipe {

    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
        WIREMILL_FACTORY_HANDLER.init(provider);
        QUANTUM_CABLE_ASSEMBLER_HANDLER.init(provider);
        ORE_RECIPE_CENTER_HANDLER.init(provider);
        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_deuterium_and_tritium_to_helium_plasma")
                .inputFluids(GTMaterials.Deuterium.getFluid(125))
                .inputFluids(GTMaterials.Tritium.getFluid(125))
                .outputFluids(GTMaterials.Helium.getFluid(FluidStorageKeys.PLASMA, 1250))
                .duration(16)
                .EUt(4096)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_carbon_and_helium_3_to_oxygen_plasma")
                .inputFluids(GTMaterials.Carbon.getFluid(16))
                .inputFluids(GTMaterials.Helium3.getFluid(125))
                .outputFluids(GTMaterials.Oxygen.getFluid(FluidStorageKeys.PLASMA, 1250))
                .duration(32)
                .EUt(4096)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_beryllium_and_deuterium_to_nitrogen_plasma")
                .inputFluids(GTMaterials.Beryllium.getFluid(16))
                .inputFluids(GTMaterials.Deuterium.getFluid(375))
                .outputFluids(GTMaterials.Nitrogen.getFluid(FluidStorageKeys.PLASMA, 1250))
                .duration(16)
                .EUt(16384)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_silicon_and_magnesium_to_iron_plasma")
                .inputFluids(GTMaterials.Silicon.getFluid(16))
                .inputFluids(GTMaterials.Magnesium.getFluid(16))
                .outputFluids(GTMaterials.Iron.getFluid(FluidStorageKeys.PLASMA, 1600))
                .duration(32)
                .EUt(VA[IV])
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_potassium_and_fluorine_to_nickel_plasma")
                .inputFluids(GTMaterials.Potassium.getFluid(16))
                .inputFluids(GTMaterials.Fluorine.getFluid(1250))
                .outputFluids(GTMaterials.Nickel.getFluid(FluidStorageKeys.PLASMA, 16))
                .duration(16)
                .EUt(VA[LuV])
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_carbon_and_magnesium_to_argon_plasma")
                .inputFluids(GTMaterials.Carbon.getFluid(16))
                .inputFluids(GTMaterials.Magnesium.getFluid(16))
                .outputFluids(GTMaterials.Argon.getFluid(FluidStorageKeys.PLASMA, 1250))
                .duration(32)
                .EUt(24576)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_neodymium_and_hydrogen_to_europium_plasma")
                .inputFluids(GTMaterials.Neodymium.getFluid(16))
                .inputFluids(GTMaterials.Hydrogen.getFluid(375))
                .outputFluids(GTMaterials.Europium.getFluid(1600))
                .duration(64)
                .EUt(24576)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_lutenium_and_chromium_to_americium_plasma")
                .inputFluids(GTMaterials.Lutetium.getFluid(16))
                .inputFluids(GTMaterials.Chromium.getFluid(16))
                .outputFluids(GTMaterials.Americium.getFluid(1600))
                .duration(64)
                .EUt(49152)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_americium_and_naquadria_to_neutronium_plasma")
                .inputFluids(GTMaterials.Americium.getFluid(128))
                .inputFluids(GTMaterials.Naquadria.getFluid(128))
                .outputFluids(GTMaterials.Neutronium.getFluid(300))
                .duration(200)
                .EUt(98304)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_silver_and_copper_to_osmium_plasma")
                .inputFluids(GTMaterials.Silver.getFluid(16))
                .inputFluids(GTMaterials.Copper.getFluid(16))
                .outputFluids(GTMaterials.Osmium.getFluid(1600))
                .duration(64)
                .EUt(24578)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_mercury_and_magnesium_to_uranium_235_plasma")
                .inputFluids(GTMaterials.Mercury.getFluid(125))
                .inputFluids(GTMaterials.Magnesium.getFluid(16))
                .outputFluids(GTMaterials.Uranium235.getFluid(1600))
                .duration(128)
                .EUt(24576)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_gold_and_aluminium_to_uranium_238_plasma")
                .inputFluids(GTMaterials.Gold.getFluid(16))
                .inputFluids(GTMaterials.Aluminium.getFluid(16))
                .outputFluids(GTMaterials.Uranium238.getFluid(1600))
                .duration(128)
                .EUt(24576)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_xenon_and_zinc_to_plutonium_239_plasma")
                .inputFluids(GTMaterials.Xenon.getFluid(125))
                .inputFluids(GTMaterials.Zinc.getFluid(16))
                .outputFluids(GTMaterials.Plutonium239.getFluid(1600))
                .duration(128)
                .EUt(49152)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_krypton_and_cerium_to_plutonium_241_plasma")
                .inputFluids(GTMaterials.Krypton.getFluid(125))
                .inputFluids(GTMaterials.Cerium.getFluid(16))
                .outputFluids(GTMaterials.Plutonium241.getFluid(1600))
                .duration(128)
                .EUt(49152)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_hydrogen_and_vanadium_to_chromium_plasma")
                .inputFluids(GTMaterials.Hydrogen.getFluid(125))
                .inputFluids(GTMaterials.Vanadium.getFluid(16))
                .outputFluids(GTMaterials.Chromium.getFluid(1600))
                .duration(64)
                .EUt(24576)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_gallium_and_radon_to_duranium_plasma")
                .inputFluids(GTMaterials.Gallium.getFluid(16))
                .inputFluids(GTMaterials.Radon.getFluid(125))
                .outputFluids(GTMaterials.Duranium.getFluid(1600))
                .duration(32)
                .EUt(16384)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_titanium_and_duranium_to_tritanium_plasma")
                .inputFluids(GTMaterials.Titanium.getFluid(48))
                .inputFluids(GTMaterials.Duranium.getFluid(32))
                .outputFluids(GTMaterials.Tritanium.getFluid(1600))
                .duration(16)
                .EUt(VA[LuV])
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_gold_and_mercury_to_radon_plasma")
                .inputFluids(GTMaterials.Gold.getFluid(16))
                .inputFluids(GTMaterials.Mercury.getFluid(16))
                .outputFluids(GTMaterials.Radon.getFluid(1250))
                .duration(64)
                .EUt(VA[LuV])
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_silver_and_lithium_to_indium_plasma")
                .inputFluids(GTMaterials.Silver.getFluid(L))
                .inputFluids(GTMaterials.Lithium.getFluid(L))
                .outputFluids(GTMaterials.Indium.getFluid(L*10))
                .duration(16)
                .EUt(24576)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_enriched_naquadah_and_radon_to_naquadria_plasma")
                .inputFluids(GTMaterials.NaquadahEnriched.getFluid(16))
                .inputFluids(GTMaterials.Radon.getFluid(125))
                .outputFluids(GTMaterials.Naquadria.getFluid(400))
                .duration(64)
                .EUt(49152)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_lantanum_and_silicon_to_lutetium_plasma")
                .inputFluids(GTMaterials.Lanthanum.getFluid(16))
                .inputFluids(GTMaterials.Silicon.getFluid(16))
                .outputFluids(GTMaterials.Lutetium.getFluid(1600))
                .duration(16)
                .EUt(VA[IV])
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_arsenic_and_ruthenium_to_darmstadtium_plasma")
                .inputFluids(GTMaterials.Arsenic.getFluid(32))
                .inputFluids(GTMaterials.Ruthenium.getFluid(16))
                .outputFluids(GTMaterials.Darmstadtium.getFluid(1600))
                .duration(32)
                .EUt(VA[LuV])
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_plutonium_241_and_hydrogen_gas_to_americium_plasma")
                .inputFluids(GTMaterials.Plutonium241.getFluid(144))
                .inputFluids(GTMaterials.Hydrogen.getFluid(FluidStorageKeys.GAS, 2000))
                .outputFluids(GTMaterials.Americium.getFluid(FluidStorageKeys.PLASMA, 1440))
                .duration(64)
                .EUt(98304)
                .save(provider);

        SUPER_FUSION_REACTOR_RECIPE.recipeBuilder("easy_silver_and_helium_3_to_tin_plasma")
                .inputFluids(GTMaterials.Silver.getFluid(144))
                .inputFluids(GTMaterials.Helium3.getFluid(375))
                .outputFluids(GTMaterials.Tin.getFluid(FluidStorageKeys.PLASMA, 1440))
                .duration(16)
                .EUt(49152)
                .save(provider);
        General_Fuel_Generator.recipeBuilder("naphtha")
                .inputFluids(Naphtha.getFluid(1))
                .duration(10)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("sulfuric_light_fuel")
                .inputFluids(SulfuricLightFuel.getFluid(4))
                .duration(5)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("methanol")
                .inputFluids(Methanol.getFluid(4))
                .duration(8)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("ethanol")
                .inputFluids(Ethanol.getFluid(1))
                .duration(6)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("octane")
                .inputFluids(Octane.getFluid(2))
                .duration(5)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("biodiesel")
                .inputFluids(BioDiesel.getFluid(1))
                .duration(8)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("light_fuel")
                .inputFluids(LightFuel.getFluid(1))
                .duration(10)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("diesel")
                .inputFluids(Diesel.getFluid(1))
                .duration(15)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("cetane_diesel")
                .inputFluids(CetaneBoostedDiesel.getFluid(2))
                .duration(45)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("rocket_fuel")
                .inputFluids(RocketFuel.getFluid(16))
                .duration(125)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("gasoline")
                .inputFluids(Gasoline.getFluid(1))
                .duration(50)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("high_octane_gasoline")
                .inputFluids(HighOctaneGasoline.getFluid(1))
                .duration(100)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("toluene")
                .inputFluids(Toluene.getFluid(1))
                .duration(10)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("light_oil")
                .inputFluids(OilLight.getFluid(32))
                .duration(5)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("raw_oil")
                .inputFluids(RawOil.getFluid(64))
                .duration(15)
                .EUt(-V[LV])
                .save(provider);

        // steam generator fuels
        General_Fuel_Generator.recipeBuilder("steam")
                .inputFluids(Steam.getFluid(640))
                .outputFluids(DistilledWater.getFluid(4))
                .duration(10)
                .EUt(-V[LV])
                .save(provider);

        // gas turbine fuels
        General_Fuel_Generator.recipeBuilder("natural_gas")
                .inputFluids(NaturalGas.getFluid(8))
                .duration(5)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("wood_gas")
                .inputFluids(WoodGas.getFluid(8))
                .duration(6)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("sulfuric_gas")
                .inputFluids(SulfuricGas.getFluid(32))
                .duration(25)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("sulfuric_naphtha")
                .inputFluids(SulfuricNaphtha.getFluid(4))
                .duration(5)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("coal_gas")
                .inputFluids(CoalGas.getFluid(1))
                .duration(3)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("methane")
                .inputFluids(Methane.getFluid(2))
                .duration(7)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("ethylene")
                .inputFluids(Ethylene.getFluid(1))
                .duration(4)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("refinery_gas")
                .inputFluids(RefineryGas.getFluid(1))
                .duration(5)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("ethane")
                .inputFluids(Ethane.getFluid(4))
                .duration(21)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("propene")
                .inputFluids(Propene.getFluid(1))
                .duration(6)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("butadiene")
                .inputFluids(Butadiene.getFluid(16))
                .duration(102)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("propane")
                .inputFluids(Propane.getFluid(4))
                .duration(29)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("butene")
                .inputFluids(Butene.getFluid(1))
                .duration(8)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("phenol")
                .inputFluids(Phenol.getFluid(1))
                .duration(9)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("benzene")
                .inputFluids(Benzene.getFluid(1))
                .duration(11)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("butane")
                .inputFluids(Butane.getFluid(4))
                .duration(37)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("lpg")
                .inputFluids(LPG.getFluid(1))
                .duration(10)
                .EUt(-V[LV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("nitrobenzene") // TODO Too OP pls nerf
                .inputFluids(Nitrobenzene.getFluid(1))
                .duration(40)
                .EUt(-V[LV])
                .save(provider);

        // plasma turbine
        General_Fuel_Generator.recipeBuilder("helium")
                .inputFluids(Helium.getFluid(FluidStorageKeys.PLASMA, 1))
                .outputFluids(Helium.getFluid(1))
                .duration(40)
                .EUt(-V[EV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("oxygen")
                .inputFluids(Oxygen.getFluid(FluidStorageKeys.PLASMA, 1))
                .outputFluids(Oxygen.getFluid(1))
                .duration(48)
                .EUt(-V[EV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("nitrogen")
                .inputFluids(Nitrogen.getFluid(FluidStorageKeys.PLASMA, 1))
                .outputFluids(Nitrogen.getFluid(1))
                .duration(64)
                .EUt(-V[EV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("argon")
                .inputFluids(Argon.getFluid(FluidStorageKeys.PLASMA, 1))
                .outputFluids(Argon.getFluid(1))
                .duration(96)
                .EUt(-V[EV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("iron")
                .inputFluids(Iron.getFluid(FluidStorageKeys.PLASMA, 1))
                .outputFluids(Iron.getFluid(1))
                .duration(112)
                .EUt(-V[EV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("tin")
                .inputFluids(Tin.getFluid(FluidStorageKeys.PLASMA, 1))
                .outputFluids(Tin.getFluid(1))
                .duration(128)
                .EUt(-V[EV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("nickel")
                .inputFluids(Nickel.getFluid(FluidStorageKeys.PLASMA, 1))
                .outputFluids(Nickel.getFluid(1))
                .duration(192)
                .EUt(-V[EV])
                .save(provider);

        General_Fuel_Generator.recipeBuilder("americium")
                .inputFluids(Americium.getFluid(FluidStorageKeys.PLASMA, 1))
                .outputFluids(Americium.getFluid(1))
                .duration(320)
                .EUt(-V[EV])
                .save(provider);
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if (material.hasFlag(MaterialFlags.NO_UNIFICATION)) {
                continue;
            }
            if(material.getName() == "borosilicate_glass"){
                continue;
            }
            if (specialJudge(material.getName())) continue;
            if (!material.isSolid()) continue;
            Boolean flag = false;
            Boolean flag2 = false;
            GTRecipeBuilder gtRecipeBuilder = null;

            if (material.hasProperty(PropertyKey.INGOT)) {
                gtRecipeBuilder = Component_Factory.recipeBuilder("_Component_Factory_" + material.getName())
                        .inputItems(ingot, material);
                flag2 = true;
            }
            if (material.hasProperty(PropertyKey.GEM)) {
                gtRecipeBuilder = Component_Factory.recipeBuilder("_Component_Factory_" + material.getName())
                        .inputItems(gem, material);
                flag2 = true;
            }
            if (!flag2) continue;
            gtRecipeBuilder = gtRecipeBuilder.duration(40 * 20)
                    .EUt(GTValues.VA[GTValues.MV]);
            if (material.hasFlag(MaterialFlags.GENERATE_ROD)) {

                gtRecipeBuilder = gtRecipeBuilder.outputItems(rod, material, 8);
                flag = true;
            }

            if (material.hasFlag(MaterialFlags.GENERATE_PLATE)) {

                gtRecipeBuilder = gtRecipeBuilder.outputItems(plate, material, 4);
                flag = true;
            }

            if (material.hasFlag(MaterialFlags.GENERATE_BOLT_SCREW)) {

                gtRecipeBuilder = gtRecipeBuilder.outputItems(bolt, material, 32);
                gtRecipeBuilder = gtRecipeBuilder.outputItems(screw, material, 32);
                flag = true;
            }

            if (material.hasFlag(MaterialFlags.GENERATE_RING)) {

                gtRecipeBuilder = gtRecipeBuilder.outputItems(ring, material, 32);
                flag = true;
            }

            if (material.hasFlag(MaterialFlags.GENERATE_GEAR)) {

                gtRecipeBuilder = gtRecipeBuilder.outputItems(gear, material, 1);
                flag = true;
            }

            if (material.hasFlag(MaterialFlags.GENERATE_SMALL_GEAR)) {

                gtRecipeBuilder = gtRecipeBuilder.outputItems(gearSmall, material, 4);
                flag = true;
            }

            if (material.hasFlag(MaterialFlags.GENERATE_ROTOR)) {

                gtRecipeBuilder = gtRecipeBuilder.outputItems(rotor, material, 1);
                flag = true;
            }
            if (material.hasFlag(MaterialFlags.GENERATE_SPRING)) {

                gtRecipeBuilder = gtRecipeBuilder.outputItems(spring, material, 4);
                flag = true;
            }
            if (material.hasFlag(MaterialFlags.GENERATE_SPRING_SMALL)) {

                gtRecipeBuilder = gtRecipeBuilder.outputItems(springSmall, material, 16);
                flag = true;
            }
            if (material.hasFlag(MaterialFlags.GENERATE_FRAME)) {

                gtRecipeBuilder = gtRecipeBuilder.outputItems(frameGt, material, 2);
                flag = true;
            }
            if (material.hasFlag(MaterialFlags.GENERATE_FOIL)) {

                gtRecipeBuilder = gtRecipeBuilder.outputItems(foil, material, 16);
                flag = true;
            }

            if (flag) {
                gtRecipeBuilder.save(provider);
            }

        }
        for (Material material : GTCEuAPI.materialManager.getRegisteredMaterials()) {
            if(material.hasFlag(MaterialFlags.NO_UNIFICATION)){continue;}
            if(material.hasProperty(PropertyKey.ORE)){
                GTERecipeTypes.SteamOp_Recipe.recipeBuilder("easy_steam_op_"+material.getName())
                        .EUt(8)
                        .duration(200)
                        .inputItems(rawOre, material)
                        .outputItems(dust,material,3)
                        .circuitMeta(1)
                        .save(provider);
            }
        }
    }

    private static boolean specialJudge(String name) {
        if (name == "infinite") return true;
        if (name == "raw_rubber") return true;
        return false;
    }
}
