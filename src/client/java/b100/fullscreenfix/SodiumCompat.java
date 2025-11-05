package b100.fullscreenfix;

import b100.lib.client.translate.Translate;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl;
import net.caffeinemc.mods.sodium.client.gui.options.control.CyclingControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraft.text.Text;

public class SodiumCompat {
	
	public static OptionStorageImpl optionStorage = new OptionStorageImpl();
	
	public static OptionImpl<Object, FullscreenMode> getCustomFullscreenButton() {
		return OptionImpl.createBuilder(FullscreenMode.class, optionStorage)
				.setName(Translate.translate("option.fullscreen"))
				.setTooltip(Text.translatable("sodium.options.fullscreen.tooltip"))
				.setControl(option1 -> new CyclingControl<>(option1, FullscreenMode.class, new Text[] {
						Translate.translate("option.fullscreen.off"),
						Translate.translate("option.fullscreen.on"),
						Translate.translate("option.fullscreen.borderless")
				}))
				.setBinding((options, value) -> FullscreenFix.setFullscreenMode(value), (options) -> FullscreenFix.getCurrentFullscreenMode())
				.build();
	}
	
	public static class OptionStorageImpl implements OptionStorage<Object> {

		@Override
		public Object getData() {
			return null;
		}

		@Override
		public void save() {
			FullscreenFix.saveConfig();
		}
		
	}
}
