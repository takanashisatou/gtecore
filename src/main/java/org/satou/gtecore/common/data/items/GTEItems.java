package org.satou.gtecore.common.data.items;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.client.renderer.cover.ICoverRenderer;
import com.gregtechceu.gtceu.client.renderer.cover.SimpleCoverRenderer;
import com.gregtechceu.gtceu.common.data.GTCovers;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.CoverPlaceBehavior;
import com.gregtechceu.gtceu.common.item.TerminalBehavior;
import com.gregtechceu.gtceu.common.item.TooltipBehavior;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.hepdd.gtmthings.GTMThings;
import com.hepdd.gtmthings.common.cover.WirelessEnergyReceiveCover;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.satou.gtecore.GTECore;
import org.satou.gtecore.api.registry.GTECoreRegistration;
import org.satou.gtecore.common.data.GTECreativeModeTabs;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Supplier;

import static com.gregtechceu.gtceu.api.GTValues.MAX;
import static com.gregtechceu.gtceu.api.GTValues.VNF;
import static com.gregtechceu.gtceu.common.data.GTItems.attach;
import static com.gregtechceu.gtceu.common.data.machines.GTMachineUtils.ALL_TIERS;
import static com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE;
import static org.satou.gtecore.api.registry.GTECoreRegistration.GTECore_REGISTRATE;
import static org.satou.gtecore.common.data.GTECreativeModeTabs.MORE_ITEMS;

