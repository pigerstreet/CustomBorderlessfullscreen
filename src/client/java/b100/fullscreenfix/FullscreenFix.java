package b100.fullscreenfix;

import static b100.fullscreenfix.Global.INDEV;
import static b100.fullscreenfix.Global.MODID;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.blaze3d.platform.Window;
import com.mojang.serialization.Codec;

import b100.fullscreenfix.gui.ConfigScreen;
import b100.fullscreenfix.mixin.access.WindowAccess;
import b100.fullscreenfix.util.GLFWUtil;
import b100.lib.client.gui.util.GuiUtils;
import b100.lib.client.mixin.IScreen;
import b100.lib.config.properties.PropertiesFile;
import b100.lib.config.property.BooleanProperty;
import b100.lib.config.property.BooleanPropertyImpl;
import b100.lib.config.property.Property;
import b100.lib.translate.Translations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.OptionInstance.TooltipSupplier;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.Util.OS;

public class FullscreenFix {
	
	private static Window window;

	public static boolean windowNeedsUpdate = true;
	public static boolean fullscreenModeWasChanged = false;
	public static boolean renderResolutionNeedsUpdate = false;
	public static boolean guiResolutionNeedsUpdate = false;

	/**
	 * The actual size of the window framebuffer, in pixels.
	 *
	 * While a custom render resolution is active the game is told that the framebuffer has the size
	 * of that resolution, so the real size has to be kept separately: it is what the finished frame
	 * gets stretched onto.
	 */
	public static int realFramebufferWidth = 0;
	public static int realFramebufferHeight = 0;

	/**
	 * The actual size of the window in screen coordinates.
	 *
	 * The game is told that the window has the size of the custom render resolution instead, so that
	 * everything reading the window size gets numbers that match the coordinate space the game is
	 * rendering and laying its gui out in. Cursor positions arrive from GLFW in the real coordinate
	 * space though, so they have to be scaled into that same space.
	 */
	public static int realScreenWidth = 0;
	public static int realScreenHeight = 0;

	/**
	 * Upper limit for the scale item icons and picture in picture elements are rendered at.
	 *
	 * A gui resolution much smaller than the window asks for a very large scale, and every step costs
	 * video memory in the item atlas without being visible, so it is worth stopping somewhere.
	 */
	private static final int MAX_EFFECTIVE_GUI_SCALE = 8;
	
	// Config
	public static final PropertiesFile CONFIG = new PropertiesFile(Global.CONFIG_FILE);
	
	public static final BooleanProperty ENABLE_NEXT_LAUNCH = BooleanProperty.create(Global.MOD_ENABLED);
	public static final BooleanProperty ENABLE_FULLSCREEN = BooleanProperty.create(false, FullscreenFix::isFullscreenEnabled, FullscreenFix::setFullscreen);
	public static final BooleanProperty EXCLUSIVE_FULLSCREEN = new BooleanPropertyImpl(getExclusiveFullscreenDefaultValue()) {
		@Override
		public void setBoolean(boolean value) {
			if(value != getBoolean()) {
				super.setBoolean(value);
				updateWindow();
			}
		}
	};
	public static final BooleanProperty AUTO_MINIMIZE = new BooleanPropertyImpl(false) {
		@Override
		public void setBoolean(boolean value) {
			if(value != getBoolean()) {
				super.setBoolean(value);
				updateWindow();
			}
		};
	};
	public static final BooleanProperty START_IN_FULLSCREEN = new BooleanPropertyImpl(true);
	public static final BooleanProperty REPLACE_VIDEO_SETTINGS_BUTTON = new BooleanPropertyImpl(true);
	public static final BooleanProperty CAPTURE_CURSOR = new BooleanPropertyImpl(false) {
		@Override
		public void setBoolean(boolean value) {
			super.setBoolean(value);
			GLFWUtil.updateCursorMode();
		};
	};
	public static final BooleanProperty CONFIG_SCREEN_HOTKEY_ENABLED = new BooleanPropertyImpl(true);
	public static Property<VideoMode> FULLSCREEN_VIDEO_MODE = Property.create(null, VideoMode::parse).addValueChangeListener(value -> updateWindow());
	public static Property<RenderResolution> RENDER_RESOLUTION = Property.create(null, RenderResolution::parse).addValueChangeListener(value -> renderResolutionNeedsUpdate = true);
	public static final BooleanProperty KEEP_ASPECT_RATIO = new BooleanPropertyImpl(false);
	public static final BooleanProperty SHARP_SCALING = new BooleanPropertyImpl(false);
	public static Property<RenderResolution> GUI_RESOLUTION = Property.create(null, RenderResolution::parse).addValueChangeListener(value -> guiResolutionNeedsUpdate = true);
	public static final BooleanProperty GUI_KEEP_ASPECT_RATIO = new BooleanPropertyImpl(false) {
		@Override
		public void setBoolean(boolean value) {
			if(value != getBoolean()) {
				super.setBoolean(value);
				guiResolutionNeedsUpdate = true;
			}
		};
	};
	public static Property<MonitorInfo> LAST_FULLSCREEN_MONITOR = Property.create(null, MonitorInfo::fromConfigString).addValueChangeListener(value -> {
		debugPrint("Change fullscreen monitor: " + value.toConfigString());
		CONFIG.save();
	});
	
