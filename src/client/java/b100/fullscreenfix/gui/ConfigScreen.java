package b100.fullscreenfix.gui;

import b100.fullscreenfix.FullscreenFix;
import b100.fullscreenfix.Global;
import b100.fullscreenfix.VideoMode;
import b100.lib.client.gui.GuiButton;
import b100.lib.client.gui.GuiContainer;
import b100.lib.client.gui.GuiElement;
import b100.lib.client.gui.GuiScrollListScreen;
import b100.lib.client.gui.config.BooleanToggleElement;
import b100.lib.client.gui.config.ConfigElement;
import b100.lib.client.gui.config.CustomOptionElement;
import b100.lib.client.gui.config.SaveConfigButton;
import b100.lib.client.mixin.IScreen;
import net.minecraft.text.Text;

public class ConfigScreen extends GuiScrollListScreen {
	
	public GuiButton cancelButton;
	public SaveConfigButton saveConfigButton;

	private CustomOptionElement<VideoMode> fullscreenResolutionButton;
	
	public ConfigScreen(IScreen parentScreen) {
		super(parentScreen);
		
		title = FullscreenFix.TRANS.asText("screen.fullscreenSettings.title");
	}
	
	@Override
	public void onInit() {
		saveConfigButton = new SaveConfigButton(this);
		saveConfigButton.addActionListener((e) -> {
			FullscreenFix.saveConfig();
			back();
		});
		
		super.onInit();
		
		cancelButton = add(new GuiButton(this, FullscreenFix.TRANS.asText("button.cancel")).addActionListener((e) -> back()));
		add(saveConfigButton);
	}
	
	@Override
	public void initScrollElements() {
		scrollList.add(new BooleanToggleElement(this, "option.enableMod", FullscreenFix.isModEnabledNextLaunch()).addSaveConsumer(newValue -> FullscreenFix.setModEnabled(newValue)));
		
		if(Global.MOD_ENABLED) {
			scrollList.add(new BooleanToggleElement(this, "option.fullscreen", FullscreenFix.isFullscreenEnabled()).addSaveConsumer(newValue -> FullscreenFix.setFullscreen(newValue)));
			scrollList.add(new BooleanToggleElement(this, "option.borderlessFullscreen", FullscreenFix.isBorderlessEnabled()).addSaveConsumer(newValue -> FullscreenFix.setBorderless(newValue)));
			
			if(Global.OS_WINDOWS) {
				scrollList.add(new BooleanToggleElement(this, "option.windowsFullscreenOptimizations", FullscreenFix.isWindowsFullscreenOptimizationsEnabled()).addSaveConsumer(newValue -> FullscreenFix.setWindowsFullscreenOptimizations(newValue)));
			}

			scrollList.add(new BooleanToggleElement(this, "option.captureCursorInFullscreen", FullscreenFix.isCaptureCursorInFullscreenEnabled()).addSaveConsumer(newValue -> FullscreenFix.setCaptureCursorInFullscreen(newValue)));
			scrollList.add(new BooleanToggleElement(this, "option.autoMinimize", FullscreenFix.isAutoMinimizeEnabled()).addSaveConsumer(newValue -> FullscreenFix.setAutoMinimize(newValue)));
			scrollList.add(new BooleanToggleElement(this, "option.startInFullscreen", FullscreenFix.isStartInFullscreenEnabled()).addSaveConsumer(newValue -> FullscreenFix.setStartInFullscreen(newValue)));
			
			fullscreenResolutionButton = scrollList.add(new CustomOptionElement<>(this, "option.fullscreenResolution", FullscreenFix.getFullscreenVideoMode())
			.addActionListener((e) -> utils.setScreen(new ScreenResolutionsMenu(this)))
			.setToTextFunction((videoMode) -> videoMode != null ? Text.of(videoMode.toString()) : FullscreenFix.TRANS.asText("value.fullscreenResolution.default")));
			
			scrollList.add(new BooleanToggleElement(this, "option.replaceVideoSettings", FullscreenFix.shouldReplaceVideoSettingsButton()).addSaveConsumer(newValue -> FullscreenFix.setReplaceVideoSettingsButton(newValue)));
			scrollList.add(new BooleanToggleElement(this, "option.configScreenHotkeyEnabled", FullscreenFix.isConfigScreenHotkeyEnabled()).addSaveConsumer(newValue -> FullscreenFix.setConfigScreenHotkeyEnabled(newValue)));
		}
	}
	
	@Override
	public void elementAdded(GuiContainer parent, GuiElement element) {
		if(element instanceof ConfigElement) {
			ConfigElement<?> configElement = (ConfigElement<?>) element;
			configElement.addConfigElementListener(saveConfigButton);
		}
		super.elementAdded(parent, element);
	}
	
	@Override
	public void onResize() {
		setDoubleFooterButtonPositions(saveConfigButton, cancelButton);
		super.onResize();
	}
	
	@Override
	public void onScreenOpened() {
		if(fullscreenResolutionButton != null) {
			fullscreenResolutionButton.setValue(FullscreenFix.getFullscreenVideoMode());
		}
		super.onScreenOpened();
	}
}
