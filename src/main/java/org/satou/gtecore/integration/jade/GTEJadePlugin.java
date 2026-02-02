package org.satou.gtecore.integration.jade;

import com.gregtechceu.gtceu.integration.jade.provider.MEPatternBufferProvider;
import com.gregtechceu.gtceu.integration.jade.provider.MEPatternBufferProxyProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.satou.gtecore.integration.jade.provider.MEPatternBufferPlusProvider;
import org.satou.gtecore.integration.jade.provider.MEPatternBufferProxyPlusProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class GTEJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new MEPatternBufferPlusProvider(), BlockEntity.class);
        registration.registerBlockDataProvider(new MEPatternBufferProxyPlusProvider(), BlockEntity.class);
    }
    @Override
    public void registerClient(IWailaClientRegistration registration){
        registration.registerBlockComponent(new MEPatternBufferPlusProvider(), Block.class);
        registration.registerBlockComponent(new MEPatternBufferProxyPlusProvider(), Block.class);

    }
}
