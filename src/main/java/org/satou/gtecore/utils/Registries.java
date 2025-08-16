package org.satou.gtecore.utils;

import org.satou.gtecore.GTECore;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import static net.minecraft.resources.ResourceLocation.tryParse;

public class Registries {

    public static Item getItem(String s) {
        Item i = ForgeRegistries.ITEMS.getValue(tryParse(s));
        if (i == Items.AIR) {
            GTECore.LOGGER.atError().log("未找到ID为{}的物品", s);
            return Items.BARRIER;
        }
        return i;
    }

    public static ItemStack getItemStack(String s) {
        return getItemStack(s, 1);
    }

    public static ItemStack getItemStack(String s, int a) {
        return new ItemStack(getItem(s), a);
    }

    public static Block getBlock(String s) {
        Block b = ForgeRegistries.BLOCKS.getValue(tryParse(s));
        if (b == Blocks.AIR) {
            GTECore.LOGGER.atError().log("未找到ID为{}的方块", s);
            return Blocks.BARRIER;
        }
        return b;
    }

    public static Fluid getFluid(String s) {
        Fluid f = ForgeRegistries.FLUIDS.getValue(tryParse(s));
        if (f == Fluids.EMPTY) {
            GTECore.LOGGER.atError().log("未找到ID为{}的流体", s);
            return Fluids.WATER;
        }
        return f;
    }
}
