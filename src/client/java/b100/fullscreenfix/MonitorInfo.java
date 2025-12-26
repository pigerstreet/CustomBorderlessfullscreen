package b100.fullscreenfix;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;

import net.minecraft.client.util.Window;

public class MonitorInfo {
	
	public static MonitorInfo getMonitor(Window window, boolean firstUpdate) {
		if(firstUpdate) {
			MonitorInfo last = FullscreenFix.LAST_FULLSCREEN_MONITOR.get();
			if(last != null) {
				FullscreenFix.debugPrint("Using last fullscreen monitor: " + last.toConfigString());
				return last;
			}else {
				FullscreenFix.debugPrint("No last fullscreen monitor to use!");
			}
		}
		return getMonitor(window.getHandle());
	}

	public static MonitorInfo getMonitor(long window) {
		int[] x = new int[1];
		int[] y = new int[1];
		glfwGetWindowPos(window, x, y);
		
		int[] w = new int[1];
		int[] h = new int[1];
		glfwGetWindowSize(window, w, h);
		
		return getMonitorAt(x[0] + w[0] / 2, y[0] + h[0] / 2);
	}
	
	public static MonitorInfo getMonitorAt(int x, int y) {
		PointerBuffer monitorPointers = glfwGetMonitors();
		
		for(int i = monitorPointers.position(); i < monitorPointers.limit(); i++) {
			long monitor = monitorPointers.get(i);
			
			MonitorInfo info = new MonitorInfo(monitor);
			if(x >= info.posX && y >= info.posY && x < info.posX + info.width && y < info.posY + info.height) {
				return info;
			}
		}
		
		return new MonitorInfo(glfwGetPrimaryMonitor());
	}
	
	public static List<MonitorInfo> getAllMonitors() {
		List<MonitorInfo> monitorInfos = new ArrayList<>();
		
		PointerBuffer monitors = glfwGetMonitors();
		
		while(monitors.hasRemaining()) {
			long monitor = monitors.get();
			
			monitorInfos.add(new MonitorInfo(monitor));
		}
		
		return monitorInfos;
	}
	
	public static MonitorInfo fromConfigString(String string) {
		if(string == null) {
			return null;
		}
		
		Integer width = null;
		Integer height = null;
		Integer posX = null;
		Integer posY = null;
		
		String[] split = string.split(";");
		for(int i=0; i < split.length; i++) {
			String[] keyValue = split[i].split(":");
			String key = keyValue[0];
			String value = keyValue[1];
			
			if(key.equals("w")) {
				width = Integer.parseInt(value);
			}else if(key.equals("h")) {
				height = Integer.parseInt(value);
			}else if(key.equals("x")) {
				posX = Integer.parseInt(value);
			}else if(key.equals("y")) {
				posY = Integer.parseInt(value);
			}
		}
		
		List<MonitorInfo> monitorInfos = getAllMonitors();
		for(MonitorInfo info : monitorInfos) {
			if(info.width == width && info.height == height && info.posX == posX && info.posY == posY) {
				return info;
			}
		}
		return null;
	}
	
	////////////////////////////////
	
	public final long handle;
	public final int posX;
	public final int posY;
	public final int width;
	public final int height;
	public final int refreshRate;
	
	public MonitorInfo(long monitor) {
		this.handle = monitor;
		
		GLFWVidMode vidmode = glfwGetVideoMode(monitor);
		
		int[] x = new int[1];
		int[] y = new int[1];
		glfwGetMonitorPos(monitor, x, y);
		posX = x[0];
		posY = y[0];
		
		width = vidmode.width();
		height = vidmode.height();
		refreshRate = vidmode.refreshRate();
	}
	
	public String toConfigString() {
		return "x:" + posX + ";y:" + posY + ";w:" + width + ";h:" + height;
	}
	
	public static boolean comparePositionAndSize(MonitorInfo i1, MonitorInfo i2) {
		if(i1 == null || i2 == null) {
			return false;
		}
		if(i1.posX != i2.posX) return false;
		if(i1.posY != i2.posY) return false;
		if(i1.width != i2.width) return false;
		if(i1.height != i2.height) return false;
		return true;
	}

}
