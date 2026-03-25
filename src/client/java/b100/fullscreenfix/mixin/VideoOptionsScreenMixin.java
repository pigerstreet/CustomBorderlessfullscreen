package b100.fullscreenfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import b100.fullscreenfix.FullscreenFix;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;

@Mixin(value = VideoSettingsScreen.class)
public abstract class VideoOptionsScreenMixin extends OptionsSubScreen {

	// no
	private VideoOptionsScreenMixin(Screen parent, Options gameOptions, Component title) {
		super(parent, gameOptions, title);
	}
	
	@ModifyArg(method = "addOptions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/OptionsList;addSmall([Lnet/minecraft/client/OptionInstance;)V"), index = 0)
	private OptionInstance<?>[] replaceFullscreenOption(OptionInstance<?>[] options) {
		if(FullscreenFix.REPLACE_VIDEO_SETTINGS_BUTTON.getBoolean()) {
			OptionInstance<?> fullscreenOption = FullscreenFix.getVanillaFullscreenOption();
			for(int i=0; i < options.length; i++) {
				OptionInstance<?> option = options[i];
				if(option == fullscreenOption) {
					options[i] = FullscreenFix.fullscreenOption;
				}
			}	
		}
		return options;
	}
	
	@Inject(method = "onClose", at = @At(value = "TAIL"))
	private void onClose(CallbackInfo ci) {
		if(FullscreenFix.fullscreenModeWasChanged) {
			FullscreenFix.print("Save Config");
			FullscreenFix.CONFIG.save();
			FullscreenFix.fullscreenModeWasChanged = false;
		}
	}
}