	static {
		CONFIG.add("enableMod", ENABLE_NEXT_LAUNCH);
		CONFIG.add("exclusiveFullscreen", EXCLUSIVE_FULLSCREEN);
		CONFIG.add("autoMinimize", AUTO_MINIMIZE);
		CONFIG.add("startInFullscreen", START_IN_FULLSCREEN);
		CONFIG.add("replaceVideoSettingsButton", REPLACE_VIDEO_SETTINGS_BUTTON);
		CONFIG.add("captureCursorInFullscreen", CAPTURE_CURSOR);
		CONFIG.add("configScreenHotkeyEnabled", CONFIG_SCREEN_HOTKEY_ENABLED);
		CONFIG.add("fullscreenVideoMode", FULLSCREEN_VIDEO_MODE);
		CONFIG.add("renderResolution", RENDER_RESOLUTION);
		CONFIG.add("renderResolutionKeepAspectRatio", KEEP_ASPECT_RATIO);
		CONFIG.add("renderResolutionSharpScaling", SHARP_SCALING);
		CONFIG.add("guiResolution", GUI_RESOLUTION);
		CONFIG.add("guiResolutionKeepAspectRatio", GUI_KEEP_ASPECT_RATIO);
		CONFIG.load();
	}

	/**
	 * Custom option for the vanilla video settings menu
	 */
	public static OptionInstance<Integer> fullscreenOption = createFullscreenOption();
	
	public static boolean getExclusiveFullscreenDefaultValue() {
		if(Util.getPlatform() == OS.LINUX) {
			return true;
		}
		return false;
	}

	////////////////////////////////////
	
	public static final Translations TRANS = Translations.get(null);
	
	static {
		Translations.loadFromNamespace(MODID);
	}

	////////////////////////////////////
	
	public static void openConfigScreen() {
		IScreen currentScreen = (IScreen) Minecraft.getInstance().screen;
		if(!(currentScreen instanceof ConfigScreen)) {
			GuiUtils.instance.setScreen(new ConfigScreen(null));	
		}
	}
	
	////////////////////////////////////
	
	public static boolean isFullscreenEnabled() {
		if(window == null) {
			return false;
		}
		return window.isFullscreen();
	}
	
	public static void setFullscreen(boolean value) {
		WindowAccess access = (WindowAccess)(Object)window;
		access.setFullscreen(value);

		OptionInstance<Boolean> fullscreenOption = getVanillaFullscreenOption();
		if(fullscreenOption != null) {
			fullscreenOption.set(value);
		}
	}
	public static void updateWindow() {
		windowNeedsUpdate = true;
	}

	/**
	 * The custom render resolution that should currently be applied, or null to render at the
	 * native framebuffer size.
	 *
	 * A custom fullscreen video mode changes the actual display mode, so the window already has the
	 * resolution the user asked for and there is nothing to upscale. In that case the custom render
	 * resolution is ignored rather than stacking the two.
	 */
	public static RenderResolution getActiveRenderResolution() {
		if(FULLSCREEN_VIDEO_MODE.get() != null) {
			return null;
		}
		return RENDER_RESOLUTION.get();
	}

	/**
	 * The part of the window the frame is presented into, as a fraction of the window size.
	 *
	 * Stretching fills the whole window, so the fraction is 1. Keeping the aspect ratio fits the
	 * frame inside the window instead and centres it, which leaves a bar on two of the sides.
	 *
	 * The window has the same shape in framebuffer pixels and in screen coordinates, so the same
	 * fraction describes the presented area in both.
	 */
	public static double getPresentFractionWidth() {
		RenderResolution resolution = getActiveRenderResolution();
		if(resolution == null || !KEEP_ASPECT_RATIO.getBoolean() || realFramebufferWidth <= 0 || realFramebufferHeight <= 0) {
			return 1.0;
		}
		return resolution.width * getFitScale(resolution) / realFramebufferWidth;
	}

