package b100.fullscreenfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;

import b100.fullscreenfix.FullscreenFix;
import b100.fullscreenfix.PictureInPictureScale;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

/**
 * Draws a picture in picture element belonging to another mod at the size it will really be shown at.
 *
 * These elements are rendered into a texture and then stretched over the rectangle they occupy, and
 * vanilla sizes that texture as the size of the rectangle in gui units times the gui scale, then draws
 * into it through a pose scaled by that same number. That is the pixels it covers only while a gui unit
 * really is the gui scale across, which is what a gui resolution stops being true, and by a different
 * amount on each axis when the resolution is not the shape of the window.
 *
 * No single gui scale can express both amounts, so the texture is measured per axis here, and the pose
 * drawn into it is scaled per axis to match. On a 2560x1440 window laid out as 1720x1080 a gui unit is
 * 2.98 pixels across and 2.67 down, where a scale of 3 would make a texture 1620 tall to be stretched
 * onto 1440.
 *
 * The two have to move together. Sizing the texture alone leaves anything that does use the pose drawing
 * its content 11% too far down inside a texture that is then squashed back, so the whole element ends up
 * short of where the mod that drew it thinks it is; that is what NoammAddons' storage overlay does, since
 * it batches every item into one element covering the whole screen. Scaling the pose alone would leave the
 * same gap the other way round. Doing both means a renderer that draws through the pose in gui units and
 * one that ignores the pose and draws in real pixels, which is what Odin's NanoVG menus do, both land
 * exactly where they mean to.
 *
 * This is only for renderers that did not come with the game. Vanilla's are consistent with themselves at
 * whatever single scale they are given, so they keep the enlarged one and are left alone.
 */
@Mixin(value = PictureInPictureRenderer.class)
public class PictureInPictureRendererMixin {

	@ModifyVariable(method = "prepare", at = @At("STORE"), index = 5)
	private int guiResolutionTextureWidth(int textureWidth, PictureInPictureRenderState state, GuiRenderState guiRenderState, FeatureRenderDispatcher featureRenderDispatcher, int guiScale) {
		if(!sizesItsOwnContent()) {
			return textureWidth;
		}
		return toPixels(state.x1() - state.x0(), FullscreenFix.getPixelsPerGuiUnitX(), textureWidth);
	}

	@ModifyVariable(method = "prepare", at = @At("STORE"), index = 6)
	private int guiResolutionTextureHeight(int textureHeight, PictureInPictureRenderState state, GuiRenderState guiRenderState, FeatureRenderDispatcher featureRenderDispatcher, int guiScale) {
		if(!sizesItsOwnContent()) {
			return textureHeight;
		}
		return toPixels(state.y1() - state.y0(), FullscreenFix.getPixelsPerGuiUnitY(), textureHeight);
	}

	/**
	 * Vanilla scales the pose by the gui scale on both axes, which is the number of pixels it believes a
	 * gui unit covers. Replacing it with the number of pixels a gui unit really covers, one per axis, is
	 * the same statement made accurately.
	 *
	 * The depth follows the vertical rather than the horizontal so that a model drawn into one of these
	 * gains no depth over what vanilla would have given it, since the projection is unchanged and extra
	 * depth is the one thing here that could clip.
	 */
	@ModifyArgs(method = "prepare", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
	private void guiResolutionPoseScale(Args args, PictureInPictureRenderState state, GuiRenderState guiRenderState, FeatureRenderDispatcher featureRenderDispatcher, int guiScale) {
		if(!sizesItsOwnContent()) {
			return;
		}

		final double scaleX = FullscreenFix.getPixelsPerGuiUnitX() * state.scale();
		final double scaleY = FullscreenFix.getPixelsPerGuiUnitY() * state.scale();
		if(scaleX <= 0.0 || scaleY <= 0.0) {
			return;
		}

		args.set(0, (float) scaleX);
		args.set(1, (float) scaleY);
		args.set(2, (float) -scaleY);
	}

	/**
	 * Watches a renderer draw, so that one which measures its own content against the gui scale can be
	 * recognised and then sized the way it expects. See {@link PictureInPictureScale}.
	 */
	@WrapOperation(method = "prepare", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/pip/PictureInPictureRenderer;renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"))
	private void guiResolutionWatchRenderToTexture(PictureInPictureRenderer<?> renderer, PictureInPictureRenderState state, PoseStack pose, SubmitNodeCollector collector, Operation<Void> original) {
		PictureInPictureScale.beginRenderToTexture(getClass());
		try {
			original.call(renderer, state, pose, collector);
		}finally {
			PictureInPictureScale.endRenderToTexture();
		}
	}

	private static int toPixels(int guiUnits, double pixelsPerUnit, int fallback) {
		if(guiUnits <= 0 || pixelsPerUnit <= 0.0) {
			return fallback;
		}
		return Math.max(1, (int) Math.round(guiUnits * pixelsPerUnit));
	}

	private static final int ORIGIN_UNKNOWN = 0;
	private static final int ORIGIN_ANOTHER_MOD = 1;
	private static final int ORIGIN_GAME = 2;

	/**
	 * Whether the renderer this is attached to came with the game.
	 *
	 * A renderer never changes class, so this is worked out once rather than on every element it draws.
	 * The three injectors above each ask, so at three lookups per element per frame the string work
	 * behind the answer was being repeated for nothing.
	 */
	private int guiResolutionOrigin = ORIGIN_UNKNOWN;

	/**
	 * Whether this renderer is one the game did not come with, and so one whose idea of how big its own
	 * content is has to be corrected rather than trusted.
	 */
	private boolean sizesItsOwnContent() {
		if(FullscreenFix.getActiveGuiResolution() == null) {
			return false;
		}

		if(guiResolutionOrigin == ORIGIN_UNKNOWN) {
			final String name = getClass().getName();
			// Mixin renames the target class, so the package identifies it rather than the name
			final boolean fromAnotherMod = !name.startsWith("net.minecraft.") && !name.startsWith("com.mojang.");
			guiResolutionOrigin = fromAnotherMod ? ORIGIN_ANOTHER_MOD : ORIGIN_GAME;
		}

		if(guiResolutionOrigin == ORIGIN_GAME) {
			// Asked first, so the game's own renderers never reach the lookup below
			return false;
		}

		// It measures itself against the gui scale, so it is consistent only with a texture measured the
		// same way, which is what leaving it alone gives it. This one is learned while the game runs and
		// so cannot be settled once like the origin above.
		return !PictureInPictureScale.derivesSizeFromGuiScale(getClass());
	}
}
