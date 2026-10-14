package b100.fullscreenfix.mixin.access;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Mixin(Hud.class)
public interface HudAccess {

	/**
	 * Draws the crosshair, and the attack indicator with it, into whatever layer of the hud is current.
	 *
	 * Called a second time for a frame, once nothing else is going to be added to it, so that the
	 * crosshair ends up above the rest of the hud. See {@link b100.fullscreenfix.CrosshairOnTop}.
	 */
	@Invoker("extractCrosshair")
	public void invokeExtractCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);
}
