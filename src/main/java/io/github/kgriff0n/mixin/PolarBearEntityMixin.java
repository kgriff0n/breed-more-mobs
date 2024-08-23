package io.github.kgriff0n.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PolarBearEntity.class)
public abstract class PolarBearEntityMixin extends AnimalEntity implements Angerable {

	protected PolarBearEntityMixin(EntityType<? extends AnimalEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(at = @At("HEAD"), method = "initGoals")
	private void addGoal(CallbackInfo ci) {
		this.goalSelector.add(1, new AnimalMateGoal(this, 1.0));
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);
		if (this.isBreedingItem(itemStack)) {
			int i = this.getBreedingAge();
			if (!this.getWorld().isClient && i == 0 && this.canEat()) {
				this.eat(player, hand, itemStack);
				this.lovePlayer(player);
				return ActionResult.SUCCESS;
			}

			if (this.isBaby()) {
				this.eat(player, hand, itemStack);
				this.growUp(toGrowUpAge(-i), true);
				return ActionResult.success(this.getWorld().isClient);
			}

			if (this.getWorld().isClient) {
				return ActionResult.CONSUME;
			}
		}
		return ActionResult.PASS;
	}

	@Inject(method = "isBreedingItem", at = @At("HEAD"), cancellable = true)
	private void addBreedingItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(stack.isIn(ItemTags.FISHES));
	}

	@Override
	public boolean canBreedWith(AnimalEntity other) {
		if (other == this) {
			return false;
		} else if (!(other instanceof PolarBearEntity polarBearEntity)) {
			return false;
		} else {
			return polarBearEntity.isInLove() && this.isInLove();
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void grown(CallbackInfo ci) {
		if (this.happyTicksRemaining > 0) {
			if (this.happyTicksRemaining % 4 == 0) {
				((ServerWorld)this.getWorld()).spawnParticles(ParticleTypes.HAPPY_VILLAGER, this.getParticleX(1.0), this.getRandomBodyY() + 0.5, this.getParticleZ(1.0), 1, 0, 0, 0, 0);
			}

			this.happyTicksRemaining--;
		}
	}

}