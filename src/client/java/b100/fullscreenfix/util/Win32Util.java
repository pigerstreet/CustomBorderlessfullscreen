package b100.fullscreenfix.util;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.windows.User32.*;

import org.lwjgl.glfw.GLFWNativeWin32;

import b100.fullscreenfix.FullscreenFix;
import b100.fullscreenfix.MonitorInfo;
import b100.fullscreenfix.VideoMode;
import com.mojang.blaze3d.platform.Window;

public class Win32Util {
	
	public static long getWin32Handle(Window window) {
		return getWin32Handle(window.getWindow());
	}
	
	public static long getWin32Handle(long window) {
		long hwnd = GLFWNativeWin32.glfwGetWin32Window(window);
		if(hwnd == 0) {
			throw new NullPointerException("No Win32 Handle!");
		}
		return hwnd;
	}
	
	public static void updateWindowState(Window window, int windowPosX, int windowPosY, int windowWidth, int windowHeight, boolean windowWasMaximized, boolean firstUpdate) {
		boolean fullscreen = window.isFullscreen();
		boolean borderless = FullscreenFix.BORDERLESS_FULLSCREEN.getBoolean();
		boolean optimizations = FullscreenFix.FULLSCREEN_OPTIMIZATIONS.getBoolean();
		VideoMode fullscreenMode = FullscreenFix.FULLSCREEN_VIDEO_MODE.get();
		
		long hwnd = getWin32Handle(window);
		
		if(fullscreen && fullscreenMode != null) {
			FullscreenFix.print("Change to Fullscreen with custom resolution");
			
			glfwSetWindowAttrib(window.getWindow(), GLFW_DECORATED, 1);
			setWindowStyle(hwnd, WS_VISIBLE);
			glfwHideWindow(window.getWindow());
			
			MonitorInfo monitorInfo = new MonitorInfo(fullscreenMode.monitor);
			glfwSetWindowMonitor(window.getWindow(), 
					fullscreenMode.monitor,
					monitorInfo.posX,
					monitorInfo.posY,
					fullscreenMode.vidMode.width(),
					fullscreenMode.vidMode.height(),
					fullscreenMode.vidMode.refreshRate()
			);

			glfwSetWindowAttrib(window.getWindow(), GLFW_AUTO_ICONIFY, FullscreenFix.AUTO_MINIMIZE.getBoolean() ? 1 : 0);
			glfwShowWindow(window.getWindow());
			glfwFocusWindow(window.getWindow());
		}else if(!fullscreen) {
			if(windowWasMaximized) {
				FullscreenFix.print("Change to Maximized Windowed Mode");
			}else {
				FullscreenFix.print("Change to Windowed Mode");	
			}
			
			if(GLFWUtil.isFullscreen(window)) {
				GLFWUtil.disableFullscreen(window, windowPosX, windowPosY, windowWidth, windowHeight);
			}
			
			setDefaultWindowStyle(window, hwnd);
			
			// Set Position and Size
			glfwSetWindowPos(window.getWindow(), windowPosX, windowPosY);
			glfwSetWindowSize(window.getWindow(), windowWidth, windowHeight);
			if(windowWasMaximized) {
				glfwMaximizeWindow(window.getWindow());
			}
		}else if(borderless && !optimizations) {
			FullscreenFix.print("Change to Borderless Fullscreen without Fullscreen optimizations");
			
			// Disable fullscreen and then enable again, otherwise switching from custom resolution fullscreen won't work
			GLFWUtil.disableFullscreen(window, windowPosX, windowPosY, windowWidth, windowHeight);
			
			// Borderless Fullscreen without fullscreen optimizations, code taken from the Cubes without Borders Mod, licensed MIT
			// https://github.com/Kir-Antipov/cubes-without-borders
			
			MonitorInfo monitor = MonitorInfo.getMonitor(window, firstUpdate);
			GLFWUtil.enableFullscreen(window, monitor);
			glfwSetWindowAttrib(window.getWindow(), GLFW_AUTO_ICONIFY, 0);
			
			SetWindowPos(hwnd, HWND_NOTOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE);
			setWindowStyle(hwnd, WS_VISIBLE | WS_CLIPCHILDREN | WS_CLIPSIBLINGS | WS_SYSMENU | WS_GROUP);
			setWindowExtendedStyle(hwnd, WS_EX_APPWINDOW | WS_EX_ACCEPTFILES | WS_EX_COMPOSITED | WS_EX_LAYERED);
			
			FullscreenFix.LAST_FULLSCREEN_MONITOR.set(monitor);
		}else if(borderless && optimizations) {
			FullscreenFix.print("Change to Borderless Fullscreen with Fullscreen optimizations");
			
			if(GLFWUtil.isFullscreen(window)) {
				GLFWUtil.disableFullscreen(window, windowPosX, windowPosY, windowWidth, windowHeight);
			}

			setDefaultWindowStyle(window, hwnd);
			glfwSetWindowAttrib(window.getWindow(), GLFW_DECORATED, 0);
			
			MonitorInfo monitor = MonitorInfo.getMonitor(window, firstUpdate);
			glfwSetWindowPos(window.getWindow(), monitor.posX, monitor.posY);
			glfwSetWindowSize(window.getWindow(), monitor.width, monitor.height);
			
			FullscreenFix.LAST_FULLSCREEN_MONITOR.set(monitor);
		}else {
			FullscreenFix.print("Change to GLFW Fullscreen");

			// Disable fullscreen and then enable again, otherwise switching from borderless fullscreen with optimizations won't work
			GLFWUtil.disableFullscreen(window, windowPosX, windowPosY, windowWidth, windowHeight);
			
			setDefaultWindowStyle(window, hwnd);
			glfwSetWindowAttrib(window.getWindow(), GLFW_AUTO_ICONIFY, FullscreenFix.AUTO_MINIMIZE.getBoolean() ? 1 : 0);
			
			MonitorInfo monitor = MonitorInfo.getMonitor(window, firstUpdate);
			GLFWUtil.enableFullscreen(window, monitor);
			
			FullscreenFix.LAST_FULLSCREEN_MONITOR.set(monitor);
		}
		
		GLFWUtil.updateCursorMode();
	}
	
	public static void setDefaultWindowStyle(Window window, long hwnd) {
		glfwSetWindowAttrib(window.getWindow(), GLFW_DECORATED, 1);
		
		long style = WS_VISIBLE;
		
		// Enable title bar
		style |= WS_CAPTION;
		style |= WS_SYSMENU;
		
		// Enable window controls
		style |= WS_MAXIMIZEBOX;
		style |= WS_MINIMIZEBOX;
		style |= WS_THICKFRAME;
		
		setWindowStyle(hwnd, style);
	}
	
	/**
	 * https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowpos
	 */
	public static void setAlwaysOnTop(long hwnd, boolean alwaysOnTop) {
		if(alwaysOnTop) {
			SetWindowPos(hwnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE);
		}else {
			SetWindowPos(hwnd, HWND_NOTOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE);
		}
	}
	
	/**
	 * https://learn.microsoft.com/en-us/windows/win32/winmsg/window-styles
	 */
	public static void setWindowStyle(long hwnd, long style) {
		SetWindowLongPtr(hwnd, GWL_STYLE, style);
	}
	
	/**
	 * https://learn.microsoft.com/en-us/windows/win32/winmsg/extended-window-styles
	 */
	public static void setWindowExtendedStyle(long hwnd, long style) {
		SetWindowLongPtr(hwnd, GWL_EXSTYLE, style);
	}

}
