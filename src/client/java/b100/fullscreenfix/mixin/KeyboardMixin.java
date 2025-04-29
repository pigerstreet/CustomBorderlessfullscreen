package b100.fullscreenfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import b100.fullscreenfix.FullscreenFix;
import net.minecraft.client.Keyboard;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;

@Mixin(value = Keyboard.class)
public class KeyboardMixin {
	
	@WrapOperation(
		method = "onKey",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/util/Window;toggleFullscreen()V"
		)
	)
	private void openMenuInsteadOfTogglingFullscreen(Window instance, Operation<Void> original) {
		if(Screen.hasControlDown() && FullscreenFix.isConfigScreenHotkeyEnabled()) {
			FullscreenFix.openConfigScreen();
			return;
		}
		original.call(instance);
	}
	
}
