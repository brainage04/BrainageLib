package io.github.brainage04.brainagelib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.brainage04.brainagelib.drops.DropCapture;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin {
    /// An armour stand has no death loot of its own: the punch that breaks it drops the stand and
    /// its equipment from `brokenByPlayer`, and a blast that breaks it drops the equipment from
    /// `brokenByAnything`, neither of which goes through `die`. Both are wrapped, so a player who
    /// breaks a stand gets its drops and equipment. A fire names no player, and a creative break
    /// drops nothing at all.
    @WrapOperation(
            method = "hurtServer",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/decoration/ArmorStand;brokenByPlayer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void brainagelib$capturePunchedStand(
            ArmorStand stand, ServerLevel level, DamageSource source, Operation<Void> original) {
        brainagelib$capture(stand, level, source, original);
    }

    @WrapOperation(
            method = "hurtServer",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/decoration/ArmorStand;brokenByAnything(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void brainagelib$captureBlastedStand(
            ArmorStand stand, ServerLevel level, DamageSource source, Operation<Void> original) {
        brainagelib$capture(stand, level, source, original);
    }

    @Unique
    private static void brainagelib$capture(
            ArmorStand stand, ServerLevel level, DamageSource source, Operation<Void> original) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            original.call(stand, level, source);
            return;
        }
        DropCapture.begin(player);
        try {
            original.call(stand, level, source);
        } finally {
            DropCapture.end();
        }
    }
}
