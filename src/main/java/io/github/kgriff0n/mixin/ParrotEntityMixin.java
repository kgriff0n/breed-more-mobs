package io.github.kgriff0n.mixin;

import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableShoulderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
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

@Mixin(ParrotEntity.class)
public abstract class ParrotEntityMixin extends TameableShoulderEntity implements VariantHolder<ParrotEntity.Variant>, Flutterer {

	protected ParrotEntityMixin(EntityType<? extends TameableShoulderEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(at = @At("HEAD"), method = "initGoals")
	private void addGoal(CallbackInfo ci) {
		this.goalSelector.add(1, new AnimalMateGoal(this, 1.0));
	}

	@Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
	private void addBreedInteraction(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		ItemStack itemStack = player.getStackInHand(hand);
		if (this.isTamed() && this.isBreedingItem(itemStack)) {
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

	@Inject(method = "isBreedingItem", at = @At("HEAD"), cancellable = true)
	private void addBreedingItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (stack.isIn(ItemTags.PARROT_FOOD)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "createChild", at = @At("HEAD"), cancellable = true)
	private void createChild(ServerWorld world, PassiveEntity entity, CallbackInfoReturnable<PassiveEntity> cir) {
		ParrotEntity parrotEntity = new ParrotEntity(EntityType.PARROT, world);
		parrotEntity.setVariant(this.getVariant());
		if (this.isTamed()) {
			parrotEntity.setOwnerUuid(this.getOwnerUuid());
			parrotEntity.setTamed(true, true);
			parrotEntity.getAttributeInstance(EntityAttributes.GENERIC_SCALE).setBaseValue(0.5f);
		}
		cir.setReturnValue(parrotEntity);
	}

	@Inject(method = "isBaby", at = @At("HEAD"), cancellable = true)
	private void isBaby(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(this.getBreedingAge() < 0);
	}

	// Modify the conditions under which a parrot can breed another parrot - almost identical to the code found in WolfEntity.java
	@Inject(method = "canBreedWith", at = @At("HEAD"), cancellable = true)
	private void canBreedWith(AnimalEntity other, CallbackInfoReturnable<Boolean> cir) {
		if (other == this) {
			cir.setReturnValue(false);
		} else if (!this.isTamed()) {
			cir.setReturnValue(false);
		} else if (!(other instanceof ParrotEntity parrotEntity)) {
			cir.setReturnValue(false);
		} else if (!parrotEntity.isTamed()) {
			cir.setReturnValue(false);
		} else {
			cir.setReturnValue(!parrotEntity.isSitting() && parrotEntity.isInLove() && this.isInLove());
		}
	}

	@Inject(method = "tickMovement", at = @At("HEAD"))
	private void grown(CallbackInfo ci) {
		if (this.happyTicksRemaining > 0) {
			if (this.happyTicksRemaining % 4 == 0) {
				((ServerWorld)this.getWorld()).spawnParticles(ParticleTypes.HAPPY_VILLAGER, this.getParticleX(1.0), this.getRandomBodyY() + 0.5, this.getParticleZ(1.0), 1, 0, 0, 0, 0);
			}

			this.happyTicksRemaining--;
		}
		if (this.getBreedingAge() == -1) {
			this.getAttributeInstance(EntityAttributes.GENERIC_SCALE).setBaseValue(1f);
		}
	}

}