package b100.fullscreenfix;

import b100.lib.util.ConfigStringifiable;

/**
 * A custom internal render resolution.
 *
 * Unlike {@link VideoMode} this is not tied to a monitor video mode: the desktop resolution is not
 * changed at all. The game simply renders into a framebuffer of this size, which is then stretched
 * onto the real window when the frame is presented.
 */
public class RenderResolution implements ConfigStringifiable {

	/**
	 * Smallest resolution the game can sensibly render at, matches the minimum window size.
	 */
	public static final int MIN_SIZE = 1;

	public final int width;
	public final int height;

	public RenderResolution(int width, int height) {
		this.width = Math.max(MIN_SIZE, width);
		this.height = Math.max(MIN_SIZE, height);
	}

	public static RenderResolution parse(String string) {
		string = string.trim();
		if(string.length() == 0) {
			return null;
		}

		int width = 0;
		int height = 0;

		String[] str = string.split(";");
		for(int i=0; i < str.length; i++) {
			String entry = str[i];
			int j = entry.indexOf(':');
			if(j < 0) {
				continue;
			}
			String key = entry.substring(0, j);
			int value;
			try {
				value = Integer.parseInt(entry.substring(j + 1));
			}catch (NumberFormatException e) {
				FullscreenFix.print("Invalid render resolution in config: '" + string + "'");
				return null;
			}

			if(key.equals("width")) {
				width = value;
			}else if(key.equals("height")) {
				height = value;
			}
		}

		if(width <= 0 || height <= 0) {
			FullscreenFix.print("Invalid render resolution in config: '" + string + "'");
			return null;
		}

		return new RenderResolution(width, height);
	}

	@Override
	public String toString() {
		return width + " x " + height;
	}

	@Override
	public String toConfigString() {
		return "width:" + width + ";height:" + height;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		if(!(obj instanceof RenderResolution)) {
			return false;
		}
		RenderResolution other = (RenderResolution) obj;
		return width == other.width && height == other.height;
	}

	@Override
	public int hashCode() {
		return width * 31 + height;
	}

	public static boolean compare(RenderResolution o1, RenderResolution o2) {
		if(o1 == null || o2 == null) {
			return o1 == o2;
		}
		return o1.equals(o2);
	}
}
