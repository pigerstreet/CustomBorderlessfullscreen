package b100.fullscreenfix.mixin;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTextureView;

import b100.fullscreenfix.FullscreenFix;
import net.minecraft.client.Minecraft;

// GlCommandEncoder is package private, so it has to be targeted by name
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public class GlCommandEncoderMixin {

	private static final int GL_NEAREST = 0x2600;
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

		// Stretching covers the whole window, keeping the aspect ratio covers a centred part of it
		final int targetWidth = (int) Math.round(FullscreenFix.getPresentFractionWidth() * windowWidth);
		final int targetHeight = (int) Math.round(FullscreenFix.getPresentFractionHeight() * windowHeight);
		final int targetX = (windowWidth - targetWidth) / 2;
		final int targetY = (windowHeight - targetHeight) / 2;

		args.set(6, targetX);
		args.set(7, targetY);
		args.set(8, targetX + targetWidth);
		args.set(9, targetY + targetHeight);

		// Nearest neighbour keeps pixel edges crisp, linear avoids a harsh image when scaling
		args.set(11, FullscreenFix.SHARP_SCALING.getBoolean() ? GL_NEAREST : GL_LINEAR);
	}

	/**
	 * Anything outside the presented frame keeps whatever the back buffer happened to contain, so
	 * the bars left by keeping the aspect ratio have to be cleared to black first.
	 */
	@Inject(method = "presentTexture", at = @At("HEAD"))
	private void clearBorderAroundFrame(GpuTextureView textureView, CallbackInfo ci) {
		if(FullscreenFix.getActiveRenderResolution() == null) {
			return;
		}
		if(!isMainRenderTarget(textureView) || !FullscreenFix.hasEmptyBorder()) {
			return;
		}

		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);
		GlStateManager._disableScissorTest();
		GlStateManager._colorMask(15);

		// Blaze3d does not track the clear colour, so it has to be set directly
		GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		GlStateManager._clear(GL11.GL_COLOR_BUFFER_BIT);
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
