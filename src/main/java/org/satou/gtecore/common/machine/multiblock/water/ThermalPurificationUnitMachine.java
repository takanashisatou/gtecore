package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.satou.gtecore.common.data.GTEMaterials;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Consumer;

/** First-stage clarifier: redstone temperature control and maintenance-latched failures. */
public class ThermalPurificationUnitMachine extends LinkedPurificationUnitMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ThermalPurificationUnitMachine.class, LinkedPurificationUnitMachine.MANAGED_FIELD_HOLDER);
    private final PurificationThermalState thermal = new PurificationThermalState();
    private @Nullable TickableSubscription thermalSubscription;
    private long lastThermalTick;

    @DescSynced private int temperature = 650;
    @DescSynced private int lower = 600;
    @DescSynced private int upper = 700;
    @DescSynced private int stableTicks;
    @DescSynced private int targetTicks = PurificationThermalState.TARGET_INTERVAL_TICKS;
    @DescSynced private int graceTicks = PurificationThermalState.GRACE_TICKS;
    @DescSynced private boolean faulted;
    @DescSynced private boolean heating;

    public ThermalPurificationUnitMachine(IMachineBlockEntity holder) {
        super(holder, GTValues.EV);
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) { return new ThermalRecipeLogic(this); }

    public PurificationThermalState getThermalState() { return thermal; }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        var state = thermal.snapshot();
        thermal.restore(new PurificationThermalState.Snapshot(state.temperatureDeciC(), state.targetCenterC(),
                state.phaseTicks(), state.outOfRangeTicks(), 0, state.faulted(), state.outputRemainder()));
        syncThermalState();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            lastThermalTick = getLevel().getGameTime() - 1;
            thermalSubscription = subscribeServerTick(thermalSubscription, this::idleThermalTick);
        }
    }

    @Override
    public void onUnload() {
        if (thermalSubscription != null) {
            thermalSubscription.unsubscribe();
            thermalSubscription = null;
        }
        super.onUnload();
    }

    private @Nullable ThermalControlHatchPartMachine controlHatch() {
        for (var part : getParts()) {
            if (part instanceof ThermalControlHatchPartMachine hatch) return hatch;
        }
        return null;
    }

    private @Nullable IMaintenanceMachine maintenanceHatch() {
        for (var part : getParts()) {
            if (part instanceof IMaintenanceMachine maintenance && !maintenance.isFullAuto()) return maintenance;
        }
        return null;
    }

    private boolean canProcessThermally() {
        return !thermal.isFaulted() && controlHatch() != null && maintenanceHatch() != null &&
                ConfigHolder.INSTANCE.machines.enableMaintenance;
    }

    private void tryFinishRepair() {
        var maintenance = maintenanceHatch();
        if (thermal.isFaulted() && thermal.isInRange() && maintenance != null &&
                ConfigHolder.INSTANCE.machines.enableMaintenance && !maintenance.hasMaintenanceProblems()) {
            thermal.clearFault();
            getRecipeLogic().markLastRecipeDirty();
            syncThermalState();
        }
    }

    private void advanceThermal(boolean working) {
        var hatch = controlHatch();
        heating = hatch != null && hatch.isHeating();
        boolean newFault = thermal.tick(working, heating, GTValues.RNG.nextInt());
        if (newFault) {
            var maintenance = maintenanceHatch();
            if (maintenance != null) {
                maintenance.causeRandomMaintenanceProblems();
                maintenance.setTaped(false);
                maintenance.self().onChanged();
            }
            // The caller returns false from onWorking, so GTM interrupts the
            // consumed batch without producing any of its outputs.
            getRecipeLogic().markLastRecipeDirty();
        }
        syncThermalState();
    }

    /** Process only completed idle ticks; subscription ordering cannot double-count a working tick. */
    private void catchUpIdle(long now) {
        if (lastThermalTick < now - 1) {
            // A loaded machine ticks continuously. Do not simulate time spent unloaded.
            advanceThermal(false);
            lastThermalTick = now - 1;
        }
    }

    private void idleThermalTick() {
        if (!isFormed()) {
            lastThermalTick = getLevel().getGameTime() - 1;
            return;
        }
        catchUpIdle(getLevel().getGameTime());
        tryFinishRepair();
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        tryFinishRepair();
        return canProcessThermally() && super.beforeWorking(recipe);
    }

    @Override
    public boolean onWorking() {
        if (!canProcessThermally() || !super.onWorking()) return false;
        long now = getLevel().getGameTime();
        catchUpIdle(now);
        if (lastThermalTick != now) {
            advanceThermal(true);
            lastThermalTick = now;
        }
        return !thermal.isFaulted();
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof ThermalPurificationUnitMachine unit) || !unit.canProcessThermally()) {
            return ModifierFunction.NULL;
        }
        return LinkedPurificationUnitMachine.recipeModifier(machine, recipe);
    }

    private void syncThermalState() {
        temperature = thermal.temperatureDeciC();
        lower = thermal.lowerDeciC();
        upper = thermal.upperDeciC();
        stableTicks = thermal.stableTicks();
        faulted = thermal.isFaulted();
        targetTicks = PurificationThermalState.TARGET_INTERVAL_TICKS - thermal.phaseTicks();
        graceTicks = Math.max(0, PurificationThermalState.GRACE_TICKS - thermal.outOfRangeTicks());
        onChanged();
    }

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        var state = thermal.snapshot();
        var data = new CompoundTag();
        data.putInt("temperature", state.temperatureDeciC());
        data.putInt("center", state.targetCenterC());
        data.putInt("phase", state.phaseTicks());
        data.putInt("outside", state.outOfRangeTicks());
        data.putInt("stable", state.stableTicks());
        data.putBoolean("fault", state.faulted());
        data.putInt("remainder", state.outputRemainder());
        tag.put("gtePurificationThermal", data);
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        if (!tag.contains("gtePurificationThermal", Tag.TAG_COMPOUND)) return;
        var data = tag.getCompound("gtePurificationThermal");
        thermal.restore(new PurificationThermalState.Snapshot(data.getInt("temperature"), data.getInt("center"),
                data.getInt("phase"), data.getInt("outside"), data.getInt("stable"),
                data.getBoolean("fault"), data.getInt("remainder")));
        syncThermalState();
    }

    /** Scale only finished purified water, never cached recipes, inputs or by-products. */
    protected static class ThermalRecipeLogic extends LinkedRecipeLogic {
        private final ThermalPurificationUnitMachine unit;

        protected ThermalRecipeLogic(ThermalPurificationUnitMachine unit) {
            super(unit);
            this.unit = unit;
        }

        @Override
        public void handleRecipeWorking() {
            if (!unit.canProcessThermally()) {
                setWaiting(Component.translatable("gtecore.water.thermal.fault_hint"));
                return;
            }
            super.handleRecipeWorking();
        }

        @Override
        protected ActionResult handleRecipeIO(GTRecipe recipe, IO io) {
            if (io != IO.OUT) return super.handleRecipeIO(recipe, io);
            if (unit.thermal.isFaulted()) return ActionResult.FAIL_NO_REASON;
            var snapshot = unit.thermal.snapshot();
            var output = recipe.copy();
            var fluids = output.outputs.get(FluidRecipeCapability.CAP);
            if (fluids != null) {
                // GTRecipe.copy(IDENTITY) copies only the maps, not their lists
                // or mutable fluid ingredients. Detach both before scaling.
                fluids = new ArrayList<>(fluids.stream().map(content -> content.copy(FluidRecipeCapability.CAP)).toList());
                output.outputs.put(FluidRecipeCapability.CAP, fluids);
                fluids.removeIf(content -> {
                    if (content.getContent() instanceof FluidIngredient fluid &&
                            fluid.test(GTEMaterials.DistilledPurifiedWater.getFluid(1))) {
                        int amount = unit.thermal.scaleOutput(fluid.getAmount());
                        if (amount == 0) return true;
                        fluid.setAmount(amount);
                    }
                    return false;
                });
            }
            var result = super.handleRecipeIO(output, io);
            if (!result.isSuccess()) unit.thermal.restore(snapshot);
            unit.syncThermalState();
            return result;
        }
    }

    private static String degrees(int deciC) { return String.format(Locale.ROOT, "%.1f", deciC / 10.0); }

    private void addThermalSummary(List<Component> lines) {
        lines.add(Component.translatable("gtecore.water.thermal.temperature", degrees(temperature))
                .withStyle(temperature >= lower && temperature <= upper ? ChatFormatting.AQUA : ChatFormatting.GOLD));
        lines.add(Component.translatable("gtecore.water.thermal.range", degrees(lower), degrees(upper)));
        lines.add(Component.translatable(heating ? "gtecore.water.thermal_hatch.heating" :
                "gtecore.water.thermal_hatch.cooling").withStyle(heating ? ChatFormatting.RED : ChatFormatting.AQUA));
    }

    private void addEfficiencySummary(List<Component> lines) {
        lines.add(Component.translatable("gtecore.water.thermal.yield",
                String.format(Locale.ROOT, "%.1f", stableTicks / 120.0)).withStyle(ChatFormatting.GREEN));
        lines.add(Component.translatable("gtecore.water.thermal.stable", stableTicks / 20));
        lines.add(Component.translatable("gtecore.water.thermal.next_target", (targetTicks + 19) / 20));
    }

    private void addThermalStatus(List<Component> lines) {
        if (!isFormed()) {
            lines.add(Component.translatable("com.gtecore.gui.water_plant.incomplete").withStyle(ChatFormatting.RED));
        } else if (!ConfigHolder.INSTANCE.machines.enableMaintenance) {
            lines.add(Component.translatable("gtecore.water.thermal.maintenance_disabled").withStyle(ChatFormatting.RED));
        } else if (faulted) {
            lines.add(Component.translatable("gtecore.water.thermal.fault_hint").withStyle(ChatFormatting.RED));
        } else if (maintenanceHatch() != null && maintenanceHatch().hasMaintenanceProblems()) {
            lines.add(Component.translatable("gtecore.water.thermal.maintenance_required").withStyle(ChatFormatting.RED));
        } else if (getPlant() == null) {
            lines.add(Component.translatable("com.gtecore.tooltips.water_unit.unlinked"));
        } else if (temperature < lower || temperature > upper) {
            lines.add(Component.translatable("gtecore.water.thermal.grace", (graceTicks + 19) / 20)
                    .withStyle(ChatFormatting.GOLD));
        } else {
            lines.add(Component.translatable("gtecore.water.thermal.in_range").withStyle(ChatFormatting.AQUA));
        }
        lines.add(Component.translatable("gtecore.water.thermal.rule").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gtecore.water.thermal.idle_rule").withStyle(ChatFormatting.GRAY));
    }

    private ComponentPanelWidget panel(int x, int y, int width, Consumer<List<Component>> supplier) {
        return new ComponentPanelWidget(x, y, supplier).textSupplier(isRemote() ? null : supplier)
                .setMaxWidthLimit(width);
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 300, 218);
        group.setBackground(new ColorRectTexture(0xFF0B1622));
        group.addWidget(new ImageWidget(0, 0, 300, 3, new ColorRectTexture(0xFF39D0CD)));
        group.addWidget(panel(10, 10, 280, lines -> lines.add(
                Component.translatable("gtecore.water.thermal.title").withStyle(ChatFormatting.AQUA))));
        group.addWidget(new ImageWidget(8, 29, 284, 51, new ThermalGauge()));
        group.addWidget(new ProgressWidget(getRecipeLogic()::getProgressPercent, 8, 80, 284, 3)
                .setProgressTexture(new ColorRectTexture(0xFF263D50), new ColorRectTexture(0xFF83B9E1)));
        group.addWidget(new ImageWidget(8, 87, 140, 46, new ColorRectTexture(0xFF14283B)));
        group.addWidget(new ImageWidget(152, 87, 140, 46, new ColorRectTexture(0xFF14283B)));
        group.addWidget(panel(14, 92, 128, this::addThermalSummary));
        group.addWidget(panel(158, 92, 128, this::addEfficiencySummary));
        var status = new DraggableScrollableWidgetGroup(8, 139, 284, 46);
        status.addWidget(panel(4, 3, 270, this::addThermalStatus));
        group.addWidget(status);
        group.addWidget(panel(10, 194, 170, lines -> {
            lines.add(Component.translatable("gtecore.water.thermal.link_status",
                    getRequestedParallel()).withStyle(ChatFormatting.GRAY));
        }));
        group.addWidget(createDisconnectButton(188, 192, 104));
        return group;
    }

    private final class ThermalGauge implements IGuiTexture {
        @Override
        @OnlyIn(Dist.CLIENT)
        public void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {
            int left = (int) x;
            int top = (int) y;
            graphics.fill(left, top, left + width, top + height, 0xFF122638);
            int barLeft = left + 10;
            int barWidth = width - 20;
            graphics.fill(barLeft, top + 12, barLeft + barWidth, top + 28, 0xFF263D50);
            graphics.fill(barLeft + lower * barWidth / 1000, top + 12,
                    barLeft + upper * barWidth / 1000, top + 28, 0xFF197E78);
            for (int mark = 0; mark <= 10; mark++) {
                int tickX = barLeft + mark * barWidth / 10;
                graphics.fill(tickX, top + 30, tickX + 1, top + 34, 0xFF64839A);
            }
            int needle = barLeft + temperature * barWidth / 1000;
            graphics.fill(needle - 1, top + 8, needle + 2, top + 31, faulted ? 0xFFFF647C : 0xFFF1FAFF);
            graphics.fill(barLeft, top + 41, barLeft + barWidth, top + 44, 0xFF263D50);
            graphics.fill(barLeft, top + 41, barLeft + stableTicks * barWidth /
                    PurificationThermalState.FULL_EFFICIENCY_TICKS, top + 44, 0xFF39D0CD);
        }
    }
}
