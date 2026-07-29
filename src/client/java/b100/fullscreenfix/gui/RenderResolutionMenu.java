package b100.fullscreenfix.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import b100.fullscreenfix.FullscreenFix;
import b100.fullscreenfix.MonitorInfo;
import b100.fullscreenfix.RenderResolution;
import b100.lib.client.gui.element.GuiButton;
import b100.lib.client.gui.element.GuiElement;
import b100.lib.client.gui.element.GuiListButton;
import b100.lib.client.gui.element.GuiScrollableList;
import b100.lib.client.gui.element.GuiTextField;
import b100.lib.client.gui.element.GuiScrollableList.ListLayout;
import b100.lib.client.gui.screen.GuiScreen;
import b100.lib.client.gui.util.GuiUtils;
import b100.lib.client.mixin.IScreen;
import net.minecraft.network.chat.Component;

/**
 * Lets the user pick the resolution the game renders at. Unlike {@link ScreenResolutionsMenu} this
 * does not change the display mode, so the entries are not limited to what the monitor supports.
 */
public class RenderResolutionMenu extends GuiScreen {

	/**
	 * Fractions of the monitor resolution. These keep the aspect ratio of the monitor, so the image
	 * is scaled up without any distortion.
	 */
	private static final double[] NATIVE_SCALES = { 0.9, 0.8, 0.75, 0.66, 0.5, 0.33, 0.25 };

	/**
	 * Common resolutions, offered when they fit on the monitor. The ones that do not match the
	 * aspect ratio of the monitor will be stretched, which is usually the point of picking them.
	 */
	private static final int[][] COMMON_RESOLUTIONS = {
		{ 3840, 2160 }, { 2560, 1440 }, { 2560, 1080 }, { 1920, 1200 }, { 1920, 1080 },
		{ 1760, 990 }, { 1680, 1050 }, { 1600, 1200 }, { 1600, 900 }, { 1440, 1080 },
		{ 1366, 768 }, { 1280, 1024 }, { 1280, 960 }, { 1280, 800 }, { 1280, 720 },
		{ 1152, 864 }, { 1024, 768 }, { 800, 600 }, { 640, 480 },
	};

	/**
	 * Anything smaller than this is not worth rendering, anything larger is almost certainly a typo.
	 */
	private static final int MIN_TYPED_SIZE = 160;
	private static final int MAX_TYPED_SIZE = 16384;

	protected Component title;

	protected GuiButton cancelButton;
	protected GuiButton applyButton;

	protected int headerSize = 32;
	protected int inputSize = 26;
	protected int footerSize = 32;

	protected GuiScrollableList resolutionList;

	protected GuiTextField widthField;
	protected GuiTextField heightField;

	protected RenderResolution previousResolution;
	protected RenderResolution selectedResolution;

	/**
	 * True while the text fields and the list are being kept in sync with each other, so that
	 * updating one does not bounce straight back into the other.
	 */
	private boolean syncing = false;

	/**
	 * Set when the text fields contain something that is not a usable resolution. The selection is
	 * left alone in that case, but applying is blocked until it is fixed.
	 */
	private boolean typedValueInvalid = false;

	protected boolean initialized = false;

	public RenderResolutionMenu(IScreen parentScreen) {
		super(parentScreen);

		title = FullscreenFix.TRANS.asText("screen.renderResolution.title");
	}

	@Override
	protected void onInit() {
		applyButton = new GuiButton(this, FullscreenFix.TRANS.asText("button.apply"));
		cancelButton = new GuiButton(this, null);

		applyButton.addActionListener((e) -> apply());
		cancelButton.addActionListener((e) -> back());

		ListLayout layout = new ListLayout();
		layout.innerPadding = 0;
		layout.outerPadding = 2;

		resolutionList = add(new GuiScrollableList(this, layout));

		widthField = add(new GuiTextField(this, FullscreenFix.TRANS.asText("value.renderResolution.width")));
		heightField = add(new GuiTextField(this, FullscreenFix.TRANS.asText("value.renderResolution.height")));

		widthField.addActionListener((e) -> onTextChanged());
		heightField.addActionListener((e) -> onTextChanged());

		final MonitorInfo monitor = new MonitorInfo(GLFW.glfwGetPrimaryMonitor());

		resolutionList.add(new ResolutionElement(this, null, monitor));
		for(RenderResolution resolution : getResolutions(monitor)) {
			resolutionList.add(new ResolutionElement(this, resolution, monitor));
		}

		this.previousResolution = FullscreenFix.RENDER_RESOLUTION.get();
		this.selectedResolution = this.previousResolution;

		initialized = true;

		ResolutionElement element = getResolutionElement(this.selectedResolution);
		if(element != null) {
			element.setFocused(true);
		}
		updateTextFields(this.selectedResolution);

		add(applyButton);
		add(cancelButton);

		updateButtons();
	}

