package org.satou.gtecore.common.machine.multiblock.water;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.satou.gtecore.common.data.GTEMaterials;

import java.util.List;
import java.util.Iterator;
import java.util.function.Consumer;

/** Third water stage: raw load feedback and externally selected, batch-boundary regeneration. */
public class EdiPurificationUnitMachine extends LinkedPurificationUnitMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            EdiPurificationUnitMachine.class, LinkedPurificationUnitMachine.MANAGED_FIELD_HOLDER);
    private static final String STATE_TAG = "gtePurificationEdi";
    private static final String BATCH_TAG = "gteEdiBatch";
    private final PurificationEdiState edi = new PurificationEdiState();
    private @Nullable Batch batch;
    @DescSynced private long displayedLoad;

    public EdiPurificationUnitMachine(IMachineBlockEntity holder) {
        super(holder, GTValues.ZPM, GTValues.UEV);
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new EdiRecipeLogic(this);
    }

    public PurificationEdiState getEdiState() {
        return edi;
    }

    public @Nullable Batch getBatch() {
        return batch;
    }

    private @Nullable EdiRegenerationControlHatchPartMachine controlHatch() {
        for (var part : getParts()) {
            if (part instanceof EdiRegenerationControlHatchPartMachine hatch) return hatch;
        }
        return null;
    }

    public boolean isRegenerationRequested() {
        var control = controlHatch();
        return control != null && control.isRegenerationRequested();
    }

    @Override
    public boolean regressWhenWaiting() {
        return false;
    }

    private boolean canCommit(Batch planned) {
        return planned.regeneration() ? edi.load() >= planned.load() : edi.remainingCapacity() >= planned.load();
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        var planned = recipe == null ? null : readBatch(recipe);
        return isFormed() && controlHatch() != null && planned != null &&
                isRegenerationRequested() == planned.regeneration() && canCommit(planned) && super.beforeWorking(recipe);
    }

    private static long loadPerParallel(GTRecipe recipe) {
        if (!recipe.data.contains("ediRegeneration", Tag.TAG_BYTE)) return 0;
        if (recipe.data.getBoolean("ediRegeneration")) {
            // A regeneration recipe is never an alternate, cheaper source of purified water.
            return recipe.outputs.isEmpty() && recipe.tickOutputs.isEmpty() ? recipe.data.getInt("ediLoad") : 0;
        }
        if (!recipe.tickOutputs.isEmpty()) return 0;
        for (var content : recipe.getOutputContents(FluidRecipeCapability.CAP)) {
            var fluid = FluidRecipeCapability.CAP.of(content.getContent());
            if (fluid.test(GTEMaterials.UltrapureWater.getFluid(1)) && content.chance != content.maxChance) return 0;
        }
        // Charge the actual finished water, including small custom batches; never round by redstone level.
        return RecipeHelper.getOutputFluids(recipe).stream()
                .filter(fluid -> fluid.isFluidEqual(GTEMaterials.UltrapureWater.getFluid(1)))
                .mapToLong(fluid -> fluid.getAmount()).sum();
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof EdiPurificationUnitMachine unit) || !unit.isFormed() || unit.controlHatch() == null) {
            return ModifierFunction.NULL;
        }
        boolean regeneration = recipe.data.getBoolean("ediRegeneration");
        long perParallel = loadPerParallel(recipe);
        if (perParallel <= 0 || recipe.duration <= 0 || regeneration != unit.isRegenerationRequested()) {
            return ModifierFunction.NULL;
        }
        int parallel = unit.computeParallel(recipe);
        parallel = regeneration ? unit.edi.limitRegenerationParallel(perParallel, parallel) :
                unit.edi.limitProductionParallel(perParallel, parallel);
        if (parallel <= 0) return ModifierFunction.NULL;
        long load = perParallel * parallel;
        var planned = new Batch(regeneration, regeneration ? Math.min(load, unit.edi.load()) : load, parallel);
        var contents = ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(parallel))
                .outputModifier(ContentModifier.multiplier(parallel))
                .eutMultiplier(parallel).parallels(parallel).build();
        return original -> {
            var modified = contents.apply(original);
            modified.data = original.data.copy();
            modified.data.put(BATCH_TAG, writeBatch(planned));
            return modified;
        };
    }

    public record Batch(boolean regeneration, long load, int parallels) {
        public Batch {
            if (load <= 0 || load > PurificationEdiState.CAPACITY || parallels <= 0) {
                throw new IllegalArgumentException("Invalid EDI batch");
            }
        }
    }

    private static CompoundTag writeBatch(Batch batch) {
        var tag = new CompoundTag();
        tag.putBoolean("regeneration", batch.regeneration());
        tag.putLong("load", batch.load());
        tag.putInt("parallel", batch.parallels());
        return tag;
    }

    private static Batch readBatchTag(CompoundTag tag) {
        return new Batch(tag.getBoolean("regeneration"), tag.getLong("load"), tag.getInt("parallel"));
    }

    static @Nullable Batch readBatch(GTRecipe recipe) {
        if (!recipe.data.contains(BATCH_TAG, Tag.TAG_COMPOUND)) return null;
        try {
            var planned = readBatchTag(recipe.data.getCompound(BATCH_TAG));
            return planned.parallels() == recipe.parallels &&
                    planned.regeneration() == recipe.data.getBoolean("ediRegeneration") ? planned : null;
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private void syncLoad() {
        displayedLoad = edi.load();
        onChanged();
    }

    @Override
    public void saveCustomPersistedData(@NotNull CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        var state = new CompoundTag();
        state.putLong("load", edi.load());
        if (batch != null) state.put("batch", writeBatch(batch));
        tag.put(STATE_TAG, state);
    }

    @Override
    public void loadCustomPersistedData(@NotNull CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        var state = tag.getCompound(STATE_TAG);
        long savedLoad = state.getLong("load");
        edi.restore(new PurificationEdiState.Snapshot(Math.clamp(savedLoad, 0L, PurificationEdiState.CAPACITY)));
        batch = null;
        if (state.contains("batch", Tag.TAG_COMPOUND)) {
            try {
                batch = readBatchTag(state.getCompound("batch"));
            } catch (IllegalArgumentException invalid) {
                // An unaccounted legacy/corrupt batch cannot produce water or clean the membrane for free.
                batch = null;
            }
        }
        syncLoad();
    }

    public static class EdiRecipeLogic extends LinkedRecipeLogic {
        private final EdiPurificationUnitMachine ediUnit;

        protected EdiRecipeLogic(EdiPurificationUnitMachine unit) {
            super(unit);
            ediUnit = unit;
        }

        @Override
        public @NotNull Iterator<GTRecipe> searchRecipe() {
            // This small recipe type shares fluid inputs across modes. Its ingredient tree can mask a
            // valid mode even when direct matching succeeds, so select from the live registry instead.
            boolean regeneration = ediUnit.isRegenerationRequested();
            return getRecipeManager().getAllRecipesFor(ediUnit.getRecipeType()).stream()
                    .filter(ediUnit::acceptsPurificationRecipe)
                    .filter(recipe -> loadPerParallel(recipe) > 0 &&
                            recipe.data.getBoolean("ediRegeneration") == regeneration)
                    .filter(recipe -> matchRecipe(recipe).isSuccess()).iterator();
        }

        @Override
        public void setupRecipe(GTRecipe recipe) {
            var planned = readBatch(recipe);
            if (planned == null) return;
            super.setupRecipe(recipe);
            if (lastRecipe == recipe && isWorking() && progress == 0) {
                ediUnit.batch = planned;
                ediUnit.syncLoad();
            }
        }

        private boolean batchAvailable() {
            if (!ediUnit.isFormed() || ediUnit.getPlant() == null || !ediUnit.isWorkingEnabled()) {
                setWaiting(Component.translatable("gtecore.water.edi.unlinked"));
                return false;
            }
            if (lastRecipe == null || ediUnit.batch == null || !ediUnit.batch.equals(readBatch(lastRecipe)) ||
                    !ediUnit.canCommit(ediUnit.batch)) {
                interruptRecipe();
                lastRecipe = null;
                lastOriginRecipe = null;
                markLastRecipeDirty();
                return false;
            }
            // The current redstone request deliberately does not participate in this check.
            return true;
        }

        @Override
        public void handleRecipeWorking() {
            if (!batchAvailable()) return;
            var result = RecipeHelper.checkConditions(lastRecipe, this);
            if (result.isSuccess()) result = handleTickRecipe(lastRecipe);
            if (!result.isSuccess()) {
                setWaiting(result.reason());
                return;
            }
            setStatus(Status.WORKING);
            if (!ediUnit.onWorking()) {
                interruptRecipe();
                return;
            }
            progress++;
            totalContinuousRunningTime++;
        }

        @Override
        public void onRecipeFinish() {
            if (lastRecipe == null || progress < duration || !batchAvailable()) return;
            var result = RecipeHelper.handleRecipe(ediUnit, lastRecipe, IO.OUT, lastRecipe.outputs,
                    chanceCaches, false, true);
            if (result.isSuccess()) result = handleRecipeIO(lastRecipe, IO.OUT);
            if (!result.isSuccess()) {
                setWaiting(Component.translatable("gtecore.water.edi.output_blocked"));
                return;
            }
            if (ediUnit.batch.regeneration()) ediUnit.edi.regenerate(ediUnit.batch.load());
            else ediUnit.edi.produced(ediUnit.batch.load());
            ediUnit.afterWorking();
            ediUnit.batch = null;
            ediUnit.syncLoad();
            runAttempt = 0;
            runDelay = 0;
            consecutiveRecipes++;
            progress = 0;
            duration = 0;
            isActive = false;
            lastRecipe = null;
            lastOriginRecipe = null;
            if (isSuspendAfterFinish()) setStatus(Status.SUSPEND);
            else {
                setStatus(Status.IDLE);
                findAndHandleRecipe();
            }
        }

        @Override
        public void interruptRecipe() {
            super.interruptRecipe();
            ediUnit.batch = null;
            ediUnit.syncLoad();
        }

        @Override
        public void resetRecipeLogic() {
            super.resetRecipeLogic();
            // Taking apart the structure cancels its in-flight batch, never its membrane load.
            ediUnit.batch = null;
            ediUnit.syncLoad();
        }
    }

    private static Component mode(boolean regeneration) {
        return Component.translatable("gtecore.water.edi." + (regeneration ? "regeneration" : "production"));
    }

    private void addLoadText(List<Component> lines) {
        lines.add(Component.translatable("gtecore.water.edi.load", edi.load(), PurificationEdiState.CAPACITY));
        lines.add(Component.translatable("gtecore.water.edi.signal", edi.signal()).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("gtecore.water.edi.requested", mode(isRegenerationRequested())));
    }

    private void addBatchText(List<Component> lines) {
        if (batch != null) {
            lines.add(Component.translatable("gtecore.water.edi.active", mode(batch.regeneration()), batch.parallels()));
            lines.add(Component.translatable("gtecore.water.edi.time", getRecipeLogic().getProgress(), getRecipeLogic().getDuration()));
        }
        String status = !isFormed() ? "unformed" : getPlant() == null ? "unlinked" :
                batch != null ? "processing" : isRegenerationRequested() && edi.load() == 0 ? "clean" :
                !isRegenerationRequested() && edi.remainingCapacity() == 0 ? "full" : "waiting";
        lines.add(Component.translatable("gtecore.water.edi." + status));
        lines.add(Component.translatable("com.gtecore.tooltips.water_unit.energy", getStoredLinkedEnergy(), getLinkedEnergyCapacity()));
        if (getRecipeLogic().isWaiting()) lines.addAll(getRecipeLogic().getFancyTooltip());
    }

    private ComponentPanelWidget panel(int x, int y, int width, Consumer<List<Component>> supplier) {
        return new ComponentPanelWidget(x, y, supplier).textSupplier(isRemote() ? null : supplier).setMaxWidthLimit(width);
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 300, 234);
        group.setBackground(new ColorRectTexture(0xFF0B2024));
        group.addWidget(new WidgetGroup(0, 0, 300, 3).setBackground(new ColorRectTexture(0xFF4EDFC8)));
        group.addWidget(new ComponentPanelWidget(10, 11, List.of(
                Component.translatable("gtecore.water.edi.title").withStyle(ChatFormatting.AQUA))).setMaxWidthLimit(280));
        group.addWidget(panel(12, 34, 276, this::addLoadText));
        group.addWidget(new ProgressWidget(() -> (double) displayedLoad / PurificationEdiState.CAPACITY,
                12, 83, 276, 7).setProgressTexture(new ColorRectTexture(0xFF244344), new ColorRectTexture(0xFFF1BD69)));
        var detail = new DraggableScrollableWidgetGroup(8, 96, 284, 73);
        detail.addWidget(panel(4, 3, 270, this::addBatchText));
        group.addWidget(detail);
        group.addWidget(new ComponentPanelWidget(12, 175, List.of(
                Component.translatable("gtecore.water.edi.rule"),
                Component.translatable("gtecore.water.edi.mode_next_batch"))).setMaxWidthLimit(276));
        group.addWidget(createDisconnectButton(184, 210, 104));
        return group;
    }
}
