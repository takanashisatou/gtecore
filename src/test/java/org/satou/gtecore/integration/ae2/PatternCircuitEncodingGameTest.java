package org.satou.gtecore.integration.ae2;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemHandlerHelper;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import org.satou.gtecore.common.ProgrammableCircuitHandlerModify;

import java.util.List;

@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class PatternCircuitEncodingGameTest {

    private static GTRecipe recipe(int config) {
        return GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("pattern_circuit_test_" + config)
                .inputItems(new ItemStack(Items.COBBLESTONE, 2))
                .notConsumable(new ItemStack(Items.IRON_AXE))
                .circuitMeta(config)
                .outputItems(new ItemStack(Items.STONE))
                .duration(20).EUt(8).buildRawRecipe();
    }

    private static List<List<GenericStack>> inputs() {
        return List.of(List.of(new GenericStack(AEItemKey.of(Items.COBBLESTONE), 2)));
    }

    @GameTest(template = "empty", batch = "patternCircuit", required = true)
    public static void encodedPatternPreservesCircuitNbtAndOrdinaryInputs(GameTestHelper helper) {
        var original = List.of(inputs().getFirst(),
                List.of(new GenericStack(AEFluidKey.of(new FluidStack(Fluids.WATER, 1000)), 1000)));
        var retained = PatternCircuitEncoding.retainCircuits(original, recipe(8));
        var encodedInputs = retained.stream().map(List::getFirst).toArray(GenericStack[]::new);
        var output = new GenericStack(AEItemKey.of(Items.STONE), 1);
        var pattern = PatternDetailsHelper.encodeProcessingPattern(encodedInputs, new GenericStack[] { output });
        var decoded = PatternDetailsHelper.decodePattern(pattern, helper.getLevel());
        helper.assertTrue(decoded != null && decoded.getInputs().length == 3,
                "Encoded processing pattern lost its circuit or ordinary item/fluid inputs");
        helper.assertTrue(retained.getFirst().equals(original.getFirst()) &&
                        retained.get(1).equals(original.get(1)) && original.size() == 2,
                "Circuit retention changed the original item/fluid ingredients or caller's list");
        var key = (AEItemKey) retained.getLast().getFirst().what();
        helper.assertTrue(IntCircuitBehaviour.getCircuitConfiguration(key.toStack()) == 8 &&
                        retained.getLast().getFirst().amount() == 1 &&
                        decoded.getInputs()[2].getPossibleInputs()[0].what().equals(key),
                "Programmed circuit NBT or selector count was changed");
        helper.assertTrue(decoded.getOutputs()[0].equals(output), "Pattern output was changed");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "patternCircuit", required = true)
    public static void existingCircuitIsNotDuplicated(GameTestHelper helper) {
        var circuit = new GenericStack(AEItemKey.of(IntCircuitBehaviour.stack(16)), 3);
        var original = List.of(inputs().getFirst(), List.of(circuit));
        var retained = PatternCircuitEncoding.retainCircuits(original, recipe(16));
        helper.assertTrue(retained.equals(original), "Existing circuit was duplicated or its count was changed");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "patternCircuit", required = true)
    public static void onlyProgrammedInputCircuitsAreRetained(GameTestHelper helper) {
        var ordinary = GTRecipeTypes.ASSEMBLER_RECIPES.recipeBuilder("ordinary_pattern_test")
                .inputItems(new ItemStack(Items.COBBLESTONE, 2))
                .inputItems(IntCircuitBehaviour.stack(6))
                .notConsumable(new ItemStack(Items.IRON_AXE))
                .outputItems(IntCircuitBehaviour.stack(4))
                .duration(20).EUt(8).buildRawRecipe();
        helper.assertTrue(PatternCircuitEncoding.retainCircuits(inputs(), ordinary).equals(inputs()),
                "Ordinary catalyst, consumed ingredient or output circuit was added to the processing inputs");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "patternCircuit", required = true)
    public static void circuitConfigurationZeroAndMaximumStayDistinct(GameTestHelper helper) {
        var zero = new GenericStack(AEItemKey.of(IntCircuitBehaviour.stack(0)), 1);
        var original = List.of(inputs().getFirst(), List.of(zero));
        helper.assertTrue(PatternCircuitEncoding.retainCircuits(original, recipe(0)).equals(original),
                "Circuit zero was duplicated");
        var retained = PatternCircuitEncoding.retainCircuits(original, recipe(32));
        helper.assertTrue(retained.size() == 3 && retained.get(1).getFirst().equals(zero) &&
                        retained.getLast().getFirst().what().equals(AEItemKey.of(IntCircuitBehaviour.stack(32))),
                "Circuit configurations were compared by item alone or their NBT was lost");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "patternCircuit", required = true)
    public static void retainedCircuitUsesExistingSingleBlockReceiver(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(0, 2, 0));
        helper.getLevel().setBlockAndUpdate(pos, GTMachines.ASSEMBLER[GTValues.LV].getBlock().defaultBlockState());
        if (!(MetaMachine.getMachine(helper.getLevel(), pos) instanceof SimpleTieredMachine machine)) {
            helper.fail("Expected a registered single-block assembler");
            return;
        }
        helper.assertTrue(machine.getCircuitInventory() instanceof ProgrammableCircuitHandlerModify,
                "The existing GTECore circuit receiver is not active");
        var retained = PatternCircuitEncoding.retainCircuits(inputs(), recipe(8));
        var circuit = ((AEItemKey) retained.getLast().getFirst().what()).toStack(1);
        var capability = machine.getItemHandlerCap(null, false);
        helper.assertTrue(capability != null && ItemHandlerHelper.insertItem(capability, circuit, false).isEmpty(),
                "Existing single-block receiver rejected the retained circuit");
        helper.assertTrue(IntCircuitBehaviour.getCircuitConfiguration(
                machine.getCircuitInventory().storage.getStackInSlot(0)) == 8,
                "Existing single-block receiver did not apply the pattern's circuit configuration");
        helper.succeed();
    }
}
