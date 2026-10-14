package b100.fullscreenfix;

import b100.fullscreenfix.mixin.access.HudAccess;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws the crosshair after the rest of the hud, so that nothing arranged over the middle of the
 * screen can cover it.
 *
 * Vanilla draws the crosshair early and then starts a new layer of the hud, and every element added
 * after that goes into a layer of its own once it overlaps something. So anything drawn later that
 * covers the middle of the screen, which is exactly what a mod's overlay does once it has been placed
 * there, is drawn on top of the crosshair and hides it completely.
 *
 * There is no way to say "above everything" to the hud while it is being built, because that is only
 * known once the last element has been added. So the crosshair is held back instead, and drawn into
 * the frame once the hud is finished. The drawing is not reimplemented, the game's own method runs
 * again at that point, so every case it handles, being in third person or a spectator or the attack
 * indicator, behaves exactly as it does normally.
 *
 * Only the hud moves. A screen is drawn after the hud and is meant to cover it, so while one is open
 * the crosshair is left alone and menus still draw over it.
 */
public class CrosshairOnTop {

	private static GuiGraphicsExtractor deferredExtractor = null;
	private static DeltaTracker deferredDeltaTracker = null;
	private static boolean pending = false;

	/**
	 * Whether the crosshair is being drawn again later in this frame.
	 *
	 * Set for the length of that second drawing pass, and read before anything else, so that the pass
	 * cannot defer itself and the crosshair is never lost.
	 */
	private static boolean replaying = false;

	/**
	 * Whether the crosshair should be held back until the end of the frame.
	 *
	 * False while it is already being drawn, which is what stops the second pass from deferring too.
	 */
	public static boolean shouldDefer() {
		if(replaying || !FullscreenFix.CROSSHAIR_ON_TOP.getBoolean()) {
			return false;
		}
		return isHudWhatIsOnScreen();
	}

	/**
	 * Whether the crosshair belongs on top of what is being drawn, which is while the hud is what is on
	 * screen. A screen is drawn after the hud and is meant to cover it.
	 */
	private static boolean isHudWhatIsOnScreen() {
		final Minecraft minecraft = Minecraft.getInstance();
		if(minecraft == null) {
			return false;
		}
		return minecraft.gui.screen() == null || minecraft.gui.screen().isInGameUi();
	}

	public static void defer(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
		deferredExtractor = extractor;
		deferredDeltaTracker = deltaTracker;
		pending = true;
	}

	/**
	 * Draws the crosshair into the frame, at a point where everything else on the hud has been added.
	 *
	 * Called once per frame from the start of the render pass rather than from the end of the extraction
	 * pass, because a screen or an overlay is extracted after the hud is and this has to come after all
	 * of them. Nothing has been added to the hud for the rest of the frame by then.
	 */
	public static void replay() {
		if(!pending) {
			return;
		}
		pending = false;

		final GuiGraphicsExtractor extractor = deferredExtractor;
		final DeltaTracker deltaTracker = deferredDeltaTracker;
		deferredExtractor = null;
		deferredDeltaTracker = null;

		if(extractor == null) {
			return;
		}

		// Asked again rather than kept from the extraction pass. A frame that held the crosshair back
		// and then never rendered, which a level being unloaded in between would do, leaves this set
		// for the next one, and that one may well have a screen open by then. Within a frame the answer
		// is the same as it was then, so nothing that should be drawn gets skipped.
		if(!isHudWhatIsOnScreen()) {
			return;
		}

		final Minecraft minecraft = Minecraft.getInstance();
		if(minecraft == null || minecraft.gui == null) {
			return;
		}

		replaying = true;
		try {
			// The game's own method, which starts a layer of its own before drawing, so the crosshair
			// ends up in the last one
			((HudAccess) minecraft.gui.hud).invokeExtractCrosshair(extractor, deltaTracker);
		}finally {
			replaying = false;
		}
	}
}
