package org.satou.gtecore.common.machine.multiblock.water;

import org.satou.gtecore.common.data.GTEMaterials;
import org.satou.gtecore.common.data.GTERecipeTypes;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationMachines;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationParts;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEOutputHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeEnergyHatchPartMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputHatchPartMachine;
import com.hepdd.gtmthings.data.CreativeMachines;

import appeng.api.stacks.AEFluidKey;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Registered machines and real recipe IO; only structure membership is injected. */
@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class UvPurificationGameTest {
    private static final BlockPos PLANT = new BlockPos(0, 1, 0);
    private static final BlockPos UNIT = new BlockPos(1, 1, 0);
    private static final BlockPos INPUT = new BlockPos(2, 1, 0);
    private static final BlockPos OUTPUT = new BlockPos(3, 1, 0);
    private static final BlockPos ENERGY = new BlockPos(4, 1, 0);

    private record Facility(UvPurificationUnitMachine unit, CentralPurificationPlantMachine plant,
                            CreativeInputHatchPartMachine input, NotifiableFluidTank output,
                            KeyStorage waitingBuffer) {
        UvPurificationUnitMachine.UvRecipeLogic logic() {
            return (UvPurificationUnitMachine.UvRecipeLogic) unit.getRecipeLogic();
        }

        long produced() {
            if (waitingBuffer != null) {
                var fluid = GTEMaterials.UvPurifiedWater.getFluid(1);
                return waitingBuffer.storage.getOrDefault(AEFluidKey.of(fluid.getFluid(), fluid.getTag()), 0L);
            }
            long result = 0;
            for (int slot = 0; slot < output.getTanks(); slot++) {
                if (output.getFluidInTank(slot).isFluidEqual(GTEMaterials.UvPurifiedWater.getFluid(1))) {
                    result += output.getFluidInTank(slot).getAmount();
                }
            }
            return result;
        }

        void charge() throws Exception {
            // The driver executes separate logical ticks within one GameTest callback. Give each
            // tick its own native packet allowance, while retaining the real plant debit/credit path.
            setField(NotifiableEnergyContainer.class, unit.internalEnergy, "lastTimeStamp", Long.MIN_VALUE);
            var transfer = CentralPurificationPlantMachine.class.getDeclaredMethod("transferEnergyToUnits");
            transfer.setAccessible(true);
            transfer.invoke(plant);
        }

        String diagnostics() {
            return " [progress=" + logic().getProgress() + "/" + logic().getDuration() +
                    ", status=" + logic().getStatus() + ", EU=" + unit.getStoredLinkedEnergy() +
                    ", state=" + unit.getUvState().snapshot() + ", output=" + produced() + "]";
        }
    }

    private static Facility facility(GameTestHelper helper, int lamps, boolean finiteOutput) throws Exception {
        helper.setBlock(PLANT, GTEWaterPurificationMachines.CENTRAL_WATER_PURIFICATION_PLANT.getBlock());
        helper.setBlock(UNIT, GTEWaterPurificationMachines.T2_UV_OXIDATION_PURIFICATION_UNIT.getBlock());
        helper.setBlock(INPUT, CreativeMachines.CREATIVE_FLUID_INPUT_HATCH.getBlock());
        helper.setBlock(ENERGY, CreativeMachines.CREATIVE_ENERGY_INPUT_HATCH.getBlock());
        helper.setBlock(OUTPUT, finiteOutput ? GTMachines.FLUID_EXPORT_HATCH[GTValues.LV].getBlock() :
                GTAEMachines.FLUID_EXPORT_HATCH_ME.getBlock());
        var unit = (UvPurificationUnitMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(UNIT));
        var plant = (CentralPurificationPlantMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(PLANT));
        var input = (CreativeInputHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(INPUT));
        var energy = (CreativeEnergyHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(ENERGY));
        var output = (FluidHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(OUTPUT));
        var configureFluid = CreativeInputHatchPartMachine.class.getDeclaredMethod("setFluid", int.class, FluidStack.class);
        configureFluid.setAccessible(true);
        configureFluid.invoke(input, 0, GTEMaterials.DistilledPurifiedWater.getFluid(1));
        input.tank.setFluidInTank(0, GTEMaterials.DistilledPurifiedWater.getFluid(Integer.MAX_VALUE));

        setField(CreativeEnergyHatchPartMachine.class, energy, "voltage", GTValues.V[GTValues.UEV]);
        setField(CreativeEnergyHatchPartMachine.class, energy, "amps", 256);
        setField(CreativeEnergyHatchPartMachine.class, energy, "maxEnergy", GTValues.V[GTValues.UEV] * 256);
        setField(CreativeEnergyHatchPartMachine.class, energy, "setTier", GTValues.UEV);
        energy.loadCustomPersistedData(new CompoundTag());
        setField(WorkableElectricMultiblockMachine.class, plant, "energyContainer",
                new EnergyContainerList(List.of(energy.energyContainer)));
        setField(WorkableElectricMultiblockMachine.class, unit, "energyContainer",
                new EnergyContainerList(List.of(unit.internalEnergy)));
        setField(MultiblockControllerMachine.class, plant, "isFormed", true);
        setField(MultiblockControllerMachine.class, unit, "isFormed", true);
        var parts = new ArrayList<BlockPos>();
        for (int index = 0; index < lamps; index++) {
            var pos = new BlockPos(index, 1, 2);
            helper.setBlock(pos, GTEWaterPurificationParts.UV_LAMP_HATCH.getBlock());
            parts.add(helper.absolutePos(pos));
        }
        setField(MultiblockControllerMachine.class, unit, "partPositions", parts.toArray(BlockPos[]::new));
        unit.addHandlerList(RecipeHandlerList.of(IO.IN, List.of(input.tank, unit.internalEnergy)));
        unit.addHandlerList(RecipeHandlerList.of(IO.OUT, List.of(output.tank)));
        unit.bindToPlant(plant.getPos());
        plant.setParallel(1);
        unit.setPowerPercent(100);
        KeyStorage buffer = null;
        if (output instanceof MEOutputHatchPartMachine me) {
            var field = MEOutputHatchPartMachine.class.getDeclaredField("internalBuffer");
            field.setAccessible(true);
            buffer = (KeyStorage) field.get(me);
        }
        var result = new Facility(unit, plant, input, output.tank, buffer);
        result.charge();
        return result;
    }

    private static GTRecipe recipe() {
        var recipe = GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("uv_integration_batch")
                .inputFluids(GTEMaterials.DistilledPurifiedWater.getFluid(1000))
                .outputFluids(GTEMaterials.UvPurifiedWater.getFluid(1000))
                .duration(20).EUt(GTValues.V[GTValues.UEV]).buildRawRecipe();
        recipe.data.putInt("waterPurificationTier", GTValues.LuV);
        recipe.data.putInt("uvDose", 40);
        return recipe;
    }

    private static GTRecipe modify(Facility facility) {
        var origin = recipe();
        return UvPurificationUnitMachine.recipeModifier(facility.unit, origin).apply(origin);
    }

    private static void begin(GameTestHelper helper, Facility facility) {
        var modified = modify(facility);
        helper.assertTrue(modified != null, "UV recipe must be runnable with lamps and a linked plant");
        facility.logic().setupRecipe(modified);
        helper.assertTrue(facility.logic().isWorking() && facility.unit.getUvState().active(),
                "Successful recipe input must start a UV batch");
    }

    private static void work(Facility facility, int ticks) throws Exception {
        for (int tick = 0; tick < ticks; tick++) {
            facility.charge();
            facility.logic().handleRecipeWorking();
        }
    }

    @GameTest(template = "empty", batch = "uvPurification", required = true)
    public static void registeredTierAndLampCapacityDetermineDuration(GameTestHelper helper) throws Exception {
        var facility = facility(helper, 1, false);
        helper.assertTrue(facility.unit.getUnitTier() == GTValues.LuV &&
                facility.unit.getLinkVoltage() == GTValues.V[GTValues.UEV],
                "Stage identity must remain T2 while operating at UEV");
        var foreign = recipe();
        foreign.data.putInt("waterPurificationTier", GTValues.EV);
        helper.assertTrue(!facility.unit.acceptsPurificationRecipe(foreign), "UV machine accepted T1 water recipe");
        helper.assertTrue(facility.unit.getLampCount() == 1, "Registered lamp did not count as one UV lamp");
        facility.plant.setParallel(64);
        var full = modify(facility);
        helper.assertTrue(full != null && full.parallels == 64 && full.duration == 40,
                "One lamp at 100% must expose 64 parallels with 40-tick dose duration");
        facility.unit.setPowerPercent(25);
        var dim = modify(facility);
        helper.assertTrue(dim != null && dim.parallels == 64 && dim.duration == 160,
                "25% power must retain lamp capacity and extend dose time to 160 ticks");
        setField(MultiblockControllerMachine.class, facility.unit, "partPositions", new BlockPos[0]);
        helper.assertTrue(facility.unit.getLampCount() == 0 && modify(facility) == null,
                "A UV machine without a lamp must reject recipes");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "uvPurification", required = true)
    public static void realCreativeSupplyCompletesIntoMeWaitingStorage(GameTestHelper helper) throws Exception {
        var facility = facility(helper, 4, false);
        facility.plant.setParallel(256);
        var modified = modify(facility);
        helper.assertTrue(facility.unit.getLampCount() == 4 && modified != null &&
                modified.parallels == 256 && modified.duration == 40,
                "Four registered lamps must support 256 parallels");
        begin(helper, facility);
        work(facility, facility.logic().getDuration() - 1);
        helper.assertTrue(facility.produced() == 0 && !facility.unit.getUvState().complete(),
                "UV product must not appear before the final dose tick");
        work(facility, 1);
        facility.logic().onRecipeFinish();
        helper.assertTrue(facility.produced() == 256000,
                "Completed batch must be physically stored in the ME hatch waiting buffer, without an AE network" +
                        facility.diagnostics());
        facility.logic().onRecipeFinish();
        helper.assertTrue(facility.produced() == 256000, "Repeated completion duplicated the product");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "uvPurification", required = true)
    public static void disconnectedAndPowerStarvedTicksPreserveBatchDose(GameTestHelper helper) throws Exception {
        var facility = facility(helper, 1, false);
        facility.plant.setParallel(64);
        begin(helper, facility);
        work(facility, 3);
        var snapshot = facility.unit.getUvState().snapshot();
        int progress = facility.logic().getProgress();
        facility.unit.bindToPlant(null);
        for (int tick = 0; tick < 5; tick++) facility.logic().serverTick();
        helper.assertTrue(snapshot.equals(facility.unit.getUvState().snapshot()) &&
                facility.logic().getProgress() == progress && facility.produced() == 0,
                "Disconnected recipe ticks advanced dose, progress or output");
        facility.unit.bindToPlant(facility.plant.getPos());
        facility.unit.internalEnergy.changeEnergy(-facility.unit.getStoredLinkedEnergy());
        for (int tick = 0; tick < 8; tick++) facility.logic().handleRecipeWorking();
        helper.assertTrue(snapshot.equals(facility.unit.getUvState().snapshot()) && facility.logic().getProgress() == progress,
                "An unsuccessful EU tick must neither irradiate nor regress the batch");
        helper.assertTrue(!facility.logic().isSuspend(), "Repeated power failures must pause without latching suspension");
        work(facility, 1);
        helper.assertTrue(facility.logic().getProgress() == progress + 1 &&
                !snapshot.equals(facility.unit.getUvState().snapshot()),
                "Restored power did not resume dose accumulation; before=" + snapshot + facility.diagnostics());
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "uvPurification", required = true)
    public static void powerAndParallelChangesApplyOnlyToNextBatch(GameTestHelper helper) throws Exception {
        var facility = facility(helper, 1, false);
        facility.plant.setParallel(64);
        begin(helper, facility);
        var frozen = facility.unit.getUvState().plan();
        facility.unit.setPowerPercent(25);
        facility.plant.setParallel(32);
        helper.assertTrue(frozen.equals(facility.unit.getUvState().plan()) && facility.logic().getDuration() == 40,
                "Changing settings rewrote an in-flight plan");
        work(facility, 40);
        facility.logic().onRecipeFinish();
        helper.assertTrue(facility.produced() == 64000,
                "Changing settings changed the first batch's yield" + facility.diagnostics());
        var next = modify(facility);
        helper.assertTrue(next != null && next.parallels == 32 && next.duration == 80,
                "Next batch did not adopt 32 parallels and 25% lamp power");
        facility.logic().setupRecipe(next);
        helper.assertTrue(!frozen.equals(facility.unit.getUvState().plan()), "Next batch retained the previous frozen plan");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "uvPurification", required = true)
    public static void blockEntityNbtRestoresMidBatchDoseAndFinishesOnce(GameTestHelper helper) throws Exception {
        var facility = facility(helper, 1, false);
        facility.plant.setParallel(64);
        begin(helper, facility);
        work(facility, 7);
        var state = facility.unit.getUvState().snapshot();
        var holder = helper.getBlockEntity(UNIT);
        var saved = holder.saveWithFullMetadata();
        facility.unit.getUvState().clear();
        holder.load(saved);
        helper.assertTrue(state.equals(facility.unit.getUvState().snapshot()) && facility.logic().getProgress() == 7,
                "Block entity NBT did not restore the frozen plan, dose and recipe progress together; saved=" + state +
                        facility.diagnostics());
        work(facility, 33);
        facility.logic().onRecipeFinish();
        facility.logic().onRecipeFinish();
        helper.assertTrue(facility.produced() == 64000, "Reloaded batch lost or duplicated its output");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "uvPurification", required = true)
    public static void blockedOrdinaryOutputRetainsFinishedBatchWithoutExtraEnergy(GameTestHelper helper) throws Exception {
        // This test intentionally targets finite output capacity; other tests use real ME outputs.
        var facility = facility(helper, 1, true);
        begin(helper, facility);
        work(facility, 1);
        helper.assertTrue(facility.unit.getUvState().complete() && facility.logic().getProgress() < facility.logic().getDuration(),
                "A small batch should finish its dose before its chemical residence time");
        facility.logic().onRecipeFinish();
        helper.assertTrue(facility.produced() == 0 && facility.unit.getUvState().active(),
                "Full UV dose must not bypass the minimum chemical residence time");
        facility.output.setFluidInTank(0, new FluidStack(Fluids.LAVA, facility.output.getTankCapacity(0)));
        work(facility, facility.logic().getDuration() - facility.logic().getProgress());
        long stored = facility.unit.getStoredLinkedEnergy();
        var finished = facility.unit.getUvState().snapshot();
        facility.logic().onRecipeFinish();
        facility.logic().onRecipeFinish();
        helper.assertTrue(facility.produced() == 0 && finished.equals(facility.unit.getUvState().snapshot()) &&
                facility.unit.getStoredLinkedEnergy() == stored,
                "Blocked completion lost dose, leaked product or consumed additional power");
        facility.output.setFluidInTank(0, FluidStack.EMPTY);
        facility.logic().onRecipeFinish();
        facility.logic().onRecipeFinish();
        helper.assertTrue(facility.produced() == 1000, "Unblocking output did not release exactly one completed batch");
        helper.succeed();
    }

    private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
