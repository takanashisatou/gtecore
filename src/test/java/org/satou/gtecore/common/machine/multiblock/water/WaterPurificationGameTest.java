package org.satou.gtecore.common.machine.multiblock.water;

import org.satou.gtecore.common.data.machines.GTEWaterPurificationMachines;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationParts;
import org.satou.gtecore.common.data.machines.GTEMultiMachines2;
import org.satou.gtecore.common.data.GTEMaterials;
import org.satou.gtecore.common.data.GTERecipeTypes;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Real registered block entities and their data-stick/LDLib persistence paths. */
@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class WaterPurificationGameTest {

    private static final BlockPos FIRST_PLANT = new BlockPos(1, 1, 1);
    private static final BlockPos SECOND_PLANT = new BlockPos(3, 1, 1);
    private static final BlockPos UNIT = new BlockPos(1, 1, 3);

    private record Network(CentralPurificationPlantMachine first, CentralPurificationPlantMachine second,
                           LinkedPurificationUnitMachine unit) {}

    private static Network placeNetwork(GameTestHelper helper) {
        helper.setBlock(FIRST_PLANT, GTEWaterPurificationMachines.CENTRAL_WATER_PURIFICATION_PLANT.getBlock());
        helper.setBlock(SECOND_PLANT, GTEWaterPurificationMachines.CENTRAL_WATER_PURIFICATION_PLANT.getBlock());
        helper.setBlock(UNIT, GTEWaterPurificationMachines.T1_CLARIFIER_PURIFICATION_UNIT.getBlock());
        return new Network(
                (CentralPurificationPlantMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(FIRST_PLANT)),
                (CentralPurificationPlantMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(SECOND_PLANT)),
                (LinkedPurificationUnitMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(UNIT)));
    }

    @GameTest(template = "empty", batch = "waterPurification", required = true)
    public static void registeredStagesRejectOtherTiersAndLegacyMachineIsDisabled(GameTestHelper helper) {
        var definitions = List.of(GTEWaterPurificationMachines.T1_CLARIFIER_PURIFICATION_UNIT,
                GTEWaterPurificationMachines.T2_UV_OXIDATION_PURIFICATION_UNIT,
                GTEWaterPurificationMachines.T3_EDI_ULTRAPURE_PURIFICATION_UNIT);
        int[] tiers = { GTValues.EV, GTValues.LuV, GTValues.ZPM };
        var recipe = GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("stage_isolation_regression")
                .outputFluids(new FluidStack(Fluids.WATER, 1000)).duration(20).EUt(32).buildRawRecipe();
        for (int stage = 0; stage < definitions.size(); stage++) {
            BlockPos pos = new BlockPos(stage + 1, 1, 2);
            helper.setBlock(pos, definitions.get(stage).getBlock());
            var unit = (LinkedPurificationUnitMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(pos));
            var unitUi = unit.createUIWidget();
            helper.assertTrue(unitUi != null && unitUi.getSize().width > 0 && unitUi.getSize().height > 0,
                    "Stage UI must construct with positive dimensions on the dedicated server");
            for (int candidate = 0; candidate < tiers.length; candidate++) {
                recipe.data.putInt("waterPurificationTier", tiers[candidate]);
                helper.assertTrue(unit.acceptsPurificationRecipe(recipe) == (stage == candidate),
                        "Registered stage " + (stage + 1) + " accepted the wrong purification tier or rejected its own");
            }
            recipe.data.remove("waterPurificationTier");
            helper.assertTrue(!unit.acceptsPurificationRecipe(recipe), "Missing stage tag must be rejected");
        }
        helper.setBlock(FIRST_PLANT, GTEWaterPurificationMachines.CENTRAL_WATER_PURIFICATION_PLANT.getBlock());
        var plant = (CentralPurificationPlantMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(FIRST_PLANT));
        var plantUi = plant.createUIWidget();
        helper.assertTrue(plantUi != null && plantUi.getSize().width > 0 && plantUi.getSize().height > 0,
                "Plant UI must construct with positive dimensions on the dedicated server");
        var legacyTypes = GTEMultiMachines2.ULTRA_PURE_WATER_REFINERY.getRecipeTypes();
        helper.assertTrue(legacyTypes.length == 1 && legacyTypes[0] == GTRecipeTypes.DUMMY_RECIPES,
                "Legacy ultra-pure refinery must have only the dummy recipe type");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "waterPurification", required = true)
    public static void typedDataStickBindsAndRejectsForeignDimension(GameTestHelper helper) {
        Network network = placeNetwork(helper);
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var stick = GTItems.TOOL_DATA_STICK.asStack();
        network.first.onDataStickShiftUse(player, stick);
        helper.assertTrue(network.unit.onDataStickUse(player, stick) == InteractionResult.SUCCESS,
                "Copied same-dimension plant address was rejected");
        helper.assertTrue(network.first.hasUnit(network.unit.getPos()) &&
                network.first.getPos().equals(network.unit.getPlantPos()), "Binding was not bidirectional");

        network.unit.bindToPlant(null);
        stick.getOrCreateTag().getCompound("gteWaterPurificationLink")
                .putString("dimension", Level.NETHER.location().toString());
        helper.assertTrue(network.unit.onDataStickUse(player, stick) == InteractionResult.PASS,
                "Foreign dimension address must be rejected even with matching coordinates");
        helper.assertTrue(network.unit.getPlantPos() == null && !network.first.hasUnit(network.unit.getPos()),
                "Rejected address changed membership");

        network.unit.onDataStickShiftUse(player, stick);
        helper.assertTrue(network.unit.onDataStickUse(player, stick) == InteractionResult.PASS,
                "A unit address must not be accepted as a plant address");
        helper.assertTrue(network.first.onDataStickUse(player, stick) == InteractionResult.SUCCESS,
                "Plant must accept a correctly typed unit address");
        helper.assertTrue(network.first.hasUnit(network.unit.getPos()), "Reverse-direction data stick did not bind");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "waterPurification", required = true)
    public static void rebindAndDisconnectRemoveReverseMembership(GameTestHelper helper) {
        Network network = placeNetwork(helper);
        helper.assertTrue(network.unit.bindToPlant(network.first.getPos()), "Initial binding failed");
        helper.assertTrue(network.unit.bindToPlant(network.second.getPos()), "Rebinding failed");
        helper.assertTrue(!network.first.hasUnit(network.unit.getPos()) && network.second.hasUnit(network.unit.getPos()),
                "Rebinding left stale reverse membership at old plant");
        helper.assertTrue(network.second.getPos().equals(network.unit.getPlantPos()), "Forward address is stale");
        helper.assertTrue(network.unit.getPlant() == null, "An incomplete plant must not power the unit");
        network.second.disconnectAll();
        helper.assertTrue(network.unit.getPlantPos() == null && !network.second.hasUnit(network.unit.getPos()),
                "Disconnect-all did not clear both sides");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "waterPurification", required = true)
    public static void blockEntityNbtPreservesNetworkAndParallel(GameTestHelper helper) {
        Network network = placeNetwork(helper);
        network.unit.bindToPlant(network.first.getPos());
        network.first.setParallel(37);
        network.unit.internalEnergy.changeEnergy(12345);
        BlockEntity plantHolder = helper.getBlockEntity(FIRST_PLANT);
        BlockEntity unitHolder = helper.getBlockEntity(UNIT);
        CompoundTag plantNbt = plantHolder.saveWithFullMetadata();
        CompoundTag unitNbt = unitHolder.saveWithFullMetadata();

        network.unit.bindToPlant(null);
        network.first.setParallel(1);
        network.unit.internalEnergy.changeEnergy(-network.unit.getStoredLinkedEnergy());
        plantHolder.load(plantNbt);
        unitHolder.load(unitNbt);
        helper.assertTrue(network.first.hasUnit(network.unit.getPos()), "Plant membership was lost in block entity NBT");
        helper.assertTrue(network.first.getPos().equals(network.unit.getPlantPos()), "Unit address was lost in managed NBT");
        helper.assertTrue(network.first.getParallel() == 37, "Plant parallel setting was lost in managed NBT");
        helper.assertTrue(network.unit.getStoredLinkedEnergy() == 12345, "Internal energy was lost in managed NBT");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "waterPurification", required = true)
    public static void linkedPowerConservesEnergyAndSkipsUnaffordableUnit(GameTestHelper helper) throws Exception {
        Network network = placeNetwork(helper);
        helper.setBlock(UNIT, GTEWaterPurificationMachines.T1_CLARIFIER_PURIFICATION_UNIT.getBlock());
        var cheap = (LinkedPurificationUnitMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(UNIT));
        BlockPos costlyPos = new BlockPos(0, 1, 3);
        helper.setBlock(costlyPos, GTEWaterPurificationMachines.T3_EDI_ULTRAPURE_PURIFICATION_UNIT.getBlock());
        var costly = (LinkedPurificationUnitMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(costlyPos));
        setField(MultiblockControllerMachine.class, network.first, "isFormed", true);
        setField(MultiblockControllerMachine.class, network.second, "isFormed", true);
        setField(MultiblockControllerMachine.class, cheap, "isFormed", true);
        setField(MultiblockControllerMachine.class, costly, "isFormed", true);
        cheap.bindToPlant(network.first.getPos());
        costly.bindToPlant(network.first.getPos());
        var source = new NotifiableEnergyContainer(network.first, 10000000, costly.getLinkVoltage(), 256, 0, 0);
        setField(WorkableElectricMultiblockMachine.class, network.first, "energyContainer", new EnergyContainerList(List.of(source)));
        var transfer = CentralPurificationPlantMachine.class.getDeclaredMethod("transferEnergyToUnits");
        transfer.setAccessible(true);

        source.changeEnergy(cheap.getLinkVoltage() - 1);
        helper.assertTrue((long) transfer.invoke(network.first) == 0 && cheap.getStoredLinkedEnergy() == 0 &&
                costly.getStoredLinkedEnergy() == 0 && source.getEnergyStored() == cheap.getLinkVoltage() - 1,
                "Less than one packet must neither debit the source nor credit a unit");
        source.changeEnergy(1);
        // Packed coordinates sort the lower-x costly unit before the cheaper one.
        setField(CentralPurificationPlantMachine.class, network.first, "nextUnitIndex", 0);
        helper.assertTrue((long) transfer.invoke(network.first) == cheap.getLinkVoltage() &&
                source.getEnergyStored() == 0 && cheap.getStoredLinkedEnergy() == cheap.getLinkVoltage() &&
                costly.getStoredLinkedEnergy() == 0, "Unaffordable first unit must not starve affordable later unit");

        source.changeEnergy(costly.getLinkVoltage() * 4 + cheap.getLinkVoltage() * 2);
        long beforeSource = source.getEnergyStored();
        long beforeUnits = cheap.getStoredLinkedEnergy() + costly.getStoredLinkedEnergy();
        long moved = (long) transfer.invoke(network.first);
        long received = cheap.getStoredLinkedEnergy() + costly.getStoredLinkedEnergy() - beforeUnits;
        helper.assertTrue(moved > 0 && moved == received && moved == beforeSource - source.getEnergyStored(),
                "Actual source debit must exactly equal total actual unit credit");
        cheap.bindToPlant(network.second.getPos());
        long stored = cheap.getStoredLinkedEnergy();
        helper.assertTrue(cheap.acceptLinkedEnergy(network.first, 1) == 0 && cheap.getStoredLinkedEnergy() == stored,
                "Old plant retained authority to charge a rebound unit");

        var tank = new NotifiableFluidTank(cheap, 1, 16000, IO.OUT);
        cheap.addHandlerList(RecipeHandlerList.of(IO.OUT, List.of(tank)));
        var recipe = GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("unlinked_completion_regression")
                .outputFluids(new FluidStack(Fluids.WATER, 1000)).duration(1).EUt(32).buildRawRecipe();
        var logic = cheap.getRecipeLogic();
        setField(RecipeLogic.class, logic, "lastRecipe", recipe);
        setField(RecipeLogic.class, logic, "progress", 1);
        setField(RecipeLogic.class, logic, "duration", 1);
        logic.setStatus(RecipeLogic.Status.WORKING);
        cheap.bindToPlant(null);
        logic.serverTick();
        helper.assertTrue(cheap.getStoredLinkedEnergy() == stored && tank.getFluidInTank(0).isEmpty() &&
                logic.getProgress() == 1, "Unlinked logic tick consumed EU, advanced progress, or emitted output");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "waterPurification", required = true)
    public static void breakingStructureResetsStabilityWithoutWashingFaultOrGrace(GameTestHelper helper) {
        var unit = (ThermalPurificationUnitMachine) placeNetwork(helper).unit;
        var state = unit.getThermalState();
        state.restore(new PurificationThermalState.Snapshot(800, 65, 333, 299, 6000, false, 17));
        unit.onStructureInvalid();
        helper.assertTrue(state.stableTicks() == 0 && state.phaseTicks() == 333 && state.outOfRangeTicks() == 299,
                "Dismantling must reset stability without refreshing target or grace timers");
        state.restore(new PurificationThermalState.Snapshot(800, 65, 334, 301, 0, true, 0));
        unit.onStructureInvalid();
        helper.assertTrue(state.isFaulted() && state.phaseTicks() == 334 && state.outOfRangeTicks() == 301,
                "Dismantling must not clear a latched maintenance fault");
        helper.succeed();
    }

    private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @GameTest(template = "empty", batch = "waterPurification", required = true)
    public static void thermalHatchReadsRealRedstone(GameTestHelper helper) {
        helper.setBlock(UNIT, GTEWaterPurificationParts.THERMAL_CONTROL_HATCH.getBlock());
        var hatch = (ThermalControlHatchPartMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(UNIT));
        helper.assertTrue(!hatch.isHeating() && hatch.getRedstoneStrength() == 0, "Unpowered hatch must cool");
        helper.setBlock(UNIT.above(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(hatch.isHeating() && hatch.getRedstoneStrength() == 15, "Powered hatch must heat");
        helper.setBlock(UNIT.above(), Blocks.AIR);
        helper.assertTrue(!hatch.isHeating(), "Removing redstone must switch back to cooling");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "waterPurification", required = true)
    public static void thermalFaultDamagesRealMaintenanceAndSurvivesNbt(GameTestHelper helper) throws Exception {
        Network network = placeNetwork(helper);
        var unit = (ThermalPurificationUnitMachine) network.unit;
        BlockPos maintenancePos = new BlockPos(3, 1, 3);
        helper.setBlock(maintenancePos, GTMachines.MAINTENANCE_HATCH.getBlock());
        var maintenance = (IMaintenanceMachine) MetaMachine.getMachine(helper.getLevel(), helper.absolutePos(maintenancePos));
        // Inject only structure membership; thermal failure and GTM maintenance remain real code.
        var partPositions = MultiblockControllerMachine.class.getDeclaredField("partPositions");
        partPositions.setAccessible(true);
        partPositions.set(unit, new BlockPos[] { helper.absolutePos(maintenancePos) });
        maintenance.setMaintenanceProblems(IMaintenanceMachine.NO_PROBLEMS);
        maintenance.setTaped(true);
        var advance = ThermalPurificationUnitMachine.class.getDeclaredMethod("advanceThermal", boolean.class);
        advance.setAccessible(true);
        var repair = ThermalPurificationUnitMachine.class.getDeclaredMethod("tryFinishRepair");
        repair.setAccessible(true);
        boolean maintenanceEnabled = ConfigHolder.INSTANCE.machines.enableMaintenance;
        ConfigHolder.INSTANCE.machines.enableMaintenance = true;
        try {
            unit.getThermalState().restore(new PurificationThermalState.Snapshot(800, 65, 0, 300, 900, false, 0));
            advance.invoke(unit, true);
            helper.assertTrue(unit.getThermalState().isFaulted(), "Expired thermal grace did not latch fault");
            helper.assertTrue(maintenance.hasMaintenanceProblems() && !maintenance.isTaped(),
                    "Thermal failure did not damage the ordinary GTM maintenance hatch");
            var savedFault = unit.getThermalState().snapshot();
            BlockEntity holder = helper.getBlockEntity(UNIT);
            CompoundTag savedNbt = holder.saveWithFullMetadata();
            unit.getThermalState().clearFault();
            holder.load(savedNbt);
            helper.assertTrue(unit.getThermalState().snapshot().equals(savedFault), "Fault/timers lost across machine NBT");
            maintenance.setMaintenanceProblems(IMaintenanceMachine.NO_PROBLEMS);
            repair.invoke(unit);
            helper.assertTrue(unit.getThermalState().isFaulted(), "Repair cleared fault before temperature recovered");
            maintenance.setMaintenanceProblems(IMaintenanceMachine.ALL_PROBLEMS);
            for (int tick = 0; tick < 150; tick++) advance.invoke(unit, false);
            repair.invoke(unit);
            helper.assertTrue(unit.getThermalState().isInRange() && unit.getThermalState().isFaulted(),
                    "Cooling alone must not bypass GTM maintenance");
            for (int tool = 0; tool < 6; tool++) maintenance.setMaintenanceFixed(tool);
            repair.invoke(unit);
            helper.assertTrue(!unit.getThermalState().isFaulted() && unit.getThermalState().stableTicks() == 0,
                    "Successful GTM maintenance at safe temperature did not reset the fault");
        } finally {
            ConfigHolder.INSTANCE.machines.enableMaintenance = maintenanceEnabled;
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "waterPurification", required = true)
    public static void completionYieldScalesOnlyPurifiedWaterAndKeepsCachedRecipe(GameTestHelper helper) {
        Network network = placeNetwork(helper);
        var unit = (ThermalPurificationUnitMachine) network.unit;
        var tank = new NotifiableFluidTank(unit, 2, 16000, IO.OUT);
        unit.addHandlerList(RecipeHandlerList.of(IO.OUT, List.of(tank)));
        var recipe = GTERecipeTypes.WATER_PURIFICATION_RECIPES.recipeBuilder("thermal_yield_regression")
                .outputFluids(GTEMaterials.DistilledPurifiedWater.getFluid(1000))
                .outputFluids(new FluidStack(Fluids.WATER, 200))
                .duration(20).EUt(32).buildRawRecipe();
        var logic = (ThermalPurificationUnitMachine.ThermalRecipeLogic) unit.getRecipeLogic();
        unit.getThermalState().restore(new PurificationThermalState.Snapshot(650, 65, 0, 0, 6000, false, 0));
        helper.assertTrue(logic.handleRecipeIO(recipe, IO.OUT).isSuccess(), "Half-yield output failed");
        helper.assertTrue(fluidAmount(tank, GTEMaterials.DistilledPurifiedWater.getFluid(1)) == 500,
                "50% completion efficiency must produce 500 mB");
        helper.assertTrue(fluidAmount(tank, new FluidStack(Fluids.WATER, 1)) == 200, "By-product was incorrectly scaled");
        var cached = (FluidIngredient) recipe.outputs.get(FluidRecipeCapability.CAP).get(0).getContent();
        helper.assertTrue(cached.getAmount() == 1000, "Cached recipe output was mutated");
        unit.getThermalState().restore(new PurificationThermalState.Snapshot(650, 65, 0, 0, 12000, false, 0));
        helper.assertTrue(logic.handleRecipeIO(recipe, IO.OUT).isSuccess(), "Full-yield output failed");
        helper.assertTrue(fluidAmount(tank, GTEMaterials.DistilledPurifiedWater.getFluid(1)) == 1500,
                "Second completion must resample current efficiency, not reuse first batch yield");
        helper.assertTrue(cached.getAmount() == 1000, "Second completion mutated cached recipe");
        helper.succeed();
    }

    private static int fluidAmount(NotifiableFluidTank tank, FluidStack fluid) {
        int amount = 0;
        for (int index = 0; index < tank.getTanks(); index++) {
            if (tank.getFluidInTank(index).isFluidEqual(fluid)) amount += tank.getFluidInTank(index).getAmount();
        }
        return amount;
    }
}
