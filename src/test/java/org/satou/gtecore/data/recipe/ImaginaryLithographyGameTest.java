package org.satou.gtecore.data.recipe;

import org.satou.gtecore.common.data.GTEBlocks;
import org.satou.gtecore.common.data.GTEMaterials;
import org.satou.gtecore.common.data.GTERecipeTypes;
import org.satou.gtecore.common.data.items.GTEItems;
import org.satou.gtecore.common.data.machines.GTEMultiMachines2;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.gregtechceu.gtceu.integration.ae2.machine.MEOutputBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeEnergyHatchPartMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputBusPartMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputHatchPartMachine;
import com.hepdd.gtmthings.data.CreativeMachines;

import appeng.api.stacks.AEItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.satou.gtecore.common.data.machines.GTEImaginaryMachines;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.ArrayList;
import java.util.Arrays;

/** Registered lithography recipes, live recipe execution, and the actual registered structure. */
@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class ImaginaryLithographyGameTest {
    private static Map<Item, Integer> products() {
        return Map.of(GTEItems.IMAGINARY_TREE_CPU_WAFER.asItem(), 1,
                GTEItems.IMAGINARY_TREE_CIRCUIT_CHIP.asItem(), 16,
                GTEItems.RAW_IMAGINARY_TREE_CHIP.asItem(), 4,
                GTEItems.ENGRAVED_IMAGINARY_TREE_CHIP.asItem(), 1,
                GTEItems.IMAGINARY_TREE_CPU_CHIP.asItem(), 1);
    }

    private static GTRecipe registeredOutput(GameTestHelper helper, GTRecipeType type, Item output) {
        var recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(type).stream()
                .filter(recipe -> RecipeHelper.getOutputItems(recipe).stream().anyMatch(stack -> stack.is(output))).toList();
        helper.assertTrue(recipes.size() == 1, "Expected exactly one registered recipe for " + output);
        var recipe = recipes.getFirst();
        helper.assertTrue(type.getLookup().getLookup().getRecipes(false).anyMatch(indexed -> indexed.id.equals(recipe.id)),
                "Missing ingredient lookup entry: " + recipe.id);
        helper.assertTrue(RecipeHelper.getRecipeEUtTier(recipe) == GTValues.UEV, "Expected UEV: " + recipe.id);
        return recipe;
    }

    @GameTest(template = "empty", batch = "imaginaryLithography", required = true)
    public static void registeredRecipesBelongToDedicatedCenterWithoutControllerSelfLock(GameTestHelper helper) {
        var definition = GTEImaginaryMachines.IMAGINARY_LITHOGRAPHY_CENTER;
        helper.assertTrue(Arrays.asList(definition.getRecipeTypes()).contains(GTERecipeTypes.IMAGINARY_LITHOGRAPHY),
                "Center must expose its dedicated recipe map");
        helper.assertTrue(!Arrays.asList(GTEMultiMachines2.TREE_OF_IMAGINARY.getRecipeTypes()).contains(GTERecipeTypes.IMAGINARY_LITHOGRAPHY),
                "The tree must not absorb lithography processing");
        for (Item product : products().keySet()) {
            var recipe = registeredOutput(helper, GTERecipeTypes.IMAGINARY_LITHOGRAPHY, product);
            if (product == GTEItems.IMAGINARY_TREE_CPU_WAFER.asItem() || product == GTEItems.ENGRAVED_IMAGINARY_TREE_CHIP.asItem()) {
                helper.assertTrue(recipe.getInputContents(ItemRecipeCapability.CAP).stream().anyMatch(content ->
                        content.chance == 0 && ItemRecipeCapability.CAP.of(content.content).test(GTEItems.YIN_YANG_GLASS_LENS.asStack())),
                        "Optical processing must retain its non-consumable lens: " + recipe.id);
            }
            helper.assertTrue(helper.getLevel().getRecipeManager().getAllRecipesFor(GTERecipeTypes.TREE_OF_IMAGINARY).stream()
                    .noneMatch(tree -> RecipeHelper.getOutputItems(tree).stream().anyMatch(stack -> stack.is(product))),
                    "Lithography product leaked into the tree recipe map: " + recipe.id);
        }
        Item controller = definition.asStack().getItem();
        var assembly = registeredOutput(helper, GTRecipeTypes.ASSEMBLY_LINE_RECIPES, controller);
        for (var ingredient : RecipeHelper.getInputContents(assembly, ItemRecipeCapability.CAP)) {
            helper.assertTrue(Arrays.stream(ingredient.getItems()).anyMatch(stack ->
                    !stack.is(controller) && !products().containsKey(stack.getItem())),
                    "Controller requires its own downstream product: " + assembly.id);
        }
        helper.assertTrue(RecipeHelper.getInputItems(assembly).stream().anyMatch(stack -> stack.is(GTEItems.IMAGINARY_TREE_WAFER.asItem())),
                "Center construction must retain the ordinary tree wafer prerequisite");
        helper.succeed();
    }

    private record Facility(WorkableElectricMultiblockMachine holder, MEOutputBusPartMachine output,
                            KeyStorage waitingBuffer) {}

    private static Facility facility(GameTestHelper helper) throws Exception {
        var controller = new BlockPos(0, 1, 0);
        var outputPos = new BlockPos(1, 1, 0);
        helper.setBlock(controller, GTEImaginaryMachines.IMAGINARY_LITHOGRAPHY_CENTER.getBlock());
        helper.setBlock(outputPos, GTAEMachines.ITEM_EXPORT_BUS_ME.getBlock());
        var holder = (WorkableElectricMultiblockMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(controller));
        var output = (MEOutputBusPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(outputPos));
        var field = MEOutputBusPartMachine.class.getDeclaredField("internalBuffer");
        field.setAccessible(true);
        return new Facility(holder, output, (KeyStorage) field.get(output));
    }

    private static void configure(Object part, String method, Class<?> valueType, Object value) throws Exception {
        var inputs = Class.forName("org.satou.gtecore.common.command.CreativeTestInputs");
        var configure = inputs.getDeclaredMethod(method, part.getClass(), valueType);
        configure.setAccessible(true);
        configure.invoke(null, part, value);
    }

    private static void supply(GameTestHelper helper, Facility facility, GTRecipe recipe,
                               FluidStack replacementWater) throws Exception {
        facility.holder.getCapabilitiesProxy().clear();
        facility.holder.getCapabilitiesFlat().clear();
        facility.holder.addHandlerList(RecipeHandlerList.of(IO.OUT, List.of(facility.output.getInventory())));
        int slot = 0;
        for (var input : RecipeHelper.getInputItems(recipe)) {
            var pos = new BlockPos(slot++, 1, 2);
            helper.setBlock(pos, CreativeMachines.CREATIVE_ITEM_INPUT_BUS.getBlock());
            var bus = (CreativeInputBusPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(pos));
            var template = input;
            configure(bus, "item", ItemStack.class, template);
            facility.holder.addHandlerList(RecipeHandlerList.of(IO.IN, List.of(bus.getInventory())));
        }
        slot = 0;
        for (var input : RecipeHelper.getInputFluids(recipe)) {
            var pos = new BlockPos(slot++, 1, 3);
            helper.setBlock(pos, CreativeMachines.CREATIVE_FLUID_INPUT_HATCH.getBlock());
            var hatch = (CreativeInputHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(pos));
            var template = replacementWater != null && input.isFluidEqual(GTEMaterials.UltrapureWater.getFluid(1)) ?
                    replacementWater : input;
            configure(hatch, "fluid", FluidStack.class, template);
            facility.holder.addHandlerList(RecipeHandlerList.of(IO.IN, List.of(hatch.tank)));
        }
        var energyPos = new BlockPos(2, 1, 0);
        helper.setBlock(energyPos, CreativeMachines.CREATIVE_ENERGY_INPUT_HATCH.getBlock());
        var energy = (CreativeEnergyHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(energyPos));
        configure(energy, "energy", int.class, GTValues.UEV);
        facility.holder.addHandlerList(RecipeHandlerList.of(IO.IN, List.of(energy.energyContainer)));
    }

    @GameTest(template = "empty", batch = "imaginaryLithography", required = true)
    public static void allFiveProcessesRejectLowerWaterAndExecuteIntoMeWaitingStorage(GameTestHelper helper) throws Exception {
        var facility = facility(helper);
        for (var product : products().entrySet()) {
            var recipe = registeredOutput(helper, GTERecipeTypes.IMAGINARY_LITHOGRAPHY, product.getKey());
            for (var water : List.of(GTEMaterials.DistilledPurifiedWater.getFluid(1), GTEMaterials.UvPurifiedWater.getFluid(1))) {
                supply(helper, facility, recipe, water);
                helper.assertTrue(!RecipeHelper.matchRecipe(facility.holder, recipe).isSuccess(),
                        "Lower purification tier must be rejected: " + recipe.id);
            }
            supply(helper, facility, recipe, null);
            helper.assertTrue(RecipeHelper.matchRecipe(facility.holder, recipe).isSuccess() &&
                    RecipeHelper.matchTickRecipe(facility.holder, recipe).isSuccess(),
                    "Ultrapure water, creative inputs and UEV energy must match: " + recipe.id);
            var key = AEItemKey.of(new ItemStack(product.getKey()));
            long before = facility.waitingBuffer.storage.getOrDefault(key, 0L);
            var logic = facility.holder.getRecipeLogic();
            logic.setWorkingEnabled(true);
            logic.setupRecipe(recipe);
            helper.assertTrue(logic.isWorking(), "Recipe failed to enter working state: " + recipe.id);
            logic.setWorkingEnabled(false); // Prevent automatic setup of another batch after completion.
            helper.assertTrue(facility.waitingBuffer.storage.getOrDefault(key, 0L) == before,
                    "Starting a recipe must not emit its output");
            var energy = (CreativeEnergyHatchPartMachine) MetaMachine.getMachine(helper.getLevel(),
                    helper.absolutePos(new BlockPos(2, 1, 0)));
            // Drive exactly the registered duration through native tick IO. GTMThings' infinity
            // container itself implements non-depleting EU; no synthetic refill is necessary.
            // suspendAfterFinish prevents rescheduling, but native onRecipeFinish may retain
            // WORKING with completed progress, so SUSPEND is not a completion contract.
            for (int tick = 0; tick < recipe.duration; tick++) {
                logic.serverTick();
                helper.assertTrue(logic.getProgress() == tick + 1,
                        "Recipe did not advance at tick " + tick + ": " + recipe.id +
                                ", progress=" + logic.getProgress() + "/" + logic.getDuration() +
                                ", status=" + logic.getStatus() +
                                ", EU=" + energy.energyContainer.getEnergyStored() +
                                "/" + energy.energyContainer.getEnergyCapacity());
                if (tick + 1 < recipe.duration) {
                    helper.assertTrue(facility.waitingBuffer.storage.getOrDefault(key, 0L) == before,
                            "Recipe emitted output before its registered duration: " + recipe.id);
                }
            }
            helper.assertTrue(facility.waitingBuffer.storage.getOrDefault(key, 0L) - before == product.getValue(),
                    "Expected one batch in the real ME waiting buffer (no connected AE network): " + recipe.id);
            // Stop this manually driven single cycle; pause-after-finish behavior is outside
            // this recipe/IO test. Clear its state before configuring the next registered recipe.
            logic.interruptRecipe();
            logic.resetRecipeLogic();
            logic.setWorkingEnabled(false);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "imaginaryLithography", required = true)
    public static void registeredPreviewFormsAndRequiresCoreGlassAndCoil(GameTestHelper helper) {
        var definition = GTEImaginaryMachines.IMAGINARY_LITHOGRAPHY_CENTER;
        var shape = definition.getMatchingShapes().getFirst().getBlocks();
        var level = helper.getLevel();
        // Above the other empty-template fixtures, with explicit cleanup of every touched block.
        BlockPos anchor = helper.absolutePos(new BlockPos(0, 30, 0));
        var placed = new ArrayList<BlockPos>();
        var critical = new HashMap<net.minecraft.world.level.block.Block, BlockPos>();
        var required = List.of(GTEBlocks.IMAGINARY_CORE_CASING.get(), GTEBlocks.IMAGINARY_GLASS.get(), GTEBlocks.IMAGINARY_COIL.get());
        BlockPos controllerPos = null;
        try {
            for (int x = 0; x < shape.length; x++) for (int y = 0; y < shape[x].length; y++) {
                for (int z = 0; z < shape[x][y].length; z++) {
                    var state = shape[x][y][z].getBlockState();
                    if (state.hasProperty(BlockStateProperties.FACING)) state = state.setValue(BlockStateProperties.FACING, Direction.NORTH);
                    if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
                    var pos = anchor.offset(x, y, z);
                    placed.add(pos);
                    level.setBlock(pos, state, 2);
                    if (state.is(definition.getBlock())) controllerPos = pos;
                    if (required.contains(state.getBlock())) critical.putIfAbsent(state.getBlock(), pos);
                }
            }
            helper.assertTrue(controllerPos != null && critical.size() == required.size(),
                    "Preview must include controller, optical core, glass and coil");
            var controller = (WorkableElectricMultiblockMachine) MetaMachine.getMachine(level, controllerPos);
            helper.assertTrue(controller.checkPatternWithLock(), "Registered lithography preview must match its real pattern");
            controller.setFlipped(controller.getMultiblockState().isNeededFlip());
            controller.onStructureFormed();
            helper.assertTrue(controller.isFormed(), "The real center did not form");
            for (var pos : critical.values()) {
                var original = level.getBlockState(pos);
                controller.onStructureInvalid();
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                helper.assertTrue(!controller.checkPatternWithLock(), "Structure accepted missing critical block " + original);
                level.setBlock(pos, original, 2);
                helper.assertTrue(controller.checkPatternWithLock(), "Restoring critical block must restore structure validity");
                controller.onStructureFormed();
            }
            controller.onStructureInvalid();
        } finally {
            for (var pos : placed) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        helper.succeed();
    }
}
