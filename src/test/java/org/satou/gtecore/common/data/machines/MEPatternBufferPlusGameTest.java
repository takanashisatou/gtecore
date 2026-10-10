package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Real registered blocks, LDLib persistence, recipe IO and mirror subscriptions. */
@PrefixGameTestTemplate(false)
@GameTestHolder("gtecore")
public class MEPatternBufferPlusGameTest {

    private static MEPatternBufferPlusPartMachine buffer(GameTestHelper helper, int x) {
        var pos = helper.absolutePos(new BlockPos(x, 2, 0));
        helper.getLevel().removeBlock(pos, false);
        helper.getLevel().setBlockAndUpdate(pos, GTEMachines.ME_PATTERN_BUFFER_PLUS.getBlock().defaultBlockState());
        return (MEPatternBufferPlusPartMachine) MetaMachine.getMachine(helper.getLevel(), pos);
    }

    private static CompoundTag contents(long items, long fluid) {
        var tag = new CompoundTag();
        if (items > 0) {
            var entries = new ListTag();
            var stack = new ItemStack(Items.COBBLESTONE).serializeNBT();
            stack.putLong("real", items);
            entries.add(stack);
            tag.put("inventory", entries);
        }
        if (fluid > 0) {
            var entries = new ListTag();
            var stack = new FluidStack(Fluids.WATER, 1).writeToNBT(new CompoundTag());
            stack.putLong("real", fluid);
            entries.add(stack);
            tag.put("fluidInventory", entries);
        }
        return tag;
    }

