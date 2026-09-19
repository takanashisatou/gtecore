package org.satou.gtecore.common.command;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.common.data.GCYMBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputBusPartMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeInputHatchPartMachine;
import com.hepdd.gtmthings.common.block.machine.multiblock.part.CreativeEnergyHatchPartMachine;
import com.hepdd.gtmthings.data.CreativeMachines;
import com.gregtechceu.gtceu.common.machine.multiblock.part.MaintenanceHatchPartMachine;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.satou.gtecore.GTECore;
import org.satou.gtecore.common.data.GTEMaterials;
import org.satou.gtecore.common.data.items.GTEItems;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationMachines;
import org.satou.gtecore.common.data.machines.GTEWaterPurificationParts;
import org.satou.gtecore.common.machine.multiblock.water.CentralPurificationPlantMachine;
import org.satou.gtecore.common.machine.multiblock.water.ThermalPurificationUnitMachine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Operator-only, transactional construction of a physically wired, normally ticking test plant. */
@Mod.EventBusSubscriber(modid = GTECore.MOD_ID)
public final class WaterPurificationTestCommand {

    private WaterPurificationTestCommand() {}

    public record Layout(BlockPos plant, BlockPos unit, BlockPos signal, BlockPos control,
                         BlockPos repeater, BlockPos fluidOutput, BlockPos itemOutput,
                         BlockPos energy, BlockPos waterInput, BlockPos flocculantInput,
                         BlockPos itemInput, BlockPos minimum, BlockPos maximum) {}

