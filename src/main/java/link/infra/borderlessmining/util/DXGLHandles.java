package link.infra.borderlessmining.util;

import org.lwjgl.glfw.GLFW;

public class DXGLHandles {
	public static long glfwDXGLInit = GLFW.getLibrary().getFunctionAddress("glfwDXGLInit");
	public static long glfwBindDXGLRenderbuffer = GLFW.getLibrary().getFunctionAddress("glfwBindDXGLRenderbuffer");
	public static long glfwUnbindDXGLRenderbuffer = GLFW.getLibrary().getFunctionAddress("glfwUnbindDXGLRenderbuffer");
}
