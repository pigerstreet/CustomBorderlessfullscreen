package b100.fullscreenfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTextureView;

import b100.fullscreenfix.FullscreenFix;
import net.minecraft.client.Minecraft;

// GlCommandEncoder is package private, so it has to be targeted by name
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public class GlCommandEncoderMixin {

	private static final int GL_LINEAR = 0x2601;

	/**
	 * Presenting a frame blits the main render target onto the window. Vanilla always blits it 1:1,
	 * so with a custom render resolution the frame would end up in the corner of the window at its
	 * original size. Stretching the destination over the whole window is what turns the smaller
	 * render target into a proper upscaled borderless fullscreen image.
	 *
	 * Only the main render target may be stretched. Other mods present their own render targets
	 * through here as well, and those are already sized and positioned the way they want them, so
	 * stretching those too would blow them up over the whole window.
	 *
	 * Argument order is
	 * (source, dest, srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter).
	 */
	@ModifyArgs(method = "presentTexture", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/DirectStateAccess;blitFrameBuffers(IIIIIIIIIIII)V"))
	private void stretchFrameOntoWindow(Args args, @Local(argsOnly = true) GpuTextureView textureView) {
		if(FullscreenFix.getActiveRenderResolution() == null) {
			return;
		}

		if(!isMainRenderTarget(textureView)) {
			return;
		}

		final int windowWidth = FullscreenFix.realFramebufferWidth;
		final int windowHeight = FullscreenFix.realFramebufferHeight;

		if(windowWidth <= 0 || windowHeight <= 0) {
			return;
		}

		final int sourceWidth = args.get(4);
		final int sourceHeight = args.get(5);

		if(sourceWidth == windowWidth && sourceHeight == windowHeight) {
			// Nothing to scale, leave the nearest neighbour blit alone
			return;
		}

		args.set(8, windowWidth);
		args.set(9, windowHeight);

		// Nearest neighbour is fine for a 1:1 blit but produces a very harsh image when scaling
		args.set(11, GL_LINEAR);
	}

	private static boolean isMainRenderTarget(GpuTextureView textureView) {
		final Minecraft minecraft = Minecraft.getInstance();
		if(minecraft == null) {
			return false;
		}

		final RenderTarget mainRenderTarget = minecraft.getMainRenderTarget();
		if(mainRenderTarget == null) {
			return false;
		}

		return textureView == mainRenderTarget.getColorTextureView();
	}
}
