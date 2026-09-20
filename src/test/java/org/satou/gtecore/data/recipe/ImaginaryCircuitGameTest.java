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
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

/** Registered board/SoC fabrication, final circuit transactions, progression graph and real structure. */
@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class ImaginaryCircuitGameTest {
    private static Map<Item, Integer> products() {
        return Map.of(GTEItems.IMAGINARY_TREE_CIRCUIT_BOARD.asItem(), 4,
                GTEItems.IMAGINARY_TREE_PRINTED_CIRCUIT_BOARD.asItem(), 1,
                GTEItems.IMAGINARY_TREE_SOC.asItem(), 2);
    }

    private static Map<Item, Integer> finalProducts() {
        return Map.of(GTEItems.IMAGINARY_TREE_PROCESSOR_UHV.asItem(), 4,
                GTEItems.IMAGINARY_TREE_PROCESSOR_ASSEMBLY_UEV.asItem(), 2,
                GTEItems.IMAGINARY_TREE_PROCESSOR_COMPUTER_UIV.asItem(), 1,
                GTEItems.IMAGINARY_TREE_PROCESSOR_MAINFRAME_UXV.asItem(), 1);
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

    @GameTest(template = "empty", batch = "imaginaryCircuit", required = true)
    public static void dedicatedRecipesHaveCompleteAcyclicInputsAndControllerDoesNotSelfLock(GameTestHelper helper) {
        var definition = GTEImaginaryMachines.IMAGINARY_CIRCUIT_FABRICATOR;
        helper.assertTrue(Arrays.asList(definition.getRecipeTypes()).contains(GTERecipeTypes.IMAGINARY_CIRCUIT_FABRICATION),
                "Fabricator must expose its dedicated recipe map");
        helper.assertTrue(!Arrays.asList(GTEMultiMachines2.TREE_OF_IMAGINARY.getRecipeTypes()).contains(GTERecipeTypes.IMAGINARY_CIRCUIT_FABRICATION),
                "The tree must not absorb board and SoC fabrication");
        for (Item product : products().keySet()) {
            registeredOutput(helper, GTERecipeTypes.IMAGINARY_CIRCUIT_FABRICATION, product);
            helper.assertTrue(helper.getLevel().getRecipeManager().getAllRecipesFor(GTERecipeTypes.TREE_OF_IMAGINARY).stream()
                    .noneMatch(tree -> RecipeHelper.getOutputItems(tree).stream().anyMatch(stack -> stack.is(product))),
                    "Fabrication product leaked into tree recipes: " + product);
        }
        Map<Item, List<GTRecipe>> sources = new HashMap<>();
        for (var entry : helper.getLevel().getRecipeManager().getRecipes()) {
            if (!(entry instanceof GTRecipe recipe)) continue;
            for (var ingredient : RecipeHelper.getOutputContents(recipe, ItemRecipeCapability.CAP)) {
                for (var output : ingredient.getItems()) {
                    if (!output.isEmpty()) sources.computeIfAbsent(output.getItem(), ignored -> new ArrayList<>()).add(recipe);
                }
            }
        }
        for (Item product : finalProducts().keySet()) {
            registeredOutput(helper, GTERecipeTypes.TREE_OF_IMAGINARY, product);
            helper.assertTrue(hasInputPath(product, sources, new HashSet<>(), Set.of()),
                    "Final circuit has a missing or cyclic imaginary precursor: " + product);
        }
        Item controller = definition.asStack().getItem();
        var assembly = registeredOutput(helper, GTRecipeTypes.ASSEMBLY_LINE_RECIPES, controller);
        Set<Item> forbidden = new HashSet<>(products().keySet());
        forbidden.addAll(finalProducts().keySet());
        forbidden.add(controller);
        for (var ingredient : RecipeHelper.getInputContents(assembly, ItemRecipeCapability.CAP)) {
            helper.assertTrue(Arrays.stream(ingredient.getItems()).anyMatch(stack -> !stack.isEmpty() &&
                    hasInputPath(stack.getItem(), sources, new HashSet<>(), forbidden)),
                    "Controller has no available pre-fabrication input alternative: " + assembly.id);
        }
        helper.assertTrue(GTEItems.IMAGINARY_TREE_PROCESSOR_UHV.asStack().is(CustomTags.UHV_CIRCUITS) &&
                GTEItems.IMAGINARY_TREE_PROCESSOR_ASSEMBLY_UEV.asStack().is(CustomTags.UEV_CIRCUITS) &&
                GTEItems.IMAGINARY_TREE_PROCESSOR_COMPUTER_UIV.asStack().is(CustomTags.UIV_CIRCUITS) &&
                GTEItems.IMAGINARY_TREE_PROCESSOR_MAINFRAME_UXV.asStack().is(CustomTags.UXV_CIRCUITS),
                "Circuit tags must retain UHV/UEV/UIV/UXV independently of UEV fabrication voltage");
        helper.succeed();
    }

    private static boolean hasInputPath(Item item, Map<Item, List<GTRecipe>> sources,
                                        Set<Item> visiting, Set<Item> forbidden) {
        if (forbidden.contains(item)) return false;
        // Existing non-imaginary ingredients are the boundary of this progression graph.
        if (!BuiltInRegistries.ITEM.getKey(item).getPath().contains("imaginary")) return true;
        if (!visiting.add(item)) return false;
        try {
            return sources.getOrDefault(item, List.of()).stream().anyMatch(recipe ->
                    RecipeHelper.getInputContents(recipe, ItemRecipeCapability.CAP).stream().allMatch(ingredient ->
                            Arrays.stream(ingredient.getItems()).anyMatch(stack -> !stack.isEmpty() &&
                                    hasInputPath(stack.getItem(), sources, visiting, forbidden))));
        } finally {
            visiting.remove(item);
        }
    }

    private record Facility(WorkableElectricMultiblockMachine holder, MEOutputBusPartMachine output,
                            KeyStorage waitingBuffer) {}

    private static Facility facility(GameTestHelper helper, boolean tree) throws Exception {
        var controller = new BlockPos(0, 1, 0);
        var outputPos = new BlockPos(1, 1, 0);
        helper.setBlock(controller, tree ? GTEMultiMachines2.TREE_OF_IMAGINARY.getBlock() :
                GTEImaginaryMachines.IMAGINARY_CIRCUIT_FABRICATOR.getBlock());
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

    @GameTest(template = "empty", batch = "imaginaryCircuit", required = true)
    public static void allThreeProcessesRejectLowerWaterAndExecuteIntoMeWaitingStorage(GameTestHelper helper) throws Exception {
        var facility = facility(helper, false);
        for (var product : products().entrySet()) {
            var recipe = registeredOutput(helper, GTERecipeTypes.IMAGINARY_CIRCUIT_FABRICATION, product.getKey());
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

    @GameTest(template = "empty", batch = "imaginaryCircuit", required = true)
    public static void allFourFinalCircuitsExecuteNativeIoAtUevIntoMeWaitingStorage(GameTestHelper helper) throws Exception {
        var facility = facility(helper, true);
        for (var product : finalProducts().entrySet()) {
            var recipe = registeredOutput(helper, GTERecipeTypes.TREE_OF_IMAGINARY, product.getKey());
            if (RecipeHelper.getInputFluids(recipe).stream().anyMatch(fluid ->
                    fluid.isFluidEqual(GTEMaterials.UltrapureWater.getFluid(1)))) {
                for (var water : List.of(GTEMaterials.DistilledPurifiedWater.getFluid(1), GTEMaterials.UvPurifiedWater.getFluid(1))) {
                    supply(helper, facility, recipe, water);
                    helper.assertTrue(!RecipeHelper.matchRecipe(facility.holder, recipe).isSuccess(),
                            "Lower-tier water matched final assembly: " + recipe.id);
                }
            }
            supply(helper, facility, recipe, null);
            helper.assertTrue(RecipeHelper.matchRecipe(facility.holder, recipe).isSuccess() &&
                    RecipeHelper.matchTickRecipe(facility.holder, recipe).isSuccess(),
                    "Final circuit must accept its registered inputs and UEV energy: " + recipe.id);
            var key = AEItemKey.of(new ItemStack(product.getKey()));
            long before = facility.waitingBuffer.storage.getOrDefault(key, 0L);
            helper.assertTrue(RecipeHelper.handleRecipeIO(facility.holder, recipe, IO.IN, new HashMap<>()).isSuccess(),
                    "Final circuit input transaction failed: " + recipe.id);
            helper.assertTrue(facility.waitingBuffer.storage.getOrDefault(key, 0L) == before,
                    "Input transaction must not emit final circuits");
            helper.assertTrue(RecipeHelper.handleRecipeIO(facility.holder, recipe, IO.OUT, new HashMap<>()).isSuccess(),
                    "Final circuit output transaction failed: " + recipe.id);
            helper.assertTrue(facility.waitingBuffer.storage.getOrDefault(key, 0L) - before == product.getValue(),
                    "Expected exactly one final circuit batch in ME waiting storage, without an AE network: " + recipe.id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "imaginaryCircuit", required = true)
    public static void registeredPreviewFormsAndRequiresCoreGlassAndCoil(GameTestHelper helper) {
        var definition = GTEImaginaryMachines.IMAGINARY_CIRCUIT_FABRICATOR;
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
                    "Preview must include controller, fabrication core, glass and coil");
            var controller = (WorkableElectricMultiblockMachine) MetaMachine.getMachine(level, controllerPos);
            helper.assertTrue(controller.checkPatternWithLock(), "Registered fabrication preview must match its real pattern");
            controller.setFlipped(controller.getMultiblockState().isNeededFlip());
            controller.onStructureFormed();
            helper.assertTrue(controller.isFormed(), "The real fabricator did not form");
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
