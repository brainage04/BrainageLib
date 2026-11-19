package io.github.brainage04.brainagelib.mixin;

import io.github.brainage04.brainagelib.drops.SupportTicks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Every block tick a level schedules passes through here, so a tick scheduled while a player's
/// break runs is recorded for them ({@link SupportTicks}).
@Mixin(LevelTicks.class)
public abstract class LevelTicksMixin {
    @SuppressWarnings("unchecked")
    @Inject(method = "schedule", at = @At("HEAD"))
    private void brainagelib$recordSupportTick(ScheduledTick<?> tick, CallbackInfo callback) {
        if (tick.type() instanceof Block block) {
            SupportTicks.record(
                    (LevelTicks<Block>) (Object) this, tick.pos(), block);
        }
    }
}
