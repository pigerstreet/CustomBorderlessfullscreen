package b100.fullscreenfix.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder;

@Mixin(value = SodiumConfigBuilder.class, remap = false)
public class SodiumConfigBuilderMixin {
	
	@WrapOperation(method = "lambda$buildGeneralPage$6", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/caffeinemc/mods/sodium/client/config/structure/Config;onGameNeedsRestart()V"))
	private static void removeRestartRequiredMessage(Operation<Void> original) {
		
	}
	
}
