package org.satou.gtecore.mixin;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.multiblock.steam.LargeBoilerMachine;
import com.gregtechceu.gtceu.config.ConfigHolder;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

import static com.gregtechceu.gtceu.common.machine.multiblock.steam.LargeBoilerMachine.TICKS_PER_STEAM_GENERATION;

@Mixin(LargeBoilerMachine.class)
public abstract class LargeBoilerMachineMixin {
    @Shadow(remap = false)
    private int currentTemperature, throttle;
    @Shadow(remap = false)
    protected abstract int getCoolDownRate();
    @Shadow(remap = false)
    private int steamGenerated;
    @Shadow(remap = false)
    protected abstract void updateSteamSubscription();
    /**
     * @author Satou
     * @reason Meow!
     */
    @Overwrite(remap = false)
    protected void updateCurrentTemperature() {

        if (((LargeBoilerMachine)((Object)this)).recipeLogic.isWorking()) {
            if (((LargeBoilerMachine)((Object)this)).getOffsetTimer() % 10 == 0) {
                if (((LargeBoilerMachine) ((Object) this)).getCurrentTemperature() < ((LargeBoilerMachine)((Object)this)).getMaxTemperature()) {
                    currentTemperature = Mth.clamp(currentTemperature + ((LargeBoilerMachine)((Object)this)).heatSpeed * 10, 0, ((LargeBoilerMachine)((Object)this)).getMaxTemperature());
                }
            }
        } else if (currentTemperature > 0) {
            currentTemperature -= getCoolDownRate();
        }

        if (((LargeBoilerMachine)((Object)this)).isFormed() && ((LargeBoilerMachine)((Object)this)).getOffsetTimer() % ((LargeBoilerMachine)((Object)this)).TICKS_PER_STEAM_GENERATION == 0) {
            long maxDrain = (long) currentTemperature * throttle * TICKS_PER_STEAM_GENERATION /
                    (ConfigHolder.INSTANCE.machines.largeBoilers.steamPerWater * 100L);
            if (currentTemperature < 100) {
                steamGenerated = 0;
            } else if (maxDrain > 0) { // if maxDrain is 0 because throttle is too low, skip trying to make steam
                // drain water
                var drainWater = List.of(FluidIngredient.of(Fluids.WATER,(int)maxDrain));
                List<IRecipeHandler<?>> inputTanks = new ArrayList<>();
                inputTanks.addAll(((LargeBoilerMachine)((Object)this)).getCapabilitiesFlat(IO.IN, FluidRecipeCapability.CAP));
                inputTanks.addAll(((LargeBoilerMachine)((Object)this)).getCapabilitiesFlat(IO.BOTH, FluidRecipeCapability.CAP));
                for (IRecipeHandler<?> tank : inputTanks) {
                    drainWater = (List<FluidIngredient>) tank.handleRecipe(IO.IN, null, drainWater, false);
                    if (drainWater == null || drainWater.isEmpty()) {
                        break;
                    }
                }
                int drained = (drainWater == null || drainWater.isEmpty()) ? (int)maxDrain :
                        (int) (maxDrain - drainWater.get(0).getAmount());

                steamGenerated = drained * ConfigHolder.INSTANCE.machines.largeBoilers.steamPerWater;

                if (drained > 0) {
                    // fill steam
                    var fillSteam = List.of(FluidIngredient.of(GTMaterials.Steam.getFluid(steamGenerated)));
                    List<IRecipeHandler<?>> outputTanks = new ArrayList<>();
                    outputTanks.addAll(((LargeBoilerMachine)((Object)this)).getCapabilitiesFlat(IO.OUT, FluidRecipeCapability.CAP));
                    outputTanks.addAll(((LargeBoilerMachine)((Object)this)).getCapabilitiesFlat(IO.BOTH, FluidRecipeCapability.CAP));
                    for (IRecipeHandler<?> tank : outputTanks) {
                        fillSteam = (List<FluidIngredient>) tank.handleRecipe(IO.OUT, null, fillSteam, false);
                        if (fillSteam == null) break;
                    }
                }

                // check explosion
                if (drained < maxDrain) {
                    ((LargeBoilerMachine)((Object)this)).doExplosion(2f);
                    var center = ((LargeBoilerMachine)((Object)this)).getPos().below().relative(((LargeBoilerMachine)((Object)this)).getFrontFacing().getOpposite());
                    if (GTValues.RNG.nextInt(100) > 80) {
                        ((LargeBoilerMachine)((Object)this)).doExplosion(center, 2f);
                    }
                    for (Direction x : Direction.Plane.HORIZONTAL) {
                        for (Direction y : Direction.Plane.HORIZONTAL) {
                            if (GTValues.RNG.nextInt(100) > 80) {
                                ((LargeBoilerMachine)((Object)this)).doExplosion(center.relative(x).relative(y), 2f);
                            }
                        }
                    }
                }
            }
        }
        updateSteamSubscription();
    }
}
