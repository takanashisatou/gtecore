package org.satou.gtecore.common.machine.multiblock.water;

import appeng.api.stacks.AEFluidKey;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEOutputBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEOutputHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeEnergyHatchPartMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputBusPartMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputHatchPartMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.satou.gtecore.common.command.WaterPurificationTestCommand;
import org.satou.gtecore.common.data.GTEMaterials;

/** Command parsing and complete installations, driven exclusively by normal world ticks. */
@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class WaterPurificationCommandGameTest {
    private static BlockPos emptySite(GameTestHelper helper, int tier) {
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 1)
                .subtract(WaterPurificationTestCommand.minimum(BlockPos.ZERO, tier)));
        for (BlockPos pos : BlockPos.betweenClosed(WaterPurificationTestCommand.minimum(anchor, tier),
                WaterPurificationTestCommand.maximum(anchor, tier))) {
            helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        return anchor;
    }

    @GameTest(template = "empty", batch = "waterCommand", required = true)
    public static void dispatcherAcceptsGradesAndLegacyCoordinates(GameTestHelper helper) {
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        var source = helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel()).withPermission(2);
        for (String suffix : new String[] { "", " 1", " 2", " 3", " 1 ~3 ~1 ~3", " 2 ~3 ~1 ~3",
                " 3 ~3 ~1 ~3", " 1 2 3", " ~3 ~1 ~3", " 2 1 2 3", " 3 1 2 3" }) {
            String command = "gte_water_test" + suffix;
            var parsed = dispatcher.parse(command, source);
            helper.assertTrue(!parsed.getReader().canRead() && parsed.getContext().getCommand() != null,
                    "Valid command must parse completely: " + command + "; " + parsed.getExceptions());
        }
        var invalid = dispatcher.parse("gte_water_test 4", source);
        helper.assertTrue(invalid.getReader().canRead() || invalid.getContext().getCommand() == null,
                "Unsupported grade 4 must not be executable");
        var denied = dispatcher.parse("gte_water_test 2", source.withPermission(1));
        helper.assertTrue(denied.getReader().canRead() || denied.getContext().getCommand() == null,
                "The test-building command must remain operator-only");
        helper.succeed();
    }

    @GameTest(template = "water_tier_lab", batch = "waterCommandBuild", timeoutTicks = 300, required = true)
    public static void gradeTwoProcessesUsingFourLampsAndCreativeSupplies(GameTestHelper helper) throws Exception {
        var layout = WaterPurificationTestCommand.build(helper.getLevel(), emptySite(helper, 2), 2);
        var unit = (UvPurificationUnitMachine) MetaMachine.getMachine(helper.getLevel(), layout.unit());
        var plant = assertInfrastructure(helper, layout, unit);
        var buffer = outputBuffer(helper, layout);
        helper.assertTrue(unit.getLampCount() == 4 && unit.getPowerPercent() == 100,
                "Grade 2 must contain four actual UV lamps configured at full power");
        helper.runAtTickTime(200, () -> {
            helper.assertTrue(amount(buffer, GTEMaterials.UvPurifiedWater.getFluid(1)) > 0,
                    "Grade 2 must produce actual UV water in the ME waiting buffer");
            assertSupplies(helper, layout, plant);
            helper.succeed();
        });
    }

    @GameTest(template = "water_tier_lab", batch = "waterCommandGradeThree", timeoutTicks = 1100, required = true)
    public static void gradeThreeNaturallyRegeneratesAndResumesProduction(GameTestHelper helper) throws Exception {
        var layout = WaterPurificationTestCommand.build(helper.getLevel(), emptySite(helper, 3), 3);
        var unit = (EdiPurificationUnitMachine) MetaMachine.getMachine(helper.getLevel(), layout.unit());
        var plant = assertInfrastructure(helper, layout, unit);
        var buffer = outputBuffer(helper, layout);
        helper.assertTrue(MetaMachine.getMachine(helper.getLevel(), layout.signal()) instanceof EdiLoadSignalHatchPartMachine &&
                MetaMachine.getMachine(helper.getLevel(), layout.control()) instanceof EdiRegenerationControlHatchPartMachine,
                "Grade 3 must contain real load-signal and regeneration-control ports");
        boolean[] observed = new boolean[4];
        long[] previousLoad = { 0 };
        long[] outputAtRelease = { 0 };
        for (int tick = 1; tick <= 1000; tick++) {
            helper.runAtTickTime(tick, () -> {
                long load = unit.getEdiState().load();
                boolean requested = unit.isRegenerationRequested();
                if (requested && unit.getEdiState().signal() >= 12) observed[0] = true;
                if (observed[0] && load < previousLoad[0]) observed[1] = true;
                if (observed[1] && !requested && unit.getEdiState().signal() <= 3 && !observed[2]) {
                    observed[2] = true;
                    outputAtRelease[0] = amount(buffer, GTEMaterials.UltrapureWater.getFluid(1));
                }
                if (observed[2] && amount(buffer, GTEMaterials.UltrapureWater.getFluid(1)) > outputAtRelease[0]) {
                    observed[3] = true;
                    assertSupplies(helper, layout, plant);
                    helper.succeed();
                    return;
                }
                previousLoad[0] = load;
            });
        }
        helper.runAtTickTime(1001, () -> {
            if (observed[3]) return;
            helper.assertTrue(observed[0] && observed[1] && observed[2] && observed[3],
                    "Natural production must set the latch, regenerate, reset and resume output: requested=" + observed[0]
                            + ", cleaned=" + observed[1] + ", released=" + observed[2] + ", resumed=" + observed[3]
                            + ", load=" + unit.getEdiState().load() + circuitDiagnostics(helper, layout));
            assertSupplies(helper, layout, plant);
            helper.succeed();
        });
    }

    private static CentralPurificationPlantMachine assertInfrastructure(GameTestHelper helper,
            WaterPurificationTestCommand.Layout layout, LinkedPurificationUnitMachine unit) {
        var plant = (CentralPurificationPlantMachine) MetaMachine.getMachine(helper.getLevel(), layout.plant());
        helper.assertTrue(plant.isFormed() && unit.isFormed() && unit.getPlant() == plant && plant.getParallel() == 64,
                "The selected stage must form, bind and use the configured 64 parallels");
        helper.assertTrue(MetaMachine.getMachine(helper.getLevel(), layout.itemOutput()) instanceof MEOutputBusPartMachine,
                "Selected stages must retain a real ME item output bus");
        var energy = (CreativeEnergyHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), layout.energy());
        helper.assertTrue(energy.energyContainer.getInputVoltage() == GTValues.V[GTValues.UEV],
                "Selected stages must receive UEV creative power");
        return plant;
    }

    private static void assertSupplies(GameTestHelper helper, WaterPurificationTestCommand.Layout layout,
            CentralPurificationPlantMachine plant) {
        var level = helper.getLevel();
        var water = (CreativeInputHatchPartMachine) MetaMachine.getMachine(level, layout.waterInput());
        var reagent = (CreativeInputHatchPartMachine) MetaMachine.getMachine(level, layout.flocculantInput());
        var items = (CreativeInputBusPartMachine) MetaMachine.getMachine(level, layout.itemInput());
        helper.assertTrue(water.tank.getFluidInTank(0).getAmount() == Integer.MAX_VALUE &&
                reagent.tank.getFluidInTank(0).getAmount() == Integer.MAX_VALUE &&
                (layout.tier() == 2 || items.getInventory().getStackInSlot(0).getCount() == Integer.MAX_VALUE),
                "Normally ticking creative supplies must remain unlimited after processing");
        helper.assertTrue(plant.getTransferredPerSecond() > 0, "The real plant must transfer power to its working unit");
    }

    private static KeyStorage outputBuffer(GameTestHelper helper, WaterPurificationTestCommand.Layout layout) throws Exception {
        var output = (MEOutputHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), layout.fluidOutput());
        var field = MEOutputHatchPartMachine.class.getDeclaredField("internalBuffer");
        field.setAccessible(true);
        return (KeyStorage) field.get(output);
    }

    private static long amount(KeyStorage buffer, FluidStack fluid) {
        return buffer.storage.getOrDefault(AEFluidKey.of(fluid.getFluid(), fluid.getTag()), 0L);
    }

    private static String circuitDiagnostics(GameTestHelper helper, WaterPurificationTestCommand.Layout layout) {
        var anchor = layout.minimum().subtract(WaterPurificationTestCommand.minimum(BlockPos.ZERO, 3));
        var text = new StringBuilder("; circuit:");
        for (int[] point : new int[][] { {4,1,6}, {4,1,7}, {4,1,13}, {4,1,14}, {5,1,14}, {5,1,13},
                {4,1,15}, {3,1,15}, {4,1,17}, {12,1,13}, {13,2,13}, {17,2,16}, {17,3,17},
                {17,3,20}, {-2,3,20}, {-2,3,1}, {0,3,-3}, {2,3,-3}, {2,2,-2}, {2,1,-1}, {2,1,0} }) {
            var pos = anchor.offset(point[0], point[1], point[2]);
            text.append(" ").append(pos.subtract(anchor)).append("=").append(helper.getLevel().getBlockState(pos))
                    .append(" in=").append(helper.getLevel().getBestNeighborSignal(pos));
            if (helper.getLevel().getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ComparatorBlockEntity comparator) {
                text.append(" out=").append(comparator.getOutputSignal());
            }
        }
        return text.toString();
    }

    @GameTest(template = "water_tier_lab", batch = "waterCommandGuard", required = true)
    public static void selectedGradeChecksEntireFootprintBeforeWriting(GameTestHelper helper) {
        for (int tier : new int[] { 2, 3 }) {
            BlockPos anchor = emptySite(helper, tier);
            BlockPos minimum = WaterPurificationTestCommand.minimum(anchor, tier);
            BlockPos maximum = WaterPurificationTestCommand.maximum(anchor, tier);
            helper.getLevel().setBlockAndUpdate(maximum, Blocks.BEDROCK.defaultBlockState());
            boolean rejected = false;
            try {
                WaterPurificationTestCommand.build(helper.getLevel(), anchor, tier);
            } catch (IllegalStateException expected) {
                rejected = true;
            }
            helper.assertTrue(rejected && helper.getLevel().getBlockState(maximum).is(Blocks.BEDROCK),
                    "Grade " + tier + " must reject an obstruction at the far corner");
            for (BlockPos pos : BlockPos.betweenClosed(minimum, maximum)) {
                if (!pos.equals(maximum)) helper.assertTrue(helper.getLevel().isEmptyBlock(pos),
                        "Rejected construction changed " + pos);
            }
            helper.getLevel().setBlockAndUpdate(maximum, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = "water_tier_lab", batch = "waterCommandGuard", required = true)
    public static void invalidGradeAndUnloadedSiteCannotBuild(GameTestHelper helper) {
        BlockPos anchor = emptySite(helper, 3);
        boolean rejected = false;
        try {
            WaterPurificationTestCommand.build(helper.getLevel(), anchor, 4);
        } catch (IllegalArgumentException | IllegalStateException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "The build API must reject unsupported grade 4");
        for (BlockPos pos : BlockPos.betweenClosed(WaterPurificationTestCommand.minimum(anchor, 3),
                WaterPurificationTestCommand.maximum(anchor, 3))) {
            helper.assertTrue(helper.getLevel().isEmptyBlock(pos), "Invalid grade changed a block at " + pos);
        }
        BlockPos unloaded = new BlockPos(29_000_000, anchor.getY(), 29_000_000);
        helper.assertTrue(!helper.getLevel().hasChunkAt(unloaded), "Remote guard fixture unexpectedly loaded");
        for (int tier : new int[] { 2, 3 }) {
            rejected = false;
            try {
                WaterPurificationTestCommand.build(helper.getLevel(), unloaded, tier);
            } catch (IllegalStateException expected) {
                rejected = true;
            }
            helper.assertTrue(rejected && !helper.getLevel().hasChunkAt(unloaded),
                    "Construction must neither load nor modify an unloaded grade " + tier + " site");
        }
        helper.succeed();
    }
}
