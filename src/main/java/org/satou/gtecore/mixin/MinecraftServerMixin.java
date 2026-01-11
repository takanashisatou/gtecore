package org.satou.gtecore.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.satou.gtecore.config.GTEConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow @Final protected WorldData worldData;

    @Shadow public abstract ServerLevel overworld();

    @Inject(method = "createLevels",at=@At("RETURN"))
    protected void createLevels(ChunkProgressListener listener, CallbackInfo ci) {
        if (GTEConfig.INSTANCE.superPeace) {
            worldData.setDifficulty(Difficulty.PEACEFUL);
            worldData.setDifficultyLocked(true);
            ServerLevelData serverleveldata = worldData.overworldData();
            serverleveldata.setRaining(false);
            serverleveldata.setThundering(false);
            serverleveldata.setClearWeatherTime(1000000000);
            serverleveldata.setDayTime(6000L);
            serverleveldata.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,this.overworld().getServer());
            serverleveldata.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,this.overworld().getServer());
        }
    }
}
