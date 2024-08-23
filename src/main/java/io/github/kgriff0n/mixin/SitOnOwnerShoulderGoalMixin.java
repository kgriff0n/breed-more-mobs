package io.github.kgriff0n.mixin;

import net.minecraft.entity.ai.goal.SitOnOwnerShoulderGoal;
import net.minecraft.entity.passive.TameableShoulderEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SitOnOwnerShoulderGoal.class)
public class SitOnOwnerShoulderGoalMixin {

    @Shadow @Final private TameableShoulderEntity tameable;

    @Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
    private void canStart(CallbackInfoReturnable<Boolean> cir) {
        if (this.tameable.isBaby()) {
            cir.setReturnValue(false);
        }
    }

}
