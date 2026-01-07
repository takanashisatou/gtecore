package org.satou.gtecore.common.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import com.gregtechceu.gtceu.common.data.models.GTModels;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.lowdragmc.lowdraglib.test.TestJava;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.client.model.generators.ModelFile;
import org.satou.gtecore.GTECore;
import org.satou.gtecore.api.registry.GTECoreRegistration;
import org.satou.gtecore.common.GTECoilBlock;

import java.util.function.Supplier;

import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;
import static org.satou.gtecore.api.registry.GTECoreRegistration.GTECore_REGISTRATE;
import static org.satou.gtecore.common.data.GTECreativeModeTabs.MORE_MACHINES;

public class GTEBlocks {
    static {
        GTECore_REGISTRATE.creativeModeTab(()->MORE_MACHINES);
    }
    public static BlockEntry<Block> SUPER_STRING_CASING = createCasingBlock("super_string_casing",GTECore.id("block/casings/super_string_casing/super_string_casing"));
    public static BlockEntry<Block> EIGHT_TRIGMAS_CASING = createCasingBlock("eight_trigmas_casing",GTECore.id("block/casings/eight_trigmas/eight_trigmas_casing"));
    public static BlockEntry<GTECoilBlock> YIN_YANG_COIL = createCoilBlock(GTECoilBlock.GTECoilType.YIN_YANG_COIL);
    public static BlockEntry<Block> KAN_SHUI_CASING = createCasingBlock("kan_shui_casing",GTECore.id("block/casings/uhv/kan_shui_casing"));
    public static BlockEntry<Block> KUN_GEN_CASING = createCasingBlock("kun_gen_casing",GTECore.id("block/casings/uhv/kun_gen_casing"));
    public static BlockEntry<Block> LI_HUO_CASING = createCasingBlock("li_huo_casing",GTECore.id("block/casings/uhv/li_huo_casing"));

    public static BlockEntry<Block> BASE_DARK_CONCRETE = createCasingBlock("base_dark_concrete",GTECore.id("block/casings/concrete/base_dark_concrete"));
    public static BlockEntry<Block> BASE_LIGHT_CONCRETE = createCasingBlock("base_light_concrete",GTECore.id("block/casings/concrete/base_light_concrete"));
    public static BlockEntry<Block> BASE_MID_CONCRETE = createCasingBlock("base_mid_concrete",GTECore.id("block/casings/concrete/base_mid_concrete"));

    public static BlockEntry<Block> YIN_YANG_FIELD_RESTRICTION = createCasingBlock("yin_yang_field_restriction",GTECore.id("block/casings/yin_yang_field_restriction/yin_yang_field_restriction"));
    public static BlockEntry<Block> createCasingBlock(String name, ResourceLocation texture) {
        return createCasingBlock(name, Block::new, texture, () -> Blocks.IRON_BLOCK,
                () -> RenderType::solid);
    }
    private static BlockEntry<GTECoilBlock> createCoilBlock(ICoilType coilType) {
        //GTCEuAPI.HEATING_COILS.put(coilType, coilBlock);
        return GTECore_REGISTRATE
                .block("%s_coil_block".formatted(coilType.getName()), p -> new GTECoilBlock(p, coilType))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .addLayer(() -> RenderType::cutoutMipped)
                .blockstate(GTEBlocks.createCoilModel(coilType))
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .item(BlockItem::new)
                .build()
                .register();
    }
    public static NonNullBiConsumer<DataGenContext<Block, GTECoilBlock>, RegistrateBlockstateProvider> createCoilModel(ICoilType coilType) {
        return (ctx, prov) -> {
            String name = ctx.getName();
            ActiveBlock block = ctx.getEntry();
            ModelFile inactive = prov.models().cubeAll(name, coilType.getTexture());
            ModelFile active = prov.models().withExistingParent(name + "_active", GTCEu.id("block/cube_2_layer/all"))
                    .texture("bot_all", coilType.getTexture())
                    .texture("top_all", coilType.getTexture().withSuffix("_bloom"));
            prov.getVariantBuilder(block)
                    .partialState().with(GTBlockStateProperties.ACTIVE, false).modelForState().modelFile(inactive)
                    .addModel()
                    .partialState().with(GTBlockStateProperties.ACTIVE, true).modelForState().modelFile(active)
                    .addModel();
        };
    }
    public static BlockEntry<Block> createCasingBlock(String name,
                                                      NonNullFunction<BlockBehaviour.Properties, Block> blockSupplier,
                                                      ResourceLocation texture,
                                                      NonNullSupplier<? extends Block> properties,
                                                      Supplier<Supplier<RenderType>> type) {
        return GTECore_REGISTRATE.block(name, blockSupplier)
                .initialProperties(properties)
                .properties(p -> p.isValidSpawn((state, level, pos, ent) -> false))
                .addLayer(type)
                .exBlockstate(GTModels.cubeAllModel(texture))
                .tag(CustomTags.MINEABLE_WITH_CONFIG_VALID_PICKAXE_WRENCH)
                .item(BlockItem::new)
                .build()
                .register();
    }
    public static void init() {}
}

