package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeEnergyHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEOutputHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEOutputBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputHatchPartMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputBusPartMachine;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import appeng.api.stacks.AEFluidKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.satou.gtecore.common.command.WaterPurificationTestCommand;
import org.satou.gtecore.common.data.GTEMaterials;

/** Real structures, real recipes, and vanilla redstone ticks: no simulated control loop. */
@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class ThermalRedstoneGameTest {
    private static BlockPos emptySite(GameTestHelper helper) {
        // Keep the entire facility inside its template so GameTest owns/ticks all of its chunks.
        BlockPos anchor = helper.absolutePos(new BlockPos(0, 1, 3));
        for (BlockPos pos : BlockPos.betweenClosed(WaterPurificationTestCommand.minimum(anchor),
                WaterPurificationTestCommand.maximum(anchor))) {
            helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        return anchor;
    }

    @GameTest(template = "thermal_lab", batch = "thermalRedstone", timeoutTicks = 1000, required = true)
    public static void builtPlantTracksChangingTargetAndFaultsWhenWireIsBroken(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var layout = WaterPurificationTestCommand.build(level, emptySite(helper));
        var unit = (ThermalPurificationUnitMachine) MetaMachine.getMachine(level, layout.unit());
        var plant = (CentralPurificationPlantMachine) MetaMachine.getMachine(level, layout.plant());
        var signal = (ThermalSignalHatchPartMachine) MetaMachine.getMachine(level, layout.signal());
        var control = (ThermalControlHatchPartMachine) MetaMachine.getMachine(level, layout.control());
        var output = (MEOutputHatchPartMachine) MetaMachine.getMachine(level, layout.fluidOutput());
        helper.assertTrue(MetaMachine.getMachine(level, layout.itemOutput()) instanceof MEOutputBusPartMachine,
                "Test facility must use a real ME item output bus");
        var water = (CreativeInputHatchPartMachine) MetaMachine.getMachine(level, layout.waterInput());
        var flocculant = (CreativeInputHatchPartMachine) MetaMachine.getMachine(level, layout.flocculantInput());
        var carbon = (CreativeInputBusPartMachine) MetaMachine.getMachine(level, layout.itemInput());
        var fluidTemplateField = CreativeInputHatchPartMachine.class.getDeclaredField("creativeTanks");
        fluidTemplateField.setAccessible(true);
        var fluidTemplates = (CustomFluidTank[]) fluidTemplateField.get(water);
        helper.assertTrue(fluidTemplates[0].getFluid().getAmount() == 1,
                "Creative fluid supply must populate its persistent phantom configuration, not just the live tank");
        var itemTemplateField = CreativeInputBusPartMachine.class.getDeclaredField("creativeStorage");
        itemTemplateField.setAccessible(true);
        helper.assertTrue(((ItemStackTransfer) itemTemplateField.get(carbon)).getStackInSlot(0).getCount() == 1,
                "Creative item supply must populate its persistent phantom configuration");
        var bufferField = MEOutputHatchPartMachine.class.getDeclaredField("internalBuffer");
        bufferField.setAccessible(true);
        var buffer = (KeyStorage) bufferField.get(output);
        var purified = GTEMaterials.DistilledPurifiedWater.getFluid(1);
        var purifiedKey = AEFluidKey.of(purified.getFluid(), purified.getTag());
        var energy = (CreativeEnergyHatchPartMachine) MetaMachine.getMachine(level, layout.energy());
        helper.assertTrue(unit.isFormed() && plant.isFormed() && unit.getPlant() == plant,
                "Command must produce genuinely formed and linked machines");
        helper.assertTrue(level.getServer().getCommands().getDispatcher().getRoot().getChild("gte_water_test") != null,
                "One-command test facility must be registered");
        helper.assertTrue(signal.getOutputDirectSignal(Direction.SOUTH) == 0,
                "Signal hatch must not strongly power internal casings");

        helper.runAtTickTime(100, () -> {
            helper.assertTrue(unit.getThermalState().stableTicks() > 40,
                    "Constructed machine must actually process recipes and gain stability");
            helper.assertTrue(buffer.storage.getOrDefault(purifiedKey, 0L) > 0,
                    "Real recipe outputs must enter the ME hatch's waiting buffer even without an AE network");
            helper.assertTrue(water.tank.getFluidInTank(0).getAmount() == Integer.MAX_VALUE &&
                    flocculant.tank.getFluidInTank(0).getAmount() == Integer.MAX_VALUE &&
                    carbon.getInventory().getStackInSlot(0).getCount() == Integer.MAX_VALUE,
                    "Creative inputs must not be depleted by successful processing");
            helper.assertTrue(plant.getTransferredPerSecond() > 0,
                    "The plant must transfer power from the real creative energy hatch");
            helper.assertTrue(energy.energyContainer.getEnergyStored() == energy.energyContainer.getEnergyCapacity()
                    && energy.energyContainer.getEnergyStored() > 0,
                    "Creative energy must remain available after actual processing");
            var state = unit.getThermalState().snapshot();
            // Advance only the schedule for the test. The production recipe tick selects its real random target.
            unit.getThermalState().restore(new PurificationThermalState.Snapshot(state.temperatureDeciC(),
                    state.targetCenterC(), 1199, state.outOfRangeTicks(), state.stableTicks(),
                    state.faulted(), state.outputRemainder()));
        });
        helper.runAtTickTime(250, () -> {
            helper.assertTrue(unit.getThermalState().snapshot().targetCenterC() != 65,
                    "A real working tick must change the target at the interval boundary");
            helper.assertTrue(unit.getThermalState().isInRange() && !unit.getThermalState().isFaulted(),
                    "Vanilla dust/repeater circuit must follow the changed target within the grace period");
            var state = unit.getThermalState().snapshot();
            // Force a low-temperature condition to verify actual signal direction and physical wiring.
            unit.getThermalState().restore(new PurificationThermalState.Snapshot(state.targetCenterC() * 10 - 25,
                    state.targetCenterC(), state.phaseTicks(), state.outOfRangeTicks(), state.stableTicks(),
                    state.faulted(), state.outputRemainder()));
        });
        helper.runAtTickTime(260, () -> {
            helper.assertTrue(signal.getOutputSignal(Direction.SOUTH) == 15 && signal.getOutputSignal(Direction.NORTH) == 0,
                    "Only the outward face of the signal hatch must emit the heating request");
            helper.assertTrue(control.isHeating(), "Real redstone wires must deliver the request to the input hatch");
        });
        helper.runAtTickTime(420, () -> {
            helper.assertTrue(!unit.getThermalState().isFaulted() && unit.getThermalState().isInRange(),
                    "Physical feedback must stabilize without command-block assistance: state=" + unit.getThermalState().snapshot()
                            + ", signal=" + signal.getOutputSignal(Direction.SOUTH) + ", input=" + control.getRedstoneStrength()
                            + ", repeater=" + level.getBlockState(layout.repeater()) + ", formed=" + unit.isFormed());
            level.setBlockAndUpdate(layout.repeater(), Blocks.AIR.defaultBlockState());
        });
        helper.runAtTickTime(430, () -> helper.assertTrue(!control.isHeating(),
                "Breaking the repeater must really disconnect the input, not leave hidden direct power"));
        helper.runAtTickTime(850, () -> {
            helper.assertTrue(unit.getThermalState().isFaulted() && unit.getThermalState().stableTicks() == 0,
                    "Continuous cooling after a broken circuit must trigger the 15-second grace failure");
            helper.assertTrue(unit.getParts().stream().anyMatch(part -> part instanceof IMaintenanceMachine maintenance
                    && maintenance.hasMaintenanceProblems()), "A broken control loop must trigger real GTM maintenance");
            helper.succeed();
        });
    }

    @GameTest(template = "thermal_lab", batch = "thermalBuildGuard", required = true)
    public static void occupiedBuildAreaIsNotOverwritten(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos anchor = emptySite(helper);
        BlockPos obstruction = anchor.offset(3, 1, 3);
        level.setBlockAndUpdate(obstruction, Blocks.BEDROCK.defaultBlockState());
        boolean rejected = false;
        try {
            WaterPurificationTestCommand.build(level, anchor);
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected && level.getBlockState(obstruction).is(Blocks.BEDROCK),
                "Occupied volume must be rejected without changing the obstruction");
        for (BlockPos pos : BlockPos.betweenClosed(WaterPurificationTestCommand.minimum(anchor),
                WaterPurificationTestCommand.maximum(anchor))) {
            if (!pos.equals(obstruction)) helper.assertTrue(level.isEmptyBlock(pos),
                    "Rejected construction left a partial installation at " + pos);
        }
        helper.succeed();
    }
}
