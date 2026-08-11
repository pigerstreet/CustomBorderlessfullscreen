package b100.fullscreenfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import b100.fullscreenfix.FullscreenFix;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

/**
 * Sizes a picture in picture texture belonging to another mod to the pixels it will actually cover.
 *
 * These elements are rendered into a texture and then stretched over the rectangle they occupy, and
 * vanilla sizes that texture as the size of the rectangle in gui units times the gui scale. That is
 * exactly the pixels it covers only while a gui unit really is the gui scale across, which is what a
 * gui resolution stops being true, and by a different amount on each axis when the resolution is not
 * the shape of the window.
 *
 * No single gui scale can express both amounts, so the texture is sized per axis here instead. On a
 * 2560x1440 window laid out as 1720x1080 a gui unit is 2.98 pixels across and 2.67 down; sizing by a
 * scale of 3 gives a texture 1620 tall stretched onto 1440, and content drawn into it lands 11% above
 * where the mod that drew it thinks it did. Since it hit tests against its own idea of where that is,
 * everything it draws responds slightly above the pointer, which is what Odin's click gui does.
 *
 * This is only for renderers that did not come with the game. Vanilla's draw through a pose scaled by
 * the gui scale it was given, one number for both axes, so their content would no longer fill a
 * texture measured per axis. They keep the enlarged scale, which for them agrees with itself.
 */
@Mixin(value = PictureInPictureRenderer.class)
public class PictureInPictureRendererMixin {

	@ModifyVariable(method = "prepare", at = @At("STORE"), index = 4)
	private int guiResolutionTextureWidth(int textureWidth, PictureInPictureRenderState state, GuiRenderState guiRenderState, int guiScale) {
		if(!sizesItsOwnContent()) {
			return textureWidth;
		}
		return toPixels(state.x1() - state.x0(), FullscreenFix.getPixelsPerGuiUnitX(), textureWidth);
	}

	@ModifyVariable(method = "prepare", at = @At("STORE"), index = 5)
	private int guiResolutionTextureHeight(int textureHeight, PictureInPictureRenderState state, GuiRenderState guiRenderState, int guiScale) {
		if(!sizesItsOwnContent()) {
			return textureHeight;
		}
		return toPixels(state.y1() - state.y0(), FullscreenFix.getPixelsPerGuiUnitY(), textureHeight);
	}

	private static int toPixels(int guiUnits, double pixelsPerUnit, int fallback) {
		if(guiUnits <= 0 || pixelsPerUnit <= 0.0) {
			return fallback;
		}
		return Math.max(1, (int) Math.round(guiUnits * pixelsPerUnit));
	}

	/**
	 * Whether this renderer draws its content at a size of its own choosing rather than through the
	 * pose it is handed. Everything that did not come with the game is treated as though it does,
	 * because the texture then matches the screen exactly and that is what such a renderer assumes.
	 */
	private boolean sizesItsOwnContent() {
		if(FullscreenFix.getActiveGuiResolution() == null) {
			return false;
		}
		final String name = getClass().getName();
		// Mixin renames the target class, so the package identifies it rather than the name
		return !name.startsWith("net.minecraft.") && !name.startsWith("com.mojang.");
	}
}
