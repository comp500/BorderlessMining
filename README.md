# Borderless Mining [![Borderless Mining CurseForge Badge](http://cf.way2muchnoise.eu/full_borderless-mining_borders%20removed.svg)](https://www.curseforge.com/minecraft/mc-mods/borderless-mining)
Borderless Mining changes the fullscreen option to use a borderless window that fills the screen. This allows you to have Minecraft open on one monitor, while using other programs on another monitor - as normally Minecraft minimises when you unfocus it.

This mod also adds the Borderless Windowed option to the Video Settings screen, so you can easily disable the mod ingame, and has a configuration menu accessible through Mod Menu.

This mod is a rewrite of a Forge 1.12 mod, [Fullscreen Windowed (Borderless)](https://minecraft.curseforge.com/projects/fullscreen-windowed-borderless-for-minecraft), however it used to be a fork of that mod (which the 1.12 version of this mod is) - see the bottom of the Curseforge page for the original description.

## 1.13+ Changes
- Minecraft 1.13+ uses LWJGL 3, which completely changes how window management works.
- It technically uses borderless fullscreen when your fullscreen mode is the same as your display mode (Fullscreen Resolution: Current), but when you unfocus the screen it minimises.
- GLFW provides a way to disable this functionality with a window hint, GLFW\_AUTO\_ICONIFY, but this just makes a window that is atop all other windows!
- Therefore, this mod makes the normal fullscreen setting (controllable with a slider) use a window with no borders that covers the entirety of the current window. (although this can be configured)
- Unfortunately, this has *slightly worse* performance than the normal borderless fullscreen mode.
- On Mac, this mod is disabled by default, as it is impossible to position the window above the menu bar.
- This mod fixes a couple bugs: [MC-175431](https://bugs.mojang.com/browse/MC-175431) [MC-175437](https://bugs.mojang.com/browse/MC-175437)
- Other related Mojira bugs include: [MC-158117](https://bugs.mojang.com/browse/MC-158117) [MC-175233](https://bugs.mojang.com/browse/MC-175233) [MC-168675](https://bugs.mojang.com/browse/MC-168675)