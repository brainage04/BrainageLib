package io.github.brainage04.brainagelib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.brainage04.brainagelib.drops.DropCapture;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// The ender dragon releases its experience during its 200-tick death animation, long after the
/// 100-tick kill credit has expired, so the killer is recorded on the first death tick and resolved
/// again for each award. The record is not saved: a dragon whose death animation spans a server
/// restart drops the rest in the world.
@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {
    @Unique private UUID brainagelib$killer;

    @Inject(method = "tickDeath", at = @At("HEAD"))
    private void brainagelib$recordKiller(CallbackInfo ci) {
        EnderDragon dragon = (EnderDragon) (Object) this;
        if (dragon.dragonDeathTime == 0) {
            ServerPlayer killer = DropCapture.mobKiller(dragon);
            brainagelib$killer = killer == null ? null : killer.getUUID();
        }
    }

    @WrapOperation(
            method = "tickDeath",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private void brainagelib$captureDeathExperience(
            ServerLevel level, Vec3 position, int amount, Operation<Void> original) {
        ServerPlayer killer =
                brainagelib$killer == null
                        ? null
                        : level.getServer().getPlayerList().getPlayer(brainagelib$killer);
        if (killer == null) {
            original.call(level, position, amount);
            return;
        }
        DropCapture.begin(killer);
        try {
            original.call(level, position, amount);
        } finally {
            DropCapture.end();
        }
    }
}
