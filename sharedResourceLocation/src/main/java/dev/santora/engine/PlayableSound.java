package dev.santora.engine;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.ResourceLocation;

public record PlayableSound(ResourceLocation eventId, Sound sound) {
}
