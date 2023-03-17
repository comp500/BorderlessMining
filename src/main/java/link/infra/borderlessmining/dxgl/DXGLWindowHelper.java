package link.infra.borderlessmining.dxgl;

import link.infra.borderlessmining.mixin.DXGLMinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.resource.DefaultResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.RECT;
import org.lwjgl.system.windows.User32;
import org.lwjgl.system.windows.WINDOWPLACEMENT;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiFunction;

public class DXGLWindowHelper {
	public static void migrateAll() {

	}

	/**
	 * Copy placement settings (position, size, maximised, unmaximised position/size) from one window to another
	 * Also migrates GLFW monitor (for fullscreen)
	 * @param oldGLFWHandle The old window (which will be hidden)
	 * @param newGLFWHandle The new window (about to be shown)
	 */
	public static void migrateCoordinates(long oldGLFWHandle, long newGLFWHandle) {
		long oldHwnd = GLFWNativeWin32.glfwGetWin32Window(oldGLFWHandle);
		long newHwnd = GLFWNativeWin32.glfwGetWin32Window(newGLFWHandle);
		if (oldHwnd == 0 || newHwnd == 0) {
			throw new RuntimeException("Failed to get win32 handle from GLFW handles for placement migration");
		}
		long oldMonitor = GLFW.glfwGetWindowMonitor(oldGLFWHandle);
		long newMonitor = GLFW.glfwGetWindowMonitor(newGLFWHandle);
		// Ensure the new handle is in a reasonable state
		if (newMonitor != 0) {
			throw new IllegalStateException("Should not migrate coordinates to a fullscreen window");
		}
		try (MemoryStack stack = MemoryStack.stackPush()) {
			WINDOWPLACEMENT placement = WINDOWPLACEMENT.callocStack(stack);
			if (!User32.GetWindowPlacement(oldHwnd, placement)) {
				throw new RuntimeException("Failed to get placement of previous window for migration (handle " + oldHwnd + ")");
			}

			// If in fullscreen, need to update fullscreen state
			if (oldMonitor != 0) {
				// First relinquish control of the monitor from the old handle (should keep the same coordinates in case drivers do funky heuristics)
				// TODO: should be using screen coordinates, not workspace coords from windowplacement?
				RECT coords = placement.rcNormalPosition();
				GLFW.glfwSetWindowMonitor(oldGLFWHandle, 0, coords.left(), coords.top(),
					coords.right() - coords.left(), coords.bottom() - coords.top(),
					GLFW.GLFW_DONT_CARE);
				// Then give the new handle the old monitor
				// TODO: use coordinates from video mode?
				GLFW.glfwSetWindowMonitor(newGLFWHandle, oldMonitor,
					coords.left(), coords.top(),
					coords.right() - coords.left(), coords.bottom() - coords.top(),
					GLFW.GLFW_DONT_CARE); // TODO: refresh rate from Window settings?
			}

			// Finally update window placement of the new window
			// TODO: does this update stuff properly???
			if (!User32.SetWindowPlacement(newHwnd, placement)) {
				throw new RuntimeException("Failed to set placement of new window for migration (handle " + newHwnd + ")");
			}
		}
	}

	/**
	 * Migrate callbacks from one window to another; removing them from the old window
	 */
	public static void migrateCallbacks(long oldGLFWHandle, long newGLFWHandle) {
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetWindowPosCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetWindowSizeCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetWindowCloseCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetWindowRefreshCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetWindowFocusCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetWindowIconifyCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetWindowMaximizeCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetFramebufferSizeCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetWindowContentScaleCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetKeyCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetCharCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetCharModsCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetMouseButtonCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetCursorPosCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetCursorEnterCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetScrollCallback);
		migrateCallback(oldGLFWHandle, newGLFWHandle, GLFW::nglfwSetDropCallback);
	}

	private static void migrateCallback(long oldGLFWHandle, long newGLFWHandle, BiFunction<Long, Long, Long> callbackSetter) {
		long callback = callbackSetter.apply(oldGLFWHandle, 0L);
		callbackSetter.apply(newGLFWHandle, callback);
	}

	private static final int[] INPUT_MODES = new int[]{ GLFW.GLFW_CURSOR, GLFW.GLFW_STICKY_KEYS, GLFW.GLFW_STICKY_MOUSE_BUTTONS, GLFW.GLFW_LOCK_KEY_MODS, GLFW.GLFW_RAW_MOUSE_MOTION };

	/**
	 * Copy input modes from one window to another
	 */
	public static void migrateInputModes(long oldGLFWHandle, long newGLFWHandle) {
		for (int mode : INPUT_MODES) {
			int value = GLFW.glfwGetInputMode(oldGLFWHandle, mode);
			GLFW.glfwSetInputMode(newGLFWHandle, mode, value);
		}
	}

	public static void updateTitles(long oldGLFWHandle, long newGLFWHandle) {
		// Easier to just query the window title from MinecraftClient than the existing window
		String title = ((DXGLMinecraftClientAccessor) MinecraftClient.getInstance()).callGetWindowTitle();
		GLFW.glfwSetWindowTitle(newGLFWHandle, title);

		GLFW.glfwSetWindowTitle(oldGLFWHandle, "Minecraft (DXGL offscreen context)");
	}

	// TODO: rerun this when exiting fullscreen if not run outside fullscreen - window chrome doesn't exist in fullscreen (so isn't set)
	public static void fixIcon(Window window) {
		// Can't fetch the original icon: need to grab it again
		DefaultResourcePack pack = MinecraftClient.getInstance().getResourcePackProvider().getPack();
		try {
			InputStream x16 = pack.open(ResourceType.CLIENT_RESOURCES, new Identifier("icons/icon_16x16.png"));
			InputStream x32 = pack.open(ResourceType.CLIENT_RESOURCES, new Identifier("icons/icon_32x32.png"));
			window.setIcon(x16, x32);
		} catch (IOException err) {
			// TODO: log err
		}
	}
}
