package org.satou.gtecore.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.satou.gtecore.GTECore;
import org.satou.gtecore.common.network.packets.SPacketStructureHighlight;

import java.util.Optional;
import java.util.function.Function;

public final class GTECoreNetwork {

    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            GTECore.id("network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int nextPacketId = 0;
    private static boolean initialized = false;

    private GTECoreNetwork() {}

    public interface INetPacket {

        void encode(FriendlyByteBuf buffer);

        void execute(NetworkEvent.Context context);
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        register(SPacketStructureHighlight.class, SPacketStructureHighlight::new, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static <T extends INetPacket> void register(Class<T> cls, Function<FriendlyByteBuf, T> decode,
                                                       NetworkDirection direction) {
        INSTANCE.registerMessage(nextPacketId++, cls, INetPacket::encode, decode, (msg, ctx) -> {
            NetworkEvent.Context context = ctx.get();
            context.enqueueWork(() -> msg.execute(context));
            context.setPacketHandled(true);
        }, Optional.ofNullable(direction));
    }

    public static void sendToPlayer(ServerPlayer player, INetPacket packet) {
        if (player != null && player.connection != null) {
            INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}
