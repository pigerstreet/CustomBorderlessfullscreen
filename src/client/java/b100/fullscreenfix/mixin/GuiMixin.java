package b100.fullscreenfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import b100.fullscreenfix.FullscreenFix;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws the crosshair as though there were no gui resolution.
 *
 * Stretching a gui resolution onto a window of a different shape is what keeps hud positions exact,
 * and it makes a gui unit wider than it is tall. Everything laid out in those units is drawn that way
 * and that is the point, but the crosshair is not laid out in anything: it is one small square in the
 * middle of the screen, and a square drawn 12% wider than it is tall is simply wrong.
 *
 * The gui is drawn with a single projection, so nothing can be left out of it. Scaling the crosshair
 * by the opposite amount comes to the same thing, since the two cancel and it lands at the size and
 * shape it has without this feature. Scaling around the middle of the screen keeps it centred.
 *
 * The attack indicator is drawn by the same method and goes with it, which is what is wanted, as it
 * sits directly under the crosshair and would otherwise no longer match it.
 */
@Mixin(value = Gui.class)
public class GuiMixin {

	@Inject(method = "extractCrosshair", at = @At("HEAD"))
	private void guiResolutionUnstretchCrosshair(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
		if(!shouldUnstretch()) {
			return;
		}

		final float scaleX = (float) counterScale(FullscreenFix.getPixelsPerGuiUnitX());
		final float scaleY = (float) counterScale(FullscreenFix.getPixelsPerGuiUnitY());

		final float centreX = extractor.guiWidth() / 2.0f;
		final float centreY = extractor.guiHeight() / 2.0f;

		extractor.pose().pushMatrix();
		extractor.pose().translate(centreX, centreY);
		extractor.pose().scale(scaleX, scaleY);
		extractor.pose().translate(-centreX, -centreY);
	}

	/**
	 * Every return is matched, including the early ones for spectators and for not being in first
	 * person, so the stack cannot be left unbalanced by the crosshair simply not being drawn.
	 */
	@Inject(method = "extractCrosshair", at = @At("RETURN"))
	private void guiResolutionRestoreAfterCrosshair(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
		if(!shouldUnstretch()) {
			return;
		}
		extractor.pose().popMatrix();
	}

	/**
	 * The factor that takes a gui unit back to being the gui scale across, which is what it would be
	 * without a gui resolution. One when the axis is already that, so nothing is done needlessly.
	 */
	private static double counterScale(double pixelsPerGuiUnit) {
		if(pixelsPerGuiUnit <= 0.0) {
			return 1.0;
		}
		final int guiScale = FullscreenFix.getRealGuiScale(0);
		return guiScale <= 0 ? 1.0 : guiScale / pixelsPerGuiUnit;
	}

	private static boolean shouldUnstretch() {
		return FullscreenFix.NORMAL_CROSSHAIR.getBoolean() && FullscreenFix.getActiveGuiResolution() != null;
	}
}
