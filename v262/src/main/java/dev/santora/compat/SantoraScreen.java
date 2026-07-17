package dev.santora.compat;

import dev.santora.ui.SantoraScreenBase;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class SantoraScreen extends SantoraScreenBase {

	@Override
	public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(gfx, mouseX, mouseY, partialTick);
		draw(new ExtractorCanvas(gfx), mouseX, mouseY);
	}
}
