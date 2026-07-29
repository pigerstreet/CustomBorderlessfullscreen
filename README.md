# Custom Borderless Fullscreen

A fork of [Borderless Fullscreen](https://github.com/Bestsoft101/Borderless-Fullscreen) by Bestsoft100, for Minecraft **26.1.2** (Fabric).

It adds a **custom render resolution** that works together with borderless fullscreen, the way Counter-Strike 2 does it: the game renders at a resolution you choose and the finished frame is stretched over the whole window. The monitor keeps its own resolution, so alt-tabbing stays instant and there is no black screen when switching away.

## Render Resolution vs Fullscreen Resolution

The mod now has two different resolution options and they do very different things:

| Option | What it does |
| --- | --- |
| **Render Resolution** (added by this fork) | Renders at a custom resolution and stretches it to fill the window. The display mode is never touched, so it works with borderless fullscreen and alt-tab is instant. |
| **Fullscreen Resolution** (from the original mod) | Changes the actual display mode of the monitor. This forces exclusive fullscreen and causes the usual black screen when switching windows. |

Setting a Fullscreen Resolution overrides the Render Resolution, because a real display mode change already gives the requested resolution and there is nothing left to scale.

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

## License

GPL-3.0, same as the original mod. See `LICENSE.txt`.

Original mod and all of the code this builds on: **Bestsoft100** — https://github.com/Bestsoft101/Borderless-Fullscreen

The borderless fullscreen window handling on Windows is based on the [Cubes without Borders](https://github.com/Kir-Antipov/cubes-without-borders) mod (MIT), as noted in the source.