    private static ItemStack pattern() {
        return PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.COBBLESTONE), 1) },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.STONE), 1) });
    }

    @GameTest(template = "empty", batch = "mePlus", required = true)
    public static void persistedCallbackDoesNotReenterSlot(GameTestHelper helper) {
        var machine = buffer(helper, 0);
        var slot = machine.getInternalInventory()[0];
        var handlers = (InternalSlotRecipeHandlerPlus.SlotRHL) machine.getInternalRecipeHandler().getSlotHandlers().get(0);
        int[] calls = new int[3];
        handlers.getItemRecipeHandler().addChangedListener(() -> calls[0]++);
        handlers.getFluidRecipeHandler().addChangedListener(() -> calls[1]++);
        // ReadonlyRef.replaceHandler uses exactly this capture-and-replace protocol.
        Runnable previous = slot.getOnContentsChanged();
        slot.setOnContentsChanged(() -> {
            calls[2]++;
            previous.run();
        });
        try {
            slot.deserializeNBT(contents(5, 1000));
        } catch (StackOverflowError error) {
            helper.fail("LDLib's persisted-field callback recursively reentered the slot");
            return;
        }
        helper.assertTrue(calls[0] == 1 && calls[1] == 1 && calls[2] == 1,
                "Persistence, item and fluid listeners must each fire exactly once");
        var holder = machine.getHolder().getSelf();
        var saved = holder.saveWithFullMetadata();
        holder.load(saved);
        slot.onContentsChanged();
        helper.assertTrue(slot.getTotalItemCount() == 5 && slot.getTotalFluidAmount() == 1000,
                "Actual block-entity save/load lost buffered materials");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "mePlus", required = true)
    public static void simulationDoesNotSpendTheSameInputsTwice(GameTestHelper helper) {
        var machine = buffer(helper, 0);
        var slot = machine.getInternalInventory()[0];
        slot.deserializeNBT(contents(5, 1000));
        var items = new ArrayList<Ingredient>(List.of(
                SizedIngredient.create(new ItemStack(Items.COBBLESTONE, 4)),
                SizedIngredient.create(new ItemStack(Items.COBBLESTONE, 4))));
        var remainingItems = slot.handleItemInternal(items, true);
        helper.assertTrue(remainingItems != null && remainingItems.size() == 1 &&
                        ((SizedIngredient) remainingItems.get(0)).getAmount() == 3 && slot.getTotalItemCount() == 5,
                "Simulation reused the same five items or changed live inventory");
        var fluids = new ArrayList<>(List.of(
                FluidIngredient.of(new FluidStack(Fluids.WATER, 750)),
                FluidIngredient.of(new FluidStack(Fluids.WATER, 750))));
        var remainingFluids = slot.handleFluidInternal(fluids, true);
        helper.assertTrue(remainingFluids != null && remainingFluids.get(0).getAmount() == 500 &&
                        slot.getTotalFluidAmount() == 1000,
                "Simulation reused the same fluid or changed live inventory");
        items = new ArrayList<>(List.of(
                SizedIngredient.create(new ItemStack(Items.COBBLESTONE, 4)),
                SizedIngredient.create(new ItemStack(Items.COBBLESTONE, 4))));
        helper.assertTrue(slot.handleItemInternal(items, false) != null && slot.isItemEmpty(),
                "Execution must consume exactly the five available items");
        fluids = new ArrayList<>(List.of(
                FluidIngredient.of(new FluidStack(Fluids.WATER, 750)),
                FluidIngredient.of(new FluidStack(Fluids.WATER, 750))));
        helper.assertTrue(slot.handleFluidInternal(fluids, false) != null && slot.isFluidEmpty(),
                "Execution must consume exactly the available fluid");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "mePlus", required = true)
    public static void duplicatePatternsSurviveBlockEntityReload(GameTestHelper helper) {
        var machine = buffer(helper, 0);
        machine.getTerminalPatternInventory().setItemDirect(0, pattern());
        machine.getTerminalPatternInventory().setItemDirect(1, pattern());
        helper.assertTrue(machine.getAvailablePatterns().size() == 1,
                "Equivalent patterns must be advertised once without crashing");
        var holder = machine.getHolder().getSelf();
        holder.load(holder.saveWithFullMetadata());
        machine.onLoad();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(machine.getAvailablePatterns().size() == 1,
                    "Reload rejected duplicate patterns");
            machine.getTerminalPatternInventory().setItemDirect(0, ItemStack.EMPTY);
            helper.assertTrue(machine.getAvailablePatterns().size() == 1,
                    "Removing one duplicate removed the other pattern");
            machine.getTerminalPatternInventory().setItemDirect(1, ItemStack.EMPTY);
            helper.assertTrue(machine.getAvailablePatterns().isEmpty(), "Removed pattern remained advertised");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "mePlus", required = true)
    public static void longCountsAndCircuitRemainBoundedAndPersisted(GameTestHelper helper) {
        var machine = buffer(helper, 0);
        var slot = machine.getInternalInventory()[0];
        slot.deserializeNBT(contents(Long.MAX_VALUE, Long.MAX_VALUE));
        slot.circuitInventory.setStackInSlot(0, IntCircuitBehaviour.stack(7));
        helper.assertTrue(slot.getItems().size() == 1 && slot.getFluids().size() == 1,
                "Recipe discovery must not expand long counts into billions of stacks");
        var saved = slot.serializeNBT();
        slot.deserializeNBT(contents(1, 1));
        slot.deserializeNBT(saved);
        helper.assertTrue(slot.getTotalItemCount() == Long.MAX_VALUE && slot.getTotalFluidAmount() == Long.MAX_VALUE &&
                        IntCircuitBehaviour.getCircuitConfiguration(slot.circuitInventory.getStackInSlot(0)) == 7,
                "Reload lost long counts or the per-pattern circuit");
        slot.deserializeNBT(new CompoundTag());
        helper.assertTrue(slot.isItemEmpty() && slot.isFluidEmpty() && slot.circuitInventory.isEmpty(),
                "Empty reload must replace old contents and invalidate caches");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "mePlus", required = true)
    public static void replacingOfflinePatternQueuesItsRefund(GameTestHelper helper) {
        var machine = buffer(helper, 0);
        machine.getTerminalPatternInventory().setItemDirect(0, pattern());
        var slot = machine.getInternalInventory()[0];
        slot.deserializeNBT(contents(9, 2500));
        machine.getTerminalPatternInventory().setItemDirect(0, ItemStack.EMPTY);
        helper.assertTrue(slot.isItemEmpty() && slot.isFluidEmpty(), "Old inputs contaminated a replacement pattern");
        var saved = machine.getHolder().getSelf().saveWithFullMetadata();
        helper.assertTrue(saved.toString().contains("internalBufferItem") && saved.toString().contains("internalBufferFluid"),
                "Offline refunds must be part of block-entity persistence");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "mePlus", required = true)
    public static void outputQueuesWakeAndNeverOverflow(GameTestHelper helper) {
        var machine = buffer(helper, 0);
        machine.setOnline(true);
        helper.assertTrue(!machine.shouldSubscribe(), "Empty output queue should sleep");
        helper.assertTrue(machine.getInventory().insertItemInternal(0, new ItemStack(Items.STONE, 64), true).isEmpty() &&
                        !machine.shouldSubscribe(), "Output simulation changed the queue");
        machine.getInventory().insertItemInternal(0, new ItemStack(Items.STONE, 64), false);
        helper.assertTrue(machine.shouldSubscribe(), "New output failed to wake ME export");
        var storage = new KeyStorage();
        var key = AEItemKey.of(Items.STONE);
        storage.storage.put(key, Long.MAX_VALUE - 1);
        var handler = MEPatternBufferOutputHandlers.items(machine, storage);
        helper.assertTrue(handler.insertItemInternal(0, new ItemStack(Items.STONE, 64), false).getCount() == 63 &&
                        storage.storage.getLong(key) == Long.MAX_VALUE,
                "Output overflow lost the unaccepted remainder");
        helper.assertTrue(handler.insertItemInternal(0, new ItemStack(Items.STONE, 2), false).getCount() == 2,
                "A full long-count queue must reject all remaining output");
        var fluidStorage = new KeyStorage();
        var fluidKey = AEFluidKey.of(Fluids.WATER);
        fluidStorage.storage.put(fluidKey, (long) Integer.MAX_VALUE);
        var tank = MEPatternBufferOutputHandlers.fluids(machine, fluidStorage);
        helper.assertTrue(tank.fillInternal(new FluidStack(Fluids.WATER, 1000), NotifiableFluidTank.FluidAction.EXECUTE) == 1000 &&
                        fluidStorage.storage.getLong(fluidKey) == (long) Integer.MAX_VALUE + 1000,
                "Fluid output was incorrectly capped at 32-bit counts");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "mePlus", required = true)
    public static void mirrorRebindingClearsOldListenersAndInputs(GameTestHelper helper) {
        var first = buffer(helper, 0);
        var second = buffer(helper, 1);
        var pos = helper.absolutePos(new BlockPos(2, 2, 0));
        helper.getLevel().setBlockAndUpdate(pos, GTEMachines.ME_PATTERN_BUFFER_PROXY_PLUS.getBlock().defaultBlockState());
        var mirror = (MEPatternBufferProxyPlusPartMachine) MetaMachine.getMachine(helper.getLevel(), pos);
        mirror.setBuffer(first.getPos());
        int[] calls = { 0 };
        var handler = mirror.getProxySlotRecipeHandler().getProxySlotHandlers().get(0);
        handler.subscribe(() -> calls[0]++);
        mirror.setBuffer(first.getPos());
        first.getInternalInventory()[0].deserializeNBT(contents(4, 1000));
        helper.assertTrue(handler.getTotalContentAmount() >= 4, "Mirror did not expose its buffer inputs");
        mirror.setBuffer(second.getPos());
        int afterRebind = calls[0];
        first.getInternalInventory()[0].onContentsChanged();
        helper.assertTrue(calls[0] == afterRebind && first.getProxies().isEmpty() && second.getProxies().contains(mirror),
                "Rebinding retained the old buffer or its listener");
        second.getInternalInventory()[0].deserializeNBT(contents(8, 2000));
        helper.assertTrue(calls[0] > afterRebind, "New buffer failed to notify the mirror");
        // A removed BE in a still-loaded chunk is immediately recreated by
        // Level.getBlockEntity. Invoke the machine unload lifecycle directly.
        second.onUnload();
        var resolvedAfterUnload = mirror.getBuffer();
        helper.assertTrue(resolvedAfterUnload == null && handler.getTotalContentAmount() == 0 &&
                        second.getPos().equals(mirror.getBufferPos()),
                "Chunk unload must clear live inputs while preserving the binding position; resolved=" +
                        resolvedAfterUnload + ", amount=" + handler.getTotalContentAmount() +
                        ", pos=" + mirror.getBufferPos() + ", source=" + second.getPos() +
                        ", available=" + second.isBufferAvailable() + ", removed=" + second.isInValid());
        second.onLoad();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(mirror.getBuffer() == second && handler.getTotalContentAmount() >= 8,
                    "Buffer reload did not restore the mirror's recipe handlers");
            mirror.setBuffer(null);
            helper.assertTrue(mirror.getBuffer() == null && handler.getTotalContentAmount() == 0 && second.getProxies().isEmpty(),
                    "Unbinding left live recipe inputs or stale proxy membership");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "mePlusNetwork", required = true, timeoutTicks = 200)
    public static void aePatternSharedRecipeAndOutputsReachRealStorage(GameTestHelper helper) {
        var machine = buffer(helper, 0);
        machine.setFrontFacing(Direction.NORTH);
        var controllerPos = helper.absolutePos(new BlockPos(0, 2, 3));
        helper.getLevel().setBlockAndUpdate(controllerPos, GTMultiMachines.LARGE_CHEMICAL_REACTOR.getBlock().defaultBlockState());
        var controller = (WorkableMultiblockMachine) MetaMachine.getMachine(helper.getLevel(), controllerPos);
        machine.addedToController(controller);
        // Only membership is injected; all handlers and the recipe runner are real.
        for (var handler : machine.getRecipeHandlers()) controller.addHandlerList(handler);
        machine.getShareInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        machine.getShareTank().setFluidInTank(0, new FluidStack(Fluids.LAVA, 50));
        var encoded = PatternDetailsHelper.encodeProcessingPattern(
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.COBBLESTONE), 1),
                        new GenericStack(AEFluidKey.of(Fluids.WATER), 100) },
                new GenericStack[] { new GenericStack(AEItemKey.of(Items.STONE), 2),
                        new GenericStack(AEFluidKey.of(Fluids.WATER), 250) });
        machine.getTerminalPatternInventory().setItemDirect(0, encoded);
        var details = PatternDetailsHelper.decodePattern(encoded, helper.getLevel());
        var recipe = GTRecipeTypes.LARGE_CHEMICAL_RECIPES.recipeBuilder("me_plus_regression")
                .inputItems(Items.COBBLESTONE).inputItems(Items.IRON_INGOT)
                .inputFluids(new FluidStack(Fluids.WATER, 100), new FluidStack(Fluids.LAVA, 50))
                .outputItems(new ItemStack(Items.STONE, 2)).outputFluids(new FluidStack(Fluids.WATER, 250))
                .duration(1).buildRawRecipe();
        var drivePos = helper.absolutePos(new BlockPos(2, 2, 0));
        var energyPos = helper.absolutePos(new BlockPos(4, 2, 0));
        helper.getLevel().setBlockAndUpdate(drivePos, AEBlocks.DRIVE.block().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(energyPos, AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        var drive = (DriveBlockEntity) helper.getLevel().getBlockEntity(drivePos);
        var energy = (AENetworkBlockEntity) helper.getLevel().getBlockEntity(energyPos);
        drive.getInternalInventory().setItemDirect(0, AEItems.ITEM_CELL_1K.stack());
        drive.getInternalInventory().setItemDirect(1, AEItems.FLUID_CELL_1K.stack());
        helper.runAfterDelay(5, () -> {
            GridHelper.createConnection(machine.getMainNode().getNode(), energy.getMainNode().getNode());
            GridHelper.createConnection(drive.getMainNode().getNode(), energy.getMainNode().getNode());
            boolean[] produced = { false };
            helper.succeedWhen(() -> {
                helper.assertTrue(machine.getMainNode().isActive(), "AE network has not booted");
                if (!produced[0]) {
                    KeyCounter[] inputs = new KeyCounter[details.getInputs().length];
                    for (int i = 0; i < inputs.length; i++) {
                        inputs[i] = new KeyCounter();
                        var input = details.getInputs()[i].getPossibleInputs()[0];
                        inputs[i].add(input.what(), input.amount());
                    }
                    helper.assertTrue(machine.pushPattern(details, inputs), "AE provider rejected valid mixed inputs");
                    helper.assertTrue(RecipeHelper.matchRecipe(controller, recipe).isSuccess(),
                            "Pattern inputs did not combine with the shared item/fluid inventories");
                    helper.assertTrue(RecipeHelper.handleRecipeIO(controller, recipe, IO.IN, Map.of()).isSuccess(),
                            "Real recipe runner failed to consume inputs");
                    helper.assertTrue(RecipeHelper.handleRecipeIO(controller, recipe, IO.OUT, Map.of()).isSuccess(),
                            "Real recipe runner rejected item/fluid output queues");
                    produced[0] = true;
                }
                var available = machine.getGrid().getStorageService().getInventory().getAvailableStacks();
                helper.assertTrue(available.get(AEItemKey.of(Items.STONE)) == 2 &&
                                available.get(AEFluidKey.of(Fluids.WATER)) == 250,
                        "Queued item/fluid outputs have not reached the real AE storage cells");
            });
        });
    }
}
