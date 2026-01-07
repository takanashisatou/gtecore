package org.satou.gtecore.client;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.*;
import com.gregtechceu.gtceu.client.HudGuiOverlay;
import com.gregtechceu.gtceu.client.PonderInit;
import com.gregtechceu.gtceu.client.TooltipsHandler;
import com.gregtechceu.gtceu.client.model.item.FacadeUnbakedModel;
import com.gregtechceu.gtceu.client.model.machine.MachineModelLoader;
import com.gregtechceu.gtceu.client.particle.HazardParticle;
import com.gregtechceu.gtceu.client.particle.MufflerParticle;
import com.gregtechceu.gtceu.client.renderer.entity.GTBoatRenderer;
import com.gregtechceu.gtceu.client.renderer.entity.GTExplosiveRenderer;
import com.gregtechceu.gtceu.client.renderer.item.decorator.GTComponentItemDecorator;
import com.gregtechceu.gtceu.client.renderer.item.decorator.GTLampItemOverlayRenderer;
import com.gregtechceu.gtceu.client.renderer.item.decorator.GTTankItemFluidPreview;
import com.gregtechceu.gtceu.client.renderer.item.decorator.GTToolBarRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.client.renderer.machine.impl.FluidAreaRender;
import com.gregtechceu.gtceu.common.data.GTBlockEntities;
import com.gregtechceu.gtceu.common.data.GTEntityTypes;
import com.gregtechceu.gtceu.common.data.GTParticleTypes;
import com.gregtechceu.gtceu.common.entity.GTBoat;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.map.ftbchunks.FTBChunksPlugin;
import com.gregtechceu.gtceu.utils.input.KeyBind;
import com.gregtechceu.gtceu.utils.input.SyncedKeyMapping;
import net.minecraft.client.model.BoatModel;
import net.minecraft.client.model.ChestBoatModel;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.satou.gtecore.GTECore;
import org.satou.gtecore.common.CommonProxy;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy {

    public ClientProxy() {
        super();
        init();
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(ClientProxy::clientSetup);

    }
    @SubscribeEvent
    public static void onTooltipEvent(ItemTooltipEvent event) {
        TooltipsHandler.appendTooltips(event.getItemStack(), event.getFlags(), event.getToolTip());
    }
    private static void init() {
        initializeDynamicRendersGTE();
    }
    @SubscribeEvent
    public void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GTEntityTypes.DYNAMITE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(GTEntityTypes.POWDERBARREL.get(), GTExplosiveRenderer::new);
        event.registerEntityRenderer(GTEntityTypes.INDUSTRIAL_TNT.get(), GTExplosiveRenderer::new);

        event.registerBlockEntityRenderer(GTBlockEntities.GT_SIGN.get(), SignRenderer::new);
        event.registerBlockEntityRenderer(GTBlockEntities.GT_HANGING_SIGN.get(), HangingSignRenderer::new);

        event.registerEntityRenderer(GTEntityTypes.BOAT.get(), c -> new GTBoatRenderer(c, false));
        event.registerEntityRenderer(GTEntityTypes.CHEST_BOAT.get(), c -> new GTBoatRenderer(c, true));

        for (var type : GTBoat.BoatType.values()) {
            ForgeHooksClient.registerLayerDefinition(GTBoatRenderer.getBoatModelName(type), BoatModel::createBodyModel);
            ForgeHooksClient.registerLayerDefinition(GTBoatRenderer.getChestBoatModelName(type),
                    ChestBoatModel::createBodyModel);
        }
    }

    @SubscribeEvent
    public void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof IComponentItem) {
                event.register(item, GTComponentItemDecorator.INSTANCE);
            }
            if (item instanceof IGTTool) {
                event.register(item, GTToolBarRenderer.INSTANCE);
            }
            if (item instanceof LampBlockItem) {
                event.register(item, GTLampItemOverlayRenderer.INSTANCE);
            }
            if (item instanceof DrumMachineItem) {
                event.register(item, GTTankItemFluidPreview.DRUM);
            }
            if (item instanceof QuantumTankMachineItem) {
                event.register(item, GTTankItemFluidPreview.QUANTUM_TANK);
            }
        }
    }

    @SubscribeEvent
    public void registerKeyBindings(RegisterKeyMappingsEvent event) {
        KeyBind.onRegisterKeyBinds(event);
        SyncedKeyMapping.onRegisterKeyBinds(event);
    }

    @SubscribeEvent
    public void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("hud", new HudGuiOverlay());
    }

    @SubscribeEvent
    public void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(GTParticleTypes.HAZARD_PARTICLE.get(), HazardParticle.Provider::new);
        event.registerSpriteSet(GTParticleTypes.MUFFLER_PARTICLE.get(), MufflerParticle.Provider::new);
    }
    @SubscribeEvent
    public void onRegisterModelLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(MachineModelLoader.ID.getPath(), MachineModelLoader.INSTANCE);
        event.register("facade", FacadeUnbakedModel.Loader.INSTANCE);
    }
    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
        PonderInit.init();
        MachineOwner.init();
        if (ConfigHolder.INSTANCE.compat.minimap.toggle.ftbChunksIntegration &&
                GTCEu.isModLoaded(GTValues.MODID_FTB_CHUNKS)) {
            FTBChunksPlugin.addEventListeners();
        }
    }
    private static void initializeDynamicRendersGTE() {
        //DynamicRenderManager.register(GTECore.id("fluid_area"), FluidAreaRender.TYPE);
    }

    private static void clientSetup(FMLClientSetupEvent event) {}
}
