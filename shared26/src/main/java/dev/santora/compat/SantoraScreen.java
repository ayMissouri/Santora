package dev.santora.compat;

import dev.santora.ui.SantoraScreenBase;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

public final class SantoraScreen extends SantoraScreenBase {

	public SantoraScreen(Screen parent) {
		super(parent);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(gfx, mouseX, mouseY, partialTick);
		draw(new ExtractorCanvas(gfx), mouseX, mouseY);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
		if (wantsVanillaBackground()) {
			super.extractBackground(gfx, mouseX, mouseY, partialTick);
		}
	}
}
