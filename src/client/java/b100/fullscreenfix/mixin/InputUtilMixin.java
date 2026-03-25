package b100.fullscreenfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import b100.fullscreenfix.FullscreenFix;
import b100.fullscreenfix.util.GLFWUtil;
import com.mojang.blaze3d.platform.InputConstants;

@Mixin(value = InputConstants.class)
public class InputUtilMixin {
	
	@ModifyArg(
		method = "grabOrReleaseMouse",
		at = @At(
			value = "INVOKE",
			target = "Lorg/lwjgl/glfw/GLFW;glfwSetInputMode(JII)V"
		),
		index = 2
	)
	private static int modifyCursorParameter(int cursorMode) {
		final int prevCursorMode = cursorMode;
		cursorMode = GLFWUtil.getUpdatedCursorMode(cursorMode);
		if(cursorMode != prevCursorMode) {
			FullscreenFix.debugPrint("Modified Cursor Mode: " + GLFWUtil.getCursorModeString(prevCursorMode) + " -> " + GLFWUtil.getCursorModeString(cursorMode));	
		}
		return cursorMode;
	}
}
