package org.satou.gtecore.common.machine.multiblock.water;

import org.satou.gtecore.common.data.GTEMaterials;
import org.satou.gtecore.common.data.items.GTEItems;
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
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEOutputHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeEnergyHatchPartMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputHatchPartMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputBusPartMachine;
import com.hepdd.gtmthings.data.CreativeMachines;

import appeng.api.stacks.AEFluidKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Registered machines and real recipe IO; only structure membership is injected. */
@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class EdiPurificationGameTest {
    private static final BlockPos PLANT = new BlockPos(0, 1, 0);
    private static final BlockPos UNIT = new BlockPos(1, 1, 0);
    private static final BlockPos INPUT = new BlockPos(2, 1, 0);
    private static final BlockPos OUTPUT = new BlockPos(3, 1, 0);
    private static final BlockPos ENERGY = new BlockPos(4, 1, 0);
    private static final BlockPos CONTROL = new BlockPos(1, 1, 2);
    private static final BlockPos CONTROL_POWER = CONTROL.above();

    private record Facility(EdiPurificationUnitMachine unit, CentralPurificationPlantMachine plant,
                            CreativeInputHatchPartMachine input, NotifiableFluidTank output,
                            KeyStorage waitingBuffer) {
        EdiPurificationUnitMachine.EdiRecipeLogic logic() {
            return (EdiPurificationUnitMachine.EdiRecipeLogic) unit.getRecipeLogic();
        }

        long produced() {
            if (waitingBuffer != null) {
                var fluid = GTEMaterials.UltrapureWater.getFluid(1);
                return waitingBuffer.storage.getOrDefault(AEFluidKey.of(fluid.getFluid(), fluid.getTag()), 0L);
            }
            long result = 0;
            for (int slot = 0; slot < output.getTanks(); slot++) {
                if (output.getFluidInTank(slot).isFluidEqual(GTEMaterials.UltrapureWater.getFluid(1))) {
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
                    ", state=" + unit.getEdiState().snapshot() + ", output=" + produced() + "]";
        }
    }

    private static Facility facility(GameTestHelper helper, boolean finiteOutput) throws Exception {
        helper.setBlock(PLANT, GTEWaterPurificationMachines.CENTRAL_WATER_PURIFICATION_PLANT.getBlock());
        helper.setBlock(UNIT, GTEWaterPurificationMachines.T3_EDI_ULTRAPURE_PURIFICATION_UNIT.getBlock());
        helper.setBlock(INPUT, CreativeMachines.CREATIVE_FLUID_INPUT_HATCH.getBlock());
        helper.setBlock(ENERGY, CreativeMachines.CREATIVE_ENERGY_INPUT_HATCH.getBlock());
        helper.setBlock(OUTPUT, finiteOutput ? GTMachines.FLUID_EXPORT_HATCH[GTValues.LV].getBlock() :
                GTAEMachines.FLUID_EXPORT_HATCH_ME.getBlock());
        var unit = (EdiPurificationUnitMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(UNIT));
        var plant = (CentralPurificationPlantMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(PLANT));
        var input = (CreativeInputHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(INPUT));
        var energy = (CreativeEnergyHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(ENERGY));
        var output = (FluidHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(OUTPUT));
        var configureFluid = CreativeInputHatchPartMachine.class.getDeclaredMethod("setFluid", int.class, FluidStack.class);
        configureFluid.setAccessible(true);
        configureFluid.invoke(input, 0, GTEMaterials.UvPurifiedWater.getFluid(1));
        input.tank.setFluidInTank(0, GTEMaterials.UvPurifiedWater.getFluid(Integer.MAX_VALUE));

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
        helper.setBlock(CONTROL, GTEWaterPurificationParts.EDI_REGENERATION_CONTROL_HATCH.getBlock());
        setField(MultiblockControllerMachine.class, unit, "partPositions",
                new BlockPos[]{helper.absolutePos(CONTROL), helper.absolutePos(INPUT), helper.absolutePos(OUTPUT)});
        unit.addHandlerList(RecipeHandlerList.of(IO.IN, List.of(input.tank, unit.internalEnergy)));
        unit.addHandlerList(RecipeHandlerList.of(IO.OUT, List.of(output.tank)));
        unit.bindToPlant(plant.getPos());
        plant.setParallel(1);
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

    // Synthetic inputs isolate controller bookkeeping; registered chemistry is covered by the structure test.
    private static GTRecipe recipe(boolean regeneration) {
        var builder = GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder(
                regeneration ? "edi_integration_regeneration" : "edi_integration_production")
                .inputFluids(GTEMaterials.UvPurifiedWater.getFluid(regeneration ? 100 : 800))
                .duration(regeneration ? 2 : 30).EUt(GTValues.V[GTValues.UEV]);
        if (!regeneration) builder.outputFluids(GTEMaterials.UltrapureWater.getFluid(800));
        var recipe = builder.buildRawRecipe();
        recipe.data.putInt("waterPurificationTier", GTValues.ZPM);
        recipe.data.putInt("ediLoad", 800);
        recipe.data.putBoolean("ediRegeneration", regeneration);
        return recipe;
    }

    private static GTRecipe modify(Facility facility, boolean regeneration) {
        var origin = recipe(regeneration);
        return EdiPurificationUnitMachine.recipeModifier(facility.unit, origin).apply(origin);
    }

    private static void requestRegeneration(GameTestHelper helper, Facility facility, boolean requested) {
        helper.setBlock(CONTROL_POWER, requested ? Blocks.REDSTONE_BLOCK : Blocks.AIR);
        helper.assertTrue(facility.unit.isRegenerationRequested() == requested,
                "Actual neighboring redstone must select the requested EDI mode");
    }

    private static void begin(GameTestHelper helper, Facility facility, boolean regeneration) {
        var modified = modify(facility, regeneration);
        helper.assertTrue(modified != null, "Selected EDI recipe must be runnable" + facility.diagnostics());
        facility.logic().setupRecipe(modified);
        helper.assertTrue(facility.logic().isWorking(), "Successful EDI input must start a batch" + facility.diagnostics());
    }

    private static void work(Facility facility, int ticks) throws Exception {
        for (int tick = 0; tick < ticks; tick++) {
            facility.charge();
            facility.logic().handleRecipeWorking();
        }
    }

    private static void complete(Facility facility) throws Exception {
        work(facility, facility.logic().getDuration() - facility.logic().getProgress());
        facility.logic().onRecipeFinish();
    }

    @GameTest(template = "empty", batch = "ediPurification", required = true)
    public static void stageIsolationAndPoweredCleanIdleRejectWrongRecipes(GameTestHelper helper) throws Exception {
        var facility = facility(helper, false);
        helper.assertTrue(facility.unit.getUnitTier() == GTValues.ZPM &&
                facility.unit.getLinkVoltage() == GTValues.V[GTValues.UEV],
                "Stage identity must remain T3 while operating at UEV");
        for (int tier : new int[]{GTValues.EV, GTValues.LuV}) {
            var foreign = recipe(false);
            foreign.data.putInt("waterPurificationTier", tier);
            helper.assertTrue(EdiPurificationUnitMachine.recipeModifier(facility.unit, foreign).apply(foreign) == null,
                    "EDI accepted another purification stage " + tier);
        }
        helper.assertTrue(modify(facility, false) != null && modify(facility, true) == null,
                "Unpowered EDI must select production only");
        requestRegeneration(helper, facility, true);
        long input = facility.input.tank.getFluidInTank(0).getAmount();
        long energy = facility.unit.getStoredLinkedEnergy();
        for (int tick = 0; tick < 5; tick++) {
            helper.assertTrue(modify(facility, false) == null && modify(facility, true) == null,
                    "Held regeneration signal at zero load must keep both recipes idle");
            facility.logic().serverTick();
        }
        helper.assertTrue(facility.input.tank.getFluidInTank(0).getAmount() == input &&
                facility.unit.getStoredLinkedEnergy() == energy && facility.produced() == 0,
                "Clean idle consumed inputs/energy or emitted water");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "ediPurification", required = true)
    public static void loadCommitsWithActualMeStoredProductExactlyOnce(GameTestHelper helper) throws Exception {
        var facility = facility(helper, false);
        facility.plant.setParallel(256);
        var modified = modify(facility, false);
        helper.assertTrue(modified != null && modified.parallels == 256 && modified.duration == 30,
                "UEV link must support the requested 256 production parallels");
        begin(helper, facility, false);
        work(facility, 29);
        helper.assertTrue(facility.unit.getEdiState().load() == 0 && facility.produced() == 0,
                "In-flight production must not accrue membrane load or release water");
        facility.logic().onRecipeFinish();
        helper.assertTrue(facility.unit.getEdiState().load() == 0 && facility.produced() == 0,
                "Early completion call bypassed residence time");
        work(facility, 1);
        facility.logic().onRecipeFinish();
        facility.logic().onRecipeFinish();
        helper.assertTrue(facility.produced() == 204800 && facility.unit.getEdiState().load() == 204800,
                "Exactly one completed batch must enter actual ME waiting storage and membrane load" +
                        facility.diagnostics());
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "ediPurification", required = true)
    public static void blockedOutputDefersLoadUntilProductCanBeCommitted(GameTestHelper helper) throws Exception {
        // Finite output is intentional here: this test exercises native output backpressure.
        var facility = facility(helper, true);
        begin(helper, facility, false);
        facility.output.setFluidInTank(0, new FluidStack(Fluids.LAVA, facility.output.getTankCapacity(0)));
        work(facility, 30);
        long energy = facility.unit.getStoredLinkedEnergy();
        facility.logic().onRecipeFinish();
        facility.logic().onRecipeFinish();
        helper.assertTrue(facility.unit.getEdiState().load() == 0 && facility.produced() == 0 &&
                facility.unit.getStoredLinkedEnergy() == energy,
                "Blocked completion committed load/product or charged additional EU" + facility.diagnostics());
        facility.output.setFluidInTank(0, FluidStack.EMPTY);
        facility.logic().onRecipeFinish();
        facility.logic().onRecipeFinish();
        helper.assertTrue(facility.unit.getEdiState().load() == 800 && facility.produced() == 800,
                "Unblocked output must commit exactly one 800 mB batch" + facility.diagnostics());
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "ediPurification", required = true)
    public static void fullCapacityStopsProductionAndPaidRegenerationClearsTail(GameTestHelper helper) throws Exception {
        var facility = facility(helper, false);
        facility.plant.setParallel(256);
        facility.unit.getEdiState().produced(PurificationEdiState.CAPACITY - 1600);
        var capped = modify(facility, false);
        helper.assertTrue(capped != null && capped.parallels == 2,
                "Remaining capacity must cap a large production request to two parallels");
        begin(helper, facility, false);
        complete(facility);
        helper.assertTrue(facility.unit.getEdiState().load() == PurificationEdiState.CAPACITY &&
                facility.unit.getEdiState().signal() == 15 && modify(facility, false) == null,
                "Full membranes must stop production at the exact capacity");
        requestRegeneration(helper, facility, true);
        helper.assertTrue(modify(facility, true) != null, "Full membranes must permit paid regeneration");
        begin(helper, facility, true);
        complete(facility);
        helper.assertTrue(facility.unit.getEdiState().load() == PurificationEdiState.CAPACITY - 204800 &&
                facility.produced() == 1600, "Regeneration must reduce load without generating product");
        facility.unit.getEdiState().restore(new PurificationEdiState.Snapshot(801));
        var tail = modify(facility, true);
        helper.assertTrue(tail != null && tail.parallels == 2,
                "801 mB remaining load must require two paid regeneration parallels");
        begin(helper, facility, true);
        helper.assertTrue(facility.unit.getEdiState().load() == 801, "Starting regeneration cleared load prematurely");
        complete(facility);
        facility.logic().onRecipeFinish();
        helper.assertTrue(facility.unit.getEdiState().load() == 0 && modify(facility, true) == null &&
                facility.produced() == 1600, "Paid tail must clamp to zero once and remain idle while powered");
        requestRegeneration(helper, facility, false);
        helper.assertTrue(modify(facility, false) != null, "Cleared membranes must resume production after signal removal");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "ediPurification", required = true)
    public static void redstoneChangesSelectNextBatchWithoutRewritingActiveBatch(GameTestHelper helper) throws Exception {
        var facility = facility(helper, false);
        facility.plant.setParallel(4);
        begin(helper, facility, false);
        work(facility, 7);
        requestRegeneration(helper, facility, true);
        facility.plant.setParallel(1);
        complete(facility);
        helper.assertTrue(facility.produced() == 3200 && facility.unit.getEdiState().load() == 3200,
                "Mode and parallel changes rewrote in-flight production" + facility.diagnostics());
        helper.assertTrue(modify(facility, false) == null, "Next batch ignored the regeneration request");
        begin(helper, facility, true);
        work(facility, 1);
        requestRegeneration(helper, facility, false);
        complete(facility);
        helper.assertTrue(facility.unit.getEdiState().load() == 2400 && facility.produced() == 3200,
                "Removing redstone changed the active regeneration batch" + facility.diagnostics());
        helper.assertTrue(modify(facility, true) == null && modify(facility, false) != null,
                "Next batch failed to return to production mode");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "ediPurification", required = true)
    public static void pauseAndBlockEntityReloadPreserveLoadAndPendingBatch(GameTestHelper helper) throws Exception {
        var facility = facility(helper, false);
        facility.unit.getEdiState().produced(1600);
        begin(helper, facility, false);
        work(facility, 7);
        facility.unit.bindToPlant(null);
        for (int tick = 0; tick < 5; tick++) facility.logic().serverTick();
        helper.assertTrue(facility.logic().getProgress() == 7 && facility.unit.getEdiState().load() == 1600,
                "Disconnected ticks advanced progress or modified membrane load");
        facility.unit.bindToPlant(facility.plant.getPos());
        facility.unit.internalEnergy.changeEnergy(-facility.unit.getStoredLinkedEnergy());
        for (int tick = 0; tick < 8; tick++) facility.logic().handleRecipeWorking();
        helper.assertTrue(facility.logic().getProgress() == 7 && !facility.logic().isSuspend() &&
                facility.unit.getEdiState().load() == 1600 && facility.produced() == 0,
                "Power starvation must pause progress without latching suspension or altering load");
        var holder = helper.getBlockEntity(UNIT);
        var saved = holder.saveWithFullMetadata();
        facility.unit.getEdiState().regenerate(1600);
        holder.load(saved);
        helper.assertTrue(facility.unit.getEdiState().load() == 1600 && facility.logic().getProgress() == 7,
                "Block entity NBT must restore committed load and in-flight progress together" + facility.diagnostics());
        complete(facility);
        facility.logic().onRecipeFinish();
        helper.assertTrue(facility.unit.getEdiState().load() == 2400 && facility.produced() == 800,
                "Reloaded production lost or duplicated its output/load" + facility.diagnostics());
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "ediPurification", required = true)
    public static void structureInvalidationDiscardsPendingBatchButKeepsMembraneLoad(GameTestHelper helper) throws Exception {
        var facility = facility(helper, false);
        facility.unit.getEdiState().produced(1600);
        begin(helper, facility, false);
        work(facility, 7);
        facility.unit.onPartUnload();
        helper.assertTrue(facility.unit.getEdiState().load() == 1600, "Part unload erased committed membrane load");
        facility.unit.onStructureInvalid();
        facility.logic().onRecipeFinish();
        helper.assertTrue(facility.unit.getEdiState().load() == 1600 && facility.produced() == 0 &&
                facility.logic().getLastRecipe() == null,
                "Invalidating the structure must discard pending production without resetting load" + facility.diagnostics());
        var holder = helper.getBlockEntity(UNIT);
        var saved = holder.saveWithFullMetadata();
        facility.unit.getEdiState().regenerate(1600);
        holder.load(saved);
        helper.assertTrue(facility.unit.getEdiState().load() == 1600,
                "An invalidated controller must retain its membrane load across NBT saves");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "ediPurification", required = true)
    public static void registeredRecipeSearchSelectsBothModesWithEveryIngredientPresent(GameTestHelper helper) throws Exception {
        var facility = facility(helper, false);
        var reagentPos = new BlockPos(2, 1, 2);
        var resinPos = new BlockPos(3, 1, 2);
        helper.setBlock(reagentPos, CreativeMachines.CREATIVE_FLUID_INPUT_HATCH.getBlock());
        helper.setBlock(resinPos, CreativeMachines.CREATIVE_ITEM_INPUT_BUS.getBlock());
        var reagent = (CreativeInputHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(reagentPos));
        var resin = (CreativeInputBusPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(resinPos));
        setField(MultiblockControllerMachine.class, facility.unit, "partPositions",
                new BlockPos[]{helper.absolutePos(CONTROL), helper.absolutePos(INPUT), helper.absolutePos(OUTPUT),
                        helper.absolutePos(reagentPos), helper.absolutePos(resinPos)});
        // Reuse the command's real phantom configuration and refill subscription, not just live inventories.
        var inputs = Class.forName("org.satou.gtecore.common.command.CreativeTestInputs");
        var configureFluid = inputs.getDeclaredMethod("fluid", CreativeInputHatchPartMachine.class, FluidStack.class);
        configureFluid.setAccessible(true);
        configureFluid.invoke(null, reagent, GTEMaterials.ElectronicAcidBaseReagent.getFluid(1));
        var configureItem = inputs.getDeclaredMethod("item", CreativeInputBusPartMachine.class, net.minecraft.world.item.ItemStack.class);
        configureItem.setAccessible(true);
        configureItem.invoke(null, resin, GTEItems.MIXED_BED_RESIN_BEADS.asStack());
        facility.unit.addHandlerList(RecipeHandlerList.of(IO.IN, List.of(reagent.tank, resin.getInventory())));
        facility.logic().findAndHandleRecipe();
        var production = facility.logic().getLastRecipe();
        helper.assertTrue(production != null && production.id.getPath().endsWith("/ultrapure_water") &&
                !production.data.getBoolean("ediRegeneration") && facility.logic().isWorking(),
                "Real recipe search must select production with every ingredient present; selected=" +
                        (production == null ? "none" : production.id) + facility.diagnostics());
        facility.logic().resetRecipeLogic();
        facility.unit.getEdiState().produced(8000);
        requestRegeneration(helper, facility, true);
        facility.logic().findAndHandleRecipe();
        var regeneration = facility.logic().getLastRecipe();
        helper.assertTrue(regeneration != null && regeneration.id.getPath().endsWith("/ultrapure_water_regeneration") &&
                regeneration.data.getBoolean("ediRegeneration") && regeneration.outputs.isEmpty() &&
                regeneration.tickOutputs.isEmpty() && facility.logic().isWorking(),
                "Same-input production must not shadow the registered regeneration recipe; selected=" +
                        (regeneration == null ? "none" : regeneration.id) + facility.diagnostics());
        complete(facility);
        helper.assertTrue(facility.unit.getEdiState().load() == 7200 && facility.produced() == 0,
                "Registered regeneration must clean one paid batch without making T3 water" + facility.diagnostics());
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "ediPurification", required = true)
    public static void bothHatchAssemblyRecipesRemainInTheIngredientIndex(GameTestHelper helper) {
        var assembler = GTRecipeTypes.ASSEMBLER_RECIPES;
        var registered = helper.getLevel().getRecipeManager().getAllRecipesFor(assembler).stream()
                .map(recipe -> recipe.id).collect(java.util.stream.Collectors.toSet());
        var indexed = assembler.getLookup().getLookup().getRecipes(false)
                .map(recipe -> recipe.id).collect(java.util.stream.Collectors.toSet());
        for (String name : List.of("edi_load_signal_hatch", "edi_regeneration_control_hatch")) {
            var id = new net.minecraft.resources.ResourceLocation("gtceu", "assembler/" + name);
            helper.assertTrue(registered.contains(id) && indexed.contains(id),
                    "Both distinct hatch recipes must survive registration and ingredient indexing: " + id);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "ediPurification", required = true)
    public static void registeredDetectorExposesRawAnalogLoadOnlyOnItsOutwardFace(GameTestHelper helper) throws Exception {
        var facility = facility(helper, false);
        var pos = new BlockPos(3, 1, 2);
        helper.setBlock(pos, GTEWaterPurificationParts.EDI_LOAD_SIGNAL_HATCH.getBlock());
        var sensor = (EdiLoadSignalHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(pos));
        sensor.addedToController(facility.unit);
        facility.unit.getEdiState().produced(800);
        sensor.refreshSignal();
        helper.assertTrue(sensor.getAnalogOutputSignal() == 0 && facility.unit.getEdiState().load() == 800,
                "Small membrane load must remain stored even below the first analog step");
        facility.unit.getEdiState().produced(799200);
        sensor.refreshSignal();
        var state = helper.getLevel().getBlockState(helper.absolutePos(pos));
        helper.assertTrue(state.hasAnalogOutputSignal() && sensor.getAnalogOutputSignal() == 10 &&
                state.getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(pos)) == 10,
                "Registered block comparator output must expose raw load strength 10");
        for (var side : Direction.values()) {
            helper.assertTrue(sensor.getOutputSignal(side) == (side == sensor.getFrontFacing().getOpposite() ? 10 : 0) &&
                    sensor.getOutputDirectSignal(side) == 0,
                    "Load detector must weakly power only its outward face, never strongly power the casing");
        }
        sensor.removedFromController(facility.unit);
        helper.assertTrue(sensor.getAnalogOutputSignal() == 0 && facility.unit.getEdiState().load() == 800000,
                "Removing the detector must clear its output without erasing the controller load");
        helper.succeed();
    }

    private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
