package org.satou.gtecore.common.machine.multiblock.generator;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.multiblock.steam.SteamParallelMultiblockMachine;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FUEL_ENGINE extends WorkableElectricMultiblockMachine implements ITieredMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            FUEL_ENGINE.class, WorkableMultiblockMachine.MANAGED_FIELD_HOLDER);

    private static final FluidStack OXYGEN_STACK = GTMaterials.Oxygen.getFluid(1);
    private static final FluidStack LIQUID_OXYGEN_STACK = GTMaterials.Oxygen.getFluid(FluidStorageKeys.LIQUID, 4);
    private static final FluidStack LUBRICANT_STACK = GTMaterials.Lubricant.getFluid(1);

    @Getter
    private final int tier;
    // runtime
    @DescSynced
    private boolean isOxygenBoosted = false;
    private int runningTimer = 0;

    public FUEL_ENGINE(IMachineBlockEntity holder, int tier) {
        super(holder);
        this.tier = tier;
    }

    public FUEL_ENGINE(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, args);
        this.tier = tier;
    }

    private boolean isExtreme() {
        return getTier() > GTValues.EV;
    }

    public boolean isBoostAllowed() {
        return getMaxVoltage() >= GTValues.V[getTier() + 1];
    }

    //////////////////////////////////////
    // ****** Recipe Logic *******//
    //////////////////////////////////////

    @Override
    public long getOverclockVoltage() {
        if (isOxygenBoosted) return GTValues.V[tier] * 2;
        else return GTValues.V[tier];
    }

    protected GTRecipe getLubricantRecipe() {
        return GTRecipeBuilder.ofRaw().inputFluids(LUBRICANT_STACK).buildRawRecipe();
    }

    protected GTRecipe getBoostRecipe() {
        return GTRecipeBuilder.ofRaw().inputFluids(isExtreme() ? LIQUID_OXYGEN_STACK : OXYGEN_STACK).buildRawRecipe();
    }

    /**
     * @return EUt multiplier that should be applied to the engine's output
     */
    protected double getProductionBoost() {
        if (!isOxygenBoosted) return 1;
        return isExtreme() ? 2.0 : 1.5;
    }

    /**
     * Recipe Modifier for <b>Combustion Engine Multiblocks</b> - can be used as a valid {@link RecipeModifier}
     * <p>
     * Recipe is rejected if the machine's intakes are obstructed or if it doesn't have lubricant<br>
     * Recipe is parallelized up to {@code desiredEUt / recipeEUt} times.
     * EUt is further multiplied by the production boost of the engine.
     *
     * @param machine a {@link FUEL_ENGINE}
     * @param recipe  recipe
     * @return A {@link ModifierFunction} for the given Combustion Engine
     */
    public static int getParallelAmount(MetaMachine machine, GTRecipe recipe, int parallelLimit) {
        if (parallelLimit <= 1) return parallelLimit;
        if (!(machine instanceof IRecipeLogicMachine rlm)) return 1;
        // First check if we are limited by recipe inputs. This can short circuit a lot of consecutive checking
        int maxInputMultiplier = FUEL_ENGINE.getMaxByInput(rlm, recipe, parallelLimit, Collections.emptyList());
        if (maxInputMultiplier == 0) return 0;
        return maxInputMultiplier;
        // GTECore.LOGGER.log(Level.valueOf("1"),maxInputMultiplier);
        // Simulate the merging of the maximum amount of recipes that can be run with these items
        // and limit by the amount we can successfully merge
    }

    /**
     * @param holder        The inventories
     * @param recipe        The recipe
     * @param parallelLimit hard cap on the amount returned
     * @param capsToSkip    the capabilities to skip parallel testing
     * @return returns the amount of possible time a recipe can be made from a given input inventory
     */
    public static int getMaxByInput(IRecipeCapabilityHolder holder, GTRecipe recipe, int parallelLimit,
                                    List<RecipeCapability<?>> capsToSkip) {
        int minimum = Integer.MAX_VALUE;

        // non-tick inputs.
        for (RecipeCapability<?> cap : recipe.inputs.keySet()) {
            if (cap.doMatchInRecipe() && !capsToSkip.contains(cap)) {
                // Find the maximum number of recipes that can be performed from the contents of the input inventories
                minimum = Math.min(minimum, cap.getMaxParallelByInput(holder, recipe, parallelLimit, false));
            }
        }

        // tick inputs.
        for (RecipeCapability<?> cap : recipe.tickInputs.keySet()) {
            if (cap.doMatchInRecipe() && !capsToSkip.contains(cap)) {
                // Find the maximum number of recipes that can be performed from the contents of the input inventories
                minimum = Math.min(minimum, cap.getMaxParallelByInput(holder, recipe, parallelLimit, true));
            }
        }

        if (minimum == Integer.MAX_VALUE) return 0;
        return minimum;
    }

    public static ModifierFunction recipeModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof FUEL_ENGINE engineMachine)) {
            return RecipeModifier.nullWrongType(FUEL_ENGINE.class, machine);
        }
        EnergyStack EUt = recipe.getOutputEUt();
        // has lubricant
        if (!EUt.isEmpty()) {
            int maxParallel = 2000000000; // get maximum parallel
            int actualParallel = FUEL_ENGINE.getParallelAmount(engineMachine, recipe, maxParallel);
            ((FUEL_ENGINE) machine).recipeLogic.setMultiParallelLogic(true);
            ((FUEL_ENGINE) machine).recipeLogic.setMultiParallelCount(maxParallel);
            double eutMultiplier = actualParallel * engineMachine.getProductionBoost();
            return ModifierFunction.builder()
                    .inputModifier(ContentModifier.multiplier(actualParallel))
                    .outputModifier(ContentModifier.multiplier(actualParallel))
                    .eutMultiplier(eutMultiplier)
                    .parallels(actualParallel)
                    .build();
        }
        return ModifierFunction.NULL;
    }
    public static ModifierFunction recipeModifieForFusionReactor(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof WorkableElectricMultiblockMachine engineMachine)) {
            return RecipeModifier.nullWrongType(WorkableElectricMultiblockMachine.class, machine);
        }
        EnergyStack EUt = recipe.getOutputEUt();
        // has lubricant
            int maxParallel = 2000000000; // get maximum parallel
            int actualParallel = FUEL_ENGINE.getParallelAmount(engineMachine, recipe, maxParallel);
            //double eutMultiplier = actualParallel * engineMachine.getProductionBoost();
            return ModifierFunction.builder()
                    .inputModifier(ContentModifier.multiplier(actualParallel))
                    .outputModifier(ContentModifier.multiplier(actualParallel))
                    .parallels(actualParallel)
                    .build();

    }

    public static ModifierFunction recipeModifierForRing(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof WorkableElectricMultiblockMachine engineMachine)) {
            return RecipeModifier.nullWrongType(WorkableElectricMultiblockMachine.class, machine);
        }
        int maxParallel = 1024; // get maximum parallel
        int actualParallel = FUEL_ENGINE.getParallelAmount(engineMachine, recipe, maxParallel);
        double eutMultiplier = actualParallel;
        return ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(actualParallel))
                .outputModifier(ContentModifier.multiplier(actualParallel))
                .eutMultiplier(eutMultiplier)
                .parallels(actualParallel)
                .durationModifier(ContentModifier.multiplier(0.00001))
                .build();
    }
    public static ModifierFunction recipeModifierForSTEAMOP(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof SteamParallelMultiblockMachine engineMachine)) {
            return RecipeModifier.nullWrongType(SteamParallelMultiblockMachine.class, machine);
        }
        int maxParallel = 1000000000; // get maximum parallel
        int actualParallel = FUEL_ENGINE.getParallelAmount(engineMachine, recipe, maxParallel);
        engineMachine.recipeLogic.setMultiParallelLogic(true);
        engineMachine.recipeLogic.setMultiParallelCount(maxParallel);
        double eutMultiplier = actualParallel;
        return ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(actualParallel))
                .outputModifier(ContentModifier.multiplier(actualParallel))
                .eutMultiplier(eutMultiplier)
                .parallels(actualParallel)
                .durationModifier(ContentModifier.multiplier(0.00001))
                .build();
    }
    public static ModifierFunction recipeModifierForOmega(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof WorkableElectricMultiblockMachine engineMachine)) {
            return RecipeModifier.nullWrongType(SteamParallelMultiblockMachine.class, machine);
        }
        int maxParallel = 1000000000; // get maximum parallel
        int actualParallel = FUEL_ENGINE.getParallelAmount(engineMachine, recipe, maxParallel);
        double eutMultiplier = actualParallel;
        return ModifierFunction.builder()
                .inputModifier(ContentModifier.multiplier(actualParallel))
                .outputModifier(ContentModifier.multiplier(actualParallel))
                .parallels(actualParallel)
                .durationModifier(ContentModifier.multiplier(0.00001))
                .build();
    }
    @Override
    public boolean regressWhenWaiting() {
        return false;
    }

    //////////////////////////////////////
    // ******* GUI ********//
    //////////////////////////////////////

    @Override
    public void addDisplayText(List<Component> textList) {
        MultiblockDisplayText.Builder builder = MultiblockDisplayText.builder(textList, isFormed())
                .setWorkingStatus(recipeLogic.isWorkingEnabled(), recipeLogic.isActive());

        long lastEUt = recipeLogic.getLastRecipe() != null ?
                recipeLogic.getLastRecipe().getOutputEUt().getTotalEU() : 0;
        if (isExtreme()) {
            builder.addEnergyProductionLine(GTValues.V[tier + 1], lastEUt);
        } else {
            builder.addEnergyProductionAmpsLine(GTValues.V[tier] * 3, 3);
        }

        if (isActive() && isWorkingEnabled()) {
            builder.addCurrentEnergyProductionLine(lastEUt);
        }

        builder.addFuelNeededLine(getRecipeFluidInputInfo(), recipeLogic.getDuration());

        if (isFormed && isOxygenBoosted) {
            final var key = isExtreme() ? "gtceu.multiblock.large_combustion_engine.liquid_oxygen_boosted" :
                    "gtceu.multiblock.large_combustion_engine.oxygen_boosted";
            builder.addCustom(tl -> tl.add(Component.translatable(key).withStyle(ChatFormatting.AQUA)));
        }

        builder.addWorkingStatusLine();
    }

    @Nullable
    public String getRecipeFluidInputInfo() {
        // Previous Recipe is always null on first world load, so try to acquire a new recipe
        GTRecipe recipe = recipeLogic.getLastRecipe();
        if (recipe == null) {
            Iterator<GTRecipe> iterator = recipeLogic.searchRecipe();
            recipe = iterator.hasNext() ? iterator.next() : null;
            if (recipe == null) return null;
        }
        FluidStack requiredFluidInput = RecipeHelper.getInputFluids(recipe).get(0);

        long ocAmount = getMaxVoltage() / recipe.getOutputEUt().getTotalEU();
        int neededAmount = GTMath.saturatedCast(ocAmount * requiredFluidInput.getAmount());
        return ChatFormatting.RED + FormattingUtil.formatNumbers(neededAmount) + "mB";
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }
}
