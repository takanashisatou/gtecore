package org.satou.gtecore.data.recipe;

import org.satou.gtecore.common.data.GTEMaterials;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationMachines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.AssemblyLineMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;

/** Registered T2 controller recipe and real assembly-line IO, independently of line geometry. */
@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class WaterPurificationAssemblyGameTest {

    @GameTest(template = "empty", batch = "waterPurificationAssembly", required = true)
    public static void t2ControllerCanConsumeBothFluidsAndFinish(GameTestHelper helper) {
        var controller = GTEWaterPurificationMachines.T2_UV_OXIDATION_PURIFICATION_UNIT.asStack();
        var recipes = helper.getLevel().getRecipeManager().getRecipes().stream()
                .filter(GTRecipe.class::isInstance).map(GTRecipe.class::cast)
                .filter(recipe -> RecipeHelper.getOutputItems(recipe).stream()
                        .anyMatch(output -> output.is(controller.getItem()))).toList();
        helper.assertTrue(recipes.size() == 1, "Expected exactly one T2 controller production recipe");
        var recipe = recipes.getFirst();
        var fluids = RecipeHelper.getInputFluids(recipe);
        helper.assertTrue(fluids.size() <= recipe.recipeType.getMaxInputs(FluidRecipeCapability.CAP),
                "T2 controller needs " + fluids.size() + " fluids but " + recipe.recipeType + " only supports " +
                        recipe.recipeType.getMaxInputs(FluidRecipeCapability.CAP));
        helper.assertTrue(recipe.recipeType == GTRecipeTypes.ASSEMBLY_LINE_RECIPES,
                "The two-fluid T2 controller must be manufactured in an assembly line");
        helper.assertTrue(fluids.size() == 2 && fluids.get(0).isFluidEqual(GTMaterials.HydrogenPeroxide.getFluid(4000)) &&
                fluids.get(0).getAmount() == 4000 &&
                fluids.get(1).isFluidEqual(GTEMaterials.DistilledPurifiedWater.getFluid(8000)) &&
                fluids.get(1).getAmount() == 8000 && RecipeHelper.getRecipeEUtTier(recipe) == GTValues.UEV,
                "T2 controller must retain both original fluids, quantities and UEV progression");

        helper.setBlock(new BlockPos(0, 2, 0), GTMultiMachines.ASSEMBLY_LINE.getBlock());
        var line = (AssemblyLineMachine) MetaMachine.getMachine(helper.getLevel(),
                helper.absolutePos(new BlockPos(0, 2, 0)));
        var parts = new HashSet<IMultiPart>();
        var buses = new ArrayList<ItemBusPartMachine>();
        var items = RecipeHelper.getInputItems(recipe);
        for (int i = 0; i < items.size(); i++) {
            var pos = new BlockPos(i, 2, 1);
            helper.setBlock(pos, GTMachines.ITEM_IMPORT_BUS[GTValues.ULV].getBlock());
            var bus = (ItemBusPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(pos));
            buses.add(bus);
            parts.add(bus);
        }
        var hatches = new ArrayList<FluidHatchPartMachine>();
        for (int i = 0; i < fluids.size(); i++) {
            var pos = new BlockPos(i, 2, 2);
            helper.setBlock(pos, GTMachines.FLUID_IMPORT_HATCH[GTValues.HV].getBlock());
            var hatch = (FluidHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(pos));
            hatches.add(hatch);
            parts.add(hatch);
        }
        helper.setBlock(new BlockPos(2, 2, 2), GTMachines.ENERGY_INPUT_HATCH[GTValues.UEV].getBlock());
        var energy = (EnergyHatchPartMachine) MetaMachine.getMachine(helper.getLevel(),
                helper.absolutePos(new BlockPos(2, 2, 2)));
        parts.add(energy);
        helper.setBlock(new BlockPos(3, 2, 2), GTMachines.ITEM_EXPORT_BUS[GTValues.ULV].getBlock());
        var output = (ItemBusPartMachine) MetaMachine.getMachine(helper.getLevel(),
                helper.absolutePos(new BlockPos(3, 2, 2)));
        parts.add(output);

        // Use formation to initialize native voltage, handlers and their order. Each
        // ingredient has its own ordinary bus/hatch, as in ordered assembly lines.
        line.getMultiblockState().getMatchContext().set("parts", parts);
        line.getMultiblockState().error = null;
        line.onStructureFormed();
        buses.sort(AssemblyLineMachine.partSorter(line));
        hatches.sort(AssemblyLineMachine.partSorter(line));
        for (int i = 0; i < items.size(); i++) buses.get(i).getInventory().setStackInSlot(0, items.get(i).copy());
        for (int i = 0; i < fluids.size(); i++) hatches.get(i).tank.setFluidInTank(0, fluids.get(i).copy());
        energy.energyContainer.setEnergyStored(energy.energyContainer.getEnergyCapacity());
        var logic = line.getRecipeLogic();
        logic.findAndHandleRecipe();
        helper.assertTrue(logic.isWorking(), "Assembly line could not discover and start the registered T2 recipe");
        helper.assertTrue(buses.stream().allMatch(bus -> bus.getInventory().getStackInSlot(0).isEmpty()) &&
                hatches.stream().allMatch(hatch -> hatch.tank.getFluidInTank(0).isEmpty()),
                "Starting T2 controller assembly must consume every item and both fluid inputs");
        int duration = logic.getDuration();
        for (int tick = 0; tick < duration; tick++) {
            energy.energyContainer.setEnergyStored(energy.energyContainer.getEnergyCapacity());
            logic.handleRecipeWorking();
        }
        helper.assertTrue(logic.getProgress() == duration, "T2 controller assembly did not finish its powered ticks");
        logic.onRecipeFinish();
        var result = output.getInventory().getStackInSlot(0);
        helper.assertTrue(result.is(controller.getItem()) && result.getCount() == 1,
                "Completed assembly must output exactly one T2 purification controller");
        helper.succeed();
    }
}
