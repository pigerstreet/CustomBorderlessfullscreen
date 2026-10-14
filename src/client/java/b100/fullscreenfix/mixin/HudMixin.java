package b100.fullscreenfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import b100.fullscreenfix.CrosshairOnTop;
import b100.fullscreenfix.FullscreenFix;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

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
@Mixin(value = Hud.class)
public class HudMixin {

	@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
	private void guiResolutionUnstretchCrosshair(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
		if(CrosshairOnTop.shouldDefer()) {
			// Held back and drawn again once the rest of the hud has been added, so that nothing
			// another mod places over the middle of the screen can cover it. The scaling below is not
			// applied here, it is applied on that second pass instead, where the same code runs it
			// with the stack still balanced
			CrosshairOnTop.defer(extractor, deltaTracker);
			ci.cancel();
			return;
		}

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
	 * Draws the crosshair in a fixed colour instead of by inverting what is behind it.
	 *
	 * The sprite is pure white, and the inverting blend is what colours it, which means a background
	 * close to mid grey inverts to something close to mid grey and the crosshair vanishes into it.
	 * Drawing that same sprite with an ordinary textured blend and a colour of its own makes it read
	 * the same way on every background.
	 *
	 * Only the crosshair itself is replaced. The attack indicator is drawn from the same pipeline but
	 * from different sprites and is left alone, so it still looks the way it does without this mod.
	 */
	@WrapOperation(method = "extractCrosshair", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private void crosshairFixedColor(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, Operation<Void> original) {
		if(!FullscreenFix.CROSSHAIR_FIXED_COLOR.getBoolean()) {
			original.call(graphics, pipeline, sprite, x, y, width, height);
			return;
		}

		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, FullscreenFix.CROSSHAIR_COLOR.get().argb);
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
