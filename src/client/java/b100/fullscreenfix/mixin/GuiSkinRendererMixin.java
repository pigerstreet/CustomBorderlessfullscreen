package b100.fullscreenfix.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import b100.fullscreenfix.FullscreenFix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.GuiSkinRenderer;
import net.minecraft.client.renderer.state.WindowRenderState;

/**
 * Keeps the player model in the inventory the right size when a custom gui resolution is used.
 *
 * The model is drawn into its own texture, sized by {@link GuiRendererMixin} from the real size of a
 * gui unit rather than from the gui scale, but the scale the model itself is drawn at is read
 * straight off the window state. Leaving that one alone would draw the model at the wrong size
 * inside a correctly sized texture.
 */
@Mixin(value = GuiSkinRenderer.class)
public class GuiSkinRendererMixin {

	@ModifyExpressionValue(method = "renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/GuiSkinRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/renderer/state/WindowRenderState;guiScale:I"))
	private int guiResolutionSkinScale(int guiScale) {
		final Minecraft minecraft = Minecraft.getInstance();
		if(minecraft == null || minecraft.gameRenderer == null) {
			return guiScale;
		}

		final WindowRenderState window = minecraft.gameRenderer.getGameRenderState().windowRenderState;
		if(window == null || window.width <= 0 || window.height <= 0) {
			return guiScale;
		}

		return FullscreenFix.getEffectiveGuiScale(window.width, window.height, guiScale);
	}
}
