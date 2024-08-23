package io.github.kgriff0n;

import net.fabricmc.api.ModInitializer;

import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BreedMoreMobs implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("breed-more-mobs");

	@Override
	public void onInitialize() {
		LOGGER.info("Breed More Mobs loaded");
	}
}