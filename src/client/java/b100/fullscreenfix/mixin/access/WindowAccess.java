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
	
}
