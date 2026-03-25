package b100.fullscreenfix.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import b100.fullscreenfix.SodiumCompat;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder;

@Mixin(value = SodiumConfigBuilder.class, remap = false)
public class SodiumConfigBuilderMixin {
	
	@WrapOperation(method = "lambda$buildGeneralPage$6", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/caffeinemc/mods/sodium/client/config/structure/Config;onGameNeedsRestart()V"))
	private static void removeRestartRequiredMessage(Operation<Void> original) {
		
	}
	
	@ModifyArg(
			method = "buildGeneralPage",
			at = @At(
					value = "INVOKE", ordinal = 0,
					target = "Lnet/caffeinemc/mods/sodium/api/config/structure/OptionGroupBuilder;addOption(Lnet/caffeinemc/mods/sodium/api/config/structure/OptionBuilder;)Lnet/caffeinemc/mods/sodium/api/config/structure/OptionGroupBuilder;"
			),
			slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=sodium:general.fullscreen_mode"))
	)
	private static OptionBuilder replaceFullscreenButton(OptionBuilder optionBuilder, @Local(argsOnly = true) ConfigBuilder builder) {
		return SodiumCompat.getCustomFullscreenButton(builder);
	}
	
}
