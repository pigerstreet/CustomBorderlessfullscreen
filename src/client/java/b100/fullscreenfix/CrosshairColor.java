package b100.fullscreenfix;

/**
 * The colours the crosshair can be drawn in, for when it is not being drawn by inverting what is
 * behind it.
 *
 * These are the sixteen colour palette's brighter entries, which are the ones that stay readable
 * against both a bright sky and a dark cave. Black is there for anyone who wants it, but it is the
 * one that disappears into a dark background the way white disappears into a bright one.
 */
public enum CrosshairColor {

	WHITE(0xFFFFFFFF),
	RED(0xFFFF5555),
	GREEN(0xFF55FF55),
	CYAN(0xFF55FFFF),
	YELLOW(0xFFFFFF55),
	MAGENTA(0xFFFF55FF),
	BLACK(0xFF000000);

	public final int argb;

	CrosshairColor(int argb) {
		this.argb = argb;
	}
}
