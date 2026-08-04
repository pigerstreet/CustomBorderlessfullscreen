# Custom Borderless Fullscreen

A fork of [Borderless Fullscreen](https://github.com/Bestsoft101/Borderless-Fullscreen) by Bestsoft100, for Minecraft **26.1.2** (Fabric).

It adds a **custom render resolution** that works together with borderless fullscreen, the way Counter-Strike 2 does it: the game renders at a resolution you choose and the finished frame is stretched over the whole window. The monitor keeps its own resolution, so alt-tabbing stays instant and there is no black screen when switching away.

It also adds a **GUI resolution**, which moves only the gui and the hud into the coordinate space of a different resolution while the game keeps rendering at the real one. If the reason for wanting a custom resolution is that a hud was set up at a different one, this fixes that without making anything blurry.

## The three resolution options

They sound similar and do very different things:

| Option | What it does |
| --- | --- |
| **GUI Resolution** (added by this fork) | Lays the gui and the hud out as if the window had this resolution. The game still renders at the real resolution, so nothing is upscaled and nothing is blurry. |
| **Render Resolution** (added by this fork) | Renders at a custom resolution and stretches it to fill the window. The display mode is never touched, so it works with borderless fullscreen and alt-tab is instant. |
| **Fullscreen Resolution** (from the original mod) | Changes the actual display mode of the monitor. This forces exclusive fullscreen and causes the usual black screen when switching windows. |

Setting a Fullscreen Resolution overrides the Render Resolution, because a real display mode change already gives the requested resolution and there is nothing left to scale.

### Which one to use

If the hud from another mod sits in the wrong place because it was positioned at a different resolution, use the **GUI Resolution** and set it to the resolution the hud was set up at. The gui goes back to being laid out the way it was, and the game keeps rendering sharp at the native resolution. Leave the Render Resolution on Default.

Use the **Render Resolution** instead when the point is to actually render fewer pixels, for performance.

### Stretching, and why the gui can end up looking wrong

A gui resolution whose shape differs from the window is stretched to fit it, because that is what keeps every hud position exact as a fraction of the screen. The cost is that the gui is then genuinely distorted, and by the difference between the two aspect ratios: laying out a 1720x1080 gui on a 2368x1320 window makes a gui unit 4.13 pixels wide and 3.67 pixels tall, so everything is drawn 13% wider than it is tall. Inventory slots stop being square, and item icons and text are stretched with them. Nothing is misaligned — the whole gui, the hud and anything another mod draws are all in the same stretched space — but it does not look right, and on a big enough difference it looks obviously wrong.

**GUI Keep Aspect Ratio** scales both axes by the same amount instead, so nothing is distorted. The coordinate space then covers the whole window rather than matching the gui resolution exactly: 1720x1080 on that window gives 646x360 instead of 574x360. Vertical positions and anything measured from the left or the top edge stay exactly where the hud put them, which is usually all that is wanted; only positions measured as a fraction of the width move, and they move to where they belong on a wider screen.

Turn it on unless a hud specifically needs fractional positions to match. If the gui resolution and the monitor have the same aspect ratio the option makes no difference at all.

**GUI Resolution: HUD Only** applies the gui resolution only while no screen is open. Menus have no misplaced hud to fix, and stretching them to a different shape is pure downside, so with this on they are laid out and drawn exactly as they are without the mod. The cost is that the hud goes back to its normal size for as long as any screen is open, chat included, because the hud and the screen on top of it share one projection per frame.

The resolution picker lists the gui coordinate space each entry produces, as `-> 574x360`, and marks the one matching the current space with `(now)`. That space is the number that actually decides where a hud lands, so the entry to pick is the one whose space matches what the hud was set up in.

### When something ends up in the wrong place

Turn on **Log Coordinates**. It reports the sizes the game is working with, and in particular the two independent ways a gui coordinate becomes a pixel:

```
Pixels per gui unit: cursor uses 4.125 x 3.667, drawing uses 4.130 x 3.667 (off by 1.001 x 1.0, 1.0 is correct)
```

Cursor positions are scaled by the gui size over the window size in screen coordinates; drawing is scaled by the window in pixels over the projection extent. Those come from different fields via different code, and they have to agree. A factor of about 1.001 is normal, and is present in vanilla too, because the gui size is rounded up to whole units while the extent is not. Anything larger means clicking lands somewhere other than what is on screen.

For the Counter-Strike style setup:

- Exclusive Fullscreen: **off**
- Fullscreen Resolution: **Default**
- Render Resolution: whatever you like

Picking a resolution with a different aspect ratio than the monitor stretches the image, which is usually the reason for choosing one.

## Usage

Open the config screen with **Ctrl + your fullscreen key** (F11 by default), or through Mod Menu, then choose **Render Resolution**.

The menu offers the monitor resolution, fractions of it that keep the aspect ratio, and common resolutions that fit on the monitor. A resolution can also be typed into the two text fields at the top, anything between 160 and 16384.

It can also be set in `config/fullscreenfix.properties`:

```
renderResolution:width:1280;height:720
```

Remove the line to render at the native resolution again.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.18.4 or newer
- Java 25

b100lib is bundled inside the jar, so no extra download is needed. Mod Menu is optional.

## Building

The game requires Java 25. Gradle downloads a matching toolchain by itself, so any recent JDK can run the build:

```
./gradlew build
```

The jar ends up in `build/libs/`.

## How it works

Everything hangs off `Window.framebufferWidth` / `framebufferHeight`, which the game uses for the main render target, the gui scale and the mouse mapping:

```
framebufferWidth/Height
  -> GameRenderer.extractWindow() -> resize() -> mainRenderTarget.resize()
  -> Window.setGuiScale() -> guiScaledWidth/Height -> gui and mouse
  -> RenderTarget.blitToScreen() -> GlCommandEncoder.presentTexture()
```

`WindowMixin` reports the custom resolution there and remembers the real framebuffer size separately, so the render target, gui scale and mouse coordinates all follow without any further patching.

`presentTexture` in 26.1.2 blits the frame with the destination rectangle equal to the source rectangle, which would leave a smaller frame sitting in the corner of the window. `GlCommandEncoderMixin` changes the destination to the real framebuffer size and switches the filter from `GL_NEAREST` to `GL_LINEAR` so the upscale is not needlessly harsh.

### GUI Resolution

The gui resolution never touches the framebuffer. Two numbers decide the coordinate space the gui lives in, and vanilla derives both from the same division:

```
Window.setGuiScale()      guiScaledWidth  = ceil(framebufferWidth / guiScale)   <- what gui code lays itself out against
GuiRenderer.draw()        setupOrtho(..., framebufferWidth / guiScale, ...)     <- what that space is drawn with
```

The projection extent is a float and covers the whole window whatever its value, so replacing the numerator in both places with the gui resolution moves the entire gui into the coordinate space of a differently sized window without changing a single thing about how the world is rendered. `WindowMixin` handles the first, `GuiRendererMixin` the second, and both read the extent from one place in `FullscreenFix` so they cannot disagree — if they did, everything would be offset by the difference.

Nothing has to be done about the mouse. Cursor positions are mapped with `xpos * guiScaledWidth / getScreenWidth()`, and since the gui still covers the whole window that ratio is already correct.

Item icons and the picture in picture elements are the exception to "nothing is upscaled": they are rendered into their own textures at a size taken from the gui scale, so they follow the effective scale instead, otherwise they would be the one blurry part of a sharp gui.

## License

GPL-3.0, same as the original mod. See `LICENSE.txt`.

Original mod and all of the code this builds on: **Bestsoft100** — https://github.com/Bestsoft101/Borderless-Fullscreen

The borderless fullscreen window handling on Windows is based on the [Cubes without Borders](https://github.com/Kir-Antipov/cubes-without-borders) mod (MIT), as noted in the source.
