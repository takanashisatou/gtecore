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
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.ae2.machine.MEOutputBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;
import com.gregtechceu.gtceu.utils.ResearchManager;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeEnergyHatchPartMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputBusPartMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputHatchPartMachine;
import com.hepdd.gtmthings.data.CreativeMachines;

import appeng.api.stacks.AEItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Registered recipe integration: real creative parts and ME waiting storage, without tree geometry or timing. */
@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class ImaginaryFoundationGameTest {

    private static Map<Item, GTRecipeType> foundationTypes() {
        return Map.ofEntries(
                Map.entry(GTEBlocks.IMAGINARY_CASING.asItem(), GTRecipeTypes.ASSEMBLY_LINE_RECIPES),
                Map.entry(GTEBlocks.IMAGINARY_BRANCH_CASING.asItem(), GTRecipeTypes.ASSEMBLY_LINE_RECIPES),
                Map.entry(GTEBlocks.IMAGINARY_CONTAINMENT_CASING.asItem(), GTRecipeTypes.ASSEMBLY_LINE_RECIPES),
                Map.entry(GTEBlocks.IMAGINARY_ENERGY_CONDUIT.asItem(), GTRecipeTypes.ASSEMBLY_LINE_RECIPES),
                Map.entry(GTEBlocks.IMAGINARY_CORE_CASING.asItem(), GTERecipeTypes.KUN_GEN_STAR_HUB),
                Map.entry(GTEBlocks.IMAGINARY_COIL.asItem(), GTERecipeTypes.KUN_GEN_STAR_HUB),
                Map.entry(GTEBlocks.IMAGINARY_GLASS.asItem(), GTERecipeTypes.RED_SUN_STAR_CORE),
                Map.entry(GTEItems.IMAGINARY_GROWTH_MEDIUM.asItem(), GTERecipeTypes.RED_SUN_STAR_CORE),
                Map.entry(GTEBlocks.IMAGINARY_LEAF_MATRIX.asItem(), GTRecipeTypes.ASSEMBLY_LINE_RECIPES));
    }

    private static GTRecipe registeredOutput(GameTestHelper helper, GTRecipeType type, Item output) {
        var recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(type).stream()
                .filter(recipe -> RecipeHelper.getOutputItems(recipe).stream().anyMatch(stack -> stack.is(output)))
                .toList();
        helper.assertTrue(recipes.size() == 1, "Expected one registered production recipe for " + output + ": " + recipes);
        var recipe = recipes.get(0);
        helper.assertTrue(type.getLookup().getLookup().getRecipes(false).anyMatch(indexed -> indexed.id.equals(recipe.id)),
                "Recipe disappeared from the ingredient lookup: " + recipe.id);
        helper.assertTrue(RecipeHelper.getRecipeEUtTier(recipe) == GTValues.UEV,
                "Foundation/tree production must operate at UEV: " + recipe.id);
        return recipe;
    }

    @GameTest(template = "empty", batch = "imaginaryFoundation", required = true)
    public static void registeredFoundationAndControllerHaveAnAcyclicPreTreeInputGraph(GameTestHelper helper) {
        Map<Item, GTRecipe> recipes = new HashMap<>();
        foundationTypes().forEach((item, type) -> recipes.put(item, registeredOutput(helper, type, item)));
        Item controller = GTEMultiMachines2.TREE_OF_IMAGINARY.asStack().getItem();
        var assembly = registeredOutput(helper, GTRecipeTypes.ASSEMBLY_LINE_RECIPES, controller);
        if (ConfigHolder.INSTANCE.machines.enableResearch) {
            var conditions = assembly.conditions.stream().filter(ResearchCondition.class::isInstance)
                    .map(ResearchCondition.class::cast).toList();
            helper.assertTrue(!conditions.isEmpty(), "Tree controller must retain its research prerequisite");
            for (var condition : conditions) {
                helper.assertTrue(condition.data.iterator().hasNext(), "Controller research must not be empty");
                for (var entry : condition.data) {
                    var scans = helper.getLevel().getRecipeManager().getAllRecipesFor(GTRecipeTypes.SCANNER_RECIPES).stream()
                            .filter(scan -> RecipeHelper.getOutputItems(scan).stream().anyMatch(stack -> {
                                var research = ResearchManager.readResearchId(stack);
                                return research != null && research.researchId().equals(entry.getResearchId());
                            })).toList();
                    helper.assertTrue(!scans.isEmpty() && scans.stream().allMatch(scan ->
                            RecipeHelper.getRecipeEUtTier(scan) == GTValues.UEV),
                            "Controller research must have an available UEV scanner recipe: " + entry.getResearchId());
                }
            }
        }
        recipes.put(controller, assembly);
        for (Item item : recipes.keySet()) {
            helper.assertTrue(hasPreTreePath(item, recipes, new HashSet<>(), controller),
                    "No acyclic pre-tree input path for " + recipes.get(item).id);
        }
        helper.succeed();
    }

    private static boolean hasPreTreePath(Item item, Map<Item, GTRecipe> recipes,
                                          Set<Item> visiting, Item controller) {
        if (!visiting.add(item)) return false;
        try {
            for (var ingredient : RecipeHelper.getInputContents(recipes.get(item), ItemRecipeCapability.CAP)) {
                // Tags are alternatives: a later tree circuit must not invalidate an available
                // predecessor circuit. Each ingredient still needs one complete acyclic path.
                boolean available = false;
                for (var stack : ingredient.getItems()) {
                    Item input = stack.getItem();
                    String path = BuiltInRegistries.ITEM.getKey(input).getPath();
                    if (input == controller || path.contains("imaginary_tree")) continue;
                    if (!recipes.containsKey(input) || hasPreTreePath(input, recipes, visiting, controller)) {
                        available = true;
                        break;
                    }
                }
                if (!available) return false;
            }
            return true;
        } finally {
            visiting.remove(item);
        }
    }

    private record Facility(WorkableElectricMultiblockMachine holder, MEOutputBusPartMachine output,
                            KeyStorage waitingBuffer) {}

    private static Facility facility(GameTestHelper helper) throws Exception {
        var controller = new BlockPos(0, 1, 0);
        var outputPos = new BlockPos(1, 1, 0);
        helper.setBlock(controller, GTEMultiMachines2.TREE_OF_IMAGINARY.getBlock());
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
                               FluidStack replacementWater, boolean substituteLeaf) throws Exception {
        facility.holder.getCapabilitiesProxy().clear();
        facility.holder.getCapabilitiesFlat().clear();
        facility.holder.addHandlerList(RecipeHandlerList.of(IO.OUT, List.of(facility.output.getInventory())));
        int slot = 0;
        for (var input : RecipeHelper.getInputItems(recipe)) {
            var pos = new BlockPos(slot++, 1, 2);
            helper.setBlock(pos, CreativeMachines.CREATIVE_ITEM_INPUT_BUS.getBlock());
            var bus = (CreativeInputBusPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(pos));
            var template = substituteLeaf && input.is(GTEItems.IMAGINARY_GROWTH_MEDIUM.asItem()) ?
                    GTEBlocks.IMAGINARY_LEAF_MATRIX.asStack() : input;
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

    @GameTest(template = "empty", batch = "imaginaryFoundation", required = true)
    public static void growthMediumAndLeafMatrixRequireThirdStageWater(GameTestHelper helper) throws Exception {
        var facility = facility(helper);
        var types = foundationTypes();
        for (Item product : List.of(GTEItems.IMAGINARY_GROWTH_MEDIUM.asItem(), GTEBlocks.IMAGINARY_LEAF_MATRIX.asItem())) {
            var recipe = registeredOutput(helper, types.get(product), product);
            for (var wrongWater : List.of(GTEMaterials.DistilledPurifiedWater.getFluid(1), GTEMaterials.UvPurifiedWater.getFluid(1))) {
                supply(helper, facility, recipe, wrongWater, false);
                helper.assertTrue(!RecipeHelper.matchRecipe(facility.holder, recipe).isSuccess(),
                        "Lower-tier water bypassed the pre-tree purification gate: " + recipe.id);
            }
            supply(helper, facility, recipe, null, false);
            helper.assertTrue(RecipeHelper.matchRecipe(facility.holder, recipe).isSuccess(),
                    "Third-stage water and complete foundation inputs must match " + recipe.id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "imaginaryFoundation", required = true)
    public static void registeredBouleAndWaferRejectLowerWaterAndCommitOneBatchToMeWaitingStorage(GameTestHelper helper) throws Exception {
        var facility = facility(helper);
        for (Item product : List.of(GTEItems.IMAGINARY_TREE_BOULE.asItem(), GTEItems.IMAGINARY_TREE_WAFER.asItem())) {
            var recipe = registeredOutput(helper, GTERecipeTypes.TREE_OF_IMAGINARY, product);
            for (var wrongWater : List.of(GTEMaterials.DistilledPurifiedWater.getFluid(1), GTEMaterials.UvPurifiedWater.getFluid(1))) {
                supply(helper, facility, recipe, wrongWater, false);
                helper.assertTrue(!RecipeHelper.matchRecipe(facility.holder, recipe).isSuccess(),
                        "Lower-tier water matched " + recipe.id);
            }
            if (product == GTEItems.IMAGINARY_TREE_BOULE.asItem()) {
                supply(helper, facility, recipe, null, true);
                helper.assertTrue(!RecipeHelper.matchRecipe(facility.holder, recipe).isSuccess(),
                        "Structural leaf matrix must not replace consumed growth medium");
                helper.assertTrue(RecipeHelper.getInputItems(recipe).stream().noneMatch(stack -> stack.is(GTEBlocks.IMAGINARY_LEAF_MATRIX.asItem())),
                        "Boule production must not consume structural leaf matrices");
            }
            supply(helper, facility, recipe, null, false);
            helper.assertTrue(RecipeHelper.matchRecipe(facility.holder, recipe).isSuccess() &&
                    RecipeHelper.matchTickRecipe(facility.holder, recipe).isSuccess(),
                    "Complete creative inputs and UEV power must match " + recipe.id);
            long before = facility.waitingBuffer.storage.getOrDefault(AEItemKey.of(new ItemStack(product)), 0L);
            helper.assertTrue(RecipeHelper.handleRecipeIO(facility.holder, recipe, IO.IN, new HashMap<>()).isSuccess(),
                    "Registered recipe input debit failed: " + recipe.id);
            helper.assertTrue(facility.waitingBuffer.storage.getOrDefault(AEItemKey.of(new ItemStack(product)), 0L) == before,
                    "Input phase prematurely emitted product");
            helper.assertTrue(RecipeHelper.handleRecipeIO(facility.holder, recipe, IO.OUT, new HashMap<>()).isSuccess(),
                    "Registered recipe output commit failed: " + recipe.id);
            int expected = product == GTEItems.IMAGINARY_TREE_BOULE.asItem() ? 4 : 16;
            helper.assertTrue(facility.waitingBuffer.storage.getOrDefault(AEItemKey.of(new ItemStack(product)), 0L) - before == expected,
                    "Exactly one batch must enter the actual ME waiting buffer (no connected AE network): " + recipe.id);
        }
        helper.succeed();
    }
}
