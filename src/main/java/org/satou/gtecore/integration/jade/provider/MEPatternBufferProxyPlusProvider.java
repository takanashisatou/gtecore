package org.satou.gtecore.integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.client.util.TooltipHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.satou.gtecore.GTECore;
import org.satou.gtecore.common.data.machines.MEPatternBufferProxyPlusPartMachine;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class MEPatternBufferProxyPlusProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    @Override
    public void appendTooltip(ITooltip iTooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof MEPatternBufferProxyPlusPartMachine) {
                CompoundTag serverData = blockAccessor.getServerData();
                if (!serverData.getBoolean("formed")) return;
                if (!serverData.getBoolean("bound")) {
                    iTooltip.add(Component.translatable("gtecore.top.buffer_not_bound").withStyle(ChatFormatting.RED));
                    return;
                }

                int[] pos = serverData.getIntArray("pos");
                iTooltip.add(Component.translatable("gtecore.top.buffer_bound_pos", pos[0], pos[1], pos[2])
                        .withStyle(TooltipHelper.RAINBOW_HSL_SLOW));

                MEPatternBufferPlusProvider.readBufferTag(iTooltip, serverData);
            }
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof IMachineBlockEntity blockEntity) {
            if (blockEntity.getMetaMachine() instanceof MEPatternBufferProxyPlusPartMachine proxy) {
                if (!proxy.isFormed()) {
                    compoundTag.putBoolean("formed", false);
                    return;
                }
                compoundTag.putBoolean("formed", true);
                var buffer = proxy.getBuffer();
                if (buffer == null) {
                    compoundTag.putBoolean("bound", false);
                    return;
                }
                compoundTag.putBoolean("bound", true);

                var pos = buffer.getPos();
                compoundTag.putIntArray("pos", new int[] { pos.getX(), pos.getY(), pos.getZ() });
                MEPatternBufferPlusProvider.writeBufferTag(compoundTag, buffer);
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return GTECore.id("me_pattern_buffer_proxy_plus");
    }
}
