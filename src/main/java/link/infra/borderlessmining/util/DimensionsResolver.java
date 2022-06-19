package link.infra.borderlessmining.util;

import link.infra.borderlessmining.config.ConfigHandler;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

public class DimensionsResolver {
	private static final Logger LOGGER = LogManager.getLogger(DimensionsResolver.class);

	public int x;
	public int y;
	public int width;
	public int height;

	public boolean resolve(Window window, MonitorTracker tracker) {
		if (ConfigHandler.getInstance().customWindowDimensions != null &&
			ConfigHandler.getInstance().customWindowDimensions.enabled &&
			!ConfigHandler.getInstance().customWindowDimensions.useMonitorCoordinates) {
			x = 0;
			y = 0;
			width = 0;
			height = 0;
		} else if (ConfigHandler.getInstance().forceWindowMonitor < 0) {
			Monitor monitor = tracker.getMonitor(window);
			if (monitor == null) {
				LOGGER.error("Failed to get a valid monitor for determining fullscreen size!");
				return false;
			}
			VideoMode mode = monitor.getCurrentVideoMode();
			x = monitor.getViewportX();
			y = monitor.getViewportY();
			width = mode.getWidth();
			height = mode.getHeight();
		} else {
			PointerBuffer monitors = GLFW.glfwGetMonitors();
			if (monitors == null || monitors.limit() < 1) {
				LOGGER.error("Failed to get a valid monitor list for determining fullscreen position!");
				return false;
			}
			long monitorHandle;
			if (ConfigHandler.getInstance().forceWindowMonitor >= monitors.limit()) {
				LOGGER.warn("Monitor " + ConfigHandler.getInstance().forceWindowMonitor + " is greater than list size " + monitors.limit() + ", using monitor 0");
				monitorHandle = monitors.get(0);
			} else {
				monitorHandle = monitors.get(ConfigHandler.getInstance().forceWindowMonitor);
			}
			try (MemoryStack stack = MemoryStack.stackPush()) {
				IntBuffer xBuf = stack.mallocInt(1);
				IntBuffer yBuf = stack.mallocInt(1);
				GLFW.glfwGetMonitorPos(monitorHandle, xBuf, yBuf);
				x = xBuf.get();
				y = yBuf.get();
			}
			GLFWVidMode mode = GLFW.glfwGetVideoMode(monitorHandle);
			if (mode == null) {
				LOGGER.error("Failed to get a video mode for the current monitor!");
				return false;
			} else {
				width = mode.width();
				height = mode.height();
			}
		}

		if (ConfigHandler.getInstance().customWindowDimensions != null) {
			ConfigHandler.CustomWindowDimensions dims = ConfigHandler.getInstance().customWindowDimensions;
			if (dims.enabled) {
				if (dims.useMonitorCoordinates) {
					x += dims.x;
					y += dims.y;
				} else {
					x = dims.x;
					y = dims.y;
				}
				if (dims.width > 0 && dims.height > 0) {
					width = dims.width;
					height = dims.height;
				} else if (!dims.useMonitorCoordinates) {
					LOGGER.error("Both width and height must be > 0 when specifying absolute coordinates!");
					return false;
				}
			}
		}
		return true;
	}

}
