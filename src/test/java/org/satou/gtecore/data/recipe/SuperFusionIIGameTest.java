package org.satou.gtecore.data.recipe;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.satou.gtecore.common.data.GTEBlocks;
import org.satou.gtecore.common.data.GTEMaterials;
import org.satou.gtecore.common.data.GTERecipeTypes;
import org.satou.gtecore.common.data.machines.GTEFusionMachines;
import org.satou.gtecore.common.data.machines.GTEMultiMachine;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.ingot;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.plate;

@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class SuperFusionIIGameTest {

    private static GTRecipe output(GameTestHelper helper, GTRecipeType type, Item item) {
        var recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(type).stream()
                .filter(recipe -> RecipeHelper.getOutputItems(recipe).stream().anyMatch(stack -> stack.is(item))).toList();
        helper.assertTrue(recipes.size() == 1, "Expected one production recipe for " + item + ": " + recipes);
        return recipes.getFirst();
    }

    private static GTRecipe alloy(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(GTERecipeTypes.SUPER_FUSION_REACTOR_II_RECIPES)
                .stream().filter(recipe -> RecipeHelper.getOutputFluids(recipe).stream()
                        .anyMatch(stack -> stack.getFluid() == GTEMaterials.YinYangFusionAlloy.getFluid())).toList();
        helper.assertTrue(recipes.size() == 1, "Expected one Super Fusion II alloy recipe");
        return recipes.getFirst();
    }

    @GameTest(template = "empty", batch = "superFusionProgression", required = true)
    public static void uhvBootstrapReachesUevAndImaginaryCasings(GameTestHelper helper) {
        var controller = output(helper, GTRecipeTypes.ASSEMBLY_LINE_RECIPES,
                GTEFusionMachines.SUPER_FUSION_REACTOR_II.asStack().getItem());
        var casing = output(helper, GTRecipeTypes.ASSEMBLER_RECIPES, GTBlocks.MACHINE_CASING_UEV.asItem());
        for (var recipe : List.of(controller, casing, alloy(helper))) {
            helper.assertTrue(RecipeHelper.getRecipeEUtTier(recipe) == GTValues.UHV,
                    "UEV bootstrap must be executable at UHV: " + recipe.id);
        }
        helper.assertTrue(RecipeHelper.getInputItems(controller).stream()
                        .anyMatch(stack -> stack.is(GTEMultiMachine.Super_Fusion_Reactor.asStack().getItem())),
                "Super Fusion II must upgrade the existing Super Fusion reactor");
        for (var ingredient : RecipeHelper.getInputContents(controller, ItemRecipeCapability.CAP)) {
            helper.assertTrue(Arrays.stream(ingredient.getItems()).anyMatch(stack -> {
                String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                return !id.startsWith("uev_") && !id.contains("imaginary") && !id.contains("yin_yang_fusion_alloy") &&
                        stack.getItem() != GTEFusionMachines.SUPER_FUSION_REACTOR_II.asStack().getItem();
            }), "Controller depends on a product beyond its own bootstrap stage");
        }
        var alloyIngot = ChemicalHelper.get(ingot, GTEMaterials.YinYangFusionAlloy);
        var alloyPlate = ChemicalHelper.get(plate, GTEMaterials.YinYangFusionAlloy);
        helper.assertTrue(!alloyIngot.isEmpty() && !alloyPlate.isEmpty(), "Alloy ingot/plate forms were not registered");
        helper.assertTrue(helper.getLevel().getRecipeManager().getAllRecipesFor(GTRecipeTypes.FLUID_SOLIDFICATION_RECIPES)
                        .stream().anyMatch(recipe -> RecipeHelper.getInputFluids(recipe).stream()
                                .anyMatch(fluid -> fluid.getFluid() == GTEMaterials.YinYangFusionAlloy.getFluid()) &&
                                RecipeHelper.getOutputItems(recipe).stream().anyMatch(stack -> stack.is(alloyIngot.getItem()))),
                "Fusion alloy cannot be cast into ingots");
        helper.assertTrue(RecipeHelper.getInputItems(casing).stream().anyMatch(stack -> stack.is(alloyPlate.getItem())),
                "UEV casing bypasses the new Fusion II alloy");
        var imaginary = output(helper, GTRecipeTypes.ASSEMBLY_LINE_RECIPES, GTEBlocks.IMAGINARY_CASING.asItem());
        helper.assertTrue(RecipeHelper.getInputItems(imaginary).stream()
                        .anyMatch(stack -> stack.is(GTBlocks.MACHINE_CASING_UEV.asItem()) && stack.getCount() == 8),
                "Imaginary casing does not consume UEV mechanical casings");
        helper.assertTrue(Arrays.asList(GTEFusionMachines.SUPER_FUSION_REACTOR_II.getRecipeTypes())
                        .contains(GTERecipeTypes.SUPER_FUSION_REACTOR_RECIPE) &&
                        !Arrays.asList(GTEMultiMachine.Super_Fusion_Reactor.getRecipeTypes())
                                .contains(GTERecipeTypes.SUPER_FUSION_REACTOR_II_RECIPES),
                "Upgrade must retain old recipes while keeping the new alloy exclusive");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "superFusionStructure", required = true)
    public static void previewAndRealStructureRetainTheOriginalFusionRing(GameTestHelper helper) {
        var before = GTEMultiMachine.Super_Fusion_Reactor.getMatchingShapes().getFirst().getBlocks();
        var after = GTEFusionMachines.SUPER_FUSION_REACTOR_II.getMatchingShapes().getFirst().getBlocks();
        helper.assertTrue(after.length == 15 && after[0].length == 3 && after[0][0].length == 15,
                "Fusion II must retain the 15 x 3 x 15 ring");
        int coils = 0, controllers = 0;
        for (int z = 0; z < 15; z++) {
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 15; x++) {
                    var state = after[z][y][x].getBlockState();
                    helper.assertTrue(state.isAir() == before[z][y][x].getBlockState().isAir(),
                            "Fusion ring occupied coordinates changed at " + x + "," + y + "," + z);
                    if (state.is(GTEBlocks.YIN_YANG_COIL.get())) coils++;
                    if (state.is(GTEFusionMachines.SUPER_FUSION_REACTOR_II.getBlock())) controllers++;
                }
            }
        }
        helper.assertTrue(coils == 4 && controllers == 1,
                "Preview must contain four coils and one controller: coils=" + coils + ", controllers=" + controllers);
        var pos = helper.absolutePos(new BlockPos(0, 20, 0));
        for (var clear : BlockPos.betweenClosed(pos.offset(-18, -2, -18), pos.offset(18, 5, 18))) {
            helper.getLevel().setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
        }
        helper.getLevel().setBlockAndUpdate(pos, GTEFusionMachines.SUPER_FUSION_REACTOR_II.getBlock().defaultBlockState());
        var machine = (WorkableElectricMultiblockMachine) MetaMachine.getMachine(helper.getLevel(), pos);
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        GameType previous = player.gameMode.getGameModeForPlayer();
        player.setGameMode(GameType.CREATIVE);
        try {
            machine.getPattern().autoBuild(player, new MultiblockState(helper.getLevel(), pos));
        } finally {
            player.setGameMode(previous);
        }
        helper.assertTrue(machine.checkPatternWithLock(), "Real Fusion II structure did not form: " + machine.getMultiblockState().error);
        machine.onStructureFormed();
        helper.assertTrue(machine.isFormed(), "Formation lifecycle did not accept the ring");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "superFusionRecipeIO", required = true)
    public static void registeredAlloyRecipeConsumesInputsAndProducesMoltenAlloy(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 2, 0), GTEFusionMachines.SUPER_FUSION_REACTOR_II.getBlock());
        var machine = (WorkableElectricMultiblockMachine) MetaMachine.getMachine(helper.getLevel(),
                helper.absolutePos(new BlockPos(0, 2, 0)));
        var recipe = alloy(helper);
        int i = 0;
        for (var fluid : RecipeHelper.getInputFluids(recipe)) {
            var pos = new BlockPos(++i, 2, 0);
            helper.setBlock(pos, GTMachines.FLUID_IMPORT_HATCH[GTValues.UHV].getBlock());
            var hatch = (FluidHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(pos));
            hatch.tank.setFluidInTank(0, fluid.copy());
            machine.addHandlerList(RecipeHandlerList.of(IO.IN, hatch.tank));
        }
        var outputPos = new BlockPos(++i, 2, 0);
        helper.setBlock(outputPos, GTMachines.FLUID_EXPORT_HATCH[GTValues.UHV].getBlock());
        var output = (FluidHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(outputPos));
        machine.addHandlerList(RecipeHandlerList.of(IO.OUT, output.tank));
        helper.assertTrue(RecipeHelper.matchRecipe(machine, recipe).isSuccess(), "Registered alloy recipe does not match its inputs");
        helper.assertTrue(RecipeHelper.handleRecipeIO(machine, recipe, IO.IN, Map.of()).isSuccess(), "Alloy input consumption failed");
        helper.assertTrue(RecipeHelper.handleRecipeIO(machine, recipe, IO.OUT, Map.of()).isSuccess(), "Molten alloy output failed");
        helper.assertTrue(output.tank.getFluidInTank(0).getFluid() == GTEMaterials.YinYangFusionAlloy.getFluid() &&
                        output.tank.getFluidInTank(0).getAmount() == 2304,
                "Actual output does not contain the registered molten alloy amount");
        helper.succeed();
    }
}
