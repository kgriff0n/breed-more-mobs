package io.github.kgriff0n.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(HappyGhastEntity.class)
public class HappyGhastEntityMixin extends AnimalEntity {
    @Shadow
    @Final
    public static Predicate<ItemStack> FOOD_PREDICATE;

    protected HappyGhastEntityMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(at = @At("HEAD"), method = "initGoals")
    private void addGoal(CallbackInfo ci) {
        this.goalSelector.add(1, new AnimalMateGoal(this, 0.5));
    }

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void addInteractions(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack itemStack = player.getStackInHand(hand);

        // Breed
        if (this.isBreedingItem(itemStack)) {
            int i = this.getBreedingAge();
            if (!this.getEntityWorld().isClient() && i == 0 && this.canEat()) {
                this.eat(player, hand, itemStack);
                this.lovePlayer(player);
                cir.setReturnValue(ActionResult.SUCCESS_SERVER);
            }

            if (this.getEntityWorld().isClient()) {
                cir.setReturnValue(ActionResult.CONSUME);
            }
        }
    }

    @Override
    public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return EntityType.HAPPY_GHAST.create(world, SpawnReason.BREEDING);
    }

    @Inject(at = @At("HEAD"), method = "canEat", cancellable = true)
    public void canEat(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.getLoveTicks() <= 0);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return FOOD_PREDICATE.test(stack);
    }

    @Override
    public boolean canBreedWith(AnimalEntity other) {
        if (other == this) {
            return false;
        } else if (!(other instanceof HappyGhastEntity happyGhastEntity)) {
            return false;
        } else {
            // For some reason, the happy ghasts are unable to spawn
            // the baby when they meet, so we force it to appear here
            if (happyGhastEntity.isInLove() && this.isInLove()) {
                this.setLoveTicks(0);
                ServerWorld world = (ServerWorld) this.getEntityWorld();
                PassiveEntity baby = createChild(world, this);
                baby.setBaby(true);
                baby.setPosition(this.getEntityPos());
                world.spawnEntity(baby);
                return true;
            }
            return false;
        }
    }
}
