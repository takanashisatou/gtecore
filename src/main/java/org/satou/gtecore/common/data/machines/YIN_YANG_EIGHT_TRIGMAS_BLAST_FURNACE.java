package org.satou.gtecore.common.data.machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.PistonEvent;
import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.satou.gtecore.common.data.GTEBlocks;

import java.util.List;

import static com.gregtechceu.gtceu.api.pattern.Predicates.abilities;
import static com.gregtechceu.gtceu.api.pattern.Predicates.autoAbilities;
import static org.satou.gtecore.utils.GTEUtils.easy;

public class YIN_YANG_EIGHT_TRIGMAS_BLAST_FURNACE extends WorkableElectricMultiblockMachine implements IDisplayUIMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            YIN_YANG_EIGHT_TRIGMAS_BLAST_FURNACE.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);
    public YIN_YANG_EIGHT_TRIGMAS_BLAST_FURNACE(IMachineBlockEntity holder) {
        super(holder);
    }
    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }
    @Persisted
    public boolean QING_LONG_MODULE_ENABLED = false;
    @Persisted
    public boolean ZHU_QUE_MODULE_ENABLED = false;
    @Persisted
    public boolean BAI_HU_MODULE_ENABLED = false;
    @Persisted
    public boolean XUAN_WU_MODULE_ENABLED = false;
    @Override
    public BlockPattern getPattern() {
        if(getLevel() != null) checkModule();
       return super.getPattern();
    }

    private void checkModule() {
        check_QING_LONG_MODULE();
        check_ZHU_QUE_MODULE();
        check_BAI_HU_MODULE();
        check_XUAN_WU_MODULE();
    }

    private final int[] XUAN_WU_OFFSETS_X = {-15,-15,-15,-15,-14,-13,-12,-11,-10,-10,-9,-11,-10,-11,-11};
    //private int[] XUAN_WU_OFFSETS_Y = {-15,-15,-15 -15,-15,-15};
    private final int[] XUAN_WU_OFFSETS_Z = {-5,-4,  -3, -1, -2,  -3, -4,  -4,-4,-3,-2, -1,-1,0,1};
    private void check_XUAN_WU_MODULE() {
        XUAN_WU_MODULE_ENABLED = false;
        Level world = getLevel();
        if(world == null) return;
        var pos = getPos();

        for (int i = 0; i < XUAN_WU_OFFSETS_X.length;i++){
            var npos = new BlockPos(pos.getX() + XUAN_WU_OFFSETS_X[i],pos.getY() + (-15),pos.getZ() + XUAN_WU_OFFSETS_Z[i]);
            if(world.getBlockState(npos) != GTEBlocks.XUANWU_MODULE.getDefaultState()) return;
        }
        XUAN_WU_MODULE_ENABLED = true;
    }
    private final int[] BAI_HU_OFFSETS_X = {-14,-10,-15,-14,-11,-15,-14,-13,-12,-11,-10,-10,-12,-14,-15,-15,-13,-12,-11};
    private final int[] BAI_HU_OFFSETS_Z = {-29,-29,-28,-28,-28,-27,-27,-27,-27,-27,-26,-25,-25,-25,-24,-23,-23,-23,-22};
    private void check_BAI_HU_MODULE() {
        BAI_HU_MODULE_ENABLED = false;
        Level world = getLevel();
        if(world == null) return;
        var pos = getPos();

        for (int i = 0; i < BAI_HU_OFFSETS_X.length;i++){
            var npos = new BlockPos(pos.getX() +BAI_HU_OFFSETS_X[i],pos.getY() + (-15),pos.getZ() + BAI_HU_OFFSETS_Z[i]);
            if(world.getBlockState(npos) != GTEBlocks.BAIHU_MODULE.getDefaultState()) return;
        }
       BAI_HU_MODULE_ENABLED = true;
    }
    private final int[] ZHU_QUE_OFFSETS_X = {9,10,11,12,13,14,15,12,13,11,14,10,13};
    private final int[] ZHU_QUE_OFFSETS_Z = {-26,-27,-27,-26,-26,-27,-28,-25,-25,-24,-24,-23,-23};
    private void check_ZHU_QUE_MODULE() {
        ZHU_QUE_MODULE_ENABLED = false;
        Level world = getLevel();
        if(world == null) return;
        var pos = getPos();
        for (int i = 0; i < ZHU_QUE_OFFSETS_X.length;i++){
            var npos = new BlockPos(pos.getX() +ZHU_QUE_OFFSETS_X[i],pos.getY() + (-15),pos.getZ() + ZHU_QUE_OFFSETS_Z[i]);
            if(world.getBlockState(npos) != GTEBlocks.ZHUQUE_MODULE.getDefaultState()) return;
        }
        ZHU_QUE_MODULE_ENABLED = true;
    }
    private final int[] QING_LONG_OFFSETS_X = {9,9,10,11,10,11,12,13,14,15,9,12,13,15,8,11,15,11,14,13};
    private final int[] QING_LONG_OFFSETS_Z = {-5,-4,-4,-4,-3,-3,-3,-3,-3,-3,-2,-2,-2,-2,-1,-1,-1,0,0,1};
    private void check_QING_LONG_MODULE() {
        QING_LONG_MODULE_ENABLED = false;
        Level world = getLevel();
        if(world == null) return;
        var pos = getPos();
        for (int i = 0; i < QING_LONG_OFFSETS_X.length;i++){
            var npos = new BlockPos(pos.getX() +QING_LONG_OFFSETS_X[i],pos.getY() + (-15),pos.getZ() + QING_LONG_OFFSETS_Z[i]);
            if(world.getBlockState(npos) != GTEBlocks.QINLONG_MODULE.getDefaultState()) return;
        }
        QING_LONG_MODULE_ENABLED = true;
    }
    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if(isFormed()){
            if(XUAN_WU_MODULE_ENABLED){
                textList.add(Component.translatable("gtecore.xuanwu_module.enabled"));
            }else {
                textList.add(Component.translatable("gtecore.xuanwu_module.disabled"));
            }
            if(ZHU_QUE_MODULE_ENABLED){
                textList.add(Component.translatable("gtecore.zhuque_module.enabled"));
            }else{
                textList.add(Component.translatable("gtecore.zhuque_module.disabled"));
            }
            if(QING_LONG_MODULE_ENABLED){
                textList.add(Component.translatable("gtecore.qinglong_module.enabled"));
            }else{
                textList.add(Component.translatable("gtecore.qinglong_module.disabled"));
            }
            if(BAI_HU_MODULE_ENABLED){
                textList.add(Component.translatable("gtecore.baihu_module.enabled"));
            }else{
                textList.add(Component.translatable("gtecore.baihu_module.disabled"));
            }
        }
    }
}
