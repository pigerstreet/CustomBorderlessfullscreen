package b100.fullscreenfix.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Window;

import b100.fullscreenfix.FullscreenFix;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;

@Mixin(value = KeyboardHandler.class)
public class KeyboardMixin {
	
	@WrapOperation(
		method = "keyPress",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/platform/Window;toggleFullScreen()V"
		)
	)
	private void openMenuInsteadOfTogglingFullscreen(Window instance, Operation<Void> original, @Local(argsOnly = true) KeyEvent event) {
		if((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0 && FullscreenFix.CONFIG_SCREEN_HOTKEY_ENABLED.getBoolean()) {
			FullscreenFix.openConfigScreen();
			return;
		}
		original.call(instance);
	}
	
}