	public static double getPresentFractionHeight() {
		RenderResolution resolution = getActiveRenderResolution();
		if(resolution == null || !KEEP_ASPECT_RATIO.getBoolean() || realFramebufferWidth <= 0 || realFramebufferHeight <= 0) {
			return 1.0;
		}
		return resolution.height * getFitScale(resolution) / realFramebufferHeight;
	}

	private static double getFitScale(RenderResolution resolution) {
		return Math.min((double) realFramebufferWidth / resolution.width,
				(double) realFramebufferHeight / resolution.height);
	}

	/**
	 * True when the frame does not cover the whole window, so the area around it has to be cleared.
	 */
	public static boolean hasEmptyBorder() {
		return getPresentFractionWidth() < 1.0 || getPresentFractionHeight() < 1.0;
	}

	/**
	 * Distance from the window edge to the presented frame, in screen coordinates. Cursor positions
	 * have to have this taken off before being scaled, otherwise everything is off by the size of
	 * the bar once the frame no longer starts in the corner of the window.
	 */
	public static double getCursorOffsetX() {
		return (1.0 - getPresentFractionWidth()) / 2.0 * realScreenWidth;
	}

	public static double getCursorOffsetY() {
		return (1.0 - getPresentFractionHeight()) / 2.0 * realScreenHeight;
	}

	/**
	 * Factor to convert a cursor position from the real window into the coordinate space the game
	 * believes the window has. 1 when no custom render resolution is active.
	 */
	public static double getCursorScaleX() {
		RenderResolution resolution = getActiveRenderResolution();
		if(resolution == null || realScreenWidth <= 0) {
			return 1.0;
		}
		final double presentedWidth = getPresentFractionWidth() * realScreenWidth;
		if(presentedWidth <= 0.0) {
			return 1.0;
		}
		return resolution.width / presentedWidth;
	}

	public static double getCursorScaleY() {
		RenderResolution resolution = getActiveRenderResolution();
		if(resolution == null || realScreenHeight <= 0) {
			return 1.0;
		}
		final double presentedHeight = getPresentFractionHeight() * realScreenHeight;
		if(presentedHeight <= 0.0) {
			return 1.0;
		}
		return resolution.height / presentedHeight;
	}
	
	/**
	 * The window size the gui and the hud are laid out for, or null to lay them out for the real
	 * window like vanilla does.
	 *
	 * This is not a render resolution: the game keeps rendering at the real size of the window, only
	 * the coordinate space the gui is placed in changes. That makes a hud that was positioned in a
	 * window of this size end up in the same places again without having to upscale the whole frame.
	 */
	public static RenderResolution getActiveGuiResolution() {
		return GUI_RESOLUTION.get();
	}

	/**
	 * The width of the gui coordinate space, in gui units.
	 *
	 * Vanilla uses framebufferWidth / guiScale, both for the size of the space that gui code lays
	 * itself out in and for the projection that space is drawn with. A custom gui resolution only
	 * replaces the numerator, so every position works out exactly as it would in a window of that
	 * size, while the projection still covers the whole of the real window.
	 *
	 * The gui resolution rarely has the same shape as the window, so by default the space is
	 * stretched to fill it. That is what keeps positions exact. Keeping the aspect ratio instead
	 * scales both axes by the same amount, which does not distort anything but does change the
	 * coordinate space in one axis, so positions along that axis drift.
	 */
	public static double getGuiExtentWidth(int viewportWidth, int viewportHeight, int guiScale) {
		RenderResolution resolution = getActiveGuiResolution();
		if(resolution == null || guiScale <= 0) {
			return (double) viewportWidth / Math.max(1, guiScale);
		}
		if(!GUI_KEEP_ASPECT_RATIO.getBoolean()) {
			return (double) resolution.width / guiScale;
		}
		return viewportWidth / getGuiUniformScale(resolution, viewportWidth, viewportHeight, guiScale);
	}

	public static double getGuiExtentHeight(int viewportWidth, int viewportHeight, int guiScale) {
		RenderResolution resolution = getActiveGuiResolution();
		if(resolution == null || guiScale <= 0) {
			return (double) viewportHeight / Math.max(1, guiScale);
		}
		if(!GUI_KEEP_ASPECT_RATIO.getBoolean()) {
			return (double) resolution.height / guiScale;
		}
		return viewportHeight / getGuiUniformScale(resolution, viewportWidth, viewportHeight, guiScale);
	}

	/**
	 * The single scale used when the aspect ratio is kept. Fitting the gui resolution inside the
	 * window rather than covering it means the coordinate space is never smaller than the one the hud
	 * was placed in, so nothing that used to be on screen ends up outside it.
	 */
	private static double getGuiUniformScale(RenderResolution resolution, int viewportWidth, int viewportHeight, int guiScale) {
		final double fit = Math.min((double) viewportWidth / resolution.width,
				(double) viewportHeight / resolution.height);
		if(fit <= 0.0) {
			return guiScale;
		}
		return guiScale * fit;
	}

