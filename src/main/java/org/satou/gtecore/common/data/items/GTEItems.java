package org.satou.gtecore.common.data.items;

import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.item.TooltipBehavior;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.satou.gtecore.api.registry.GTECoreRegistration;
import org.satou.gtecore.common.data.GTECreativeModeTabs;

import static com.gregtechceu.gtceu.common.data.GTItems.attach;
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
    public static void init() {
    }
}