	/**
	 * Builds the list of offered resolutions: the monitor resolution itself, fractions of it, and
	 * any common resolution that fits on it. Duplicates are dropped, the largest comes first.
	 */
	public static List<RenderResolution> getResolutions(MonitorInfo monitor) {
		Set<RenderResolution> resolutions = new LinkedHashSet<>();

		resolutions.add(new RenderResolution(monitor.width, monitor.height));

		for(double scale : NATIVE_SCALES) {
			// Round to even numbers, odd framebuffer sizes scale badly
			int width = ((int)Math.round(monitor.width * scale) / 2) * 2;
			int height = ((int)Math.round(monitor.height * scale) / 2) * 2;
			if(width >= 320 && height >= 240) {
				resolutions.add(new RenderResolution(width, height));
			}
		}

		for(int[] resolution : COMMON_RESOLUTIONS) {
			if(resolution[0] <= monitor.width && resolution[1] <= monitor.height) {
				resolutions.add(new RenderResolution(resolution[0], resolution[1]));
			}
		}

		List<RenderResolution> sorted = new ArrayList<>(resolutions);
		sorted.sort((a, b) -> Long.compare((long)b.width * b.height, (long)a.width * a.height));

		return sorted;
	}

	public static String getAspectRatio(int width, int height) {
		int divisor = gcd(width, height);
		int x = width / divisor;
		int y = height / divisor;

		// Ratios like 1366:768 reduce to something unreadable, approximate those instead
		if(x > 32 || y > 32) {
			return String.format("%.2f:1", width / (double) height);
		}
		return x + ":" + y;
	}

	private static int gcd(int a, int b) {
		return b == 0 ? a : gcd(b, a % b);
	}

	public void apply() {
		if(typedValueInvalid) {
			return;
		}
		FullscreenFix.RENDER_RESOLUTION.set(selectedResolution);
		FullscreenFix.CONFIG.save();
		previousResolution = selectedResolution;
		updateButtons();
	}

	/**
	 * Called when an entry in the list is picked.
	 */
	public void setResolution(RenderResolution resolution) {
		if(!initialized || syncing) {
			return;
		}
		selectedResolution = resolution;
		typedValueInvalid = false;
		updateTextFields(resolution);
		updateButtons();
	}

	/**
	 * Called when either of the two text fields is edited. An empty pair of fields means the default
	 * resolution, anything unparseable blocks applying until it is corrected.
	 */
	private void onTextChanged() {
		if(!initialized || syncing) {
			return;
		}

		final String widthText = widthField.getText().trim();
		final String heightText = heightField.getText().trim();

		if(widthText.isEmpty() && heightText.isEmpty()) {
			typedValueInvalid = false;
			selectedResolution = null;
			updateListSelection(null);
			updateButtons();
			return;
		}

		final int width = parseSize(widthText);
		final int height = parseSize(heightText);

		if(width < 0 || height < 0) {
			// Half typed or out of range, keep the previous selection but block applying
			typedValueInvalid = true;
			updateButtons();
			return;
		}

		typedValueInvalid = false;
		selectedResolution = new RenderResolution(width, height);
		updateListSelection(selectedResolution);
		updateButtons();
	}

	/**
	 * Returns the parsed size, or -1 if it is not a number within the allowed range.
	 */
	private static int parseSize(String text) {
		if(text.isEmpty()) {
			return -1;
		}
		int value;
		try {
			value = Integer.parseInt(text);
		}catch (NumberFormatException e) {
			return -1;
		}
		if(value < MIN_TYPED_SIZE || value > MAX_TYPED_SIZE) {
			return -1;
		}
		return value;
	}

