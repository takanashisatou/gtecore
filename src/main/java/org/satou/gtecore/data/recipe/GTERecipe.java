package org.satou.gtecore.data.recipe;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import net.minecraft.data.recipes.FinishedRecipe;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.Americium;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.*;
import static org.satou.gtecore.common.data.GTERecipeTypes.Component_Factory;
import static org.satou.gtecore.common.data.GTERecipeTypes.General_Fuel_Generator;

public class GTERecipe {

    public static void init(@NotNull Consumer<FinishedRecipe> provider) {
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
    }

    private static boolean specialJudge(String name) {
        if (name == "infinite") return true;
        if (name == "raw_rubber") return true;
        return false;
    }
}