    /** Inclusive bounds, including all empty clearance and the new foundation. */
    public static BlockPos minimum(BlockPos anchor) { return anchor.offset(0, -1, -3); }
    public static BlockPos maximum(BlockPos anchor) { return anchor.offset(18, 7, 6); }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gte_water_test")
                .requires(source -> source.hasPermission(2))
                .executes(context -> execute(context.getSource(), BlockPos.containing(context.getSource().getPosition()).offset(3, 1, 3)))
                .then(Commands.argument("origin", BlockPosArgument.blockPos())
                        .executes(context -> execute(context.getSource(), BlockPosArgument.getBlockPos(context, "origin")))));
    }

    private static int execute(CommandSourceStack source, BlockPos anchor) {
        try {
            Layout layout = build(source.getLevel(), anchor);
            source.sendSuccess(() -> Component.literal("T1 净水控温试验场已建成：范围 " + layout.minimum().toShortString()
                    + " → " + layout.maximum().toShortString() + "；控制器 " + layout.unit().toShortString()
                    + "。单并行，创造输入仓/总线无限供料，创造能源仓持续供电，ME 输出仓/总线缓冲产物。"
                    + "未接 AE 网络时可在 ME 仓的等待列表查看产物。"
                    + "红石中继器 " + layout.repeater().toShortString() + " 可拆下模拟断线；请保持区块加载。"), true);
            return 1;
        } catch (IllegalStateException failure) {
            source.sendFailure(Component.literal(failure.getMessage()));
            return 0;
        }
    }

    public static Layout build(ServerLevel level, BlockPos anchor) {
        BlockPos minimum = minimum(anchor), maximum = maximum(anchor);
        // Validate the entire volume before touching any block, including reserved air.
        for (BlockPos cursor : BlockPos.betweenClosed(minimum, maximum)) {
            if (level.isOutsideBuildHeight(cursor) || !level.getWorldBorder().isWithinBounds(cursor)
                    || !level.hasChunkAt(cursor)) {
                throw new IllegalStateException("建造范围未加载或超出边界：" + cursor.toShortString());
            }
            BlockState state = level.getBlockState(cursor);
            if (!state.canBeReplaced() || !state.getFluidState().isEmpty() || level.getBlockEntity(cursor) != null) {
                throw new IllegalStateException("建造范围必须为空，阻挡位置：" + cursor.toShortString()
                        + "；所需范围 " + minimum.toShortString() + " → " + maximum.toShortString());
            }
        }

        Map<BlockPos, BlockState> plan = new LinkedHashMap<>();
        BlockPos unit = preview(plan, anchor, GTEWaterPurificationMachines.T1_CLARIFIER_PURIFICATION_UNIT);
        BlockPos plant = preview(plan, anchor.offset(12, 0, 0), GTEWaterPurificationMachines.CENTRAL_WATER_PURIFICATION_PLANT);
        BlockPos signal = find(plan, GTEWaterPurificationParts.THERMAL_SIGNAL_HATCH);
        BlockPos control = find(plan, GTEWaterPurificationParts.THERMAL_CONTROL_HATCH);
        List<BlockPos> unitPorts = casingPositions(plan, anchor, 9);
        List<BlockPos> plantPorts = casingPositions(plan, anchor.offset(12, 0, 0), 7);
        BlockPos water = replace(plan, unitPorts.removeFirst(), CreativeMachines.CREATIVE_FLUID_INPUT_HATCH);
        BlockPos flocculant = replace(plan, unitPorts.removeFirst(), CreativeMachines.CREATIVE_FLUID_INPUT_HATCH);
        BlockPos fluidOutput = replace(plan, unitPorts.removeFirst(), GTAEMachines.FLUID_EXPORT_HATCH_ME);
        BlockPos itemInput = replace(plan, unitPorts.removeFirst(), CreativeMachines.CREATIVE_ITEM_INPUT_BUS);
        BlockPos itemOutput = replace(plan, unitPorts.removeFirst(), GTAEMachines.ITEM_EXPORT_BUS_ME);
        BlockPos energy = replace(plan, plantPorts.removeFirst(), CreativeMachines.CREATIVE_ENERGY_INPUT_HATCH);
        replace(plan, plantPorts.removeFirst(), GTMachines.MAINTENANCE_HATCH);

        // Closed loop: signal front -> dust north -> dust across -> repeater south into control.
        BlockPos repeater = control.north();
        for (int x = Math.min(signal.getX(), control.getX()); x <= Math.max(signal.getX(), control.getX()); x++) {
            plan.put(new BlockPos(x, signal.getY(), signal.getZ() - 2), Blocks.REDSTONE_WIRE.defaultBlockState());
        }
        plan.put(signal.north(), Blocks.REDSTONE_WIRE.defaultBlockState());
        plan.put(repeater, Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.NORTH));
        // Supports must be placed before the components that require them.
        Map<BlockPos, BlockState> complete = new LinkedHashMap<>();
        for (int x = 0; x <= 18; x++) for (int z = -3; z <= 6; z++) {
            complete.put(anchor.offset(x, -1, z), Blocks.STONE_BRICKS.defaultBlockState());
        }
        for (var entry : plan.entrySet()) {
            if (entry.getValue().is(Blocks.REDSTONE_WIRE) || entry.getValue().is(Blocks.REPEATER)) {
                complete.put(entry.getKey().below(), Blocks.STONE_BRICKS.defaultBlockState());
            }
        }
        complete.putAll(plan);
        for (BlockPos pos : complete.keySet()) {
            if (pos.getX() < minimum.getX() || pos.getX() > maximum.getX()
                    || pos.getY() < minimum.getY() || pos.getY() > maximum.getY()
                    || pos.getZ() < minimum.getZ() || pos.getZ() > maximum.getZ()) {
                throw new IllegalStateException("结构预览超出已检查的建造范围：" + pos.toShortString());
            }
        }
        Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        try {
            for (var entry : complete.entrySet()) {
                original.put(entry.getKey(), level.getBlockState(entry.getKey()));
                if (!level.setBlock(entry.getKey(), entry.getValue(), Block.UPDATE_ALL)) {
                    throw new IllegalStateException("无法放置方块：" + entry.getKey().toShortString());
                }
            }
            for (BlockPos pos : complete.keySet()) {
                if (MetaMachine.getMachine(level, pos) instanceof MaintenanceHatchPartMachine maintenance) {
                    maintenance.fixAllMaintenanceProblems();
                }
            }
            CentralPurificationPlantMachine plantMachine = machine(level, plant, CentralPurificationPlantMachine.class);
            ThermalPurificationUnitMachine unitMachine = machine(level, unit, ThermalPurificationUnitMachine.class);
            CreativeTestInputs.energy(machine(level, energy, CreativeEnergyHatchPartMachine.class));
            form(plantMachine);
            form(unitMachine);
            plantMachine.setParallel(1);
            if (!unitMachine.bindToPlant(plant)) throw new IllegalStateException("净水中枢绑定失败");
            CreativeTestInputs.fluid(machine(level, water, CreativeInputHatchPartMachine.class), GTMaterials.Water.getFluid(1));
            CreativeTestInputs.fluid(machine(level, flocculant, CreativeInputHatchPartMachine.class), GTEMaterials.CompositeFlocculant.getFluid(1));
            CreativeTestInputs.item(machine(level, itemInput, CreativeInputBusPartMachine.class), GTEItems.MODIFIED_CARBON_MICROSPHERES.asStack());
            return new Layout(plant, unit, signal, control, repeater, fluidOutput, itemOutput, energy,
                    water, flocculant, itemInput, minimum, maximum);
        } catch (RuntimeException failure) {
            // A later failure may follow successful formation of the first machine.
            // Detach its parts and world mapping before removing the generated block entities.
            var saved = MultiblockWorldSavedData.getOrCreate(level);
            for (BlockPos pos : original.keySet()) {
                if (MetaMachine.getMachine(level, pos) instanceof MultiblockControllerMachine controller) {
                    controller.onStructureInvalid();
                    saved.removeMapping(controller.getMultiblockState());
                    saved.removeAsyncLogic(controller);
                }
            }
            List<BlockPos> reverse = new ArrayList<>(original.keySet());
            for (int i = reverse.size() - 1; i >= 0; i--) {
                BlockPos pos = reverse.get(i);
                // Remove inventory-bearing entities first, so rollback never spills generated contents.
                level.removeBlockEntity(pos);
                level.setBlock(pos, original.get(pos), Block.UPDATE_ALL);
            }
            throw new IllegalStateException("试验场未建成，已还原：" + failure.getMessage(), failure);
        }
    }

    private static void form(MultiblockControllerMachine machine) {
        if (!machine.checkPatternWithLock()) throw new IllegalStateException("真实结构检查失败：" + machine.getPos().toShortString());
        machine.setFlipped(machine.getMultiblockState().isNeededFlip());
        machine.onStructureFormed();
        var saved = MultiblockWorldSavedData.getOrCreate((ServerLevel) machine.getLevel());
        saved.addMapping(machine.getMultiblockState());
        saved.removeAsyncLogic(machine);
    }

    private static <T> T machine(ServerLevel level, BlockPos pos, Class<T> type) {
        var machine = MetaMachine.getMachine(level, pos);
        if (!type.isInstance(machine)) throw new IllegalStateException("机器类型错误：" + pos.toShortString());
        return type.cast(machine);
    }

    private static BlockPos preview(Map<BlockPos, BlockState> plan, BlockPos origin, MultiblockMachineDefinition definition) {
        var shape = definition.getMatchingShapes().getFirst().getBlocks();
        for (int x = 0; x < shape.length; x++) for (int y = 0; y < shape[x].length; y++) for (int z = 0; z < shape[x][y].length; z++) {
            BlockState state = shape[x][y][z].getBlockState();
            if (state.isAir()) continue;
            if (state.getBlock() instanceof MetaMachineBlock
                    && state.getBlock() != definition.getBlock()
                    && state.getBlock() != GTEWaterPurificationParts.THERMAL_SIGNAL_HATCH.getBlock()
                    && state.getBlock() != GTEWaterPurificationParts.THERMAL_CONTROL_HATCH.getBlock()
                    && !(definition == GTEWaterPurificationMachines.T1_CLARIFIER_PURIFICATION_UNIT
                    && state.getBlock() == GTMachines.MAINTENANCE_HATCH.getBlock())) {
                state = GCYMBlocks.CASING_WATERTIGHT.getDefaultState();
            }
            if (state.hasProperty(BlockStateProperties.FACING)) state = state.setValue(BlockStateProperties.FACING, Direction.NORTH);
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            plan.put(origin.offset(x, y, z), state);
        }
        return find(plan, definition);
    }

    private static BlockPos find(Map<BlockPos, BlockState> plan, MachineDefinition definition) {
        return plan.entrySet().stream().filter(entry -> entry.getValue().getBlock() == definition.getBlock())
                .map(Map.Entry::getKey).findFirst().orElseThrow(() -> new IllegalStateException("结构预览缺少 " + definition.getId()));
    }

    private static List<BlockPos> casingPositions(Map<BlockPos, BlockState> plan, BlockPos origin, int width) {
        return new ArrayList<>(plan.entrySet().stream()
                .filter(entry -> entry.getValue().is(GCYMBlocks.CASING_WATERTIGHT.get())
                        && entry.getKey().getX() >= origin.getX() && entry.getKey().getX() < origin.getX() + width)
                .map(Map.Entry::getKey).toList());
    }

    private static BlockPos replace(Map<BlockPos, BlockState> plan, BlockPos pos, MachineDefinition definition) {
        BlockState state = definition.getBlock().defaultBlockState();
        // Ports are chosen from the westmost casings. Point outward so neighboring tanks
        // cannot automatically transfer their contents into one another along the row.
        if (state.hasProperty(BlockStateProperties.FACING)) state = state.setValue(BlockStateProperties.FACING, Direction.WEST);
        plan.put(pos, state);
        return pos;
    }
}