	private void updateTextFields(RenderResolution resolution) {
		syncing = true;
		if(resolution == null) {
			widthField.setText("");
			heightField.setText("");
		}else {
			widthField.setText(Integer.toString(resolution.width));
			heightField.setText(Integer.toString(resolution.height));
		}
		syncing = false;
	}

	/**
	 * Highlights the list entry matching the typed resolution, or clears the highlight when the
	 * typed resolution is not one of the offered ones.
	 */
	private void updateListSelection(RenderResolution resolution) {
		final ResolutionElement match = getResolutionElement(resolution);

		syncing = true;
		for(GuiElement element : resolutionList.elements) {
			if(element instanceof ResolutionElement) {
				ResolutionElement resolutionElement = (ResolutionElement) element;
				resolutionElement.setFocused(resolutionElement == match);
			}
		}
		syncing = false;
	}

	public void updateButtons() {
		if(typedValueInvalid) {
			applyButton.setClickable(false);
			cancelButton.text = FullscreenFix.TRANS.asText("button.cancel");
			return;
		}
		if(!RenderResolution.compare(selectedResolution, previousResolution)) {
			applyButton.setClickable(true);
			cancelButton.text = FullscreenFix.TRANS.asText("button.cancel");
		}else {
			applyButton.setClickable(false);
			cancelButton.text = FullscreenFix.TRANS.asText("button.done");
		}
	}

	public ResolutionElement getResolutionElement(RenderResolution resolution) {
		for(GuiElement element : resolutionList.elements) {
			if(element instanceof ResolutionElement) {
				ResolutionElement resolutionElement = (ResolutionElement) element;
				if(RenderResolution.compare(resolutionElement.resolution, resolution)) {
					return resolutionElement;
				}
			}
		}
		return null;
	}

	@Override
	public void draw() {
		utils.drawCenteredText(title, width / 2, headerSize / 2 - 4, 0xFFFFFF, true);

		// Separator between the width and height fields
		utils.drawCenteredText(Component.literal("x"), width / 2, headerSize + 6, 0xA0A0A0, true);

		super.draw();

		if(typedValueInvalid) {
			utils.drawCenteredText(FullscreenFix.TRANS.asText("value.renderResolution.invalid"),
					width / 2, height - footerSize - 12, 0xFF5555, true);
		}
	}

	@Override
	public boolean keyEvent(int key, int scancode, int modifiers, boolean pressed) {
		if(pressed && key == GLFW.GLFW_KEY_ENTER) {
			apply();
			back();
		}
		return super.keyEvent(key, scancode, modifiers, pressed);
	}

	@Override
	public void onResize() {
		int listWidth = Math.min(260, width - 20);
		int x0 = width / 2 - listWidth / 2;
		int y0 = headerSize + inputSize;
		int h = height - headerSize - inputSize - footerSize;

		// Two text fields either side of a small gap for the "x" separator
		int fieldWidth = (listWidth - 14) / 2;
		widthField.setPosition(x0, headerSize).setSize(fieldWidth, 20);
		heightField.setPosition(x0 + listWidth - fieldWidth, headerSize).setSize(fieldWidth, 20);

		resolutionList.setPosition(x0, y0).setSize(listWidth, h);

		for(GuiElement element : resolutionList.elements) {
			element.setSize(listWidth, 20);
		}

		GuiUtils.setDoubleFooterButtonPositions(this, height - footerSize + 4, applyButton, cancelButton);

		super.onResize();
	}

	class ResolutionElement extends GuiListButton {

		public final RenderResolution resolution;

		public ResolutionElement(GuiScreen screen, RenderResolution resolution, MonitorInfo monitor) {
			super(screen);
			this.resolution = resolution;

			if(resolution == null) {
				this.text = FullscreenFix.TRANS.asText("value.renderResolution.default");
			}else {
				StringBuilder str = new StringBuilder();
				str.append(resolution.width).append(" x ").append(resolution.height);
				str.append(" (").append(getAspectRatio(resolution.width, resolution.height)).append(')');
				if(resolution.width == monitor.width && resolution.height == monitor.height) {
					str.append(" - ").append(FullscreenFix.TRANS.asString("value.renderResolution.native"));
				}
				this.text = Component.nullToEmpty(str.toString());
			}
		}

		@Override
		public void onFocusChanged() {
			if(isFocused()) {
				setResolution(resolution);
			}
			super.onFocusChanged();
		}
	}
}
