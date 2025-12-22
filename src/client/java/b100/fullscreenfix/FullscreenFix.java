package b100.fullscreenfix;

import static b100.fullscreenfix.Global.*;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.serialization.Codec;

import b100.fullscreenfix.gui.ConfigScreen;
import b100.fullscreenfix.mixin.access.WindowAccess;
import b100.fullscreenfix.util.GLFWUtil;
import b100.lib.client.gui.GuiUtils;
import b100.lib.client.mixin.IScreen;
import b100.lib.client.translate.Translations;
import b100.lib.client.util.ConfigUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.option.SimpleOption.TooltipFactory;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class FullscreenFix {

	private static Window window;
	
	// Config
	
	public static boolean windowNeedsUpdate = true;
	public static boolean fullscreenModeWasChanged = false;
	
	private static boolean enableModNextLaunch = Global.MOD_ENABLED;
	private static boolean borderlessFullscreen = true;
	private static boolean fullscreenOptimizations = true;
	private static boolean autoMinimize = true;
	private static boolean startInFullscreen = true;
	private static boolean replaceVideoSettingsButton = true;
	private static boolean captureCursorInFullscreen = false;
	private static boolean configScreenHotkeyEnabled = true;
	
	/**
	 * May be null for current resolution
	 */
	private static VideoMode fullscreenVideoMode;
	
	/**
	 * Custom option for the vanilla video settings menu
	 */
	public static SimpleOption<Integer> fullscreenOption = createFullscreenOption();
	
	public static final Translations TRANS = Translations.get(null);
	
	static {
		Translations.loadFromNamespace(MODID);
		
		loadConfig();
	}

	@SuppressWarnings("resource")
	public static void openConfigScreen() {
		IScreen currentScreen = (IScreen) MinecraftClient.getInstance().currentScreen;
		if(!(currentScreen instanceof ConfigScreen)) {
			GuiUtils.instance.setScreen(new ConfigScreen(null));	
		}
	}
	
	////////////////////////////////////
	
	public static boolean isModEnabledNextLaunch() {
		return enableModNextLaunch;
	}
	
	public static void setModEnabled(boolean value) {
		enableModNextLaunch = value;
	}
	
	public static boolean isBorderlessEnabled() {
		return borderlessFullscreen;
	}
	
	public static void setBorderless(boolean value) {
		if(value != borderlessFullscreen) {
			borderlessFullscreen = value;
			updateWindow();
		}
	}
	
	public static boolean isWindowsFullscreenOptimizationsEnabled() {
		return fullscreenOptimizations;
	}
	
	public static void setWindowsFullscreenOptimizations(boolean value) {
		if(value != fullscreenOptimizations) {
			fullscreenOptimizations = value;
			updateWindow();
		}
	}
	
	public static boolean isFullscreenEnabled() {
		if(window == null) {
			return false;
		}
		return window.isFullscreen();
	}
	
	public static void setFullscreen(boolean value) {
		WindowAccess access = (WindowAccess)(Object)window;
		access.setFullscreen(value);

		SimpleOption<Boolean> fullscreenOption = getVanillaFullscreenOption();
		if(fullscreenOption != null) {
			fullscreenOption.setValue(value);
		}
	}
	
	public static VideoMode getFullscreenVideoMode() {
		return fullscreenVideoMode;
	}
	
	public static void setFullscreenVideoMode(VideoMode value) {
		if(!VideoMode.compare(fullscreenVideoMode, value)) {
			fullscreenVideoMode = value;
			updateWindow();
		}
	}
	
	public static boolean isAutoMinimizeEnabled() {
		return autoMinimize;
	}
	
	public static void setAutoMinimize(boolean value) {
		if(autoMinimize != value) {
			autoMinimize = value;
			updateWindow();
		}
	}
	
	public static boolean isStartInFullscreenEnabled() {
		return startInFullscreen;
	}
	
	public static void setStartInFullscreen(boolean value) {
		startInFullscreen = value;
	}
	
	public static void setReplaceVideoSettingsButton(boolean value) {
		replaceVideoSettingsButton = value;
	}
	
	public static boolean shouldReplaceVideoSettingsButton() {
		return replaceVideoSettingsButton;
	}
	
	public static void setCaptureCursorInFullscreen(boolean value) {
		captureCursorInFullscreen = value;
		
		GLFWUtil.updateCursorMode();
	}
	
	public static boolean isCaptureCursorInFullscreenEnabled() {
		return captureCursorInFullscreen;
	}
	
	public static void setConfigScreenHotkeyEnabled(boolean value) {
		configScreenHotkeyEnabled = value;
	}
	
	public static boolean isConfigScreenHotkeyEnabled() {
		return configScreenHotkeyEnabled;
	}
	
	////////////////////////////////////
	
	public static void updateWindow() {
		windowNeedsUpdate = true;
	}
	
	public static void setWindow(Window window) {
		FullscreenFix.window = window;
	}
	
	////////////////////////////////////
	
	private static SimpleOption<Integer> createFullscreenOption() {
		return new SimpleOption<Integer>("fullscreenoverride",
				SimpleOption.emptyTooltip(),
				(text, value) -> Text.of("idkwhatthisdoes"),
				new SimpleOption.Callbacks<>() {
					@Override
					public Function<SimpleOption<Integer>, ClickableWidget> getWidgetCreator(TooltipFactory<Integer> tooltipFactory, GameOptions gameOptions, int x, int y, int width, Consumer<Integer> changeCallback) {
						return option -> {
							final ButtonWidget button = ButtonWidget.builder(getFullscreenModeDisplayText(), (pressedButton) -> {
								setFullscreenMode(getCurrentFullscreenMode().next());
								pressedButton.setMessage(getFullscreenModeDisplayText());
							}).build();
							return button;
						};
					}
					@Override
					public Optional<Integer> validate(Integer value) {
						return Optional.of(MathHelper.clamp(value, 0, 2));
					}
					@Override
					public Codec<Integer> codec() {
						return Codec.INT;
					}
				}, 0, (newValue) -> {}
		);
	}
	
	private static Text getFullscreenModeDisplayText() {
		FullscreenMode mode = getCurrentFullscreenMode();
		StringBuilder str = new StringBuilder();
		str.append(TRANS.asString("option.fullscreen")).append(": ");
		if(mode == FullscreenMode.BORDERLESS) {
			str.append(TRANS.asString("option.fullscreen.borderless"));
		}else if(mode == FullscreenMode.ON) {
			str.append(TRANS.asString("option.fullscreen.on"));
		}else {
			str.append(TRANS.asString("option.fullscreen.off"));
		}
		return Text.of(str.toString());
	}
	
	public static FullscreenMode getCurrentFullscreenMode() {
		if(isFullscreenEnabled()) {
			if(isBorderlessEnabled()) {
				return FullscreenMode.BORDERLESS;
			}
			return FullscreenMode.ON;
		}
		return FullscreenMode.OFF;
	}
	
	public static void setFullscreenMode(FullscreenMode mode) {
		if(mode == FullscreenMode.OFF) {
			setFullscreen(false);
		}else {
			setFullscreen(true);
			if(mode == FullscreenMode.BORDERLESS) {
				setBorderless(true);
			}else {
				setBorderless(false);
			}
		}
		fullscreenModeWasChanged = true;
	}
	
	@SuppressWarnings("resource")
	public static SimpleOption<Boolean> getVanillaFullscreenOption() {
		try {
			return MinecraftClient.getInstance().options.getFullscreen();	
		}catch (NullPointerException e) {
			return null;
		}
	}
	
	////////////////////////////////////
	
	public static void loadConfig() {
		ConfigUtil.loadConfig(CONFIG_FILE, (key, value) -> parse(key, value), ':');
	}
	
	public static void saveConfig() {
		StringBuilder str = new StringBuilder();
		str.append("enableMod:" + enableModNextLaunch + "\n");
		str.append("borderlessFullscreen:" + borderlessFullscreen + "\n");
		str.append("fullscreenOptimizations:" + fullscreenOptimizations + "\n");
		str.append("captureCursorInFullscreen:" + captureCursorInFullscreen + "\n");
		str.append("autoMinimize:" + autoMinimize + "\n");
		str.append("startInFullscreen:" + startInFullscreen + "\n");
		if(fullscreenVideoMode != null) {
			str.append("fullscreenMode:" + fullscreenVideoMode.toConfigString() + "\n");	
		}
		str.append("replaceVideoSettingsButton:" + replaceVideoSettingsButton + "\n");
		str.append("configScreenHotkeyEnabled:" + configScreenHotkeyEnabled + "\n");
		
		ConfigUtil.saveStringToFile(str.toString(), CONFIG_FILE);
	}

	public static void parse(String key, String value) {
		if(key.equals("borderlessFullscreen")) {
			borderlessFullscreen = value.equalsIgnoreCase("true");
		}else if(key.equals("fullscreenOptimizations")) {
			fullscreenOptimizations = value.equalsIgnoreCase("true");
		}else if(key.equals("autoMinimize")) {
			autoMinimize = value.equalsIgnoreCase("true");
		}else if(key.equals("startInFullscreen")) {
			startInFullscreen = value.equalsIgnoreCase("true");
		}else if(key.equals("fullscreenMode")) {
			fullscreenVideoMode = VideoMode.parse(value);
		}else if(key.equals("replaceVideoSettingsButton")) {
			replaceVideoSettingsButton = value.equalsIgnoreCase("true");
		}else if(key.equals("captureCursorInFullscreen")) {
			captureCursorInFullscreen = value.equalsIgnoreCase("true");
		}else if(key.equals("configScreenHotkeyEnabled")) {
			configScreenHotkeyEnabled = value.equalsIgnoreCase("true");
		}
	}
	
	////////////////////////////////////
	
	public static void debugPrint(String string) {
		if(INDEV) {
			System.out.print("[FullscreenFixDebug] " + string + "\n");
		}
	}
	
	public static void print(String string) {
		Global.print(string);
	}

}
