package b100.fullscreenfix.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import b100.fullscreenfix.FullscreenFix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.WindowRenderState;

/**
 * Draws the gui in the coordinate space a custom gui resolution asks for.
 *
 * The gui is drawn with an orthographic projection whose extent is the size of that coordinate space,
 * covering the whole window. Replacing the extent therefore rescales the gui without touching how the
 * world is rendered, which is the entire point of the option: the game keeps rendering at the real
 * resolution of the window and only the gui moves into the coordinate space of a different one.
 */
@Mixin(value = GuiRenderer.class)
public class GuiRendererMixin {

	/**
	 * The extent has to be derived from the same numbers as {@link WindowMixin#onSetGuiScale}, because
	 * gui code lays itself out against those and is drawn with this. Anything that made the two
	 * disagree would show up as everything being offset by the difference.
	 */
	@ModifyArgs(method = "draw(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Projection;setupOrtho(FFFFZ)V"))
	private void useGuiResolutionForGuiProjection(Args args) {
		if(FullscreenFix.getActiveGuiResolution() == null) {
			return;
		}

		final WindowRenderState window = getWindowRenderState();
		if(window == null || window.guiScale <= 0 || window.width <= 0 || window.height <= 0) {
			return;
		}

		final float extentWidth = (float) FullscreenFix.getGuiExtentWidth(window.width, window.height, window.guiScale);
		final float extentHeight = (float) FullscreenFix.getGuiExtentHeight(window.width, window.height, window.guiScale);

		args.set(2, extentWidth);
		args.set(3, extentHeight);

		logProjection(window, extentWidth, extentHeight);
	}

	private float loggedExtentWidth = 0.0f;
	private float loggedExtentHeight = 0.0f;
	private int loggedViewportWidth = -1;
	private int loggedViewportHeight = -1;

	/**
	 * Reports the coordinate space the gui is actually being drawn with whenever it changes. Getting
	 * this out of step with the size gui code lays itself out against is the one way this feature can
	 * go wrong, and it looks like everything being slightly offset rather than like an error, so it is
	 * worth being able to read both numbers off the log.
	 *
	 * This runs for every frame, so nothing is built until one of the values has actually changed.
	 */
	private void logProjection(WindowRenderState window, float extentWidth, float extentHeight) {
		if(extentWidth == loggedExtentWidth && extentHeight == loggedExtentHeight
				&& window.width == loggedViewportWidth && window.height == loggedViewportHeight) {
			return;
		}

		loggedExtentWidth = extentWidth;
		loggedExtentHeight = extentHeight;
		loggedViewportWidth = window.width;
		loggedViewportHeight = window.height;

		FullscreenFix.debugPrint("Gui projection: extent=" + extentWidth + "x" + extentHeight
				+ " over viewport " + window.width + "x" + window.height
				+ ", one gui unit is " + (window.width / extentWidth) + " x " + (window.height / extentHeight) + " pixels"
				+ ", item scale " + FullscreenFix.getEffectiveGuiScale(window.width, window.height, window.guiScale));
	}

	/**
	 * Item icons are rendered into an atlas at a size taken from the gui scale, then drawn into the
	 * gui at whatever size the coordinate space asks for. With a gui resolution smaller than the
	 * window a gui unit covers more pixels than the gui scale suggests, so the atlas has to be
	 * rendered larger or the icons are the one part of the gui that gets upscaled.
	 *
	 * This is the read the surrounding method compares against its cached value to decide whether to
	 * throw the atlas away, so changing it here also rebuilds the atlas when the option changes.
	 */
	@ModifyExpressionValue(method = "getGuiScaleInvalidatingItemAtlasIfChanged", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/renderer/state/WindowRenderState;guiScale:I"))
	private int guiResolutionItemAtlasScale(int guiScale) {
		return effectiveGuiScale(guiScale);
	}

	/**
	 * The same for the picture in picture elements, which each render into their own texture sized
	 * from the gui scale. The player model in the inventory and the map are drawn this way.
	 */
	@ModifyExpressionValue(method = "preparePictureInPicture", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/renderer/state/WindowRenderState;guiScale:I"))
	private int guiResolutionPictureInPictureScale(int guiScale) {
		return effectiveGuiScale(guiScale);
	}

	private static int effectiveGuiScale(int guiScale) {
		final WindowRenderState window = getWindowRenderState();
		if(window == null || window.width <= 0 || window.height <= 0) {
			return guiScale;
		}
		return FullscreenFix.getEffectiveGuiScale(window.width, window.height, guiScale);
	}

	private static WindowRenderState getWindowRenderState() {
		final Minecraft minecraft = Minecraft.getInstance();
		if(minecraft == null || minecraft.gameRenderer == null) {
			return null;
		}
		return minecraft.gameRenderer.getGameRenderState().windowRenderState;
	}
}
