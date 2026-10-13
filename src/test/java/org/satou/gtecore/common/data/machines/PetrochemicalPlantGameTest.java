package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;

import dev.arbor.gtnn.common.machine.multiblock.ChemicalPlantMachine;
import dev.arbor.gtnn.common.recipe.PlantCasingCondition;
import dev.arbor.gtnn.data.GTNNMachines;
import dev.arbor.gtnn.data.GTNNRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.satou.gtecore.common.data.GTERecipeTypes;

import java.util.ArrayList;
import java.util.HashSet;

@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class PetrochemicalPlantGameTest {

    @GameTest(template = "empty", batch = "petrochemicalStructure", required = true, timeoutTicks = 400)
    public static void realStructureAcceptsTwoOptionalCatalystHatches(GameTestHelper helper) {
        var origin = helper.absolutePos(BlockPos.ZERO);
        // The empty template only clears its own small footprint. CI worlds can
        // have terrain above the relative test height, and autobuild preserves it.
        // Reserve an empty site that fits the whole structure below the build limit.
        var pos = new BlockPos(origin.getX(), helper.getLevel().getMaxBuildHeight() - 70, origin.getZ());
        // Model the obstructing terrain that exposed the CI failure, even in a flat world.
        for (var direction : Direction.values()) {
            helper.getLevel().setBlock(pos.relative(direction), Blocks.STONE.defaultBlockState(), 2);
        }
        for (var clear : BlockPos.betweenClosed(pos.offset(-44, -44, -44), pos.offset(44, 44, 44))) {
            if (!helper.getLevel().getBlockState(clear).isAir()) {
                helper.getLevel().setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        helper.getLevel().setBlockAndUpdate(pos,
                GTEMultiMachine.INTEGRATED_PETROCHEMICAL_PLANT.getBlock().defaultBlockState());
        var machine = (WorkableElectricMultiblockMachine) MetaMachine.getMachine(helper.getLevel(), pos);
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var previous = player.gameMode.getGameModeForPlayer();
        player.setGameMode(GameType.CREATIVE);
        try {
            machine.getPattern().autoBuild(player, new MultiblockState(helper.getLevel(), pos));
        } finally {
            player.setGameMode(previous);
        }
        // Creative autobuild also selects optional, limited parts. Start from zero
        // catalyst hatches before independently checking the zero/two/three limits.
        for (var candidate : BlockPos.betweenClosed(pos.offset(-44, -41, -44), pos.offset(44, 41, 44))) {
            if (helper.getLevel().getBlockState(candidate).is(GTNNMachines.INSTANCE.getCATALYST_HATCH().getBlock())) {
                helper.getLevel().setBlockAndUpdate(candidate,
                        GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get().defaultBlockState());
            }
        }
        helper.assertTrue(machine.checkPatternWithLock(),
                "Integrated plant cannot form without catalyst hatches: " + patternError(machine));
        var candidates = new ArrayList<BlockPos>();
        for (var candidate : BlockPos.betweenClosed(pos.offset(-44, -41, -44), pos.offset(44, 41, 44))) {
            if (helper.getLevel().getBlockState(candidate).is(GCYMBlocks.CASING_HIGH_TEMPERATURE_SMELTING.get())) {
                candidates.add(candidate.immutable());
                if (candidates.size() == 3) break;
            }
        }
        helper.assertTrue(candidates.size() == 3, "Could not find replaceable integrated plant casings");
        for (int i = 0; i < 2; i++) {
            helper.getLevel().setBlockAndUpdate(candidates.get(i),
                    GTNNMachines.INSTANCE.getCATALYST_HATCH().getBlock().defaultBlockState());
        }
        helper.assertTrue(machine.checkPatternWithLock(),
                "Two catalyst hatches are rejected by the real pattern: " + patternError(machine));
        helper.getLevel().setBlockAndUpdate(candidates.get(2),
                GTNNMachines.INSTANCE.getCATALYST_HATCH().getBlock().defaultBlockState());
        helper.assertTrue(!machine.checkPatternWithLock(), "More than two catalyst hatches must remain disallowed");
        helper.succeed();
    }

    private static String patternError(WorkableElectricMultiblockMachine machine) {
        var error = machine.getMultiblockState().error;
        return error == null ? "none" : error.getErrorInfo().getString() + " at " + error.getPos() +
                " (block: " + error.getWorld().getBlockState(error.getPos()) + ")";
    }

    private static WorkableElectricMultiblockMachine plant(GameTestHelper helper) {
        var pos = new BlockPos(0, 2, 0);
        helper.setBlock(pos, GTEMultiMachine.INTEGRATED_PETROCHEMICAL_PLANT.getBlock());
        return (WorkableElectricMultiblockMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(pos));
    }

    private static GTRecipe hydrazine(GameTestHelper helper) {
        return helper.getLevel().getRecipeManager()
                .getAllRecipesFor(GTNNRecipeTypes.INSTANCE.getCHEMICAL_PLANT_RECIPES()).stream()
                .filter(recipe -> recipe.id.getPath().equals("chemical_plant/hydrazine"))
                .findFirst().orElseThrow(() -> new AssertionError("Registered hydrazine recipe is missing"));
    }

    @GameTest(template = "empty", batch = "petrochemicalRecipes", required = true)
    public static void integratedPlantAcceptsAllSixCasingTiersWithoutChangingOriginalPlant(GameTestHelper helper) {
        var integrated = plant(helper);
        helper.setBlock(new BlockPos(1, 2, 0), GTNNMachines.INSTANCE.getCHEMICAL_PLANT().getBlock());
        var original = (ChemicalPlantMachine) MetaMachine.getMachine(helper.getLevel(),
                helper.absolutePos(new BlockPos(1, 2, 0)));
        original.setCasingTier(PlantCasingCondition.STEEL);
        helper.setBlock(new BlockPos(2, 2, 0), GTEMultiMachine.Desulfurization_Machine.getBlock());
        var unrelated = (WorkableElectricMultiblockMachine) MetaMachine.getMachine(helper.getLevel(),
                helper.absolutePos(new BlockPos(2, 2, 0)));
        var recipe = hydrazine(helper);
        for (int tier = PlantCasingCondition.BRONZE; tier <= PlantCasingCondition.TUNGSTEN_STEEL; tier++) {
            var condition = new PlantCasingCondition(tier);
            helper.assertTrue(condition.check(recipe, integrated.getRecipeLogic()),
                    "Integrated plant rejects chemical plant casing tier " + tier);
            helper.assertTrue(condition.check(recipe, original.getRecipeLogic()) == (tier <= PlantCasingCondition.STEEL),
                    "Original chemical plant no longer enforces its casing tier");
            helper.assertTrue(!condition.check(recipe, unrelated.getRecipeLogic()),
                    "Casing compatibility leaked into an unrelated machine");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "petrochemicalRecipes", required = true)
    public static void reverseAndOtherRecipeConditionsRemainEnforced(GameTestHelper helper) {
        var integrated = plant(helper);
        var recipe = hydrazine(helper);
        helper.assertTrue(!new PlantCasingCondition(true, PlantCasingCondition.STEEL)
                        .check(recipe, integrated.getRecipeLogic()),
                "Reverse casing condition was bypassed");
        var other = GTNNRecipeTypes.INSTANCE.getCHEMICAL_PLANT_RECIPES().recipeBuilder("petrochemical_dimension_test")
                .addCondition(new PlantCasingCondition(PlantCasingCondition.STEEL))
                .dimension(new ResourceLocation("minecraft", "the_nether"))
                .duration(20).EUt(120).buildRawRecipe();
        helper.assertTrue(!RecipeHelper.checkConditions(other, integrated.getRecipeLogic()).isSuccess(),
                "Accepting the casing condition must not bypass the dimension requirement");
        helper.assertTrue(recipe.conditions.stream().anyMatch(PlantCasingCondition.class::isInstance),
                "The registered recipe lost its original casing condition");
        var ownRecipe = GTERecipeTypes.INTEGRATED_PETROCHEMICAL_PLANT.recipeBuilder("unrelated_casing_test")
                .duration(20).EUt(120).buildRawRecipe();
        helper.assertTrue(!new PlantCasingCondition(PlantCasingCondition.STEEL)
                        .check(ownRecipe, integrated.getRecipeLogic()),
                "Compatibility must be confined to chemical plant recipes");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "petrochemicalRecipes", required = true)
    public static void registeredHydrazineRecipeConsumesInputsAndFinishes(GameTestHelper helper) {
        var integrated = plant(helper);
        var recipe = hydrazine(helper);
        var parts = new HashSet<IMultiPart>();
        helper.setBlock(new BlockPos(1, 2, 0), GTMachines.ENERGY_INPUT_HATCH[GTValues.HV].getBlock());
        var energy = (EnergyHatchPartMachine) MetaMachine.getMachine(helper.getLevel(),
                helper.absolutePos(new BlockPos(1, 2, 0)));
        energy.energyContainer.setEnergyStored(energy.energyContainer.getEnergyCapacity());
        parts.add(energy);
        helper.setBlock(new BlockPos(2, 2, 0), GTMachines.ITEM_IMPORT_BUS[GTValues.HV].getBlock());
        var items = (ItemBusPartMachine) MetaMachine.getMachine(helper.getLevel(),
                helper.absolutePos(new BlockPos(2, 2, 0)));
        var itemInputs = RecipeHelper.getInputItems(recipe);
        for (int i = 0; i < itemInputs.size(); i++) items.getInventory().setStackInSlot(i, itemInputs.get(i).copy());
        parts.add(items);
        var fluidInputs = RecipeHelper.getInputFluids(recipe);
        for (int i = 0; i < fluidInputs.size(); i++) {
            var pos = new BlockPos(3 + i, 2, 0);
            helper.setBlock(pos, GTMachines.FLUID_IMPORT_HATCH[GTValues.HV].getBlock());
            var input = (FluidHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(pos));
            input.tank.setFluidInTank(0, fluidInputs.get(i).copy());
            parts.add(input);
        }
        var outputPos = new BlockPos(3 + fluidInputs.size(), 2, 0);
        helper.setBlock(outputPos, GTMachines.FLUID_EXPORT_HATCH[GTValues.HV].getBlock());
        var output = (FluidHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(outputPos));
        parts.add(output);
        // Attach actual hatches through the formation lifecycle so voltage tier,
        // handlers and subscriptions are initialized as they are in a built plant.
        integrated.getMultiblockState().getMatchContext().set("parts", parts);
        integrated.getMultiblockState().error = null;
        integrated.onStructureFormed();
        helper.assertTrue(RecipeHelper.matchContents(integrated, recipe).isSuccess(),
                "Hydrazine ingredients or power do not match before condition testing");
        helper.assertTrue(integrated.getRecipeLogic().checkMatchedRecipeAvailable(recipe),
                "Recipe logic refused the registered hydrazine recipe with all ingredients and power present");
        helper.assertTrue(!RecipeHelper.matchRecipe(integrated, recipe).isSuccess(),
                "Starting hydrazine did not consume its fluid inputs");
        integrated.getRecipeLogic().handleRecipeWorking();
        helper.assertTrue(integrated.getRecipeLogic().getProgress() == 1,
                "The running recipe could not pass its per-tick condition and energy checks");
        integrated.getRecipeLogic().onRecipeFinish();
        var expected = RecipeHelper.getOutputFluids(recipe).getFirst();
        helper.assertTrue(output.tank.getFluidInTank(0).isFluidEqual(expected) &&
                        output.tank.getFluidInTank(0).getAmount() == expected.getAmount(),
                "Finishing hydrazine did not produce its registered fluid output");
        helper.succeed();
    }
}