public class GTEItems {
    static{
        GTECore_REGISTRATE.creativeModeTab(()->MORE_ITEMS);
    }
    public static ItemEntry<ComponentItem> SUPER_STRING_PROCESSOR_ZPM = GTECore_REGISTRATE.item("super_string_processor", ComponentItem::create)
            .lang("super string processor").tag(CustomTags.ZPM_CIRCUITS)
            .onRegister(GTItems.attach(new TooltipBehavior(lines->{
                lines.add(Component.translatable("item.gtecore.super_string_processor.tooltip.0"));
                lines.add(Component.translatable("item.gtecore.super_string_processor.tooltip.1"));
            })))
            .register();
    public static ItemEntry<ComponentItem> SUPER_STRING_PROCESSOR_ASSEMBLY_UV = GTECore_REGISTRATE.item("super_string_processor_assembly",ComponentItem::create)
            .lang("super string processor assembly").tag(CustomTags.UV_CIRCUITS)
            .onRegister(GTItems.attach(new TooltipBehavior(lines->{
                lines.add(Component.translatable("item.gtecore.super_string_processor_assembly.tooltip.0"));
                lines.add(Component.translatable("item.gtecore.super_string_processor_assembly.tooltip.1"));
            })))
            .register();
    public static ItemEntry<ComponentItem> SUPER_STRING_PROCESSOR_COMPUTER_UHV = GTECore_REGISTRATE.item("super_string_processor_computer",ComponentItem::create)
            .lang("super string processor computer").tag(CustomTags.UHV_CIRCUITS)
            .onRegister(GTItems.attach(new TooltipBehavior(lines->{
                lines.add(Component.translatable("item.gtecore.super_string_processor_computer.tooltip.0"));
                lines.add(Component.translatable("item.gtecore.super_string_processor_computer.tooltip.1"));
            })))
            .register();
    public static ItemEntry<ComponentItem> SUPER_STRING_PROCESSOR_MAINFRAME_UEV = GTECore_REGISTRATE.item("super_string_processor_mainframe",ComponentItem::create)
            .lang("super string processor mainframe").tag(CustomTags.UEV_CIRCUITS)
            .onRegister(GTItems.attach(new TooltipBehavior(lines->{
                lines.add(Component.translatable("item.gtecore.super_string_processor_mainframe.tooltip.0"));
                lines.add(Component.translatable("item.gtecore.super_string_processor_mainframe.tooltip.1"));
            })))
            .register();
    public static ItemEntry<Item> SUPER_STRING_CIRCUIT_BOARD = GTECore_REGISTRATE.item("super_string_circuit_board",Item::new)
            .lang("super string circuit board")
            .register();
    public static ItemEntry<Item> SUPER_STRING_PRINTED_CIRCUIT_BOARD = GTECore_REGISTRATE.item("super_string_printed_circuit_board",Item::new)
            .lang("super string circuit board")
            .register();
    public static ItemEntry<Item> ORIGINAL_STRING = GTECore_REGISTRATE.item("original_string",Item::new)
            .lang("Original String")
            .register();
    public static ItemEntry<Item> ALPHA_STRING = GTECore_REGISTRATE.item("alpha_string",Item::new)
            .lang("Alpha String")
            .register();
    public static ItemEntry<Item> BETA_STRING = GTECore_REGISTRATE.item("beta_string",Item::new)
            .lang("Beta String")
            .register();
    public static ItemEntry<Item> GAMMA_STRING = GTECore_REGISTRATE.item("gamma_string",Item::new)
            .lang("Gamma String")
            .register();
    public static ItemEntry<ComponentItem> YIN_YANG_PROCESSOR_UV = GTECore_REGISTRATE.item("yin_yang_processor", ComponentItem::create)
            .lang("Yin Yang processor").tag(CustomTags.UV_CIRCUITS)
            .onRegister(GTItems.attach(new TooltipBehavior(lines->{
                lines.add(Component.translatable("item.gtecore.yin_yang_processor.tooltip.0"));
                lines.add(Component.translatable("item.gtecore.yin_yang_processor.tooltip.1"));
            })))
            .register();
    public static ItemEntry<ComponentItem> YIN_YANG_PROCESSOR_ASSEMBLY_UHV = GTECore_REGISTRATE.item("yin_yang_processor_assembly", ComponentItem::create)
            .lang("Yin Yang processor assembly").tag(CustomTags.UHV_CIRCUITS)
            .onRegister(GTItems.attach(new TooltipBehavior(lines->{
                lines.add(Component.translatable("item.gtecore.yin_yang_processor_assembly.tooltip.0"));
                lines.add(Component.translatable("item.gtecore.yin_yang_processor_assembly.tooltip.1"));
            })))
            .register();
    public static ItemEntry<ComponentItem> YIN_YANG_PROCESSOR_COMPUTER_UEV = GTECore_REGISTRATE.item("yin_yang_processor_computer", ComponentItem::create)
            .lang("Yin Yang processor computer").tag(CustomTags.UEV_CIRCUITS)
            .onRegister(GTItems.attach(new TooltipBehavior(lines->{
                lines.add(Component.translatable("item.gtecore.yin_yang_processor_computer.tooltip.0"));
                lines.add(Component.translatable("item.gtecore.yin_yang_processor_computer.tooltip.1"));
            })))
            .register();
    public static ItemEntry<ComponentItem> YIN_YANG_PROCESSOR_MAINFRAME_UIV = GTECore_REGISTRATE.item("yin_yang_processor_mainframe", ComponentItem::create)
            .lang("Yin Yang processor computer").tag(CustomTags.UIV_CIRCUITS)
            .onRegister(GTItems.attach(new TooltipBehavior(lines->{
                lines.add(Component.translatable("item.gtecore.yin_yang_processor_mainframe.tooltip.0"));
                lines.add(Component.translatable("item.gtecore.yin_yang_processor_mainframe.tooltip.1"));
            })))
            .register();
    public static ItemEntry<Item> RUNE_DUI = GTECore_REGISTRATE.item("rune_dui",Item::new)
            .lang("Rune Dui")
            .register();
    public static ItemEntry<Item> RUNE_GEN = GTECore_REGISTRATE.item("rune_gen",Item::new)
            .lang("Rune Gen")
            .register();
    public static ItemEntry<Item> RUNE_KAN = GTECore_REGISTRATE.item("rune_kan",Item::new)
            .lang("Rune Kan")
            .register();
    public static ItemEntry<Item> RUNE_KUN = GTECore_REGISTRATE.item("rune_kun",Item::new)
            .lang("Rune Kun")
            .register();
    public static ItemEntry<Item> RUNE_LI = GTECore_REGISTRATE.item("rune_li",Item::new)
            .lang("Rune Li")
            .register();
    public static ItemEntry<Item> RUNE_QIAN = GTECore_REGISTRATE.item("rune_qian",Item::new)
            .lang("Rune Qian")
            .register();
    public static ItemEntry<Item> RUNE_XUN = GTECore_REGISTRATE.item("rune_xun",Item::new)
            .lang("Rune Xun")
            .register();
    public static ItemEntry<Item> RUNE_ZHEN = GTECore_REGISTRATE.item("rune_zhen",Item::new)
            .lang("Rune Zhen")
            .register();
    public static ItemEntry<Item> DUI_CHIP = GTECore_REGISTRATE.item("dui_chip",Item::new)
            .lang("Dui Chip")
            .register();
    public static ItemEntry<Item> GEN_CHIP = GTECore_REGISTRATE.item("gen_chip",Item::new)
            .lang("Gen Chip")
            .register();
    public static ItemEntry<Item> GOD_NUGGET = GTECore_REGISTRATE.item("god_nugget",Item::new)
            .lang("God Nugget")
            .register();
    public static ItemEntry<Item> SYMBOL_PAPER_FIRE = GTECore_REGISTRATE.item("symbol_paper_fire",Item::new)
            .lang("Symbol Paper of Fire")
            .register();
    public static ItemEntry<Item> SYMBOL_PAPER_WATER = GTECore_REGISTRATE.item("symbol_paper_water",Item::new)
            .lang("Symbol Paper of Water")
            .register();
    public static ItemEntry<Item> SYMBOL_PAPER_EARTH = GTECore_REGISTRATE.item("symbol_paper_earth",Item::new)
            .lang("Symbol Paper of Earth")
            .register();
    public static ItemEntry<Item> SYMBOL_PAPER_WOOD = GTECore_REGISTRATE.item("symbol_paper_wood",Item::new)
            .lang("Symbol Paper of Wood")
            .register();
    public static ItemEntry<Item> SYMBOL_PAPER_GOLD = GTECore_REGISTRATE.item("symbol_paper_gold",Item::new)
            .lang("Symbol Paper of Gold")
            .register();
    public static ItemEntry<Item> YANG = GTECore_REGISTRATE.item("yang",Item::new)
            .lang("Yang")
            .register();
    public static ItemEntry<Item> YING = GTECore_REGISTRATE.item("yin",Item::new)
            .lang("Ying")
            .register();
    public static ItemEntry<Item> YIN_YANG_WAFER = GTECore_REGISTRATE.item("yin_yang_wafer",Item::new)
            .lang("Yin Yang Wafer")
            .register();
    public static ItemEntry<Item> YIN_YANG_GLASS_LENS = GTECore_REGISTRATE.item("yinyang_glass_lens",Item::new)
            .lang("Yin Yang Glass Lens")
            .register();
    public static ItemEntry<Item> YIN_YANG_CIRCUIT_BOARD = GTECore_REGISTRATE.item("yin_yang_circuit_board",Item::new)
            .lang("Yin Yang Circuit Board")
            .register();
    public static ItemEntry<Item> YIN_YANG_CIRCUIT_CHIP = GTECore_REGISTRATE.item("yin_yang_circuit_chip",Item::new)
            .lang("Yin Yang Circuit Chip")
            .register();
    public static ItemEntry<Item> YIN_YANG_BOULE = GTECore_REGISTRATE.item("yin_yang_boule",Item::new)
            .lang("Yin Yang Boule")
            .register();
    public static ItemEntry<Item> YIN_YANG_CPU_WAFER = GTECore_REGISTRATE.item("yin_yang_cpu_wafer",Item::new)
            .lang("Yin Yang CPU Wafer")
            .register();
    public static ItemEntry<ComponentItem> CHECK_STRUCTURE_TERMINAL = GTECore_REGISTRATE.item("check_structure_terminal", ComponentItem::create)
            .lang("Structure Testing Terminal")
            .properties(p -> p.stacksTo(1))
            .onRegister(attach(new StructureTestingTerminalBehavior()))
            .register();
    public static void init() {
    }
}
