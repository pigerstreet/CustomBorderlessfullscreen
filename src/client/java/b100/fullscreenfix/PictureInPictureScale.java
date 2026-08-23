package b100.fullscreenfix;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers which picture in picture renderers work out how big their own content is from the gui scale.
 *
 * A renderer belonging to another mod draws into a texture in one of two ways, and the two want opposite
 * things from a gui resolution. One draws in the pixels of the texture it was given, either through the
 * pose or by asking the texture how big it is, and wants that texture to be the pixels the element really
 * covers. The other multiplies its own size in gui units by {@code Window.getGuiScale()}, which is the
 * number of pixels vanilla guarantees a gui unit covers, and wants a texture measured the same way.
 *
 * There is no size that satisfies both, and nothing about a renderer says up front which it is. What does
 * say so is whether it reads the gui scale at all while it draws, so that is what gets watched: the gui
 * scale getter notes the renderer that was drawing when it was read, and from then on that renderer is
 * sized the way it expects rather than the way the window is. Being wrong for the first frame an element
 * appears costs nothing, because the texture is rebuilt as soon as its size changes.
 *
 * Only the render thread ever draws one of these, so the renderer currently drawing is a plain field. The
 * set it feeds is read from the same thread but written rarely and never cleared, so it is concurrent to
 * keep it safe if that ever stops being true.
 */
public class PictureInPictureScale {

	private static final Set<Class<?>> derivesSizeFromGuiScale = ConcurrentHashMap.newKeySet();

	private static Class<?> renderingToTexture = null;

	public static void beginRenderToTexture(Class<?> renderer) {
		renderingToTexture = renderer;
	}

	public static void endRenderToTexture() {
		renderingToTexture = null;
	}

	/**
	 * Notes that the gui scale was read, and by whom if a renderer is drawing into its texture right now.
	 *
	 * @return whether one is, and so whether the caller is a renderer that has to be answered with the
	 *         scale its texture was actually measured with rather than the one the window is set to
	 */
	public static boolean isRenderingToTexture() {
		final Class<?> renderer = renderingToTexture;
		if(renderer == null) {
			return false;
		}
		derivesSizeFromGuiScale.add(renderer);
		return true;
	}

	public static boolean derivesSizeFromGuiScale(Class<?> renderer) {
		return derivesSizeFromGuiScale.contains(renderer);
	}
}
