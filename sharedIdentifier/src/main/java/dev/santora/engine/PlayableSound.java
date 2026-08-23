package dev.santora.engine;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.Identifier;

public record PlayableSound(Identifier eventId, Sound sound) {
}
