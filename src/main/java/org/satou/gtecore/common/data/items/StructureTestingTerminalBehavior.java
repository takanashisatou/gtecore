package org.satou.gtecore.common.data.items;

import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternError;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;
import com.gregtechceu.gtceu.api.pattern.error.SinglePredicateError;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.pattern.util.PatternMatchContext;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.satou.gtecore.GTECore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class StructureTestingTerminalBehavior implements IInteractionItem {
    private boolean sp_flag = true;
    public boolean checkPatternAtPro(Player player,BlockPattern pattern,MultiblockState worldState, boolean savePredicate){
        IMultiController controller = worldState.getController();
        if (controller == null) {
            worldState.setError(new PatternStringError("no controller found"));
            return false;
        }
        BlockPos centerPos = controller.self().getPos();
        Direction frontFacing = controller.self().getFrontFacing();
        Direction[] facings = controller.hasFrontFacing() ? new Direction[] { frontFacing } :
                new Direction[] { Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST };
        Direction upwardsFacing = controller.self().getUpwardsFacing();
        boolean allowsFlip = controller.self().allowFlip();
        for (Direction direction : facings) {
            boolean result = checkPatternAtPro(player,pattern,worldState, centerPos, direction, upwardsFacing, false, savePredicate);
            if (result) {
                return true;
            } else if (allowsFlip) {
                if(!sp_flag) return false;
                return checkPatternAtPro(player,pattern,worldState, centerPos, direction, upwardsFacing, true, savePredicate);
            }
        }
        return false;
    }
    private BlockPos setActualRelativeOffset(BlockPattern pattern,int x, int y, int z, Direction facing, Direction upwardsFacing,
                                             boolean isFlipped) {
        int[] c0 = new int[] { x, y, z }, c1 = new int[3];
        if (facing == Direction.UP || facing == Direction.DOWN) {
            Direction of = facing == Direction.DOWN ? upwardsFacing : upwardsFacing.getOpposite();
            for (int i = 0; i < 3; i++) {
                switch (pattern.structureDir[i].getActualDirection(of)) {
                    case UP -> c1[1] = c0[i];
                    case DOWN -> c1[1] = -c0[i];
                    case WEST -> c1[0] = -c0[i];
                    case EAST -> c1[0] = c0[i];
                    case NORTH -> c1[2] = -c0[i];
                    case SOUTH -> c1[2] = c0[i];
                }
            }
            int xOffset = upwardsFacing.getStepX();
            int zOffset = upwardsFacing.getStepZ();
            int tmp;
            if (xOffset == 0) {
                tmp = c1[2];
                c1[2] = zOffset > 0 ? c1[1] : -c1[1];
                c1[1] = zOffset > 0 ? -tmp : tmp;
            } else {
                tmp = c1[0];
                c1[0] = xOffset > 0 ? c1[1] : -c1[1];
                c1[1] = xOffset > 0 ? -tmp : tmp;
            }
            if (isFlipped) {
                if (upwardsFacing == Direction.NORTH || upwardsFacing == Direction.SOUTH) {
                    c1[0] = -c1[0]; // flip X-axis
                } else {
                    c1[2] = -c1[2]; // flip Z-axis
                }
            }
        } else {
            for (int i = 0; i < 3; i++) {
                switch (pattern.structureDir[i].getActualDirection(facing)) {
                    case UP -> c1[1] = c0[i];
                    case DOWN -> c1[1] = -c0[i];
                    case WEST -> c1[0] = -c0[i];
                    case EAST -> c1[0] = c0[i];
                    case NORTH -> c1[2] = -c0[i];
                    case SOUTH -> c1[2] = c0[i];
                }
            }
            if (upwardsFacing == Direction.WEST || upwardsFacing == Direction.EAST) {
                int xOffset = upwardsFacing == Direction.EAST ? facing.getClockWise().getStepX() :
                        facing.getClockWise().getOpposite().getStepX();
                int zOffset = upwardsFacing == Direction.EAST ? facing.getClockWise().getStepZ() :
                        facing.getClockWise().getOpposite().getStepZ();
                int tmp;
                if (xOffset == 0) {
                    tmp = c1[2];
                    c1[2] = zOffset > 0 ? -c1[1] : c1[1];
                    c1[1] = zOffset > 0 ? tmp : -tmp;
                } else {
                    tmp = c1[0];
                    c1[0] = xOffset > 0 ? -c1[1] : c1[1];
                    c1[1] = xOffset > 0 ? tmp : -tmp;
                }
            } else if (upwardsFacing == Direction.SOUTH) {
                c1[1] = -c1[1];
                if (facing.getStepX() == 0) {
                    c1[0] = -c1[0];
                } else {
                    c1[2] = -c1[2];
                }
            }
            if (isFlipped) {
                if (upwardsFacing == Direction.NORTH || upwardsFacing == Direction.SOUTH) {
                    if (facing == Direction.NORTH || facing == Direction.SOUTH) {
                        c1[0] = -c1[0]; // flip X-axis
                    } else {
                        c1[2] = -c1[2]; // flip Z-axis
                    }
                } else {
                    c1[1] = -c1[1]; // flip Y-axis
                }
            }
        }
        return new BlockPos(c1[0], c1[1], c1[2]);
    }
    public boolean checkPatternAtPro(Player player,BlockPattern pattern,MultiblockState worldState, BlockPos centerPos, Direction frontFacing,
                                  Direction upwardsFacing, boolean isFlipped, boolean savePredicate) {
        try {
            boolean flag = true;
            boolean findFirstAisle = false;
            var PatternClass = pattern.getClass();
            var centerOffset = PatternClass.getDeclaredField("centerOffset");
            centerOffset.setAccessible(true);
            var fingerLength = PatternClass.getDeclaredField("fingerLength");
            fingerLength.setAccessible(true);
            var thumbLength = PatternClass.getDeclaredField("thumbLength");
            thumbLength.setAccessible(true);
            var palmLength = PatternClass.getDeclaredField("palmLength");
            palmLength.setAccessible(true);
            var blockMatches = PatternClass.getDeclaredField("blockMatches");
            blockMatches.setAccessible(true);
            var formedRepetitionCount = PatternClass.getDeclaredField("formedRepetitionCount");
            formedRepetitionCount.setAccessible(true);
            int minZ = -((int [])centerOffset.get(pattern))[4];
            worldState.clean();
            PatternMatchContext matchContext = worldState.getMatchContext();
            Set<MultiblockState> failedMatchContext = new HashSet<>();
            Object2IntMap<SimplePredicate> globalCount = worldState.getGlobalCount();
            Object2IntMap<SimplePredicate> layerCount = worldState.getLayerCount();
            // Checking aisles
            for (int c = 0, z = minZ++, r; c < fingerLength.getInt(pattern); c++) {
                // Checking repeatable slices
                int validRepetitions = 0;
                loop:
                for (r = 0; (findFirstAisle ? r < pattern.aisleRepetitions[c][1] : z <= -(((int [])centerOffset.get(pattern))[3])); r++) {
                    // Checking single slice
                    layerCount.clear();

                    for (int b = 0, y = -(((int [])centerOffset.get(pattern))[1]); b < thumbLength.getInt(pattern); b++, y++) {
                        for (int a = 0, x = -(((int [])centerOffset.get(pattern))[0]); a < palmLength.getInt(pattern); a++, x++) {
                            worldState.setError(null);
                            TraceabilityPredicate predicate = ((TraceabilityPredicate [][][])blockMatches.get(pattern))[c][b][a];
                            BlockPos pos = setActualRelativeOffset(pattern,x, y, z, frontFacing, upwardsFacing, isFlipped)
                                    .offset(centerPos.getX(), centerPos.getY(), centerPos.getZ());
                            boolean world_state_flag = true;
                            if (!worldState.update(pos, predicate)) {
                                return false;
                            }
                            if (predicate.addCache()) {
                                worldState.addPosCache(pos);
                                if (savePredicate) {
                                    matchContext.getOrCreate("predicates", HashMap::new).put(pos, predicate);
                                }
                            }
                            boolean canPartShared = true;
                            if (worldState.getTileEntity() instanceof IMachineBlockEntity machineBlockEntity &&
                                    machineBlockEntity.getMetaMachine() instanceof IMultiPart part) { // add detected parts
                                if (!predicate.isAny()) {
                                    if (part.isFormed() && !part.canShared() &&
                                            !part.hasController(worldState.controllerPos)) { // check part can be shared
                                        canPartShared = false;
                                        worldState.setError(new PatternStringError("multiblocked.pattern.error.share"));
                                        failedMatchContext.add(worldState);
                                    } else {
                                        matchContext.getOrCreate("parts", HashSet::new).add(part);
                                    }
                                }
                            }
                            if (worldState.getBlockState().getBlock() instanceof ActiveBlock) {
                                matchContext.getOrCreate("vaBlocks", LongOpenHashSet::new)
                                        .add(worldState.getPos().asLong());
                            }
                            if (!predicate.test(worldState) || !canPartShared) {
                                failedMatchContext.add(worldState);
                                // matching failed
                                if (findFirstAisle) {
                                    if (r < pattern.aisleRepetitions[c][0]) {// retreat to see if the first aisle can start later
                                        r = c = 0;
                                        z = minZ++;
                                        matchContext.reset();
                                        findFirstAisle = false;
                                    }
                                } else {
                                    z++;// continue searching for the first aisle
                                }
                                continue loop;
                            }
                            matchContext.getOrCreate("ioMap", Long2ObjectOpenHashMap::new).put(worldState.getPos().asLong(),
                                    worldState.io);
                        }
                    }
                    findFirstAisle = true;

                    z++;
                    // Check layer-local matcher predicate
                    for (var entry : layerCount.object2IntEntrySet()) {
                        if (entry.getIntValue() < entry.getKey().minLayerCount) {
                            worldState.setError(new SinglePredicateError(entry.getKey(), 3));
                            failedMatchContext.add(worldState);

                            flag = false;
                        }
                    }
                    validRepetitions++;
                }
                // Repetitions out of range or Fail Match
                if (r < pattern.aisleRepetitions[c][0] || worldState.hasError() || !findFirstAisle) {
                    if (!worldState.hasError()) {
                        worldState.setError(new PatternError());
                    }
                    failedMatchContext.add(worldState);
                   flag = false;
                }

                // finished checking the aisle, so store the repetitions
                ((int [])formedRepetitionCount.get(pattern))[c] = validRepetitions;
            }

            // Check count matches amount
            for (var entry : globalCount.object2IntEntrySet()) {
                if (entry.getIntValue() < entry.getKey().minCount) {
                    worldState.setError(new SinglePredicateError(entry.getKey(), 1));
                   flag = false;
                }
                failedMatchContext.add(worldState);
            }

            worldState.setError(null);
            worldState.setNeededFlip(isFlipped);
            if(!flag){
                if(!sp_flag) return false;
                failedMatchContext.forEach((world_state)->{
                    var pos = world_state.getPos();
                    int posx = pos.getX();
                    int posy = pos.getY();
                    int posz = pos.getZ();
                    player.sendSystemMessage(Component.translatable("com.gtecore.structure_tesing.tooltips.at").append(" X:").append(String.valueOf(posx)).append(" Y:").append(String.valueOf(posy)).append(" Z:").append(String.valueOf(posz)).append(((MultiblockState)world_state).getBlockState().getBlock().getName()).append(Component.translatable("com.gtecore.structure_tesing.tooltips.place_error")));
                    sp_flag = false;
                });
            }
            return flag;
        }catch (Exception e){
            GTECore.LOGGER.error(e.getMessage());
        }
        return false;
    }
    public void checkStructure(Player player,IMultiController controller){
        sp_flag = true;
        BlockPattern pattern = controller.getPattern();
        if(pattern != null){
            checkPatternAtPro(player,controller.getPattern(),controller.getMultiblockState(), false);
        }
    }
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            Level level = context.getLevel();
            BlockPos blockPos = context.getClickedPos();
            if (context.getPlayer() != null &&
                    MetaMachine.getMachine(level, blockPos) instanceof IMultiController controller) {
                if (!controller.isFormed()) {
                    if (!level.isClientSide) {
                        Player player = context.getPlayer();
                        player.sendSystemMessage(Component.translatable("com.gtecore.structure_tesing.tooltips.failure"));
                        checkStructure(player,controller);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }else {
                    if(!level.isClientSide) {
                        Player player = context.getPlayer();
                        player.sendSystemMessage(Component.translatable("com.gtecore.structure_tesing.tooltips.success"));
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Item item, Level level, Player player, InteractionHand usedHand) {
        ItemStack heldItem = player.getItemInHand(usedHand);
        return InteractionResultHolder.pass(heldItem);
    }
}
