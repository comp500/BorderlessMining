package link.infra.borderlessmining;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class GLFWReplace implements PreLaunchEntrypoint {
	private final Logger LOGGER = LogManager.getLogger("BorderlessMining 2?");

	@Override
	public void onPreLaunch() {
		Path dllCopyPath = Paths.get(".borderlessmining", "glfw3.dll");

		try {
			Files.createDirectories(dllCopyPath.getParent());
			InputStream stream = GLFWReplace.class.getClassLoader().getResourceAsStream("assets/borderlessmining/glfw3.dll");
			if (stream == null) {
				LOGGER.error("Failed to read GLFW dll");
			} else {
				Files.copy(stream, dllCopyPath, StandardCopyOption.REPLACE_EXISTING);
				Configuration.GLFW_LIBRARY_NAME.set(dllCopyPath.toString());
				String glfwVersionString = GLFW.glfwGetVersionString();
				if (glfwVersionString.contains("BorderlessMining")) {
					LOGGER.info("GLFW replaced, version string: " + glfwVersionString);
				} else {
					LOGGER.error("GLFW replacement failed! Version string: " + glfwVersionString);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed to copy GLFW dll", e);
		}
	}
}
