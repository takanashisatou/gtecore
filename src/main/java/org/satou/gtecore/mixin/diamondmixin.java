package org.satou.gtecore.mixin;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import com.google.common.collect.Maps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

import static net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.*;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class diamondmixin {

    @Shadow
    private static void add(Map<Item, Integer> p_204303_, TagKey<Item> p_204304_, int p_204305_) {}

    @Shadow
    private static void add(Map<Item, Integer> p_58375_, ItemLike p_58376_, int p_58377_) {}

    @Inject(method = "getFuel", at = @At("HEAD"), cancellable = true)
    private static void getFuel(CallbackInfoReturnable<Map<Item, Integer>> cir) {
        Map<Item, Integer> map = Maps.newLinkedHashMap();
        add(map, Items.DIAMOND, 10240000);
        add(map, Blocks.DIAMOND_BLOCK, 102400000);
        add(map, Items.LAVA_BUCKET, 20000);
        add(map, Blocks.COAL_BLOCK, 16000);
        add(map, Items.BLAZE_ROD, 2400);
        add(map, Items.COAL, 1600);
        add(map, Items.CHARCOAL, 1600);
        add(map, ItemTags.LOGS, 300);
        add(map, ItemTags.BAMBOO_BLOCKS, 300);
        add(map, ItemTags.PLANKS, 300);
        add(map, Blocks.BAMBOO_MOSAIC, 300);
        add(map, ItemTags.WOODEN_STAIRS, 300);
        add(map, Blocks.BAMBOO_MOSAIC_STAIRS, 300);
        add(map, ItemTags.WOODEN_SLABS, 150);
        add(map, Blocks.BAMBOO_MOSAIC_SLAB, 150);
        add(map, ItemTags.WOODEN_TRAPDOORS, 300);
        add(map, ItemTags.WOODEN_PRESSURE_PLATES, 300);
        add(map, ItemTags.WOODEN_FENCES, 300);
        add(map, ItemTags.FENCE_GATES, 300);
        add(map, Blocks.NOTE_BLOCK, 300);
        add(map, Blocks.BOOKSHELF, 300);
        add(map, Blocks.CHISELED_BOOKSHELF, 300);
        add(map, Blocks.LECTERN, 300);
        add(map, Blocks.JUKEBOX, 300);
        add(map, Blocks.CHEST, 300);
        add(map, Blocks.TRAPPED_CHEST, 300);
        add(map, Blocks.CRAFTING_TABLE, 300);
        add(map, Blocks.DAYLIGHT_DETECTOR, 300);
        add(map, ItemTags.BANNERS, 300);
        add(map, Items.BOW, 300);
        add(map, Items.FISHING_ROD, 300);
        add(map, Blocks.LADDER, 300);
        add(map, ItemTags.SIGNS, 200);
        add(map, ItemTags.HANGING_SIGNS, 800);
        add(map, Items.WOODEN_SHOVEL, 200);
        add(map, Items.WOODEN_SWORD, 200);
        add(map, Items.WOODEN_HOE, 200);
        add(map, Items.WOODEN_AXE, 200);
        add(map, Items.WOODEN_PICKAXE, 200);
        add(map, ItemTags.WOODEN_DOORS, 200);
        add(map, ItemTags.BOATS, 1200);
        add(map, ItemTags.WOOL, 100);
        add(map, ItemTags.WOODEN_BUTTONS, 100);
        add(map, Items.STICK, 100);
        add(map, ItemTags.SAPLINGS, 100);
        add(map, Items.BOWL, 100);
        add(map, ItemTags.WOOL_CARPETS, 67);
        add(map, Blocks.DRIED_KELP_BLOCK, 4001);
        add(map, Items.CROSSBOW, 300);
        add(map, Blocks.BAMBOO, 50);
        add(map, Blocks.DEAD_BUSH, 100);
        add(map, Blocks.SCAFFOLDING, 50);
        add(map, Blocks.LOOM, 300);
        add(map, Blocks.BARREL, 300);
        add(map, Blocks.CARTOGRAPHY_TABLE, 300);
        add(map, Blocks.FLETCHING_TABLE, 300);
        add(map, Blocks.SMITHING_TABLE, 300);
        add(map, Blocks.COMPOSTER, 300);
        add(map, Blocks.AZALEA, 100);
        add(map, Blocks.FLOWERING_AZALEA, 100);
        add(map, Blocks.MANGROVE_ROOTS, 300);
        cir.setReturnValue(map);
        cir.cancel();
    }
}
