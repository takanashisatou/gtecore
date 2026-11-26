package org.satou.gtecore.mixin;

import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = BlockPattern.class, remap = false)
public interface BlockPatternAccessor {

    @Accessor("blockMatches")
    TraceabilityPredicate[][][] gtecore$getBlockMatches();

    @Accessor("fingerLength")
    int gtecore$getFingerLength();

    @Accessor("thumbLength")
    int gtecore$getThumbLength();

    @Accessor("palmLength")
    int gtecore$getPalmLength();

    @Accessor("centerOffset")
    int[] gtecore$getCenterOffset();

    @Invoker("setActualRelativeOffset")
    BlockPos gtecore$setActualRelativeOffset(int x, int y, int z, Direction facing, Direction upwardsFacing,
                                             boolean isFlipped);
}
