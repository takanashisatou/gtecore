package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** UEV water treatment: a batch needs both its chemical residence time and its UV dose. */
public class UvPurificationUnitMachine extends LinkedPurificationUnitMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            UvPurificationUnitMachine.class, LinkedPurificationUnitMachine.MANAGED_FIELD_HOLDER);
    private static final String PLAN_TAG = "gteUvPlan";
    private static final String STATE_TAG = "gtePurificationUv";

    private final PurificationUvState uv = new PurificationUvState();
    @Persisted @DescSynced private int powerPercent = 100;
    @DescSynced private long deliveredDose;
    @DescSynced private long requiredDose;

    public UvPurificationUnitMachine(IMachineBlockEntity holder) {
        // LuV remains the existing recipe-stage tag; this stage serves the UEV Imaginary line.
        super(holder, GTValues.LuV, GTValues.UEV);
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new UvRecipeLogic(this);
    }

    public PurificationUvState getUvState() {
        return uv;
    }

    public int getLampCount() {
        return (int) getParts().stream().filter(UvLampHatchPartMachine.class::isInstance).count();
    }

    public int getPowerPercent() {
        return powerPercent;
    }

    /** The active batch keeps its plan, duration and EU/t until it has finished. */
    public void setPowerPercent(int percent) {
        if (percent < 25 || percent > 100 || percent % 25 != 0) return;
        if (powerPercent == percent) return;
        powerPercent = percent;
        getRecipeLogic().markLastRecipeDirty();
        onChanged();
    }

    @Override
    public boolean regressWhenWaiting() {
        return false;
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        var plan = recipe == null ? null : readPlan(recipe);
        return isFormed() && plan != null && getLampCount() >= plan.lamps() && super.beforeWorking(recipe);
    }

    @Override
    public boolean onWorking() {
        if (!uv.active() || !super.onWorking()) return false;
        uv.tick();
        syncDose();
        return true;
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof UvPurificationUnitMachine unit) || !unit.isFormed()) return ModifierFunction.NULL;
        int lamps = unit.getLampCount();
        int dose = recipe.data.getInt("uvDose");
        if (lamps < 1 || lamps > PurificationUvState.MAX_LAMPS || dose <= 0 || recipe.duration <= 0) return ModifierFunction.NULL;
        int parallel = unit.computeParallel(recipe);
        if (parallel <= 0) return ModifierFunction.NULL;
        final PurificationUvState.Plan plan;
        try {
            plan = PurificationUvState.plan(parallel, lamps, unit.powerPercent, recipe.duration, dose);
        } catch (IllegalArgumentException unsupportedDose) {
            return ModifierFunction.NULL;
        }
        var contents = ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(parallel))
                .outputModifier(ContentModifier.multiplier(parallel))
                .eutMultiplier(parallel * unit.powerPercent / 100.0)
                .parallels(parallel)
                .build();
        return original -> {
            var modified = contents.apply(original);
            modified.duration = plan.durationTicks();
            // ModifierFunction shares the source data by default. Never add a batch plan to the cached recipe.
            modified.data = original.data.copy();
            modified.data.put(PLAN_TAG, writePlan(plan));
            return modified;
        };
    }

    private static CompoundTag writePlan(PurificationUvState.Plan plan) {
        var tag = new CompoundTag();
        tag.putInt("parallel", plan.parallels());
        tag.putInt("lamps", plan.lamps());
        tag.putInt("power", plan.powerPercent());
        tag.putInt("baseTicks", plan.baseDurationTicks());
        tag.putInt("dose", plan.dosePerParallel());
        return tag;
    }

    private static PurificationUvState.Plan readPlanTag(CompoundTag tag) {
        return PurificationUvState.plan(tag.getInt("parallel"), tag.getInt("lamps"), tag.getInt("power"),
                tag.getInt("baseTicks"), tag.getInt("dose"));
    }

    static @Nullable PurificationUvState.Plan readPlan(GTRecipe recipe) {
        if (!recipe.data.contains(PLAN_TAG, Tag.TAG_COMPOUND)) return null;
        try {
            var plan = readPlanTag(recipe.data.getCompound(PLAN_TAG));
            return recipe.parallels == plan.parallels() && recipe.duration >= plan.durationTicks() ? plan : null;
        } catch (IllegalArgumentException | ArithmeticException ignored) {
            return null;
        }
    }

    private void syncDose() {
        deliveredDose = uv.deliveredDose();
        requiredDose = uv.requiredDose();
        onChanged();
    }

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        var state = new CompoundTag();
        if (uv.active()) {
            state.put("plan", writePlan(uv.plan()));
            state.putLong("delivered", uv.deliveredDose());
        }
        tag.put(STATE_TAG, state);
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        if (powerPercent < 25 || powerPercent > 100 || powerPercent % 25 != 0) powerPercent = 100;
        uv.clear();
        var state = tag.getCompound(STATE_TAG);
        if (state.contains("plan", Tag.TAG_COMPOUND)) {
            try {
                uv.restore(new PurificationUvState.Snapshot(readPlanTag(state.getCompound("plan")),
                        state.getLong("delivered")));
            } catch (IllegalArgumentException | ArithmeticException ignored) {
                // Invalid or old batches must not become free product. The recipe logic will discard them.
                uv.clear();
            }
        }
        syncDose();
    }

    /** Retains the linked controller's tick gate and commits completed outputs exactly once. */
    public static class UvRecipeLogic extends LinkedRecipeLogic {
        private final UvPurificationUnitMachine uvUnit;

        protected UvRecipeLogic(UvPurificationUnitMachine unit) {
            super(unit);
            uvUnit = unit;
        }

        @Override
        public void setupRecipe(GTRecipe recipe) {
            var plan = readPlan(recipe);
            if (plan == null) return;
            super.setupRecipe(recipe);
            if (lastRecipe == recipe && isWorking() && progress == 0) {
                uvUnit.uv.begin(plan);
                uvUnit.syncDose();
            }
        }

        private boolean batchAvailable() {
            if (!uvUnit.isFormed() || uvUnit.getPlant() == null || !uvUnit.isWorkingEnabled()) {
                setWaiting(Component.translatable("gtecore.water.uv.unlinked"));
                return false;
            }
            var plan = uvUnit.uv.plan();
            if (lastRecipe == null || plan == null || !plan.equals(readPlan(lastRecipe))) {
                // Legacy pre-dose batches cannot be resumed as fully irradiated water.
                interruptRecipe();
                lastRecipe = null;
                lastOriginRecipe = null;
                markLastRecipeDirty();
                return false;
            }
            if (uvUnit.getLampCount() < plan.lamps()) {
                setWaiting(Component.translatable("gtecore.water.uv.no_lamps"));
                return false;
            }
            return true;
        }

        @Override
        public void handleRecipeWorking() {
            if (!batchAvailable()) return;
            var result = RecipeHelper.checkConditions(lastRecipe, this);
            if (result.isSuccess()) result = handleTickRecipe(lastRecipe);
            if (!result.isSuccess()) {
                // A power outage pauses this batch; neither residence time nor UV dose regresses.
                setWaiting(result.reason());
                return;
            }
            setStatus(Status.WORKING);
            if (!uvUnit.onWorking()) {
                interruptRecipe();
                return;
            }
            progress++;
            totalContinuousRunningTime++;
        }

        @Override
        public void onRecipeFinish() {
            if (lastRecipe == null || progress < duration) return;
            if (!batchAvailable()) return;
            if (!uvUnit.uv.complete()) {
                // Defensive recovery for a saved duration/progress mismatch: keep paying for exposure.
                progress = Math.min(progress, duration - 1);
                return;
            }
            var result = RecipeHelper.handleRecipe(uvUnit, lastRecipe, IO.OUT, lastRecipe.outputs,
                    chanceCaches, false, true);
            if (result.isSuccess()) result = handleRecipeIO(lastRecipe, IO.OUT);
            if (!result.isSuccess()) {
                setWaiting(Component.translatable("gtecore.water.uv.output_blocked"));
                return;
            }
            // Base RecipeLogic ignores output failure. Only advance after a successful output commit here.
            uvUnit.afterWorking();
            consecutiveRecipes++;
            runAttempt = 0;
            runDelay = 0;
            progress = 0;
            duration = 0;
            isActive = false;
            lastRecipe = null;
            lastOriginRecipe = null;
            uvUnit.uv.clear();
            uvUnit.syncDose();
            if (isSuspendAfterFinish()) {
                setStatus(Status.SUSPEND);
            } else {
                setStatus(Status.IDLE);
                findAndHandleRecipe();
            }
        }

        @Override
        public void interruptRecipe() {
            super.interruptRecipe();
            uvUnit.uv.clear();
            uvUnit.syncDose();
        }

        @Override
        public void resetRecipeLogic() {
            super.resetRecipeLogic();
            uvUnit.uv.clear();
            uvUnit.syncDose();
        }
    }

    private void addConfigurationText(List<Component> lines) {
        lines.add(Component.translatable("gtecore.water.uv.lamps", getLampCount()).withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("gtecore.water.uv.power", powerPercent));
        String status = !isFormed() ? "unformed" : getPlant() == null ? "unlinked" :
                getLampCount() == 0 ? "no_lamps" : uv.active() ? "processing" : "idle";
        lines.add(Component.translatable("gtecore.water.uv." + status).withStyle(ChatFormatting.GRAY));
    }

    private void addBatchText(List<Component> lines) {
        var plan = uv.plan();
        if (plan != null) {
            lines.add(Component.translatable("gtecore.water.uv.batch", plan.parallels(), plan.powerPercent()));
            lines.add(Component.translatable("gtecore.water.uv.dose", uv.deliveredDose(), uv.requiredDose()));
            lines.add(Component.translatable("gtecore.water.uv.time", getRecipeLogic().getProgress(), getRecipeLogic().getDuration()));
        } else {
            lines.add(Component.translatable("gtecore.water.uv.idle"));
        }
        lines.add(Component.translatable("com.gtecore.tooltips.water_unit.energy", getStoredLinkedEnergy(), getLinkedEnergyCapacity()));
        if (getRecipeLogic().isWaiting()) lines.addAll(getRecipeLogic().getFancyTooltip());
    }

    private ComponentPanelWidget panel(int x, int y, int width, Consumer<List<Component>> supplier) {
        return new ComponentPanelWidget(x, y, supplier).textSupplier(isRemote() ? null : supplier).setMaxWidthLimit(width);
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 300, 240);
        group.setBackground(new ColorRectTexture(0xFF121225));
        group.addWidget(new WidgetGroup(0, 0, 300, 3).setBackground(new ColorRectTexture(0xFFAD8CFF)));
        group.addWidget(new ComponentPanelWidget(10, 11, List.of(
                Component.translatable("gtecore.water.uv.title").withStyle(ChatFormatting.LIGHT_PURPLE))).setMaxWidthLimit(280));
        group.addWidget(panel(12, 34, 276, this::addConfigurationText));
        group.addWidget(new ProgressWidget(() -> requiredDose == 0 ? 0 : (double) deliveredDose / requiredDose,
                12, 82, 276, 7).setProgressTexture(new ColorRectTexture(0xFF30284D), new ColorRectTexture(0xFFB799FF)));
        var batch = new com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup(8, 95, 284, 73);
        batch.addWidget(panel(4, 3, 270, this::addBatchText));
        group.addWidget(batch);
        group.addWidget(new ComponentPanelWidget(12, 174, List.of(
                Component.translatable("gtecore.water.uv.next_batch").withStyle(ChatFormatting.GRAY))).setMaxWidthLimit(276));
        for (int index = 0; index < 4; index++) {
            int percent = (index + 1) * 25;
            // TextTexture formats literal labels on the client, so a visible percent must be escaped.
            group.addWidget(new ButtonWidget(12 + index * 70, 192, 66, 18,
                    new GuiTextureGroup(new ColorRectTexture(0xFF30284D), new TextTexture(percent + "%%")),
                    click -> { if (!click.isRemote) setPowerPercent(percent); }));
        }
        group.addWidget(createDisconnectButton(184, 216, 104));
        return group;
    }
}
