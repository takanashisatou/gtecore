package org.satou.gtecore.common;

import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.GTUtil;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.satou.gtecore.GTECore;

import java.util.List;

public class GTECoilBlock extends ActiveBlock {
    public ICoilType coilType;

    public GTECoilBlock(BlockBehaviour.Properties properties, ICoilType coilType) {
        super(properties);
        this.coilType = coilType;
    }

    public enum GTECoilType implements StringRepresentable, ICoilType {


        YIN_YANG_COIL("yin_yang", 10800, 16, 8, GTMaterials.Tritanium,
                GTECore.id("block/casings/coils/yin_yang_coil"));

        @NotNull
        @Getter
        private final String name;
        // electric blast furnace properties
        @Getter
        private final int coilTemperature;
        // multi smelter properties
        @Getter
        private final int level;
        @Getter
        private final int energyDiscount;
        @NotNull
        @Getter
        private final Material material;
        @NotNull
        @Getter
        private final ResourceLocation texture;

        GTECoilType(String name, int coilTemperature, int level, int energyDiscount, Material material,
                 ResourceLocation texture) {
            this.name = name;
            this.coilTemperature = coilTemperature;
            this.level = level;
            this.energyDiscount = energyDiscount;
            this.material = material;
            this.texture = texture;
        }

        public int getTier() {
            return this.ordinal();
        }

        @NotNull
        @Override
        public String toString() {
            return getName();
        }

        @Override
        @NotNull
        public String getSerializedName() {
            return name;
        }
    }
}
