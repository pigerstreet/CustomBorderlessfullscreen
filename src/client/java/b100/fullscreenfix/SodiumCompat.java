package b100.fullscreenfix;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.EnumOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder.FullscreenMode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class SodiumCompat {
	
	public static OptionBuilder getCustomFullscreenButton(ConfigBuilder builder) {
		EnumOptionBuilder<FullscreenMode> optionBuilder = builder.createEnumOption(Identifier.fromNamespaceAndPath("sodium", "general.fullscreen_mode"), FullscreenMode.class);
		
		Consumer<FullscreenMode> set = value -> FullscreenFix.setFullscreenMode(value);
		Supplier<FullscreenMode> get = FullscreenFix::getCurrentFullscreenMode;
		StorageEventHandler save = FullscreenFix.CONFIG::save;
		
		optionBuilder.setName(FullscreenFix.TRANS.asText("option.fullscreen"));
		optionBuilder.setStorageHandler(save);
		optionBuilder.setTooltip(Component.translatable("sodium.options.fullscreen_mode.tooltip"));
		optionBuilder.setDefaultValue(FullscreenMode.OFF);
		optionBuilder.setBinding(set, get);
		optionBuilder.setElementNameProvider(value -> {
			if(value == FullscreenMode.EXCLUSIVE) {
				return FullscreenFix.TRANS.asText("option.fullscreen.exclusive");
			}
			if(value == FullscreenMode.BORDERLESS) {
				return FullscreenFix.TRANS.asText("option.fullscreen.borderless");
			}
			return FullscreenFix.TRANS.asText("option.fullscreen.off");
		});
		
		return optionBuilder;
	}
}
