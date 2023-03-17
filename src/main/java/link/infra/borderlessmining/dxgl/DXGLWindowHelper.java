package link.infra.borderlessmining.dxgl;

import link.infra.borderlessmining.mixin.DXGLMinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.windows.User32;
import org.lwjgl.system.windows.WINDOWPLACEMENT;

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
		try (MemoryStack stack = MemoryStack.stackPush()) {
			WINDOWPLACEMENT placement = WINDOWPLACEMENT.callocStack(stack);
			if (!User32.GetWindowPlacement(oldHwnd, placement)) {
				throw new RuntimeException("Failed to get placement of previous window for migration (handle " + oldHwnd + ")");
			}
			if (!User32.SetWindowPlacement(newHwnd, placement)) {
				throw new RuntimeException("Failed to set placement of new window for migration (handle " + newHwnd + ")");
			}
//			if (oldMonitor != newMonitor && oldMonitor != 0) {
//				// TODO: should be using screen coordinates, not workspace coords from windowplacement?
//				RECT coords = placement.rcNormalPosition();
//				GLFW.glfwSetWindowMonitor(newGLFWHandle, oldMonitor,
//					coords.left(), coords.top(),
//					coords.right() - coords.left(), coords.bottom() - coords.top(),
//					GLFW.GLFW_DONT_CARE); // TODO: refresh rate from Window settings?
//			}
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
}
