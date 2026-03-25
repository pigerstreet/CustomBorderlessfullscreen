package b100.fullscreenfix;

public class SodiumCompat {
	
//	public static OptionStorageImpl optionStorage = new OptionStorageImpl();
//	
//	public static OptionImpl<Object, FullscreenMode> getCustomFullscreenButton() {
//		return OptionImpl.createBuilder(FullscreenMode.class, optionStorage)
//				.setName(FullscreenFix.TRANS.asText("option.fullscreen"))
//				.setTooltip(Component.translatable("sodium.options.fullscreen.tooltip"))
//				.setControl(option1 -> new CyclingControl<>(option1, FullscreenMode.class, new Component[] {
//					FullscreenFix.TRANS.asText("option.fullscreen.off"),
//					FullscreenFix.TRANS.asText("option.fullscreen.on"),
//					FullscreenFix.TRANS.asText("option.fullscreen.borderless")
//				}))
//				.setBinding((options, value) -> FullscreenFix.setFullscreenMode(value), (options) -> FullscreenFix.getCurrentFullscreenMode())
//				.build();
//	}
//	
//	public static class OptionStorageImpl implements OptionStorage<Object> {
//
//		@Override
//		public Object getData() {
//			return null;
//		}
//
//		@Override
//		public void save() {
//			FullscreenFix.CONFIG.save();
//		}
//		
//	}
}
