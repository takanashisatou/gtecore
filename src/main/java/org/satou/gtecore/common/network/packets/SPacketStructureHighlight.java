package org.satou.gtecore.common.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.satou.gtecore.client.renderer.StructureHighlightRenderer;
import org.satou.gtecore.common.data.items.StructureTestingTerminalBehavior;
import org.satou.gtecore.common.data.items.StructureTestingTerminalBehavior.HighlightKind;
import org.satou.gtecore.common.data.items.StructureTestingTerminalBehavior.HighlightTarget;
import org.satou.gtecore.common.network.GTECoreNetwork;

import java.util.ArrayList;
import java.util.List;

public final class SPacketStructureHighlight implements GTECoreNetwork.INetPacket {

    private final ResourceKey<Level> dimension;
    private final boolean clear;
    private final boolean notifyOnClear;
    private final int durationTicks;
    private final List<HighlightTarget> targets;

    public SPacketStructureHighlight(ResourceKey<Level> dimension, boolean clear, boolean notifyOnClear,
                                     int durationTicks, List<HighlightTarget> targets) {
        this.dimension = dimension;
        this.clear = clear;
        this.notifyOnClear = notifyOnClear;
        this.durationTicks = durationTicks;
        this.targets = List.copyOf(targets);
    }

    public static SPacketStructureHighlight highlight(ResourceKey<Level> dimension, int durationTicks,
                                                      List<HighlightTarget> targets) {
        return new SPacketStructureHighlight(dimension, false, false, durationTicks, targets);
    }

    public static SPacketStructureHighlight clear(ResourceKey<Level> dimension, boolean notifyOnClear) {
        return new SPacketStructureHighlight(dimension, true, notifyOnClear, 0, List.of());
    }

    public SPacketStructureHighlight(FriendlyByteBuf buffer) {
        this.dimension = buffer.readResourceKey(Registries.DIMENSION);
        this.clear = buffer.readBoolean();
        this.notifyOnClear = buffer.readBoolean();
        this.durationTicks = buffer.readVarInt();
        int count = Math.min(Math.max(0, buffer.readVarInt()), StructureTestingTerminalBehavior.MAX_HIGHLIGHTS);
        List<HighlightTarget> decoded = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            BlockPos pos = buffer.readBlockPos();
            HighlightKind kind = buffer.readEnum(HighlightKind.class);
            boolean primary = buffer.readBoolean();
            decoded.add(new HighlightTarget(pos, kind, primary));
        }
        this.targets = List.copyOf(decoded);
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceKey(dimension);
        buffer.writeBoolean(clear);
        buffer.writeBoolean(notifyOnClear);
        buffer.writeVarInt(durationTicks);
        int count = Math.min(targets.size(), StructureTestingTerminalBehavior.MAX_HIGHLIGHTS);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            HighlightTarget target = targets.get(i);
            buffer.writeBlockPos(target.pos());
            buffer.writeEnum(target.kind());
            buffer.writeBoolean(target.primary());
        }
    }

    @Override
    public void execute(NetworkEvent.Context context) {
        if (context.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            return;
        }
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> StructureHighlightRenderer.handlePacket(dimension, clear, notifyOnClear, durationTicks, targets));
    }
}
