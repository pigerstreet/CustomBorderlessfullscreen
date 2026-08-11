package b100.fullscreenfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import b100.fullscreenfix.FullscreenFix;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;

/**
 * Keeps the enlarged gui scale away from picture in picture renderers that did not come with the game.
 *
 * Each of these renders into a texture whose size is this scale times the size of the element, and a
 * gui resolution makes a gui unit cover more pixels than the gui scale says, so vanilla's renderers
 * are handed a larger one to stop them being the only blurry part of a sharp gui. That works because
 * they draw through the pose they are given, which is scaled by the same number, so the content grows
 * with the texture.
 *
 * A renderer from another mod need not do that. One that draws its content at a size of its own
 * choosing, which is what anything rendering with NanoVG does, puts the same content into a texture
 * that is now half again too big, and it comes out smaller and higher up than the mod expects. That
 * is invisible until it is clicked on, at which point the mod hit tests against where it thinks it
 * drew and everything responds slightly away from the pointer.
 *
 * There is no way to grow such content to match, since the mod never told anyone how it is sized.
 * Leaving those textures at the real gui scale gives that mod exactly what it would get without this
 * feature, which costs a little sharpness in a place another mod is in charge of anyway.
 */
@Mixin(value = PictureInPictureRenderer.class)
public class PictureInPictureRendererMixin {

	@ModifyVariable(method = "prepare", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int guiResolutionKeepScaleForForeignRenderers(int guiScale) {
		if(FullscreenFix.getActiveGuiResolution() == null) {
			return guiScale;
		}
		if(isVanilla()) {
			return guiScale;
		}
		return FullscreenFix.getRealGuiScale(guiScale);
	}

	private boolean isVanilla() {
		final String name = getClass().getName();
		// Mixin renames the target class, so the package is what identifies it rather than the name
		return name.startsWith("net.minecraft.") || name.startsWith("com.mojang.");
	}
}