	/**
	 * The size of a gui unit in real pixels, rounded up to a whole number.
	 *
	 * Item icons and the picture in picture elements are not drawn as geometry, they are rendered
	 * into their own textures at a size taken from the gui scale and then drawn into the gui at
	 * whatever size the coordinate space asks for. A custom gui resolution makes a gui unit larger
	 * than the gui scale suggests, so those textures have to be rendered larger to match, otherwise
	 * they are the one part of the gui that ends up blurry.
	 */
	public static int getEffectiveGuiScale(int viewportWidth, int viewportHeight, int guiScale) {
		if(getActiveGuiResolution() == null || guiScale <= 0) {
			return guiScale;
		}

		final double extentWidth = getGuiExtentWidth(viewportWidth, viewportHeight, guiScale);
		final double extentHeight = getGuiExtentHeight(viewportWidth, viewportHeight, guiScale);
		if(extentWidth <= 0.0 || extentHeight <= 0.0) {
			return guiScale;
		}

		final double scale = Math.max(viewportWidth / extentWidth, viewportHeight / extentHeight);
		return Mth.clamp((int) Math.ceil(scale), guiScale, MAX_EFFECTIVE_GUI_SCALE);
	}

	/**
	 * The size of the gui coordinate space in whole gui units, matching how vanilla rounds it.
	 */
	public static int toGuiScaledSize(double extent) {
		return Math.max(1, (int) Math.ceil(extent));
	}

	public static void setWindow(Window window) {
		FullscreenFix.window = window;
	}
	
	////////////////////////////////////
	
	private static OptionInstance<Integer> createFullscreenOption() {
		return new OptionInstance<Integer>("fullscreenoverride",
				OptionInstance.noTooltip(),
				(text, value) -> Component.nullToEmpty("idkwhatthisdoes"),
				new OptionInstance.ValueSet<>() {
					@Override
					public Function<OptionInstance<Integer>, AbstractWidget> createButton(TooltipSupplier<Integer> tooltipFactory, Options gameOptions, int x, int y, int width, Consumer<Integer> changeCallback) {
						return option -> {
							final Button button = Button.builder(getFullscreenModeDisplayText(), (pressedButton) -> {
								setFullscreenMode(getCurrentFullscreenMode().next());
								pressedButton.setMessage(getFullscreenModeDisplayText());
							}).build();
							return button;
						};
					}
					@Override
					public Optional<Integer> validateValue(Integer value) {
						return Optional.of(Mth.clamp(value, 0, 2));
					}
					@Override
					public Codec<Integer> codec() {
						return Codec.INT;
					}
				}, 0, (newValue) -> {}
		);
	}
	
	private static Component getFullscreenModeDisplayText() {
		FullscreenMode mode = getCurrentFullscreenMode();
		
		StringBuilder str = new StringBuilder();
		str.append(TRANS.asString("option.fullscreen")).append(": ");
		if(mode == FullscreenMode.BORDERLESS) {
			str.append(TRANS.asString("option.fullscreen.borderless"));
		}else if(mode == FullscreenMode.EXCLUSIVE) {
			str.append(TRANS.asString("option.fullscreen.exclusive"));
		}else {
			str.append(TRANS.asString("option.fullscreen.off"));
		}
		
		return Component.literal(str.toString());
	}
	
	public static FullscreenMode getCurrentFullscreenMode() {
		if(isFullscreenEnabled()) {
			if(EXCLUSIVE_FULLSCREEN.getBoolean()) {
				return FullscreenMode.EXCLUSIVE;
			}
			return FullscreenMode.BORDERLESS;
		}
		return FullscreenMode.OFF;
	}
	
	public static void setFullscreenMode(FullscreenMode mode) {
		if(mode == FullscreenMode.OFF) {
			setFullscreen(false);
		}else {
			setFullscreen(true);
			if(mode == FullscreenMode.EXCLUSIVE) {
				EXCLUSIVE_FULLSCREEN.setBoolean(true);
			}else {
				EXCLUSIVE_FULLSCREEN.setBoolean(false);
			}
		}
		fullscreenModeWasChanged = true;
	}
	
	public static OptionInstance<Boolean> getVanillaExclusiveFullscreenOption() {
		try {
			return Minecraft.getInstance().options.exclusiveFullscreen();	
		}catch (NullPointerException e) {
			return null;
		}
	}
	
	public static OptionInstance<Boolean> getVanillaFullscreenOption() {
		try {
			return Minecraft.getInstance().options.fullscreen();	
		}catch (NullPointerException e) {
			return null;
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
