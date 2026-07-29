package b100.fullscreenfix.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import b100.fullscreenfix.FullscreenFix;
import net.minecraft.client.MouseHandler;

@Mixin(value = MouseHandler.class)
public class MouseHandlerMixin {

	/**
	 * Cursor positions arrive from GLFW in the real coordinate space of the window, while the game
	 * is told the window has the size of the custom render resolution. They have to be scaled into
	 * that same space, otherwise everything that turns a cursor position into gui coordinates ends
	 * up off by the ratio between the two.
	 */
	@ModifyVariable(method = "onMove", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private double scaleCursorX(double xpos) {
		return xpos * FullscreenFix.getCursorScaleX();
	}

	@ModifyVariable(method = "onMove", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private double scaleCursorY(double ypos) {
		return ypos * FullscreenFix.getCursorScaleY();
	}

	/**
	 * Looking around uses the accumulated cursor movement directly rather than as a position, so
	 * scaling it would quietly change the mouse sensitivity. Scaling it back undoes that, which
	 * keeps looking around feeling exactly the same as it does without a render resolution.
	 */
	@ModifyExpressionValue(method = "turnPlayer", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/MouseHandler;accumulatedDX:D"))
	private double undoCursorScaleForTurningX(double accumulatedDX) {
		final double scale = FullscreenFix.getCursorScaleX();
		return scale == 0.0 ? accumulatedDX : accumulatedDX / scale;
	}

	@ModifyExpressionValue(method = "turnPlayer", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/MouseHandler;accumulatedDY:D"))
	private double undoCursorScaleForTurningY(double accumulatedDY) {
		final double scale = FullscreenFix.getCursorScaleY();
		return scale == 0.0 ? accumulatedDY : accumulatedDY / scale;
	}
}
