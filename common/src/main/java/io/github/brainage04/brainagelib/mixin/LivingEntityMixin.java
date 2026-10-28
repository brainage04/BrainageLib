package io.github.brainage04.brainagelib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.brainage04.brainagelib.drops.DropCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /// Wraps the call in `die` rather than the method, so overrides that drop items before calling
    /// super (a fox's held item) are covered. On NeoForge the drops pass `LivingDropsEvent` and are
    /// spawned inside the call too. `ServerPlayer.die` does not call this method;
    /// `ServerPlayerMixin` covers players.
    @WrapOperation(
            method = "die",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/LivingEntity;dropAllDeathLoot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void brainagelib$captureDeathLoot(
            LivingEntity victim, ServerLevel level, DamageSource source, Operation<Void> original) {
        ServerPlayer killer = DropCapture.mobKiller(victim);
        if (killer == null) {
            original.call(victim, level, source);
            return;
        }
        DropCapture.begin(killer);
        try {
            original.call(victim, level, source);
        } finally {
            DropCapture.end();
        }
    }
}
