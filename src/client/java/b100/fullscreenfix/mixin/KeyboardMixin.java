package b100.fullscreenfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import b100.fullscreenfix.FullscreenFix;

@Mixin(value = KeyboardHandler.class)
public class KeyboardMixin {
	
	@WrapOperation(
		method = "keyPress",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/platform/Window;toggleFullScreen()V"
		)
	)
	private void openMenuInsteadOfTogglingFullscreen(Window instance, Operation<Void> original) {
		if(Screen.hasControlDown() && FullscreenFix.CONFIG_SCREEN_HOTKEY_ENABLED.getBoolean()) {
			FullscreenFix.openConfigScreen();
			return;
		}
		original.call(instance);
	}
	
}
