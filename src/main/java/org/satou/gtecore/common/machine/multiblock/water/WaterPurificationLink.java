package org.satou.gtecore.common.machine.multiblock.water;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/** Typed, dimension-aware addresses; unrelated data-stick payloads cannot create a link. */
final class WaterPurificationLink {

    private static final String KEY = "gteWaterPurificationLink";

    private WaterPurificationLink() {}

    static void write(ItemStack stick, Level level, BlockPos pos, String kind) {
        CompoundTag address = new CompoundTag();
        address.putLong("pos", pos.asLong());
        address.putString("dimension", level.dimension().location().toString());
        address.putString("kind", kind);
        stick.getOrCreateTag().put(KEY, address);
    }

    static @Nullable BlockPos read(ItemStack stick, Level level, String kind) {
        CompoundTag tag = stick.getTag();
        if (tag == null || !tag.contains(KEY, Tag.TAG_COMPOUND)) return null;
        CompoundTag address = tag.getCompound(KEY);
        if (!address.contains("pos", Tag.TAG_LONG) || !kind.equals(address.getString("kind")) ||
                !level.dimension().location().toString().equals(address.getString("dimension"))) return null;
        BlockPos pos = BlockPos.of(address.getLong("pos"));
        return level.hasChunkAt(pos) ? pos : null;
    }
}
