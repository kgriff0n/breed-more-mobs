package io.github.kgriff0n.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

@Mixin(SkeletonHorseEntity.class)
public abstract class SkeletonHorseEntityMixin extends AbstractHorseEntity {

	protected SkeletonHorseEntityMixin(EntityType<? extends AbstractHorseEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
	private void addInteractions(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		ItemStack itemStack = player.getStackInHand(hand);
		// Tame
		if (!this.isTame()) {
			if (itemStack.isOf(Items.AIR)) {
				player.swingHand(hand);
				player.startRiding(this, true);
			} else {
				this.playAngrySound();
			}
			cir.setReturnValue(ActionResult.success(this.getWorld().isClient));
		}

		// Breed
		if (this.isTame() && this.isBreedingItem(itemStack)) {
			int i = this.getBreedingAge();
			if (!this.getWorld().isClient && i == 0 && this.canEat()) {
				this.eat(player, hand, itemStack);
				this.lovePlayer(player);
				cir.setReturnValue(ActionResult.SUCCESS);
			}

			if (this.isBaby()) {
				this.eat(player, hand, itemStack);
				this.growUp(toGrowUpAge(-i), true);
				cir.setReturnValue(ActionResult.success(this.getWorld().isClient));
			}

			if (this.getWorld().isClient) {
				cir.setReturnValue(ActionResult.CONSUME);
			}
		}
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.MEAT);
	}

	@Override
	public boolean canBreedWith(AnimalEntity other) {
		if (other == this) {
			return false;
		} else if (!(other instanceof SkeletonHorseEntity skeletonHorseEntity)) {
			return false;
		} else {
			return skeletonHorseEntity.isInLove() && this.isInLove();
		}
	}

	@Inject(method = "createChild", at = @At("HEAD"), cancellable = true)
	private void createChild(ServerWorld world, PassiveEntity entity, CallbackInfoReturnable<PassiveEntity> cir) {
		SkeletonHorseEntity skeletonHorseEntity = new SkeletonHorseEntity(EntityType.SKELETON_HORSE, world);
		skeletonHorseEntity.setOwnerUuid(this.getOwnerUuid());
		skeletonHorseEntity.setTame(true);
		skeletonHorseEntity.setBaby(true);
		cir.setReturnValue(skeletonHorseEntity);
	}

	@Inject(method = "tickMovement", at = @At("HEAD"))
	private void grown(CallbackInfo ci) {
		if (this.happyTicksRemaining > 0) {
			if (this.happyTicksRemaining % 4 == 0 && !this.getWorld().isClient()) {
				((ServerWorld)this.getWorld()).spawnParticles(ParticleTypes.HAPPY_VILLAGER, this.getParticleX(1.0), this.getRandomBodyY() + 0.5, this.getParticleZ(1.0), 1, 0, 0, 0, 0);
			}
			this.happyTicksRemaining--;
		}
	}

}