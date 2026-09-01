package b100.fullscreenfix.mixin.access;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Window.class)
public interface WindowAccess {
	
	@Accessor("fullscreen")
	public void setFullscreen(boolean fullscreen);
	
	@Invoker("setMode")
	public void invokeUpdateWindowRegion();

	/**
	 * The gui scale as the window has it, without going through the getter.
	 *
	 * This mod answers {@code getGuiScale} with an enlarged number while a picture in picture renderer is
	 * measuring its own content, and other mods hook it as well. Anything working out the real size of a
	 * gui unit has to be reading the same number the gui was laid out from, which is this field.
	 */
	@Accessor("guiScale")
	public int getGuiScaleField();
	
}
